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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.miles.wear.data.model.AchievementBadge
import com.example.miles.wear.data.model.RecordsSummary
import com.example.miles.wear.data.model.WeeklyProgress
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

private val KM_GOALS = listOf(0.0, 5.0, 10.0, 15.0, 25.0, 50.0)
private val MIN_GOALS = listOf(0, 60, 120, 180, 300, 420)

/**
 * Streaks, personal records, weekly goals and achievement badges — all
 * computed from real saved workout history and daily stats. Nothing hardcoded.
 */
@Composable
fun RecordsScreen() {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val repository = MilesWearApplication.instance.repository

    var records by remember { mutableStateOf<RecordsSummary?>(null) }
    var weekly by remember { mutableStateOf<WeeklyProgress?>(null) }
    var badges by remember { mutableStateOf<List<AchievementBadge>>(emptyList()) }
    val settings by repository.settings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        records = repository.computeRecords()
        weekly = repository.computeWeeklyProgress()
        badges = repository.computeBadges()
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

            // ----- Weekly goals (real data, tap chips to change goal) -----
            val w = weekly
            if (w != null) {
                item { SectionHeader("🎯 WEEKLY GOALS • ${w.weekLabel}") }

                // Distance goal editor + progress
                item {
                    GoalEditorRow(
                        label = "Distance goal",
                        valueLabel = goalKmLabel(settings.weeklyDistanceKm)
                    ) {
                        val next = KM_GOALS[(KM_GOALS.indexOf(settings.weeklyDistanceKm) + 1) % KM_GOALS.size]
                        repository.updateSettings(settings.copy(weeklyDistanceKm = next))
                    }
                }
                if (settings.weeklyDistanceKm > 0) {
                    item {
                        ProgressRow(
                            label = "Distance",
                            value = "${formatKm(w.distanceKm)} / ${formatKm(w.distanceGoalKm)} km",
                            fraction = (w.distanceKm / w.distanceGoalKm).toFloat().coerceIn(0f, 1f)
                        )
                    }
                }
                // Active time goal editor + progress
                item {
                    GoalEditorRow(
                        label = "Time goal",
                        valueLabel = goalMinLabel(settings.weeklyActiveMinutes)
                    ) {
                        val next = MIN_GOALS[(MIN_GOALS.indexOf(settings.weeklyActiveMinutes) + 1) % MIN_GOALS.size]
                        repository.updateSettings(settings.copy(weeklyActiveMinutes = next))
                    }
                }
                if (settings.weeklyActiveMinutes > 0) {
                    item {
                        ProgressRow(
                            label = "Active time",
                            value = "${w.activeMinutes} / ${settings.weeklyActiveMinutes} min",
                            fraction = (w.activeMinutes.toFloat() / settings.weeklyActiveMinutes).coerceIn(0f, 1f)
                        )
                    }
                }
                if (settings.weeklyDistanceKm == 0.0 && settings.weeklyActiveMinutes == 0) {
                    item {
                        Text(
                            text = "Tap a goal above to set it (0 = off)",
                            fontSize = 9.sp,
                            color = MutedGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // ----- Achievement badges (computed from real data) -----
            if (badges.isNotEmpty()) {
                item { SectionHeader("🏅 BADGES") }
                badges.chunked(2).forEach { pair ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pair.forEach { badge ->
                                BadgeTile(
                                    badge = badge,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
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
private fun GoalEditorRow(label: String, valueLabel: String, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14161C))
            .clickable { onTap() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
        Text(text = valueLabel, fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ProgressRow(label: String, value: String, fraction: Float) {
    Column(modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 9.sp, color = MutedGray)
            Text(text = value, fontSize = 9.sp, color = VividGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF222834))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VividGreen)
            )
        }
    }
}

@Composable
private fun BadgeTile(badge: AchievementBadge, modifier: Modifier = Modifier) {
    val dim = !badge.unlocked
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (dim) Color(0xFF131418) else Color(0xFF1A2416))
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Text(
            text = badge.emoji,
            fontSize = 20.sp,
            modifier = Modifier.alpha(if (dim) 0.35f else 1f)
        )
        Text(
            text = badge.title,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (dim) MutedGray else Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (dim) badge.progress else "EARNED ✓",
            fontSize = 7.sp,
            color = if (dim) MutedGray else VividGreen,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
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

private fun formatKm(km: Double): String =
    if (km == km.toLong().toDouble()) km.toLong().toString() else String.format("%.1f", km)

private fun goalKmLabel(km: Double): String = if (km <= 0) "OFF" else "${formatKm(km)} km"

private fun goalMinLabel(min: Int): String = when {
    min <= 0 -> "OFF"
    min % 60 == 0 -> "${min / 60}h"
    else -> "$min min"
}