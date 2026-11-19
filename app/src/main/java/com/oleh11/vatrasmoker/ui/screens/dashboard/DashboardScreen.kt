package com.oleh11.vatrasmoker.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oleh11.vatrasmoker.R
import com.oleh11.vatrasmoker.data.model.SmokerPowerMode
import com.oleh11.vatrasmoker.ui.components.ConnectionStatusBadge
import com.oleh11.vatrasmoker.ui.components.LiveCookChart
import com.oleh11.vatrasmoker.ui.components.ProbeCard
import com.oleh11.vatrasmoker.ui.components.RadialTempGauge
import com.oleh11.vatrasmoker.ui.components.SmokeDial
import com.oleh11.vatrasmoker.ui.theme.AlertRed
import com.oleh11.vatrasmoker.ui.theme.Charcoal600
import com.oleh11.vatrasmoker.ui.theme.Charcoal700
import com.oleh11.vatrasmoker.ui.theme.Charcoal800
import com.oleh11.vatrasmoker.ui.theme.Charcoal900
import com.oleh11.vatrasmoker.ui.theme.FlameAmber
import com.oleh11.vatrasmoker.ui.theme.FlameOrange
import com.oleh11.vatrasmoker.ui.theme.HickoryBrown
import com.oleh11.vatrasmoker.ui.theme.SmokerDark
import com.oleh11.vatrasmoker.ui.theme.SuccessGreen
import com.oleh11.vatrasmoker.ui.theme.TextGray
import com.oleh11.vatrasmoker.ui.theme.TextWhite
import com.oleh11.vatrasmoker.ui.theme.WarningYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.smokerState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val cookHistory by viewModel.cookHistory.collectAsState()
    val showProbes by viewModel.showMeatProbes.collectAsState()
    var pendingTempF by remember(state.targetChamberTempF) { mutableStateOf(state.targetChamberTempF) }
    var pendingSmoke by remember(state.smokeLevel) { mutableStateOf(state.smokeLevel) }
    var showExtraProbes by remember { mutableStateOf(false) }

    var showShutdownDialog by remember { mutableStateOf(false) }
    var showWifiDialog by remember { mutableStateOf(false) }
    var wifiSsidInput by remember { mutableStateOf(state.wifiSsid ?: "Home_BBQ_2.4G") }
    var wifiPasswordInput by remember { mutableStateOf("") }
    var wifiSavedNotice by remember { mutableStateOf(false) }

    // Shutdown Safety Confirmation Dialog
    if (showShutdownDialog) {
        AlertDialog(
            onDismissRequest = { showShutdownDialog = false },
            title = {
                Text(
                    text = "Shutdown Cabela's Smoker?",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "The controller will initiate the automated fan cooldown cycle to safely burn down remaining wood pellets in the burn pot before fully powering off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.shutdownGrill()
                        showShutdownDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = TextWhite)
                ) {
                    Text("Confirm Shutdown", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShutdownDialog = false }) {
                    Text("Cancel", color = TextWhite)
                }
            },
            containerColor = Charcoal800
        )
    }

    // Wi-Fi Network Setup Dialog
    if (showWifiDialog) {
        AlertDialog(
            onDismissRequest = { showWifiDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = FlameOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Grill Wi-Fi Network",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your 2.4 GHz home Wi-Fi credentials to transmit them to your Cabela's smoker over Bluetooth.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = wifiSsidInput,
                        onValueChange = { wifiSsidInput = it },
                        label = { Text("Network Name (SSID)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlameOrange,
                            unfocusedBorderColor = Charcoal700,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = wifiPasswordInput,
                        onValueChange = { wifiPasswordInput = it },
                        label = { Text("Password (WPA2)") },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = FlameOrange)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlameOrange,
                            unfocusedBorderColor = Charcoal700,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (wifiSavedNotice) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Network credentials transmitted to grill!",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setWifiNetwork(wifiSsidInput, wifiPasswordInput)
                        wifiSavedNotice = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlameOrange, contentColor = TextWhite)
                ) {
                    Text("Save & Connect Grill", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showWifiDialog = false
                    wifiSavedNotice = false
                }) {
                    Text("Close", color = TextWhite)
                }
            },
            containerColor = Charcoal800
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Vatra Smoker Pro Logo",
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VATRA SMOKER PRO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = FlameAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Cook Time: ${state.formattedCookDuration}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    ConnectionStatusBadge(
                        connectionState = connectionState,
                        onClick = onNavigateToScan
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Charcoal900)
            )
        },
        containerColor = Charcoal900,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Grill Controls Action Bar (Single Clear Power/Shutdown Control + Wi-Fi Setup)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Single Clear Power / Shutdown Button
                    Button(
                        onClick = {
                            if (state.powerMode == SmokerPowerMode.RUNNING) {
                                showShutdownDialog = true
                            } else {
                                viewModel.startupGrill()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (state.powerMode) {
                                SmokerPowerMode.RUNNING -> AlertRed
                                SmokerPowerMode.SHUTDOWN -> FlameAmber
                                else -> SuccessGreen
                            },
                            contentColor = TextWhite
                        ),
                        border = BorderStroke(1.dp, Charcoal600),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(
                            imageVector = when (state.powerMode) {
                                SmokerPowerMode.RUNNING -> Icons.Default.PowerOff
                                SmokerPowerMode.SHUTDOWN -> Icons.Default.HourglassBottom
                                else -> Icons.Default.PowerSettingsNew
                            },
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (state.powerMode) {
                                    SmokerPowerMode.RUNNING -> "Shutdown Smoker"
                                    SmokerPowerMode.SHUTDOWN -> "Cooldown Active"
                                    else -> "Start Smoker"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = when (state.powerMode) {
                                    SmokerPowerMode.RUNNING -> "Tap to safely cool down"
                                    SmokerPowerMode.SHUTDOWN -> "Fan cooling firepot"
                                    else -> "Ignite & preheat grill"
                                },
                                color = TextWhite.copy(alpha = 0.9f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Set Grill Network Button
                    OutlinedButton(
                        onClick = { showWifiDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (state.wifiConnected) SuccessGreen else FlameOrange),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Charcoal800,
                            contentColor = TextWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (state.wifiConnected) SuccessGreen else FlameOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (state.wifiConnected) "Wi-Fi: OK" else "Grill Wi-Fi",
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = if (state.wifiConnected) "Online" else "Configure",
                                color = TextGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            // Main Grill Temperature Control
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Charcoal800),
                    border = BorderStroke(1.dp, Charcoal600),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        RadialTempGauge(
                            currentTempF = state.currentChamberTempF,
                            targetTempF = pendingTempF,
                            unit = state.temperatureUnit,
                            onTargetChange = { pendingTempF = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Setpoint Buttons
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val quickPresets = listOf(
                                Pair(180, "Smoke"),
                                Pair(225, "Low/Slow"),
                                Pair(250, "BBQ"),
                                Pair(275, "Poultry"),
                                Pair(350, "Roast"),
                                Pair(450, "Sear")
                            )
                            items(quickPresets) { (temp, label) ->
                                val isSelected = pendingTempF == temp
                                OutlinedButton(
                                    onClick = { pendingTempF = temp },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) FlameOrange else Charcoal700,
                                        contentColor = TextWhite
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) FlameAmber else Charcoal700),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = state.temperatureUnit.format(temp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) TextWhite else TextGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (pendingTempF != state.targetChamberTempF) {
                            Spacer(modifier = Modifier.height(14.dp))
                            ConfirmBar(
                                label = "Confirm ${state.temperatureUnit.format(pendingTempF)}",
                                onConfirm = { viewModel.setTargetTemperature(pendingTempF) },
                                onCancel = { pendingTempF = state.targetChamberTempF }
                            )
                        }
                    }
                }
            }

            // Smoke Level Dial
            item {
                Column {
                    SmokeDial(
                        smokeLevel = pendingSmoke,
                        onLevelChange = { pendingSmoke = it }
                    )
                    if (pendingSmoke != state.smokeLevel) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ConfirmBar(
                            label = "Confirm smoke level $pendingSmoke",
                            onConfirm = { viewModel.setSmokeLevel(pendingSmoke) },
                            onCancel = { pendingSmoke = state.smokeLevel }
                        )
                    }
                }
            }

            if (showProbes) {
            // Dual Meat Probes Section (Probes 1 & 2 Optimized)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "MEAT PROBES",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    OutlinedButton(
                        onClick = { showExtraProbes = !showExtraProbes },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, Charcoal700)
                    ) {
                        Text(
                            text = if (showExtraProbes) "Hide probes 3 & 4" else "Show probes 3 & 4",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                }
            }

            item {
                ProbeCard(
                    probe = state.probe1,
                    unit = state.temperatureUnit,
                    onTargetChange = { viewModel.setProbeTarget(1, it) }
                )
            }

            item {
                ProbeCard(
                    probe = state.probe2,
                    unit = state.temperatureUnit,
                    onTargetChange = { viewModel.setProbeTarget(2, it) }
                )
            }

            if (showExtraProbes) {
                item {
                    ProbeCard(
                        probe = state.probe3,
                        unit = state.temperatureUnit,
                        onTargetChange = { viewModel.setProbeTarget(3, it) }
                    )
                }

                item {
                    ProbeCard(
                        probe = state.probe4,
                        unit = state.temperatureUnit,
                        onTargetChange = { viewModel.setProbeTarget(4, it) }
                    )
                }
            }
            }

            // Live Cook Temperature Curve Chart
            item {
                LiveCookChart(
                    history = cookHistory,
                    unit = state.temperatureUnit,
                    showProbes = showProbes
                )
            }
        }
    }
}

/** Two buttons shown under a control once its value was changed but not yet sent to the grill. */
@Composable
private fun ConfirmBar(label: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Charcoal600),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray),
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel", maxLines = 1)
        }
        Button(
            onClick = onConfirm,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FlameOrange, contentColor = TextWhite),
            modifier = Modifier.weight(2f)
        ) {
            Text(label, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
