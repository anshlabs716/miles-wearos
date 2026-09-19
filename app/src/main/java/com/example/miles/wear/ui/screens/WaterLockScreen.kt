package com.example.miles.wear.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WaterLockScreen(
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var unlockProgress by remember { mutableFloatStateOf(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val animatedProgress by animateFloatAsState(
        targetValue = unlockProgress,
        animationSpec = tween(150),
        label = "unlockProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        unlockProgress = 0f
                        holdJob?.cancel()
                        holdJob = coroutineScope.launch {
                            val steps = 20
                            for (i in 1..steps) {
                                delay(100L) // 2000ms total
                                unlockProgress = i.toFloat() / steps
                            }
                            // Trigger unlock vibration
                            try {
                                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                                    vm?.defaultVibrator
                                } else {
                                    @Suppress("DEPRECATION")
                                    context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                                }
                                vibrator?.vibrate(
                                    VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                            } catch (e: Exception) {
                                // fallback
                            }
                            onUnlock()
                        }
                        tryAwaitRelease()
                        holdJob?.cancel()
                        unlockProgress = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Unlock Progress Ring
        Canvas(modifier = Modifier.size(160.dp)) {
            val stroke = 6.dp.toPx()
            drawArc(
                color = Color(0xFF1E2836),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = VividGreen,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "💧",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "WATER LOCKED",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (unlockProgress > 0f) "Keep holding..." else "Touch & hold 2s\nto unlock",
                fontSize = 11.sp,
                color = if (unlockProgress > 0f) VividGreen else MutedGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
