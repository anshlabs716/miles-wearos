package com.example.miles.wear.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A real place search result (OpenStreetMap data via Nominatim). */
data class SearchResult(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Real local place search via Nominatim (OpenStreetMap geocoder, free,
 * no API key). Queries are bounded to 5 results so the watch UI stays usable.
 */
object WearPlaceSearch {

    suspend fun search(query: String, limit: Int = 5): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=$limit&q=$encoded"

        val body: String = try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "MILES-WearOS/1.0 (activity tracker)")
            }
            try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            return@withContext emptyList()
        }

        try {
            val arr = JSONArray(body)
            val out = mutableListOf<SearchResult>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                out.add(
                    SearchResult(
                        displayName = obj.optString("display_name"),
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lon", 0.0)
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }
}