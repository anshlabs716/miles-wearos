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
import com.example.miles.wear.data.model.HeartRateZone
import com.example.miles.wear.data.model.LiveHeartRate
import com.example.miles.wear.data.model.LiveWorkoutMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

class SensorTracker(private val context: Context) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Sensors
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    // Reactive State
    private val _liveHeartRate = MutableStateFlow(LiveHeartRate())
    val liveHeartRate: StateFlow<LiveHeartRate> = _liveHeartRate.asStateFlow()

    private val _liveMetrics = MutableStateFlow(LiveWorkoutMetrics())
    val liveMetrics: StateFlow<LiveWorkoutMetrics> = _liveMetrics.asStateFlow()

    private val _isLowPowerMode = MutableStateFlow(false)
    val isLowPowerMode: StateFlow<Boolean> = _isLowPowerMode.asStateFlow()

    private val _batteryPercent = MutableStateFlow(100)
    val batteryPercent: StateFlow<Int> = _batteryPercent.asStateFlow()

    // Internal workout telemetry accumulation
    private var isTracking = false
    private var startTimestamp = 0L
    private var initialStepCount = -1
    private var workoutSteps = 0
    private var initialAltitude = 0.0
    private var currentAltitude = 0.0
    private var maxAltitude = 0.0
    private var minAltitude = 0.0
    private var totalCaloriesBurned = 0.0
    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0.0

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
    }

    fun startTracking() {
        isTracking = true
        startTimestamp = System.currentTimeMillis()
        initialStepCount = -1
        workoutSteps = 0
        totalDistanceMeters = 0.0
        totalCaloriesBurned = 0.0
        lastLocation = null
        initialAltitude = 0.0
        currentAltitude = 0.0
        stepTimestamps.clear()

        registerSensors()
        requestLocationUpdates()
    }

    fun stopTracking() {
        isTracking = false
        unregisterSensors()
        removeLocationUpdates()
    }

    private fun registerSensors() {
        val delay = if (_isLowPowerMode.value) {
            SensorManager.SENSOR_DELAY_NORMAL
        } else {
            SensorManager.SENSOR_DELAY_UI
        }

        heartRateSensor?.let {
            sensorManager.registerListener(this, it, delay)
        }
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, delay)
        }
        stepDetectorSensor?.let {
            sensorManager.registerListener(this, it, delay)
        }
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
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
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                val minTime = if (_isLowPowerMode.value) 10000L else 2000L
                val minDistance = if (_isLowPowerMode.value) 10f else 2f
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minTime,
                    minDistance,
                    this
                )
            }
        } catch (e: SecurityException) {
            Log.w("SensorTracker", "Location permission not granted yet for GPS", e)
        } catch (e: Exception) {
            Log.e("SensorTracker", "Error starting GPS", e)
        }
    }

    private fun removeLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            Log.e("SensorTracker", "Error removing GPS updates", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                val rawBpm = event.values.getOrNull(0)?.toInt() ?: 0
                if (rawBpm > 0) {
                    val reading = LiveHeartRate(
                        bpm = rawBpm,
                        accuracy = event.accuracy,
                        timestamp = System.currentTimeMillis()
                    )
                    _liveHeartRate.value = reading
                    updateWorkoutMetrics()
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val totalDeviceSteps = event.values.getOrNull(0)?.toInt() ?: 0
                if (initialStepCount < 0) {
                    initialStepCount = totalDeviceSteps
                }
                // Handle midnight counter reset or delta
                val delta = if (totalDeviceSteps >= initialStepCount) {
                    totalDeviceSteps - initialStepCount
                } else {
                    totalDeviceSteps // Midnight reset happened
                }
                workoutSteps = delta
                recordStepTimestamp()
                updateWorkoutMetrics()
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                recordStepTimestamp()
                if (stepCounterSensor == null) {
                    workoutSteps += 1
                    updateWorkoutMetrics()
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

    private fun pruneStepWindow(now: Long) {
        val cutoff = now - 60000L
        while (stepTimestamps.peek()?.let { it < cutoff } == true) {
            stepTimestamps.poll()
        }
    }

    fun tickTimer(elapsedSeconds: Long) {
        // Accumulate active calorie calculation every second based on heart rate zone
        val currentBpm = _liveHeartRate.value.bpm
        val calBurnRatePerSec = when (HeartRateZone.fromBpm(currentBpm)) {
            HeartRateZone.RESTING -> 0.02   // ~1.2 kcal/min
            HeartRateZone.WARMUP -> 0.08    // ~4.8 kcal/min
            HeartRateZone.AEROBIC -> 0.15   // ~9.0 kcal/min
            HeartRateZone.THRESHOLD -> 0.22 // ~13.2 kcal/min
            HeartRateZone.ANAEROBIC -> 0.28 // ~16.8 kcal/min
            HeartRateZone.MAX -> 0.35       // ~21 kcal/min
        }
        totalCaloriesBurned += calBurnRatePerSec
        updateWorkoutMetrics(elapsedSeconds)
    }

    private fun updateWorkoutMetrics(elapsedSeconds: Long = _liveMetrics.value.elapsedSeconds) {
        val now = System.currentTimeMillis()
        pruneStepWindow(now)
        val cadence = stepTimestamps.size // Steps in the last 60 seconds = SPM

        val elevGain = if (initialAltitude != 0.0 && currentAltitude > initialAltitude) {
            currentAltitude - initialAltitude
        } else 0.0

        _liveMetrics.value = LiveWorkoutMetrics(
            elapsedSeconds = elapsedSeconds,
            heartRate = _liveHeartRate.value.bpm,
            hrAccuracy = _liveHeartRate.value.accuracy,
            steps = workoutSteps,
            cadenceSpm = cadence,
            caloriesKcal = totalCaloriesBurned.toInt(),
            distanceMeters = totalDistanceMeters,
            elevationGainMeters = elevGain,
            speedMps = lastLocation?.speed?.toDouble() ?: 0.0
        )
    }

    override fun onLocationChanged(location: Location) {
        if (!isTracking) return
        lastLocation?.let { prev ->
            val dist = prev.distanceTo(location)
            if (dist > 1.0) { // filter GPS jitter
                totalDistanceMeters += dist
            }
        }
        lastLocation = location
        updateWorkoutMetrics()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_HEART_RATE) {
            _liveHeartRate.value = _liveHeartRate.value.copy(accuracy = accuracy)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
