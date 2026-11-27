package com.oleh11.vatrasmoker.data.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WifiScanState(
    val networks: List<WifiNetwork> = emptyList(),
    val scanning: Boolean = false,
    /** Why the list cannot be filled, or null. */
    val problem: WifiScanProblem? = null
)

enum class WifiScanProblem { WIFI_OFF, LOCATION_OFF, NOT_ALLOWED }

/** Lists nearby networks for the grill Wi-Fi picker. Results stay in memory. */
class WifiScanner(context: Context) {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _state = MutableStateFlow(WifiScanState())
    val state: StateFlow<WifiScanState> = _state.asStateFlow()

    private var registered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = publish(scanning = false)
    }

    fun scan() {
        val wifi = wifiManager
        val problem = when {
            wifi == null || !wifi.isWifiEnabled -> WifiScanProblem.WIFI_OFF
            // Android returns an empty list while Location is switched off
            locationManager != null && !LocationManagerCompat.isLocationEnabled(locationManager) -> WifiScanProblem.LOCATION_OFF
            else -> null
        }
        if (problem != null) {
            _state.value = WifiScanState(problem = problem)
            return
        }
        if (!registered) {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            registered = true
        }
        _state.value = _state.value.copy(scanning = true, problem = null)
        @Suppress("DEPRECATION")
        val started = wifi!!.startScan()
        // Android limits how often apps may scan; show the last results when it says no
        if (!started) publish(scanning = false)
    }

    private fun publish(scanning: Boolean) {
        _state.value = try {
            @Suppress("DEPRECATION")
            val accessPoints = wifiManager?.scanResults.orEmpty().map {
                AccessPoint(it.SSID.orEmpty(), it.level, it.frequency, it.capabilities.orEmpty())
            }
            WifiScanState(networks = WifiNetworks.forGrill(accessPoints), scanning = scanning)
        } catch (e: SecurityException) {
            WifiScanState(problem = WifiScanProblem.NOT_ALLOWED)
        }
    }

    fun notAllowed() {
        _state.value = WifiScanState(problem = WifiScanProblem.NOT_ALLOWED)
    }

    fun stop() {
        if (registered) {
            appContext.unregisterReceiver(receiver)
            registered = false
        }
    }
}
