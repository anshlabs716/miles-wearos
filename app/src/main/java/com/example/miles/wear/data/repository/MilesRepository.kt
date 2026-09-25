package com.example.miles.wear.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.miles.wear.data.local.dao.DayStatsDao
import com.example.miles.wear.data.local.dao.PetDao
import com.example.miles.wear.data.local.dao.QueueDao
import com.example.miles.wear.data.local.dao.SavedPinDao
import com.example.miles.wear.data.local.dao.SavedRouteDao
import com.example.miles.wear.data.local.dao.TrainingProgressDao
import com.example.miles.wear.data.local.dao.WorkoutSessionDao
import com.example.miles.wear.data.local.entity.DayStatsEntity
import com.example.miles.wear.data.local.entity.PetEntity
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.SavedPinEntity
import com.example.miles.wear.data.local.entity.SavedRouteEntity
import com.example.miles.wear.data.local.entity.TrainingProgressEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.AchievementBadge
import com.example.miles.wear.data.model.DistanceUnit
import com.example.miles.wear.data.model.GpsPoint
import com.example.miles.wear.data.model.HudLayoutMode
import com.example.miles.wear.data.model.PetType
import com.example.miles.wear.data.model.PrimaryMetricType
import com.example.miles.wear.data.model.RecordsSummary
import com.example.miles.wear.data.model.ThemeAccent
import com.example.miles.wear.data.model.TrainingCatalog
import com.example.miles.wear.data.model.TrainingPlanState
import com.example.miles.wear.data.model.WearSettings
import com.example.miles.wear.data.model.WeeklyProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MilesRepository(
    context: Context,
    private val queueDao: QueueDao,
    private val sessionDao: WorkoutSessionDao,
    private val pinDao: SavedPinDao,
    private val dayStatsDao: DayStatsDao,
    private val petDao: PetDao,
    private val routeDao: SavedRouteDao,
    private val trainingDao: TrainingProgressDao
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
        val weeklyDistanceKm = prefs.getFloat("weekly_distance_km", 0f).toDouble()
        val weeklyActiveMinutes = prefs.getInt("weekly_active_minutes", 0)
        val lazyDaysPerWeek = prefs.getInt("lazy_days_per_week", 2)
        val moveReminderEnabled = prefs.getBoolean("move_reminder_enabled", false)
        val moveReminderIntervalMin = prefs.getInt("move_reminder_interval_min", 60)
        val voiceNavEnabled = prefs.getBoolean("voice_nav_enabled", true)
        val calorieGoalKcal = prefs.getInt("calorie_goal_kcal", 500)
        val bodyWeightKg = prefs.getInt("body_weight_kg", 70)

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
            stepGoal = stepGoal,
            weeklyDistanceKm = weeklyDistanceKm,
            weeklyActiveMinutes = weeklyActiveMinutes,
            lazyDaysPerWeek = lazyDaysPerWeek,
            moveReminderEnabled = moveReminderEnabled,
            moveReminderIntervalMin = moveReminderIntervalMin,
            voiceNavEnabled = voiceNavEnabled,
            calorieGoalKcal = calorieGoalKcal,
            bodyWeightKg = bodyWeightKg
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
            .putFloat("weekly_distance_km", newSettings.weeklyDistanceKm.toFloat())
            .putInt("weekly_active_minutes", newSettings.weeklyActiveMinutes)
            .putInt("lazy_days_per_week", newSettings.lazyDaysPerWeek)
            .putBoolean("move_reminder_enabled", newSettings.moveReminderEnabled)
            .putInt("move_reminder_interval_min", newSettings.moveReminderIntervalMin)
            .putBoolean("voice_nav_enabled", newSettings.voiceNavEnabled)
            .putInt("calorie_goal_kcal", newSettings.calorieGoalKcal)
            .putInt("body_weight_kg", newSettings.bodyWeightKg)
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

    // ----- Saved pins -----
    val allPins: Flow<List<SavedPinEntity>> = pinDao.getAllPins()

    suspend fun getPinsNow(): List<SavedPinEntity> = withContext(Dispatchers.IO) {
        pinDao.getAllPinsNow()
    }

    suspend fun addPin(name: String, lat: Double, lon: Double, note: String = "") {
        withContext(Dispatchers.IO) {
            pinDao.insertPin(SavedPinEntity(name = name, latitude = lat, longitude = lon, note = note))
        }
    }

    suspend fun deletePin(id: Long) {
        withContext(Dispatchers.IO) {
            pinDao.deletePin(id)
        }
    }

    // ----- Daily stats + streaks -----
    /**
     * Today's persisted day-stats row (real steps/distance/calories).
     * Re-read every 30s so the date rolls over correctly past midnight.
     */
    fun observeTodayStats(): Flow<DayStatsEntity?> = flow {
        while (currentCoroutineContext().isActive) {
            emit(withContext(Dispatchers.IO) {
                dayStatsDao.getDay(dateKey(System.currentTimeMillis()))
            })
            delay(30_000L)
        }
    }

    /** Adds today's activity into the day_stats row (used at workout finish). */
    suspend fun recordDayActivity(distanceMeters: Double, steps: Int, activeSeconds: Long, calories: Int) {
        withContext(Dispatchers.IO) {
            val key = dateKey(System.currentTimeMillis())
            val existing = dayStatsDao.getDay(key)
            dayStatsDao.upsertDay(
                DayStatsEntity(
                    dateKey = key,
                    steps = (existing?.steps ?: 0) + steps,
                    distanceMeters = (existing?.distanceMeters ?: 0.0) + distanceMeters,
                    activeSeconds = (existing?.activeSeconds ?: 0L) + activeSeconds,
                    calories = (existing?.calories ?: 0) + calories,
                    restingBpm = existing?.restingBpm ?: 0
                )
            )
        }
    }

    /** Lowest sustained HR seen today (real sensor samples while idle). */
    suspend fun recordRestingBpm(bpm: Int) {
        if (bpm < 40 || bpm > 160) return
        withContext(Dispatchers.IO) {
            val key = dateKey(System.currentTimeMillis())
            val existing = dayStatsDao.getDay(key)
            if (existing == null) {
                dayStatsDao.upsertDay(DayStatsEntity(dateKey = key, restingBpm = bpm))
            } else if (existing.restingBpm == 0 || bpm < existing.restingBpm) {
                dayStatsDao.upsertDay(existing.copy(restingBpm = bpm))
            }
        }
    }

    suspend fun todayRestingBpm(): Int = withContext(Dispatchers.IO) {
        dayStatsDao.getDay(dateKey(System.currentTimeMillis()))?.restingBpm ?: 0
    }

    /** Real streaks + personal records from saved workout history and day stats. */
    suspend fun computeRecords(): RecordsSummary = withContext(Dispatchers.IO) {
        val sessions = sessionDao.getAllSessionsNow()
        val days = dayStatsDao.getAllDays()

        val byDay = sessions.groupBy { dateKey(it.startTime) }
        val workDays = byDay.filterValues { it.isNotEmpty() }.keys
        val distanceDays = byDay.entries.filter { (_, list) -> list.sumOf { s -> s.distanceMeters } > 5.0 }
            .map { it.key }
            .toSet()
        val stepDays = days.filter { it.steps > 0 }.map { it.dateKey }.toSet()

        val totalWorkouts = sessions.size
        val totalDistance = sessions.sumOf { it.distanceMeters }
        val longestDist = sessions.maxOfOrNull { it.distanceMeters } ?: 0.0
        val longestDur = sessions.maxOfOrNull { it.durationSeconds } ?: 0L
        val mostSteps = sessions.maxOfOrNull { it.totalSteps } ?: 0
        val maxElev = sessions.maxOfOrNull { it.elevationGainMeters } ?: 0.0

        // Fastest pace = smallest (durationSeconds / distanceKm) for sessions with distance
        val fastestPace = sessions
            .filter { it.distanceMeters >= 50.0 && it.durationSeconds > 0 }
            .minOfOrNull { it.durationSeconds.toDouble() / (it.distanceMeters / 1000.0) } ?: 0.0

        RecordsSummary(
            workoutStreak = countStreak(workDays),
            distanceStreak = countStreak(distanceDays),
            stepStreak = countStreak(stepDays),
            longestDistanceMeters = longestDist,
            longestDurationSeconds = longestDur,
            fastestPace = fastestPace,
            mostStepsInWorkout = mostSteps,
            maxElevationGainMeters = maxElev,
            totalWorkouts = totalWorkouts,
            totalDistanceMeters = totalDistance
        )
    }

    private fun countStreak(days: Set<String>): Int {
        if (days.isEmpty()) return 0
        val daySet = days.mapNotNull { localDateToEpochDay(it) }.toMutableSet()
        if (daySet.isEmpty()) return 0

        // A streak counts from the most recent day; today can be the start
        // or continue from yesterday if today has no entry yet.
        val today = java.time.LocalDate.now().toEpochDay()
        var cursor = if (daySet.contains(today)) today else today - 1
        var streak = 0
        while (daySet.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    private fun dateKey(timeMillis: Long): String {
        val dt = java.time.Instant.ofEpochMilli(timeMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        return dt.toString() // yyyy-MM-dd
    }

    private fun localDateToEpochDay(key: String): Long? {
        return try {
            java.time.LocalDate.parse(key).toEpochDay()
        } catch (_: Exception) {
            null
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

    // ----- Fitness pet (fed by real steps) -----
    suspend fun getPet(): PetEntity? = withContext(Dispatchers.IO) {
        petDao.getPet()
    }

    suspend fun savePet(petType: PetType, petName: String) {
        withContext(Dispatchers.IO) {
            petDao.savePet(PetEntity(petType = petType.name, petName = petName))
        }
    }

    /** Days where the step goal was really hit — these feed the pet. */
    suspend fun treatsFed(): Int = withContext(Dispatchers.IO) {
        val goal = _settings.value.stepGoal
        dayStatsDao.getAllDays().count { it.steps >= goal }
    }

    /** Real distance + active minutes for the current ISO week (Mon–Sun). */
    suspend fun computeWeeklyProgress(): WeeklyProgress = withContext(Dispatchers.IO) {
        val today = java.time.LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        val s = _settings.value
        val days = dayStatsDao.getAllDays()
            .filter { day ->
                localDateToEpochDay(day.dateKey)?.let { it in monday.toEpochDay()..sunday.toEpochDay() } == true
            }
        WeeklyProgress(
            distanceKm = days.sumOf { it.distanceMeters } / 1000.0,
            activeMinutes = (days.sumOf { it.activeSeconds } / 60L).toInt(),
            distanceGoalKm = s.weeklyDistanceKm,
            minutesGoal = s.weeklyActiveMinutes,
            weekLabel = "${monday.monthValue}/${monday.dayOfMonth} – ${sunday.monthValue}/${sunday.dayOfMonth}"
        )
    }

    /** Badges computed from real history — locked badges show honest progress. */
    suspend fun computeBadges(): List<AchievementBadge> = withContext(Dispatchers.IO) {
        val sessions = sessionDao.getAllSessionsNow()
        val days = dayStatsDao.getAllDays()
        val byDay = sessions.groupBy { dateKey(it.startTime) }
        val workDays = byDay.filterValues { it.isNotEmpty() }.keys

        val totalWorkouts = sessions.size
        val totalDistance = sessions.sumOf { it.distanceMeters }
        val longestDist = sessions.maxOfOrNull { it.distanceMeters } ?: 0.0
        val maxElev = sessions.maxOfOrNull { it.elevationGainMeters } ?: 0.0
        val totalSeconds = sessions.sumOf { it.durationSeconds }
        val streak = countStreak(workDays)
        val petPen = petDao.getPet()
        val treats = if (petPen != null) dayStatsDao.getAllDays().count { it.steps >= _settings.value.stepGoal } else 0

        fun km(m: Double) = String.format(java.util.Locale.US, "%.1f", m / 1000.0)
        fun h(sec: Long) = String.format(java.util.Locale.US, "%.1f", sec / 3600.0)

        listOf(
            AchievementBadge(
                key = "first", emoji = "🏃", title = "First Workout",
                description = "Finish your first workout",
                unlocked = totalWorkouts >= 1,
                progress = "$totalWorkouts / 1 workouts"
            ),
            AchievementBadge(
                key = "ten", emoji = "🔟", title = "Ten Workouts",
                description = "Finish 10 workouts",
                unlocked = totalWorkouts >= 10,
                progress = "$totalWorkouts / 10 workouts"
            ),
            AchievementBadge(
                key = "five_k", emoji = "🚀", title = "5K Finisher",
                description = "Longest workout over 5 km",
                unlocked = longestDist >= 5000.0,
                progress = "${km(longestDist)} km / 5 km"
            ),
            AchievementBadge(
                key = "ten_k", emoji = "🎯", title = "10K Finisher",
                description = "Longest workout over 10 km",
                unlocked = longestDist >= 10000.0,
                progress = "${km(longestDist)} km / 10 km"
            ),
            AchievementBadge(
                key = "hundred_k", emoji = "💯", title = "100K Club",
                description = "100 km total distance",
                unlocked = totalDistance >= 100000.0,
                progress = "${km(totalDistance)} km / 100 km"
            ),
            AchievementBadge(
                key = "streak7", emoji = "🔥", title = "7-Day Streak",
                description = "Work out 7 days in a row",
                unlocked = streak >= 7,
                progress = "$streak / 7 days"
            ),
            AchievementBadge(
                key = "tenk_steps", emoji = "👟", title = "10k Steps Day",
                description = "Hit 10,000 real steps in a day",
                unlocked = days.any { it.steps >= 10000 },
                progress = "${days.count { it.steps >= 10000 }} days"
            ),
            AchievementBadge(
                key = "elev500", emoji = "⛰️", title = "Elevation 500m",
                description = "500 m elevation gain in one workout",
                unlocked = maxElev >= 500.0,
                progress = "${maxElev.toInt()} m / 500 m"
            ),
            AchievementBadge(
                key = "ten_hours", emoji = "⏱️", title = "10 Hours",
                description = "10 hours of total workout time",
                unlocked = totalSeconds >= 36000L,
                progress = "${h(totalSeconds)} h / 10 h"
            ),
            AchievementBadge(
                key = "pet_friend", emoji = "🐾", title = "Pet Friend",
                description = "Feed your pet treats with real steps",
                unlocked = treats >= 1,
                progress = "$treats treats"
            )
        )
    }

    // ----- Saved routes (real GPS points from navigation or pins) -----
    val allSavedRoutes: Flow<List<SavedRouteEntity>> = routeDao.getAllRoutes()

    suspend fun getRoutesNow(): List<SavedRouteEntity> = withContext(Dispatchers.IO) {
        routeDao.getAllRoutesNow()
    }

    suspend fun getRouteById(id: Long): SavedRouteEntity? = withContext(Dispatchers.IO) {
        routeDao.getRouteById(id)
    }

    suspend fun saveRoute(name: String, points: List<GpsPoint>, distanceMeters: Double) {
        withContext(Dispatchers.IO) {
            if (points.size < 2) return@withContext
            routeDao.insertRoute(
                SavedRouteEntity(
                    name = name,
                    pointsJson = gpsPointsToJson(points),
                    distanceMeters = distanceMeters
                )
            )
        }
    }

    suspend fun deleteRoute(id: Long) {
        withContext(Dispatchers.IO) {
            routeDao.deleteRoute(id)
        }
    }

    /** Decodes a saved route's points back for drawing/following. */
    fun routePoints(route: SavedRouteEntity): List<GpsPoint> = parseGpsPoints(route.pointsJson)

    // ----- Progressive training plans -----
    fun setPendingTrainingDay(dayId: String?) {
        prefs.edit()
            .putString("training_pending_day", dayId ?: "")
            .apply()
    }

    /** Marks the pending plan day complete (called when a workout really finishes). */
    suspend fun completePendingTrainingDayIfAny() = withContext(Dispatchers.IO) {
        val dayId = prefs.getString("training_pending_day", null)?.takeIf { it.isNotBlank() }
            ?: return@withContext
        val progress = trainingDao.getProgress() ?: return@withContext
        val done = (parseDaySet(progress.completedJson) + dayId).toSortedSet()
        val plan = TrainingCatalog.byKey(progress.planKey)
        val allDone = plan != null && plan.workoutDays.all { it.id in done }
        trainingDao.saveProgress(
            progress.copy(
                completedJson = JSONArray(done.toList()).toString(),
                finishedAtEpochDay = if (allDone) java.time.LocalDate.now().toEpochDay() else progress.finishedAtEpochDay
            )
        )
        prefs.edit().remove("training_pending_day").apply()
    }

    suspend fun trainingPlanState(): TrainingPlanState = withContext(Dispatchers.IO) {
        val p = trainingDao.getProgress() ?: return@withContext TrainingPlanState()
        TrainingPlanState(
            planKey = p.planKey,
            started = p.startedEpochDay > 0,
            completedDayIds = parseDaySet(p.completedJson),
            finished = p.finishedAtEpochDay > 0
        )
    }

    suspend fun startTrainingPlan(key: String) = withContext(Dispatchers.IO) {
        trainingDao.saveProgress(
            TrainingProgressEntity(
                planKey = key,
                startedEpochDay = java.time.LocalDate.now().toEpochDay(),
                completedJson = "[]"
            )
        )
    }

    private fun parseDaySet(json: String): Set<String> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }.toSet()
    } catch (_: Exception) {
        emptySet()
    }

    private fun gpsPointsToJson(points: List<GpsPoint>): String {
        val arr = JSONArray()
        points.forEach { p ->
            arr.put(
                JSONObject()
                    .put("lat", p.lat)
                    .put("lon", p.lon)
                    .put("alt", p.alt)
                    .put("speed", p.speed)
                    .put("t", p.timestamp)
            )
        }
        return arr.toString()
    }

    private fun parseGpsPoints(json: String): List<GpsPoint> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            GpsPoint(
                lat = o.optDouble("lat"),
                lon = o.optDouble("lon"),
                alt = o.optDouble("alt", 0.0),
                speed = o.optDouble("speed", 0.0),
                timestamp = o.optLong("t", 0L)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    // ----- Backup / restore support (all real data, no placeholders) -----
    suspend fun allSessionsNow(): List<WorkoutSessionEntity> = withContext(Dispatchers.IO) {
        sessionDao.getAllSessionsNow()
    }

    suspend fun allDaysNow(): List<DayStatsEntity> = withContext(Dispatchers.IO) {
        dayStatsDao.getAllDays()
    }

    suspend fun petNow(): PetEntity? = withContext(Dispatchers.IO) {
        petDao.getPet()
    }

    suspend fun trainingNow(): TrainingProgressEntity? = withContext(Dispatchers.IO) {
        trainingDao.getProgress()
    }

    suspend fun replaceSessions(sessions: List<WorkoutSessionEntity>) {
        withContext(Dispatchers.IO) {
            sessionDao.clearSessions()
            sessionDao.insertAll(sessions)
        }
    }

    suspend fun replacePins(pins: List<SavedPinEntity>) {
        withContext(Dispatchers.IO) {
            pinDao.clearPins()
            pinDao.insertAll(pins)
        }
    }

    suspend fun replaceDays(days: List<DayStatsEntity>) {
        withContext(Dispatchers.IO) {
            dayStatsDao.clearDays()
            dayStatsDao.insertAll(days)
        }
    }

    suspend fun replaceRoutes(routes: List<SavedRouteEntity>) {
        withContext(Dispatchers.IO) {
            routeDao.clearRoutes()
            routeDao.insertAll(routes)
        }
    }

    suspend fun replaceTraining(progress: TrainingProgressEntity?) {
        withContext(Dispatchers.IO) {
            trainingDao.clearProgress()
            if (progress != null) trainingDao.saveProgress(progress)
        }
    }

    suspend fun replacePet(pet: PetEntity?) {
        withContext(Dispatchers.IO) {
            petDao.clearPet()
            if (pet != null) petDao.savePet(pet)
        }
    }

    fun dumpPrefs(): Map<String, Any?> = prefs.all

    fun restorePrefs(values: Map<String, Any>) {
        val editor = prefs.edit()
        values.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                is String -> editor.putString(key, value)
                else -> editor.putString(key, value.toString())
            }
        }
        editor.apply()
        // Refresh the settings flow from the restored values
        _settings.value = loadSettingsFromPrefs()
    }
}
