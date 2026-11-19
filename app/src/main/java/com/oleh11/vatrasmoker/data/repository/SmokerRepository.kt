package com.oleh11.vatrasmoker.data.repository

import android.bluetooth.BluetoothDevice
import com.oleh11.vatrasmoker.data.ble.DiscoveredGattInfo
import com.oleh11.vatrasmoker.data.model.ConnectionState
import com.oleh11.vatrasmoker.data.model.CookDataPoint
import com.oleh11.vatrasmoker.data.model.MeatProbe
import com.oleh11.vatrasmoker.data.model.SmokerPowerMode
import com.oleh11.vatrasmoker.data.model.SmokerState
import com.oleh11.vatrasmoker.data.model.TemperatureUnit
import kotlinx.coroutines.flow.StateFlow

interface SmokerRepository {
    val connectionState: StateFlow<ConnectionState>
    val smokerState: StateFlow<SmokerState>
    val discoveredDevices: StateFlow<List<BluetoothDevice>>
    val cookHistory: StateFlow<List<CookDataPoint>>
    val isSimulatorMode: StateFlow<Boolean>
    val showMeatProbes: StateFlow<Boolean>
    fun setShowMeatProbes(enabled: Boolean)

    // Diagnostics & Debugging
    val debugLogs: StateFlow<List<String>>
    val discoveredGattServices: StateFlow<List<DiscoveredGattInfo>>
    val rawIncomingHex: StateFlow<List<String>>
    fun clearLogs()
    fun initiateBond(device: BluetoothDevice)
    fun sendRawHex(hexString: String)

    fun startScan()
    fun stopScan()
    fun connectBle(device: BluetoothDevice)
    fun disconnect()
    fun setSimulatorMode(enabled: Boolean)

    fun setTargetTemperature(tempF: Int)
    fun setSmokeLevel(level: Int)
    fun setProbeTarget(probeId: Int, targetF: Int)
    fun setPowerMode(mode: SmokerPowerMode)
    fun setTemperatureUnit(unit: TemperatureUnit)
    fun provisionWifi(ssid: String, pass: String)
    fun resetCookSession()
}
