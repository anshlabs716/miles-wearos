package com.example.miles.wear.data.repository

import com.example.miles.wear.data.local.dao.QueueDao
import com.example.miles.wear.data.local.dao.WorkoutSessionDao
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MilesRepository(
    private val queueDao: QueueDao,
    private val sessionDao: WorkoutSessionDao
) {
    val unsyncedCount: Flow<Int> = queueDao.getUnsyncedCount()
    val allSessions: Flow<List<WorkoutSessionEntity>> = sessionDao.getAllSessions()

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
}
