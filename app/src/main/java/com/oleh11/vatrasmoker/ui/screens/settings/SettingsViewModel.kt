package com.oleh11.vatrasmoker.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oleh11.vatrasmoker.data.model.SmokerState
import com.oleh11.vatrasmoker.data.model.TemperatureUnit
import com.oleh11.vatrasmoker.data.repository.SmokerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SmokerRepository,
    private val context: Context
) : ViewModel() {

    val smokerState: StateFlow<SmokerState> = repository.smokerState
    val isSimulatorMode: StateFlow<Boolean> = repository.isSimulatorMode
    val showMeatProbes: StateFlow<Boolean> = repository.showMeatProbes

    fun setShowMeatProbes(enabled: Boolean) = repository.setShowMeatProbes(enabled)

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            repository.setTemperatureUnit(unit)
        }
    }

    fun setSimulatorMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSimulatorMode(enabled)
        }
    }

    class Factory(
        private val repository: SmokerRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, context) as T
        }
    }
}
