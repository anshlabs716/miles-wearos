package com.example.miles.wear

import com.example.miles.wear.data.imports.WorkoutImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Wave D: real-data import — GPX + MILES JSON + backup detection. */
class DataImportTest {

    private val singleTrackGpx = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="test">
          <trk>
            <name>Morning Run</name>
            <type>running</type>
            <trkseg>
              <trkpt lat="37.7749" lon="-122.4194">
                <ele>15.0</ele>
                <time>2024-05-01T08:00:00Z</time>
              </trkpt>
              <trkpt lat="37.7755" lon="-122.4180">
                <ele>16.5</ele>
                <time>2024-05-01T08:03:00Z</time>
              </trkpt>
              <trkpt lat="37.7761" lon="-122.4166">
                <ele>18.0</ele>
                <time>2024-05-01T08:06:00Z</time>
              </trkpt>
            </trkseg>
          </trk>
        </gpx>
    """.trimIndent()

    @Test
    fun `gpx single track imports one real session with distance and route`() {
        val sessions = WorkoutImporter.importGpx(singleTrackGpx)
        assertEquals(1, sessions.size)
        val s = sessions[0]
        assertTrue("distance should be real meters", s.distanceMeters > 0.0)
        assertEquals("RUNNING", s.workoutType)
        assertEquals(360L, s.durationSeconds) // 6 minutes of real timestamps
        assertTrue("elevation gain captured", s.elevationGainMeters in 2.9..3.1)
        assertTrue("route stored", s.routeGeoJson.startsWith("[{\"lat\":37.7749"))
        assertEquals(3, com.example.miles.wear.sensor.SensorTracker.parseRouteJson(s.routeGeoJson).size)
        assertEquals(0, s.caloriesKcal) // never guessed
    }

    @Test
    fun `gpx multi-track file imports every track`() {
        val gpx = singleTrackGpx.replace(
            "</gpx>",
            "<trk><name>Bike</name><type>cycling</type><trkseg>" +
                "<trkpt lat=\"10.0\" lon=\"20.0\"/><trkpt lat=\"10.001\" lon=\"20.002\"/>" +
                "</trkseg></trk></gpx>"
        )
        val sessions = WorkoutImporter.importGpx(gpx)
        assertEquals(2, sessions.size)
        assertEquals("CYCLING", sessions[1].workoutType)
        assertTrue(sessions[1].routeGeoJson.isNotBlank())
    }

    @Test
    fun `self-closing trkpt points are supported`() {
        val gpx = """<gpx><trk><trkseg>
            <trkpt lat="1.0" lon="2.0"/>
            <trkpt lat="1.001" lon="2.001"/>
            <trkpt lat="1.002" lon="2.002"/>
        </trkseg></trk></gpx>"""
        val sessions = WorkoutImporter.importGpx(gpx)
        assertEquals(1, sessions.size)
        assertTrue(sessions[0].distanceMeters > 0.0)
        assertEquals(3, com.example.miles.wear.sensor.SensorTracker.parseRouteJson(sessions[0].routeGeoJson).size)
    }

    @Test
    fun `gpx without tracks imports nothing`() {
        assertEquals(0, WorkoutImporter.importGpx("<gpx><wpt lat=\"1\" lon=\"2\"/></gpx>").size)
    }

    @Test
    fun `MILES single workout json imports with real numbers`() {
        val json = """
            {
              "format": "MILES-WEAROS-workout",
              "type": "ROAD",
              "durationSeconds": 900,
              "distanceMeters": 2100.0,
              "avgBpm": 0,
              "caloriesKcal": 0,
              "trackPoints": [
                {"lat": 37.7749, "lon": -122.4194, "alt": 10.0, "speed": 2.0, "time": "2024-05-01T08:00:00Z"},
                {"lat": 37.7760, "lon": -122.4170, "alt": 11.0, "speed": 2.5, "time": "2024-05-01T08:15:00Z"}
              ]
            }
        """.trimIndent()
        val sessions = WorkoutImporter.importWorkoutsJson(json)
        assertEquals(1, sessions.size)
        val s = sessions[0]
        assertTrue(s.distanceMeters > 0.0)
        assertEquals(900L, s.durationSeconds)
        assertTrue(s.routeGeoJson.contains("\"time\""))
    }

    @Test
    fun `MILES workout array json imports each workout`() {
        val json = """
            {
              "format": "MILES-WEAROS-workouts",
              "workouts": [
                {"format":"MILES-WEAROS-workout","type":"ROAD","durationSeconds":600,"distanceMeters":1200.0,
                 "trackPoints":[{"lat":1.0,"lon":2.0,"time":"2024-05-01T09:00:00Z"},{"lat":1.01,"lon":2.01,"time":"2024-05-01T09:10:00Z"}]},
                {"format":"MILES-WEAROS-workout","type":"HIKE","durationSeconds":300,"distanceMeters":400.0,
                 "trackPoints":[{"lat":3.0,"lon":4.0,"time":"2024-05-02T09:00:00Z"},{"lat":3.005,"lon":4.005,"time":"2024-05-02T09:05:00Z"}]}
              ]
            }
        """.trimIndent()
        val sessions = WorkoutImporter.importWorkoutsJson(json)
        assertEquals(2, sessions.size)
        assertEquals("HIKE", sessions[1].workoutType)
    }

    @Test
    fun `backup format is detected`() {
        assertTrue(WorkoutImporter.isBackupContent("{\"format\":\"MILES-WEAROS-backup\",\"sessions\":[]}"))
        assertFalse(WorkoutImporter.isBackupContent("{\"format\":\"MILES-WEAROS-workout\"}"))
    }
}