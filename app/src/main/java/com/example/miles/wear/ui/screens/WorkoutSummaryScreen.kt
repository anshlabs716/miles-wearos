package com.example.miles.wear.ui.screens

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
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.WorkoutType
import com.example.miles.wear.sensor.SensorTracker
import com.example.miles.wear.ui.components.RouteMapView
import com.example.miles.wear.ui.components.StatPill
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

@Composable
fun WorkoutSummaryScreen(
    onDone: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val repository = MilesWearApplication.instance.repository
    val settings by repository.settings.collectAsStateWithLifecycle()
    val phoneMessaging = MilesWearApplication.instance.phoneMessagingManager

    var latestSession by remember { mutableStateOf<WorkoutSessionEntity?>(null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        latestSession = repository.getLatestWorkout()
    }

    val session = latestSession
    val durationMin = (session?.durationSeconds ?: 0L) / 60
    val durationSec = (session?.durationSeconds ?: 0L) % 60
    val formattedTime = String.format("%02d:%02d", durationMin, durationSec)
    val distanceFormatted = settings.unit.formatDistance(session?.distanceMeters ?: 0.0)
    val paceFormatted = settings.unit.formatPace(session?.distanceMeters ?: 0.0, session?.durationSeconds ?: 0L)
    val emoji = WorkoutType.fromString(session?.workoutType ?: "").emoji
    val parsedRoute = remember(session?.routeGeoJson) {
        SensorTracker.parseRouteJson(session?.routeGeoJson)
    }

    if (showDiscardConfirm) {
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
                    text = "This workout will be deleted from history.",
                    fontSize = 10.sp,
                    color = MutedGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                Chip(
                    onClick = { showDiscardConfirm = false },
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
                        coroutineScope.launch {
                            session?.id?.let { repository.deleteWorkoutSession(it) }
                            onDone()
                        }
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
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "WORKOUT COMPLETE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = VividGreen,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$emoji ${session?.workoutType ?: "Running"}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill(
                        value = formattedTime,
                        label = "DURATION",
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = "$distanceFormatted ${settings.unit.distanceLabel}",
                        label = "DISTANCE",
                        color = VividGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill(
                        value = "$paceFormatted ${settings.unit.paceLabel}",
                        label = "AVG PACE",
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = if ((session?.avgBpm ?: 0) > 0) "♥ ${session?.avgBpm} BPM" else "♥ -- BPM",
                        label = "AVG HR",
                        color = CoralFlame,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill(
                        value = "${session?.totalSteps ?: 0}",
                        label = "STEPS",
                        color = ElectricAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = "${session?.caloriesKcal ?: 0} kcal",
                        label = "CALORIES",
                        color = CoralFlame,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // GPS Route Summary Map
            if (parsedRoute.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GPS ROUTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp)
                        )
                        RouteMapView(
                            route = parsedRoute,
                            enableReplay = true,
                            showControls = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    }
                }
            }

            // Sync status feedback
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161618))
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (session?.isSyncedToPhone == true) "Synced to MILES phone app" else "Saved locally (Offline-first)",
                        fontSize = 10.sp,
                        color = if (session?.isSyncedToPhone == true) VividGreen else ElectricAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Primary Save Action
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Chip(
                    onClick = {
                        coroutineScope.launch {
                            phoneMessaging.flushOfflineQueue()
                            onDone()
                        }
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = VividGreen,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Save Workout",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }

            // Secondary Discard Action
            item {
                Chip(
                    onClick = { showDiscardConfirm = true },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF221414),
                        contentColor = CoralFlame
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Discard",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CoralFlame,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
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
