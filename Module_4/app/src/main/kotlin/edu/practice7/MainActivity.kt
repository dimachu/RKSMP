package edu.practice7

import edu.moduleui.PracticeShell
import edu.moduleui.usePracticeSystemBars
import edu.moduleui.PracticeHeading
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface as DisplaySurface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel

private val compassColors = darkColorScheme(
    primary = Color(0xFFACC7F6),
    onPrimary = Color(0xFF0D243D),
    primaryContainer = Color(0xFF243B70),
    onPrimaryContainer = Color(0xFFD9E9FD),
    background = Color(0xFF121E32),
    onBackground = Color(0xFFEAF0F9),
    surface = Color(0xFF121E32),
    onSurface = Color(0xFFEAF0F9),
    surfaceVariant = Color(0xFF24354F),
    onSurfaceVariant = Color(0xFFC6D1E1),
    secondary = Color(0xFF9BA5B8),
    outline = Color(0xFF8B9AB0),
    error = Color(0xFFFF7478),
    outlineVariant = Color(0xFF3B4B5E),
)

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private val model: CompassModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usePracticeSystemBars()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            PracticeShell("Модуль 4", onBack = ::finish) {
                MaterialTheme(colorScheme = compassColors) {
                    Surface(Modifier.fillMaxSize()) {
                        CompassScreen(model, hasRotationSensor = rotationSensor != null)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        val sensorMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(sensorMatrix, event.values)

        @Suppress("DEPRECATION")
        val displayRotation = windowManager.defaultDisplay.rotation
        val (horizontalAxis, verticalAxis) = when (displayRotation) {
            DisplaySurface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            DisplaySurface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            DisplaySurface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }

        val displayMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(sensorMatrix, horizontalAxis, verticalAxis, displayMatrix)
        val orientation = SensorManager.getOrientation(displayMatrix, FloatArray(3))
        model.update(Math.toDegrees(orientation[0].toDouble()).toFloat())
    }
}

class CompassModel : ViewModel() {
    var azimuth by mutableFloatStateOf(0f)
        private set

    var rotation by mutableFloatStateOf(0f)
        private set

    fun update(degrees: Float) {
        azimuth = normalizedAzimuth(degrees)
        rotation = nearestNorthRotation(azimuth, rotation)
    }
}

@Composable
private fun CompassScreen(model: CompassModel, hasRotationSensor: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PracticeHeading("Компас")

        if (hasRotationSensor) {
            val angle by animateFloatAsState(model.rotation, label = "north")
            CompassReadout(angle, model.azimuth.toInt(), Modifier.weight(1f).fillMaxSize())
        } else {
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Устройство не поддерживает датчик ориентации",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CompassReadout(angle: Float, azimuth: Int, modifier: Modifier = Modifier) {
    Layout(
        modifier = modifier,
        content = {
            Text("N", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
            CompassDial(angle)
            Text(
                "Азимут: $azimuth°",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val labelConstraints = Constraints(maxWidth = width, maxHeight = height)
        val north = measurables[0].measure(labelConstraints)
        val readout = measurables[2].measure(labelConstraints)
        val gap = 8.dp.roundToPx()
        val available = (height - north.height - readout.height - 2 * gap).coerceAtLeast(0)
        val diameter = minOf((width * 0.78f).toInt(), available, 400.dp.roundToPx())
        val dial = measurables[1].measure(Constraints.fixed(diameter, diameter))
        val top = ((height - north.height - dial.height - readout.height - 2 * gap) / 2)
            .coerceAtLeast(0)

        layout(width, height) {
            north.placeRelative((width - north.width) / 2, top)
            dial.placeRelative((width - dial.width) / 2, top + north.height + gap)
            readout.placeRelative((width - readout.width) / 2, top + north.height + dial.height + 2 * gap)
        }
    }
}

@Composable
private fun CompassDial(angle: Float) {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxSize()) {
        val rim = 2.dp.toPx()
        val radius = (size.minDimension / 2 - rim).coerceAtLeast(0f)
        drawCircle(colors.outline, radius = radius, style = Stroke(rim))
        repeat(12) { hour ->
            rotate(hour * 30f) {
                val tickLength = radius * if (hour % 3 == 0) 0.09f else 0.04f
                drawLine(
                    colors.outline,
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y - radius + tickLength),
                    strokeWidth = rim,
                )
            }
        }
        val halfNeedleWidth = size.minDimension * 0.045f
        val needleLength = size.minDimension * 0.4f
        val north = Path().apply {
            moveTo(center.x, center.y - needleLength)
            lineTo(center.x - halfNeedleWidth, center.y)
            lineTo(center.x + halfNeedleWidth, center.y)
            close()
        }
        val south = Path().apply {
            moveTo(center.x, center.y + needleLength)
            lineTo(center.x - halfNeedleWidth, center.y)
            lineTo(center.x + halfNeedleWidth, center.y)
            close()
        }
        rotate(angle) {
            drawPath(north, colors.error)
            drawPath(south, colors.secondary)
        }
    }
}
