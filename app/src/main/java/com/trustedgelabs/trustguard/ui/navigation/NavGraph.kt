package com.trustedgelabs.trustguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trustedgelabs.trustguard.ui.screens.applist.AppListScreen
import com.trustedgelabs.trustguard.ui.screens.battery.BatteryHealthScreen
import com.trustedgelabs.trustguard.ui.screens.blocking.DnsBlockingScreen
import com.trustedgelabs.trustguard.ui.screens.dashboard.DashboardScreen
import com.trustedgelabs.trustguard.ui.screens.detail.DetailScreen
import com.trustedgelabs.trustguard.ui.screens.adware.AdwareScreen
import com.trustedgelabs.trustguard.ui.screens.integrity.AppIntegrityScreen
import com.trustedgelabs.trustguard.ui.screens.network.NetworkMonitorScreen
import com.trustedgelabs.trustguard.ui.screens.optimization.OptimizationScreen
import com.trustedgelabs.trustguard.ui.screens.fakeidentity.FakeIdentityScreen
import com.trustedgelabs.trustguard.ui.screens.appsecurity.AppSecurityScreen
import com.trustedgelabs.trustguard.ui.screens.bloatware.BloatwareScreen
import com.trustedgelabs.trustguard.ui.screens.privacy.PrivacyScreen
import com.trustedgelabs.trustguard.ui.screens.firewall.FirewallScreen
import com.trustedgelabs.trustguard.ui.screens.packetsniffer.PacketSnifferScreen
import com.trustedgelabs.trustguard.ui.screens.vault.VaultScreen
import com.trustedgelabs.trustguard.ui.screens.premium.PremiumScreen
import com.trustedgelabs.trustguard.ui.screens.recovery.RecoveryScreen
import com.trustedgelabs.trustguard.ui.screens.settings.SettingsScreen
import com.trustedgelabs.trustguard.ui.screens.storage.StorageAnalyzerScreen
import com.trustedgelabs.trustguard.ui.screens.familyshield.FamilyShieldScreen
import com.trustedgelabs.trustguard.ui.screens.familyshield.ParentPanelScreen
import com.trustedgelabs.trustguard.ui.screens.virusscan.VirusScanScreen
import com.trustedgelabs.trustguard.ui.screens.wifi.WifiSecurityScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAppList = {
                    navController.navigate(Screen.AppList.route)
                },
                onNavigateToDetail = { packageName ->
                    navController.navigate(Screen.Detail.createRoute(packageName))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToBlocking = {
                    navController.navigate(Screen.DnsBlocking.route)
                },
                onNavigateToRecovery = {
                    navController.navigate(Screen.Recovery.route)
                },
                onNavigateToAdware = {
                    navController.navigate(Screen.Adware.route)
                },
                onNavigateToPremium = {
                    navController.navigate(Screen.Premium.route)
                },
                onNavigateToOptimization = {
                    navController.navigate(Screen.Optimization.route)
                },
                onNavigateToWifi = {
                    navController.navigate(Screen.WifiSecurity.route)
                },
                onNavigateToBattery = {
                    navController.navigate(Screen.BatteryHealth.route)
                },
                onNavigateToStorage = {
                    navController.navigate(Screen.StorageAnalyzer.route)
                },
                onNavigateToIntegrity = {
                    navController.navigate(Screen.AppIntegrity.route)
                },
                onNavigateToNetwork = {
                    navController.navigate(Screen.NetworkMonitor.route)
                },
                onNavigateToFamilyShield = {
                    navController.navigate(Screen.FamilyShield.route)
                },
                onNavigateToVirusScan = {
                    navController.navigate(Screen.VirusScan.route)
                },
                onNavigateToFirewall = {
                    navController.navigate(Screen.Firewall.route)
                },
                onNavigateToFakeIdentity = {
                    navController.navigate(Screen.FakeIdentity.route)
                },
                onNavigateToVault = {
                    navController.navigate(Screen.Vault.route)
                },
                onNavigateToPacketSniffer = {
                    navController.navigate(Screen.PacketSniffer.route)
                },
                onNavigateToBloatware = {
                    navController.navigate(Screen.Bloatware.route)
                },
                onNavigateToPrivacy = {
                    navController.navigate(Screen.Privacy.route)
                },
                onNavigateToAppSecurity = {
                    navController.navigate(Screen.AppSecurity.route)
                }
            )
        }

        composable(Screen.AppList.route) {
            AppListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { packageName ->
                    navController.navigate(Screen.Detail.createRoute(packageName))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DnsBlocking.route) {
            DnsBlockingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Recovery.route) {
            RecoveryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Adware.route) {
            AdwareScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Premium.route) {
            PremiumScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Optimization.route) {
            OptimizationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPremium = { }
            )
        }

        composable(Screen.WifiSecurity.route) {
            WifiSecurityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BatteryHealth.route) {
            BatteryHealthScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StorageAnalyzer.route) {
            StorageAnalyzerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppIntegrity.route) {
            AppIntegrityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NetworkMonitor.route) {
            NetworkMonitorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VirusScan.route) {
            VirusScanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FamilyShield.route) {
            FamilyShieldScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToParentPanel = { navController.navigate(Screen.ParentPanel.route) }
            )
        }

        composable(Screen.Firewall.route) {
            FirewallScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FakeIdentity.route) {
            FakeIdentityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Vault.route) {
            VaultScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PacketSniffer.route) {
            PacketSnifferScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Bloatware.route) {
            BloatwareScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppSecurity.route) {
            AppSecurityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ParentPanel.route) {
            ParentPanelScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
            DetailScreen(
                packageName = packageName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
