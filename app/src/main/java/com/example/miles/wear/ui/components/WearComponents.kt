package com.example.miles.wear.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.miles.wear.data.model.HeartRateZone
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.VividGreen

@Composable
fun HeartRateZoneRing(
    bpm: Int,
    accuracy: Int,
    modifier: Modifier = Modifier
) {
    val zone = HeartRateZone.fromBpm(bpm)
    val zoneColor = Color(zone.colorHex)

    val sweepAngle = if (bpm > 0) {
        val fraction = ((bpm - 40f) / 160f).coerceIn(0.1f, 1.0f)
        fraction * 270f
    } else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(110.dp)) {
            val strokeWidth = 8.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)

            // Background arc (270 degrees, bottom gap for controls)
            drawArc(
                color = Color(0xFF222222),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active zone arc
            if (sweepAngle > 0f) {
                drawArc(
                    color = zoneColor.copy(alpha = pulseAlpha),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (bpm > 0) "$bpm" else "--",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (bpm > 0) zoneColor else MutedGray
            )
            Text(
                text = zone.title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = zoneColor
            )
            // Accuracy indicator
            AccuracyIndicator(accuracy = accuracy)
        }
    }
}

@Composable
fun AccuracyIndicator(accuracy: Int) {
    val (label, dotColor) = when (accuracy) {
        4 -> "HI" to VividGreen
        3 -> "MED" to NeonCyan
        2 -> "LOW" to ElectricAmber
        1 -> "UNREL" to CoralFlame
        else -> "NO SEN" to MutedGray
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(top = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            fontSize = 8.sp,
            color = MutedGray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DailyRingsMini(
    steps: Int,
    stepGoal: Int,
    calories: Int,
    calorieGoal: Int,
    bpm: Int,
    modifier: Modifier = Modifier
) {
    val stepFraction = (steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
    val calFraction = (calories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f)
    val hrFraction = (bpm.toFloat() / 180f).coerceIn(0f, 1f)

    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 5.dp.toPx()

            // Outer ring: Steps (Green)
            val outerPad = stroke / 2f
            drawArc(
                color = VividGreen.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(outerPad, outerPad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke)
            )
            drawArc(
                color = VividGreen,
                startAngle = -90f,
                sweepAngle = stepFraction * 360f,
                useCenter = false,
                topLeft = Offset(outerPad, outerPad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Middle ring: Calories (Coral)
            val midPad = stroke * 1.6f
            val midSize = size.width - (midPad * 2)
            drawArc(
                color = CoralFlame.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(midPad, midPad),
                size = Size(midSize, midSize),
                style = Stroke(stroke)
            )
            drawArc(
                color = CoralFlame,
                startAngle = -90f,
                sweepAngle = calFraction * 360f,
                useCenter = false,
                topLeft = Offset(midPad, midPad),
                size = Size(midSize, midSize),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Inner ring: BPM (Cyan)
            val inPad = stroke * 2.8f
            val inSize = size.width - (inPad * 2)
            drawArc(
                color = NeonCyan.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inPad, inPad),
                size = Size(inSize, inSize),
                style = Stroke(stroke)
            )
            drawArc(
                color = NeonCyan,
                startAngle = -90f,
                sweepAngle = hrFraction * 360f,
                useCenter = false,
                topLeft = Offset(inPad, inPad),
                size = Size(inSize, inSize),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun StatPill(
    value: String,
    label: String,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = MutedGray,
            fontWeight = FontWeight.Medium
        )
    }
}
