package com.example.miles.wear

import com.example.miles.wear.data.export.WorkoutExporter
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Export/backup: every format is built from real session data — no placeholders. */
class ExportBackupTest {

    private fun sampleSession() = WorkoutSessionEntity(
        id = 42,
        workoutType = "RUN",
        startTime = 1727121600000L, // 2024-09-23 20:00 UTC
        endTime = 1727123400000L,
        durationSeconds = 1800,
        totalSteps = 2500,
        avgBpm = 148,
        maxBpm = 172,
        caloriesKcal = 210,
        distanceMeters = 3120.6,
        elevationGainMeters = 42.3,
        routeGeoJson = """[
            {"lat":51.5074,"lon":-0.1278,"alt":30.0,"speed":2.6,"time":1727121600000},
            {"lat":51.5075,"lon":-0.1276,"alt":32.0,"speed":2.8,"time":1727121660000}
        ]"""
    )

    @Test
    fun `GPX contains the real track points and session metadata`() {
        val gpx = WorkoutExporter.gpxForSession(sampleSession())
        assertTrue(gpx.contains("<gpx creator=\"MILES Wear OS\" version=\"1.1\""))
        assertTrue(gpx.contains("<name>RUN</name>"))
        assertTrue(gpx.contains("lat=\"51.5074000\" lon=\"-0.1278000\""))
        assertTrue(gpx.contains("<ele>30.0</ele>"))
        assertTrue(gpx.contains("<time>2024-09-23T20:00:00Z</time>"))
        assertTrue(gpx.endsWith("</gpx>\n"))
    }

    @Test
    fun `bulk GPX contains one track per session`() {
        val gpx = WorkoutExporter.gpxForAll(listOf(sampleSession(), sampleSession().copy(id = 43)))
        assertEquals(2, "  <trk>".toRegex().findAll(gpx).count())
    }

    @Test
    fun `CSV summary row carries real aggregate numbers`() {
        val csv = WorkoutExporter.csvForAll(listOf(sampleSession()))
        val lines = csv.trim().split("\n")
        assertEquals(2, lines.size)
        assertEquals("date,type,duration_seconds,distance_km,steps,avg_bpm,max_bpm,calories_kcal,elevation_gain_m", lines[0])
        val row = lines[1].split(",")
        assertEquals("RUN", row[1])
        assertEquals("1800", row[2])
        assertEquals("3.121", row[3])
        assertEquals("2500", row[4])
        assertEquals("148", row[5])
        assertEquals("172", row[6])
        assertEquals("210", row[7])
    }

    @Test
    fun `point CSV exports every track point`() {
        val csv = WorkoutExporter.csvPoints(sampleSession())
        val lines = csv.trim().split("\n")
        assertEquals(3, lines.size)
        assertTrue(lines[1].startsWith("2024-09-23T20:00:00Z,51.5074000,-0.1278000,30.0,2.60"))
    }

    @Test
    fun `JSON export is valid JSON with the real track point count`() {
        val json = WorkoutExporter.jsonForSession(sampleSession())
        val obj = JSONObject(json)
        assertEquals("RUN", obj.getString("type"))
        assertEquals(3120.6, obj.getDouble("distanceMeters"), 0.001)
        assertEquals(2, obj.getJSONArray("trackPoints").length())
        assertEquals(51.5074, obj.getJSONArray("trackPoints").getJSONObject(0).getDouble("lat"), 0.0001)
    }

    @Test
    fun `bulk JSON wraps all sessions`() {
        val json = WorkoutExporter.jsonForAll(listOf(sampleSession(), sampleSession().copy(id = 7)))
        val obj = JSONObject(json)
        assertEquals("MILES-WEAROS-workouts", obj.getString("format"))
        assertEquals(2, obj.getJSONArray("workouts").length())
    }

    @Test
    fun `voice cue wording reads naturally with real distances`() {
        assertEquals(
            "Turn left onto High Street in 320 meters",
            com.example.miles.wear.engine.NavigationVoice.cue("Turn left", "High Street", 325.0)
        )
        assertEquals(
            "You have arrived",
            com.example.miles.wear.engine.NavigationVoice.cue("Arrive", "", 0.0)
        )
        assertEquals(
            "Head east in 1.2 kilometers",
            com.example.miles.wear.engine.NavigationVoice.cue("Head east", "", 1240.0)
        )
    }
}