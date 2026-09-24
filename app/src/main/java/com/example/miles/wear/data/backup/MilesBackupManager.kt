package com.example.miles.wear.data.backup

import android.content.Context
import com.example.miles.wear.data.local.entity.DayStatsEntity
import com.example.miles.wear.data.local.entity.PetEntity
import com.example.miles.wear.data.local.entity.SavedPinEntity
import com.example.miles.wear.data.local.entity.SavedRouteEntity
import com.example.miles.wear.data.local.entity.TrainingProgressEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.export.WorkoutExporter
import com.example.miles.wear.data.repository.MilesRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Full local backup/restore of every real data table + settings,
 * as a portable JSON archive (same spirit as the phone's backup).
 */
object MilesBackupManager {

    const val BACKUP_FORMAT = "MILES-WEAROS-backup"
    const val FORMAT_VERSION = 1

    suspend fun createBackup(context: Context, repo: MilesRepository): File? {
        val backup = JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("formatVersion", FORMAT_VERSION)
            .put("createdEpochMs", System.currentTimeMillis())
            .put("prefs", JSONObject(repo.dumpPrefs()))

        val sessions = JSONArray()
        repo.allSessionsNow().forEach { sessions.put(sessionJson(it)) }
        backup.put("sessions", sessions)

        val pins = JSONArray()
        repo.getPinsNow().forEach { pins.put(pinJson(it)) }
        backup.put("pins", pins)

        val days = JSONArray()
        repo.allDaysNow().forEach { days.put(dayJson(it)) }
        backup.put("dayStats", days)

        val routes = JSONArray()
        repo.getRoutesNow().forEach { routes.put(routeJson(it)) }
        backup.put("routes", routes)

        repo.trainingNow()?.let { backup.put("training", trainingJson(it)) }
        repo.petNow()?.let { backup.put("pet", petJson(it)) }

        val name = "miles_backup_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())}.json"
        return WorkoutExporter.write(context, name, backup.toString(2))
    }

    suspend fun restore(context: Context, repo: MilesRepository, file: File): Boolean {
        return try {
            val root = JSONObject(file.readText())
            if (root.optString("format") != BACKUP_FORMAT) return false

            root.optJSONObject("prefs")?.let { prefs ->
                val map = HashMap<String, Any>()
                prefs.keys().forEach { k ->
                    val v = prefs.get(k)
                    if (v != JSONObject.NULL) map[k] = v
                }
                repo.restorePrefs(map)
            }

            val sessions = buildList {
                root.optJSONArray("sessions")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        sessionFromJson(arr.optJSONObject(i))?.let { add(it) }
                    }
                }
            }
            repo.replaceSessions(sessions)

            val pins = buildList {
                root.optJSONArray("pins")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        pinFromJson(arr.optJSONObject(i))?.let { add(it) }
                    }
                }
            }
            repo.replacePins(pins)

            val days = buildList {
                root.optJSONArray("dayStats")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        dayFromJson(arr.optJSONObject(i))?.let { add(it) }
                    }
                }
            }
            repo.replaceDays(days)

            val routes = buildList {
                root.optJSONArray("routes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        routeFromJson(arr.optJSONObject(i))?.let { add(it) }
                    }
                }
            }
            repo.replaceRoutes(routes)

            root.optJSONObject("training")?.let { repo.replaceTraining(trainingFromJson(it)) }
            root.optJSONObject("pet")?.let { repo.replacePet(petFromJson(it)) }

            true
        } catch (_: Exception) {
            false
        }
    }

    // ----- Session -----
    private fun sessionJson(s: WorkoutSessionEntity) = JSONObject()
        .put("id", s.id)
        .put("workoutType", s.workoutType)
        .put("startTime", s.startTime)
        .put("endTime", s.endTime)
        .put("durationSeconds", s.durationSeconds)
        .put("totalSteps", s.totalSteps)
        .put("avgBpm", s.avgBpm)
        .put("maxBpm", s.maxBpm)
        .put("caloriesKcal", s.caloriesKcal)
        .put("distanceMeters", s.distanceMeters)
        .put("elevationGainMeters", s.elevationGainMeters)
        .put("isSyncedToPhone", s.isSyncedToPhone)
        .put("routeGeoJson", s.routeGeoJson)
        .put("splitsJson", s.splitsJson)

    private fun sessionFromJson(o: JSONObject?): WorkoutSessionEntity? {
        if (o == null) return null
        return WorkoutSessionEntity(
            id = o.optLong("id", 0),
            workoutType = o.optString("workoutType", "GENERAL"),
            startTime = o.optLong("startTime", 0),
            endTime = o.optLong("endTime", 0),
            durationSeconds = o.optLong("durationSeconds", 0),
            totalSteps = o.optInt("totalSteps", 0),
            avgBpm = o.optInt("avgBpm", 0),
            maxBpm = o.optInt("maxBpm", 0),
            caloriesKcal = o.optInt("caloriesKcal", 0),
            distanceMeters = o.optDouble("distanceMeters", 0.0),
            elevationGainMeters = o.optDouble("elevationGainMeters", 0.0),
            isSyncedToPhone = o.optBoolean("isSyncedToPhone", false),
            routeGeoJson = o.optString("routeGeoJson", ""),
            splitsJson = o.optString("splitsJson", "")
        )
    }

    // ----- Pin -----
    private fun pinJson(p: SavedPinEntity) = JSONObject()
        .put("id", p.id)
        .put("name", p.name)
        .put("latitude", p.latitude)
        .put("longitude", p.longitude)
        .put("note", p.note)
        .put("createdAt", p.createdAt)

    private fun pinFromJson(o: JSONObject?): SavedPinEntity? {
        if (o == null) return null
        return SavedPinEntity(
            id = o.optLong("id", 0),
            name = o.optString("name", "Pin"),
            latitude = o.optDouble("latitude", 0.0),
            longitude = o.optDouble("longitude", 0.0),
            note = o.optString("note", ""),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    // ----- Day stats -----
    private fun dayJson(d: DayStatsEntity) = JSONObject()
        .put("dateKey", d.dateKey)
        .put("steps", d.steps)
        .put("distanceMeters", d.distanceMeters)
        .put("activeSeconds", d.activeSeconds)
        .put("calories", d.calories)
        .put("restingBpm", d.restingBpm)

    private fun dayFromJson(o: JSONObject?): DayStatsEntity? {
        if (o == null) return null
        return DayStatsEntity(
            dateKey = o.optString("dateKey", ""),
            steps = o.optInt("steps", 0),
            distanceMeters = o.optDouble("distanceMeters", 0.0),
            activeSeconds = o.optLong("activeSeconds", 0),
            calories = o.optInt("calories", 0),
            restingBpm = o.optInt("restingBpm", 0)
        )
    }

    // ----- Route -----
    private fun routeJson(r: SavedRouteEntity) = JSONObject()
        .put("id", r.id)
        .put("name", r.name)
        .put("pointsJson", r.pointsJson)
        .put("distanceMeters", r.distanceMeters)
        .put("createdAt", r.createdAt)

    private fun routeFromJson(o: JSONObject?): SavedRouteEntity? {
        if (o == null) return null
        return SavedRouteEntity(
            id = o.optLong("id", 0),
            name = o.optString("name", "Route"),
            pointsJson = o.optString("pointsJson", "[]"),
            distanceMeters = o.optDouble("distanceMeters", 0.0),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    // ----- Training -----
    private fun trainingJson(t: TrainingProgressEntity) = JSONObject()
        .put("planKey", t.planKey)
        .put("startedEpochDay", t.startedEpochDay)
        .put("completedJson", t.completedJson)
        .put("finishedAtEpochDay", t.finishedAtEpochDay)

    private fun trainingFromJson(o: JSONObject?): TrainingProgressEntity? {
        if (o == null) return null
        return TrainingProgressEntity(
            planKey = o.optString("planKey", ""),
            startedEpochDay = o.optLong("startedEpochDay", 0),
            completedJson = o.optString("completedJson", "[]"),
            finishedAtEpochDay = o.optLong("finishedAtEpochDay", 0)
        )
    }

    // ----- Pet -----
    private fun petJson(p: PetEntity) = JSONObject()
        .put("petType", p.petType)
        .put("petName", p.petName)

    private fun petFromJson(o: JSONObject?): PetEntity? {
        if (o == null) return null
        return PetEntity(
            petType = o.optString("petType", "DOG"),
            petName = o.optString("petName", "Miles")
        )
    }
}