package com.example.miles.wear

import com.example.miles.wear.sensor.CalorieEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Real-data calorie engine: HR zones, movement METs, and daily step calories. */
class CalorieEstimatorTest {

    @Test
    fun `heart rate zones burn more as intensity rises`() {
        val resting = CalorieEstimator.fromHeartRate(70)
        val aerobic = CalorieEstimator.fromHeartRate(130)
        val max = CalorieEstimator.fromHeartRate(190)
        assertTrue("aerobic should beat resting", aerobic > resting)
        assertTrue("max should beat aerobic", max > aerobic)
    }

    @Test
    fun `no heart rate falls back to resting rate instead of zero`() {
        // bpm 0 / absent sensor must still return a real non-zero baseline
        assertTrue(CalorieEstimator.fromHeartRate(0) > 0.0)
    }

    @Test
    fun `movement estimate rises with measured speed`() {
        val standing = CalorieEstimator.fromMovement(0.2, 0, 70)
        val walking = CalorieEstimator.fromMovement(1.5, 100, 70)
        val running = CalorieEstimator.fromMovement(5.0, 165, 70)
        assertTrue(walking > standing)
        assertTrue(running > walking)
    }

    @Test
    fun `real step cadence is used when GPS speed is missing`() {
        val byCadence = CalorieEstimator.fromMovement(0.0, 160, 70)
        val idle = CalorieEstimator.fromMovement(0.0, 0, 70)
        assertTrue("cadence should register as movement", byCadence > idle)
    }

    @Test
    fun `heavier body estimates more calories for the same movement`() {
        val light = CalorieEstimator.fromMovement(1.5, 100, 55)
        val heavy = CalorieEstimator.fromMovement(1.5, 100, 95)
        assertTrue(heavy > light)
    }

    @Test
    fun `absurd weights are clamped to a sane range`() {
        val tiny = CalorieEstimator.fromMovement(1.5, 100, 1)
        val normal = CalorieEstimator.fromMovement(1.5, 100, 30)
        assertEquals("weights below 30kg clamp to 30kg", normal, tiny, 0.0001)

        val huge = CalorieEstimator.fromMovement(1.5, 100, 9999)
        val maxed = CalorieEstimator.fromMovement(1.5, 100, 250)
        assertEquals(maxed, huge, 0.0001)
    }

    @Test
    fun `ten thousand steps gives a realistic everyday burn for 70kg`() {
        val kcal = CalorieEstimator.dailyActiveFromSteps(10_000, 70)
        // Typical trackers report ~400-500 kcal for 10k steps at 70 kg
        assertTrue("10k steps should be 380-480 kcal, was $kcal", kcal in 380..480)
    }

    @Test
    fun `daily step calories scale with body weight and step count`() {
        val tenK = CalorieEstimator.dailyActiveFromSteps(10_000, 70)
        val fiveK = CalorieEstimator.dailyActiveFromSteps(5_000, 70)
        val heavy = CalorieEstimator.dailyActiveFromSteps(10_000, 100)
        assertTrue(fiveK < tenK)
        assertTrue(heavy > tenK)
    }

    @Test
    fun `no steps means no everyday calories`() {
        assertEquals(0, CalorieEstimator.dailyActiveFromSteps(0, 70))
        assertEquals(0, CalorieEstimator.dailyActiveFromSteps(-5, 70))
    }
}