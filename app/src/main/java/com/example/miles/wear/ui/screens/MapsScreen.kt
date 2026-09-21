package com.example.miles.wear.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.wear.compose.material3.Button
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MyLocationNewOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.gpsprovider.GpsMyLocationProvider

private enum class WearMapLayer(val label: String) {
    OSM("OSM"),
    SATELLITE("SAT"),
    TOPO("TOPO")
}

@Composable
fun MapsScreen() {
    val context = LocalContext.current
    var layer by remember { mutableStateOf(WearMapLayer.OSM) }
    var isFollowing by remember { mutableStateOf(true) }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(0.0, 0.0))
        }
    }

    fun recenter() {
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

        lastLocation?.let {
            mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
            mapView.controller.setZoom(15.0)
            isFollowing = true
        }
    }

    LaunchedEffect(layer) {
        mapView.setTileSource(
            when (layer) {
                WearMapLayer.OSM -> TileSourceFactory.MAPNIK
                WearMapLayer.SATELLITE -> TileSourceFactory.USGS_SAT
                WearMapLayer.TOPO -> TileSourceFactory.USGS_TOPO
            }
        )
        mapView.invalidate()
    }

    DisposableEffect(mapView) {
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        var locationOverlay: MyLocationNewOverlay? = null

        if (hasLocationPermission) {
            try {
                locationOverlay = MyLocationNewOverlay(
                    GpsMyLocationProvider(context),
                    mapView
                ).also {
                    it.enableMyLocation()
                    mapView.overlays.add(it)
                }

                val scaleBar = ScaleBarOverlay(mapView).apply {
                    setCentred(true)
                    setScaleBarOffset(8, 8)
                }
                mapView.overlays.add(scaleBar)

                recenter()
            } catch (_: SecurityException) {
                // Permission may have changed while the map was being created.
            }
        }

        mapView.onResume()

        onDispose {
            locationOverlay?.disableMyLocation()
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

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "MILES MAP • ${layer.label}",
                color = NeonCyan
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WearMapLayer.entries.forEach { candidate ->
                    Chip(
                        onClick = {
                            layer = candidate
                            isFollowing = false
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (layer == candidate) NeonCyan else Color(0xDD000000),
                            contentColor = if (layer == candidate) Color.Black else Color.White
                        ),
                        modifier = Modifier.size(width = 62.dp, height = 34.dp),
                        label = {
                            Text(
                                text = candidate.label,
                                color = if (layer == candidate) Color.Black else Color.White
                            )
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    mapView.controller.setZoom(
                        (mapView.zoomLevelDouble - 1.0).coerceAtLeast(3.0)
                    )
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
            }

            Button(
                onClick = {
                    recenter()
                    isFollowing = true
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = if (isFollowing) "Following location" else "Center on location"
                )
            }

            Button(
                onClick = {
                    mapView.controller.setZoom(
                        (mapView.zoomLevelDouble + 1.0).coerceAtMost(19.0)
                    )
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }
        }

        Text(
            text = when (layer) {
                WearMapLayer.OSM -> "© OpenStreetMap contributors"
                WearMapLayer.SATELLITE -> "USGS imagery"
                WearMapLayer.TOPO -> "USGS topographic maps"
            },
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp)
                .background(Color(0x99000000))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
