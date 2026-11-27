package com.oleh11.vatrasmoker.ui.screens.wifi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.oleh11.vatrasmoker.data.wifi.WifiNetwork
import com.oleh11.vatrasmoker.data.wifi.WifiScanProblem
import com.oleh11.vatrasmoker.data.wifi.WifiScanner
import com.oleh11.vatrasmoker.ui.theme.Accent
import com.oleh11.vatrasmoker.ui.theme.Charcoal700
import com.oleh11.vatrasmoker.ui.theme.Charcoal800
import com.oleh11.vatrasmoker.ui.theme.FlameOrange
import com.oleh11.vatrasmoker.ui.theme.OnAccent
import com.oleh11.vatrasmoker.ui.theme.TextGray
import com.oleh11.vatrasmoker.ui.theme.TextWhite

/** Pick a nearby network (or type one), enter its password and send both to the grill. */
@Composable
fun WifiPickerDialog(onSend: (ssid: String, password: String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scanner = remember { WifiScanner(context) }
    val scan by scanner.state.collectAsState()

    var selected by remember { mutableStateOf<WifiNetwork?>(null) }
    var typing by remember { mutableStateOf(false) }
    var typedName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) scanner.scan() else scanner.notAllowed()
    }

    fun search() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scanner.scan()
        } else {
            permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    DisposableEffect(Unit) {
        search()
        onDispose { scanner.stop() }
    }

    val choosing = selected == null && !typing
    val ssid = if (typing) typedName.trim() else selected?.ssid.orEmpty()
    val needsPassword = typing || selected?.secured == true
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Accent,
        unfocusedBorderColor = Charcoal700,
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Charcoal800,
        title = {
            Text(
                text = if (choosing) "Choose Wi-Fi for the grill" else "Connect the grill",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (choosing) {
                    Text("Showing 2.4 GHz networks, which the grill needs.", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    val message = when {
                        scan.problem == WifiScanProblem.WIFI_OFF -> "Turn on Wi-Fi on your phone, then search again."
                        scan.problem == WifiScanProblem.LOCATION_OFF -> "Turn on Location. Android needs it to list Wi-Fi networks."
                        scan.problem == WifiScanProblem.NOT_ALLOWED ->
                            "Allow precise location for this app to list networks, or type the name instead."
                        scan.scanning && scan.networks.isEmpty() -> "Searching..."
                        scan.networks.isEmpty() -> "No 2.4 GHz networks found."
                        else -> null
                    }
                    if (scan.scanning) {
                        CircularProgressIndicator(color = FlameOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (message != null) {
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                    }
                    if (scan.problem == WifiScanProblem.LOCATION_OFF) {
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) {
                            Text("Open Location settings", color = FlameOrange)
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(scan.networks, key = { it.ssid }) { network ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = network
                                        password = ""
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = FlameOrange, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(network.ssid, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(signalLabel(network.rssi), style = MaterialTheme.typography.bodySmall, color = TextGray)
                                }
                                if (network.secured) {
                                    Icon(Icons.Default.Lock, contentDescription = "Password needed", tint = TextGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { search() }, enabled = !scan.scanning) { Text("Search again", color = TextWhite) }
                        TextButton(onClick = {
                            typing = true
                            password = ""
                        }) { Text("Other network", color = TextWhite) }
                    }
                } else {
                    if (typing) {
                        OutlinedTextField(
                            value = typedName,
                            onValueChange = { typedName = it },
                            label = { Text("Network name (SSID)") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(ssid, style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (needsPassword) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("This network has no password.", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                    TextButton(onClick = {
                        selected = null
                        typing = false
                    }) { Text("Back to the list", color = TextWhite) }
                }
            }
        },
        confirmButton = {
            if (!choosing) {
                Button(
                    onClick = { onSend(ssid, if (needsPassword) password else "") },
                    // A typed network may be open, so only a listed secured one insists on a password
                    enabled = ssid.isNotBlank() && (typing || !needsPassword || password.isNotEmpty()),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent)
                ) {
                    Text("Send to grill", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextWhite) }
        }
    )
}

private fun signalLabel(rssi: Int) = when {
    rssi >= -60 -> "Strong signal"
    rssi >= -72 -> "Fair signal"
    else -> "Weak signal"
}
