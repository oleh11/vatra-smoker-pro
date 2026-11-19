package com.oleh11.vatrasmoker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.oleh11.vatrasmoker.ui.navigation.AppNavigation
import com.oleh11.vatrasmoker.ui.theme.CabelasSmokerCompanionTheme
import com.oleh11.vatrasmoker.ui.theme.Charcoal900

class MainActivity : ComponentActivity() {

    /** Foreground service of type connectedDevice needs BLUETOOTH_CONNECT on Android 12+. */
    private fun startMonitoringIfAllowed() {
        val ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (ok) com.oleh11.vatrasmoker.service.CookMonitoringService.startService(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { startMonitoringIfAllowed() }

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                } else {
                    startMonitoringIfAllowed()
                }
            }

            CabelasSmokerCompanionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Charcoal900
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
