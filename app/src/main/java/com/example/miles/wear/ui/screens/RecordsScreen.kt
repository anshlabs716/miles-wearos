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
import com.example.miles.wear.MilesWearApplication
import com.example.miles.wear.data.model.RecordsSummary
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

/**
 * Streaks + personal records, all computed from real saved workout history
 * and daily stats. Nothing hardcoded.
 */
@Composable
fun RecordsScreen() {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val repository = MilesWearApplication.instance.repository

    var records by remember { mutableStateOf<RecordsSummary?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        records = repository.computeRecords()
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
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "🏆 RECORDS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricAmber,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (records == null) "Computing…" else "From real workout history",
                        fontSize = 9.sp,
                        color = MutedGray
                    )
                }
            }

            val r = records
            if (r != null) {
                item { SectionHeader("STREAKS") }
                item { StatRow("🔥 Workout streak", "${r.workoutStreak} day${if (r.workoutStreak == 1) "" else "s"}", VividGreen) }
                item { StatRow("📏 Distance streak", "${r.distanceStreak} day${if (r.distanceStreak == 1) "" else "s"}", VividGreen) }
                item { StatRow("👟 Step streak", "${r.stepStreak} day${if (r.stepStreak == 1) "" else "s"}", VividGreen) }

                item { SectionHeader("PERSONAL RECORDS") }
                item { StatRow("🚀 Longest distance", formatDistance(r.longestDistanceMeters), NeonCyan) }
                item { StatRow("⏱️ Longest workout", formatDuration(r.longestDurationSeconds), NeonCyan) }
                if (r.fastestPace > 0) {
                    item { StatRow("⚡ Fastest pace", formatPace(r.fastestPace), NeonCyan) }
                }
                if (r.mostStepsInWorkout > 0) {
                    item { StatRow("🦵 Most steps (one workout)", "${r.mostStepsInWorkout}", NeonCyan) }
                }
                if (r.maxElevationGainMeters > 0) {
                    item { StatRow("⛰️ Max elevation gain", "+${formatNum(r.maxElevationGainMeters)} m", NeonCyan) }
                }

                item { SectionHeader("TOTALS") }
                item { StatRow("📋 Workouts", "${r.totalWorkouts}", MutedGray) }
                item { StatRow("📐 Total distance", formatDistance(r.totalDistanceMeters), MutedGray) }
            } else {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = NeonCyan,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(text = value, fontSize = 11.sp, color = color, fontWeight = FontWeight.Black)
    }
}

private fun formatDistance(meters: Double): String = when {
    meters <= 0.0 -> "--"
    meters >= 1000.0 -> formatNum(meters / 1000.0) + " km"
    else -> "${meters.toInt()} m"
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "--"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m} min"
}

private fun formatPace(secondsPerKm: Double): String {
    val total = secondsPerKm.toInt()
    val m = total / 60
    val s = total % 60
    return String.format("%d'%02d\" /km", m, s)
}

private fun formatNum(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else String.format("%.1f", value)