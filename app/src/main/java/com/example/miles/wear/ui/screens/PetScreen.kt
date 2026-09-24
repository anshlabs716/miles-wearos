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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.example.miles.wear.data.local.entity.PetEntity
import com.example.miles.wear.data.model.PetType
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

private val PET_NAMES = listOf("Miles", "Pixel", "Sport", "Rex", "Mochi")

/**
 * Fitness Pet companion — fed entirely by real steps. Adopt a pet, see its
 * feeding progress from today's real steps, and collect treats for every day
 * the step goal is hit. Lazy days let you skip a workday without losing it.
 */
@Composable
fun PetScreen(onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val repository = MilesWearApplication.instance.repository
    val sensorTracker = MilesWearApplication.instance.sensorTracker

    val settings by repository.settings.collectAsStateWithLifecycle()
    val metrics by sensorTracker.liveMetrics.collectAsStateWithLifecycle()
    val todaySessions by repository.getTodaySessions().collectAsStateWithLifecycle(initialValue = emptyList())

    var pet by remember { mutableStateOf<PetEntity?>(null) }
    var treats by remember { mutableStateOf(0) }
    var adopting by remember { mutableStateOf(false) }
    var chosenType by remember { mutableStateOf(PetType.DOG) }
    var chosenName by remember { mutableStateOf(PET_NAMES.first()) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        pet = repository.getPet()
        treats = repository.treatsFed()
    }

    val todaySteps = if (metrics.dailySteps > 0) metrics.dailySteps else todaySessions.sumOf { it.totalSteps } + metrics.steps
    val fedFraction = (todaySteps.toFloat() / settings.stepGoal).coerceIn(0f, 1f)
    val fedToday = todaySteps >= settings.stepGoal

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
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "🐕 FITNESS PET",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricAmber,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Fed by real steps only",
                        fontSize = 9.sp,
                        color = MutedGray
                    )
                }
            }

            val current = pet
            if (current == null) {
                item {
                    Text(
                        text = "👋 No pet yet\nAdopt a companion!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14161C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = PetType.fromName(current.petType).emoji, fontSize = 44.sp)
                    }
                }
                item {
                    Text(
                        text = "${current.petName} (${PetType.fromName(current.petType).label})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF14161C))
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = "🍽️ TODAY'S FEEDING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedGray,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (fedToday) "Fully fed! 🎉" else "Hungry… ${todaySteps} / ${settings.stepGoal} steps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fedToday) VividGreen else Color.White,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF222834))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fedFraction)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(VividGreen)
                            )
                        }
                    }
                }

                item {
                    Chip(
                        onClick = { adopting = true },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF18181C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 2.dp),
                        label = {
                            Text("🦴 Treats earned: $treats", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        },
                        secondaryLabel = {
                            Text("Tap to change pet", fontSize = 9.sp, color = MutedGray)
                        }
                    )
                }

                item {
                    Text(
                        text = "😴 Lazy days (${settings.lazyDaysPerWeek}/week): rest days still protect your streaks",
                        fontSize = 9.sp,
                        color = MutedGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 4.dp)
                    )
                }
            }

            item {
                Chip(
                    onClick = { adopting = true },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = VividGreen,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 4.dp),
                    label = {
                        Text(
                            text = if (current == null) "Adopt a Pet" else "Change Pet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = onBack,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 4.dp),
                    label = {
                        Text("← Back", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }
        }
    }

    if (adopting) {
        AdoptionSheet(
            chosenType = chosenType,
            chosenName = chosenName,
            onTypeChange = { chosenType = it },
            onNameChange = { chosenName = it },
            onConfirm = {
                coroutineScope.launch {
                    repository.savePet(chosenType, chosenName)
                    pet = repository.getPet()
                    treats = repository.treatsFed()
                    adopting = false
                }
            },
            onDismiss = { adopting = false }
        )
    }
}

@Composable
private fun AdoptionSheet(
    chosenType: PetType,
    chosenName: String,
    onTypeChange: (PetType) -> Unit,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ADOPT A PET",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = ElectricAmber,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PetType.entries.forEach { type ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == chosenType) Color(0xFF00364F) else Color(0xFF14161C))
                            .padding(6.dp)
                            .clickable { onTypeChange(type) }
                    ) {
                        Text(text = type.emoji, fontSize = 22.sp)
                        Text(text = type.label, fontSize = 8.sp, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NAME",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MutedGray,
                modifier = Modifier.fillMaxWidth(0.92f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PET_NAMES.forEach { name ->
                    Chip(
                        onClick = { onNameChange(name) },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (name == chosenName) NeonCyan else Color(0xFF18181C),
                            contentColor = if (name == chosenName) Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(text = name, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Chip(
                onClick = onConfirm,
                colors = ChipDefaults.chipColors(backgroundColor = VividGreen, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth(0.92f),
                label = {
                    Text(
                        text = "Adopt ${chosenType.emoji} $chosenName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Chip(
                onClick = onDismiss,
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF18181C), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(0.92f),
                label = {
                    Text(
                        text = "Cancel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            )
        }
    }
}