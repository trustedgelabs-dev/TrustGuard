package com.trustedgelabs.trustguard.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AppList : Screen("app_list")
    data object Settings : Screen("settings")
    data object DnsBlocking : Screen("dns_blocking")
    data object Recovery : Screen("recovery")
    data object Adware : Screen("adware")
    data object Premium : Screen("premium")
    data object Optimization : Screen("optimization")
    data object WifiSecurity : Screen("wifi_security")
    data object BatteryHealth : Screen("battery_health")
    data object StorageAnalyzer : Screen("storage_analyzer")
    data object AppIntegrity : Screen("app_integrity")
    data object NetworkMonitor : Screen("network_monitor")
    data object FamilyShield : Screen("family_shield")
    data object ParentPanel : Screen("parent_panel")
    data object VirusScan : Screen("virus_scan")
    data object Firewall : Screen("firewall")
    data object FakeIdentity : Screen("fake_identity")
    data object Vault : Screen("vault")
    data object PacketSniffer : Screen("packet_sniffer")
    data object Bloatware : Screen("bloatware")
    data object Privacy : Screen("privacy")
    data object AppSecurity : Screen("app_security")
    data object Detail : Screen("detail/{packageName}") {
        fun createRoute(packageName: String) = "detail/$packageName"
    }
}
