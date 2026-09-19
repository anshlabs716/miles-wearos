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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.miles.wear.data.model.GpsStatus
import com.example.miles.wear.data.model.HeartRateZone
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
    onDiscardWorkout: () -> Unit,
    onEnableWaterLock: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val sensorTracker = MilesWearApplication.instance.sensorTracker
    val repository = MilesWearApplication.instance.repository
    val settings by repository.settings.collectAsStateWithLifecycle()

    val metrics by sensorTracker.liveMetrics.collectAsStateWithLifecycle()
    val liveHr by sensorTracker.liveHeartRate.collectAsStateWithLifecycle()
    val gpsStatus by sensorTracker.gpsStatus.collectAsStateWithLifecycle()
    val isLowPower by sensorTracker.isLowPowerMode.collectAsStateWithLifecycle()

    var isPaused by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val serviceIntent = Intent(context, WorkoutTrackingService::class.java).apply {
            action = WorkoutTrackingService.ACTION_START
            putExtra(WorkoutTrackingService.EXTRA_WORKOUT_TYPE, workoutType.name)
        }
        context.startForegroundService(serviceIntent)
    }

    val elapsedMinutes = metrics.elapsedSeconds / 60
    val elapsedSeconds = metrics.elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", elapsedMinutes, elapsedSeconds)
    val distanceFormatted = settings.unit.formatDistance(metrics.distanceMeters)
    val paceFormatted = settings.unit.formatPace(metrics.distanceMeters, metrics.elapsedSeconds)

    // Discard Confirmation Dialog
    if (showDiscardDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OLEDBlack)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Discard workout?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This workout will not be saved.",
                    fontSize = 10.sp,
                    color = MutedGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                Chip(
                    onClick = { showDiscardDialog = false },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF222226),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f),
                    label = {
                        Text(
                            text = "CANCEL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    onClick = {
                        val intent = Intent(context, WorkoutTrackingService::class.java).apply {
                            action = WorkoutTrackingService.ACTION_DISCARD
                        }
                        context.startService(intent)
                        onDiscardWorkout()
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF3B1010),
                        contentColor = CoralFlame
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f),
                    label = {
                        Text(
                            text = "DISCARD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralFlame,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
        }
        return
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
            // Header: Activity Name & GPS Status Badge
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${workoutType.emoji} ${workoutType.displayName.uppercase()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    }

                    // GPS Status pill
                    val gpsColor = when (gpsStatus) {
                        GpsStatus.READY -> VividGreen
                        GpsStatus.SEARCHING -> ElectricAmber
                        GpsStatus.UNAVAILABLE -> CoralFlame
                        GpsStatus.INDOOR -> MutedGray
                    }
                    Text(
                        text = gpsStatus.label,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = gpsColor
                    )
                }
            }

            // Paused Banner or Main Timer Display
            item {
                if (isPaused) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF332008))
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "WORKOUT PAUSED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricAmber,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = formattedTime,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = formattedTime,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Distance & Pace Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill(
                        value = "$distanceFormatted ${settings.unit.distanceLabel}",
                        label = "DISTANCE",
                        color = VividGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = "$paceFormatted ${settings.unit.paceLabel}",
                        label = "PACE",
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Heart Rate HUD
            item {
                if (!sensorTracker.isHeartRateSensorPresent) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF18181A))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "♥ -- BPM",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedGray
                        )
                        Text(
                            text = "Sensor unavailable",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                } else {
                    HeartRateZoneRing(
                        bpm = liveHr.bpm,
                        accuracy = liveHr.accuracy,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Steps & Calories Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill(
                        value = "${metrics.steps} steps",
                        label = "WORKOUT STEPS",
                        color = ElectricAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = "${metrics.caloriesKcal} kcal",
                        label = "CALORIES",
                        color = CoralFlame,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Action Controls
            item {
                Spacer(modifier = Modifier.height(4.dp))
                if (isPaused) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Resume Button
                        Chip(
                            onClick = {
                                val intent = Intent(context, WorkoutTrackingService::class.java).apply {
                                    action = WorkoutTrackingService.ACTION_RESUME
                                }
                                context.startService(intent)
                                isPaused = false
                            },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = VividGreen,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    text = "Resume",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )

                        // Finish Button
                        Chip(
                            onClick = {
                                val intent = Intent(context, WorkoutTrackingService::class.java).apply {
                                    action = WorkoutTrackingService.ACTION_FINISH
                                }
                                context.startService(intent)
                                onFinishWorkout()
                            },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFF1E2638),
                                contentColor = NeonCyan
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    text = "Finish",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )

                        // Discard Button
                        Chip(
                            onClick = { showDiscardDialog = true },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = Color(0xFF331414),
                                contentColor = CoralFlame
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    text = "Discard",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralFlame,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                } else {
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
                                isPaused = true
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
                                    color = ElectricAmber,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
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
                                    color = CoralFlame,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }
            }

            // Water Lock Button
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
                                fontSize = 11.sp,
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
