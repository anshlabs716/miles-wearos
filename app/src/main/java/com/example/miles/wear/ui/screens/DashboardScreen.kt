package com.example.miles.wear.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.example.miles.wear.R
import com.example.miles.wear.data.local.entity.PetEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity
import com.example.miles.wear.data.model.GpsStatus
import com.example.miles.wear.data.model.PetType
import com.example.miles.wear.data.model.WeeklyProgress
import com.example.miles.wear.data.model.WorkoutType
import com.example.miles.wear.engine.WearWeather
import com.example.miles.wear.engine.WearWeatherFetcher
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onStartWorkout: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenCompass: () -> Unit,
    onStartQuickWorkout: (WorkoutType) -> Unit,
    onOpenMirrored: () -> Unit,
    onSelectSession: (Long) -> Unit,
    onOpenRecords: () -> Unit = {},
    onOpenPet: () -> Unit = {},
    onOpenRoutes: () -> Unit = {},
    onOpenTraining: () -> Unit = {},
    onOpenExport: () -> Unit = {}
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val sensorTracker = MilesWearApplication.instance.sensorTracker
    val phoneMessaging = MilesWearApplication.instance.phoneMessagingManager
    val repository = MilesWearApplication.instance.repository

    val metrics by sensorTracker.liveMetrics.collectAsStateWithLifecycle()
    val liveHr by sensorTracker.liveHeartRate.collectAsStateWithLifecycle()
    val connection by phoneMessaging.connectionStatus.collectAsStateWithLifecycle()
    val mirroredMetrics by phoneMessaging.mirroredMetrics.collectAsStateWithLifecycle()
    val unsyncedCount by repository.unsyncedCount.collectAsStateWithLifecycle(initialValue = 0)
    val sessions by repository.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val todaySessions by repository.getTodaySessions().collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by repository.settings.collectAsStateWithLifecycle()
    val gpsStatus by sensorTracker.gpsStatus.collectAsStateWithLifecycle()
    val batteryPercent by sensorTracker.batteryPercent.collectAsStateWithLifecycle()

    // Real current weather (Open-Meteo), fetched once on load, tap to refresh
    val context = LocalContext.current
    var weather by remember { mutableStateOf(WearWeather()) }
    var weatherRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        weather = WearWeatherFetcher.fetch(context)
    }

    // Fitness pet + weekly goals (real data, refreshed on load)
    var pet by remember { mutableStateOf<PetEntity?>(null) }
    var weekly by remember { mutableStateOf<WeeklyProgress?>(null) }
    LaunchedEffect(Unit) {
        pet = repository.getPet()
        weekly = repository.computeWeeklyProgress()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        phoneMessaging.refreshConnectedNodes()
    }

    // Real aggregated metrics for Today (from live sensors + completed workouts today)
    val todayWorkoutSteps = todaySessions.sumOf { it.totalSteps }
    val displaySteps = if (metrics.dailySteps > 0) {
        metrics.dailySteps
    } else {
        todayWorkoutSteps + metrics.steps
    }

    val todayWorkoutDistance = todaySessions.sumOf { it.distanceMeters }
    val displayDistanceMeters = todayWorkoutDistance + metrics.distanceMeters
    val formattedDistance = settings.unit.formatDistance(displayDistanceMeters)

    val todayWorkoutSeconds = todaySessions.sumOf { it.durationSeconds } + metrics.elapsedSeconds
    val todayActiveMinutes = (todayWorkoutSeconds / 60).toInt()

    val todayCalories = todaySessions.sumOf { it.caloriesKcal } + metrics.caloriesKcal

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
            // Header: Official MILES Logo & Title
            item {
                val headerPadding = if (settings.compactCards) 10.dp else 16.dp
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = headerPadding, bottom = 2.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_miles_logo),
                        contentDescription = "MILES Logo",
                        modifier = Modifier.size(if (settings.compactCards) 22.dp else 28.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "MILES",
                        fontSize = if (settings.compactCards) 14.sp else 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )

                    // Quick Glanceable Device Status: Watch Battery & GPS (Configurable in Settings)
                    if (settings.showBatteryInDashboard || settings.showGpsInDashboard) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (settings.showBatteryInDashboard) {
                                // Watch Battery Status
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🔋", fontSize = 9.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$batteryPercent%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (batteryPercent > 20) VividGreen else CoralFlame
                                    )
                                }
                            }

                            if (settings.showGpsInDashboard) {
                                // GPS Status Pill
                                val gpsColor = when (gpsStatus) {
                                    GpsStatus.READY -> VividGreen
                                    GpsStatus.SEARCHING -> ElectricAmber
                                    GpsStatus.UNAVAILABLE -> CoralFlame
                                    GpsStatus.INDOOR -> MutedGray
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "📍", fontSize = 9.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = gpsStatus.label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = gpsColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Phone Mirrored Workout Banner (if active)
            if (mirroredMetrics.isPhoneActive) {
                item {
                    Chip(
                        onClick = onOpenMirrored,
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF00364F),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 4.dp),
                        label = {
                            Text(
                                text = "📱 Phone Workout Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = "Tap to view live wrist HUD",
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        }
                    )
                }
            }

            // Today's Activity Card (Live hardware sensors & today's recorded workouts)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF14161C))
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = "TODAY'S STEPS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = String.format("%,d", displaySteps),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = VividGreen
                    )

                    // Distance (from active workouts and recorded daily sessions)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Distance: ",
                            fontSize = 11.sp,
                            color = MutedGray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$formattedDistance ${settings.unit.distanceLabel}",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary Glanceables: Active Time, Calories, Heart Rate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Active time
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$todayActiveMinutes min", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            Text(text = "Active", fontSize = 8.sp, color = MutedGray)
                        }
                        // Calories
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$todayCalories kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoralFlame)
                            Text(text = "Burned", fontSize = 8.sp, color = MutedGray)
                        }
                        // Heart Rate
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val hrText = if (!sensorTracker.isHeartRateSensorPresent) {
                                "♥ --"
                            } else if (liveHr.bpm > 0) {
                                "♥ ${liveHr.bpm}"
                            } else {
                                "♥ --"
                            }
                            Text(text = hrText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricAmber)
                            Text(
                                text = if (sensorTracker.isHeartRateSensorPresent) "BPM" else "No Sensor",
                                fontSize = 8.sp,
                                color = MutedGray
                            )
                        }
                    }

                    // Daily Activity Progress Bar (configurable daily goal from Settings)
                    Spacer(modifier = Modifier.height(6.dp))
                    val stepGoal = settings.stepGoal
                    val progressFraction = (displaySteps.toFloat() / stepGoal).coerceIn(0f, 1f)
                    val progressPercent = (progressFraction * 100).toInt()
                    val goalLabel = if (stepGoal >= 1000 && stepGoal % 1000 == 0) {
                        "${stepGoal / 1000}k"
                    } else {
                        "$stepGoal"
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "DAILY GOAL ($goalLabel)", fontSize = 8.sp, color = MutedGray, fontWeight = FontWeight.Bold)
                            Text(text = "$progressPercent%", fontSize = 8.sp, color = VividGreen, fontWeight = FontWeight.Bold)
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
                                    .fillMaxWidth(progressFraction)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(VividGreen)
                            )
                        }
                    }
                }
            }

            // Weather (real Open-Meteo data, tap to refresh)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val w = weather
                Chip(
                    onClick = {
                        weatherRefreshing = true
                        coroutineScope.launch {
                            weather = WearWeatherFetcher.fetch(context)
                            weatherRefreshing = false
                        }
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌦️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            if (w.isAvailable) {
                                Text(
                                    text = "${w.emoji ?: ""} ${w.temperatureC?.toInt() ?: "--"}° • ${w.condition ?: ""}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = if (weatherRefreshing) "Refreshing…" else "Weather",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = if (w.isAvailable) {
                                "Wind ${w.windSpeedKmh?.toInt() ?: "--"} km/h"
                            } else {
                                "Offline — tap to retry"
                            },
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Fitness Pet (real steps feed it; tap to open pet screen)
            item {
                val p = pet
                val petEmoji = p?.let { PetType.fromName(it.petType).emoji } ?: "🐕"
                val petName = p?.petName ?: "Adopt a Pet"
                val current = pet
                Chip(
                    onClick = onOpenPet,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (current == null) Color(0xFF2A1F00) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = petEmoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = petName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (current == null) ElectricAmber else Color.White
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = if (current == null) "Feed it with real steps" else "Fed by real steps · tap to visit",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Weekly Goals (real distance + active time for this week)
            item {
                val w = weekly
                val hasGoals = (w?.distanceGoalKm ?: 0.0) > 0 || (w?.minutesGoal ?: 0) > 0
                Chip(
                    onClick = onOpenRecords,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏆", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasGoals && w != null) "Week ${w.weekLabel}" else "Weekly Goals",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasGoals) NeonCyan else MutedGray
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = if (hasGoals && w != null) {
                                buildString {
                                    if (w.distanceGoalKm > 0) append("${formatKm(w.distanceKm)}/${formatKm(w.distanceGoalKm)} km")
                                    if (w.distanceGoalKm > 0 && w.minutesGoal > 0) append(" · ")
                                    if (w.minutesGoal > 0) append("${w.activeMinutes}/${w.minutesGoal} min")
                                }
                            } else {
                                "Set goals in Records"
                            },
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Large Primary Action: START WORKOUT
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    onClick = onStartWorkout,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = VividGreen,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 4.dp),
                    label = {
                        Text(
                            text = "START WORKOUT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                    }
                )
            }

            // Quick Actions: one-tap workout starts (real tracking, same engine as START WORKOUT)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.94f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickStartChip(
                        text = "🚶 Walk",
                        onClick = { onStartQuickWorkout(WorkoutType.WALK) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickStartChip(
                        text = "🏃 Run",
                        onClick = { onStartQuickWorkout(WorkoutType.RUN) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(0.94f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickStartChip(
                        text = "🚴 Bike",
                        onClick = { onStartQuickWorkout(WorkoutType.CYCLING) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickStartChip(
                        text = "⛰️ Hike",
                        onClick = { onStartQuickWorkout(WorkoutType.HIKE) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Quick Access: Map
            item {
                Chip(
                    onClick = onOpenMap,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🗺️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Map",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = "OpenStreetMap • Current location",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Quick Access: Compass
            item {
                Chip(
                    onClick = onOpenCompass,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🧭", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compass",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricAmber
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = "Magnetic heading",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Quick Access: History
            item {
                Chip(
                    onClick = onOpenHistory,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📋", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "History",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = "${sessions.size} recorded sessions",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Quick Access: Records & Streaks
            item {
                Chip(
                    onClick = onOpenRecords,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF1A1408),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏆", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Records & Streaks",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricAmber
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = "PRs from real workout history",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Quick Access: Saved Routes
            item {
                Chip(
                    onClick = onOpenRoutes,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF0E2433),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📍", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Routes",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = "Saved routes • follow anytime",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Quick Access: Training Plans
            item {
                Chip(
                    onClick = onOpenTraining,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF33141E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏋️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Training Plans",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralFlame
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = "C25K • 5K • 10K • HIIT",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Quick Access: Settings
            item {
                Chip(
                    onClick = onOpenSettings,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚙️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Settings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = "Units, Sensors & Privacy",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Quick Access: Export & Backup
            item {
                Chip(
                    onClick = onOpenExport,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF15201A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💾", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Export & Backup",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    secondaryLabel = {
                        Text(
                            text = "GPX • CSV • JSON • Full backup",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Quick Access: Hardware Diagnostics
            item {
                Chip(
                    onClick = onOpenDiagnostics,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF141418),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sensor Diagnostics",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan
                            )
                        }
                    }
                )
            }

            // Phone Connection Status Card
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141416))
                        .clickable {
                            coroutineScope.launch {
                                phoneMessaging.refreshConnectedNodes()
                                phoneMessaging.flushOfflineQueue()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (connection.isConnected) VividGreen else ElectricAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (connection.isConnected) "Phone Connected (${connection.phoneNodeName})" else "Standalone Mode (Tap to sync)",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    if (unsyncedCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• $unsyncedCount queue",
                            fontSize = 9.sp,
                            color = CoralFlame,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Last Workout Snapshot (if exists)
            val latest = sessions.firstOrNull()
            if (latest != null) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "LAST WORKOUT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedGray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                item {
                    val durationMin = latest.durationSeconds / 60
                    val distKm = settings.unit.formatDistance(latest.distanceMeters)
                    val emoji = WorkoutType.fromString(latest.workoutType).emoji

                    Chip(
                        onClick = { onSelectSession(latest.id) },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF18181C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 2.dp),
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = latest.workoutType,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        },
                        secondaryLabel = {
                            Text(
                                text = "$durationMin min • $distKm ${settings.unit.distanceLabel} • ${latest.caloriesKcal} kcal",
                                fontSize = 9.sp,
                                color = VividGreen
                            )
                        }
                    )
                }

                // Recent Activities Summary (up to 3 recent workouts, configurable in settings)
                if (settings.showRecentActivitiesInDashboard && sessions.size > 1) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "RECENT ACTIVITIES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedGray,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    val recentSublist = sessions.drop(1).take(2)
                    for (pastSession in recentSublist) {
                        item {
                            val durMin = pastSession.durationSeconds / 60
                            val dist = settings.unit.formatDistance(pastSession.distanceMeters)
                            val actEmoji = WorkoutType.fromString(pastSession.workoutType).emoji

                            Chip(
                                onClick = { onSelectSession(pastSession.id) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = Color(0xFF141416),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .padding(vertical = 2.dp),
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = actEmoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = pastSession.workoutType,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                },
                                secondaryLabel = {
                                    Text(
                                        text = "$durMin min • $dist ${settings.unit.distanceLabel}",
                                        fontSize = 8.sp,
                                        color = MutedGray
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/** Small two-button quick-start chip used for one-tap workout launch. */
@Composable
private fun QuickStartChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        onClick = onClick,
        colors = ChipDefaults.chipColors(
            backgroundColor = Color(0xFF23262E),
            contentColor = Color.White
        ),
        modifier = modifier.padding(vertical = 2.dp),
        label = {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    )
}

private fun formatKm(km: Double): String =
    if (km == km.toLong().toDouble()) km.toLong().toString() else String.format("%.1f", km)
