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
import androidx.compose.foundation.layout.width
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
import com.example.miles.wear.data.model.DistanceUnit
import com.example.miles.wear.data.model.WorkoutType
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
fun HistoryScreen(
    onSelectSession: (Long) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val repository = MilesWearApplication.instance.repository
    val sessions by repository.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by repository.settings.collectAsStateWithLifecycle()

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
                        text = "WORKOUT HISTORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${sessions.size} recorded sessions",
                        fontSize = 10.sp,
                        color = MutedGray
                    )
                }
            }

            if (sessions.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 24.dp)
                    ) {
                        Text(
                            text = "No workouts recorded yet",
                            fontSize = 12.sp,
                            color = MutedGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(sessions) { session ->
                    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                    val dateStr = dateFormat.format(Date(session.startTime))
                    val durationMin = session.durationSeconds / 60
                    val distStr = settings.unit.formatDistance(session.distanceMeters)
                    val emoji = WorkoutType.fromString(session.workoutType).emoji

                    Chip(
                        onClick = { onSelectSession(session.id) },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF18181C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 3.dp),
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = session.workoutType,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        },
                        secondaryLabel = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$durationMin min · $distStr ${settings.unit.distanceLabel}",
                                    fontSize = 10.sp,
                                    color = VividGreen
                                )
                                Text(
                                    text = if (session.isSyncedToPhone) "✓ Synced" else "• Local",
                                    fontSize = 9.sp,
                                    color = if (session.isSyncedToPhone) NeonCyan else ElectricAmber
                                )
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
