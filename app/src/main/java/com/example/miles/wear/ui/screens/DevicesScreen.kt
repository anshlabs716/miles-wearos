package com.example.miles.wear.ui.screens

import androidx.compose.foundation.background
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
import com.example.miles.wear.sensor.BleSensorManager
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch

/**
 * External BLE sensor connections: heart-rate straps + cadence sensors.
 * Real GATT data feeds the live workout signals — nothing simulated.
 */
@Composable
fun DevicesScreen(onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val ble = MilesWearApplication.instance.bleSensorManager

    val state by ble.state.collectAsStateWithLifecycle()
    val discovered by ble.discovered.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
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
                        text = "📡 BLE SENSORS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Heart rate straps + cadence",
                        fontSize = 9.sp,
                        color = MutedGray
                    )
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

            if (!ble.isSupported) {
                item {
                    Text(
                        "Bluetooth LE unavailable on this device.",
                        fontSize = 10.sp,
                        color = CoralFlame,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                return@ScalingLazyColumn
            }

            if (!ble.hasPermissions()) {
                item {
                    Text(
                        "BLE permissions not granted — restart MILES.",
                        fontSize = 10.sp,
                        color = ElectricAmber,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                return@ScalingLazyColumn
            }

            // Current connection status
            item {
                val (statusText, statusColor) = when (val s = state) {
                    is BleSensorManager.ConnectionState.Connected ->
                        "● Connected: ${s.name}" to VividGreen
                    is BleSensorManager.ConnectionState.Connecting ->
                        "◌ Connecting ${s.name ?: ""}…" to ElectricAmber
                    is BleSensorManager.ConnectionState.Scanning ->
                        "◉ Scanning…" to NeonCyan
                    else -> "○ Disconnected" to MutedGray
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 4.dp)
                )
            }

            // Scan / stop chip
            item {
                Chip(
                    onClick = { ble.toggleScan() },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (state is BleSensorManager.ConnectionState.Scanning) Color(0xFF1A2A38) else Color(0xFF182A3A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 2.dp),
                    label = {
                        Text(
                            text = if (state is BleSensorManager.ConnectionState.Scanning) "⏹ Stop scan" else "🔍 Scan for sensors",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }

            // Live connected metrics (real GATT data)
            val connected = state as? BleSensorManager.ConnectionState.Connected
            if (connected != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .padding(vertical = 4.dp)
                            .background(Color(0xFF141416), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "HEART RATE",
                            fontSize = 9.sp,
                            color = MutedGray
                        )
                        Text(
                            text = connected.hrBpm?.let { "$it BPM" } ?: "waiting…",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = CoralFlame
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "CADENCE", fontSize = 9.sp, color = MutedGray)
                        Text(
                            text = connected.cadenceRpm?.let { "$it rpm" } ?: "waiting…",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricAmber
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "BATTERY", fontSize = 9.sp, color = MutedGray)
                        Text(
                            text = connected.batteryPct?.let { "$it%" } ?: "—",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VividGreen
                        )
                    }
                }

                item {
                    Chip(
                        onClick = { ble.disconnect() },
                        colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF33141E), contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 2.dp),
                        label = {
                            Text("✕ Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoralFlame, textAlign = TextAlign.Center)
                        }
                    )
                }
            } else {
                // Discovered device list
                item {
                    Text(
                        text = "FOUND DEVICES (${discovered.size})",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricAmber,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.fillMaxWidth(0.92f).padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                if (discovered.isEmpty()) {
                    item {
                        Text(
                            "No sensors nearby yet — tap Scan. (HR straps + cadence sensors)",
                            fontSize = 10.sp,
                            color = MutedGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    discovered.forEach { device ->
                        item {
                            Chip(
                                onClick = { ble.connect(device) },
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = if (state is BleSensorManager.ConnectionState.Connecting) Color(0xFF18181C) else Color(0xFF1A2A38),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 2.dp),
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🎯", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = device.name ?: device.address,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                },
                                secondaryLabel = {
                                    Text(text = device.address, fontSize = 8.sp, color = MutedGray)
                                }
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}