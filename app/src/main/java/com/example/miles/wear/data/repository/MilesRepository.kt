package com.example.miles.wear.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.miles.wear.data.local.dao.QueueDao
import com.example.miles.wear.data.local.dao.WorkoutSessionDao
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.DistanceUnit
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

        return WearSettings(
            unit = unit,
            keepScreenOn = keepScreenOn,
            gpsEnabled = gpsEnabled,
            hrEnabled = hrEnabled,
            stepTrackingEnabled = stepTrackingEnabled
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

    suspend fun deleteWorkoutSession(id: Long) {
        withContext(Dispatchers.IO) {
            sessionDao.deleteSession(id)
        }
    }
}
