package com.example.miles.wear.engine

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.example.miles.wear.data.model.GpsPoint
import com.example.miles.wear.data.model.NavState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real turn-by-turn navigation for the watch via OSRM (free, no API key).
 * Fetches a walking/cycling route, draws it on the map and exposes a live
 * next-turn state with haptic alerts at segment boundaries.
 */
object WearRoutingEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _route = MutableStateFlow<List<GpsPoint>?>(null)
    val route: StateFlow<List<GpsPoint>?> = _route.asStateFlow()

    private val _navState = MutableStateFlow<NavState?>(null)
    val navState: StateFlow<NavState?> = _navState.asStateFlow()

    private val _destination = MutableStateFlow<GpsPoint?>(null)
    val destination: StateFlow<GpsPoint?> = _destination.asStateFlow()

    private var fetched: Route? = null
    private var manager: LocationManager? = null
    private var locListener: LocationListener? = null
    private var updateJob: Job? = null
    private var lastSegmentIndex = -1
    private var alertedNearTurn = false
    private var currentPosition: GpsPoint? = null

    data class RouteStep(val text: String, val streetName: String, val distanceMeters: Double)

    data class Route(
        val points: List<GpsPoint>,
        val steps: List<RouteStep>,
        val totalMeters: Double,
        val durationSeconds: Long
    ) {
        // Cumulative distance at the start of each step
        val cumStart: List<Double> by lazy {
            val out = mutableListOf(0.0)
            var acc = 0.0
            steps.forEach { acc += it.distanceMeters; out.add(acc) }
            out
        }
    }

    fun isNavigating(): Boolean = _navState.value?.isArrived == false && _route.value != null

    /** Total length (meters) of the currently active route, real or followed. */
    val currentTotalMeters: Double
        get() = _route.value?.let { pts -> fetched?.totalMeters ?: 0.0 } ?: 0.0

    /**
     * Follow a saved route (no network needed): draws it and tracks the user
     * along it with the same progress/arrival/ETA logic as live navigation.
     */
    @SuppressLint("MissingPermission")
    fun followRoute(context: Context, points: List<GpsPoint>, name: String) {
        stop()
        if (points.size < 2) return
        val totalMeters = polylineMeters(points)
        if (totalMeters <= 0.0) return
        // Real speed assumption for the ETA: brisk walk ≈ 1.4 m/s
        val durationSeconds = (totalMeters / 1.4).toLong().coerceAtLeast(60L)
        val route = Route(
            points = points,
            steps = listOf(RouteStep("Follow $name", "", totalMeters)),
            totalMeters = totalMeters,
            durationSeconds = durationSeconds
        )
        fetched = route
        _destination.value = points.lastOrNull()
        _route.value = points
        _navState.value = NavState(
            maneuver = "Follow route",
            streetName = name,
            distanceToNextMeters = totalMeters,
            remainingMeters = totalMeters,
            remainingMinutes = durationSeconds / 60L,
            isFetching = false,
            hasRoute = true
        )

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        manager = lm
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                currentPosition = GpsPoint(location.latitude, location.longitude, location.altitude, location.speed.toDouble(), location.time)
            }
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
            @Deprecated("Deprecated in Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        locListener = listener
        try {
            val provider = when {
                lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true -> LocationManager.GPS_PROVIDER
                lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            provider?.let {
                lm?.requestLocationUpdates(it, 1000L, 2f, listener, Looper.getMainLooper())
            }
        } catch (_: SecurityException) {
        }

        startUpdateLoop(route)
    }

    /** Approximate route length across the polyline (haversine per segment). */
    private fun polylineMeters(points: List<GpsPoint>): Double {
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += haversineMeters(points[i], points[i + 1])
        }
        return total
    }

    private fun haversineMeters(a: GpsPoint, b: GpsPoint): Double {
        val earthR = 6371000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * earthR * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Begin navigating to [dest] from the device's current location. */
    @SuppressLint("MissingPermission")
    fun start(context: Context, dest: GpsPoint, profile: String = "walking") {
        stop()
        _navState.value = NavState("Fetching route…", "", 0.0, 0.0, 0, isFetching = true, hasRoute = false)
        val appContext = context.applicationContext
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        val startLocation = lastKnown(lm)
        if (startLocation == null) {
            _navState.value = NavState("No GPS fix", "", 0.0, 0.0, 0, hasRoute = false)
            return
        }

        scope.launch {
            val route = fetchRoute(
                startLocation.latitude, startLocation.longitude,
                dest.lat, dest.lon, profile
            )
            if (route == null || route.points.size < 2) {
                _navState.value = NavState("No route found", "", 0.0, 0.0, 0, hasRoute = false)
                return@launch
            }
            fetched = route
            _destination.value = dest
            _route.value = route.points
            _navState.value = NavState(
                maneuver = route.steps.firstOrNull()?.text ?: "Start",
                streetName = route.steps.firstOrNull()?.streetName ?: "",
                distanceToNextMeters = route.steps.getOrNull(1)?.distanceMeters ?: route.totalMeters,
                remainingMeters = route.totalMeters,
                remainingMinutes = (route.durationSeconds / 60L),
                isFetching = false,
                hasRoute = true
            )

            manager = lm
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    currentPosition = GpsPoint(location.latitude, location.longitude, location.altitude, location.speed.toDouble(), location.time)
                }
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            locListener = listener
            try {
                val provider = when {
                    lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true -> LocationManager.GPS_PROVIDER
                    lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }
                provider?.let {
                    lm?.requestLocationUpdates(it, 1000L, 2f, listener, Looper.getMainLooper())
                }
            } catch (_: SecurityException) {
            }

            startUpdateLoop(route)
        }
    }

    fun stop() {
        updateJob?.cancel()
        updateJob = null
        try {
            locListener?.let { manager?.removeUpdates(it) }
        } catch (_: Exception) {
        }
        locListener = null
        manager = null
        fetched = null
        lastSegmentIndex = -1
        alertedNearTurn = false
        currentPosition = null
        _route.value = null
        _destination.value = null
        _navState.value = null
    }

    private fun startUpdateLoop(route: Route) {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                withContext(Dispatchers.Default) {
                    val pos = currentPosition ?: return@withContext
                    val covered = routeCoveredMeters(route.points, pos)
                    updateState(route, covered, pos)
                }
                delay(2000L)
            }
        }
    }

    private fun updateState(route: Route, coveredMeters: Double, pos: GpsPoint) {
        val total = route.totalMeters
        if (total <= 0.0) return

        // Which step segment we are on
        val cum = route.cumStart
        var seg = 0
        while (seg < cum.size - 1 && coveredMeters >= cum[seg + 1]) seg++
        seg = seg.coerceIn(0, route.steps.lastIndex)

        val currentStep = route.steps[seg]
        val nextEnd = if (seg + 1 < cum.size) cum[seg + 1] else total
        val distToNext = (nextEnd - coveredMeters).coerceAtLeast(0.0)
        val remaining = (total - coveredMeters).coerceAtLeast(0.0)
        val etaMin = (route.durationSeconds * (remaining / total) / 60L).toLong().coerceAtLeast(0L)

        val arrived = remaining <= 20.0
        val isNewSegment = seg != lastSegmentIndex
        lastSegmentIndex = seg

        if (arrived) {
            if (!(_navState.value?.isArrived == true)) {
                _navState.value = NavState(
                    maneuver = "Arrived",
                    streetName = currentStep.streetName,
                    distanceToNextMeters = 0.0,
                    remainingMeters = 0.0,
                    remainingMinutes = 0,
                    isArrived = true,
                    hasRoute = true
                )
            }
            return
        }

        val nextStep = route.steps.getOrNull(seg + 1)
        _navState.value = NavState(
            maneuver = currentStep.text,
            streetName = currentStep.streetName,
            distanceToNextMeters = distToNext,
            remainingMeters = remaining,
            remainingMinutes = etaMin,
            isFetching = false,
            hasRoute = true
        )
        nextStep?.let { /* preview kept for richer HUD */ }
    }

    /** Fetches an OSRM route between two coordinates. */
    suspend fun fetchRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, profile: String = "walking"): Route? = withContext(Dispatchers.IO) {
        val endpoint = "https://router.project-osrm.org/route/v1/$profile/" +
            "$fromLon,$fromLat;$toLon,$toLat?overview=full&geometries=geojson&steps=true&alternatives=false"
        val body: String = try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
            }
            try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            return@withContext null
        }

        try {
            val root = JSONObject(body)
            if (root.optString("code") != "Ok") return@withContext null
            val routeJson = root.optJSONArray("routes")?.optJSONObject(0) ?: return@withContext null

            val points = mutableListOf<GpsPoint>()
            val geometry = routeJson.optJSONObject("geometry") ?: return@withContext null
            val coords = geometry.optJSONArray("coordinates") ?: return@withContext null
            for (i in 0 until coords.length()) {
                val c = coords.optJSONArray(i) ?: continue
                points.add(GpsPoint(c.optDouble(1), c.optDouble(0), 0.0, 0.0, 0L))
            }
            if (points.size < 2) return@withContext null

            val steps = mutableListOf<RouteStep>()
            val leg = routeJson.optJSONArray("legs")?.optJSONObject(0)
            val stepArray = leg?.optJSONArray("steps")
            if (stepArray != null) {
                for (i in 0 until stepArray.length()) {
                    val s = stepArray.optJSONObject(i) ?: continue
                    val maneuver = s.optJSONObject("maneuver")
                    val type = maneuver?.optString("type") ?: "continue"
                    val modifier = maneuver?.optString("modifier") ?: ""
                    steps.add(
                        RouteStep(
                            text = maneuverText(type, modifier),
                            streetName = s.optString("name"),
                            distanceMeters = s.optDouble("distance", 0.0)
                        )
                    )
                }
            }

            Route(
                points = points,
                steps = steps,
                totalMeters = routeJson.optDouble("distance", 0.0),
                durationSeconds = routeJson.optLong("duration", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun maneuverText(type: String, modifier: String): String {
        if (type == "depart") return "Head ${bearingText(modifier)}"
        if (type == "arrive") return "Arrive"
        if (type == "end of road") return "End of road"
        return when (modifier) {
            "left" -> "Turn left"
            "right" -> "Turn right"
            "sharp left" -> "Sharp left"
            "sharp right" -> "Sharp right"
            "slight left" -> "Keep left"
            "slight right" -> "Keep right"
            "straight" -> "Continue straight"
            "uturn" -> "U-turn"
            else -> if (type == "continue") "Continue" else "Continue on"
        }
    }

    private fun bearingText(modifier: String): String = when (modifier) {
        "east" -> "East"
        "north" -> "North"
        "south" -> "South"
        "west" -> "West"
        "northeast" -> "Northeast"
        "northwest" -> "Northwest"
        "southeast" -> "Southeast"
        "southwest" -> "Southwest"
        else -> "Forward"
    }

    private fun lastKnown(lm: LocationManager?): Location? {
        if (lm == null) return null
        return try {
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).mapNotNull { provider ->
                try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }.maxByOrNull { it.time }
        } catch (_: Exception) {
            null
        }
    }

    /** Meters along the route polyline to the point nearest [pos]. */
    private fun routeCoveredMeters(points: List<GpsPoint>, pos: GpsPoint): Double {
        var bestDist = Double.MAX_VALUE
        var covered = 0.0
        var acc = 0.0
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val segLen = hypot(b.lat - a.lat, b.lon - a.lon)
            val (d, t) = pointSegmentDistance(a, b, pos)
            if (d < bestDist) {
                bestDist = d
                covered = acc + segLen * t
            }
            acc += segLen
        }
        return covered
    }

    /** Distance from [p] to segment [a]-[b] (in degrees-space) + projection t in 0..1. */
    private fun pointSegmentDistance(a: GpsPoint, b: GpsPoint, p: GpsPoint): Pair<Double, Double> {
        val dx = b.lat - a.lat
        val dy = b.lon - a.lon
        val len2 = dx * dx + dy * dy
        if (len2 <= 0.0) return Pair(hypot(p.lat - a.lat, p.lon - a.lon), 0.0)
        var t = ((p.lat - a.lat) * dx + (p.lon - a.lon) * dy) / len2
        t = t.coerceIn(0.0, 1.0)
        val projLat = a.lat + t * dx
        val projLon = a.lon + t * dy
        val d = hypot(p.lat - projLat, p.lon - projLon)
        // Convert degrees distance to approximate meters
        val meters = d * 111_320.0 * cos(Math.toRadians(p.lat))
        return Pair(meters, t)
    }
}