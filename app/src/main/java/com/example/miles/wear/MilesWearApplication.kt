package com.example.miles.wear

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.miles.wear.data.local.MilesDatabase
import com.example.miles.wear.data.repository.MilesRepository
import com.example.miles.wear.network.PhoneMessagingManager
import com.example.miles.wear.sensor.BleSensorManager
import com.example.miles.wear.sensor.CalorieEstimator
import com.example.miles.wear.sensor.SensorTracker
import com.example.miles.wear.service.MoveReminderReceiver
import com.example.miles.wear.service.WorkoutTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MilesWearApplication : Application() {

    lateinit var database: MilesDatabase
        private set

    lateinit var repository: MilesRepository
        private set

    lateinit var sensorTracker: SensorTracker
        private set

    lateinit var bleSensorManager: BleSensorManager
        private set

    lateinit var phoneMessagingManager: PhoneMessagingManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        lateinit var instance: MilesWearApplication
            private set
        const val NOTIFICATION_CHANNEL_ID = "miles_workout_channel"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = MilesDatabase.getInstance(this)
        repository = MilesRepository(
            this,
            database.queueDao(),
            database.workoutSessionDao(),
            database.savedPinDao(),
            database.dayStatsDao(),
            database.petDao(),
            database.savedRouteDao(),
            database.trainingProgressDao()
        )
        sensorTracker = SensorTracker(this)
        bleSensorManager = BleSensorManager(this)
        phoneMessagingManager = PhoneMessagingManager(this, repository).apply {
            initialize()
        }

        createNotificationChannel()
        MoveReminderReceiver.ensureChannel(this)
        // Re-arm move reminders if the setting is enabled (survives restarts).
        com.example.miles.wear.engine.MoveReminderManager.applySetting(this)
        // Hands-free nav voice ready when needed.
        com.example.miles.wear.engine.NavigationVoice.init(this)

        feedBleIntoSensors()
        captureRestingHr()
        syncBodyWeight()
        recordEverydayCalories()
    }

    /** Keep the sensor engine's calorie estimates on the user's real body weight. */
    private fun syncBodyWeight() {
        appScope.launch {
            repository.settings.collect { s -> sensorTracker.bodyWeightKg = s.bodyWeightKg }
        }
    }

    /**
     * Everyday active calories from the REAL daily step count, so the dashboard
     * isn't stuck at 0 until a workout is finished. Skipped while a workout is
     * running (the workout engine counts those calories itself), so nothing is
     * ever double counted.
     */
    private fun recordEverydayCalories() {
        appScope.launch {
            var lastSteps = sensorTracker.liveMetrics.value.dailySteps
            while (isActive) {
                delay(60_000L)
                val steps = sensorTracker.liveMetrics.value.dailySteps
                val deltaSteps = steps - lastSteps
                lastSteps = steps
                if (deltaSteps > 0 && !WorkoutTrackingService.isWorkoutActive) {
                    val kcal = CalorieEstimator.dailyActiveFromSteps(
                        deltaSteps,
                        repository.settings.value.bodyWeightKg
                    )
                    if (kcal > 0) repository.recordDayActivity(0.0, 0, 0, kcal)
                }
            }
        }
    }

    /** External BLE strap/cadence values become the live workout signals. */
    private fun feedBleIntoSensors() {
        appScope.launch {
            bleSensorManager.state.collect { state ->
                if (state is BleSensorManager.ConnectionState.Connected) {
                    sensorTracker.setExternalHeartRate(state.hrBpm)
                    sensorTracker.setExternalCadence(state.cadenceRpm)
                } else {
                    sensorTracker.setExternalHeartRate(null)
                    sensorTracker.setExternalCadence(null)
                }
            }
        }
    }

    /** Today's resting HR = real lowest sustained idle HR sample (1-min min). */
    private fun captureRestingHr() {
        appScope.launch {
            var minuteMin = 0
            var ticks = 0
            while (isActive) {
                val bpm = sensorTracker.liveHeartRate.value.bpm
                if (bpm in 40..150 && !sensorTracker.liveHeartRate.value.isFromExternal) {
                    minuteMin = if (minuteMin == 0) bpm else minOf(minuteMin, bpm)
                }
                delay(1000L)
                ticks++
                if (ticks >= 60) {
                    ticks = 0
                    if (minuteMin > 0) repository.recordRestingBpm(minuteMin)
                    minuteMin = 0
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
