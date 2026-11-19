package com.oleh11.vatrasmoker.ui.screens.diagnostics

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oleh11.vatrasmoker.data.ble.DiscoveredGattInfo
import com.oleh11.vatrasmoker.data.model.ConnectionState
import com.oleh11.vatrasmoker.data.repository.SmokerRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BleDiagnosticsViewModel(
    private val repository: SmokerRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val debugLogs: StateFlow<List<String>> = repository.debugLogs
    val discoveredGattServices: StateFlow<List<DiscoveredGattInfo>> = repository.discoveredGattServices
    val rawIncomingHex: StateFlow<List<String>> = repository.rawIncomingHex
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = repository.discoveredDevices

    fun startScan() {
        repository.startScan()
    }

    fun stopScan() {
        repository.stopScan()
    }

    fun connectBle(device: BluetoothDevice) {
        repository.connectBle(device)
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun clearLogs() {
        repository.clearLogs()
    }

    fun initiateBond(device: BluetoothDevice) {
        repository.initiateBond(device)
    }

    fun sendRawHex(hex: String) {
        viewModelScope.launch {
            repository.sendRawHex(hex)
        }
    }

    class Factory(private val repository: SmokerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BleDiagnosticsViewModel(repository) as T
        }
    }
}
