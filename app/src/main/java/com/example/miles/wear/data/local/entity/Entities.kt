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

/** The user's adopted fitness companion (single row). */
@Entity(tableName = "pet")
data class PetEntity(
    @PrimaryKey
    val id: Long = 1,
    val petType: String = "DOG",
    val petName: String = "Miles"
)

/** A route saved by the user (from navigation or pins) — real points only. */
@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val pointsJson: String,        // JSON array of GpsPoint
    val distanceMeters: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

/** Single-row progress for the active progressive training plan. */
@Entity(tableName = "training_progress")
data class TrainingProgressEntity(
    @PrimaryKey
    val id: Int = 1,
    val planKey: String = "",
    val startedEpochDay: Long = 0,
    val completedJson: String = "",      // ["w1d1","w3d2",...]
    val finishedAtEpochDay: Long = 0
)
