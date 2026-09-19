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
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.WorkoutType
import com.example.miles.wear.ui.components.StatPill
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutDetailScreen(
    sessionId: Long,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val repository = MilesWearApplication.instance.repository
    val settings by repository.settings.collectAsStateWithLifecycle()
    var session by remember { mutableStateOf<WorkoutSessionEntity?>(null) }

    LaunchedEffect(sessionId) {
        focusRequester.requestFocus()
        session = repository.getSessionById(sessionId)
    }

    val cur = session
    val durationMin = (cur?.durationSeconds ?: 0L) / 60
    val durationSec = (cur?.durationSeconds ?: 0L) % 60
    val formattedTime = String.format("%02d:%02d", durationMin, durationSec)
    val distFormatted = settings.unit.formatDistance(cur?.distanceMeters ?: 0.0)
    val paceFormatted = settings.unit.formatPace(cur?.distanceMeters ?: 0.0, cur?.durationSeconds ?: 0L)
    val emoji = WorkoutType.fromString(cur?.workoutType ?: "").emoji

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
                        text = "$emoji ${cur?.workoutType?.uppercase() ?: "WORKOUT"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    cur?.startTime?.let { start ->
                        val dateStr = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(start))
                        Text(
                            text = dateStr,
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
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
                        value = "$distFormatted ${settings.unit.distanceLabel}",
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
                        value = if ((cur?.avgBpm ?: 0) > 0) "${cur?.avgBpm} BPM" else "-- BPM",
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
                        value = "${cur?.totalSteps ?: 0}",
                        label = "STEPS",
                        color = ElectricAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = "${cur?.caloriesKcal ?: 0} kcal",
                        label = "CALORIES",
                        color = CoralFlame,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sync Status
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
                        text = if (cur?.isSyncedToPhone == true) "Synced to MILES Phone App" else "Stored Locally (Offline Queue)",
                        fontSize = 10.sp,
                        color = if (cur?.isSyncedToPhone == true) VividGreen else ElectricAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Delete session button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Chip(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteWorkoutSession(sessionId)
                            onBack()
                        }
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF2E1515),
                        contentColor = CoralFlame
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Delete Workout",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CoralFlame
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
