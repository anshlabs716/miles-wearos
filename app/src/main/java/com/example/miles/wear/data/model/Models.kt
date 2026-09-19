package com.example.miles.wear.data.model

enum class WorkoutType(val displayName: String, val iconRes: String) {
    RUN("Run", "ic_run"),
    WALK("Walk", "ic_walk"),
    RIDE("Ride", "ic_bike"),
    HIKE("Hike", "ic_hike"),
    INDOOR("Indoor", "ic_heart")
}

enum class WorkoutState {
    IDLE,
    ACTIVE,
    PAUSED,
    FINISHED,
    MIRRORED
}

enum class HeartRateZone(
    val title: String,
    val minBpm: Int,
    val maxBpm: Int,
    val colorHex: Long
) {
    RESTING("Resting", 0, 99, 0xFF78909CL),
    WARMUP("Warmup", 100, 119, 0xFF00B0FFL),       // Neon Cyan
    AEROBIC("Aerobic", 120, 144, 0xFF00E676L),      // Vivid Green
    THRESHOLD("Threshold", 145, 164, 0xFFFFD600L),  // Electric Amber
    ANAEROBIC("Anaerobic", 165, 179, 0xFFFF9100L),  // Deep Orange
    MAX("Max Zone", 180, 240, 0xFFFF5722L);         // Coral Flame

    companion object {
        fun fromBpm(bpm: Int): HeartRateZone = when {
            bpm <= 0 -> RESTING
            bpm < 100 -> RESTING
            bpm < 120 -> WARMUP
            bpm < 145 -> AEROBIC
            bpm < 165 -> THRESHOLD
            bpm < 180 -> ANAEROBIC
            else -> MAX
        }
    }
}

data class LiveHeartRate(
    val bpm: Int = 0,
    val accuracy: Int = 0, // 0: No Contact, 1: Unreliable, 2: Low, 3: Medium, 4: High
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveWorkoutMetrics(
    val elapsedSeconds: Long = 0L,
    val heartRate: Int = 0,
    val hrAccuracy: Int = 0,
    val steps: Int = 0,
    val cadenceSpm: Int = 0,
    val caloriesKcal: Int = 0,
    val distanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val speedMps: Double = 0.0
)

data class PhoneMirroredMetrics(
    val seconds: Long = 0L,
    val meters: Double = 0.0,
    val calories: Int = 0,
    val heartRate: Int = 0,
    val isPhoneActive: Boolean = false
)

data class DailyActivityStats(
    val steps: Int = 0,
    val stepGoal: Int = 10000,
    val activeCalories: Int = 0,
    val calorieGoal: Int = 500,
    val currentBpm: Int = 0,
    val restingBpm: Int = 68
)

data class PhoneConnectionStatus(
    val isConnected: Boolean = false,
    val phoneNodeName: String = "",
    val phoneNodeId: String = "",
    val pendingQueueCount: Int = 0
)
