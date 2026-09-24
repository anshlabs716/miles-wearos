package com.example.miles.wear.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_queue")
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "HR", "CADENCE", "METRICS", "CONTROL"
    val path: String, // "/miles/sensors/hr", etc.
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutType: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val durationSeconds: Long = 0L,
    val totalSteps: Int = 0,
    val avgBpm: Int = 0,
    val maxBpm: Int = 0,
    val caloriesKcal: Int = 0,
    val distanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val isSyncedToPhone: Boolean = false,
    val routeGeoJson: String = ""
)

@Entity(tableName = "saved_pins")
data class SavedPinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** One row per calendar day (yyyy-MM-dd): real daily totals for streaks. */
@Entity(tableName = "day_stats")
data class DayStatsEntity(
    @PrimaryKey
    val dateKey: String,             // yyyy-MM-dd (device local time)
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val activeSeconds: Long = 0L,
    val calories: Int = 0
)
