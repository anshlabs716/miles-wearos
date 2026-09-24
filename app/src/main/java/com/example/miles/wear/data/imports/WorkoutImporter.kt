package com.example.miles.wear.data.imports

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.miles.wear.data.backup.MilesBackupManager
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.GpsPoint
import com.example.miles.wear.data.repository.MilesRepository
import com.example.miles.wear.data.export.WorkoutExporter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Real-data import: brings workouts (GPX / MILES JSON) and full backups into
 * the app from outside — Storage Access Framework picks or "Share to MILES"
 * intents. Imported workouts keep their real distance, elevation, time and
 * track; nothing is invented. Unknown fields (HR, calories, steps) stay 0
 * rather than being guessed.
 */
object WorkoutImporter {

    const val WORKOUT_JSON_SINGLE = "MILES-WEAROS-workout"
    const val WORKOUT_JSON_ALL = "MILES-WEAROS-workouts"

    data class ImportResult(val count: Int, val message: String) {
        fun uiText(): String = message
    }

    // ------------------------------------------------------------------
    // Entry point used by the export screen + share intents.
    // ------------------------------------------------------------------

    /**
     * Does the right thing based on content:
     * - MILES-WEAROS-backup JSON        → full restore (all tables + prefs)
     * - MILES-WEAROS-workout(s) JSON    → insert workout sessions
     * - GPX                             → insert each <trk> as a session
     * Returns how many items landed + a user-facing status.
     */
    suspend fun importStream(
        context: Context,
        repo: MilesRepository,
        fileName: String,
        content: String
    ): ImportResult {
        if (content.isBlank()) return ImportResult(0, "Empty file — nothing imported")

        // 1) Full backup → restore everything
        if (isBackupContent(content)) {
            val stored = saveToExports(context, fileName, content)
            val ok = MilesBackupManager.restore(context, repo, stored)
            val sessionCount = try {
                JSONObject(content).optJSONArray("sessions")?.length() ?: 0
            } catch (_: Exception) {
                0
            }
            return if (ok) {
                ImportResult(sessionCount, "Backup restored ($sessionCount workouts) ✓")
            } else {
                ImportResult(0, "Backup restore failed")
            }
        }

        // 2) MILES workout JSON (single or array) + GPX tracks
        val sessions = when {
            content.trimStart().startsWith("{") && content.contains("trackPoints") ->
                importWorkoutsJson(content)
            content.trimStart().startsWith("{") && isWorkoutsJson(content) ->
                importWorkoutsJson(content)
            content.trimStart().startsWith("<") || content.trimStart().startsWith("<?xml") ->
                importGpx(content)
            else -> return ImportResult(0, "Unknown file type (use GPX, JSON, or backup)")
        }

        if (sessions.isEmpty()) return ImportResult(0, "No workout data found in file")

        var inserted = 0
        sessions.forEach { s -> repo.saveWorkoutSession(s); inserted++ }
        // keep a copy on-device so it shows in the export list
        saveToExports(context, fileName, content)

        val word = if (inserted == 1) "workout" else "workouts"
        return ImportResult(inserted, "Imported $inserted $word ✓")
    }

    fun isBackupContent(content: String): Boolean = try {
        JSONObject(content).optString("format") == MilesBackupManager.BACKUP_FORMAT
    } catch (_: Exception) {
        false
    }

    fun isWorkoutsJson(content: String): Boolean = try {
        val root = JSONObject(content)
        root.optString("format") == WORKOUT_JSON_ALL ||
            (root.optJSONArray("workouts") != null)
    } catch (_: Exception) {
        false
    }

    /** Reads + imports a content:// URI (SAF picker or share intent). */
    suspend fun importUri(
        context: Context,
        repo: MilesRepository,
        uri: Uri,
        fallbackName: String
    ): ImportResult {
        val name = displayName(context.contentResolver, uri) ?: fallbackName
        val content = readText(context.contentResolver, uri)
        return importStream(context, repo, name, content)
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? = try {
        resolver.query(uri, arrayOf(
            android.provider.OpenableColumns.DISPLAY_NAME
        ), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }

    private fun readText(resolver: ContentResolver, uri: Uri): String = try {
        resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
    } catch (_: Exception) {
        ""
    }

    private fun saveToExports(context: Context, fileName: String, content: String): File {
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "import.json" }
        val ext = File(safe).extension.ifBlank { "json" }
        val base = safe.removeSuffix(ext).trimEnd('.')
        if (base.isBlank()) return WorkoutExporter.write(context, "import_${System.currentTimeMillis()}.json", content)
        return WorkoutExporter.write(context, "${base}_${System.currentTimeMillis()}.$ext", content)
    }

    // ------------------------------------------------------------------
    // GPX → sessions (pure, unit-testable)
    // ------------------------------------------------------------------

    fun importGpx(content: String): List<WorkoutSessionEntity> {
        val tracks = extractTracks(content)
        if (tracks.isEmpty()) return emptyList()
        val out = mutableListOf<WorkoutSessionEntity>()
        tracks.forEach { track ->
            val pts = track.points.filter { it.lat != 0.0 || it.lon != 0.0 }
            if (pts.size >= 2) {
                out.add(buildSessionFromPoints(pts, track.name, track.type))
            }
        }
        return out
    }

    internal data class GpxTrack(val name: String, val type: String, val points: List<GpsPoint>)

    private val TRK_REGEX = Regex("<trk>([\\s\\S]*?)</trk>", RegexOption.IGNORE_CASE)
    private val TRKPT_TAG = Regex("<trkpt\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ATTR_LAT = Regex("lat=\"([-\\d.]+)\"", RegexOption.IGNORE_CASE)
    private val ATTR_LON = Regex("lon=\"([-\\d.]+)\"", RegexOption.IGNORE_CASE)
    private val ELE_RX = Regex("<ele>([-\\d.]+)</ele>", RegexOption.IGNORE_CASE)
    private val TIME_RX = Regex("<time>([^<]+)</time>", RegexOption.IGNORE_CASE)

    internal fun extractTracks(content: String): List<GpxTrack> {
        return TRK_REGEX.findAll(content).mapNotNull { m ->
            val body = m.groupValues[1]
            val name = Regex("<name>([\\s\\S]*?)</name>", RegexOption.IGNORE_CASE)
                .find(body)?.groupValues?.get(1)?.trim()
                ?: "Imported"
            val type = Regex("<type>([\\s\\S]*?)</type>", RegexOption.IGNORE_CASE)
                .find(body)?.groupValues?.get(1)?.trim()?.uppercase(Locale.US)
                ?: "ROAD"

            // Handles both expanded <trkpt …>…</trkpt> and self-closing <trkpt …/>
            val pts = mutableListOf<GpsPoint>()
            var idx = 0
            while (true) {
                val tagMatch = TRKPT_TAG.find(body, idx) ?: break
                val tag = tagMatch.value
                val lat = ATTR_LAT.find(tag)?.groupValues?.get(1)?.toDoubleOrNull()
                    ?: return@mapNotNull null
                val lon = ATTR_LON.find(tag)?.groupValues?.get(1)?.toDoubleOrNull()
                    ?: return@mapNotNull null

                if (tag.trimEnd().endsWith("/>")) {
                    pts.add(GpsPoint(lat = lat, lon = lon))
                    idx = tagMatch.range.last + 1
                    continue
                }

                val contentStart = tagMatch.range.last + 1
                val nextTag = Regex("<trkpt\\b|</trk", RegexOption.IGNORE_CASE).find(body, contentStart)
                val contentEnd = nextTag?.range?.first ?: body.length
                val inner = body.substring(contentStart, contentEnd)
                val ele = ELE_RX.find(inner)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                val time = TIME_RX.find(inner)?.groupValues?.get(1)?.let { parseIsoTime(it) } ?: 0L
                pts.add(GpsPoint(lat = lat, lon = lon, alt = ele, timestamp = time))
                idx = contentEnd
            }

            if (pts.size >= 2) GpxTrack(name, type, pts) else null
        }.toList()
    }

    private fun parseIsoTime(s: String): Long {
        return try {
            val clean = s.trim().removeSuffix("Z").substringBefore(".")
            ISO_FORMAT.parse(clean)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    internal fun buildSessionFromPoints(
        pts: List<GpsPoint>,
        name: String,
        type: String
    ): WorkoutSessionEntity {
        val start = pts.first().timestamp
        val end = pts.last().timestamp
        val duration = if (end > start) (end - start) / 1000 else 0L

        var distance = 0.0
        var gain = 0.0
        for (i in 0 until pts.size - 1) {
            distance += haversine(pts[i], pts[i + 1])
            val dAlt = pts[i + 1].alt - pts[i].alt
            if (dAlt > 0) gain += dAlt
        }

        return WorkoutSessionEntity(
            workoutType = type.ifBlank { "ROAD" },
            startTime = start,
            endTime = end,
            durationSeconds = duration,
            distanceMeters = distance,
            totalSteps = 0,
            avgBpm = 0,
            maxBpm = 0,
            caloriesKcal = 0,   // never guessed — only real sensor data sets this
            elevationGainMeters = gain,
            isSyncedToPhone = false,
            routeGeoJson = encodeRoute(pts),
            splitsJson = ""
        )
    }

    // ------------------------------------------------------------------
    // MILES JSON workouts → sessions (pure)
    // ------------------------------------------------------------------

    fun importWorkoutsJson(content: String): List<WorkoutSessionEntity> {
        val out = mutableListOf<WorkoutSessionEntity>()
        try {
            val root = JSONObject(content)
            val arrs = when {
                root.optString("format") == WORKOUT_JSON_ALL ->
                    root.optJSONArray("workouts")
                root.optString("format") == WORKOUT_JSON_SINGLE ||
                    root.has("trackPoints") ->
                    JSONArray().put(root)
                else -> null
            }
            if (arrs == null) return emptyList()
            for (i in 0 until arrs.length()) {
                val o = arrs.optJSONObject(i) ?: continue
                val pts = mutableListOf<GpsPoint>()
                o.optJSONArray("trackPoints")?.let { ptsArr ->
                    for (j in 0 until ptsArr.length()) {
                        val p = ptsArr.optJSONObject(j) ?: continue
                        pts.add(
                            GpsPoint(
                                lat = p.optDouble("lat", 0.0),
                                lon = p.optDouble("lon", 0.0),
                                alt = p.optDouble("alt", 0.0),
                                speed = p.optDouble("speed", 0.0),
                                timestamp = p.optString("time", "").let {
                                    if (it.isBlank()) 0L else parseIsoTime(it)
                                }
                            )
                        )
                    }
                }
                val type = o.optString("type", "ROAD").ifBlank { "ROAD" }
                val session: WorkoutSessionEntity
                if (pts.size >= 2) {
                    session = buildSessionFromPoints(pts, type, type).copy(
                        startTime = o.optLong("startTimeMs", o.optLong("startTime", 0L))
                            .takeIf { it > 0 } ?: sessionStartFromIso(o),
                        durationSeconds = o.optLong("durationSeconds", 0L),
                        totalSteps = o.optInt("steps", 0),
                        avgBpm = o.optInt("avgBpm", 0),
                        maxBpm = o.optInt("maxBpm", 0),
                        caloriesKcal = o.optInt("caloriesKcal", 0),
                        elevationGainMeters = o.optDouble("elevationGainMeters", 0.0)
                    )
                } else {
                    session = WorkoutSessionEntity(
                        workoutType = type,
                        startTime = sessionStartFromIso(o),
                        endTime = 0L,
                        durationSeconds = o.optLong("durationSeconds", 0L),
                        distanceMeters = o.optDouble("distanceMeters", 0.0),
                        totalSteps = o.optInt("steps", 0),
                        avgBpm = o.optInt("avgBpm", 0),
                        maxBpm = o.optInt("maxBpm", 0),
                        caloriesKcal = o.optInt("caloriesKcal", 0),
                        elevationGainMeters = o.optDouble("elevationGainMeters", 0.0),
                        routeGeoJson = "",
                        splitsJson = ""
                    )
                }
                if (session.distanceMeters > 0.0 || pts.size >= 2) out.add(session)
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return out
    }

    private fun sessionStartFromIso(o: JSONObject): Long =
        o.optString("startTime", "").let { if (it.isBlank()) 0L else parseIsoTime(it) }

    internal fun encodeRoute(pts: List<GpsPoint>): String {
        if (pts.isEmpty()) return ""
        val sb = StringBuilder("[")
        pts.forEachIndexed { index, p ->
            sb.append("{\"lat\":${p.lat},\"lon\":${p.lon},\"alt\":${p.alt},\"speed\":${p.speed},\"time\":${p.timestamp}}")
            if (index < pts.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    internal fun haversine(a: GpsPoint, b: GpsPoint): Double {
        val earthR = 6371000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(a.lat)) * Math.cos(Math.toRadians(b.lat)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return 2 * earthR * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h))
    }
}