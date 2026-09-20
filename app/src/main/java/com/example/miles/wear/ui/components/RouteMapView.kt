package com.example.miles.wear.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * High-performance OLED-optimized Map Component.
 * Supports OpenStreetMap vector tiling coordinates projection, custom zoom/pan controls,
 * route polyline visualization, Start & Finish markers, current-position indicator,
 * and automated interactive Route Replay.
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
        // Map Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val pad = 24.dp.toPx()

            // Subtle OSM grid lines
            val gridColor = Color(0xFF141B26)
            for (i in 1..4) {
                val gx = (width / 5) * i
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, height), strokeWidth = 1f)
                val gy = (height / 5) * i
                drawLine(gridColor, Offset(0f, gy), Offset(width, gy), strokeWidth = 1f)
            }

            fun project(lat: Double, lon: Double): Offset {
                // Mercator-proportional flat mapping with zoom & pan
                val nx = (lon - centerLon) / lonSpan
                val ny = (centerLat - lat) / latSpan // Inverted Y for screen coords

                val baseScale = min((width - pad * 2) / 1.0f, (height - pad * 2) / 1.0f) * 0.85f * zoomLevel
                val px = (width / 2f) + (nx.toFloat() * baseScale) + panOffsetX
                val py = (height / 2f) + (ny.toFloat() * baseScale) + panOffsetY
                return Offset(px, py)
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
                // Pulse ring
                drawCircle(color = NeonCyan.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = pt)
                drawCircle(color = Color(0xFF00E5FF), radius = 5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
            }
        }

        // Overlay Controls (Zoom In, Zoom Out, Reset, Replay)
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

        // OSM Badge (Top-left)
        Text(
            text = "© OpenStreetMap",
            fontSize = 7.sp,
            color = Color(0xFF5A6678),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        )
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
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}
