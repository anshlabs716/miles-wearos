package com.example.miles.wear.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.miles.wear.data.model.GpsPoint
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * OLED-friendly workout map with REAL base-map tiles:
 * - Street layer: © OpenStreetMap contributors
 * - Satellite layer: Esri World Imagery (Maxar / Earthstar Geographics / USGS)
 * Web-Mercator projection (standard slippy-map math), pan + zoom, route
 * polyline, start/finish markers, live position pulse and route replay.
 * When tiles can't load (offline), the dark grid fallback still renders.
 */
@Composable
fun RouteMapView(
    route: List<GpsPoint>,
    modifier: Modifier = Modifier,
    currentLocation: GpsPoint? = null,
    enableReplay: Boolean = false,
    showControls: Boolean = true,
    routeColor: Color = NeonCyan,
    routeThickness: Float = 3f
) {
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var useSatellite by remember { mutableStateOf(false) }

    // Tile cache: "z/x/y/layer" -> bitmap (only real loaded tiles)
    val tileCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Replay state
    var isReplaying by remember { mutableStateOf(false) }
    var replayIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isReplaying) {
        if (isReplaying && route.isNotEmpty()) {
            replayIndex = 0
            while (isReplaying && replayIndex < route.size - 1) {
                delay(120)
                replayIndex++
            }
            if (replayIndex >= route.size - 1) {
                delay(1000)
                isReplaying = false
            }
        }
    }

    if (route.isEmpty() && currentLocation == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF101216))
                .border(1.dp, Color(0xFF222834), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🗺️", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "GPS Searching / No Route",
                    fontSize = 10.sp,
                    color = MutedGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    val displayRoute = remember(route, isReplaying, replayIndex) {
        if (isReplaying && route.isNotEmpty()) {
            route.take(replayIndex + 1)
        } else {
            route
        }
    }

    // Bounds calculation
    val allPoints = if (currentLocation != null) route + currentLocation else route
    val minLat = allPoints.minOfOrNull { it.lat } ?: 0.0
    val maxLat = allPoints.maxOfOrNull { it.lat } ?: 0.0
    val minLon = allPoints.minOfOrNull { it.lon } ?: 0.0
    val maxLon = allPoints.maxOfOrNull { it.lon } ?: 0.0

    val latSpan = max(0.0008, maxLat - minLat)
    val lonSpan = max(0.0008, maxLon - minLon)
    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF090C10))
            .border(1.dp, Color(0xFF1B2433), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    panOffsetX += dragAmount.x
                    panOffsetY += dragAmount.y
                }
            }
    ) {
        // ---------- Web Mercator tile math (measured via canvas size at draw) ----------
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val pad = 24.dp.toPx()

            // Choose a slippy-map zoom whose pixel density matches the previous
            // lat/lon span scaling, so zoom/pan feel identical to before.
            val pxPerLon = ((width - pad * 2) * 0.85f * zoomLevel) / lonSpan.toFloat()
            val worldSize = max(256.0, pxPerLon.toDouble() * 360.0)
            val tileZoom = min(19, max(1, (log2(worldSize / 256.0)).roundToInt()))
            val worldPx = 256.0 * (1 shl tileZoom)

            fun lonToWorldX(lon: Double): Double = (lon + 180.0) / 360.0 * worldPx
            fun latToWorldY(lat: Double): Double {
                val rad = Math.toRadians(lat)
                val y = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0
                return y * worldPx
            }

            val centerWorldX = lonToWorldX(centerLon)
            val centerWorldY = latToWorldY(centerLat)

            fun project(lat: Double, lon: Double): Offset {
                val sx = width / 2f + (lonToWorldX(lon) - centerWorldX).toFloat() + panOffsetX
                val sy = height / 2f + (latToWorldY(lat) - centerWorldY).toFloat() + panOffsetY
                return Offset(sx, sy)
            }

            // Visible world range (pan shifts the viewport in world space)
            val shiftX = panOffsetX.toDouble()
            val shiftY = panOffsetY.toDouble()
            val worldMinX = centerWorldX - width / 2.0 - shiftX
            val worldMaxX = centerWorldX + width / 2.0 - shiftX
            val worldMinY = centerWorldY - height / 2.0 - shiftY
            val worldMaxY = centerWorldY + height / 2.0 - shiftY

            val tileX0 = max(0, floor(worldMinX / 256.0).toInt())
            val tileX1 = min((1 shl tileZoom) - 1, floor(worldMaxX / 256.0).toInt())
            val tileY0 = max(0, floor(worldMinY / 256.0).toInt())
            val tileY1 = min((1 shl tileZoom) - 1, floor(worldMaxY / 256.0).toInt())

            var tilesMixed = 0
            for (tx in tileX0..tileX1) {
                for (ty in tileY0..tileY1) {
                    val key = "$tileZoom/$tx/$ty/${if (useSatellite) "sat" else "osm"}"
                    val img = tileCache[key] ?: continue
                    val sx = width / 2f + ((tx * 256) - centerWorldX).toFloat() + panOffsetX
                    val sy = height / 2f + ((ty * 256) - centerWorldY).toFloat() + panOffsetY
                    drawImage(img, topLeft = Offset(sx, sy))
                    tilesMixed++
                }
            }

            if (tilesMixed > 0) {
                // Keep the OLED dark vibe on bright satellite imagery + route contrast
                drawRect(Color(if (useSatellite) 0x40000000 else 0x22000000))
            } else {
                // Offline fallback: subtle OSM-style grid
                val gridColor = Color(0xFF141B26)
                for (i in 1..4) {
                    val gx = (width / 5) * i
                    drawLine(gridColor, Offset(gx, 0f), Offset(gx, height), strokeWidth = 1f)
                    val gy = (height / 5) * i
                    drawLine(gridColor, Offset(0f, gy), Offset(width, gy), strokeWidth = 1f)
                }
            }

            // Draw Recorded Polyline
            if (displayRoute.size > 1) {
                val path = Path()
                displayRoute.forEachIndexed { index, p ->
                    val pt = project(p.lat, p.lon)
                    if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }

                // Route Outer Glow
                drawPath(
                    path = path,
                    color = routeColor.copy(alpha = 0.35f),
                    style = Stroke(width = (routeThickness * 2.3f).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Route Core Line
                drawPath(
                    path = path,
                    color = routeColor,
                    style = Stroke(width = routeThickness.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Draw Start Marker (Green circle with S)
            if (displayRoute.isNotEmpty()) {
                val startPt = project(displayRoute.first().lat, displayRoute.first().lon)
                drawCircle(color = VividGreen, radius = 5.dp.toPx(), center = startPt)
                drawCircle(color = Color.White, radius = 2.5f.dp.toPx(), center = startPt)
            }

            // Draw Finish Marker (Coral red circle)
            if (displayRoute.size > 1 && !isReplaying) {
                val endPt = project(displayRoute.last().lat, displayRoute.last().lon)
                drawCircle(color = CoralFlame, radius = 5.dp.toPx(), center = endPt)
                drawCircle(color = Color.White, radius = 2.5f.dp.toPx(), center = endPt)
            }

            // Current location pulse marker (during live tracking or replay)
            val activePoint = if (isReplaying && displayRoute.isNotEmpty()) {
                displayRoute.last()
            } else {
                currentLocation
            }

            activePoint?.let { loc ->
                val pt = project(loc.lat, loc.lon)
                drawCircle(color = NeonCyan.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = pt)
                drawCircle(color = Color(0xFF00E5FF), radius = 5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
            }
        }

        // ---------- Tile loader (real tiles, cached) ----------
        LaunchedEffect(zoomLevel, centerLat, centerLon, panOffsetX, panOffsetY, useSatellite) {
            if (!showControls && route.isEmpty()) return@LaunchedEffect
            // Recompute the same viewport parameters for the fetch pass
            // (independent of canvas size — approximate with 220x220 px round pick).
            // To stay simple + safe we fetch a generous 4x4 neighborhood around
            // the center tile at the current zoom; the cache covers the rest.
            val pxPerLon = ((220f - 48f) * 0.85f * zoomLevel) / lonSpan.toFloat()
            val worldSize = max(256.0, pxPerLon.toDouble() * 360.0)
            val tileZoom = min(19, max(1, (log2(worldSize / 256.0)).roundToInt()))
            val worldPx = 256.0 * (1 shl tileZoom)

            val rad = Math.toRadians(centerLat)
            val centerY = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * worldPx
            val centerX = (centerLon + 180.0) / 360.0 * worldPx

            val cTileX = floor(centerX / 256.0).toInt()
            val cTileY = floor(centerY / 256.0).toInt()
            val half = 3
            val layer = if (useSatellite) "sat" else "osm"
            for (dx in -half..half) {
                for (dy in -half..half) {
                    val tx = cTileX + dx
                    val ty = cTileY + dy
                    val maxT = (1 shl tileZoom) - 1
                    if (tx < 0 || ty < 0 || tx > maxT || ty > maxT) continue
                    val key = "$tileZoom/$tx/$ty/$layer"
                    if (tileCache.containsKey(key)) continue
                    val url = if (useSatellite) {
                        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$tileZoom/$ty/$tx"
                    } else {
                        "https://tile.openstreetmap.org/$tileZoom/$tx/$ty.png"
                    }
                    val bytes = withContext(Dispatchers.IO) { fetchTileBytes(url) } ?: continue
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue
                    tileCache[key] = bmp.asImageBitmap()
                }
            }
        }

        // Overlay Controls (Layer toggle, Zoom In, Zoom Out, Reset, Replay)
        if (showControls) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zoom Controls
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiniMapBtn(
                        text = if (useSatellite) "🛰" else "🗺",
                        color = if (useSatellite) ElectricAmber else NeonCyan
                    ) { useSatellite = !useSatellite }
                    MiniMapBtn(text = "+") { zoomLevel = min(3.5f, zoomLevel + 0.5f) }
                    MiniMapBtn(text = "−") { zoomLevel = max(0.7f, zoomLevel - 0.5f) }
                    MiniMapBtn(text = "⊙") {
                        zoomLevel = 1f
                        panOffsetX = 0f
                        panOffsetY = 0f
                    }
                }

                // Replay button if enabled
                if (enableReplay && route.size > 2) {
                    MiniMapBtn(
                        text = if (isReplaying) "⏸" else "▶",
                        color = VividGreen
                    ) {
                        isReplaying = !isReplaying
                    }
                }
            }
        }

        // Attribution (top-left)
        Text(
            text = if (useSatellite) "© Esri · Maxar · USGS" else "© OpenStreetMap",
            fontSize = 7.sp,
            color = Color(0xFF5A6678),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        )
    }
}

private fun fetchTileBytes(urlString: String): ByteArray? {
    return try {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "MILES-WearOS/1.3")
        conn.connect()
        if (conn.responseCode == 200) {
            conn.inputStream.use { it.readBytes() }
        } else null
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun MiniMapBtn(
    text: String,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0xCC1A2332))
            .border(0.5.dp, Color(0xFF334460), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (text.length > 1) 9.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}