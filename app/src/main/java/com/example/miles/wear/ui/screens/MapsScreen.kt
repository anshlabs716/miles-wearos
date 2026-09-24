package com.example.miles.wear.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Place
import androidx.wear.compose.material3.Icon
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.example.miles.wear.MilesWearApplication
import com.example.miles.wear.data.local.entity.SavedPinEntity
import com.example.miles.wear.data.model.GpsPoint
import com.example.miles.wear.engine.SearchResult
import com.example.miles.wear.engine.WearPlaceSearch
import com.example.miles.wear.engine.WearRoutingEngine
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.max
import kotlin.math.min

private data class WearMapLayer(
    val label: String,
    val attribution: String,
    val source: XYTileSource
)

private val mapLayers = listOf(
    WearMapLayer(
        "OSM",
        "© OpenStreetMap contributors",
        XYTileSource("OpenStreetMap", 0, 19, 256, ".png", arrayOf("https://tile.openstreetmap.org/"))
    ),
    WearMapLayer(
        "CYCLO",
        "© CyclOSM / OpenStreetMap",
        XYTileSource("CyclOSM", 0, 20, 256, ".png", arrayOf(
            "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
        ))
    ),
    WearMapLayer(
        "HOT",
        "© OpenStreetMap contributors / HOT",
        XYTileSource("HOT", 0, 19, 256, ".png", arrayOf(
            "https://a.tile.openstreetmap.fr/hot/",
            "https://b.tile.openstreetmap.fr/hot/",
            "https://c.tile.openstreetmap.fr/hot/"
        ))
    ),
    WearMapLayer(
        "DARK",
        "© CARTO / OpenStreetMap",
        XYTileSource("CartoDark", 0, 20, 256, ".png", arrayOf(
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/"
        ))
    ),
    WearMapLayer(
        "LIGHT",
        "© CARTO / OpenStreetMap",
        XYTileSource("CartoVoyager", 0, 20, 256, ".png", arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
        ))
    ),
    WearMapLayer(
        "TOPO",
        "© OpenTopoMap contributors",
        XYTileSource("OpenTopoMap", 0, 17, 256, ".png", arrayOf(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        ))
    ),
    WearMapLayer(
        "SAT",
        "USGS satellite imagery",
        XYTileSource("USGS Satellite", 0, 20, 256, ".jpg", arrayOf(
            "https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryOnly/MapServer/tile/"
        ))
    )
)

@Composable
fun MapsScreen(followRouteId: Long? = null) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val compact = min(configuration.screenWidthDp, configuration.screenHeightDp) <= 192
    val coroutineScope = rememberCoroutineScope()

    val repository = MilesWearApplication.instance.repository
    val pins by repository.allPins.collectAsStateWithLifecycle(initialValue = emptyList())
    val navState by WearRoutingEngine.navState.collectAsStateWithLifecycle()
    val navRoute by WearRoutingEngine.route.collectAsStateWithLifecycle()

    var layerIndex by remember { mutableStateOf(0) }
    var following by remember { mutableStateOf(true) }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }

    // Search state
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<SearchResult?>(null) }
    var selectedPin by remember { mutableStateOf<SavedPinEntity?>(null) }

    // Route builder (from pins) + save feedback
    var builtRoute by remember { mutableStateOf<List<GpsPoint>?>(null) }
    var savedFlash by remember { mutableStateOf<String?>(null) }
    var followHandled by remember { mutableStateOf(false) }

    val layer = mapLayers[layerIndex]
    val buttonSize = if (compact) 38.dp else 44.dp

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = "MILES-WearOS/${context.packageName}"
        }
        MapView(context).apply {
            setTileSource(mapLayers[0].source)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            setUseDataConnection(true)
            controller.setZoom(15.0)
        }
    }

    fun centerOn(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
    }

    fun locate() {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

        try {
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }.maxByOrNull { it.time }?.let { centerOn(it) }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    centerOn(location)
                    manager.removeUpdates(this)
                }
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }

            val provider = when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            provider?.let { manager.requestSingleUpdate(it, listener, Looper.getMainLooper()) }
        } catch (_: SecurityException) {
        }
    }

    LaunchedEffect(layerIndex) {
        mapView.setTileSource(layer.source)
        mapView.invalidate()
    }

    // Load + follow a saved route passed from the Routes screen (real points).
    LaunchedEffect(followRouteId, followHandled) {
        val id = followRouteId
        if (id != null && !followHandled) {
            followHandled = true
            coroutineScope.launch {
                val route = repository.getRouteById(id)
                if (route != null) {
                    val pts = repository.routePoints(route)
                    if (pts.size >= 2) {
                        WearRoutingEngine.followRoute(context, pts, route.name)
                        savedFlash = "Following ${route.name}"
                    }
                }
            }
        }
    }

    DisposableEffect(mapView) {
        var locationOverlay: MyLocationNewOverlay? = null

        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).also {
                    it.enableMyLocation()
                    mapView.overlays.add(it)
                }
                locate()
            } catch (_: SecurityException) {
            }
        }

        mapView.overlays.add(ScaleBarOverlay(mapView).apply {
            setCentred(true)
            setScaleBarOffset(8, 8)
        })
        mapView.onResume()

        onDispose {
            WearRoutingEngine.stop()
            locationOverlay?.disableMyLocation()
            mapView.overlays.clear()
            mapView.onPause()
        }
    }

    // Redraw pins + nav route + built route as overlays when they change
    LaunchedEffect(pins, navRoute, selectedPlace, builtRoute) {
        mapView.overlays.filterIsInstance<Marker>().forEach { mapView.overlays.remove(it) }
        mapView.overlays.filterIsInstance<Polyline>().forEach { mapView.overlays.remove(it) }

        navRoute?.let { pts ->
            if (pts.size >= 2) {
                val line = Polyline(mapView).apply {
                    setPoints(pts.map { GeoPoint(it.lat, it.lon) })
                    outlinePaint.color = 0xFF00E676.toInt()
                    outlinePaint.strokeWidth = 14f
                }
                mapView.overlays.add(line)
                try {
                    mapView.zoomToBoundingBox(line.bounds, true, 40)
                } catch (_: Exception) {
                }
            }
        }

        builtRoute?.let { pts ->
            if (pts.size >= 2) {
                val line = Polyline(mapView).apply {
                    setPoints(pts.map { GeoPoint(it.lat, it.lon) })
                    outlinePaint.color = 0xFFFF9100.toInt()
                    outlinePaint.strokeWidth = 16f
                }
                mapView.overlays.add(line)
                try {
                    mapView.zoomToBoundingBox(line.bounds, true, 40)
                } catch (_: Exception) {
                }
                pts.forEach { p ->
                    Marker(mapView).apply {
                        position = GeoPoint(p.lat, p.lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Route point"
                    }.let { mapView.overlays.add(it) }
                }
            }
        }

        pins.forEach { pin ->
            Marker(mapView).apply {
                position = GeoPoint(pin.latitude, pin.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = pin.name
                setOnMarkerClickListener { _, _ ->
                    selectedPin = pin
                    selectedPlace = null
                    vibrate(context, 40L)
                    true
                }
            }.let { mapView.overlays.add(it) }
        }

        selectedPlace?.let { place ->
            Marker(mapView).apply {
                position = GeoPoint(place.latitude, place.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = place.displayName
            }.let { mapView.overlays.add(it) }
        }

        mapView.invalidate()
    }

    // Haptic cues as navigation advances / arrives
    LaunchedEffect(navState) {
        val ns = navState ?: return@LaunchedEffect
        if (ns.isArrived) {
            vibrate(context, longArrayOf(0, 250, 120, 250, 120, 500))
        }
    }

    if (searchOpen) {
        SearchOverlay(
            query = query,
            onQueryChange = { query = it },
            results = results,
            searching = searching,
            onSearch = {
                searching = true
                coroutineScope.launch {
                    results = WearPlaceSearch.search(query)
                    searching = false
                }
            },
            onPick = { result ->
                searchOpen = false
                selectedPlace = result
                selectedPin = null
                mapView.controller.animateTo(GeoPoint(result.latitude, result.longitude))
                mapView.controller.setZoom(16.0)
            },
            onClose = {
                searchOpen = false
                results = emptyList()
            }
        )
        return
    }

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        AndroidView({ mapView }, Modifier.fillMaxSize())

        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = if (compact) 4.dp else 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("MILES MAP • ${layer.label}")

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        layerIndex = (layerIndex + 1) % mapLayers.size
                        following = false
                    },
                    modifier = Modifier.size(width = if (compact) 76.dp else 88.dp, height = 34.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Change map layer")
                    Text(layer.label)
                }

                Button(
                    onClick = {
                        following = !following
                        if (following) locate()
                    },
                    modifier = Modifier.size(width = if (compact) 76.dp else 88.dp, height = 34.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "Toggle follow mode")
                    Text(if (following) "Follow" else "Free")
                }
            }
        }

        // Navigation HUD (bottom, above action buttons)
        val ns = navState
        if (ns != null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 94.dp)
                    .background(androidx.compose.ui.graphics.Color(0xF01A1A1E), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ns.maneuver,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (ns.isArrived) VividGreen else ElectricAmber,
                        maxLines = 1
                    )
                    if (!ns.isArrived) {
                        Text(
                            text = if (ns.distanceToNextMeters >= 1000) {
                                String.format("%.1f km", ns.distanceToNextMeters / 1000.0)
                            } else {
                                "${ns.distanceToNextMeters.toInt()} m"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = if (ns.isArrived) "You have arrived 🎉" else ns.streetName.ifEmpty { "----" },
                    fontSize = 9.sp,
                    color = MutedGray,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!ns.isArrived) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ETA ${ns.remainingMinutes} min",
                            fontSize = 9.sp,
                            color = NeonCyan
                        )
                        Text(
                            text = String.format("%.1f km left", ns.remainingMeters / 1000.0),
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                }
            }
        }

        // Selected place / pin action card
        val place = selectedPlace
        val pin = selectedPin
        if (place != null || pin != null) {
            val name = place?.let { shortPlaceName(it.displayName) } ?: pin?.name ?: ""
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 94.dp)
                    .background(androidx.compose.ui.graphics.Color(0xF01A1A1E), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Button(
                        onClick = {
                            val dest = place?.let { GpsPoint(it.latitude, it.longitude) }
                                ?: pin?.let { GpsPoint(it.latitude, it.longitude) }
                            if (dest != null) {
                                selectedPlace = null
                                selectedPin = null
                                coroutineScope.launch {
                                    WearRoutingEngine.start(context, dest)
                                }
                            }
                        },
                        modifier = Modifier.size(width = 76.dp, height = 34.dp)
                    ) { Text("Go ➜") }
                    if (place != null) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.addPin(shortPlaceName(place.displayName), place.latitude, place.longitude)
                                }
                                selectedPlace = null
                            },
                            modifier = Modifier.size(width = 76.dp, height = 34.dp)
                        ) { Text("Save") }
                    } else if (pin != null) {
                        Button(
                            onClick = {
                                coroutineScope.launch { repository.deletePin(pin.id) }
                                selectedPin = null
                            },
                            modifier = Modifier.size(width = 76.dp, height = 34.dp)
                        ) { Text("Delete") }
                    }
                }
            }
        }

        // Bottom action row: Search + Drop pin + My location
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = if (compact) 6.dp else 10.dp, bottom = 54.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { mapView.controller.setZoom(min(19.0, mapView.zoomLevelDouble + 1.0)) },
                modifier = Modifier.size(buttonSize),
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "Zoom in") }

            Button(
                onClick = { mapView.controller.setZoom(max(3.0, mapView.zoomLevelDouble - 1.0)) },
                modifier = Modifier.size(buttonSize),
                shape = CircleShape
            ) { Icon(Icons.Default.Remove, contentDescription = "Zoom out") }

            Button(
                onClick = {
                    following = true
                    locate()
                },
                modifier = Modifier.size(buttonSize),
                shape = CircleShape
            ) { Icon(Icons.Default.MyLocation, contentDescription = "My location") }
        }

        Column(
            Modifier.align(Alignment.BottomStart).padding(start = if (compact) 6.dp else 10.dp, bottom = 54.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    searchOpen = true
                    selectedPlace = null
                    selectedPin = null
                },
                modifier = Modifier.size(width = 78.dp, height = 38.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search")
                Text("Search")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        val center = mapView.mapCenter
                        coroutineScope.launch {
                            repository.addPin("Pin #${pins.size + 1}", center.latitude, center.longitude)
                        }
                        vibrate(context, 50L)
                    },
                    modifier = Modifier
                        .size(width = 72.dp, height = 38.dp)
                        .background(androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Icon(Icons.Default.Place, contentDescription = "Drop pin at center")
                    Text("Pin")
                }

                Button(
                    onClick = {
                        val ordered = pins.asReversed()
                        if (ordered.size < 2) {
                            vibrate(context, longArrayOf(0, 120, 80, 120))
                            savedFlash = "Need 2+ pins"
                            return@Button
                        }
                        coroutineScope.launch {
                            val pts = ordered.map { GpsPoint(it.latitude, it.longitude, 0.0, 0.0, it.createdAt) }
                            builtRoute = pts
                            savedFlash = "Route from ${pts.size} pins"
                        }
                        vibrate(context, 50L)
                    },
                    modifier = Modifier
                        .size(width = 72.dp, height = 38.dp)
                        .background(androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Text("🧩", fontSize = 12.sp)
                    Text("Route")
                }
            }

            if (navState != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            val pts = WearRoutingEngine.route.value
                            val total = WearRoutingEngine.currentTotalMeters
                            if (pts != null && pts.size >= 2 && total > 0.0) {
                                coroutineScope.launch {
                                    repository.saveRoute(
                                        name = "Route ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date())}",
                                        points = pts,
                                        distanceMeters = total
                                    )
                                }
                                savedFlash = "Route saved ✓"
                                vibrate(context, longArrayOf(0, 80, 60, 120))
                            }
                        },
                        modifier = Modifier.size(width = 72.dp, height = 38.dp)
                    ) {
                        Text("Save")
                    }
                    Button(
                        onClick = {
                            WearRoutingEngine.stop()
                            selectedPlace = null
                            selectedPin = null
                        },
                        modifier = Modifier.size(width = 72.dp, height = 38.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Stop navigation")
                        Text("Stop")
                    }
                }
            }
        }

        // Route builder card (from pins)
        builtRoute?.let { pts ->
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 94.dp)
                    .background(androidx.compose.ui.graphics.Color(0xF0181F18), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🧩 Route: ${pts.size} pins • ${"%.2f".format(routeMeters(pts) / 1000.0)} km",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = ElectricAmber
                )
                Spacer(modifier = Modifier.size(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Button(
                        onClick = {
                            if (pts.size >= 2) {
                                coroutineScope.launch {
                                    repository.saveRoute(
                                        name = "Route ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date())}",
                                        points = pts,
                                        distanceMeters = routeMeters(pts)
                                    )
                                }
                                builtRoute = null
                                savedFlash = "Route saved ✓"
                            }
                        },
                        modifier = Modifier.size(width = 76.dp, height = 34.dp)
                    ) { Text("Save") }
                    Button(
                        onClick = { builtRoute = null },
                        modifier = Modifier.size(width = 76.dp, height = 34.dp)
                    ) { Text("Clear") }
                }
            }
        }

        // Small flash feedback (saved / hints)
        savedFlash?.let { flash ->
            Text(
                text = flash,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = VividGreen,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).navigationBarsPadding()
            )
            LaunchedEffect(flash) {
                kotlinx.coroutines.delay(1800L)
                savedFlash = null
            }
        }

        Text(
            text = if (latitude != 0.0 || longitude != 0.0)
                "${layer.attribution} • %.4f, %.4f".format(latitude, longitude)
            else layer.attribution,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 5.dp),
            style = androidx.compose.ui.text.TextStyle(fontSize = 8.sp, color = MutedGray)
        )
    }
}

@Composable
private fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SearchResult>,
    searching: Boolean,
    onSearch: () -> Unit,
    onPick: (SearchResult) -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔎 SEARCH",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = NeonCyan,
            modifier = Modifier.padding(top = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .background(androidx.compose.ui.graphics.Color(0xFF18181C), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Place, city, POI…", fontSize = 12.sp, color = MutedGray)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = onSearch,
                modifier = Modifier.size(width = 56.dp, height = 34.dp)
            ) { Icon(Icons.Default.Search, contentDescription = "Search") }
            Button(
                onClick = onClose,
                modifier = Modifier.size(width = 40.dp, height = 34.dp)
            ) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }

        if (searching) {
            Text("Searching…", fontSize = 11.sp, color = MutedGray, modifier = Modifier.padding(top = 12.dp))
        } else if (results.isEmpty()) {
            Text(
                "Type a place or POI, then tap search.\nResults come from OpenStreetMap.",
                fontSize = 10.sp,
                color = MutedGray,
                modifier = Modifier.padding(top = 14.dp),
                textAlign = TextAlign.Center
            )
        } else {
            results.forEach { result ->
                Button(
                    onClick = { onPick(result) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = androidx.wear.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF18181C)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = shortPlaceName(result.displayName),
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "%.4f, %.4f".format(result.latitude, result.longitude),
                            fontSize = 8.sp,
                            color = MutedGray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun shortPlaceName(displayName: String): String {
    val parts = displayName.split(", ")
    return if (parts.size >= 2) "${parts[0]}, ${parts[1]}" else displayName
}

/** Approximate polyline length in meters (haversine per segment). */
private fun routeMeters(points: List<GpsPoint>): Double {
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
    val h = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
        kotlin.math.cos(Math.toRadians(a.lat)) * kotlin.math.cos(Math.toRadians(b.lat)) *
        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    return 2 * earthR * kotlin.math.atan2(kotlin.math.sqrt(h), kotlin.math.sqrt(1 - h))
}

private fun vibrate(context: Context, milliseconds: Long) {
    try {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {
    }
}

private fun vibrate(context: Context, pattern: LongArray) {
    try {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    } catch (_: Exception) {
    }
}