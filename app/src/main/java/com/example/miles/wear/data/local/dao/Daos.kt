package com.example.miles.wear.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.SavedPinEntity
import com.example.miles.wear.data.local.entity.DayStatsEntity
import com.example.miles.wear.data.local.entity.PetEntity
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

    @Query("SELECT * FROM workout_sessions WHERE startTime >= :startOfDayTimestamp ORDER BY startTime DESC")
    fun getTodaySessions(startOfDayTimestamp: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions")
    suspend fun getAllSessionsNow(): List<WorkoutSessionEntity>

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}

@Dao
interface SavedPinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: SavedPinEntity): Long

    @Query("SELECT * FROM saved_pins ORDER BY createdAt DESC")
    fun getAllPins(): Flow<List<SavedPinEntity>>

    @Query("SELECT * FROM saved_pins ORDER BY createdAt DESC")
    suspend fun getAllPinsNow(): List<SavedPinEntity>

    @Query("DELETE FROM saved_pins WHERE id = :id")
    suspend fun deletePin(id: Long)
}

@Dao
interface DayStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(stats: DayStatsEntity)

    @Query("SELECT * FROM day_stats ORDER BY dateKey")
    suspend fun getAllDays(): List<DayStatsEntity>

    @Query("SELECT * FROM day_stats WHERE dateKey = :key")
    suspend fun getDay(key: String): DayStatsEntity?
}

@Dao
interface PetDao {
    @Query("SELECT * FROM pet LIMIT 1")
    suspend fun getPet(): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePet(pet: PetEntity)
}
