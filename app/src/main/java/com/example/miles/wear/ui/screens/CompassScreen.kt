package com.example.miles.wear.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.miles.wear.ui.theme.CoralFlame
import com.example.miles.wear.ui.theme.ElectricAmber
import com.example.miles.wear.ui.theme.MutedGray
import com.example.miles.wear.ui.theme.NeonCyan
import com.example.miles.wear.ui.theme.OLEDBlack
import com.example.miles.wear.ui.theme.VividGreen
import com.example.miles.wear.util.Cardinal
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real magnetic compass: magnetometer + accelerometer, low-pass filtered,
 * tilt-compensated via rotation matrix. Shows live magnetic heading in
 * degrees with a rotating ring matched to true north.
 */
@Composable
fun CompassScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var azimuth by remember { mutableFloatStateOf(-1f) }
    var hasSensor by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        hasSensor = magnetometer != null && accelerometer != null

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> lowPass(event.values, gravity, 0.20f)
                    Sensor.TYPE_MAGNETIC_FIELD -> lowPass(event.values, geomagnetic, 0.20f)
                }
                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val degrees = (Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0
                    azimuth = degrees.toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Ignore accuracy changes; the live heading is enough.
            }
        }

        if (hasSensor) {
            magnetometer?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
            accelerometer?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🧭 COMPASS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = NeonCyan,
            modifier = Modifier.padding(top = 10.dp)
        )

        if (!hasSensor) {
            Text(
                text = "No magnetic sensor on this device",
                fontSize = 11.sp,
                color = MutedGray,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "Compass requires a magnetometer (TYPE_MAGNETIC_FIELD).",
                fontSize = 9.sp,
                color = MutedGray,
                modifier = Modifier.padding(top = 8.dp)
            )
            return@Column
        }

        val heading = if (azimuth < 0f) 0f else azimuth

        Box(
            modifier = Modifier
                .padding(top = 18.dp)
                .size(208.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rotating ring: N label points to magnetic north
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .rotate(-heading),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = size.minDimension / 2f - 6.dp.toPx()

                    // Outer ring
                    drawCircle(
                        color = Color(0xFF2A3040),
                        radius = radius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                    // Cardinal + minor ticks
                    for (i in 0 until 12) {
                        val angle = Math.toRadians(i * 30.0)
                        val isCardinal = i % 3 == 0
                        val inner = if (isCardinal) radius - 7.dp.toPx() else radius - 4.dp.toPx()
                        val x1 = centerX + (inner * sin(angle)).toFloat()
                        val y1 = centerY - (inner * cos(angle)).toFloat()
                        val x2 = centerX + ((radius - 1.dp.toPx()) * sin(angle)).toFloat()
                        val y2 = centerY - ((radius - 1.dp.toPx()) * cos(angle)).toFloat()
                        drawLine(
                            color = if (isCardinal) ElectricAmber else Color(0xFF3A4254),
                            start = androidx.compose.ui.geometry.Offset(x1, y1),
                            end = androidx.compose.ui.geometry.Offset(x2, y2),
                            strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx()
                        )
                    }
                }
                Text(
                    text = "N",
                    color = VividGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
                )
                Text(
                    text = "E",
                    color = MutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
                )
                Text(
                    text = "S",
                    color = MutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                )
                Text(
                    text = "W",
                    color = MutedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp)
                )
            }

            // Fixed heading marker (device direction) at top
            Canvas(modifier = Modifier.size(208.dp)) {
                val centerX = size.width / 2f
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(centerX, 0f)
                        lineTo(centerX - 7.dp.toPx(), 10.dp.toPx())
                        lineTo(centerX + 7.dp.toPx(), 10.dp.toPx())
                        close()
                    },
                    color = CoralFlame
                )
            }

            // Center readout: degrees + cardinal direction
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (azimuth < 0f) "--°" else "${heading.toInt()}°",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = if (azimuth < 0f) "---" else Cardinal.fromDegrees(heading).title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricAmber
                )
            }
        }

        Text(
            text = "Magnetic heading • hold watch flat",
            fontSize = 9.sp,
            color = MutedGray,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

private fun lowPass(input: FloatArray, output: FloatArray, alpha: Float) {
    for (i in input.indices) {
        output[i] = output[i] + alpha * (input[i] - output[i])
    }
}