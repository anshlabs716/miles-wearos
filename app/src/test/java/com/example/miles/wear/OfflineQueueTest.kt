package com.example.miles.wear

import com.example.miles.wear.data.local.entity.QueueItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQueueTest {

    @Test
    fun testQueueItemEntityCreation() {
        val payload = """{"bpm":145,"accuracy":4,"timestamp":1700000000}"""
        val entity = QueueItemEntity(
            type = "HR",
            path = "/miles/sensors/hr",
            payloadJson = payload,
            timestamp = 1700000000L,
            isSynced = false
        )

        assertEquals("HR", entity.type)
        assertEquals("/miles/sensors/hr", entity.path)
        assertEquals(payload, entity.payloadJson)
        assertFalse(entity.isSynced)

        val updated = entity.copy(isSynced = true)
        assertTrue(updated.isSynced)
    }
}
