package com.oleh11.vatrasmoker.ui.components

import com.oleh11.vatrasmoker.ui.theme.OnAccent
import com.oleh11.vatrasmoker.ui.theme.Accent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oleh11.vatrasmoker.data.model.CookDataPoint
import com.oleh11.vatrasmoker.data.model.TemperatureUnit
import com.oleh11.vatrasmoker.ui.theme.Charcoal500
import com.oleh11.vatrasmoker.ui.theme.Charcoal700
import com.oleh11.vatrasmoker.ui.theme.Charcoal800
import com.oleh11.vatrasmoker.ui.theme.FlameOrange
import com.oleh11.vatrasmoker.ui.theme.Probe1Color
import com.oleh11.vatrasmoker.ui.theme.Probe2Color
import com.oleh11.vatrasmoker.ui.theme.TextGray
import com.oleh11.vatrasmoker.ui.theme.TextWhite

@Composable
fun LiveCookChart(
    history: List<CookDataPoint>,
    unit: TemperatureUnit,
    showProbes: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Charcoal800),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.oleh11.vatrasmoker.ui.theme.Charcoal600),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = FlameOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE COOK TEMPERATURE CURVE",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val gridColor = Charcoal500
            val emberColor = FlameOrange

            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (history.size < 2) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Gathering cook data...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val minTemp = 50f
                        val maxTemp = 350f

                        // Draw Grid Lines
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val y = height * (i.toFloat() / gridCount)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        fun drawTempPath(color: Color, selector: (CookDataPoint) -> Int) {
                            val path = Path()
                            val stepX = width / (history.size - 1).coerceAtLeast(1)

                            history.forEachIndexed { index, point ->
                                val temp = selector(point).toFloat()
                                if (temp > 0) {
                                    val x = index * stepX
                                    val normalizedY = ((temp - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
                                    val y = height - (normalizedY * height)

                                    if (path.isEmpty) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                            }

                            if (!path.isEmpty) {
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        // 1. Grill Chamber Temp
                        drawTempPath(emberColor) { it.grillTempF }
                        if (showProbes) {
                            drawTempPath(Probe1Color) { it.probe1TempF }
                            drawTempPath(Probe2Color) { it.probe2TempF }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legends
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                ChartLegend(color = FlameOrange, label = "Grill Chamber")
                if (showProbes) {
                    ChartLegend(color = Probe1Color, label = "Probe 1")
                    ChartLegend(color = Probe2Color, label = "Probe 2")
                }
            }
        }
    }
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(10.dp)
        ) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )
    }
}
