package com.example.miles.wear.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.Text
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MyLocationNewOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.gpsprovider.GpsMyLocationProvider

@Composable
fun MapsScreen() {
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(0.0, 0.0))
        }
    }

    DisposableEffect(mapView) {
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            try {
                val locationOverlay = MyLocationNewOverlay(
                    GpsMyLocationProvider(context),
                    mapView
                )
                locationOverlay.enableMyLocation()
                mapView.overlays.add(locationOverlay)

                val scaleBar = ScaleBarOverlay(mapView)
                scaleBar.setCentred(true)
                scaleBar.setScaleBarOffset(8, 8)
                mapView.overlays.add(scaleBar)

                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val lastLocation = sequenceOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER
                ).mapNotNull { provider ->
                    try {
                        locationManager.getLastKnownLocation(provider)
                    } catch (_: SecurityException) {
                        null
                    }
                }.maxByOrNull { it.time }

                if (lastLocation != null) {
                    mapView.controller.setCenter(
                        GeoPoint(lastLocation.latitude, lastLocation.longitude)
                    )
                }
            } catch (_: SecurityException) {
                // Permission may have changed while the map was being created.
            }
        }

        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.overlays.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        Chip(
            onClick = {
                mapView.controller.setZoom(15.0)
            },
            colors = ChipDefaults.chipColors(
                backgroundColor = Color(0xDD000000),
                contentColor = Color.White
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .fillMaxWidth(0.72f),
            label = {
                Text(
                    text = "MILES MAP",
                    color = NeonCyan
                )
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "© OpenStreetMap contributors",
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .background(Color(0x99000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
