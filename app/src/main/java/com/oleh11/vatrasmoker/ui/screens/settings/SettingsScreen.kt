package com.oleh11.vatrasmoker.ui.screens.settings

import com.oleh11.vatrasmoker.ui.theme.ThemeSettings
import com.oleh11.vatrasmoker.ui.theme.ThemeMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import com.oleh11.vatrasmoker.ui.theme.OnAccent
import com.oleh11.vatrasmoker.ui.theme.Accent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oleh11.vatrasmoker.data.model.TemperatureUnit
import com.oleh11.vatrasmoker.ui.theme.Charcoal600
import com.oleh11.vatrasmoker.ui.theme.Charcoal700
import com.oleh11.vatrasmoker.ui.theme.Charcoal800
import com.oleh11.vatrasmoker.ui.theme.Charcoal900
import com.oleh11.vatrasmoker.ui.theme.FlameAmber
import com.oleh11.vatrasmoker.ui.theme.FlameOrange
import com.oleh11.vatrasmoker.ui.theme.TextGray
import com.oleh11.vatrasmoker.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onShowTour: () -> Unit = {}
) {
    val smokerState by viewModel.smokerState.collectAsState()
    val isSimulatorMode by viewModel.isSimulatorMode.collectAsState()
    val showMeatProbes by viewModel.showMeatProbes.collectAsState()
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember {
        try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
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
                top = 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())
        ) {
            // Temperature Unit Setting
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Charcoal800),
                    border = BorderStroke(1.dp, Charcoal600),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = FlameOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Temperature Unit",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Fahrenheit (°F) / Celsius (°C)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                        }

                        Switch(
                            checked = smokerState.temperatureUnit == TemperatureUnit.CELSIUS,
                            onCheckedChange = { isCelsius ->
                                viewModel.setTemperatureUnit(
                                    if (isCelsius) TemperatureUnit.CELSIUS else TemperatureUnit.FAHRENHEIT
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccent,
                                checkedTrackColor = Accent,
                                uncheckedTrackColor = Charcoal700
                            )
                        )
                    }
                }
            }

            // Appearance: follow the phone, or force light / dark
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Charcoal800),
                    border = BorderStroke(1.dp, Charcoal600),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("Follows your phone by default", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Charcoal700, RoundedCornerShape(50))
                                .padding(4.dp)
                        ) {
                            listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (mode, label) ->
                                val selected = ThemeSettings.mode == mode
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selected) Accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(50))
                                        .clickable { ThemeSettings.set(appContext, mode) }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) OnAccent else TextWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Meat probes on/off
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Charcoal800),
                    border = BorderStroke(1.dp, Charcoal600),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = FlameOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Meat Probes",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Show probe temperatures and chart lines. Turn off to see only grill temperature and smoke level.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                        }

                        Switch(
                            checked = showMeatProbes,
                            onCheckedChange = { viewModel.setShowMeatProbes(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccent,
                                checkedTrackColor = Accent,
                                uncheckedTrackColor = Charcoal700
                            )
                        )
                    }
                }
            }

            // Privacy assurance (static, nothing is collected)
            item {
                SettingsInfoCard(
                    icon = Icons.Default.Lock,
                    title = "Private by design",
                    subtitle = "No ads, no accounts, no tracking and no internet permission. Everything stays between your phone and your grill."
                )
            }

            // Help, share, rate, feedback (all opt-in buttons, nothing is sent or tracked)
            item {
                SettingsActionCard(
                    icon = Icons.Default.HelpOutline,
                    title = "How to use the app",
                    subtitle = "Quick guide: connect, set temperature and smoke",
                    onClick = onShowTour
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Default.Share,
                    title = "Share the app",
                    subtitle = "Send a link to friends who also have a pellet grill",
                    onClick = {
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Vatra Smoker Pro")
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "Vatra Smoker Pro is a free, open-source app to control a Camp Chef-based pellet grill " +
                                    "(including the Cabela's Pro Series) over Bluetooth. No ads, no tracking.\n" + GITHUB_URL
                            )
                        }
                        appContext.startActivity(
                            android.content.Intent.createChooser(send, "Share Vatra Smoker Pro")
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Default.Star,
                    title = "Rate on Google Play",
                    subtitle = "Your review helps other grillers find the app",
                    onClick = { openLink(appContext, PLAY_URL) }
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Default.Feedback,
                    title = "Send feedback",
                    subtitle = "Report a problem or suggest a feature on GitHub",
                    onClick = { openLink(appContext, "$GITHUB_URL/issues") }
                )
            }

            // Hardware & Info Card
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Charcoal800),
                    border = BorderStroke(1.dp, Charcoal600),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeviceHub,
                                contentDescription = null,
                                tint = FlameOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HARDWARE & SYSTEM INFO",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRow("Target Smoker", "Cabela's Pro Series 36\" Wi-Fi Pellet Grill")
                        InfoRow("PID Controller", "Camp Chef Wi-Fi / BLE Gen 2")
                        InfoRow("Bluetooth Standard", "BLE 5.0 (Low Energy GATT)")
                        InfoRow("Wi-Fi Spectrum", "2.4 GHz 802.11 b/g/n")
                        InfoRow("Meat Probes Supported", "4 Ports (NTC 100K Thermistor)")
                        InfoRow("App Version", appVersion)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextGray)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextWhite, fontWeight = FontWeight.SemiBold)
    }
}

private const val GITHUB_URL = "https://github.com/oleh11/vatra-smoker-pro"
private const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.oleh11.vatrasmoker"

private fun openLink(context: android.content.Context, url: String) {
    try {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        // No browser or Play Store available: nothing to do
    }
}

@Composable
private fun SettingsInfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Charcoal800),
        border = BorderStroke(1.dp, Charcoal600),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = FlameOrange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Charcoal800),
        border = BorderStroke(1.dp, Charcoal600),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = FlameOrange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
        }
    }
}
