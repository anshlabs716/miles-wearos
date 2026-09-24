package com.example.miles.wear.engine

import com.example.miles.wear.data.model.WorkoutSplit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real per-km / per-mi auto splits. A split is captured the moment the live
 * tracked distance crosses another interval boundary — every value comes from
 * the actual workout (distance meters + elapsed seconds), never invented.
 */
object Splitter {

    /** True if a new split boundary was crossed; appends it when so. */
    fun append(
        currentDistanceMeters: Double,
        currentElapsedSeconds: Long,
        lastMarkMeters: Double,
        splits: MutableList<WorkoutSplit>,
        intervalMeters: Double
    ): Boolean {
        if (currentDistanceMeters <= 0.0 || intervalMeters <= 0.0) return false
        if (currentDistanceMeters - lastMarkMeters < intervalMeters) return false
        val nextIndex = splits.size + 1
        splits.add(
            WorkoutSplit(
                index = nextIndex,
                cumulativeMeters = currentDistanceMeters,
                elapsedSeconds = currentElapsedSeconds
            )
        )
        return true
    }

    fun encode(splits: List<WorkoutSplit>): String {
        if (splits.isEmpty()) return ""
        val arr = JSONArray()
        splits.forEach { s ->
            arr.put(
                JSONObject()
                    .put("i", s.index)
                    .put("m", s.cumulativeMeters)
                    .put("t", s.elapsedSeconds)
            )
        }
        return arr.toString()
    }

    fun decode(json: String?): List<WorkoutSplit> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                WorkoutSplit(
                    index = o.optInt("i", i + 1),
                    cumulativeMeters = o.optDouble("m", 0.0),
                    elapsedSeconds = o.optLong("t", 0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Format seconds-per-km as mm:ss pace. */
    fun formatPace(secondsPerKm: Double): String {
        if (secondsPerKm <= 0.0) return "--:--"
        val total = secondsPerKm.toInt()
        return String.format(java.util.Locale.US, "%d:%02d", total / 60, total % 60)
    }
}