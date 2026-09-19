package com.example.miles.wear.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Vibrate on workout started: A sharp rising double pulse.
     */
    fun vibrateWorkoutStart(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 70, 60, 120)
            val amplitudes = intArrayOf(0, 180, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }

    /**
     * Vibrate on workout paused: Two gentle pulses.
     */
    fun vibrateWorkoutPause(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 80, 80, 80)
            val amplitudes = intArrayOf(0, 150, 0, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    /**
     * Vibrate on workout resumed: A single affirmative pulse.
     */
    fun vibrateWorkoutResume(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, 200))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    /**
     * Vibrate on workout finished: A triumphant three-pulse pattern.
     */
    fun vibrateWorkoutFinish(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 90, 70, 120, 70, 200)
            val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    /**
     * Vibrate on workout discarded: Short alert buzz.
     */
    fun vibrateWorkoutDiscard(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 60, 40, 60)
            val amplitudes = intArrayOf(0, 120, 0, 120)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }

    /**
     * Vibrate on kilometer / mile split: Distinct cadence pulse.
     */
    fun vibrateSplitCue(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 120, 100, 180)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    /**
     * Subtle click feedback for UI buttons.
     */
    fun vibrateClick(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, 100))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }
}
