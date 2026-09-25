package com.example.miles.wear.sensor

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import com.example.miles.wear.data.model.GpsPoint
import com.example.miles.wear.data.model.GpsStatus
import com.example.miles.wear.data.model.HeartRateZone
import com.example.miles.wear.data.model.LiveHeartRate
import com.example.miles.wear.data.model.LiveWorkoutMetrics
import com.example.miles.wear.util.HapticHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.concurrent.ConcurrentLinkedQueue

class SensorTracker(private val context: Context) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Hardware Sensors
    private val heartRateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    val isHeartRateSensorPresent: Boolean = heartRateSensor != null
    val isStepSensorPresent: Boolean = stepCounterSensor != null || stepDetectorSensor != null
    val isPressureSensorPresent: Boolean = pressureSensor != null

    /** User's body weight (kg) — only used for real movement-based calorie estimates. */
    @Volatile
    var bodyWeightKg: Int = 70

    // Reactive State
    private val _liveHeartRate = MutableStateFlow(
        LiveHeartRate(isAvailable = isHeartRateSensorPresent)
    )
    val liveHeartRate: StateFlow<LiveHeartRate> = _liveHeartRate.asStateFlow()

    // External BLE sensors (heart-rate strap / cadence) — override watch sensors
    private val _externalHr = MutableStateFlow<Int?>(null)
    val externalHr: StateFlow<Int?> = _externalHr.asStateFlow()
    @Volatile private var externalCadenceRpm: Int? = null

    /** True when an external HR source is driving the live heart rate. */
    val isExternalHrActive: Boolean get() = _externalHr.value != null

    private val _liveMetrics = MutableStateFlow(
        LiveWorkoutMetrics(isHrAvailable = isHeartRateSensorPresent)
    )
    val liveMetrics: StateFlow<LiveWorkoutMetrics> = _liveMetrics.asStateFlow()

    private val _gpsStatus = MutableStateFlow(GpsStatus.SEARCHING)
    val gpsStatus: StateFlow<GpsStatus> = _gpsStatus.asStateFlow()

    private val _isLowPowerMode = MutableStateFlow(false)
    val isLowPowerMode: StateFlow<Boolean> = _isLowPowerMode.asStateFlow()

    private val _batteryPercent = MutableStateFlow(100)
    val batteryPercent: StateFlow<Int> = _batteryPercent.asStateFlow()

    // GPS Track Recording
    private val _recordedRoute = MutableStateFlow<List<GpsPoint>>(emptyList())
    val recordedRoute: StateFlow<List<GpsPoint>> = _recordedRoute.asStateFlow()

    // Heart rate tracking statistics
    private val _hrHistory = MutableStateFlow<List<Int>>(emptyList())
    val hrHistory: StateFlow<List<Int>> = _hrHistory.asStateFlow()
    var minBpm: Int = 0
        private set
    var maxBpm: Int = 0
        private set
    var avgBpm: Int = 0
        private set

    // Split cues
    private var lastSplitUnitIndex = 0

    // Internal workout tracking accumulation
    private var isTracking = false
    private var startTimestamp = 0L
    private var initialStepCount = -1
    private var workoutSteps = 0
    private var currentDailySteps = 0
    private var stepBaseline = -1
    private var initialAltitude = 0.0
    private var currentAltitude = 0.0
    private var maxAltitude = 0.0
    private var minAltitude = 0.0
    private var totalCaloriesBurned = 0.0
    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0.0
    private var isGpsFixAcquired = false

    // Cadence window (last 60s step timestamps)
    private val stepTimestamps = ConcurrentLinkedQueue<Long>()

    // Battery receiver for duty-cycle throttling
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val pct = (level * 100) / scale
                    _batteryPercent.value = pct
                    val lowPower = pct < 20
                    if (lowPower != _isLowPowerMode.value) {
                        _isLowPowerMode.value = lowPower
                        if (isTracking) {
                            reconfigureSensorsForPowerMode(lowPower)
                        }
                    }
                }
            }
        }
    }

    init {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            Log.e("SensorTracker", "Failed to register battery receiver", e)
        }
        // Read initial daily steps if step counter is available
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun startTracking(isIndoor: Boolean = false) {
        isTracking = true
        startTimestamp = System.currentTimeMillis()
        initialStepCount = -1
        workoutSteps = 0
        totalDistanceMeters = 0.0
        totalCaloriesBurned = 0.0
        lastLocation = null
        initialAltitude = 0.0
        currentAltitude = 0.0
        isGpsFixAcquired = false
        stepTimestamps.clear()
        _recordedRoute.value = emptyList()
        _hrHistory.value = emptyList()
        minBpm = 0
        maxBpm = 0
        avgBpm = 0
        lastSplitUnitIndex = 0

        if (isIndoor) {
            _gpsStatus.value = GpsStatus.INDOOR
        } else {
            _gpsStatus.value = if (isGpsProviderEnabled()) GpsStatus.SEARCHING else GpsStatus.UNAVAILABLE
        }

        registerSensors()
        if (!isIndoor) {
            requestLocationUpdates()
        }
    }

    fun stopTracking() {
        isTracking = false
        unregisterSensors()
        removeLocationUpdates()
    }

    private fun isGpsProviderEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    private fun settingsPrefs() =
        context.getSharedPreferences("miles_wear_prefs", Context.MODE_PRIVATE)

    private fun isEcoMode(): Boolean =
        _isLowPowerMode.value || settingsPrefs().getBoolean("battery_saver_enabled", false)

    private fun registerSensors() {
        val prefs = settingsPrefs()
        val delay = if (isEcoMode()) {
            SensorManager.SENSOR_DELAY_NORMAL
        } else {
            SensorManager.SENSOR_DELAY_UI
        }

        if (prefs.getBoolean("hr_enabled", true)) {
            heartRateSensor?.let {
                sensorManager.registerListener(this, it, delay)
            }
        }
        if (prefs.getBoolean("step_tracking_enabled", true)) {
            stepCounterSensor?.let {
                sensorManager.registerListener(this, it, delay)
            }
            stepDetectorSensor?.let {
                sensorManager.registerListener(this, it, delay)
            }
        }
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
        // Keep listening to daily step counter at low frequency
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun reconfigureSensorsForPowerMode(lowPower: Boolean) {
        unregisterSensors()
        registerSensors()
        removeLocationUpdates()
        requestLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        try {
            if (settingsPrefs().getBoolean("gps_enabled", true) &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            ) {
                val minTime = if (isEcoMode()) 10000L else 2000L
                val minDistance = if (isEcoMode()) 10f else 2f
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minTime,
                    minDistance,
                    this
                )
            } else {
                _gpsStatus.value = GpsStatus.UNAVAILABLE
            }
        } catch (e: SecurityException) {
            Log.w("SensorTracker", "Location permission not granted for GPS", e)
            _gpsStatus.value = GpsStatus.UNAVAILABLE
        } catch (e: Exception) {
            Log.e("SensorTracker", "Error starting GPS", e)
            _gpsStatus.value = GpsStatus.UNAVAILABLE
        }
    }

    private fun removeLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            Log.e("SensorTracker", "Error removing GPS updates", e)
        }
    }

    /**
     * Redirects live HR to an external BLE heart-rate strap. Passing null
     * returns control to the watch's optical sensor.
     */
    fun setExternalHeartRate(bpm: Int?) {
        _externalHr.value = bpm
        if (bpm == null) {
            // Keep last known internal reading visible.
            return
        }
        if (bpm in 30..240) {
            val reading = LiveHeartRate(
                bpm = bpm,
                accuracy = 3,
                isAvailable = true,
                isFromExternal = true,
                timestamp = System.currentTimeMillis()
            )
            _liveHeartRate.value = reading
            if (isTracking) {
                val currentList = _hrHistory.value.toMutableList()
                currentList.add(bpm)
                _hrHistory.value = currentList
                if (minBpm == 0 || bpm < minBpm) minBpm = bpm
                if (bpm > maxBpm) maxBpm = bpm
                avgBpm = currentList.average().toInt()
            }
            updateWorkoutMetrics()
        }
    }

    /** External BLE cadence sensor (RPM). Null falls back to step-derived cadence. */
    fun setExternalCadence(rpm: Int?) {
        externalCadenceRpm = rpm
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                // External BLE strap wins over the watch's optical sensor.
                if (_externalHr.value != null) return
                val rawBpm = event.values.getOrNull(0)?.toInt() ?: 0
                if (rawBpm > 0) {
                    val reading = LiveHeartRate(
                        bpm = rawBpm,
                        accuracy = event.accuracy,
                        isAvailable = true,
                        timestamp = System.currentTimeMillis()
                    )
                    _liveHeartRate.value = reading
                    if (isTracking) {
                        val currentList = _hrHistory.value.toMutableList()
                        currentList.add(rawBpm)
                        _hrHistory.value = currentList
                        if (minBpm == 0 || rawBpm < minBpm) minBpm = rawBpm
                        if (rawBpm > maxBpm) maxBpm = rawBpm
                        avgBpm = currentList.average().toInt()
                    }
                    updateWorkoutMetrics()
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val totalDeviceSteps = event.values.getOrNull(0)?.toInt() ?: 0
                // TYPE_STEP_COUNTER reports steps since the device last rebooted,
                // NOT today's steps. Convert to a real "today" count using a
                // persisted midnight baseline — never show the cumulative total.
                val today = todayKey()
                val prefs = settingsPrefs()
                val savedDate = prefs.getString("step_baseline_date", null)
                val savedBaseline = prefs.getInt("step_baseline", -1)
                val newDay = savedDate != today
                val counterReset = totalDeviceSteps < savedBaseline
                if (newDay || savedBaseline < 0 || counterReset) {
                    prefs.edit()
                        .putString("step_baseline_date", today)
                        .putInt("step_baseline", totalDeviceSteps)
                        .apply()
                    stepBaseline = totalDeviceSteps
                    currentDailySteps = 0
                } else {
                    stepBaseline = savedBaseline
                    currentDailySteps = totalDeviceSteps - stepBaseline
                }

                if (isTracking) {
                    if (initialStepCount < 0) {
                        initialStepCount = totalDeviceSteps
                    }
                    val delta = if (totalDeviceSteps >= initialStepCount) {
                        totalDeviceSteps - initialStepCount
                    } else {
                        totalDeviceSteps // Midnight counter rollover
                    }
                    workoutSteps = delta
                    recordStepTimestamp()
                    updateWorkoutMetrics()
                } else {
                    // Update daily steps in idle metrics
                    _liveMetrics.value = _liveMetrics.value.copy(dailySteps = currentDailySteps)
                }
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (isTracking) {
                    recordStepTimestamp()
                    if (stepCounterSensor == null) {
                        workoutSteps += 1
                        currentDailySteps += 1
                        updateWorkoutMetrics()
                    }
                }
            }
            Sensor.TYPE_PRESSURE -> {
                val millibarsOfPressure = event.values.getOrNull(0) ?: return
                val altitude = SensorManager.getAltitude(
                    SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                    millibarsOfPressure
                ).toDouble()

                if (initialAltitude == 0.0) {
                    initialAltitude = altitude
                    maxAltitude = altitude
                    minAltitude = altitude
                } else {
                    if (altitude > maxAltitude) maxAltitude = altitude
                    if (altitude < minAltitude) minAltitude = altitude
                }
                currentAltitude = altitude
                updateWorkoutMetrics()
            }
        }
    }

    private fun recordStepTimestamp() {
        val now = System.currentTimeMillis()
        stepTimestamps.add(now)
        pruneStepWindow(now)
    }

    private fun todayKey(): String {
        return java.time.LocalDate.now().toString() // yyyy-MM-dd
    }

    private fun pruneStepWindow(now: Long) {
        val cutoff = now - 60000L
        while (stepTimestamps.peek()?.let { it < cutoff } == true) {
            stepTimestamps.poll()
        }
    }

    fun tickTimer(elapsedSeconds: Long) {
        val hr = _liveHeartRate.value.bpm
        val hasRealHr = isHeartRateSensorPresent && hr > 0
        val speed = lastLocation?.speed?.toDouble() ?: 0.0
        val cadence = externalCadenceRpm ?: stepTimestamps.size

        // Real heart rate is the best signal; without HR hardware we fall back to
        // a MET estimate from real measured movement (speed / step cadence).
        val calBurnRatePerSec = if (hasRealHr) {
            CalorieEstimator.fromHeartRate(hr)
        } else {
            CalorieEstimator.fromMovement(speed, cadence, bodyWeightKg)
        }
        totalCaloriesBurned += calBurnRatePerSec
        updateWorkoutMetrics(elapsedSeconds)
    }

    private fun updateWorkoutMetrics(elapsedSeconds: Long = _liveMetrics.value.elapsedSeconds) {
        val now = System.currentTimeMillis()
        pruneStepWindow(now)
        val cadence = externalCadenceRpm ?: stepTimestamps.size

        val elevGain = if (initialAltitude != 0.0 && currentAltitude > initialAltitude) {
            currentAltitude - initialAltitude
        } else 0.0

        _liveMetrics.value = LiveWorkoutMetrics(
            elapsedSeconds = elapsedSeconds,
            heartRate = _liveHeartRate.value.bpm,
            hrAccuracy = _liveHeartRate.value.accuracy,
            isHrAvailable = isHeartRateSensorPresent,
            steps = workoutSteps,
            dailySteps = currentDailySteps,
            cadenceSpm = cadence,
            caloriesKcal = totalCaloriesBurned.toInt(),
            caloriesEstimated = !(isHeartRateSensorPresent && _liveHeartRate.value.bpm > 0),
            distanceMeters = totalDistanceMeters,
            elevationGainMeters = elevGain,
            speedMps = lastLocation?.speed?.toDouble() ?: 0.0,
            gpsStatus = _gpsStatus.value
        )
    }

    override fun onLocationChanged(location: Location) {
        if (!isTracking) return
        if (!isGpsFixAcquired) {
            isGpsFixAcquired = true
            _gpsStatus.value = GpsStatus.READY
        }

        lastLocation?.let { prev ->
            val dist = prev.distanceTo(location)
            if (dist > 1.5) { // filter GPS jitter
                totalDistanceMeters += dist

                // Check distance split alert
                val currentKm = (totalDistanceMeters / 1000.0).toInt()
                if (currentKm > lastSplitUnitIndex && currentKm > 0) {
                    lastSplitUnitIndex = currentKm
                    HapticHelper.vibrateSplitCue(context)
                }
            }
        }
        lastLocation = location

        // Record point for route map and replay
        val point = GpsPoint(
            lat = location.latitude,
            lon = location.longitude,
            alt = location.altitude,
            speed = location.speed.toDouble(),
            timestamp = location.time
        )
        val currentRoute = _recordedRoute.value.toMutableList()
        if (currentRoute.isEmpty() || currentRoute.last().let {
            val loc1 = Location("").apply { latitude = it.lat; longitude = it.lon }
            loc1.distanceTo(location) >= 2.5
        }) {
            currentRoute.add(point)
            _recordedRoute.value = currentRoute
        }

        updateWorkoutMetrics()
    }

    fun getRouteJson(): String {
        val points = _recordedRoute.value
        if (points.isEmpty()) return ""
        val sb = StringBuilder("[")
        points.forEachIndexed { index, p ->
            sb.append("{\"lat\":${p.lat},\"lon\":${p.lon},\"alt\":${p.alt},\"speed\":${p.speed},\"time\":${p.timestamp}}")
            if (index < points.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    companion object {
        fun parseRouteJson(json: String?): List<GpsPoint> {
            if (json.isNullOrBlank() || json == "[]") return emptyList()
            val result = mutableListOf<GpsPoint>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    result.add(
                        GpsPoint(
                            lat = obj.optDouble("lat", 0.0),
                            lon = obj.optDouble("lon", 0.0),
                            alt = obj.optDouble("alt", 0.0),
                            speed = obj.optDouble("speed", 0.0),
                            timestamp = obj.optLong("time", 0L)
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("SensorTracker", "Failed to parse route JSON", e)
            }
            return result
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_HEART_RATE) {
            _liveHeartRate.value = _liveHeartRate.value.copy(accuracy = accuracy)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER && isTracking) {
            _gpsStatus.value = GpsStatus.SEARCHING
        }
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER && isTracking) {
            _gpsStatus.value = GpsStatus.UNAVAILABLE
        }
    }
}
