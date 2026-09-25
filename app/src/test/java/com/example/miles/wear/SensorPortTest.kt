package com.example.miles.wear

import com.example.miles.wear.data.model.WorkoutSplit
import com.example.miles.wear.engine.Splitter
import com.example.miles.wear.sensor.BleSensorCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Wave C: BLE payload decoding + real auto-split logic. */
class SensorPortTest {

    // ----- BLE Heart Rate 0x2A37 -----
    @Test
    fun `8-bit HR measurement parses the second byte`() {
        assertEquals(90, BleSensorCodec.parseHeartRate(byteArrayOf(0x00, 90)))
    }

    @Test
    fun `16-bit HR measurement parses little-endian`() {
        // flags=0x01, bpm = 0x0156 = 342
        assertEquals(342, BleSensorCodec.parseHeartRate(byteArrayOf(0x01, 0x56.toByte(), 0x01)))
    }

    @Test
    fun `zero HR is rejected as no signal`() {
        assertNull(BleSensorCodec.parseHeartRate(byteArrayOf(0x00, 0x00)))
    }

    @Test
    fun `short packet returns null`() {
        assertNull(BleSensorCodec.parseHeartRate(byteArrayOf(0x00)))
    }

    // ----- BLE Cadence 0x2A5B -----
    @Test
    fun `cadence without flags parses single byte`() {
        assertEquals(84, BleSensorCodec.parseCadence(byteArrayOf(0x00, 84)))
    }

    @Test
    fun `cadence skips wheel revs and event time when flagged`() {
        // flags = 0x03 → 4 rev bytes + 2 event-time bytes, then cadence byte
        val data = byteArrayOf(
            0x03,
            0x10, 0x00, 0x00, 0x00, // cumulative wheel revs
            0x34, 0x12,             // last event time
            156.toByte()            // instantaneous cadence (156 as byte)
        )
        assertEquals(156, BleSensorCodec.parseCadence(data))
    }

    // ----- Battery 0x2A19 -----
    @Test
    fun `battery level parses`() {
        assertEquals(87, BleSensorCodec.parseBattery(byteArrayOf(87)))
    }

    // ----- Auto splits -----
    @Test
    fun `split is captured exactly at the distance interval`() {
        val splits = mutableListOf<WorkoutSplit>()
        var mark = 0.0
        // nothing below 1000 m
        assertFalse(Splitter.append(999.0, 400L, mark, splits, 1000.0))
        assertTrue(splits.isEmpty())
        // crossing 1000 m
        assertTrue(Splitter.append(1002.5, 402L, mark, splits, 1000.0))
        assertEquals(1, splits.size)
        assertEquals(1, splits[0].index)
        assertEquals(1002.5, splits[0].cumulativeMeters, 0.001)
        mark = splits.last().cumulativeMeters
        // second split only after another full interval (mark = 1002.5 → next boundary 2002.5)
        assertFalse(Splitter.append(1990.0, 800L, mark, splits, 1000.0))
        assertTrue(Splitter.append(2010.0, 805L, mark, splits, 1000.0))
        assertEquals(2, splits.size)
    }

    @Test
    fun `splits encode and decode round-trip losslessly`() {
        val splits = listOf(
            WorkoutSplit(1, 1002.5, 402),
            WorkoutSplit(2, 2001.0, 805),
            WorkoutSplit(3, 3004.25, 1200)
        )
        val decoded = Splitter.decode(Splitter.encode(splits))
        assertEquals(splits, decoded)
    }

    @Test
    fun `split pace is real seconds per km`() {
        val split = WorkoutSplit(1, 1000.0, 300)
        assertEquals(300.0, split.paceSecondsPerKm(), 0.001)
        assertEquals("5:00", Splitter.formatPace(300.0))
        assertEquals("--:--", Splitter.formatPace(0.0))
    }
}