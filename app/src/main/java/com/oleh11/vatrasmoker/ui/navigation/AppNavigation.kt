package com.oleh11.vatrasmoker.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oleh11.vatrasmoker.SmokerApp
import com.oleh11.vatrasmoker.data.wifi.WifiProvisioner
import com.oleh11.vatrasmoker.ui.screens.cook.CookPlannerScreen
import com.oleh11.vatrasmoker.ui.screens.cook.CookPlannerViewModel
import com.oleh11.vatrasmoker.ui.screens.dashboard.DashboardScreen
import com.oleh11.vatrasmoker.ui.screens.dashboard.DashboardViewModel
import com.oleh11.vatrasmoker.ui.screens.scan.DeviceScanScreen
import com.oleh11.vatrasmoker.ui.screens.scan.DeviceScanViewModel
import com.oleh11.vatrasmoker.ui.screens.settings.SettingsScreen
import com.oleh11.vatrasmoker.ui.screens.settings.SettingsViewModel
import com.oleh11.vatrasmoker.ui.screens.wifi.WifiSetupScreen
import com.oleh11.vatrasmoker.ui.screens.wifi.WifiSetupViewModel
import com.oleh11.vatrasmoker.ui.theme.Charcoal800
import com.oleh11.vatrasmoker.ui.theme.Charcoal900
import com.oleh11.vatrasmoker.ui.theme.FlameOrange
import com.oleh11.vatrasmoker.ui.theme.TextGray
import com.oleh11.vatrasmoker.ui.theme.TextWhite

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val repository = (context.applicationContext as SmokerApp).repository
    val provisioner = WifiProvisioner(context)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Charcoal800,
                contentColor = TextWhite
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(text = screen.title) },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextWhite,
                            selectedTextColor = FlameOrange,
                            indicatorColor = FlameOrange,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        )
                    )
                }
            }
        },
        containerColor = Charcoal900,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(repository))
                DashboardScreen(
                    viewModel = vm,
                    onNavigateToScan = {
                        navController.navigate(Screen.DeviceScan.route)
                    }
                )
            }

            composable(Screen.CookPlanner.route) {
                val vm: CookPlannerViewModel = viewModel(factory = CookPlannerViewModel.Factory(repository))
                CookPlannerScreen(
                    viewModel = vm,
                    onPresetAppliedNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.DeviceScan.route) {
                val vm: DeviceScanViewModel = viewModel(factory = DeviceScanViewModel.Factory(repository))
                DeviceScanScreen(
                    viewModel = vm,
                    onConnectedNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.WifiSetup.route) {
                val vm: WifiSetupViewModel = viewModel(
                    factory = WifiSetupViewModel.Factory(repository, provisioner)
                )
                WifiSetupScreen(viewModel = vm)
            }

            composable(Screen.BleDiagnostics.route) {
                val vm: com.oleh11.vatrasmoker.ui.screens.diagnostics.BleDiagnosticsViewModel = viewModel(
                    factory = com.oleh11.vatrasmoker.ui.screens.diagnostics.BleDiagnosticsViewModel.Factory(repository)
                )
                com.oleh11.vatrasmoker.ui.screens.diagnostics.BleDiagnosticsScreen(
                    viewModel = vm,
                    onNavigateBack = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(repository, context)
                )
                SettingsScreen(viewModel = vm)
            }
        }
    }
}
