# TrustGuard - Comprehensive Application Documentation

**Developer:** TrustEdge Labs
**Package Name:** `com.trustedgelabs.trustguard`
**Version:** 3.0.0 (versionCode: 14)
**Platform:** Android
**Minimum SDK:** 24 (Android 7.0 Nougat)
**Target SDK:** 35 (Android 15)
**Compile SDK:** 35
**License:** GNU General Public License v3.0

---

## Table of Contents

1. [Overview](#1-overview)
2. [Technology Stack and Dependencies](#2-technology-stack-and-dependencies)
3. [Architecture](#3-architecture)
4. [Features and Screens](#4-features-and-screens)
5. [VPN and DNS Blocking System](#5-vpn-and-dns-blocking-system)
6. [Adware Detection Engine](#6-adware-detection-engine)
7. [Permission Classification and Risk Analysis](#7-permission-classification-and-risk-analysis)
8. [File Recovery System](#8-file-recovery-system)
9. [Optimization Engine](#9-optimization-engine)
10. [Security Measures](#10-security-measures)
11. [Android Permissions](#11-android-permissions)
12. [Localization](#12-localization)
13. [Theme and Design System](#13-theme-and-design-system)
14. [Build Configuration](#14-build-configuration)
15. [Resource Files](#15-resource-files)
16. [File Structure](#16-file-structure)

---

## 1. Overview

TrustGuard is a comprehensive privacy and security application for Android devices. It scans installed apps to analyze permission risks, blocks ads and trackers at the DNS level via a local on-device VPN, detects adware, and provides file recovery and device optimization tools.

TrustGuard is fully free and open source under the GNU GPL v3.0 license. There are no in-app purchases, subscriptions, ads, or remote servers. All processing happens on the user's device.

### Core Value Propositions

| Feature | Description |
|---------|-------------|
| Permission Analysis | Scans permissions of all installed apps and assigns risk scores |
| DNS Blocking (VPN) | Blocks ad and tracker domains at the DNS level via a local VPN tunnel |
| Adware Detection | Detects suspicious apps via SDK signatures and permission combinations |
| Wi-Fi Security Scan | Analyzes the security of the connected Wi-Fi network |
| Battery Health Analysis | Shows battery state, temperature, and top consumers |
| Storage Analysis | Categorizes storage usage breakdown |
| File Recovery | Recovers deleted files using 9 different strategies |
| Device Optimization | Cleans junk files, cache, and duplicates |
| App Integrity | Verifies install source (Play Store vs sideloaded) |
| Network Monitor | Per-app network traffic monitoring |
| Family Shield | Configurable per-profile parental restrictions |

---

## 2. Technology Stack and Dependencies

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.0.21 | Primary programming language |
| Jetpack Compose | BOM 2024.09.00 | 100% Compose-based UI |
| Material 3 | 1.3.1 | Material Design 3 components |
| Android Gradle Plugin | 8.9.0 | Build system |
| Java Compatibility | VERSION_11 | JVM target |

### AndroidX Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.core:core-ktx` | 1.15.0 | Android Core KTX extensions |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.7 | Lifecycle management |
| `androidx.activity:activity-compose` | 1.10.1 | Compose Activity integration |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | ViewModel Compose integration |
| `androidx.navigation:navigation-compose` | 2.8.8 | Inter-screen navigation |
| `androidx.compose.material:material-icons-extended` | BOM | Extended Material icons |

### Third-Party Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| `io.coil-kt:coil-compose` | 2.6.0 | Image loading (app icons) |
| `io.coil-kt:coil-video` | 2.6.0 | Video thumbnail loading |

### Test Libraries

| Library | Version |
|---------|---------|
| `junit:junit` | 4.13.2 |
| `androidx.test.ext:junit` | 1.2.1 |
| `androidx.test.espresso:espresso-core` | 3.6.1 |
| `androidx.compose.ui:ui-test-junit4` | BOM |

---

## 3. Architecture

### Layered MVVM Architecture

```
Presentation Layer (UI)
    Compose Screens + Material 3 Components
    ViewModels (reactive state via StateFlow)
        |
Use Cases
    ScanAppsUseCase, AnalyzePermissionsUseCase
        |
Repository Layer
    AppRepositoryImpl, BlocklistRepositoryImpl, BlockingStatsRepository
        |
Data Source Layer
    PackageManagerDataSource, WifiSecurityDataSource, BatteryDataSource,
    StorageDataSource, AppIntegrityDataSource, OptimizationDataSource,
    MediaStoreRecoveryDataSource, BlocklistDataSource
        |
Domain Layer
    AdwareDetector, PermissionClassifier
        |
Service Layer
    TrustGuardVpnService, NotificationAdDetector, VpnControlManager
        |
Android System APIs
    PackageManager, VpnService, WifiManager, BatteryManager,
    StorageStatsManager, UsageStatsManager, TrafficStats, MediaStore
```

### State Management

- **MutableStateFlow / StateFlow:** Reactive state management across all ViewModels
- **collectAsState():** Flow consumption in Compose UI
- **SharedPreferences:** Persistent local storage (VPN filter settings, user preferences)
- **@Volatile:** Thread-safe boolean flags (VPN loop)

### Navigation Structure

`NavGraph.kt` defines navigation across the application screens:

```kotlin
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AppList : Screen("app_list")
    data object Detail : Screen("detail/{packageName}")
    data object DnsBlocking : Screen("dns_blocking")
    data object Recovery : Screen("recovery")
    data object Adware : Screen("adware")
    data object Optimization : Screen("optimization")
    data object WifiSecurity : Screen("wifi_security")
    data object BatteryHealth : Screen("battery_health")
    data object StorageAnalyzer : Screen("storage_analyzer")
    data object AppIntegrity : Screen("app_integrity")
    data object NetworkMonitor : Screen("network_monitor")
    data object Settings : Screen("settings")
}
```

All screens are accessible to all users. There are no paywalls or feature gates.

---

## 4. Features and Screens

### 4.1 Dashboard

**File:** `ui/screens/dashboard/DashboardScreen.kt`
**ViewModel:** `DashboardViewModel.kt`

The main control center.

- Animated security score arc (0-100)
- Traffic light row (red/yellow/green risk counts)
- VPN quick action bar
- Security Tools: Permission Analysis, DNS Blocking, Adware Detection, Wi-Fi Security
- Optimization Tools: Battery, Storage, Cleanup, Recovery
- Advanced Tools: App Integrity, Network Monitor
- Top 3 risky apps list

**ViewModel responsibilities:**
- Automatic scan on app launch
- VPN state observation (StateFlow)
- Adware scanning
- Security score computation

### 4.2 App List

**File:** `ui/screens/applist/AppListScreen.kt`
**ViewModel:** `AppListViewModel.kt`

- Filterable list of installed apps
- Risk filter: All / High / Medium / Low
- Show/hide system apps toggle
- Search by app name
- Risk badge for each app

### 4.3 App Detail Screen

**File:** `ui/screens/detail/DetailScreen.kt`
**ViewModel:** `DetailViewModel.kt`

- App icon, name, and risk level badge
- Overall score and dangerous permission count
- Permission list (grouped, with risk indicators)
- Trusted app transparency information (for TrustEdge Labs apps)
- App size and install source
- Uninstall and open buttons

### 4.4 DNS Blocking Screen

**File:** `ui/screens/blocking/DnsBlockingScreen.kt`

- Total blocked count for today
- Total query count and blocking percentage
- Top blocked domains list
- VPN on/off toggle
- Blocklist management (enable/disable lists)

### 4.5 Wi-Fi Security Scan

**File:** `ui/screens/wifi/WifiSecurityScreen.kt`

- 4 state handling: scanning, location permission required, error, not connected
- Connected Wi-Fi SSID
- Security protocol (WPA3/WPA2/WEP/Open)
- Security level indicator (Safe/Warning/Danger)
- Signal strength, IP address, gateway, DNS info
- Security recommendations

**Data Source:** `WifiSecurityDataSource.kt`
- Network scan via `WifiManager`
- SecurityException and general Exception handling
- Returns null on failure (crash prevention)

### 4.6 Battery Health Analysis

**File:** `ui/screens/battery/BatteryHealthScreen.kt`

- Battery level circle and charging state
- Health status (Good/Overheating/Dead, etc.)
- Temperature and voltage info
- Top battery-consuming apps over the last 24h (with progress bars)
- Battery saving tips

**Data Source:** `BatteryDataSource.kt`
- Battery info via `BatteryManager` intent
- Per-app usage via `UsageStatsManager`

### 4.7 Storage Analysis

**File:** `ui/screens/storage/StorageAnalyzerScreen.kt`

- Donut chart for storage breakdown
- Usage progress bar
- Per-category breakdown (Photos, Videos, Audio, Documents, Apps)
- Free, used, and total space

**Data Source:** `StorageDataSource.kt`
- Real storage statistics via `StatFs`
- Size computation by media type
- App storage info via `StorageStatsManager`

### 4.8 Adware Detection Screen

**File:** `ui/screens/adware/AdwareScreen.kt`
**ViewModel:** `AdwareViewModel.kt`

- Suspicious apps list (with suspicion score)
- Reason cards: overlay ads, autostart, known SDK, aggressive background
- Risk indicator and uninstall button per app

### 4.9 File Recovery Screen

**File:** `ui/screens/recovery/RecoveryScreen.kt`
**ViewModel:** `RecoveryViewModel.kt`

- 3 scan modes: Quick / Deep / Root
- Scan progress indicator
- Recoverable file list (grouped by type: Photo/Video/Audio/Document)
- Recover selected files
- Default recovery directory: `Pictures/TrustGuard_Recovered`

### 4.10 Optimization Screen

**File:** `ui/screens/optimization/OptimizationScreen.kt`
**ViewModel:** `OptimizationViewModel.kt`

- Scan types: System Junk, Duplicate Photos, Large Files, App Cache, Email Cache
- Progress indicator
- Files grouped by category
- Quick deletion and space-saving estimate

### 4.11 App Integrity

**File:** `ui/screens/integrity/AppIntegrityScreen.kt`
**ViewModel:** `AppIntegrityViewModel.kt`

- Scans install sources of all apps
- Classifies as Play Store, Sideloaded, System, or Unknown
- Sideloaded apps shown first
- Install source badge

**Data Source:** `AppIntegrityDataSource.kt`
- Source detection via `PackageManager.getInstallSourceInfo`

### 4.12 Network Monitor

**File:** `ui/screens/network/NetworkMonitorScreen.kt`

- Total sent/received data cards
- Per-app data consumption list (with progress bars)
- Uses the `TrafficStats` API

### 4.13 Settings Screen

**File:** `ui/screens/settings/SettingsScreen.kt`

- App filter mode (All vs Selected)
- Excluded apps list
- About, Version, Privacy Policy
- Developer info (TrustEdge Labs)

---

## 5. VPN and DNS Blocking System

### 5.1 General Architecture

TrustGuard uses Android's `VpnService` API to implement a local on-device DNS filtering system. It does not route all internet traffic — only DNS queries are intercepted, allowing ad and tracker domains to be blocked without exposing user traffic to a remote server.

### 5.2 Service Implementation

**File:** `service/TrustGuardVpnService.kt`

```
User sends DNS query
    |
VPN interface intercepts query (UDP port 53)
    |
DNS Packet Parsing (DnsPacket.kt)
    |
Is the domain in the blocklist?
    |
  YES --> Return 0.0.0.0 response (DnsResponseBuilder.kt)
    |
  NO  --> Forward to upstream DNS resolver
```

**Technical Details:**
- Uses fake DNS server IPs: `198.18.0.1`, `198.18.0.2` (non-routable address range)
- Only routes these IPs through the VPN tunnel — does not capture all device traffic
- Dedicated packet processing thread: `TrustGuard-VPN-Loop`
- Thread-safe `isRunning` state via `@Volatile` flag
- Foreground service requirement (Android 14+: `startForeground()` must be called within 5 seconds)
- TrustGuard itself is excluded from its own VPN (prevents infinite loops)
- `protect(socket)` ensures DNS forwarding requests do not pass through the VPN

### 5.3 DNS Packet Processing

**Files:**
- `data/dns/DnsPacket.kt` — DNS query parsing
- `data/dns/DnsResponseBuilder.kt` — Block response construction
- `data/dns/IpPacketBuilder.kt` — IP/UDP packet building

**DNS Query Parsing:**
- Parses DNS packets from raw bytes
- Extracts transaction ID, flags, question count
- Recursively parses domain name labels (length-prefixed)
- Normalizes domains to lowercase
- Returns `null` on malformed packets
- Minimum packet size validation: 12 bytes

**Block Response:**
- Returns 0.0.0.0 IP address for A-type queries
- Copies transaction ID and flags from the original query
- Sets QR=1 (response), AA=1 (authoritative), RD=1 (recursion desired)
- TTL: 300 seconds
- Uses DNS pointer compression (0xC0 0x0C)

**IP Packet Construction:**
- Swaps source/destination IP addresses
- Computes correct IP header checksum (16-bit one's complement)
- Swaps UDP source/destination ports
- TTL: 64, Don't Fragment flag set
- IPv4 UDP checksum: 0 (optional for IPv4)

### 5.4 Blocklists

**File:** `data/datasource/BlocklistDataSource.kt`

3 built-in blocklists (in the assets folder):
1. **adware** — Ad domains
2. **trackers** — Tracker domains
3. **inapp_ads** — In-app advertising domains

**Domain Matching Logic:**
- Normalizes domains (lowercase, strips `www.` prefix)
- Checks the queried domain AND all parent domains
  - Example: `tracker.example.com` → `example.com` → `com`

### 5.5 Per-App Filtering

**File:** `service/AppFilterManager.kt`

- `FilterMode`: ALL_APPS (default) or SELECTED_APPS
- Users can toggle the VPN filter on a per-app basis
- Persistent storage via `SharedPreferences`
- `getInstalledUserApps()` returns the app list with exclusion state

### 5.6 VPN State Management

**File:** `service/VpnControlManager.kt`

```kotlin
object VpnControlManager {
    val isVpnActive: StateFlow<Boolean>           // VPN active/inactive state
    val blockingStats: StateFlow<BlockingStats>   // Blocking statistics

    fun startVpn(context)   // Start service (uses startForegroundService on Android 8+)
    fun stopVpn(context)    // Stop service
}
```

- Singleton pattern for global access
- Reactive state updates via StateFlow
- Notification update every 10 blocked queries

---

## 6. Adware Detection Engine

**File:** `domain/AdwareDetector.kt`

### Detection Methods and Scoring

| Detection | Score | Condition |
|-----------|-------|-----------|
| Overlay Ad Capability | 30 | SYSTEM_ALERT_WINDOW + INTERNET |
| Autostart | 20 | BOOT_COMPLETED + INTERNET + OVERLAY |
| Known Ad SDKs | 25x (max 50) | 27+ known SDK signatures |
| Aggressive Background | 15 | WAKE_LOCK + FOREGROUND_SERVICE + BATTERY_OPT + INTERNET + BOOT |
| App Install Capability | 20 | REQUEST_INSTALL_PACKAGES + INTERNET |
| Known Adware Packages | 25 | Package prefix match |
| Suspicious Permission Combo | 15 | 4+ suspicious permissions together |

### Known Adware SDK Signatures (27+)

```
com.startapp, com.airpush, com.leadbolt, com.appnext,
cn.jpush, com.igexin, com.pgl, com.apptracker,
com.kochava, com.appsflyer.internal, com.adcolony,
com.vungle, com.chartboost, com.inmobi, com.mopub,
com.tapjoy, com.ironsource, com.applovin, com.unity3d.ads,
com.facebook.ads, com.google.android.gms.ads.mediation,
com.bytedance.sdk.openadsdk, com.baidu.mobads,
com.qq.e.ads, com.tencent.gdt, com.smaato, com.ogury
```

### Known Adware Package Prefixes

```
com.cleanmaster, com.ksmobile, com.nqmobile, com.apus,
com.uc.browser, com.dianxinos, com.duapps, com.dolphin
```

### Suspicion Threshold

- `suspicionScore >= 40` → Suspicious
- `suspicionScore >= 70` → Highly suspicious

### Safety Notes

- Apps are NEVER labeled as "adware" or "malware" (legal risk)
- Uses neutral language: "suspicious advertising behavior detected"
- Trusted apps are skipped (`PermissionClassifier.isTrustedApp()`)
- System apps are skipped

---

## 7. Permission Classification and Risk Analysis

**File:** `domain/PermissionClassifier.kt`

### High-Risk Permissions (31 total, Score: 3)

```
CAMERA, RECORD_AUDIO, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION,
READ_CONTACTS, WRITE_CONTACTS, READ_CALL_LOG, WRITE_CALL_LOG,
READ_SMS, SEND_SMS, RECEIVE_SMS, READ_PHONE_STATE, READ_PHONE_NUMBERS,
CALL_PHONE, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE,
MANAGE_EXTERNAL_STORAGE, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO,
READ_MEDIA_AUDIO, BODY_SENSORS, ACTIVITY_RECOGNITION,
READ_CALENDAR, WRITE_CALENDAR, GET_ACCOUNTS,
ACCESS_BACKGROUND_LOCATION, PROCESS_OUTGOING_CALLS,
ANSWER_PHONE_CALLS, ADD_VOICEMAIL, USE_SIP, ACCEPT_HANDOVER
```

### Medium-Risk Permissions (33 total, Score: 1)

```
INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE,
BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_CONNECT, BLUETOOTH_SCAN,
NEARBY_WIFI_DEVICES, VIBRATE, WAKE_LOCK, FOREGROUND_SERVICE,
RECEIVE_BOOT_COMPLETED, SYSTEM_ALERT_WINDOW, INSTALL_SHORTCUT,
SET_ALARM, REQUEST_INSTALL_PACKAGES, REQUEST_DELETE_PACKAGES,
PACKAGE_USAGE_STATS, BIND_ACCESSIBILITY_SERVICE,
BIND_DEVICE_ADMIN, CHANGE_WIFI_STATE, CHANGE_NETWORK_STATE,
NFC, USE_BIOMETRIC, USE_FINGERPRINT, READ_SYNC_SETTINGS,
WRITE_SYNC_SETTINGS, BIND_VPN_SERVICE, POST_NOTIFICATIONS,
SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM,
REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, QUERY_ALL_PACKAGES
```

### Trusted App Whitelist

Used internally by TrustGuard to mark its own package as trusted. The whitelist is hard-coded in `PermissionClassifier`.

Trusted apps always receive `RiskLevel.TRUSTED` and a score of 0.

### App Risk Classification

```
If any HIGH-risk permission is present  → RiskLevel.HIGH
Else if a MEDIUM-risk permission is present → RiskLevel.MEDIUM
Else                                   → RiskLevel.LOW
```

### Security Score Calculation (Dashboard)

```kotlin
fun calculateSecurityScore(apps: List<AppInfo>): Int {
    val totalPossibleRisk = apps.size * 3
    var actualRisk = 0
    for (app in apps) {
        actualRisk += when (app.riskLevel) {
            RiskLevel.HIGH -> 3
            RiskLevel.MEDIUM -> 1
            RiskLevel.LOW -> 0
            RiskLevel.TRUSTED -> 0
        }
    }
    val riskRatio = actualRisk.toFloat() / totalPossibleRisk.toFloat()
    return ((1f - riskRatio) * 100).toInt().coerceIn(0, 100)
}
```

---

## 8. File Recovery System

**File:** `data/datasource/MediaStoreRecoveryDataSource.kt`

### 9 Recovery Strategies

| # | Strategy | Description | Android Version |
|---|----------|-------------|-----------------|
| 1 | MediaStore IS_TRASHED | Files in trash bin | Android 11+ |
| 2 | File System Trash Dirs | Physical trash folders | All |
| 3 | MediaStore Path-Based Queries | Files at deleted paths | All |
| 4 | .trashed- / .pending- Prefixed | Files marked for deletion | All |
| 5 | Thumbnail Hunting | Thumbnails of deleted photos | All |
| 6 | .nomedia Hidden Dirs | Files hidden from gallery | All |
| 7 | App Cache Media | WhatsApp, Telegram, Signal, Instagram, etc. | All |
| 8 | Messaging App Media | Media in messaging apps | All |
| 9 | Root Deep Scan | Root-access filesystem scan | Root required |

### Scan Modes

- **Quick (QUICK):** Strategies 1-4
- **Deep (DEEP):** Strategies 1-8
- **Root (ROOT):** Strategies 1-9 (requires root access)

### Recovery Methods

- MediaStore update (IS_TRASHED=0)
- File system copy
- Root copy (via `su` command)
- Default destination: `Pictures/TrustGuard_Recovered`

### Supported File Types

- **Photo:** JPEG, PNG, GIF, BMP, WebP
- **Video:** MP4, AVI, MKV, MOV, 3GP
- **Audio:** MP3, WAV, OGG, FLAC, AAC
- **Document:** PDF, DOC, DOCX, TXT, XLS

---

## 9. Optimization Engine

**File:** `data/datasource/OptimizationDataSource.kt`

### Scan Categories

| Category | Description |
|----------|-------------|
| SYSTEM_CACHE | System cache files |
| APP_CACHE | Per-app cache |
| TEMP_FILES | Temporary files |
| APK_FILES | Downloaded APK files |
| THUMBNAILS | Thumbnail cache |
| EMPTY_FOLDERS | Empty folders |
| LARGE_FILE | Files larger than 100 MB |
| DUPLICATE_PHOTO | MD5-based duplicate photo detection |
| EMAIL_CACHE | Email app caches (Gmail, Outlook, Yahoo Mail) |

### Duplicate Photo Detection

- Computes MD5 hash from first 8KB + last 8KB + file size
- Groups files with the same hash
- Uses partial hashing (rather than full file hash) for performance

### Progress Feedback

- `onScanProgress` callback for real-time progress updates

---

## 10. Security Measures

### 10.1 Exception Handling Patterns

**Silent Catch (non-critical):**
```kotlin
try { builder.addDisallowedApplication(excluded) }
catch (_: Exception) {}
```

**Logged Catch (critical):**
```kotlin
catch (e: Exception) {
    Log.e("TrustGuardVPN", "Failed to establish VPN", e)
}
```

**Fallback Catch:**
```kotlin
val appName = try {
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
} catch (_: Exception) { packageName }
```

All new screens (Wi-Fi, Battery, Storage, Integrity, Network) are wrapped in try-catch. No screen produces a direct crash; users see plain-language explanations on failure.

### 10.2 Concurrency Safety

| Pattern | Usage | File |
|---------|-------|------|
| `@Volatile` | Thread-safe boolean flags | TrustGuardVpnService.kt |
| `synchronized` | Shared resource protection | VPN egress write |
| `MutableStateFlow` | Thread-safe observable state | VpnControlManager.kt |
| `SupervisorJob + Dispatchers.IO` | Coroutine lifecycle | TrustGuardVpnService.kt |
| `serviceScope.cancel()` | Clean resource release | onDestroy() |

### 10.3 Data Validation

**DNS Packet Validation:**
- Minimum packet size: 12 bytes
- Question count check: >= 1
- Offset bounds check: `offset + 4 > data.size`
- Label length check: `offset + labelLength + 1 > data.size`

**IP Packet Header Validation:**
- IP version check: IPv4 only (version == 4)
- Protocol check: UDP only (protocol == 17)
- Port check: DNS only (destPort == 53)

**Input Sanitization:**
- Domain normalization: `lowercase().removePrefix("www.")`
- SharedPreferences value validation: `valueOf()` with try-catch
- JSON parsing: `JSONObject` with try-catch and empty-list fallback

### 10.4 Data Protection

**backup_rules.xml:**
- Cloud backup content rules (empty — backup disabled)

**data_extraction_rules.xml:**
- Cloud backup exclusions: root domain excluded
- Device transfer exclusions: root domain excluded
- Protects sensitive app data from cloud sync

### 10.5 R8/ProGuard Rules

- Preserves all Compose classes
- Preserves Material Icons Extended classes
- Preserves data models and the VPN Service implementation
- Preserves Kotlin Coroutines
- Strips log output in release builds (`Log.v`, `Log.d`, `Log.i`)
- Preserves annotation attributes and inner classes

---

## 11. Android Permissions

### App / Package Scanning

| Permission | Purpose | Risk Level |
|------------|---------|------------|
| `QUERY_ALL_PACKAGES` | Required to scan all installed apps on Android 11+ | Special |

### Wi-Fi and Network

| Permission | Purpose |
|------------|---------|
| `ACCESS_WIFI_STATE` | Read Wi-Fi connection state |
| `ACCESS_NETWORK_STATE` | Read network connection state |
| `ACCESS_FINE_LOCATION` | Read Wi-Fi SSID (Android 8.1+) |
| `ACCESS_COARSE_LOCATION` | Wi-Fi SSID fallback |
| `INTERNET` | Network access for upstream DNS query forwarding |

### Foreground Services

| Permission | Purpose |
|------------|---------|
| `FOREGROUND_SERVICE` | Foreground notification for the VPN service |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ special-use type |
| `POST_NOTIFICATIONS` | Send notifications to the user |

### App Activity Monitoring

| Permission | Purpose |
|------------|---------|
| `PACKAGE_USAGE_STATS` | Monitor per-app activity (protected) |

### File Recovery

| Permission | Purpose | Android Version |
|------------|---------|-----------------|
| `MANAGE_EXTERNAL_STORAGE` | Broad file access | Android 11+ |
| `READ_MEDIA_IMAGES` | Access image files | Android 13+ |
| `READ_MEDIA_VIDEO` | Access video files | Android 13+ |
| `READ_MEDIA_AUDIO` | Access audio files | Android 13+ |
| `READ_EXTERNAL_STORAGE` | Legacy storage access | maxSdkVersion=32 |

### Manifest Components

| Component | Type | Permission | Properties |
|-----------|------|------------|------------|
| `MainActivity` | Activity | - | exported=true, portrait, LAUNCHER |
| `TrustGuardVpnService` | Service | BIND_VPN_SERVICE | exported=false, specialUse, always-on supported |
| `NotificationAdDetector` | Service | BIND_NOTIFICATION_LISTENER_SERVICE | exported=false |

---

## 12. Localization

### Supported Languages

| Language | Folder | String Count |
|----------|--------|--------------|
| English (default) | `values/strings.xml` | ~423 |
| Turkish | `values-tr/strings.xml` | ~423 (full parity) |

### String Categories

The application uses categorized string resources covering:

- App identifiers (app_name, company_name, by_company)
- Dashboard sections
- Risk traffic light (high_risk, medium_risk, safe, trusted)
- App list filtering
- App card labels
- Detail screen
- Settings
- DNS blocking
- Per-app filtering
- Notification ad detection
- File recovery (modes, sources, scan stats)
- Adware detection
- Optimization (junk categories, duplicates, large files, app cache, email cache)
- Trusted app transparency
- Per-app activity monitoring
- Wi-Fi security
- Battery analysis
- Storage / Integrity / Network / Family Shield

### Locale Management

- `LocaleManager` class handles language switching
- Applied via `attachBaseContext` in `MainActivity` and `TrustGuardApp`

---

## 13. Theme and Design System

### Color Palette

**Primary Colors:**
| Color | Hex | Usage |
|-------|-----|-------|
| TrustGreen | #00E676 | Primary accent, safe state |
| TrustTeal | #00BFA5 | Secondary accent |

**Risk Traffic Light:**
| Color | Hex | Usage |
|-------|-----|-------|
| RiskRed | #FF1744 | High risk |
| RiskYellow | #FFD600 | Medium risk |
| RiskGreen | #00E676 | Low risk |
| TrustedBlue | #448AFF | Trusted |

**Dark Theme:**
| Color | Hex | Usage |
|-------|-----|-------|
| DarkBackground | #0A0E14 | Main background |
| DarkSurface | #131A24 | Surface |
| DarkCard | #1E2A3A | Card background |

**Feature Colors:**
| Color | Usage |
|-------|-------|
| SecurityBlue | Security tools |
| WifiPurple | Wi-Fi features |
| BatteryGreen | Battery features |
| StorageCyan | Storage features |
| IntegrityIndigo | Integrity features |
| NetworkTeal | Network features |
| OptimizationOrange | Optimization features |

**Text Colors:**
| Color | Hex | Usage |
|-------|-----|-------|
| TextPrimary | #E8EAED | Primary text |
| TextSecondary | #9AA0A6 | Secondary text |

### Material 3 Integration

- 100% dark theme
- Per-feature accent colors
- `Theme.TrustGuard`: Material NoActionBar theme

---

## 14. Build Configuration

### build.gradle.kts (App)

```
compileSdk = 35
minSdk = 24
targetSdk = 35
versionCode = 14
versionName = "3.0.0"
applicationId = "com.trustedgelabs.trustguard"

// Release build
isMinifyEnabled = true    // R8 shrinking enabled
isShrinkResources = true  // Resource shrinking enabled
proguardFiles = [proguard-android-optimize.txt, proguard-rules.pro]

// Java/Kotlin
javaVersion = VERSION_11
jvmTarget = "11"
kotlinCompilerExtensionVersion = Kotlin 2.0.21
```

### gradle.properties

```
JVM Arguments: -Xmx2048m
File Encoding: UTF-8
AndroidX: Enabled
Kotlin Code Style: Official
Non-transitive R Class: Enabled
```

### Signing Configuration (Release)

- Keystore: `release-key.jks` (not committed; see `local.properties.example`)
- Key alias: `trustguard`
- Credentials are read from `local.properties` (gitignored)

---

## 15. Resource Files

### Drawables (drawable/)

| File | Description |
|------|-------------|
| `ic_launcher_background.xml` | Gradient background (dark navy) + tech pattern |
| `ic_launcher_foreground.xml` | Shield icon (green) |
| `ic_shield.xml` | Alternative shield variant |

### Adaptive Icons (mipmap-anydpi-v26/)

| File | Description |
|------|-------------|
| `ic_launcher.xml` | Adaptive icon definition |
| `ic_launcher_round.xml` | Round variant |

### XML Configurations (xml/)

| File | Description |
|------|-------------|
| `backup_rules.xml` | Cloud backup disabled |
| `data_extraction_rules.xml` | Data extraction rules (root domain excluded) |

---

## 16. File Structure

```
app/src/main/java/com/trustedgelabs/trustguard/
|
├── TrustGuardApp.kt                    # Application class
├── MainActivity.kt                     # Main Activity
|
├── billing/
│   └── BillingManager.kt               # Stub - all features are free
|
├── data/
│   ├── datasource/
│   │   ├── PackageManagerDataSource.kt
│   │   ├── WifiSecurityDataSource.kt
│   │   ├── BatteryDataSource.kt
│   │   ├── StorageDataSource.kt
│   │   ├── AppIntegrityDataSource.kt
│   │   ├── BlocklistDataSource.kt
│   │   ├── OptimizationDataSource.kt
│   │   └── MediaStoreRecoveryDataSource.kt
│   │
│   ├── dns/
│   │   ├── DnsPacket.kt
│   │   ├── DnsResponseBuilder.kt
│   │   └── IpPacketBuilder.kt
│   │
│   ├── model/
│   │   ├── AppInfo.kt
│   │   ├── PermissionInfo.kt
│   │   ├── RiskLevel.kt
│   │   ├── BatteryInfo.kt
│   │   ├── StorageInfo.kt
│   │   ├── WifiSecurityInfo.kt
│   │   ├── AppIntegrityInfo.kt
│   │   ├── BlockingStats.kt
│   │   ├── BlocklistInfo.kt
│   │   ├── RecoverableFile.kt
│   │   ├── CleanableFile.kt
│   │   └── JunkScanResult.kt
│   │
│   └── repository/
│       ├── AppRepositoryImpl.kt
│       ├── BlocklistRepositoryImpl.kt
│       └── BlockingStatsRepository.kt
|
├── domain/
│   ├── AdwareDetector.kt
│   ├── PermissionClassifier.kt
│   ├── TrustedAppTransparency.kt
│   └── usecase/
│       ├── ScanAppsUseCase.kt
│       └── AnalyzePermissionsUseCase.kt
|
├── service/
│   ├── TrustGuardVpnService.kt         # VPN service (DNS blocking)
│   ├── VpnControlManager.kt            # VPN state management
│   ├── VpnNotificationManager.kt       # VPN notification
│   ├── AppFilterManager.kt             # Per-app filtering
│   └── NotificationAdDetector.kt       # Notification ad detection
|
├── ui/
│   ├── components/
│   │   ├── SecurityScoreArc.kt
│   │   ├── TrafficLightRow.kt
│   │   ├── ScanProgressIndicator.kt
│   │   ├── AppCard.kt
│   │   ├── RiskBadge.kt
│   │   ├── ToolCard.kt
│   │   ├── VpnStatusCard.kt
│   │   └── ProStatusBar.kt
│   │
│   ├── navigation/
│   │   ├── Screen.kt
│   │   └── NavGraph.kt
│   │
│   ├── screens/
│   │   ├── dashboard/
│   │   ├── applist/
│   │   ├── detail/
│   │   ├── blocking/
│   │   ├── recovery/
│   │   ├── adware/
│   │   ├── optimization/
│   │   ├── wifi/
│   │   ├── battery/
│   │   ├── storage/
│   │   ├── integrity/
│   │   ├── network/
│   │   └── settings/
│   │
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
|
└── util/
    ├── RootDetector.kt                 # Root detection + command execution
    ├── PermissionDescriptions.kt       # Permission descriptions (EN/TR)
    ├── AppIconLoader.kt                # App icon Compose converter
    └── LocaleManager.kt                # Language management
```

---

## Document Information

- **Application Version:** 3.0.0 (versionCode 14)
- **License:** GNU General Public License v3.0
- **Developer:** TrustEdge Labs
- **Repository:** https://github.com/trustedgelabs-dev/TrustGuard
