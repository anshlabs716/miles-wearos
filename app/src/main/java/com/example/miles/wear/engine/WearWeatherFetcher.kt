package com.example.miles.wear.engine

import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Parsed current-weather snapshot for the watch dashboard. */
data class WearWeather(
    val temperatureC: Double? = null,
    val condition: String? = null,
    val emoji: String? = null,
    val windSpeedKmh: Double? = null
) {
    val isAvailable: Boolean
        get() = temperatureC != null && condition != null
}

/**
 * Real current weather from Open-Meteo (free, no API key, no account).
 * Uses the watch's last known GPS/network location; returns an empty
 * [WearWeather] when location or network is unavailable.
 */
object WearWeatherFetcher {

    suspend fun fetch(context: Context): WearWeather = withContext(Dispatchers.IO) {
        val loc = lastKnownLocation(context)
        if (loc == null) return@withContext WearWeather()

        val endpoint = StringBuilder("https://api.open-meteo.com/v1/forecast")
            .append("?latitude=").append(loc.latitude)
            .append("&longitude=").append(loc.longitude)
            .append("&current=temperature_2m,weather_code,wind_speed_10m")
            .append("&timezone=auto")
            .append("&forecast_days=1")

        val body: String = try {
            val conn = (URL(endpoint.toString()).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            return@withContext WearWeather()
        }

        try {
            val root = JSONObject(body)
            val current = root.optJSONObject("current") ?: return@withContext WearWeather()
            val temp = current.optDouble("temperature_2m", Double.NaN)
            val code = current.optInt("weather_code", -1)
            val wind = current.optDouble("wind_speed_10m", Double.NaN)
            if (temp.isNaN() || code < 0) return@withContext WearWeather()

            val (condition, emoji) = describe(code)
            WearWeather(
                temperatureC = temp,
                condition = condition,
                emoji = emoji,
                windSpeedKmh = if (wind.isNaN()) null else wind
            )
        } catch (e: Exception) {
            WearWeather()
        }
    }

    private fun lastKnownLocation(context: Context): android.location.Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /** WMO weather interpretation codes -> (condition, emoji). */
    private fun describe(code: Int): Pair<String, String> = when (code) {
        0 -> "Clear sky" to "☀️"
        1 -> "Mainly clear" to "🌤️"
        2 -> "Partly cloudy" to "⛅"
        3 -> "Overcast" to "☁️"
        45, 48 -> "Foggy" to "🌫️"
        51, 53, 55, 56, 57 -> "Drizzle" to "🌦️"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain" to "🌧️"
        71, 73, 75, 77, 85, 86 -> "Snow" to "🌨️"
        95 -> "Thunderstorm" to "⛈️"
        96, 99 -> "Storm + hail" to "⛈️"
        else -> "Unknown" to "🌡️"
    }
}