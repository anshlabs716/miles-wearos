package com.example.miles.wear.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueItem(item: QueueItemEntity): Long

    @Query("SELECT * FROM telemetry_queue WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedItems(limit: Int = 100): List<QueueItemEntity>

    @Query("SELECT COUNT(*) FROM telemetry_queue WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Query("UPDATE telemetry_queue SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM telemetry_queue WHERE isSynced = 1")
    suspend fun clearSynced()
}

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSessionEntity?

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}
