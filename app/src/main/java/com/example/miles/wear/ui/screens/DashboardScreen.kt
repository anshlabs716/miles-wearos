package com.example.miles.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import com.example.miles.wear.MilesWearApplication
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.WorkoutType
import com.example.miles.wear.ui.components.DailyRingsMini
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onStartWorkout: (WorkoutType) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenMirrored: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val sensorTracker = MilesWearApplication.instance.sensorTracker
    val phoneMessaging = MilesWearApplication.instance.phoneMessagingManager
    val repository = MilesWearApplication.instance.repository

    val metrics by sensorTracker.liveMetrics.collectAsStateWithLifecycle()
    val liveHr by sensorTracker.liveHeartRate.collectAsStateWithLifecycle()
    val connection by phoneMessaging.connectionStatus.collectAsStateWithLifecycle()
    val mirroredMetrics by phoneMessaging.mirroredMetrics.collectAsStateWithLifecycle()
    val unsyncedCount by repository.unsyncedCount.collectAsStateWithLifecycle(initialValue = 0)
    val sessions by repository.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        phoneMessaging.refreshConnectedNodes()
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
            // Header: Title & Time
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "MILES",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "ACTIVITY TRACKER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGray,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Phone Mirrored Workout Active Banner (if phone workout is running)
            if (mirroredMetrics.isPhoneActive) {
                item {
                    Chip(
                        onClick = onOpenMirrored,
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF00364F),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 4.dp),
                        label = {
                            Text(
                                text = "📱 Workout Running on Phone",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = "Tap to open Wrist HUD",
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    )
                }
            }

            // Phone Connection & Queue Pill
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF18181A))
                        .clickable {
                            coroutineScope.launch {
                                phoneMessaging.refreshConnectedNodes()
                                phoneMessaging.flushOfflineQueue()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (connection.isConnected) VividGreen else ElectricAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (connection.isConnected) "Phone Synced" else "Standalone Mode",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    if (unsyncedCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${unsyncedCount} buffered)",
                            fontSize = 9.sp,
                            color = CoralFlame,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Daily Rings Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1E22))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DailyRingsMini(
                        steps = metrics.steps,
                        stepGoal = 10000,
                        calories = metrics.caloriesKcal,
                        calorieGoal = 500,
                        bpm = liveHr.bpm
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "${metrics.steps} steps",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = VividGreen
                        )
                        Text(
                            text = "${metrics.caloriesKcal} kcal burned",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CoralFlame
                        )
                        Text(
                            text = if (liveHr.bpm > 0) "${liveHr.bpm} BPM" else "-- BPM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )
                    }
                }
            }

            // Section: Start Workout
            item {
                Text(
                    text = "START WORKOUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedGray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            // Workout Type Chips
            items(WorkoutType.values().toList()) { workoutType ->
                val (chipColor, chipIcon) = when (workoutType) {
                    WorkoutType.RUN -> VividGreen to "🏃"
                    WorkoutType.WALK -> NeonCyan to "🚶"
                    WorkoutType.RIDE -> CoralFlame to "🚴"
                    WorkoutType.HIKE -> ElectricAmber to "⛰️"
                    WorkoutType.INDOOR -> Color(0xFFE040FB) to "🏋️"
                }

                Chip(
                    onClick = { onStartWorkout(workoutType) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF1A1A1E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 3.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = chipIcon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = workoutType.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipColor
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = "GPS + Wrist HR",
                            fontSize = 10.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Section: Hardware Diagnostics
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Chip(
                    onClick = onOpenDiagnostics,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF121214),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 3.dp),
                    label = {
                        Text(
                            text = "⚡ Sensor Diagnostics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )
                    }
                )
            }

            // Recent Sessions
            if (sessions.isNotEmpty()) {
                item {
                    Text(
                        text = "RECENT WORKOUTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(sessions.take(3)) { session ->
                    SessionItemCard(session)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SessionItemCard(session: WorkoutSessionEntity) {
    val durationMin = session.durationSeconds / 60
    val durationSec = session.durationSeconds % 60
    val km = String.format("%.2f", session.distanceMeters / 1000.0)

    Column(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF18181C))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = session.workoutType,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Text(
                text = String.format("%02d:%02d", durationMin, durationSec),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$km km • ${session.caloriesKcal} kcal",
                fontSize = 11.sp,
                color = MutedGray
            )
            Text(
                text = "${session.avgBpm} avg BPM",
                fontSize = 11.sp,
                color = VividGreen
            )
        }
    }
}
