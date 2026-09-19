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
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
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

            // Section: Display & Screen
            item {
                Text(
                    text = "DISPLAY",
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
                            text = if (settings.keepScreenOn) "ON • During active workout" else "OFF",
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
