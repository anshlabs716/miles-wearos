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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import com.example.miles.wear.data.model.GoalType
import com.example.miles.wear.data.model.WorkoutMode
import com.example.miles.wear.data.model.WorkoutPlan
import com.example.miles.wear.data.model.WorkoutType
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

/**
 * Workout setup: Free / Goal / Interval modes with real, labelled targets.
 * Goal targets are presets in sensible units; the plan is passed to the
 * tracking service when the workout starts.
 */
@Composable
fun WorkoutSetupScreen(
    workoutType: WorkoutType,
    onStart: (WorkoutPlan) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(WorkoutMode.FREE) }
    var goalType by remember { mutableStateOf(GoalType.DISTANCE) }
    var goalDistance by remember { mutableStateOf(5.0) }
    var goalMinutes by remember { mutableStateOf(30) }
    var goalCalories by remember { mutableStateOf(300) }
    var workSec by remember { mutableStateOf(60) }
    var recSec by remember { mutableStateOf(30) }
    var reps by remember { mutableStateOf(8) }

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
                    coroutineScope.launch { listState.scrollBy(it.verticalScrollPixels) }
                    true
                }
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "${workoutType.emoji} ${workoutType.displayName.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text("WORKOUT SETUP", fontSize = 9.sp, color = MutedGray)
                }
            }

            // Mode picker
            item {
                Text(
                    text = "MODE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 2.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(0.94f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ModeChip("⚡ Free", mode == WorkoutMode.FREE, VividGreen, { mode = WorkoutMode.FREE }, Modifier.weight(1f))
                    ModeChip("🎯 Goal", mode == WorkoutMode.GOAL, ElectricAmber, { mode = WorkoutMode.GOAL }, Modifier.weight(1f))
                    ModeChip("🔁 Interval", mode == WorkoutMode.INTERVAL, CoralFlame, { mode = WorkoutMode.INTERVAL }, Modifier.weight(1f))
                }
            }

            if (mode == WorkoutMode.GOAL) {
                item {
                    Text(
                        text = "TARGET",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricAmber,
                        modifier = Modifier.fillMaxWidth(0.92f).padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.94f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        GoalTypeChip("Distance", goalType == GoalType.DISTANCE, { goalType = GoalType.DISTANCE }, Modifier.weight(1f))
                        GoalTypeChip("Duration", goalType == GoalType.DURATION, { goalType = GoalType.DURATION }, Modifier.weight(1f))
                        GoalTypeChip("Calories", goalType == GoalType.CALORIES, { goalType = GoalType.CALORIES }, Modifier.weight(1f))
                    }
                }
                item {
                    Text(
                        text = "VALUE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGray,
                        modifier = Modifier.fillMaxWidth(0.92f).padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                val presets: List<String>
                val selected: (String) -> Boolean
                val onPick: (String) -> Unit
                when (goalType) {
                    GoalType.DISTANCE -> {
                        presets = listOf("1 km", "3 km", "5 km", "10 km", "21 km")
                        selected = { s -> s == formatGoal(goalDistance) + " km" }
                        onPick = { s -> goalDistance = s.removeSuffix(" km").toDouble() }
                    }
                    GoalType.DURATION -> {
                        presets = listOf("10 min", "20 min", "30 min", "45 min", "60 min")
                        selected = { s -> s == "$goalMinutes min" }
                        onPick = { s -> goalMinutes = s.removeSuffix(" min").toInt() }
                    }
                    GoalType.CALORIES -> {
                        presets = listOf("100 kcal", "200 kcal", "300 kcal", "500 kcal", "750 kcal")
                        selected = { s -> s == "$goalCalories kcal" }
                        onPick = { s -> goalCalories = s.removeSuffix(" kcal").toInt() }
                    }
                }
                presets.forEach { preset ->
                    item {
                        GoalValueChip(preset, selected(preset), onPick = { onPick(preset) })
                    }
                }
            }

            if (mode == WorkoutMode.INTERVAL) {
                item {
                    Text(
                        text = "WORK",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = VividGreen,
                        modifier = Modifier.fillMaxWidth(0.92f).padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                listOf("30s", "45s", "1m", "1m30s", "2m").forEach { preset ->
                    item {
                        GoalValueChip(
                            "🔺 $preset",
                            formatSeconds(workSec) == preset,
                            onPick = { workSec = parseSeconds(preset) }
                        )
                    }
                }
                item {
                    Text(
                        text = "REST",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricAmber,
                        modifier = Modifier.fillMaxWidth(0.92f).padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                listOf("20s", "30s", "45s", "1m").forEach { preset ->
                    item {
                        GoalValueChip(
                            "🔻 $preset",
                            formatSeconds(recSec) == preset,
                            onPick = { recSec = parseSeconds(preset) }
                        )
                    }
                }
                item {
                    Text(
                        text = "ROUNDS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGray,
                        modifier = Modifier.fillMaxWidth(0.92f).padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                listOf("4", "8", "12", "16").forEach { preset ->
                    item {
                        GoalValueChip(
                            "🔁 $preset sets",
                            reps == preset.toInt(),
                            onPick = { reps = preset.toInt() }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Chip(
                    onClick = {
                        val plan = when (mode) {
                            WorkoutMode.FREE -> WorkoutPlan()
                            WorkoutMode.GOAL -> WorkoutPlan(
                                mode = WorkoutMode.GOAL,
                                goalType = goalType,
                                goalValue = when (goalType) {
                                    GoalType.DISTANCE -> goalDistance
                                    GoalType.DURATION -> goalMinutes.toDouble()
                                    GoalType.CALORIES -> goalCalories.toDouble()
                                }
                            )
                            WorkoutMode.INTERVAL -> WorkoutPlan(
                                mode = WorkoutMode.INTERVAL,
                                workSeconds = workSec,
                                recoverySeconds = recSec,
                                repetitions = reps
                            )
                        }
                        onStart(plan)
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = VividGreen,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 6.dp),
                    label = {
                        Text(
                            text = "START ➜",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ModeChip(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        onClick = onClick,
        colors = ChipDefaults.chipColors(
            backgroundColor = if (selected) accent.copy(alpha = 0.28f) else Color(0xFF18181C),
            contentColor = Color.White
        ),
        modifier = modifier.padding(vertical = 2.dp),
        label = {
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                color = if (selected) Color.White else MutedGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    )
}

@Composable
private fun GoalTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        onClick = onClick,
        colors = ChipDefaults.chipColors(
            backgroundColor = if (selected) ElectricAmber.copy(alpha = 0.25f) else Color(0xFF18181C),
            contentColor = Color.White
        ),
        modifier = modifier.padding(vertical = 2.dp),
        label = {
            Text(
                text = text,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MutedGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    )
}

@Composable
private fun GoalValueChip(
    text: String,
    selected: Boolean,
    onPick: () -> Unit
) {
    Chip(
        onClick = onPick,
        colors = ChipDefaults.chipColors(
            backgroundColor = if (selected) Color(0xFF1B3A2C) else Color(0xFF18181C),
            contentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 2.dp),
        label = {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                color = if (selected) VividGreen else Color.White
            )
        }
    )
}

private fun formatGoal(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else String.format("%.1f", value)

private fun formatSeconds(total: Int): String = when {
    total % 60 == 0 -> "${total / 60}m"
    total > 60 -> "${total / 60}m${total % 60}s"
    else -> "${total}s"
}

private fun parseSeconds(label: String): Int = when (label) {
    "30s" -> 30
    "45s" -> 45
    "1m" -> 60
    "1m30s" -> 90
    "2m" -> 120
    "20s" -> 20
    else -> 60
}