package com.oleh11.vatrasmoker.ui.screens.diagnostics

import android.annotation.SuppressLint
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oleh11.vatrasmoker.data.model.ConnectionState
import com.oleh11.vatrasmoker.ui.theme.AlertRed
import com.oleh11.vatrasmoker.ui.theme.Charcoal600
import com.oleh11.vatrasmoker.ui.theme.Charcoal700
import com.oleh11.vatrasmoker.ui.theme.Charcoal800
import com.oleh11.vatrasmoker.ui.theme.Charcoal900
import com.oleh11.vatrasmoker.ui.theme.FlameAmber
import com.oleh11.vatrasmoker.ui.theme.FlameOrange
import com.oleh11.vatrasmoker.ui.theme.SuccessGreen
import com.oleh11.vatrasmoker.ui.theme.TextGray
import com.oleh11.vatrasmoker.ui.theme.TextWhite
import com.oleh11.vatrasmoker.ui.theme.WarningYellow

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun BleDiagnosticsScreen(
    viewModel: BleDiagnosticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val logs by viewModel.debugLogs.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val discoveredServices by viewModel.discoveredGattServices.collectAsState()
    val rawHexList by viewModel.rawIncomingHex.collectAsState()
    val devices by viewModel.discoveredDevices.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var testHexInput by remember { mutableStateOf("01 00 00 01") }
    val logListState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.BugReport, contentDescription = null, tint = FlameAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BLE DIAGNOSTIC CONSOLE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val fullLog = logs.joinToString("\n")
                        clipboardManager.setText(AnnotatedString(fullLog))
                        Toast.makeText(context, "Diagnostic logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Logs", tint = FlameOrange)
                    }
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = "Clear Logs", tint = TextGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Charcoal900)
            )
        },
        containerColor = Charcoal900,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Diagnostic Status Bar
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Charcoal800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "GATT STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (connState) {
                                is ConnectionState.Connected -> "CONNECTED (${(connState as ConnectionState.Connected).deviceName})"
                                is ConnectionState.Connecting -> "CONNECTING..."
                                is ConnectionState.Scanning -> "SCANNING..."
                                is ConnectionState.Error -> "ERROR: ${(connState as ConnectionState.Error).message}"
                                else -> "DISCONNECTED"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (connState) {
                                is ConnectionState.Connected -> SuccessGreen
                                is ConnectionState.Error -> AlertRed
                                is ConnectionState.Scanning, is ConnectionState.Connecting -> FlameAmber
                                else -> TextGray
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        if (connState is ConnectionState.Scanning) {
                            Button(
                                onClick = { viewModel.stopScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = TextWhite),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop Scan", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange, contentColor = TextWhite),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Scan", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Charcoal900,
                contentColor = FlameOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = FlameOrange
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Live Logs", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("GATT Services (${discoveredServices.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Discovered (${devices.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Troubleshooting", fontWeight = FontWeight.Bold) }
                )
            }

            // Tab 0: Live Terminal Logs
            if (selectedTab == 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Charcoal900)
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        state = logListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = when {
                                    log.contains("❌") || log.contains("Error") || log.contains("Failed") -> AlertRed
                                    log.contains("✅") || log.contains("🎉") || log.contains("SUCCESS") -> SuccessGreen
                                    log.contains("🔑") || log.contains("Bond") || log.contains("MATCH") -> FlameAmber
                                    log.contains("📡") || log.contains("Found") -> FlameOrange
                                    else -> TextWhite
                                },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                // Raw Hex Test Sender
                Card(
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = Charcoal800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = testHexInput,
                            onValueChange = { testHexInput = it },
                            label = { Text("Send Hex Bytes") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FlameOrange,
                                unfocusedBorderColor = Charcoal700,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.sendRawHex(testHexInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = FlameOrange, contentColor = TextWhite),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        }
                    }
                }
            }

            // Tab 1: Discovered GATT Services Tree
            if (selectedTab == 1) {
                if (discoveredServices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No GATT Services Discovered Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextGray
                            )
                            Text(
                                text = "Connect to your Cabela's grill to inspect its services & characteristics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(discoveredServices) { service ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Charcoal800),
                                border = BorderStroke(1.dp, Charcoal600),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "📂 SERVICE UUID:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FlameOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = service.serviceUuid,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "CHARACTERISTICS (${service.characteristics.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    service.characteristics.forEach { char ->
                                        Column(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .background(Charcoal900, RoundedCornerShape(6.dp))
                                                .padding(8.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Text(
                                                text = char.charUuid,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = TextWhite,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(modifier = Modifier.padding(top = 2.dp)) {
                                                Text(
                                                    text = "PROPS: [${char.properties}]",
                                                    fontSize = 10.sp,
                                                    color = FlameAmber,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: Discovered Devices with PIN Bonding
            if (selectedTab == 2) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (devices.isEmpty()) {
                        item {
                            Text(
                                text = "No Bluetooth devices found in current scan. Tap 'Start Scan' above.",
                                color = TextGray,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                    items(devices) { device ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Charcoal800),
                            border = BorderStroke(1.dp, Charcoal600),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = device.name ?: "Unknown Device",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = device.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextGray
                                        )
                                    }
                                    Row {
                                        OutlinedButton(
                                            onClick = { viewModel.initiateBond(device) },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, FlameAmber),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = FlameAmber, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pair PIN", color = FlameAmber, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = { viewModel.connectBle(device) },
                                            shape = RoundedCornerShape(8.dp),
                                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = FlameOrange, contentColor = TextWhite),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Connect", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab 3: Troubleshooting Assistant
            if (selectedTab == 3) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Charcoal800),
                            border = BorderStroke(1.dp, Charcoal600),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "🔧 Cabela's BLE Connection Checklist",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FlameAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "1. Controller in Pairing Mode:\n• Press and hold the main controller knob on your Cabela's smoker for 3 to 5 seconds until the screen says 'PAIRING' and displays a 6-digit PIN.\n\n2. Disconnect Other Phones:\n• The PID grill controller allows ONLY 1 active Bluetooth connection. Make sure any other phone, tablet, or original app is closed / Bluetooth disconnected.\n\n3. Enable Android Location (GPS):\n• On Android 10/11/12+, the phone's Location (GPS) toggle MUST be turned ON in Android Quick Settings for BLE scanning to return devices.\n\n4. OS PIN Pairing (Bonding):\n• If the connection drops with status 133 or 19, tap 'Pair PIN' on the Discovered tab to trigger the Android system PIN dialog, and enter the 6-digit PIN shown on your smoker screen.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
