package com.oleh11.vatrasmoker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oleh11.vatrasmoker.data.model.ConnectionState
import com.oleh11.vatrasmoker.data.model.CookDataPoint
import com.oleh11.vatrasmoker.data.model.SmokerPowerMode
import com.oleh11.vatrasmoker.data.model.SmokerState
import com.oleh11.vatrasmoker.data.model.TemperatureUnit
import com.oleh11.vatrasmoker.data.repository.SmokerRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: SmokerRepository
) : ViewModel() {

    val smokerState: StateFlow<SmokerState> = repository.smokerState
    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val cookHistory: StateFlow<List<CookDataPoint>> = repository.cookHistory
    val isSimulatorMode: StateFlow<Boolean> = repository.isSimulatorMode
    val showMeatProbes: StateFlow<Boolean> = repository.showMeatProbes

    fun setTargetTemperature(tempF: Int) {
        viewModelScope.launch {
            repository.setTargetTemperature(tempF)
        }
    }

    fun setSmokeLevel(level: Int) {
        viewModelScope.launch {
            repository.setSmokeLevel(level)
        }
    }

    fun setProbeTarget(probeId: Int, targetF: Int) {
        viewModelScope.launch {
            repository.setProbeTarget(probeId, targetF)
        }
    }

    fun shutdownGrill() {
        viewModelScope.launch {
            repository.setPowerMode(SmokerPowerMode.SHUTDOWN)
        }
    }

    fun startupGrill() {
        viewModelScope.launch {
            repository.setPowerMode(SmokerPowerMode.STARTUP)
        }
    }

    fun setWifiNetwork(ssid: String, pass: String) {
        viewModelScope.launch {
            repository.provisionWifi(ssid, pass)
        }
    }

    fun setPowerMode(mode: SmokerPowerMode) {
        viewModelScope.launch {
            repository.setPowerMode(mode)
        }
    }

    fun resetCookSession() {
        viewModelScope.launch {
            repository.resetCookSession()
        }
    }

    class Factory(private val repository: SmokerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
