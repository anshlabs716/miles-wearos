package com.example.miles.wear

import com.example.miles.wear.network.LanProtocol
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** GMS-free local-network protocol: beacon + frame round-trips. */
class LanProtocolTest {

    @Test
    fun `beacon round-trips identity and real status`() {
        val beacon = LanProtocol.Beacon(
            role = LanProtocol.ROLE_WEAR,
            id = "wear-abc123",
            name = "Pixel Watch 4",
            model = "Pixel Watch 4",
            tcpPort = 51234,
            appVersion = "1.5.0",
            battery = 76,
            status = "active"
        )
        val parsed = LanProtocol.parseBeacon(LanProtocol.encodeBeacon(beacon))
        assertNotNull(parsed)
        assertEquals("wear-abc123", parsed!!.id)
        assertEquals("Pixel Watch 4", parsed.name)
        assertEquals(51234, parsed.tcpPort)
        assertEquals("1.5.0", parsed.appVersion)
        assertEquals(76, parsed.battery)
        assertEquals("active", parsed.status)
        assertEquals(LanProtocol.PROTO_VERSION, parsed.proto)
    }

    @Test
    fun `foreign and broken payloads are ignored`() {
        assertNull(LanProtocol.parseBeacon("""{"app":"something-else"}"""))
        assertNull(LanProtocol.parseBeacon("not json at all"))
        assertNull(LanProtocol.parseBeacon(""))
    }

    @Test
    fun `message frame round-trips path and payload`() {
        val payload = JSONObject().put("bpm", 142).put("accuracy", 3)
        val frame = LanProtocol.encodeMessage(LanProtocol.PATH_HR_STREAM, payload.toString())
        val (path, parsedPayload) = LanProtocol.parseMessage(frame)!!
        assertEquals(LanProtocol.PATH_HR_STREAM, path)
        assertEquals(142, parsedPayload.optInt("bpm"))
        assertEquals(3, parsedPayload.optInt("accuracy"))
    }

    @Test
    fun `malformed frames are ignored`() {
        assertNull(LanProtocol.parseMessage("not json"))
        assertNull(LanProtocol.parseMessage("""{"payload":{}}"""))
    }

    @Test
    fun `hello introduces both roles with the same fields as beacons`() {
        val beacon = LanProtocol.Beacon(
            role = LanProtocol.ROLE_PHONE,
            id = "phone-xyz",
            name = "Pixel 6 Pro",
            model = "Pixel 6 Pro",
            tcpPort = 40001,
            appVersion = "1.0.3",
            battery = 55
        )
        val fromHello = LanProtocol.beaconFromHello(LanProtocol.helloPayload(beacon))!!
        assertEquals(beacon.id, fromHello.id)
        assertEquals(beacon.role, fromHello.role)
        assertEquals(beacon.tcpPort, fromHello.tcpPort)
        assertEquals(beacon.battery, fromHello.battery)
    }

    @Test
    fun `wear and phone message paths match across both apps`() {
        // These strings are the shared contract with the phone app
        assertEquals("/miles/sensors/hr", LanProtocol.PATH_HR_STREAM)
        assertEquals("/miles/sensors/cadence", LanProtocol.PATH_CADENCE_STREAM)
        assertEquals("/miles/workout/control", LanProtocol.PATH_WORKOUT_CONTROL)
        assertEquals("/miles/metrics/live", LanProtocol.PATH_PHONE_METRICS)
        assertEquals("/miles/queue/flush", LanProtocol.PATH_FLUSH_QUEUE)
        assertTrue(LanProtocol.DISCOVERY_PORT > 0)
    }
}