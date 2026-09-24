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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.miles.wear.data.model.PlanDay
import com.example.miles.wear.data.model.PlanWeek
import com.example.miles.wear.data.model.TrainingCatalog
import com.example.miles.wear.data.model.TrainingPlan
import com.example.miles.wear.data.model.TrainingPlanState
import com.example.miles.wear.data.model.WorkoutMode
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

/**
 * Progressive training plans (C25K-style). Every workout day launches a real
 * interval/goal session; completing it marks the day ✓ in the plan.
 */
@Composable
fun TrainingPlansScreen(
    onSelectPlan: (String) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val repository = MilesWearApplication.instance.repository

    var state by remember { mutableStateOf<TrainingPlanState?>(null) }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        state = repository.trainingPlanState()
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
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = "🏋️ TRAINING PLANS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text("Real progressive programs", fontSize = 9.sp, color = MutedGray)
                }
            }

            item {
                Chip(
                    onClick = onBack,
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF18181C), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(0.94f),
                    label = {
                        Text("⬅ Back", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedGray)
                    }
                )
            }

            TrainingCatalog.all.forEach { plan ->
                val progress = state?.takeIf { it.planKey == plan.key }
                item {
                    PlanCard(
                        plan = plan,
                        isActive = progress?.started == true,
                        completed = progress?.completedDayIds?.count { plan.workoutDays.any { d -> d.id == it } } ?: 0,
                        onClick = { onSelectPlan(plan.key) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlanCard(plan: TrainingPlan, isActive: Boolean, completed: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 3.dp)
            .background(
                if (isActive) Color(0xFF14332A) else Color(0xFF18181C),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = plan.emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = plan.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) VividGreen else Color.White
                )
                Text(
                    text = plan.description,
                    fontSize = 8.sp,
                    color = MutedGray,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            if (isActive) {
                Text(text = "●", fontSize = 14.sp, color = VividGreen)
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        ProgressBar(completed, plan.totalWorkouts)
        Spacer(modifier = Modifier.height(5.dp))
        Chip(
            onClick = onClick,
            colors = ChipDefaults.chipColors(backgroundColor = if (isActive) VividGreen else Color(0xFF25252B), contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    text = if (isActive) "$completed / ${plan.totalWorkouts} done — open" else "View weeks",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.Black else Color.White,
                    textAlign = TextAlign.Center
                )
            }
        )
    }
}

@Composable
fun ProgressBar(completed: Int, total: Int) {
    val fraction = if (total <= 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .weight(1f)
                .height(5.dp)
                .background(Color(0xFF2A2A31), RoundedCornerShape(3.dp))
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(5.dp)
                    .background(VividGreen, RoundedCornerShape(3.dp))
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$completed/$total", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MutedGray)
    }
}

/** Weeks + days of one plan; tap a workout day to start it for real. */
@Composable
fun TrainingPlanDetailScreen(
    planKey: String,
    onStartDay: (PlanDay) -> Unit,
    onBack: () -> Unit
) {
    val plan = TrainingCatalog.byKey(planKey) ?: return
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val repository = MilesWearApplication.instance.repository

    var state by remember { mutableStateOf<TrainingPlanState?>(null) }
    var starting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        state = repository.trainingPlanState()
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
                        text = "${plan.emoji} ${plan.name.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = plan.description,
                        fontSize = 9.sp,
                        color = MutedGray,
                        textAlign = TextAlign.Center
                    )
                    val completed = state?.completedDayIds?.count { plan.workoutDays.any { d -> d.id == it } } ?: 0
                    Spacer(modifier = Modifier.height(4.dp))
                    ProgressBar(completed, plan.totalWorkouts)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Chip(
                        onClick = onBack,
                        colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF18181C), contentColor = Color.White),
                        modifier = Modifier.weight(1f),
                        label = {
                            Text("⬅ Back", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedGray)
                        }
                    )
                    val activePlan = state?.takeIf { it.started && it.planKey == plan.key }
                    if (activePlan == null) {
                        Chip(
                            onClick = {
                                starting = true
                                coroutineScope.launch {
                                    repository.startTrainingPlan(plan.key)
                                    state = repository.trainingPlanState()
                                    starting = false
                                }
                            },
                            colors = ChipDefaults.chipColors(backgroundColor = VividGreen, contentColor = Color.Black),
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    if (starting) "Starting…" else "▶ Start program",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }
            }

            if (state?.started != true || state?.planKey != plan.key) {
                item {
                    Text(
                        text = "Tap Start to begin. Weeks unlock as you go — 3 sessions per week, rest days in between.",
                        fontSize = 10.sp,
                        color = MutedGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            plan.weeks.forEach { week ->
                item {
                    WeekHeader(week)
                }
                week.days.forEach { day ->
                    item {
                        DayRow(
                            day = day,
                            done = state?.completedDayIds?.contains(day.id) == true,
                            disabled = state?.started != true || state?.planKey != plan.key || day.isRest,
                            onStart = { onStartDay(day) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun WeekHeader(week: PlanWeek) {
    Text(
        text = "WEEK ${week.number}",
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = ElectricAmber,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth(0.92f).padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun DayRow(day: PlanDay, done: Boolean, disabled: Boolean, onStart: () -> Unit) {
    if (day.isRest) {
        Row(
            Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 2.dp)
                .background(Color(0xFF18181C), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${day.weekday}  ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedGray)
            Text(text = "😴 Rest day", fontSize = 10.sp, color = MutedGray)
        }
        return
    }

    val accent = when {
        disabled -> Color(0xFF141416)
        done -> Color(0xFF1B3A2C)
        else -> Color(0xFF18181C)
    }
    Chip(
        onClick = { if (!disabled) onStart() },
        colors = ChipDefaults.chipColors(
            backgroundColor = accent,
            contentColor = Color.White
        ),
        enabled = !disabled,
        modifier = Modifier.fillMaxWidth(0.96f).padding(vertical = 2.dp),
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${day.weekday} · ${day.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (disabled) MutedGray else Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = day.detail(),
                        fontSize = 8.sp,
                        color = if (disabled) MutedGray.copy(alpha = 0.6f) else MutedGray,
                        maxLines = 1
                    )
                }
                if (done) {
                    Text(text = "✓", fontSize = 14.sp, fontWeight = FontWeight.Black, color = VividGreen)
                } else if (!disabled) {
                    Text(text = "▶", fontSize = 12.sp, fontWeight = FontWeight.Black, color = CoralFlame)
                }
            }
        }
    )
}

/** Human-readable detail line for a workout day (real plan specifics). */
private fun PlanDay.detail(): String = when {
    isRest -> "Rest"
    plan.mode == WorkoutMode.INTERVAL ->
        "${plan.repetitions} × ${fmtSec(plan.workSeconds)} / ${fmtSec(plan.recoverySeconds)} rest"
    plan.mode == WorkoutMode.GOAL -> "Goal ${fmtGoal(plan.goalValue)}"
    else -> "Free workout"
}

private fun fmtSec(s: Int): String = if (s % 60 == 0) "${s / 60}m" else if (s > 60) "${s / 60}m${s % 60}s" else "${s}s"

private fun fmtGoal(v: Double): String =
    if (v == v.toLong().toDouble()) "${v.toLong()} min" else "${v} km"