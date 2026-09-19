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
fun MirroredWorkoutScreen(
    onExitMirrored: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val sensorTracker = MilesWearApplication.instance.sensorTracker
    val phoneMessaging = MilesWearApplication.instance.phoneMessagingManager

    val liveHr by sensorTracker.liveHeartRate.collectAsStateWithLifecycle()
    val mirroredMetrics by phoneMessaging.mirroredMetrics.collectAsStateWithLifecycle()
    val connection by phoneMessaging.connectionStatus.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val minutes = mirroredMetrics.seconds / 60
    val seconds = mirroredMetrics.seconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val distanceKm = String.format("%.2f", mirroredMetrics.meters / 1000.0)

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
            // Header: Mirrored status
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MIRRORED PHONE HUD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "📡 Streaming Wrist HR to Phone",
                        fontSize = 9.sp,
                        color = VividGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Real Wrist HR
            item {
                HeartRateZoneRing(
                    bpm = liveHr.bpm,
                    accuracy = liveHr.accuracy,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Mirrored Phone Stats: Distance & Calories
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatPill(
                        value = "$distanceKm km",
                        label = "PHONE DISTANCE",
                        color = VividGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = "${mirroredMetrics.calories} kcal",
                        label = "PHONE CALORIES",
                        color = CoralFlame,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Remote Phone Controls
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Chip(
                        onClick = {
                            phoneMessaging.sendWorkoutControl("pause", "Mirrored")
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
                            phoneMessaging.sendWorkoutControl("finish", "Mirrored")
                            onExitMirrored()
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

            // Disconnect / Exit HUD
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Chip(
                    onClick = onExitMirrored,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF1E1E22),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 4.dp),
                    label = {
                        Text(
                            text = "Back to Watch Dashboard",
                            fontSize = 11.sp,
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
