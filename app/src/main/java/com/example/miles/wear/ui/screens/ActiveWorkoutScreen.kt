package com.example.miles.wear.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.miles.wear.data.model.WorkoutState
import com.example.miles.wear.data.model.WorkoutType
import com.example.miles.wear.service.WorkoutTrackingService
import com.example.miles.wear.ui.components.HeartRateZoneRing
import com.example.miles.wear.ui.components.StatPill
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutScreen(
    workoutType: WorkoutType,
    onFinishWorkout: () -> Unit,
    onEnableWaterLock: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val sensorTracker = MilesWearApplication.instance.sensorTracker
    val metrics by sensorTracker.liveMetrics.collectAsStateWithLifecycle()
    val liveHr by sensorTracker.liveHeartRate.collectAsStateWithLifecycle()
    val isLowPower by sensorTracker.isLowPowerMode.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Ensure tracking service is started if not already
        val serviceIntent = Intent(context, WorkoutTrackingService::class.java).apply {
            action = WorkoutTrackingService.ACTION_START
            putExtra(WorkoutTrackingService.EXTRA_WORKOUT_TYPE, workoutType.name)
        }
        context.startForegroundService(serviceIntent)
    }

    val elapsedMinutes = metrics.elapsedSeconds / 60
    val elapsedSeconds = metrics.elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", elapsedMinutes, elapsedSeconds)
    val distanceKm = String.format("%.2f", metrics.distanceMeters / 1000.0)

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
            // Header: Workout Name & Timer
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = workoutType.displayName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    if (isLowPower) {
                        Text(
                            text = "⚡ ECO SENSOR MODE (<20%)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricAmber
                        )
                    }
                }
            }

            // Central Telemetry: Heart Rate Zone Gauge
            item {
                HeartRateZoneRing(
                    bpm = liveHr.bpm,
                    accuracy = liveHr.accuracy,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Primary Stat Grid: Distance & Pace / Cadence
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill(
                        value = "$distanceKm km",
                        label = "DISTANCE",
                        color = VividGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = "${metrics.cadenceSpm} SPM",
                        label = "CADENCE",
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Secondary Stat Grid: Calories & Elevation
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill(
                        value = "${metrics.caloriesKcal} kcal",
                        label = "CALORIES",
                        color = CoralFlame,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = String.format("%.0f m", metrics.elevationGainMeters),
                        label = "ELEVATION",
                        color = ElectricAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Action Controls
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Chip(
                        onClick = {
                            val intent = Intent(context, WorkoutTrackingService::class.java).apply {
                                action = WorkoutTrackingService.ACTION_PAUSE
                            }
                            context.startService(intent)
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF2E2412),
                            contentColor = ElectricAmber
                        ),
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = "Pause",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricAmber
                            )
                        }
                    )

                    Chip(
                        onClick = {
                            val intent = Intent(context, WorkoutTrackingService::class.java).apply {
                                action = WorkoutTrackingService.ACTION_FINISH
                            }
                            context.startService(intent)
                            onFinishWorkout()
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF3B1010),
                            contentColor = CoralFlame
                        ),
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = "Finish",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralFlame
                            )
                        }
                    )
                }
            }

            // Water Lock Chip
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Chip(
                    onClick = onEnableWaterLock,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF142436),
                        contentColor = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 4.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💧", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Water Lock Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan
                            )
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
