package com.example.miles.wear

import com.example.miles.wear.data.model.DistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceUnitTest {

    @Test
    fun testMetricDistanceFormatting() {
        val unit = DistanceUnit.METRIC
        assertEquals("5.70", unit.formatDistance(5700.0))
        assertEquals("1.02", unit.formatDistance(1020.0))
        assertEquals("0.00", unit.formatDistance(0.0))
    }

    @Test
    fun testImperialDistanceFormatting() {
        val unit = DistanceUnit.IMPERIAL
        // 5000 meters ~ 3.11 miles
        assertEquals("3.11", unit.formatDistance(5000.0))
        // 1609.34 meters ~ 1.00 mile
        assertEquals("1.00", unit.formatDistance(1609.34))
    }

    @Test
    fun testPaceFormatting() {
        val unit = DistanceUnit.METRIC
        // 1000m in 300s (5 min) -> 5:00 /km
        assertEquals("5:00", unit.formatPace(1000.0, 300L))
        // 1000m in 338s (5 min 38 sec) -> 5:38 /km
        assertEquals("5:38", unit.formatPace(1000.0, 338L))
    }
}
