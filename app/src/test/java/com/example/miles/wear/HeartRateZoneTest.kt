package com.example.miles.wear

import com.example.miles.wear.data.model.HeartRateZone
import com.example.miles.wear.data.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Test

class HeartRateZoneTest {

    @Test
    fun testHeartRateZoneClassification() {
        assertEquals(HeartRateZone.RESTING, HeartRateZone.fromBpm(55))
        assertEquals(HeartRateZone.RESTING, HeartRateZone.fromBpm(90))
        assertEquals(HeartRateZone.WARMUP, HeartRateZone.fromBpm(100))
        assertEquals(HeartRateZone.WARMUP, HeartRateZone.fromBpm(114))
        assertEquals(HeartRateZone.AEROBIC, HeartRateZone.fromBpm(120))
        assertEquals(HeartRateZone.AEROBIC, HeartRateZone.fromBpm(134))
        assertEquals(HeartRateZone.THRESHOLD, HeartRateZone.fromBpm(145))
        assertEquals(HeartRateZone.THRESHOLD, HeartRateZone.fromBpm(154))
        assertEquals(HeartRateZone.ANAEROBIC, HeartRateZone.fromBpm(165))
        assertEquals(HeartRateZone.ANAEROBIC, HeartRateZone.fromBpm(174))
        assertEquals(HeartRateZone.MAX, HeartRateZone.fromBpm(180))
        assertEquals(HeartRateZone.MAX, HeartRateZone.fromBpm(200))
    }

    @Test
    fun testWorkoutTypes() {
        val types = WorkoutType.values()
        assertEquals(6, types.size)
        assertEquals("Running", WorkoutType.RUN.displayName)
        assertEquals("Walking", WorkoutType.WALK.displayName)
        assertEquals("Cycling", WorkoutType.CYCLING.displayName)
        assertEquals("Hiking", WorkoutType.HIKE.displayName)
        assertEquals("General", WorkoutType.GENERAL.displayName)
        assertEquals("Custom", WorkoutType.CUSTOM.displayName)
        // Backwards-compat aliases still resolve to real types
        assertEquals(WorkoutType.CYCLING, WorkoutType.RIDE)
        assertEquals(WorkoutType.GENERAL, WorkoutType.OTHER)
    }
}
