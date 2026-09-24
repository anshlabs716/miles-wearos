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
import com.example.miles.wear.data.local.entity.SavedRouteEntity
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Saved routes — real GPS routes the user kept from navigation or pins. */
@Composable
fun SavedRoutesScreen(
    onFollow: (Long) -> Unit,
    onNewRoute: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val repository = MilesWearApplication.instance.repository

    val routes by repository.allSavedRoutes.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by repository.settings.collectAsStateWithLifecycle()

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
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = "📍 SAVED ROUTES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${routes.size} route${if (routes.size == 1) "" else "s"}",
                        fontSize = 9.sp,
                        color = MutedGray
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Chip(
                        onClick = onBack,
                        colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF18181C), contentColor = Color.White),
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                        label = {
                            Text("⬅ Back", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedGray)
                        }
                    )
                    Chip(
                        onClick = onNewRoute,
                        colors = ChipDefaults.chipColors(backgroundColor = VividGreen, contentColor = Color.Black),
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                        label = {
                            Text("🗺️ New", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    )
                }
            }

            if (routes.isEmpty()) {
                item {
                    Text(
                        text = "No saved routes yet.\nNavigate somewhere in the map and tap Save, or drop pins and tap Route.",
                        fontSize = 10.sp,
                        color = MutedGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            } else {
                routes.forEach { route ->
                    item {
                        RouteCard(
                            route = route,
                            unitLabel = if (settings.unit.title == "Metric") "km" else "mi",
                            onFollow = { onFollow(route.id) },
                            onDelete = {
                                coroutineScope.launch { repository.deleteRoute(route.id) }
                            }
                        )
                    }
                }
            }
            item { Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RouteCard(
    route: SavedRouteEntity,
    unitLabel: String,
    onFollow: () -> Unit,
    onDelete: () -> Unit
) {
    val distance = if (unitLabel == "km") route.distanceMeters / 1000.0 else route.distanceMeters * 0.000621371
    val date = remember(route.createdAt) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(route.createdAt))
    }
    Column(
        Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 3.dp)
            .background(androidx.compose.ui.graphics.Color(0xFF18181C), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = route.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = "${String.format(Locale.US, "%.2f", distance)} $unitLabel • $date",
            fontSize = 9.sp,
            color = MutedGray
        )
        Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Chip(
                onClick = onFollow,
                colors = ChipDefaults.chipColors(backgroundColor = VividGreen, contentColor = Color.Black),
                modifier = androidx.compose.ui.Modifier.weight(1f),
                label = {
                    Text("Follow ➜", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
                }
            )
            Chip(
                onClick = onDelete,
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF2A1414), contentColor = Color.White),
                modifier = androidx.compose.ui.Modifier.weight(1f),
                label = {
                    Text("🗑 Delete", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoralFlame, textAlign = TextAlign.Center)
                }
            )
        }
    }
}