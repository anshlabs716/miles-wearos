package com.example.miles.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.NavHostController
import androidx.wear.compose.navigation.currentBackStackEntryAsState
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack

@Composable
fun WearNavigationControls(
    navController: NavHostController
) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "dashboard"
    if (route == "dashboard") return

    val width = LocalConfiguration.current.screenWidthDp
    val compact = width <= 192

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xEE000000))
            .navigationBarsPadding()
            .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = 4.dp)
            .zIndex(20f),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Chip(
            onClick = { navController.popBackStack() },
            colors = ChipDefaults.chipColors(
                backgroundColor = Color(0xFF171A20),
                contentColor = Color.White
            ),
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan
                )
            },
            label = {
                Text(text = "Back")
            }
        )

        Chip(
            onClick = {
                navController.navigate("dashboard") {
                    popUpTo("dashboard") { inclusive = false }
                    launchSingleTop = true
                }
            },
            colors = ChipDefaults.chipColors(
                backgroundColor = NeonCyan,
                contentColor = Color.Black
            ),
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color.Black
                )
            },
            label = {
                Text(text = "Home", color = Color.Black)
            }
        )
    }
}
