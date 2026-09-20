package com.example.miles.wear.data.model

enum class WorkoutType(
    val displayName: String,
    val emoji: String,
    val iconRes: String
) {
    WALK("Walking", "🚶", "ic_walk"),
    RUN("Running", "🏃", "ic_run"),
    CYCLING("Cycling", "🚴", "ic_bike"),
    HIKE("Hiking", "⛰️", "ic_hike"),
    GENERAL("General", "⚡", "ic_heart"),
    CUSTOM("Custom", "✨", "ic_custom");

    companion object {
        // Backwards compatibility aliases for tests and existing references
        val RIDE = CYCLING
        val OTHER = GENERAL
        val INDOOR = GENERAL

        fun fromString(name: String): WorkoutType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: if (name.startsWith("CUSTOM", ignoreCase = true)) CUSTOM else RUN
        }
    }
}

data class GpsPoint(
    val lat: Double,
    val lon: Double,
    val alt: Double = 0.0,
    val speed: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class WorkoutState {
    IDLE,
    STARTING,
    RUNNING,
    PAUSED,
    FINISHING,
    COMPLETED,
    DISCARDED,
    MIRRORED
}

enum class GpsStatus(val label: String) {
    SEARCHING("GPS Searching…"),
    READY("GPS Ready"),
    UNAVAILABLE("GPS Unavailable"),
    INDOOR("Indoor / Off")
}

enum class DistanceUnit(
    val title: String,
    val distanceLabel: String,
    val paceLabel: String,
    val speedLabel: String
) {
    METRIC("Metric", "km", "/km", "km/h"),
    IMPERIAL("Imperial", "mi", "/mi", "mph");

    fun formatDistance(meters: Double): String {
        return if (this == METRIC) {
            String.format("%.2f", meters / 1000.0)
        } else {
            String.format("%.2f", meters * 0.000621371)
        }
    }

    fun formatPace(meters: Double, elapsedSeconds: Long): String {
        if (meters < 15.0 || elapsedSeconds == 0L) return "--:--"
        val paceSec = if (this == METRIC) {
            (elapsedSeconds / (meters / 1000.0)).toLong()
        } else {
            (elapsedSeconds / (meters * 0.000621371)).toLong()
        }
        if (paceSec > 3599) return ">59m"
        val m = paceSec / 60
        val s = paceSec % 60
        return String.format("%d:%02d", m, s)
    }

    fun formatSpeed(speedMps: Double): String {
        val speed = if (this == METRIC) {
            speedMps * 3.6
        } else {
            speedMps * 2.23694
        }
        return String.format("%.1f", speed)
    }
}

enum class ThemeAccent(val title: String, val colorHex: Long) {
    CYAN("MILES Cyan", 0xFF00B0FF),
    CORAL("Coral Flame", 0xFFFF5722),
    GREEN("Vivid Green", 0xFF00E676),
    AMBER("Electric Amber", 0xFFFFD600),
    PURPLE("Neon Purple", 0xFFE040FB);

    val primaryColor: androidx.compose.ui.graphics.Color
        get() = androidx.compose.ui.graphics.Color(colorHex)
}

enum class PrimaryMetricType(val title: String) {
    DISTANCE("Distance"),
    PACE_SPEED("Pace / Speed"),
    HEART_RATE("Heart Rate"),
    STEPS("Steps"),
    CALORIES("Calories")
}

enum class HudLayoutMode(val title: String) {
    STANDARD("Standard HUD"),
    MAP_FOCUSED("Map First"),
    STATS_LARGE("Large Numbers")
}

data class WearSettings(
    val unit: DistanceUnit = DistanceUnit.METRIC,
    val keepScreenOn: Boolean = true,
    val gpsEnabled: Boolean = true,
    val hrEnabled: Boolean = true,
    val stepTrackingEnabled: Boolean = true,
    val autoPauseEnabled: Boolean = false,
    val hapticAlertsEnabled: Boolean = true,
    val batterySaverEnabled: Boolean = false,
    // Customization & Display Preferences
    val themeAccent: ThemeAccent = ThemeAccent.CYAN,
    val primaryMetric: PrimaryMetricType = PrimaryMetricType.DISTANCE,
    val hudLayout: HudLayoutMode = HudLayoutMode.STANDARD,
    val showGpsMapInHud: Boolean = true,
    val showCadenceInHud: Boolean = true,
    val showHeartRateZoneRing: Boolean = true,
    val showBatteryInDashboard: Boolean = true,
    val showGpsInDashboard: Boolean = true,
    val showRecentActivitiesInDashboard: Boolean = true,
    val compactCards: Boolean = false,
    val workoutStartHaptic: Boolean = true,
    val workoutPauseResumeHaptic: Boolean = true,
    val workoutFinishHaptic: Boolean = true,
    val splitHaptic: Boolean = true,
    val highContrastText: Boolean = false
)

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
    val isAvailable: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveWorkoutMetrics(
    val elapsedSeconds: Long = 0L,
    val heartRate: Int = 0,
    val hrAccuracy: Int = 0,
    val isHrAvailable: Boolean = true,
    val steps: Int = 0,             // workout steps
    val dailySteps: Int = 0,        // daily total steps
    val cadenceSpm: Int = 0,
    val caloriesKcal: Int = 0,
    val distanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val speedMps: Double = 0.0,
    val gpsStatus: GpsStatus = GpsStatus.SEARCHING
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
