package com.example.miles.wear.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.miles.wear.data.local.dao.QueueDao
import com.example.miles.wear.data.local.dao.WorkoutSessionDao
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.DistanceUnit
import com.example.miles.wear.data.model.HudLayoutMode
import com.example.miles.wear.data.model.PrimaryMetricType
import com.example.miles.wear.data.model.ThemeAccent
import com.example.miles.wear.data.model.WearSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MilesRepository(
    context: Context,
    private val queueDao: QueueDao,
    private val sessionDao: WorkoutSessionDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("miles_wear_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettingsFromPrefs())
    val settings: StateFlow<WearSettings> = _settings.asStateFlow()

    val unsyncedCount: Flow<Int> = queueDao.getUnsyncedCount()
    val allSessions: Flow<List<WorkoutSessionEntity>> = sessionDao.getAllSessions()

    private fun loadSettingsFromPrefs(): WearSettings {
        val unitName = prefs.getString("unit", DistanceUnit.METRIC.name) ?: DistanceUnit.METRIC.name
        val unit = try { DistanceUnit.valueOf(unitName) } catch (e: Exception) { DistanceUnit.METRIC }
        val keepScreenOn = prefs.getBoolean("keep_screen_on", true)
        val gpsEnabled = prefs.getBoolean("gps_enabled", true)
        val hrEnabled = prefs.getBoolean("hr_enabled", true)
        val stepTrackingEnabled = prefs.getBoolean("step_tracking_enabled", true)
        val autoPauseEnabled = prefs.getBoolean("auto_pause_enabled", false)
        val hapticAlertsEnabled = prefs.getBoolean("haptic_alerts_enabled", true)
        val batterySaverEnabled = prefs.getBoolean("battery_saver_enabled", false)
        val themeAccentName = prefs.getString("theme_accent", ThemeAccent.CYAN.name) ?: ThemeAccent.CYAN.name
        val themeAccent = try { ThemeAccent.valueOf(themeAccentName) } catch (e: Exception) { ThemeAccent.CYAN }
        val primaryMetricName = prefs.getString("primary_metric", PrimaryMetricType.DISTANCE.name) ?: PrimaryMetricType.DISTANCE.name
        val primaryMetric = try { PrimaryMetricType.valueOf(primaryMetricName) } catch (e: Exception) { PrimaryMetricType.DISTANCE }
        val hudLayoutName = prefs.getString("hud_layout", HudLayoutMode.STANDARD.name) ?: HudLayoutMode.STANDARD.name
        val hudLayout = try { HudLayoutMode.valueOf(hudLayoutName) } catch (e: Exception) { HudLayoutMode.STANDARD }
        val showGpsMapInHud = prefs.getBoolean("show_gps_map_in_hud", true)
        val showCadenceInHud = prefs.getBoolean("show_cadence_in_hud", true)
        val showHeartRateZoneRing = prefs.getBoolean("show_hr_ring", true)
        val showBatteryInDashboard = prefs.getBoolean("show_battery_dashboard", true)
        val showGpsInDashboard = prefs.getBoolean("show_gps_dashboard", true)
        val showRecentActivitiesInDashboard = prefs.getBoolean("show_recent_dashboard", true)
        val compactCards = prefs.getBoolean("compact_cards", false)
        val workoutStartHaptic = prefs.getBoolean("workout_start_haptic", true)
        val workoutPauseResumeHaptic = prefs.getBoolean("workout_pause_haptic", true)
        val workoutFinishHaptic = prefs.getBoolean("workout_finish_haptic", true)
        val splitHaptic = prefs.getBoolean("split_haptic", true)
        val highContrastText = prefs.getBoolean("high_contrast_text", false)
        val stepGoal = prefs.getInt("step_goal", 10000)

        return WearSettings(
            unit = unit,
            keepScreenOn = keepScreenOn,
            gpsEnabled = gpsEnabled,
            hrEnabled = hrEnabled,
            stepTrackingEnabled = stepTrackingEnabled,
            autoPauseEnabled = autoPauseEnabled,
            hapticAlertsEnabled = hapticAlertsEnabled,
            batterySaverEnabled = batterySaverEnabled,
            themeAccent = themeAccent,
            primaryMetric = primaryMetric,
            hudLayout = hudLayout,
            showGpsMapInHud = showGpsMapInHud,
            showCadenceInHud = showCadenceInHud,
            showHeartRateZoneRing = showHeartRateZoneRing,
            showBatteryInDashboard = showBatteryInDashboard,
            showGpsInDashboard = showGpsInDashboard,
            showRecentActivitiesInDashboard = showRecentActivitiesInDashboard,
            compactCards = compactCards,
            workoutStartHaptic = workoutStartHaptic,
            workoutPauseResumeHaptic = workoutPauseResumeHaptic,
            workoutFinishHaptic = workoutFinishHaptic,
            splitHaptic = splitHaptic,
            highContrastText = highContrastText,
            stepGoal = stepGoal
        )
    }

    fun updateSettings(newSettings: WearSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putString("unit", newSettings.unit.name)
            .putBoolean("keep_screen_on", newSettings.keepScreenOn)
            .putBoolean("gps_enabled", newSettings.gpsEnabled)
            .putBoolean("hr_enabled", newSettings.hrEnabled)
            .putBoolean("step_tracking_enabled", newSettings.stepTrackingEnabled)
            .putBoolean("auto_pause_enabled", newSettings.autoPauseEnabled)
            .putBoolean("haptic_alerts_enabled", newSettings.hapticAlertsEnabled)
            .putBoolean("battery_saver_enabled", newSettings.batterySaverEnabled)
            .putString("theme_accent", newSettings.themeAccent.name)
            .putString("primary_metric", newSettings.primaryMetric.name)
            .putString("hud_layout", newSettings.hudLayout.name)
            .putBoolean("show_gps_map_in_hud", newSettings.showGpsMapInHud)
            .putBoolean("show_cadence_in_hud", newSettings.showCadenceInHud)
            .putBoolean("show_hr_ring", newSettings.showHeartRateZoneRing)
            .putBoolean("show_battery_dashboard", newSettings.showBatteryInDashboard)
            .putBoolean("show_gps_dashboard", newSettings.showGpsInDashboard)
            .putBoolean("show_recent_dashboard", newSettings.showRecentActivitiesInDashboard)
            .putBoolean("compact_cards", newSettings.compactCards)
            .putBoolean("workout_start_haptic", newSettings.workoutStartHaptic)
            .putBoolean("workout_pause_haptic", newSettings.workoutPauseResumeHaptic)
            .putBoolean("workout_finish_haptic", newSettings.workoutFinishHaptic)
            .putBoolean("split_haptic", newSettings.splitHaptic)
            .putBoolean("high_contrast_text", newSettings.highContrastText)
            .putInt("step_goal", newSettings.stepGoal)
            .apply()
    }

    suspend fun enqueueTelemetry(type: String, path: String, payloadJson: String) {
        withContext(Dispatchers.IO) {
            val item = QueueItemEntity(
                type = type,
                path = path,
                payloadJson = payloadJson,
                timestamp = System.currentTimeMillis(),
                isSynced = false
            )
            queueDao.enqueueItem(item)
        }
    }

    suspend fun getPendingBatch(limit: Int = 50): List<QueueItemEntity> {
        return withContext(Dispatchers.IO) {
            queueDao.getUnsyncedItems(limit)
        }
    }

    suspend fun markBatchSynced(ids: List<Long>) {
        withContext(Dispatchers.IO) {
            queueDao.markAsSynced(ids)
            queueDao.clearSynced()
        }
    }

    suspend fun saveWorkoutSession(session: WorkoutSessionEntity): Long {
        return withContext(Dispatchers.IO) {
            sessionDao.insertSession(session)
        }
    }

    suspend fun updateWorkoutSession(session: WorkoutSessionEntity) {
        withContext(Dispatchers.IO) {
            sessionDao.updateSession(session)
        }
    }

    suspend fun getLatestWorkout(): WorkoutSessionEntity? {
        return withContext(Dispatchers.IO) {
            sessionDao.getLatestSession()
        }
    }

    suspend fun getSessionById(id: Long): WorkoutSessionEntity? {
        return withContext(Dispatchers.IO) {
            sessionDao.getSessionById(id)
        }
    }

    fun getTodaySessions(): Flow<List<WorkoutSessionEntity>> {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return sessionDao.getTodaySessions(calendar.timeInMillis)
    }

    suspend fun deleteWorkoutSession(id: Long) {
        withContext(Dispatchers.IO) {
            sessionDao.deleteSession(id)
        }
    }
}
