package com.oleh11.vatrasmoker.data.repository

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.oleh11.vatrasmoker.data.ble.BleManager
import com.oleh11.vatrasmoker.data.ble.DiscoveredGattInfo
import com.oleh11.vatrasmoker.data.model.CommandResult
import com.oleh11.vatrasmoker.data.model.ChangeMessage
import com.oleh11.vatrasmoker.data.model.ConnectionState
import com.oleh11.vatrasmoker.data.model.CookDataPoint
import com.oleh11.vatrasmoker.data.model.SmokerPowerMode
import com.oleh11.vatrasmoker.data.model.SmokerState
import com.oleh11.vatrasmoker.data.model.TemperatureUnit
import com.oleh11.vatrasmoker.data.simulator.MockSmokerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SmokerRepositoryImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : SmokerRepository {

    private val bleManager = BleManager(context, scope)
    private val simulatorEngine = MockSmokerEngine(scope)

    private val _isSimulatorMode = MutableStateFlow(false) // Default to real BLE scanning mode
    override val isSimulatorMode: StateFlow<Boolean> = _isSimulatorMode.asStateFlow()

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _showMeatProbes = MutableStateFlow(prefs.getBoolean("show_meat_probes", true))
    override val showMeatProbes: StateFlow<Boolean> = _showMeatProbes.asStateFlow()

    override fun setShowMeatProbes(enabled: Boolean) {
        _showMeatProbes.value = enabled
        prefs.edit().putBoolean("show_meat_probes", enabled).apply()
    }

    private val _liveNotification = MutableStateFlow(prefs.getBoolean("live_notification", false))
    override val liveNotification: StateFlow<Boolean> = _liveNotification.asStateFlow()

    override fun setLiveNotification(enabled: Boolean) {
        _liveNotification.value = enabled
        prefs.edit().putBoolean("live_notification", enabled).apply()
    }

    // The grill and simulator know nothing about the display unit, so it is applied to every update
    @Volatile private var temperatureUnit = TemperatureUnit.FAHRENHEIT

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _smokerState = MutableStateFlow(SmokerState())
    override val smokerState: StateFlow<SmokerState> = _smokerState.asStateFlow()

    override val discoveredDevices: StateFlow<List<BluetoothDevice>> = bleManager.discoveredDevices

    override val debugLogs: StateFlow<List<String>> = bleManager.debugLogs
    override val discoveredGattServices: StateFlow<List<DiscoveredGattInfo>> = bleManager.discoveredGattServices
    override val rawIncomingHex: StateFlow<List<String>> = bleManager.rawIncomingHex

    private val _cookHistory = MutableStateFlow<List<CookDataPoint>>(emptyList())
    override val cookHistory: StateFlow<List<CookDataPoint>> = _cookHistory.asStateFlow()

    init {
        // Collect BLE state
        scope.launch {
            bleManager.smokerState.collect { bleState ->
                if (!_isSimulatorMode.value) {
                    _smokerState.value = bleState.copy(temperatureUnit = temperatureUnit)
                }
            }
        }
        scope.launch {
            bleManager.connectionState.collect { connState ->
                if (!_isSimulatorMode.value) {
                    _connectionState.value = connState
                }
            }
        }

        // Collect Simulator state
        scope.launch {
            simulatorEngine.state.collect { simState ->
                if (_isSimulatorMode.value) {
                    _smokerState.value = simState.copy(temperatureUnit = temperatureUnit)
                }
            }
        }

        // Periodic temperature history recording
        scope.launch {
            while (isActive) {
                delay(5000)
                val live = _connectionState.value
                if (live !is ConnectionState.Connected && live !is ConnectionState.SimulatorActive) continue
                val current = _smokerState.value
                val dataPoint = CookDataPoint(
                    timestampMs = System.currentTimeMillis(),
                    grillTempF = current.currentChamberTempF,
                    probe1TempF = if (current.probe1.isConnected) current.probe1.currentTempF else 0,
                    probe2TempF = if (current.probe2.isConnected) current.probe2.currentTempF else 0,
                    probe3TempF = if (current.probe3.isConnected) current.probe3.currentTempF else 0,
                    probe4TempF = if (current.probe4.isConnected) current.probe4.currentTempF else 0
                )
                val currentList = _cookHistory.value
                _cookHistory.value = (currentList + dataPoint).takeLast(100)
            }
        }
    }

    override fun clearLogs() {
        bleManager.clearLogs()
    }

    override fun initiateBond(device: BluetoothDevice) {
        bleManager.initiateBond(device)
    }

    override fun sendRawHex(hexString: String) {
        bleManager.sendRawHex(hexString)
    }

    override fun startScan() {
        if (!_isSimulatorMode.value) {
            bleManager.startScan()
        }
    }

    override fun stopScan() {
        bleManager.stopScan()
    }

    override fun connectBle(device: BluetoothDevice) {
        _isSimulatorMode.value = false
        bleManager.connect(device)
    }

    override fun disconnect() {
        if (_isSimulatorMode.value) {
            _connectionState.value = ConnectionState.Disconnected
        } else {
            bleManager.disconnect()
        }
    }

    override fun setSimulatorMode(enabled: Boolean) {
        _isSimulatorMode.value = enabled
        if (enabled) {
            bleManager.disconnect()
            _connectionState.value = ConnectionState.SimulatorActive()
            _smokerState.value = simulatorEngine.state.value.copy(temperatureUnit = temperatureUnit)
            simulatorEngine.startSimulationLoop()
        } else {
            simulatorEngine.stopSimulation()
            _connectionState.value = ConnectionState.Disconnected
            _smokerState.value = SmokerState(temperatureUnit = temperatureUnit)
        }
    }

    // A channel, so a result is shown once and is kept until the screen can show it
    private val _changeMessages = Channel<String>(Channel.BUFFERED)
    override val changeMessages: Flow<String> = _changeMessages.receiveAsFlow()

    private fun isLinked() = _connectionState.value.let {
        it is ConnectionState.Connected || it is ConnectionState.SimulatorActive
    }

    /** Runs one change against the grill or simulator and reports how it went. */
    private fun change(description: String, simulate: () -> Unit, send: suspend () -> CommandResult) {
        if (!isLinked()) {
            _changeMessages.trySend("Not connected to the grill. Nothing was changed.")
            return
        }
        if (_isSimulatorMode.value) {
            simulate()
            _changeMessages.trySend(ChangeMessage.of(CommandResult.CONFIRMED, description, simulator = true))
            return
        }
        // Undispatched: commands update the screen and queue their write in the order they were tapped
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = send()
            _changeMessages.trySend(ChangeMessage.of(result, description))
        }
    }

    override fun setTargetTemperature(tempF: Int) {
        change(
            "set temperature ${temperatureUnit.format(tempF)}",
            simulate = { simulatorEngine.setTargetTemperature(tempF) },
            send = { bleManager.setTargetTemperature(tempF) }
        )
    }

    override fun setSmokeLevel(level: Int) {
        change(
            "smoke level $level",
            simulate = { simulatorEngine.setSmokeLevel(level) },
            send = { bleManager.setSmokeLevel(level) }
        )
    }

    override fun setProbeTarget(probeId: Int, targetF: Int) {
        if (_isSimulatorMode.value) {
            simulatorEngine.setProbeTarget(probeId, targetF)
        } else {
            val current = _smokerState.value
            _smokerState.value = when (probeId) {
                1 -> current.copy(probe1 = current.probe1.copy(targetTempF = targetF, isAlarmEnabled = true))
                2 -> current.copy(probe2 = current.probe2.copy(targetTempF = targetF, isAlarmEnabled = true))
                3 -> current.copy(probe3 = current.probe3.copy(targetTempF = targetF, isAlarmEnabled = true))
                4 -> current.copy(probe4 = current.probe4.copy(targetTempF = targetF, isAlarmEnabled = true))
                else -> current
            }
        }
    }

    override fun setPowerMode(mode: SmokerPowerMode) {
        val start = when (mode) {
            SmokerPowerMode.RUNNING, SmokerPowerMode.STARTUP -> true
            SmokerPowerMode.SHUTDOWN, SmokerPowerMode.OFF -> false
            else -> return
        }
        change(
            if (start) "start grill" else "shut down grill",
            simulate = { simulatorEngine.setPowerMode(mode) },
            send = { if (start) bleManager.powerStartup() else bleManager.powerShutdown() }
        )
    }

    override fun setTemperatureUnit(unit: TemperatureUnit) {
        temperatureUnit = unit
        _smokerState.value = _smokerState.value.copy(temperatureUnit = unit)
    }

    override fun provisionWifi(ssid: String, pass: String) {
        if (!isLinked()) {
            _changeMessages.trySend("Not connected to the grill. Wi-Fi details were not sent.")
            return
        }
        if (_isSimulatorMode.value) {
            _smokerState.value = _smokerState.value.copy(
                wifiConnected = true,
                wifiSsid = ssid
            )
            _changeMessages.trySend("Done (simulator): Wi-Fi details sent")
            return
        }
        scope.launch {
            _changeMessages.trySend(
                if (bleManager.provisionWifi(ssid, pass)) {
                    "\u2713 Wi-Fi details sent to the grill. Check the grill to see if it joined."
                } else {
                    "Wi-Fi details were not sent. Try again."
                }
            )
        }
    }

    override fun resetCookSession() {
        _cookHistory.value = emptyList()
        _smokerState.value = _smokerState.value.copy(
            cookStartTimeMs = System.currentTimeMillis()
        )
    }
}
