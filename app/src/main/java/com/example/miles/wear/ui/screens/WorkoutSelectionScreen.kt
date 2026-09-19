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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import com.example.miles.wear.data.model.WorkoutType
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

@Composable
fun WorkoutSelectionScreen(
    onSelectWorkout: (WorkoutType) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val activities = remember {
        listOf(
            WorkoutType.WALK to (NeonCyan to "GPS + Wrist HR"),
            WorkoutType.RUN to (VividGreen to "GPS + Pace + Cadence"),
            WorkoutType.CYCLING to (CoralFlame to "GPS + Speed + Elev"),
            WorkoutType.HIKE to (ElectricAmber to "GPS + Barometer + HR"),
            WorkoutType.OTHER to (Color(0xFFE040FB) to "Indoor Sensor HR")
        )
    }

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
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = "START WORKOUT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Choose activity",
                        fontSize = 10.sp,
                        color = MutedGray
                    )
                }
            }

            items(activities) { (workoutType, styling) ->
                val (color, subtitle) = styling

                Chip(
                    onClick = { onSelectWorkout(workoutType) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 3.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = workoutType.emoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = workoutType.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
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
