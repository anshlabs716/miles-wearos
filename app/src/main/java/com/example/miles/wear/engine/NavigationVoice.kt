package com.example.miles.wear.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Hands-free spoken navigation cues (TextToSpeech). Every cue is a real
 * maneuver from the routing engine — never made up. Respects the
 * "voice_nav_enabled" setting, and is silent by default when turned off.
 */
object NavigationVoice {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var initContext: Context? = null

    fun init(context: Context) {
        if (tts != null) return
        initContext = context.applicationContext
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                tts?.language = Locale.getDefault()
            }
        } catch (e: Exception) {
            Log.w("NavigationVoice", "TTS unavailable", e)
        }
    }

    private fun enabled(): Boolean {
        val ctx = initContext ?: return false
        val prefs = ctx.getSharedPreferences("miles_wear_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("voice_nav_enabled", true)
    }

    fun speak(text: String) {
        if (!enabled()) return
        init(initContext ?: return)
        if (!ready || text.isBlank()) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "miles_nav_" + System.currentTimeMillis())
        } catch (e: Exception) {
            Log.w("NavigationVoice", "TTS speak failed", e)
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    /** Build a natural spoken cue from a maneuver + street name. */
    fun cue(maneuver: String, streetName: String, distanceToNextMeters: Double): String {
        val dist = when {
            distanceToNextMeters <= 0.0 -> ""
            distanceToNextMeters >= 1000.0 -> " in ${
                String.format(java.util.Locale.US, "%.1f", distanceToNextMeters / 1000.0)
            } kilometers"
            else -> " in ${(distanceToNextMeters / 10).toInt() * 10} meters"
        }
        return when {
            maneuver == "Arrive" -> "You have arrived"
            maneuver.startsWith("Head") -> "$maneuver$dist"
            streetName.isNotBlank() && maneuver != "Follow route" -> "$maneuver onto $streetName$dist"
            else -> "$maneuver$dist"
        }
    }
}