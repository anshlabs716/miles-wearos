package com.example.miles.wear.sensor

/**
 * Pure BLE GATT payload decoders (Bluetooth SIG profiles):
 * Heart Rate Measurement 0x2A37 and Cycling Speed & Cadence 0x2A5B.
 * Separated from the manager so the byte parsing is unit-testable.
 */
object BleSensorCodec {

    /** UUIDs for the standard heart-rate + cadence GATT profiles. */
    const val HR_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb"
    const val HR_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
    const val CSC_SERVICE = "00001816-0000-1000-8000-00805f9b34fb"
    const val CSC_MEASUREMENT = "00002a5b-0000-1000-8000-00805f9b34fb"
    const val BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb"
    const val BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb"
    const val CCCD = "00002902-0000-1000-8000-00805f9b34fb"

    /**
     * Heart Rate Measurement: flags byte then BPM.
     * Flags bit 0 = 0 → 8-bit BPM, bit 0 = 1 → 16-bit little-endian BPM.
     */
    fun parseHeartRate(data: ByteArray): Int? {
        if (data.size < 2) return null
        val flags = data[0].toInt() and 0xFF
        val bpm = if ((flags and 0x01) != 0) {
            if (data.size < 3) return null
            (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        } else {
            data[1].toInt() and 0xFF
        }
        return if (bpm == 0) null else bpm
    }

    /**
     * CSC Measurement: flags, optional cumulative wheel revs (bit 0) and
     * last event time (bit 1), then instantaneous cadence (strides/min × 2)
     * when present.
     */
    fun parseCadence(data: ByteArray): Int? {
        if (data.size < 2) return null
        val flags = data[0].toInt() and 0xFF
        var offset = 1
        if ((flags and 0x01) != 0) offset += 4 // cumulative wheel revolutions
        if ((flags and 0x02) != 0) offset += 2 // last event time
        if (offset >= data.size) return null
        val raw = data[offset].toInt() and 0xFF
        return if (raw == 0) null else raw
    }

    fun parseBattery(data: ByteArray): Int? {
        if (data.isEmpty()) return null
        val level = data[0].toInt() and 0xFF
        return if (level in 0..100) level else null
    }
}