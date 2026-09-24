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
import com.example.miles.wear.data.model.DistanceUnit
import com.example.miles.wear.data.model.HudLayoutMode
import com.example.miles.wear.data.model.PrimaryMetricType
import com.example.miles.wear.data.model.ThemeAccent
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.NeonPurple
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = MilesWearApplication.instance.repository
    val settings by repository.settings.collectAsStateWithLifecycle()
    val phoneMessaging = MilesWearApplication.instance.phoneMessagingManager
    val phoneStatus by phoneMessaging.connectionStatus.collectAsStateWithLifecycle()
    val unsyncedCount by repository.unsyncedCount.collectAsStateWithLifecycle(initialValue = 0)

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
                        text = "SETTINGS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Preferences & Privacy",
                        fontSize = 10.sp,
                        color = MutedGray
                    )
                }
            }

            // Section: Units
            item {
                Text(
                    text = "UNITS OF MEASURE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Chip(
                    onClick = {
                        val nextUnit = if (settings.unit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC
                        repository.updateSettings(settings.copy(unit = nextUnit))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Distance: ${settings.unit.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.unit == DistanceUnit.METRIC) "Kilometers (km, /km)" else "Miles (mi, /mi)",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Section: Daily Goal
            item {
                Text(
                    text = "DAILY STEP GOAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }
            item {
                val goals = intArrayOf(5000, 8000, 10000, 15000, 20000, 25000)
                Chip(
                    onClick = {
                        val current = settings.stepGoal
                        val idx = goals.indexOf(current)
                        val next = goals[(idx + 1).coerceAtLeast(0) % goals.size]
                        repository.updateSettings(settings.copy(stepGoal = next))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Goal: ${settings.stepGoal} steps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VividGreen
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Tap to change (5k–25k). Goal bar + tile use this.",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        val options = listOf(0, 300, 400, 500, 750, 1000)
                        val next = options[(options.indexOf(settings.calorieGoalKcal) + 1) % options.size]
                        repository.updateSettings(settings.copy(calorieGoalKcal = next))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.calorieGoalKcal > 0) Color(0xFF301A20) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = if (settings.calorieGoalKcal > 0) "Calorie Goal: ${settings.calorieGoalKcal} kcal/day" else "Calorie Goal: Off",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (settings.calorieGoalKcal > 0) CoralFlame else Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Tap to cycle (off, 300–1000). Dashboard shows progress.",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            // Section: Themes & Visual Customization
            item {
                Text(
                    text = "THEME & ACCENT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Chip(
                    onClick = {
                        val accents = ThemeAccent.values()
                        val nextIdx = (settings.themeAccent.ordinal + 1) % accents.size
                        repository.updateSettings(settings.copy(themeAccent = accents[nextIdx]))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Color: ${settings.themeAccent.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(settings.themeAccent.colorHex)
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Tap to cycle theme palette",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(highContrastText = !settings.highContrastText))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.highContrastText) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "High Contrast Mode",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.highContrastText) "ON • Maximum readability" else "OFF • Standard M3",
                            fontSize = 9.sp,
                            color = if (settings.highContrastText) VividGreen else MutedGray
                        )
                    }
                )
            }

            // Section: Dashboard Customization
            item {
                Text(
                    text = "DASHBOARD CUSTOMIZATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(compactCards = !settings.compactCards))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.compactCards) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Compact Card Layout",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.compactCards) "Compact • Dense info" else "Expanded • High spacing",
                            fontSize = 9.sp,
                            color = if (settings.compactCards) VividGreen else MutedGray
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(showRecentActivitiesInDashboard = !settings.showRecentActivitiesInDashboard))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.showRecentActivitiesInDashboard) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Recent Activities Card",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.showRecentActivitiesInDashboard) "Visible on Home" else "Hidden on Home",
                            fontSize = 9.sp,
                            color = if (settings.showRecentActivitiesInDashboard) VividGreen else MutedGray
                        )
                    }
                )
            }

            // Section: Workout HUD Customization
            item {
                Text(
                    text = "WORKOUT HUD & METRICS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricAmber,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Chip(
                    onClick = {
                        val metrics = PrimaryMetricType.values()
                        val nextIdx = (settings.primaryMetric.ordinal + 1) % metrics.size
                        repository.updateSettings(settings.copy(primaryMetric = metrics[nextIdx]))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Primary: ${settings.primaryMetric.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Hero metric featured during workout",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        val layouts = HudLayoutMode.values()
                        val nextIdx = (settings.hudLayout.ordinal + 1) % layouts.size
                        repository.updateSettings(settings.copy(hudLayout = layouts[nextIdx]))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "HUD: ${settings.hudLayout.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Tap to switch HUD presentation",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(showGpsMapInHud = !settings.showGpsMapInHud))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.showGpsMapInHud) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Live GPS Route Map",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.showGpsMapInHud) "ON • Show route breadcrumb" else "OFF • Stats only",
                            fontSize = 9.sp,
                            color = if (settings.showGpsMapInHud) VividGreen else MutedGray
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(showHeartRateZoneRing = !settings.showHeartRateZoneRing))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.showHeartRateZoneRing) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "HR Zone Dial Ring",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.showHeartRateZoneRing) "ON • Color-coded bezel ring" else "OFF",
                            fontSize = 9.sp,
                            color = if (settings.showHeartRateZoneRing) VividGreen else MutedGray
                        )
                    }
                )
            }

            // Section: Display & Screen
            item {
                Text(
                    text = "DISPLAY & AOD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(keepScreenOn = !settings.keepScreenOn))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.keepScreenOn) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Keep Screen On",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.keepScreenOn) "ON • During active workout" else "OFF • Allow screen timeout",
                            fontSize = 9.sp,
                            color = if (settings.keepScreenOn) VividGreen else MutedGray
                        )
                    }
                )
            }

            // Section: Tracking Hardware
            item {
                Text(
                    text = "TRACKING SENSORS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(gpsEnabled = !settings.gpsEnabled))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.gpsEnabled) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "GPS Tracking",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.gpsEnabled) "ON • Enabled for routes" else "OFF • Indoor mode",
                            fontSize = 9.sp,
                            color = if (settings.gpsEnabled) VividGreen else MutedGray
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(hrEnabled = !settings.hrEnabled))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.hrEnabled) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Heart Rate Monitor",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.hrEnabled) "ON • Continuous wrist telemetry" else "OFF",
                            fontSize = 9.sp,
                            color = if (settings.hrEnabled) VividGreen else MutedGray
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(stepTrackingEnabled = !settings.stepTrackingEnabled))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.stepTrackingEnabled) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Step Counter",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.stepTrackingEnabled) "ON • Hardware pedometer" else "OFF",
                            fontSize = 9.sp,
                            color = if (settings.stepTrackingEnabled) VividGreen else MutedGray
                        )
                    }
                )
            }

            // Section: Workout Behavior & Battery
            item {
                Text(
                    text = "WORKOUT & BATTERY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricAmber,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(autoPauseEnabled = !settings.autoPauseEnabled))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.autoPauseEnabled) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Auto-Pause",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.autoPauseEnabled) "ON • Pauses when stationary" else "OFF • Manual control only",
                            fontSize = 9.sp,
                            color = if (settings.autoPauseEnabled) VividGreen else MutedGray
                        )
                    }
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(hapticAlertsEnabled = !settings.hapticAlertsEnabled))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.hapticAlertsEnabled) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Haptic Cues Master",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.hapticAlertsEnabled) "ON • Vibrations active" else "OFF • All silent",
                            fontSize = 9.sp,
                            color = if (settings.hapticAlertsEnabled) VividGreen else MutedGray
                        )
                    }
                )
            }

            if (settings.hapticAlertsEnabled) {
                item {
                    Chip(
                        onClick = {
                            repository.updateSettings(settings.copy(workoutStartHaptic = !settings.workoutStartHaptic))
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (settings.workoutStartHaptic) Color(0xFF182A3A) else Color(0xFF18181C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 2.dp),
                        label = {
                            Text(
                                text = "Start / Countdown Haptic",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = if (settings.workoutStartHaptic) "Enabled" else "Disabled",
                                fontSize = 9.sp,
                                color = if (settings.workoutStartHaptic) VividGreen else MutedGray
                            )
                        }
                    )
                }

                item {
                    Chip(
                        onClick = {
                            repository.updateSettings(settings.copy(workoutPauseResumeHaptic = !settings.workoutPauseResumeHaptic))
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (settings.workoutPauseResumeHaptic) Color(0xFF182A3A) else Color(0xFF18181C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 2.dp),
                        label = {
                            Text(
                                text = "Pause / Resume Haptic",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = if (settings.workoutPauseResumeHaptic) "Enabled" else "Disabled",
                                fontSize = 9.sp,
                                color = if (settings.workoutPauseResumeHaptic) VividGreen else MutedGray
                            )
                        }
                    )
                }

                item {
                    Chip(
                        onClick = {
                            repository.updateSettings(settings.copy(splitHaptic = !settings.splitHaptic))
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (settings.splitHaptic) Color(0xFF182A3A) else Color(0xFF18181C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 2.dp),
                        label = {
                            Text(
                                text = "Mile / KM Split Haptic",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = if (settings.splitHaptic) "Enabled" else "Disabled",
                                fontSize = 9.sp,
                                color = if (settings.splitHaptic) VividGreen else MutedGray
                            )
                        }
                    )
                }
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(batterySaverEnabled = !settings.batterySaverEnabled))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.batterySaverEnabled) Color(0xFF382010) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Battery Saver Mode",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (settings.batterySaverEnabled) ElectricAmber else Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.batterySaverEnabled) "ON • Optimized GPS & ambient rate" else "OFF • High accuracy",
                            fontSize = 9.sp,
                            color = if (settings.batterySaverEnabled) ElectricAmber else MutedGray
                        )
                    }
                )
            }

            // Section: Move Reminders & Voice Navigation
            item {
                Text(
                    text = "REMINDERS & VOICE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricAmber,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(moveReminderEnabled = !settings.moveReminderEnabled))
                        com.example.miles.wear.engine.MoveReminderManager.applySetting(context)
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.moveReminderEnabled) Color(0xFF382010) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Move Reminders",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (settings.moveReminderEnabled) ElectricAmber else Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.moveReminderEnabled) "ON • Nags only when idle" else "OFF • No sitting-tracker",
                            fontSize = 9.sp,
                            color = if (settings.moveReminderEnabled) VividGreen else MutedGray
                        )
                    }
                )
            }

            if (settings.moveReminderEnabled) {
                item {
                    Chip(
                        onClick = {
                            val options = listOf(30, 45, 60, 90, 120)
                            val next = options[(options.indexOf(settings.moveReminderIntervalMin) + 1).coerceAtLeast(0) % options.size]
                            repository.updateSettings(settings.copy(moveReminderIntervalMin = next))
                            com.example.miles.wear.engine.MoveReminderManager.applySetting(context)
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF18181C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 2.dp),
                        label = {
                            Text(
                                text = "Interval: ${settings.moveReminderIntervalMin} min",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = "Tap to cycle 30–120 min",
                                fontSize = 9.sp,
                                color = MutedGray
                            )
                        }
                    )
                }
            }

            item {
                Chip(
                    onClick = {
                        repository.updateSettings(settings.copy(voiceNavEnabled = !settings.voiceNavEnabled))
                        if (settings.voiceNavEnabled) {
                            com.example.miles.wear.engine.NavigationVoice.stopSpeaking()
                        }
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (settings.voiceNavEnabled) Color(0xFF182A3A) else Color(0xFF18181C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Voice Navigation",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (settings.voiceNavEnabled) "ON • Spoken turn cues" else "OFF • Silent nav only",
                            fontSize = 9.sp,
                            color = if (settings.voiceNavEnabled) VividGreen else MutedGray
                        )
                    }
                )
            }

            // Section: Sync & Connection
            item {
                Text(
                    text = "PHONE SYNC",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141416))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (phoneStatus.isConnected) "● Phone Connected" else "○ Standalone (Offline)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (phoneStatus.isConnected) VividGreen else ElectricAmber
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (phoneStatus.isConnected) phoneStatus.phoneNodeName else "Data buffered safely in local queue",
                        fontSize = 9.sp,
                        color = MutedGray
                    )
                    Text(
                        text = "Pending queue: $unsyncedCount items",
                        fontSize = 9.sp,
                        color = ElectricAmber
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))
                Chip(
                    onClick = {
                        coroutineScope.launch {
                            phoneMessaging.refreshConnectedNodes()
                            phoneMessaging.flushOfflineQueue()
                        }
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = Color(0xFF1A2A38),
                        contentColor = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = "Sync Now",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                )
            }

            // Section: Privacy Guarantee
            item {
                Text(
                    text = "PRIVACY PROMISE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VividGreen,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 10.dp, bottom = 2.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141416))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "• 100% Local-first architecture\n• No MILES account required\n• No cloud telemetry or servers\n• Zero ads, analytics, or trackers\n• Sensor data stays on your wrist",
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        color = Color.LightGray
                    )
                }
            }

            // Section: About
            item {
                Text(
                    text = "ABOUT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedGray,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 10.dp, bottom = 2.dp)
                )
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "MILES Wear OS v1.0.0",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "GPLv3 Open Source License",
                        fontSize = 9.sp,
                        color = MutedGray
                    )
                    Text(
                        text = "github.com/anshlabs716/miles-wearos",
                        fontSize = 8.sp,
                        color = NeonCyan
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
