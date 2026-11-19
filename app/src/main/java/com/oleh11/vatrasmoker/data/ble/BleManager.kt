package com.oleh11.vatrasmoker.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.oleh11.vatrasmoker.data.model.ConnectionState
import com.oleh11.vatrasmoker.data.model.MeatProbe
import com.oleh11.vatrasmoker.data.model.PelletHopperState
import com.oleh11.vatrasmoker.data.model.SmokerPowerMode
import com.oleh11.vatrasmoker.data.model.SmokerState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DiscoveredGattInfo(
    val serviceUuid: String,
    val characteristics: List<DiscoveredCharInfo>
)

data class DiscoveredCharInfo(
    val charUuid: String,
    val properties: String,
    val isReadable: Boolean,
    val isWritable: Boolean,
    val isNotifiable: Boolean
)

@SuppressLint("MissingPermission")
class BleManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val tag = "BleManager"
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private var activeGatt: BluetoothGatt? = null
    private var readNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _smokerState = MutableStateFlow(SmokerState())
    val smokerState: StateFlow<SmokerState> = _smokerState.asStateFlow()

    // Diagnostics & Debugging Observables
    private val _debugLogs = MutableStateFlow<List<String>>(listOf("[INIT] BLE Manager initialized"))
    val debugLogs: StateFlow<List<String>> = _debugLogs.asStateFlow()

    private val _discoveredGattServices = MutableStateFlow<List<DiscoveredGattInfo>>(emptyList())
    val discoveredGattServices: StateFlow<List<DiscoveredGattInfo>> = _discoveredGattServices.asStateFlow()

    private val _rawIncomingHex = MutableStateFlow<List<String>>(emptyList())
    val rawIncomingHex: StateFlow<List<String>> = _rawIncomingHex.asStateFlow()

    private var isScanning = false
    private var scanTimeoutJob: Job? = null

    // Android allows only one GATT operation in flight. Each operation holds this lock
    // until its callback arrives (or it times out), so operations never collide.
    private val gattMutex = Mutex()
    private var pendingOp: CompletableDeferred<GattResult>? = null

    private class GattResult(val status: Int, val value: ByteArray? = null)

    // Which grill family we are connected to (decides service + characteristic UUIDs)
    private var brand: SmokerGattUuid.Brand? = null
    private var grillService: BluetoothGattService? = null

    // Connection lifecycle: retries, reconnects and background jobs
    @Volatile private var lastDevice: BluetoothDevice? = null
    @Volatile private var userDisconnected = true
    private var retryCount = 0
    private var attemptId = 0
    private var sessionJob: Job? = null
    private var pollJob: Job? = null
    private var connectTimeoutJob: Job? = null
    private val grillAddresses = mutableSetOf<String>()

    private companion object {
        const val MAX_RETRIES = 4
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val POLL_INTERVAL_MS = 5_000L
        const val BOND_TIMEOUT_MS = 30_000L
        const val STATUS_INSUFFICIENT_AUTH = 5
        const val STATUS_INSUFFICIENT_ENCRYPTION = 15
    }

    init {
        registerBondReceiver()
    }

    /**
     * Runs one GATT operation and waits for its callback.
     * [start] kicks off the operation and returns false if Android refused it.
     */
    private suspend fun gattOp(
        label: String,
        timeoutMs: Long = 5000,
        start: () -> Boolean
    ): GattResult? = gattMutex.withLock {
        val waiter = CompletableDeferred<GattResult>()
        pendingOp = waiter
        val started = try {
            start()
        } catch (e: Exception) {
            logDebug("❌ $label threw: ${e.message}")
            false
        }
        if (!started) {
            pendingOp = null
            logDebug("❌ $label was refused by Android BLE stack")
            return@withLock null
        }
        val result = withTimeoutOrNull(timeoutMs) { waiter.await() }
        pendingOp = null
        if (result == null) logDebug("⏱️ $label timed out after ${timeoutMs}ms")
        result
    }

    private fun completeOp(status: Int, value: ByteArray? = null) {
        pendingOp?.complete(GattResult(status, value))
    }

    private fun logDebug(message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp] $message"
        Log.d(tag, entry)
        val current = _debugLogs.value
        _debugLogs.value = (current + entry).takeLast(200)
    }

    private fun registerBondReceiver() {
        try {
            val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                        val bondStr = when (bondState) {
                            BluetoothDevice.BOND_BONDED -> "BONDED (Paired Successfully)"
                            BluetoothDevice.BOND_BONDING -> "BONDING (Pairing in progress...)"
                            BluetoothDevice.BOND_NONE -> "BOND_NONE (Not Paired)"
                            else -> "UNKNOWN ($bondState)"
                        }
                        logDebug("🔑 Bond State Changed for ${device?.name ?: device?.address}: $bondStr")
                    }
                }
            }
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            logDebug("Warning: Could not register bond receiver: ${e.message}")
        }
    }

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    fun clearLogs() {
        _debugLogs.value = listOf("[CLEAR] Logs cleared at ${dateFormat.format(Date())}")
    }

    fun startScan(timeoutMs: Long = 20000) {
        if (!isBluetoothEnabled) {
            logDebug("❌ Scan Aborted: Bluetooth is turned OFF on this phone!")
            _connectionState.value = ConnectionState.Error("Bluetooth is turned OFF. Please turn on Bluetooth.")
            return
        }
        if (scanner == null) {
            logDebug("❌ Scan Aborted: BluetoothLeScanner is null.")
            _connectionState.value = ConnectionState.Error("BLE Scanner not available on this device.")
            return
        }

        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning()
        isScanning = true
        logDebug("🔍 BLE Scan Started (Low Latency mode, timeout ${timeoutMs / 1000}s)...")

        val filters = listOf<ScanFilter>()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(filters, settings, scanCallback)
            scanTimeoutJob?.cancel()
            scanTimeoutJob = scope.launch {
                delay(timeoutMs)
                logDebug("⏱️ BLE Scan Timeout reached (${timeoutMs / 1000}s). Stopping scan.")
                stopScan()
            }
        } catch (e: Exception) {
            logDebug("❌ Exception starting scan: ${e.message}")
            _connectionState.value = ConnectionState.Error("Scan error: ${e.localizedMessage}")
            isScanning = false
        }
    }

    fun stopScan() {
        if (!isScanning) return
        try {
            scanner?.stopScan(scanCallback)
            logDebug("🛑 BLE Scan Stopped.")
        } catch (e: Exception) {
            logDebug("Error stopping scan: ${e.message}")
        } finally {
            isScanning = false
            scanTimeoutJob?.cancel()
            if (_connectionState.value is ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    private fun isGrillResult(result: ScanResult): Boolean {
        val name = result.device.name ?: result.scanRecord?.deviceName
        val byName = name != null && SmokerGattUuid.Brand.entries.any {
            name.startsWith(it.advertisedNamePrefix.trimEnd(':'), ignoreCase = true)
        }
        val byService = result.scanRecord?.serviceUuids?.any {
            SmokerGattUuid.Brand.fromService(it.uuid) != null
        } == true
        return byName || byService
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val found = result ?: return
            val device = found.device
            val name = device.name ?: found.scanRecord?.deviceName
            val isGrill = isGrillResult(found)
            if (!isGrill && name.isNullOrBlank()) return

            if (isGrill) grillAddresses.add(device.address)
            val currentList = _discoveredDevices.value
            if (currentList.none { it.address == device.address }) {
                logDebug("📡 Found BLE Device: '${name ?: "Unknown"}' [${device.address}] RSSI: ${found.rssi}dBm ${if (isGrill) "⭐ [GRILL]" else ""}")
                _discoveredDevices.value = (currentList + device).sortedWith(
                    compareByDescending<BluetoothDevice> { it.address in grillAddresses }
                        .thenBy { it.name ?: "" }
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "SCAN_FAILED_ALREADY_STARTED (1)"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED (2)"
                SCAN_FAILED_INTERNAL_ERROR -> "SCAN_FAILED_INTERNAL_ERROR (3)"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "SCAN_FAILED_FEATURE_UNSUPPORTED (4)"
                else -> "Code $errorCode"
            }
            logDebug("❌ BLE Scan Failed: $errorMsg")
            _connectionState.value = ConnectionState.Error("Scan failed: $errorMsg")
            isScanning = false
        }
    }

    fun initiateBond(device: BluetoothDevice) {
        logDebug("🔑 Triggering OS Bluetooth Pairing Bond for ${device.name ?: device.address}...")
        try {
            val success = device.createBond()
            logDebug("🔑 createBond() returned: $success. Check for OS PIN prompt!")
        } catch (e: Exception) {
            logDebug("❌ Error initiating bond: ${e.message}")
        }
    }

    /** User-initiated connect. Retries and reconnects are handled internally. */
    fun connect(device: BluetoothDevice) {
        stopScan()
        userDisconnected = false
        retryCount = 0
        lastDevice = device
        // Start from "no data yet" so the dashboard never shows placeholder readings as real
        _smokerState.value = SmokerState(
            probe1 = MeatProbe(id = 1),
            probe2 = MeatProbe(id = 2),
            probe3 = MeatProbe(id = 3),
            probe4 = MeatProbe(id = 4),
            hopper = PelletHopperState(levelPercentage = 0)
        )
        openGatt(device)
    }

    private fun openGatt(device: BluetoothDevice) {
        val deviceName = device.name ?: "Cabela's Smoker"
        logDebug("🔌 Connecting to: '$deviceName' [${device.address}] (Bond: ${device.bondState}, attempt ${retryCount + 1})...")
        teardownGatt()
        val attempt = attemptId
        _connectionState.value = ConnectionState.Connecting(deviceName = deviceName, address = device.address)
        activeGatt = try {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            logDebug("❌ connectGatt denied: ${e.message}")
            _connectionState.value = ConnectionState.Error("Bluetooth permission missing. Allow Nearby devices in Settings.")
            return
        }
        connectTimeoutJob = scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (attempt == attemptId && _connectionState.value is ConnectionState.Connecting) {
                handleLinkFailure("Connection timed out. Is the grill powered on and within range?")
            }
        }
    }

    /** Closes the GATT link and cancels every background job without changing the UI state. */
    private fun teardownGatt() {
        attemptId++
        connectTimeoutJob?.cancel(); connectTimeoutJob = null
        sessionJob?.cancel(); sessionJob = null
        pollJob?.cancel(); pollJob = null
        pendingOp?.cancel(); pendingOp = null
        val gatt = activeGatt
        activeGatt = null
        readNotifyCharacteristic = null
        writeCharacteristic = null
        modeCharacteristic = null
        wifiSsidCharacteristic = null
        wifiPassCharacteristic = null
        grillService = null
        brand = null
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (e: Exception) {
            logDebug("Error closing GATT: ${e.message}")
        }
    }

    /** User-initiated disconnect. Does not reconnect. */
    fun disconnect() {
        userDisconnected = true
        logDebug("🔌 Disconnecting...")
        teardownGatt()
        _connectionState.value = ConnectionState.Disconnected
    }

    /** The link dropped or never came up: retry with a short back-off, then give up. */
    private fun handleLinkFailure(reason: String) {
        val device = lastDevice
        teardownGatt()
        if (!userDisconnected && device != null && retryCount < MAX_RETRIES) {
            retryCount++
            val wait = 700L * retryCount
            logDebug("🔁 $reason Retrying in ${wait}ms ($retryCount/$MAX_RETRIES)...")
            _connectionState.value = ConnectionState.Connecting(device.name ?: "Cabela's Smoker", device.address)
            val attempt = attemptId
            scope.launch {
                delay(wait)
                if (!userDisconnected && attempt == attemptId && activeGatt == null) openGatt(device)
            }
        } else {
            logDebug("❌ Giving up: $reason")
            _connectionState.value = ConnectionState.Error(reason)
        }
    }

    private fun failSession(message: String) {
        logDebug("❌ $message")
        userDisconnected = true // a setup problem will not fix itself by retrying
        teardownGatt()
        _connectionState.value = ConnectionState.Error(message)
    }

    /** Pairs with the grill if the OS has not yet, and waits for the result. */
    private suspend fun ensureBonded(device: BluetoothDevice): Boolean {
        if (device.bondState == BluetoothDevice.BOND_BONDED) return true
        if (device.bondState == BluetoothDevice.BOND_NONE) {
            logDebug("🔑 Grill requires pairing. Starting bond...")
            if (!device.createBond()) return false
        }
        var sawBonding = device.bondState == BluetoothDevice.BOND_BONDING
        val deadline = System.currentTimeMillis() + BOND_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> return true
                BluetoothDevice.BOND_BONDING -> sawBonding = true
                BluetoothDevice.BOND_NONE -> if (sawBonding) return false
            }
            delay(300)
        }
        return false
    }

    private fun needsPairing(status: Int?) =
        status == STATUS_INSUFFICIENT_AUTH || status == STATUS_INSUFFICIENT_ENCRYPTION

    /** Runs a GATT op; if the grill demands encryption, pairs once and retries. */
    private suspend fun withPairingRetry(device: BluetoothDevice, op: suspend () -> GattResult?): GattResult? {
        val first = op()
        if (needsPairing(first?.status) && ensureBonded(device)) {
            delay(500)
            return op()
        }
        return first
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt !== activeGatt) return // stale callback from a link we already closed
            val device = gatt.device
            logDebug("📶 onConnectionStateChange: device=${device.name ?: device.address}, status=$status, newState=$newState")

            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                connectTimeoutJob?.cancel()
                logDebug("✅ GATT Connected! Setting up grill session...")
                _connectionState.value = ConnectionState.Authenticating(device.name ?: "Smoker")
                sessionJob?.cancel()
                sessionJob = scope.launch { setUpSession(gatt) }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                val why = when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "The grill closed the connection."
                    133 -> "Bluetooth error 133 (connection rejected or timing collision)."
                    19 -> "The grill closed the connection (code 19). Make sure no other phone is connected."
                    8 -> "Connection lost (code 8): grill out of range or busy."
                    22 -> "Connection ended by this phone (code 22)."
                    else -> "Bluetooth error $status."
                }
                handleLinkFailure(why)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            logDebug("📦 onMtuChanged: MTU is now $mtu (status=$status)")
            completeOp(status)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            logDebug("📋 onServicesDiscovered: status=$status, total services=${gatt.services.size}")
            completeOp(status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            val hex = value.toHex()
            logDebug("📥 onCharacteristicRead [${characteristic.uuid}]: $hex (status=$status)")
            completeOp(status, value)
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val value = characteristic.value ?: ByteArray(0)
                logDebug("📥 onCharacteristicRead [${characteristic.uuid}]: ${value.toHex()} (status=$status)")
                completeOp(status, value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            onNotification(characteristic.uuid, value)
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                characteristic.value?.let { onNotification(characteristic.uuid, it) }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            logDebug("📤 onCharacteristicWrite [${characteristic.uuid}]: status=$status (${if (status == BluetoothGatt.GATT_SUCCESS) "SUCCESS" else "FAIL"})")
            completeOp(status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            logDebug("📝 onDescriptorWrite [${descriptor.characteristic.uuid}]: status=$status (${if (status == BluetoothGatt.GATT_SUCCESS) "Notifications Active" else "Fail"})")
            completeOp(status)
        }
    }

    private fun onNotification(uuid: UUID, value: ByteArray) {
        val hex = value.toHex()
        _rawIncomingHex.value = (_rawIncomingHex.value + hex).takeLast(50)
        logDebug("🔔 Notify [$uuid]: $hex")
        handleIncomingBytes(uuid, value)
    }

    /**
     * Same order as the official app: find the brand service, subscribe to the
     * multiplex characteristic, then read each value once to fill the dashboard.
     */
    private suspend fun setUpSession(gatt: BluetoothGatt) {
        delay(300)
        gattOp("requestMtu", timeoutMs = 2500) { gatt.requestMtu(185) }

        val discovered = gattOp("discoverServices", timeoutMs = 15000) { gatt.discoverServices() }
        if (discovered?.status != BluetoothGatt.GATT_SUCCESS) {
            handleLinkFailure("Could not read the grill's services (code ${discovered?.status}).")
            return
        }
        _discoveredGattServices.value = dumpAndAnalyzeServices(gatt)

        val service = gatt.services.firstOrNull { SmokerGattUuid.Brand.fromService(it.uuid) != null }
        val foundBrand = service?.let { SmokerGattUuid.Brand.fromService(it.uuid) }
        if (service == null || foundBrand == null) {
            failSession("This device is not a supported grill (grill service not found).")
            return
        }
        grillService = service
        brand = foundBrand
        logDebug("🎯 Grill service: ${foundBrand.displayName} (${service.uuid})")

        modeCharacteristic = service.getCharacteristic(foundBrand.characteristic(SmokerGattUuid.WS_ID_MODE))
        writeCharacteristic = modeCharacteristic
        wifiSsidCharacteristic = service.getCharacteristic(foundBrand.characteristic(SmokerGattUuid.WS_ID_WIFI_SSID))
        wifiPassCharacteristic = service.getCharacteristic(foundBrand.characteristic(SmokerGattUuid.WS_ID_WIFI_PASSWORD))
        if (modeCharacteristic == null) logDebug("⚠️ Mode characteristic not found; commands will not work.")

        // 1. Live updates all arrive on the multiplex characteristic
        val multiplex = service.getCharacteristic(foundBrand.multiplex)
        if (multiplex != null) {
            readNotifyCharacteristic = multiplex
            withPairingRetry(gatt.device) { enableNotifications(gatt, multiplex) }
        } else {
            logDebug("⚠️ Multiplex characteristic ${foundBrand.multiplex} not found; no live updates.")
        }

        // 2. Read current values once
        val initialReads = buildList {
            add(SmokerGattUuid.WS_ID_MODE)
            add(SmokerGattUuid.WS_ID_TRANSITIONING)
            add(SmokerGattUuid.WS_ID_CHAMBER_TEMP)
            for (p in 1..SmokerGattUuid.PROBE_COUNT) add(SmokerGattUuid.probeWsId(p))
            add(SmokerGattUuid.WS_ID_LAST_FAULT)
        }
        for (wsId in initialReads) {
            val char = service.getCharacteristic(foundBrand.characteristic(wsId)) ?: continue
            if ((char.properties and BluetoothGattCharacteristic.PROPERTY_READ) == 0) continue
            val result = withPairingRetry(gatt.device) { readChar(gatt, char) }
            if (result?.status == BluetoothGatt.GATT_SUCCESS && result.value != null) {
                _smokerState.value = SmokerPacketCodec.parseCharacteristicValue(wsId, result.value, _smokerState.value)
            } else if (result != null) {
                logDebug("⚠️ Read of wsId 0x%02X failed (status=${result.status}). If this is 5/15, pair the grill first.".format(wsId))
            }
        }

        retryCount = 0
        _connectionState.value = ConnectionState.Connected(
            deviceName = gatt.device.name ?: "Cabela's Pro Series 36\"",
            address = gatt.device.address
        )
        startPolling(gatt, service, foundBrand)
        logDebug("🎉 Smoker Connected & Ready! Listening on ${foundBrand.multiplex}")
    }

    private fun dumpAndAnalyzeServices(gatt: BluetoothGatt): List<DiscoveredGattInfo> {
        val list = mutableListOf<DiscoveredGattInfo>()
        for (service in gatt.services) {
            logDebug("  📂 Service: ${service.uuid}")
            val charList = mutableListOf<DiscoveredCharInfo>()
            for (char in service.characteristics) {
                val props = char.properties
                val isReadable = (props and BluetoothGattCharacteristic.PROPERTY_READ) != 0
                val isWritable = (props and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                val isNotifiable = (props and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0

                val propStr = buildString {
                    if (isReadable) append("READ ")
                    if (isWritable) append("WRITE ")
                    if (isNotifiable) append("NOTIFY ")
                }
                logDebug("    📄 Char: ${char.uuid} -> [$propStr]")
                charList.add(
                    DiscoveredCharInfo(
                        charUuid = char.uuid.toString(),
                        properties = propStr.trim(),
                        isReadable = isReadable,
                        isWritable = isWritable,
                        isNotifiable = isNotifiable
                    )
                )
            }
            list.add(DiscoveredGattInfo(service.uuid.toString(), charList))
        }
        return list
    }

    private var modeCharacteristic: BluetoothGattCharacteristic? = null
    private var wifiSsidCharacteristic: BluetoothGattCharacteristic? = null
    private var wifiPassCharacteristic: BluetoothGattCharacteristic? = null

    private suspend fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic): GattResult? {
        logDebug("🔔 Subscribing to notifications for: ${characteristic.uuid}...")
        val setSuccess = gatt.setCharacteristicNotification(characteristic, true)
        logDebug("   setCharacteristicNotification returned: $setSuccess")

        val descriptor = characteristic.getDescriptor(SmokerGattUuid.CCCD_DESCRIPTOR)
        if (descriptor == null) {
            logDebug("   Warning: CCCD Descriptor not found on characteristic ${characteristic.uuid}")
            return null
        }
        val value = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }
        val result = gattOp("writeDescriptor ${characteristic.uuid}") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
        if (result?.status != BluetoothGatt.GATT_SUCCESS) {
            logDebug("⚠️ Subscribe failed (status=${result?.status}).")
        }
        return result
    }

    /** Notifications drive live updates; this slow poll is a safety net if one is ever missed. */
    private fun startPolling(gatt: BluetoothGatt, service: BluetoothGattService, foundBrand: SmokerGattUuid.Brand) {
        pollJob?.cancel()
        pollJob = scope.launch {
            val ids = buildList {
                add(SmokerGattUuid.WS_ID_MODE)
                add(SmokerGattUuid.WS_ID_CHAMBER_TEMP)
                for (p in 1..SmokerGattUuid.PROBE_COUNT) add(SmokerGattUuid.probeWsId(p))
            }
            while (true) {
                delay(POLL_INTERVAL_MS)
                if (gatt !== activeGatt) return@launch
                for (wsId in ids) {
                    val char = service.getCharacteristic(foundBrand.characteristic(wsId)) ?: continue
                    if ((char.properties and BluetoothGattCharacteristic.PROPERTY_READ) == 0) continue
                    val r = readChar(gatt, char)
                    if (r?.status == BluetoothGatt.GATT_SUCCESS && r.value != null) {
                        _smokerState.value = SmokerPacketCodec.parseCharacteristicValue(wsId, r.value, _smokerState.value)
                    }
                }
            }
        }
    }

    private suspend fun readChar(gatt: BluetoothGatt, char: BluetoothGattCharacteristic): GattResult? =
        gattOp("read ${char.uuid}") { gatt.readCharacteristic(char) }

    private suspend fun writeChar(
        gatt: BluetoothGatt,
        char: BluetoothGattCharacteristic,
        payload: ByteArray
    ): GattResult? {
        val writeType = if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 &&
            (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
        ) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
        return gattOp("write ${char.uuid} ${payload.toHex()}") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(char, payload, writeType) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                char.writeType = writeType
                @Suppress("DEPRECATION")
                char.value = payload
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        }
    }

    private fun handleIncomingBytes(uuid: UUID, bytes: ByteArray) {
        val currentBrand = brand
        when {
            // Multiplex: [wsId][value...]
            currentBrand != null && uuid == currentBrand.multiplex ->
                _smokerState.value = SmokerPacketCodec.parseMultiplexNotification(bytes, _smokerState.value)

            // A single characteristic: wsId is the last byte of its UUID
            currentBrand != null && uuid.toString().startsWith(currentBrand.uuidPrefix, ignoreCase = true) -> {
                val wsId = uuid.toString().takeLast(2).toInt(16).toByte()
                _smokerState.value = SmokerPacketCodec.parseCharacteristicValue(wsId, bytes, _smokerState.value)
            }

            else -> SmokerPacketCodec.decodeStatusPacket(bytes, _smokerState.value)?.let { _smokerState.value = it }
        }
    }

    fun sendCommand(payload: ByteArray) {
        val gatt = activeGatt ?: run {
            logDebug("❌ sendCommand failed: activeGatt is null (Not connected)")
            return
        }
        val char = modeCharacteristic ?: writeCharacteristic ?: run {
            logDebug("❌ sendCommand failed: mode characteristic not found on this grill")
            return
        }
        logDebug("📤 Queuing Command write [${char.uuid}]: ${payload.toHex()}")
        scope.launch {
            val result = writeChar(gatt, char, payload)
            if (result?.status != BluetoothGatt.GATT_SUCCESS) {
                logDebug("❌ Command write failed (status=${result?.status})")
            }
        }
    }

    fun sendRawHex(hexString: String) {
        val cleanHex = hexString.replace(" ", "").replace("0x", "")
        if (cleanHex.length % 2 != 0) {
            logDebug("❌ Invalid hex string: length must be even")
            return
        }
        val bytes = ByteArray(cleanHex.length / 2) { i ->
            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        sendCommand(bytes)
    }

    fun setTargetTemperature(tempF: Int) {
        logDebug("🎯 Setting Target Temp to $tempF°F")
        val currentSmoke = _smokerState.value.smokeLevel
        sendCommand(SmokerPacketCodec.buildCabelasRunModePacket(tempF, currentSmoke))
        _smokerState.value = _smokerState.value.copy(targetChamberTempF = tempF)
    }

    fun setSmokeLevel(level: Int) {
        logDebug("💨 Setting Smoke Level to $level")
        val currentTemp = _smokerState.value.targetChamberTempF.takeIf { it > 0 } ?: 225
        sendCommand(SmokerPacketCodec.buildCabelasRunModePacket(currentTemp, level))
        _smokerState.value = _smokerState.value.copy(smokeLevel = level)
    }

    fun powerStartup() {
        logDebug("🔥 Sending Power Startup (RUN mode) command")
        val currentTemp = _smokerState.value.targetChamberTempF.takeIf { it > 0 } ?: 225
        val currentSmoke = _smokerState.value.smokeLevel
        sendCommand(SmokerPacketCodec.buildCabelasRunModePacket(currentTemp, currentSmoke))
        _smokerState.value = _smokerState.value.copy(powerMode = SmokerPowerMode.STARTUP)
    }

    fun powerShutdown() {
        logDebug("🛑 Sending Power Shutdown (STANDBY) command")
        sendCommand(SmokerPacketCodec.buildCabelasShutdownPacket())
        _smokerState.value = _smokerState.value.copy(powerMode = SmokerPowerMode.SHUTDOWN)
    }

    fun provisionWifi(ssid: String, pass: String) {
        logDebug("📶 Transmitting Wi-Fi Provisioning (SSID='$ssid')...")
        val gatt = activeGatt ?: return
        val ssidChar = wifiSsidCharacteristic
        val passChar = wifiPassCharacteristic
        if (ssidChar == null || passChar == null) {
            logDebug("❌ Wi-Fi characteristics (0x20/0x21) not found on this grill.")
            return
        }
        scope.launch {
            for (chunk in SmokerPacketCodec.buildChunkedStringWrites(ssid)) {
                writeChar(gatt, ssidChar, chunk)
            }
            for (chunk in SmokerPacketCodec.buildChunkedStringWrites(pass)) {
                writeChar(gatt, passChar, chunk)
            }
            logDebug("✅ Wi-Fi credentials sent to characteristics 0x20/0x21")
        }
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}
