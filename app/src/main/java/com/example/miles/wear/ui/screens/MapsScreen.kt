package com.example.miles.wear.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.wear.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.max
import kotlin.math.min

private data class WearMapLayer(
    val label: String,
    val attribution: String,
    val source: XYTileSource
)

private val mapLayers = listOf(
    WearMapLayer(
        "OSM",
        "© OpenStreetMap contributors",
        XYTileSource("OpenStreetMap", 0, 19, 256, ".png", arrayOf("https://tile.openstreetmap.org/"))
    ),
    WearMapLayer(
        "CYCLO",
        "© CyclOSM / OpenStreetMap",
        XYTileSource("CyclOSM", 0, 20, 256, ".png", arrayOf(
            "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
        ))
    ),
    WearMapLayer(
        "HOT",
        "© OpenStreetMap contributors / HOT",
        XYTileSource("HOT", 0, 19, 256, ".png", arrayOf(
            "https://a.tile.openstreetmap.fr/hot/",
            "https://b.tile.openstreetmap.fr/hot/",
            "https://c.tile.openstreetmap.fr/hot/"
        ))
    ),
    WearMapLayer(
        "DARK",
        "© CARTO / OpenStreetMap",
        XYTileSource("CartoDark", 0, 20, 256, ".png", arrayOf(
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/"
        ))
    ),
    WearMapLayer(
        "LIGHT",
        "© CARTO / OpenStreetMap",
        XYTileSource("CartoVoyager", 0, 20, 256, ".png", arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
        ))
    ),
    WearMapLayer(
        "TOPO",
        "© OpenTopoMap contributors",
        XYTileSource("OpenTopoMap", 0, 17, 256, ".png", arrayOf(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        ))
    ),
    WearMapLayer(
        "SAT",
        "USGS satellite imagery",
        XYTileSource("USGS Satellite", 0, 20, 256, ".jpg", arrayOf(
            "https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryOnly/MapServer/tile/"
        ))
    )
)

@Composable
fun MapsScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val compact = min(configuration.screenWidthDp, configuration.screenHeightDp) <= 192

    var layerIndex by remember { mutableStateOf(0) }
    var following by remember { mutableStateOf(true) }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }

    val layer = mapLayers[layerIndex]
    val buttonSize = if (compact) 38.dp else 44.dp

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = "MILES-WearOS/\${context.packageName}"
        }
        MapView(context).apply {
            setTileSource(mapLayers[0].source)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            setUseDataConnection(true)
            controller.setZoom(15.0)
        }
    }

    fun centerOn(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        if (following) {
            mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
        }
    }

    fun locate() {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

        try {
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }.maxByOrNull { it.time }?.let { centerOn(it) }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    centerOn(location)
                    manager.removeUpdates(this)
                }
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }

            val provider = when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            provider?.let { manager.requestSingleUpdate(it, listener, Looper.getMainLooper()) }
        } catch (_: SecurityException) {
        }
    }

    LaunchedEffect(layerIndex) {
        mapView.setTileSource(layer.source)
        mapView.invalidate()
    }

    DisposableEffect(mapView) {
        var locationOverlay: MyLocationNewOverlay? = null

        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).also {
                    it.enableMyLocation()
                    mapView.overlays.add(it)
                }
                locate()
            } catch (_: SecurityException) {
            }
        }

        mapView.overlays.add(ScaleBarOverlay(mapView).apply {
            setCentred(true)
            setScaleBarOffset(8, 8)
        })
        mapView.onResume()

        onDispose {
            locationOverlay?.disableMyLocation()
            mapView.overlays.clear()
            mapView.onPause()
        }
    }

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        AndroidView({ mapView }, Modifier.fillMaxSize())

        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = if (compact) 4.dp else 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("MILES MAP • \${layer.label}")

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        layerIndex = (layerIndex + 1) % mapLayers.size
                        following = false
                    },
                    modifier = Modifier.size(width = if (compact) 76.dp else 88.dp, height = 34.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Change map layer")
                    Text(layer.label)
                }

                Button(
                    onClick = {
                        following = !following
                        if (following) locate()
                    },
                    modifier = Modifier.size(width = if (compact) 76.dp else 88.dp, height = 34.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "Toggle follow mode")
                    Text(if (following) "Follow" else "Free")
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = if (compact) 6.dp else 10.dp, bottom = 54.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { mapView.controller.setZoom(min(19.0, mapView.zoomLevelDouble + 1.0)) },
                modifier = Modifier.size(buttonSize),
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "Zoom in") }

            Button(
                onClick = { mapView.controller.setZoom(max(3.0, mapView.zoomLevelDouble - 1.0)) },
                modifier = Modifier.size(buttonSize),
                shape = CircleShape
            ) { Icon(Icons.Default.Remove, contentDescription = "Zoom out") }

            Button(
                onClick = {
                    following = true
                    locate()
                },
                modifier = Modifier.size(buttonSize),
                shape = CircleShape
            ) { Icon(Icons.Default.MyLocation, contentDescription = "My location") }
        }

        Text(
            text = if (latitude != 0.0 || longitude != 0.0)
                "\${layer.attribution} • %.4f, %.4f".format(latitude, longitude)
            else layer.attribution,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 5.dp)
        )
    }
}
