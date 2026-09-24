package com.example.miles.wear.data.export

import android.content.Context
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.GpsPoint
import com.example.miles.wear.sensor.SensorTracker
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Real-data exports — GPX / CSV / JSON built from actual stored sessions
 * and their recorded GPS track points. Nothing is invented.
 */
object WorkoutExporter {

    private fun isoTime(epochMs: Long): String {
        if (epochMs <= 0) return ""
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(epochMs))
    }

    private fun pointsOf(session: WorkoutSessionEntity): List<GpsPoint> =
        SensorTracker.parseRouteJson(session.routeGeoJson)

    // ----- GPX -----

    /** One session as a full GPX 1.1 track (real track points). */
    fun gpxForSession(session: WorkoutSessionEntity): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx creator=\"MILES Wear OS\" version=\"1.1\" " +
            "xmlns=\"http://www.topografix.com/GPX/1/1\" " +
            "xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\">\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>${esc(session.workoutType)} ${sessionDate(session.startTime)}</name>\n")
        sb.append("    <time>${isoTime(session.startTime)}</time>\n")
        sb.append("  </metadata>\n")
        appendTrack(sb, session)
        sb.append("</gpx>\n")
        return sb.toString()
    }

    /** Every session as one multi-track GPX file. */
    fun gpxForAll(sessions: List<WorkoutSessionEntity>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx creator=\"MILES Wear OS\" version=\"1.1\" " +
            "xmlns=\"http://www.topografix.com/GPX/1/1\" " +
            "xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\">\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>MILES Wear OS — all workouts</name>\n")
        sb.append("  </metadata>\n")
        sessions.forEach { appendTrack(sb, it) }
        sb.append("</gpx>\n")
        return sb.toString()
    }

    private fun appendTrack(sb: StringBuilder, session: WorkoutSessionEntity) {
        sb.append("  <trk>\n")
        sb.append("    <name>${esc(session.workoutType)}</name>\n")
        sb.append("    <type>${esc(session.workoutType.lowercase())}</type>\n")
        sb.append("    <trkseg>\n")
        pointsOf(session).forEach { p ->
            sb.append("      <trkpt lat=\"${fmt(p.lat)}\" lon=\"${fmt(p.lon)}\">\n")
            if (p.alt != 0.0) sb.append("        <ele>${fmt1(p.alt)}</ele>\n")
            val t = isoTime(p.timestamp)
            if (t.isNotEmpty()) sb.append("        <time>$t</time>\n")
            if (p.speed > 0.0) sb.append("        <gpxtpx:extensions>\n          <gpxtpx:TrackPointExtension>\n            <gpxtpx:speed>${fmt2(p.speed)}</gpxtpx:speed>\n          </gpxtpx:TrackPointExtension>\n        </gpxtpx:extensions>\n")
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
    }

    // ----- CSV -----

    /** One summary row per session (works standalone or combined). */
    fun csvSummaryRow(session: WorkoutSessionEntity): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(session.startTime))
        return listOf(
            date,
            session.workoutType,
            session.durationSeconds,
            fmt3(session.distanceMeters / 1000.0),
            session.totalSteps,
            session.avgBpm,
            session.maxBpm,
            session.caloriesKcal,
            fmt1(session.elevationGainMeters)
        ).joinToString(",")
    }

    fun csvHeader(): String =
        "date,type,duration_seconds,distance_km,steps,avg_bpm,max_bpm,calories_kcal,elevation_gain_m"

    fun csvForAll(sessions: List<WorkoutSessionEntity>): String {
        val sb = StringBuilder(csvHeader()).append("\n")
        sessions.forEach { sb.append(csvSummaryRow(it)).append("\n") }
        return sb.toString()
    }

    /** Per-track-point CSV for one session: time,lat,lon,alt_m,speed_mps. */
    fun csvPoints(session: WorkoutSessionEntity): String {
        val sb = StringBuilder("time,lat,lon,altitude_m,speed_mps\n")
        pointsOf(session).forEach { p ->
            sb.append(isoTime(p.timestamp))
                .append(",").append(fmt(p.lat))
                .append(",").append(fmt(p.lon))
                .append(",").append(fmt1(p.alt))
                .append(",").append(fmt2(p.speed))
                .append("\n")
        }
        return sb.toString()
    }

    // ----- Native JSON -----

    fun jsonForSession(session: WorkoutSessionEntity): String {
        val obj = JSONObject()
            .put("format", "MILES-WEAROS-workout")
            .put("id", session.id)
            .put("type", session.workoutType)
            .put("startTime", isoTime(session.startTime))
            .put("endTime", isoTime(session.endTime))
            .put("durationSeconds", session.durationSeconds)
            .put("distanceMeters", session.distanceMeters)
            .put("steps", session.totalSteps)
            .put("avgBpm", session.avgBpm)
            .put("maxBpm", session.maxBpm)
            .put("caloriesKcal", session.caloriesKcal)
            .put("elevationGainMeters", session.elevationGainMeters)
        val arr = JSONArray()
        pointsOf(session).forEach { p ->
            arr.put(
                JSONObject()
                    .put("lat", p.lat)
                    .put("lon", p.lon)
                    .put("alt", p.alt)
                    .put("speed", p.speed)
                    .put("time", isoTime(p.timestamp))
            )
        }
        obj.put("trackPoints", arr)
        return obj.toString(2)
    }

    fun jsonForAll(sessions: List<WorkoutSessionEntity>): String {
        val arr = JSONArray()
        sessions.forEach { arr.put(JSONObject(jsonForSession(it))) }
        return JSONObject()
            .put("format", "MILES-WEAROS-workouts")
            .put("workouts", arr)
            .toString(2)
    }

    private fun sessionDate(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMs))

    private fun fmt(value: Double): String = String.format(Locale.US, "%.7f", value)
    private fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
    private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)
    private fun fmt3(value: Double): String = String.format(Locale.US, "%.3f", value)

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    // ----- File storage (app-specific external directory, no permissions) -----

    fun dir(context: Context): File =
        (context.getExternalFilesDir(null) ?: context.filesDir).resolve("exports").apply { mkdirs() }

    fun write(context: Context, fileName: String, content: String): File {
        val file = dir(context).resolve(fileName)
        file.writeText(content)
        return file
    }

    fun list(context: Context): List<File> =
        dir(context).listFiles { f -> f.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun delete(context: Context, fileName: String) {
        dir(context).resolve(fileName).delete()
    }

    fun share(context: Context, file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, file.name)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(send, "Export " + file.name))
        } catch (_: Exception) {
            // No share targets (e.g. watch with no apps) — file stays on device.
        }
    }
}