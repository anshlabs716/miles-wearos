package com.example.miles.wear.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.example.miles.wear.MainActivity
import com.example.miles.wear.MilesWearApplication
import com.example.miles.wear.R
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.GpsStatus
import com.example.miles.wear.data.model.GoalType
import com.example.miles.wear.data.model.HeartRateZone
import com.example.miles.wear.data.model.IntervalPhase
import com.example.miles.wear.data.model.WorkoutMode
import com.example.miles.wear.data.model.WorkoutPlan
import com.example.miles.wear.data.model.WorkoutState
import com.example.miles.wear.data.model.WorkoutType
import com.example.miles.wear.engine.WorkoutPlanHub
import com.example.miles.wear.util.HapticHelper
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

class WorkoutTrackingService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    private val repository get() = MilesWearApplication.instance.repository
    private val sensorTracker get() = MilesWearApplication.instance.sensorTracker
    private val phoneMessaging get() = MilesWearApplication.instance.phoneMessagingManager

    private val _workoutState = MutableStateFlow(WorkoutState.IDLE)
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    private val _currentWorkoutType = MutableStateFlow(WorkoutType.RUN)
    val currentWorkoutType: StateFlow<WorkoutType> = _currentWorkoutType.asStateFlow()

    // Goal / interval plan (free by default)
    private val _currentPlan = MutableStateFlow(WorkoutPlan())
    val currentPlan: StateFlow<WorkoutPlan> = _currentPlan.asStateFlow()

    private var goalAlerted = false

    private var sessionStartTime = 0L
    private var elapsedSeconds = 0L
    private var lastVibratedKilometer = 0
    private var currentSessionId: Long = 0
    private var autoPauseJob: Job? = null
    private var slowTicks = 0
    private var fastTicks = 0

    inner class LocalBinder : Binder() {
        fun getService(): WorkoutTrackingService = this@WorkoutTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_FINISH = "ACTION_FINISH"
        const val ACTION_DISCARD = "ACTION_DISCARD"
        const val ACTION_START_MIRRORED = "ACTION_START_MIRRORED"

        const val EXTRA_WORKOUT_TYPE = "EXTRA_WORKOUT_TYPE"
        const val EXTRA_PLAN = "EXTRA_PLAN"
        private const val NOTIFICATION_ID = 1001

        /** True while a real workout is running — suppresses move reminders. */
        @Volatile
        var isWorkoutActive = false

        fun startWorkoutIntent(context: Context, type: WorkoutType, plan: WorkoutPlan = WorkoutPlan()): Intent {
            return Intent(context, WorkoutTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WORKOUT_TYPE, type.name)
                putExtra(EXTRA_PLAN, plan.encode())
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val typeName = intent.getStringExtra(EXTRA_WORKOUT_TYPE) ?: WorkoutType.RUN.name
                val type = WorkoutType.fromString(typeName)
                val plan = WorkoutPlan.decode(intent.getStringExtra(EXTRA_PLAN))
                startWorkout(type, isMirrored = false, plan = plan)
            }
            ACTION_START_MIRRORED -> {
                val typeName = intent.getStringExtra(EXTRA_WORKOUT_TYPE) ?: WorkoutType.RUN.name
                val type = WorkoutType.fromString(typeName)
                startWorkout(type, isMirrored = true)
            }
            ACTION_PAUSE -> pauseWorkout()
            ACTION_RESUME -> resumeWorkout()
            ACTION_FINISH -> finishWorkout()
            ACTION_DISCARD -> discardWorkout()
        }
        return START_STICKY
    }

    fun startWorkout(type: WorkoutType, isMirrored: Boolean = false, plan: WorkoutPlan = WorkoutPlan()) {
        _currentWorkoutType.value = type
        _currentPlan.value = plan
        WorkoutPlanHub.reset(0f, null)
        goalAlerted = false
        isWorkoutActive = true
        _workoutState.value = if (isMirrored) WorkoutState.MIRRORED else WorkoutState.RUNNING
        sessionStartTime = System.currentTimeMillis()
        elapsedSeconds = 0L
        lastVibratedKilometer = 0

        // Haptic feedback for workout start
        if (repository.settings.value.workoutStartHaptic) {
            vibratePattern(longArrayOf(0, 150, 100, 200))
        }

        val isIndoor = (type == WorkoutType.OTHER)
        sensorTracker.startTracking(isIndoor = isIndoor)

        val notification = buildOngoingNotification("00:00", 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val typeFlags = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            startForeground(NOTIFICATION_ID, notification, typeFlags)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Save session placeholder in Room
        scope.launch {
            val entity = WorkoutSessionEntity(
                workoutType = type.displayName,
                startTime = sessionStartTime
            )
            currentSessionId = repository.saveWorkoutSession(entity)
        }

        startTicker()
        startAutoPauseWatcher()
    }

    fun pauseWorkout() {
        if (_workoutState.value == WorkoutState.RUNNING || _workoutState.value == WorkoutState.MIRRORED) {
            _workoutState.value = WorkoutState.PAUSED
            if (repository.settings.value.workoutPauseResumeHaptic) {
                vibratePattern(longArrayOf(0, 300))
            }
            tickerJob?.cancel()
        }
    }

    fun resumeWorkout() {
        if (_workoutState.value == WorkoutState.PAUSED) {
            _workoutState.value = WorkoutState.RUNNING
            if (repository.settings.value.workoutPauseResumeHaptic) {
                vibratePattern(longArrayOf(0, 150, 100, 150))
            }
            startTicker()
        }
    }

    fun finishWorkout() {
        _workoutState.value = WorkoutState.COMPLETED
        tickerJob?.cancel()
        autoPauseJob?.cancel()
        sensorTracker.stopTracking()
        isWorkoutActive = false

        // Haptic celebratory vibration (toggleable in Settings)
        if (repository.settings.value.workoutFinishHaptic) {
            vibratePattern(longArrayOf(0, 200, 100, 200, 100, 400))
        }

        val finalMetrics = sensorTracker.liveMetrics.value
        // Real route + heart-rate statistics captured from the live tracker
        val routeJson = sensorTracker.getRouteJson()
        val hrAvg = sensorTracker.avgBpm
        val hrMax = sensorTracker.maxBpm
        scope.launch {
            if (currentSessionId > 0) {
                val updated = WorkoutSessionEntity(
                    id = currentSessionId,
                    workoutType = _currentWorkoutType.value.displayName,
                    startTime = sessionStartTime,
                    endTime = System.currentTimeMillis(),
                    durationSeconds = elapsedSeconds,
                    totalSteps = finalMetrics.steps,
                    avgBpm = if (hrAvg > 0) hrAvg else finalMetrics.heartRate,
                    maxBpm = if (hrMax > 0) hrMax else finalMetrics.heartRate,
                    caloriesKcal = finalMetrics.caloriesKcal,
                    distanceMeters = finalMetrics.distanceMeters,
                    elevationGainMeters = finalMetrics.elevationGainMeters,
                    isSyncedToPhone = false,
                    routeGeoJson = routeJson
                )
                repository.updateWorkoutSession(updated)
                // Accumulate today's real totals (drives step/activity streaks)
                repository.recordDayActivity(
                    distanceMeters = finalMetrics.distanceMeters,
                    steps = finalMetrics.steps,
                    activeSeconds = elapsedSeconds,
                    calories = finalMetrics.caloriesKcal
                )
                // A training-plan workout launched the session: mark that day ✓
                repository.completePendingTrainingDayIfAny()
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun discardWorkout() {
        _workoutState.value = WorkoutState.DISCARDED
        tickerJob?.cancel()
        autoPauseJob?.cancel()
        sensorTracker.stopTracking()
        isWorkoutActive = false

        scope.launch {
            if (currentSessionId > 0) {
                repository.deleteWorkoutSession(currentSessionId)
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Auto-pause/resume (toggleable in Settings): pauses when GPS shows the
     * user is stationary (>15s under 0.5 m/s), resumes when moving again.
     */
    private fun startAutoPauseWatcher() {
        autoPauseJob?.cancel()
        autoPauseJob = scope.launch {
            while (isActive) {
                val prefs = repository.settings.value
                val isOutdoor = _currentWorkoutType.value != WorkoutType.OTHER &&
                    _currentWorkoutType.value != WorkoutType.GENERAL
                if (prefs.autoPauseEnabled && isOutdoor) {
                    val m = sensorTracker.liveMetrics.value
                    when (_workoutState.value) {
                        WorkoutState.RUNNING -> {
                            if (m.gpsStatus == GpsStatus.READY && m.speedMps < 0.5) {
                                slowTicks++
                                if (slowTicks >= 3) { // ~15 seconds stationary
                                    slowTicks = 0
                                    pauseWorkout()
                                }
                            } else {
                                slowTicks = 0
                            }
                        }

                        WorkoutState.PAUSED -> {
                            if (m.speedMps > 1.5) {
                                fastTicks++
                                if (fastTicks >= 2) {
                                    fastTicks = 0
                                    resumeWorkout()
                                }
                            } else {
                                fastTicks = 0
                            }
                        }

                        else -> {
                            slowTicks = 0
                            fastTicks = 0
                        }
                    }
                } else {
                    slowTicks = 0
                    fastTicks = 0
                }
                delay(5000L)
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                elapsedSeconds++
                sensorTracker.tickTimer(elapsedSeconds)

                val metrics = sensorTracker.liveMetrics.value
                val hr = sensorTracker.liveHeartRate.value

                // Broadcast live streams to phone
                if (hr.bpm > 0) {
                    phoneMessaging.broadcastHeartRate(hr.bpm, hr.accuracy)
                }
                if (metrics.cadenceSpm > 0) {
                    phoneMessaging.broadcastCadence(metrics.cadenceSpm)
                }

                // Check kilometer splits for vibration cue
                val currentKm = (metrics.distanceMeters / 1000.0).toInt()
                if (currentKm > lastVibratedKilometer && currentKm > 0) {
                    lastVibratedKilometer = currentKm
                    if (repository.settings.value.splitHaptic) {
                        vibratePattern(longArrayOf(0, 200, 150, 200, 150, 300))
                    }
                }

                // Check HR zone alerts
                if (hr.bpm >= HeartRateZone.MAX.minBpm && repository.settings.value.hapticAlertsEnabled) {
                    vibratePattern(longArrayOf(0, 100, 80, 100))
                }

                // Goal / interval plan progress (real metric values from the tracker)
                evaluatePlan(metrics)

                // Update notification
                val timeStr = formatDuration(elapsedSeconds)
                val notification = buildOngoingNotification(timeStr, hr.bpm)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun evaluatePlan(metrics: com.example.miles.wear.data.model.LiveWorkoutMetrics) {
        val plan = _currentPlan.value
        when (plan.mode) {
            WorkoutMode.GOAL -> {
                val progress = when (plan.goalType) {
                    GoalType.DISTANCE -> if (plan.goalValue > 0) (metrics.distanceMeters / 1000.0 / plan.goalValue).toFloat() else 1f
                    GoalType.DURATION -> if (plan.goalValue > 0) (metrics.elapsedSeconds / 60.0 / plan.goalValue).toFloat() else 1f
                    GoalType.CALORIES -> if (plan.goalValue > 0) (metrics.caloriesKcal / plan.goalValue).toFloat() else 1f
                }
                WorkoutPlanHub.setGoalProgress(progress)
                if (progress >= 1f && !goalAlerted) {
                    goalAlerted = true
                    vibratePattern(longArrayOf(0, 200, 120, 200, 120, 400))
                    val goalName = plan.goalType.title
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(
                        NOTIFICATION_ID,
                        buildOngoingNotification(formatDuration(elapsedSeconds), 0, note = "$goalName goal reached! 🎉")
                    )
                }
            }

            WorkoutMode.INTERVAL -> {
                val phase = computeIntervalPhase(elapsedSeconds, plan)
                if (phase != WorkoutPlanHub.intervalPhase.value) {
                    WorkoutPlanHub.setIntervalPhase(phase)
                    if (phase == null) {
                        // Interval set complete
                        vibratePattern(longArrayOf(0, 200, 120, 200, 120, 400))
                    } else {
                        // Phase change cue (work starts = strong, rest = light)
                        if (phase.isWork) {
                            vibratePattern(longArrayOf(0, 120, 80, 120))
                        } else {
                            vibratePattern(longArrayOf(0, 80))
                        }
                    }
                }
            }

            WorkoutMode.FREE -> Unit
        }
    }

    /** Pure interval timeline: work(i) then rest(i) for each of `reps`, then a cooldown. */
    private fun computeIntervalPhase(elapsedSec: Long, plan: WorkoutPlan): IntervalPhase? {
        var rem = elapsedSec
        for (set in 1..plan.repetitions) {
            if (rem < plan.workSeconds) {
                return IntervalPhase(true, rem, plan.workSeconds.toLong(), set, plan.repetitions)
            }
            rem -= plan.workSeconds
            if (set < plan.repetitions) {
                if (rem < plan.recoverySeconds) {
                    return IntervalPhase(false, rem, plan.recoverySeconds.toLong(), set, plan.repetitions)
                }
                rem -= plan.recoverySeconds
            }
        }
        // Cooldown after the final work set
        if (rem < plan.recoverySeconds) {
            return IntervalPhase(false, rem, plan.recoverySeconds.toLong(), plan.repetitions, plan.repetitions, isCooldown = true)
        }
        return null
    }

    private fun formatDuration(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun buildOngoingNotification(timeStr: String, bpm: Int, note: String = ""): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val metrics = sensorTracker.liveMetrics.value
        val distanceStr = String.format("%.2f km", metrics.distanceMeters / 1000.0)

        val builder = NotificationCompat.Builder(this, MilesWearApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MILES • ${_currentWorkoutType.value.displayName}")
            .setContentText(if (note.isNotEmpty()) note else "$timeStr • $distanceStr" + if (bpm > 0) " • $bpm BPM" else "")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val ongoingActivity = OngoingActivity.Builder(this, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_launcher_foreground)
            .setTouchIntent(pendingIntent)
            .setStatus(
                Status.Builder()
                    .addTemplate("${_currentWorkoutType.value.displayName} $timeStr • $distanceStr")
                    .build()
            )
            .build()

        ongoingActivity.apply(this)
        return builder.build()
    }

    private fun vibratePattern(timings: LongArray) {
        if (!repository.settings.value.hapticAlertsEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(timings, -1)
                    it.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(timings, -1)
                }
            }
        } catch (e: Exception) {
            // safely ignored
        }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        sensorTracker.stopTracking()
        isWorkoutActive = false
        super.onDestroy()
    }
}
