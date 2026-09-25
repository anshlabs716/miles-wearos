package com.example.miles.wear.sensor

import com.example.miles.wear.data.model.HeartRateZone

/**
 * Calorie engine — always driven by real sensor data, never invented.
 *
 * Two real signals are used, in order of accuracy:
 *  1. **Measured heart rate** (watch HR sensor or BLE strap) → HR-zone burn rate.
 *  2. **Measured movement** (GPS speed and/or real step cadence) → MET estimate
 *     via the standard equation `kcal/min = MET * 3.5 * kg / 200`.
 *
 * Anything derived from movement is an *estimate* (no heart rate was measured),
 * so callers surface `caloriesEstimated = true` instead of pretending it is exact.
 */
object CalorieEstimator {

    /** kcal per second from a real heart-rate reading. */
    fun fromHeartRate(bpm: Int): Double = when (HeartRateZone.fromBpm(bpm)) {
        HeartRateZone.RESTING -> 0.02   // ~1.2 kcal/min
        HeartRateZone.WARMUP -> 0.08    // ~4.8 kcal/min
        HeartRateZone.AEROBIC -> 0.15   // ~9.0 kcal/min
        HeartRateZone.THRESHOLD -> 0.22 // ~13.2 kcal/min
        HeartRateZone.ANAEROBIC -> 0.28 // ~16.8 kcal/min
        HeartRateZone.MAX -> 0.35       // ~21 kcal/min
    }

    /** kcal per second from real movement when no heart rate is available. */
    fun fromMovement(speedMps: Double, cadenceSpm: Int, weightKg: Int): Double {
        val met = metForMovement(speedMps, cadenceSpm)
        val kcalPerMinute = met * 3.5 * clampWeight(weightKg) / 200.0
        return kcalPerMinute / 60.0
    }

    /**
     * MET value chosen from what the device actually measured.
     * GPS speed wins when present; real step cadence is the fallback.
     */
    fun metForMovement(speedMps: Double, cadenceSpm: Int): Double = when {
        speedMps >= 4.5 -> 8.0   // running
        speedMps >= 2.5 -> 5.8   // brisk walk / light jog
        speedMps >= 1.4 -> 3.5   // walking
        speedMps >= 0.8 -> 2.8   // slow walk
        cadenceSpm >= 140 -> 8.0
        cadenceSpm >= 110 -> 5.8
        cadenceSpm >= 90 -> 3.5
        cadenceSpm >= 60 -> 2.8
        else -> 1.3              // standing / light movement
    }

    /**
     * Everyday active calories from the real daily step count.
     * Per-step energy uses walking MET (3.5) at a ~100 steps/min cadence, which
     * lands around 400–450 kcal per 10k steps for a 70 kg person.
     */
    fun dailyActiveFromSteps(steps: Int, weightKg: Int): Int {
        if (steps <= 0) return 0
        val kcalPerStep = 3.5 * 3.5 * clampWeight(weightKg) / 200.0 / 100.0
        return (steps * kcalPerStep).toInt().coerceAtLeast(0)
    }

    private fun clampWeight(weightKg: Int): Double = weightKg.coerceIn(30, 250).toDouble()
}
