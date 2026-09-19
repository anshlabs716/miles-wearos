package com.example.miles.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import com.example.miles.wear.MilesWearApplication
import com.example.miles.wear.ui.components.AccuracyIndicator
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

@Composable
fun SensorsDiagnosticScreen(
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val sensorTracker = MilesWearApplication.instance.sensorTracker
    val phoneMessaging = MilesWearApplication.instance.phoneMessagingManager
    val repository = MilesWearApplication.instance.repository

    val liveHr by sensorTracker.liveHeartRate.collectAsStateWithLifecycle()
    val metrics by sensorTracker.liveMetrics.collectAsStateWithLifecycle()
    val batteryPct by sensorTracker.batteryPercent.collectAsStateWithLifecycle()
    val isLowPower by sensorTracker.isLowPowerMode.collectAsStateWithLifecycle()
    val connection by phoneMessaging.connectionStatus.collectAsStateWithLifecycle()
    val unsyncedCount by repository.unsyncedCount.collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(OLEDBlack)
                .focusRequester(focusRequester)
                .focusable()
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "HARDWARE TELEMETRY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Real Sensors Only",
                        fontSize = 9.sp,
                        color = MutedGray
                    )
                }
            }

            // Heart Rate Telemetry Card
            item {
                DiagnosticRowCard(
                    title = "HEART RATE SENSOR",
                    primaryText = if (liveHr.bpm > 0) "${liveHr.bpm} BPM" else "Searching...",
                    primaryColor = if (liveHr.bpm > 0) VividGreen else ElectricAmber,
                    secondaryContent = {
                        AccuracyIndicator(accuracy = liveHr.accuracy)
                    }
                )
            }

            // Step Counter & Cadence
            item {
                DiagnosticRowCard(
                    title = "STEP DETECTOR & CADENCE",
                    primaryText = "${metrics.steps} steps • ${metrics.cadenceSpm} SPM",
                    primaryColor = NeonCyan
                )
            }

            // Barometer / Altimeter
            item {
                DiagnosticRowCard(
                    title = "PRESSURE & BAROMETER",
                    primaryText = String.format("+%.1f m elevation gain", metrics.elevationGainMeters),
                    primaryColor = ElectricAmber
                )
            }

            // Battery & Duty Cycling
            item {
                DiagnosticRowCard(
                    title = "BATTERY & POWER MODE",
                    primaryText = "$batteryPct% ${if (isLowPower) "(Eco Mode)" else "(Normal)"}",
                    primaryColor = if (isLowPower) CoralFlame else VividGreen
                )
            }

            // Phone Sync & Queue
            item {
                DiagnosticRowCard(
                    title = "WEARABLE DATA LAYER",
                    primaryText = if (connection.isConnected) "Connected: ${connection.phoneNodeName}" else "Disconnected (Offline)",
                    primaryColor = if (connection.isConnected) VividGreen else ElectricAmber,
                    subtitle = "$unsyncedCount buffered offline telemetry records"
                )
            }

            // Manual Flush Button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    onClick = {
                        coroutineScope.launch {
                            phoneMessaging.flushOfflineQueue()
                        }
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF1E2836),
                        contentColor = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 3.dp),
                    label = {
                        Text(
                            text = "🔄 Force Queue Sync",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )
                    }
                )
            }

            // Back Button
            item {
                Chip(
                    onClick = onBack,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF1E1E22),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 3.dp),
                    label = {
                        Text(
                            text = "Back",
                            fontSize = 12.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DiagnosticRowCard(
    title: String,
    primaryText: String,
    primaryColor: Color = Color.White,
    subtitle: String? = null,
    secondaryContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16161A))
            .padding(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MutedGray,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            secondaryContent?.invoke()
        }
        subtitle?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = it,
                fontSize = 10.sp,
                color = MutedGray
            )
        }
    }
}
