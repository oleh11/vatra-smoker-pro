package com.oleh11.vatrasmoker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oleh11.vatrasmoker.data.model.TemperatureUnit
import com.oleh11.vatrasmoker.ui.theme.Accent
import com.oleh11.vatrasmoker.ui.theme.Charcoal500
import com.oleh11.vatrasmoker.ui.theme.Charcoal700
import com.oleh11.vatrasmoker.ui.theme.FlameAmber
import com.oleh11.vatrasmoker.ui.theme.FlameOrange
import com.oleh11.vatrasmoker.ui.theme.OnAccent
import com.oleh11.vatrasmoker.ui.theme.SmokerDark
import com.oleh11.vatrasmoker.ui.theme.TextGray
import com.oleh11.vatrasmoker.ui.theme.TextWhite
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadialTempGauge(
    currentTempF: Int,
    targetTempF: Int,
    unit: TemperatureUnit,
    onTargetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minTempF: Int = 160,
    maxTempF: Int = 500
) {
    val animatedCurrentTemp by animateFloatAsState(
        targetValue = currentTempF.toFloat(),
        animationSpec = tween(durationMillis = 250),
        label = "currentTempAnim"
    )

    val currentFraction = ((animatedCurrentTemp - minTempF) / (maxTempF - minTempF)).coerceIn(0f, 1f)
    val targetFraction = ((targetTempF - minTempF).toFloat() / (maxTempF - minTempF)).coerceIn(0f, 1f)

    // 270 degree arc, opening at the bottom
    val startAngle = 135f
    val sweepTotal = 270f
    val currentSweep = currentFraction * sweepTotal

    // Theme colors must be read here, not inside the draw lambda
    val trackColor = Charcoal500
    val markerColor = Accent
    val markerCore = OnAccent
    val ember = FlameOrange
    val amber = FlameAmber

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(260.dp)
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                val arcSize = Size(radius * 2, radius * 2)
                val topLeft = Offset(center.x - radius, center.y - radius)

                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                if (currentSweep > 0) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(amber, ember, SmokerDark, ember),
                            center = center
                        ),
                        startAngle = startAngle,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Target marker
                val targetAngleRad = Math.toRadians((startAngle + (targetFraction * sweepTotal)).toDouble())
                val targetX = (center.x + radius * cos(targetAngleRad)).toFloat()
                val targetY = (center.y + radius * sin(targetAngleRad)).toFloat()
                drawCircle(color = markerColor, radius = 11.dp.toPx(), center = Offset(targetX, targetY))
                drawCircle(color = markerCore, radius = 4.dp.toPx(), center = Offset(targetX, targetY))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = FlameOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GRILL TEMP",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = unit.format(currentTempF),
                    style = MaterialTheme.typography.displayLarge,
                    fontFamily = FontFamily.Monospace,
                    color = TextWhite,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = CircleShape,
                    color = Charcoal700
                ) {
                    Text(
                        text = "TARGET  ${unit.format(targetTempF)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SETPOINT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = unit.format(targetTempF),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    color = TextWhite,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Slider(
                value = targetTempF.toFloat(),
                onValueChange = { newVal ->
                    val stepped = ((newVal / 5f).toInt() * 5).coerceIn(minTempF, maxTempF)
                    onTargetChange(stepped)
                },
                valueRange = minTempF.toFloat()..maxTempF.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = Accent,
                    inactiveTrackColor = Charcoal500
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Stepper: -10  -5  value  +5  +10
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            StepButton("-10", filled = false) { onTargetChange((targetTempF - 10).coerceAtLeast(minTempF)) }
            Spacer(modifier = Modifier.width(8.dp))
            StepButton(icon = Icons.Default.Remove, filled = false, description = "Minus 5") {
                onTargetChange((targetTempF - 5).coerceAtLeast(minTempF))
            }
            Text(
                text = unit.format(targetTempF),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            StepButton(icon = Icons.Default.Add, filled = true, description = "Plus 5") {
                onTargetChange((targetTempF + 5).coerceAtMost(maxTempF))
            }
            Spacer(modifier = Modifier.width(8.dp))
            StepButton("+10", filled = true) { onTargetChange((targetTempF + 10).coerceAtMost(maxTempF)) }
        }
    }
}

@Composable
private fun StepButton(
    label: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    filled: Boolean,
    description: String? = null,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (filled) Accent else Charcoal700,
            contentColor = if (filled) OnAccent else TextWhite
        ),
        modifier = Modifier.size(44.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = description)
        } else {
            Text(text = label.orEmpty(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}
