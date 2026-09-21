package com.example.miles.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.miles.wear.data.model.WorkoutType
import com.example.miles.wear.ui.screens.ActiveWorkoutScreen
import com.example.miles.wear.ui.screens.DashboardScreen
import com.example.miles.wear.ui.screens.HistoryScreen
import com.example.miles.wear.ui.screens.MirroredWorkoutScreen
import com.example.miles.wear.ui.screens.MapsScreen
import com.example.miles.wear.ui.screens.SensorsDiagnosticScreen
import com.example.miles.wear.ui.screens.SettingsScreen
import com.example.miles.wear.ui.screens.WaterLockScreen
import com.example.miles.wear.ui.screens.WorkoutDetailScreen
import com.example.miles.wear.ui.screens.WorkoutSelectionScreen
import com.example.miles.wear.ui.screens.WorkoutSummaryScreen
import com.example.miles.wear.ui.theme.MilesWearTheme
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import com.example.miles.wear.ui.components.WearNavigationControls

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navToMirrored = intent.getBooleanExtra("NAV_TO_MIRRORED", false)

        setContent {
            MilesWearTheme {
                MilesAppContent(initialNavToMirrored = navToMirrored)
            }
        }
    }
}

@Composable
fun MilesAppContent(initialNavToMirrored: Boolean = false) {
    val navController = rememberSwipeDismissableNavController()
    var permissionsGranted by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val baseDensity = LocalDensity.current
    val shortestSideDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val adaptiveScale = when {
        shortestSideDp <= 170 -> 0.82f
        shortestSideDp <= 184 -> 0.88f
        shortestSideDp <= 192 -> 0.92f
        shortestSideDp <= 210 -> 0.96f
        shortestSideDp >= 240 -> 1.06f
        else -> 1.00f
    }
    val adaptiveFontScale = (baseDensity.fontScale * adaptiveScale).coerceIn(0.85f, 1.15f)

    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        permissionsGranted = allGranted
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        val allHave = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        permissionsGranted = allHave
        if (!allHave) {
            permissionLauncher.launch(requiredPermissions)
        }
        if (initialNavToMirrored) {
            navController.navigate("mirrored_workout")
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density * adaptiveScale,
            fontScale = adaptiveFontScale
        )
    ) {
        if (!permissionsGranted) {
            PermissionRequestScreen(
                onRequest = { permissionLauncher.launch(requiredPermissions) }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = "dashboard"
                ) {
            composable("dashboard") {
                DashboardScreen(
                    onStartWorkout = {
                        navController.navigate("select_workout")
                    },
                    onOpenHistory = {
                        navController.navigate("history")
                    },
                    onOpenSettings = {
                        navController.navigate("settings")
                    },
                    onOpenDiagnostics = {
                        navController.navigate("diagnostics")
                    },
                    onOpenMap = {
                        navController.navigate("map")
                    },
                    onOpenMirrored = {
                        navController.navigate("mirrored_workout")
                    },
                    onSelectSession = { id ->
                        navController.navigate("workout_detail/$id")
                    }
                )
            }

            composable("select_workout") {
                WorkoutSelectionScreen(
                    onSelectWorkout = { workoutType ->
                        navController.navigate("active_workout/${workoutType.name}")
                    }
                )
            }

            composable("active_workout/{type}") { backStackEntry ->
                val typeName = backStackEntry.arguments?.getString("type") ?: WorkoutType.RUN.name
                val type = WorkoutType.fromString(typeName)
                ActiveWorkoutScreen(
                    workoutType = type,
                    onFinishWorkout = {
                        navController.navigate("workout_summary") {
                            popUpTo("dashboard")
                        }
                    },
                    onDiscardWorkout = {
                        navController.popBackStack("dashboard", inclusive = false)
                    },
                    onEnableWaterLock = {
                        navController.navigate("water_lock")
                    }
                )
            }

            composable("map") {
                MapsScreen()
            }

            composable("mirrored_workout") {
                MirroredWorkoutScreen(
                    onExitMirrored = {
                        navController.popBackStack()
                    }
                )
            }

            composable("workout_summary") {
                WorkoutSummaryScreen(
                    onDone = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            }

            composable("history") {
                HistoryScreen(
                    onSelectSession = { id ->
                        navController.navigate("workout_detail/$id")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("workout_detail/{id}") { backStackEntry ->
                val idStr = backStackEntry.arguments?.getString("id") ?: "0"
                val id = idStr.toLongOrNull() ?: 0L
                WorkoutDetailScreen(
                    sessionId = id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("diagnostics") {
                SensorsDiagnosticScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("water_lock") {
                WaterLockScreen(
                    onUnlock = { navController.popBackStack() }
                )
                }

                WearNavigationControls(navController = navController)
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚡ SENSORS NEEDED",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "MILES requires Body Sensors, GPS, and Activity recognition for real wrist telemetry.",
            fontSize = 11.sp,
            color = MutedGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Chip(
            onClick = onRequest,
            colors = ChipDefaults.chipColors(
                backgroundColor = VividGreen,
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth(0.9f),
            label = {
                Text(
                    text = "Grant Permissions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        )
    }
}
