package com.trustedgelabs.trustguard.domain

import java.util.Locale

/**
 * Provides transparency information for TrustEdge Labs apps.
 *
 * For each trusted app, explains:
 * - Why each permission is needed
 * - What network connections are made and why
 * - What data is collected/stored
 * - Privacy guarantee
 */
object TrustedAppTransparency {

    data class AppTransparencyInfo(
        val developerName: String,
        val privacyGuarantee: String,
        val permissionExplanations: Map<String, String>,
        val networkConnections: List<NetworkConnection>,
        val dataPolicy: String
    )

    data class NetworkConnection(
        val host: String,
        val purpose: String,
        val dataTransferred: String
    )

    /**
     * Check if transparency info is available for this package.
     */
    fun hasTransparencyInfo(packageName: String): Boolean {
        return getTransparencyInfo(packageName) != null
    }

    /**
     * Get transparency info for a trusted app.
     */
    fun getTransparencyInfo(packageName: String): AppTransparencyInfo? {
        val isTr = Locale.getDefault().language == "tr"

        return when {
            packageName == "com.trustedgelabs.trustguard" -> getTrustGuardInfo(isTr)
            packageName == "com.trustedgelabs.ghostnotify" ||
            packageName == "com.cihan.ghostnotify" -> getGhostNotifyInfo(isTr)
            packageName == "com.trustedgelabs.ibans" ||
            packageName == "com.cihan.ibans" -> getIbansAppInfo(isTr)
            // Generic TrustEdge Labs app
            packageName.startsWith("com.trustedgelabs.") ||
            packageName.startsWith("com.cihan.") -> getGenericTrustedInfo(isTr, packageName)
            else -> null
        }
    }

    // ══════════════════════════════════════════════════════════════
    // TrustGuard
    // ══════════════════════════════════════════════════════════════

    private fun getTrustGuardInfo(isTr: Boolean): AppTransparencyInfo {
        return if (isTr) AppTransparencyInfo(
            developerName = "TrustEdge Labs",
            privacyGuarantee = "TrustGuard, TrustEdge Labs tarafından geliştirilmiştir. Verileriniz hiçbir zaman dış sunuculara gönderilmez. Tüm analizler tamamen cihazınızda gerçekleşir. İnternet bağlantısı yalnızca DNS engelleme için kullanılır.",
            permissionExplanations = mapOf(
                "android.permission.MANAGE_EXTERNAL_STORAGE" to "Silinen dosyaları taramak ve kurtarmak için gereklidir. Dosyalarınız asla dışarı aktarılmaz.",
                "android.permission.READ_MEDIA_IMAGES" to "Silinen fotoğrafları bulup kurtarmak için kullanılır.",
                "android.permission.READ_MEDIA_VIDEO" to "Silinen videoları bulup kurtarmak için kullanılır.",
                "android.permission.READ_MEDIA_AUDIO" to "Silinen ses dosyalarını bulup kurtarmak için kullanılır.",
                "android.permission.INTERNET" to "Yalnızca DNS engelleme (reklam/takipçi engelleme) için kullanılır. Hiçbir kişisel veri dışarı gönderilmez.",
                "android.permission.FOREGROUND_SERVICE" to "VPN tabanlı reklam engelleme servisini arka planda çalıştırmak için gereklidir.",
                "android.permission.POST_NOTIFICATIONS" to "VPN koruma durumunu ve engelleme istatistiklerini bildirim olarak göstermek için kullanılır.",
                "android.permission.QUERY_ALL_PACKAGES" to "Yüklü uygulamaların izinlerini analiz edebilmek için gereklidir. Uygulama listeniz asla dışarı aktarılmaz.",
                "android.permission.RECEIVE_BOOT_COMPLETED" to "Cihaz yeniden başladığında VPN korumasını otomatik devam ettirmek için kullanılır.",
                "android.permission.READ_EXTERNAL_STORAGE" to "Android 12 ve altı cihazlarda dosya kurtarma için gereklidir."
            ),
            networkConnections = listOf(
                NetworkConnection(
                    host = "1.1.1.1 (Cloudflare DNS)",
                    purpose = "Engellenmemiş DNS sorgularını yönlendirme",
                    dataTransferred = "Yalnızca DNS sorguları (alan adı çözümleme)"
                )
            ),
            dataPolicy = "Tüm veriler cihazınızda kalır. Uygulama izin analizi, dosya tarama sonuçları ve engelleme istatistikleri yalnızca cihazınızın yerel depolama alanında (SharedPreferences) saklanır. Hiçbir veri uzak sunuculara gönderilmez."
        ) else AppTransparencyInfo(
            developerName = "TrustEdge Labs",
            privacyGuarantee = "TrustGuard is developed by TrustEdge Labs. Your data is never sent to external servers. All analysis is performed entirely on your device. Internet connection is only used for DNS blocking.",
            permissionExplanations = mapOf(
                "android.permission.MANAGE_EXTERNAL_STORAGE" to "Required to scan and recover deleted files. Your files are never exported.",
                "android.permission.READ_MEDIA_IMAGES" to "Used to find and recover deleted photos.",
                "android.permission.READ_MEDIA_VIDEO" to "Used to find and recover deleted videos.",
                "android.permission.READ_MEDIA_AUDIO" to "Used to find and recover deleted audio files.",
                "android.permission.INTERNET" to "Only used for DNS blocking (ad/tracker blocking). No personal data is ever sent externally.",
                "android.permission.FOREGROUND_SERVICE" to "Required to run the VPN-based ad blocking service in the background.",
                "android.permission.POST_NOTIFICATIONS" to "Used to display VPN protection status and blocking statistics as notifications.",
                "android.permission.QUERY_ALL_PACKAGES" to "Required to analyze permissions of installed apps. Your app list is never exported.",
                "android.permission.RECEIVE_BOOT_COMPLETED" to "Used to automatically resume VPN protection when the device restarts.",
                "android.permission.READ_EXTERNAL_STORAGE" to "Required for file recovery on Android 12 and below devices."
            ),
            networkConnections = listOf(
                NetworkConnection(
                    host = "1.1.1.1 (Cloudflare DNS)",
                    purpose = "Forward non-blocked DNS queries",
                    dataTransferred = "DNS queries only (domain name resolution)"
                )
            ),
            dataPolicy = "All data stays on your device. Permission analysis, file scan results, and blocking statistics are stored only in your device's local storage (SharedPreferences). No data is sent to remote servers."
        )
    }

    // ══════════════════════════════════════════════════════════════
    // GhostNotify
    // ══════════════════════════════════════════════════════════════

    private fun getGhostNotifyInfo(isTr: Boolean): AppTransparencyInfo {
        return if (isTr) AppTransparencyInfo(
            developerName = "TrustEdge Labs",
            privacyGuarantee = "GhostNotify, TrustEdge Labs tarafından geliştirilmiştir. Bildirim verileriniz asla dış sunuculara gönderilmez. Tüm işlemler tamamen cihazınızda gerçekleşir.",
            permissionExplanations = mapOf(
                "android.permission.FOREGROUND_SERVICE" to "Bildirim izleme servisini arka planda çalıştırmak için gereklidir.",
                "android.permission.RECEIVE_BOOT_COMPLETED" to "Cihaz yeniden başladığında bildirim izlemeyi otomatik devam ettirmek için kullanılır.",
                "android.permission.WAKE_LOCK" to "Bildirim geldiğinde cihazın uyumasını engelleyerek bildirimleri yakalamak için kullanılır.",
                "android.permission.POST_NOTIFICATIONS" to "Size bildirim durumu hakkında bilgi vermek için kullanılır.",
                "android.permission.INTERNET" to "Yalnızca Google Play abonelik sistemi için kullanılır. Bildirim verileriniz asla dışarı gönderilmez.",
                "android.permission.ACCESS_NETWORK_STATE" to "Ağ bağlantısı durumunu kontrol etmek için kullanılır."
            ),
            networkConnections = listOf(
                NetworkConnection(
                    host = "play.googleapis.com",
                    purpose = "Google Play abonelik doğrulama",
                    dataTransferred = "Yalnızca abonelik durumu bilgisi"
                )
            ),
            dataPolicy = "Bildirim geçmişi ve ayarlar yalnızca cihazınızda saklanır. Hiçbir bildirim verisi uzak sunuculara gönderilmez."
        ) else AppTransparencyInfo(
            developerName = "TrustEdge Labs",
            privacyGuarantee = "GhostNotify is developed by TrustEdge Labs. Your notification data is never sent to external servers. All processing happens entirely on your device.",
            permissionExplanations = mapOf(
                "android.permission.FOREGROUND_SERVICE" to "Required to run the notification monitoring service in the background.",
                "android.permission.RECEIVE_BOOT_COMPLETED" to "Used to automatically resume notification monitoring when the device restarts.",
                "android.permission.WAKE_LOCK" to "Used to keep the device awake to capture notifications.",
                "android.permission.POST_NOTIFICATIONS" to "Used to inform you about notification status.",
                "android.permission.INTERNET" to "Only used for Google Play subscription system. Your notification data is never sent externally.",
                "android.permission.ACCESS_NETWORK_STATE" to "Used to check network connection status."
            ),
            networkConnections = listOf(
                NetworkConnection(
                    host = "play.googleapis.com",
                    purpose = "Google Play subscription verification",
                    dataTransferred = "Subscription status only"
                )
            ),
            dataPolicy = "Notification history and settings are stored only on your device. No notification data is sent to remote servers."
        )
    }

    // ══════════════════════════════════════════════════════════════
    // IbansApp
    // ══════════════════════════════════════════════════════════════

    private fun getIbansAppInfo(isTr: Boolean): AppTransparencyInfo {
        return if (isTr) AppTransparencyInfo(
            developerName = "TrustEdge Labs",
            privacyGuarantee = "IbansApp, TrustEdge Labs tarafından geliştirilmiştir. IBAN ve finansal verileriniz asla dış sunuculara gönderilmez. Tüm veriler cihazınızda şifrelenmiş olarak saklanır.",
            permissionExplanations = mapOf(
                "android.permission.INTERNET" to "Yalnızca Google Play abonelik sistemi için kullanılır. IBAN verileriniz asla dışarı gönderilmez.",
                "android.permission.CAMERA" to "QR kod ve IBAN taramak için kullanılır. Kamera verileri asla kaydedilmez veya dışarı aktarılmaz."
            ),
            networkConnections = listOf(
                NetworkConnection(
                    host = "play.googleapis.com",
                    purpose = "Google Play abonelik doğrulama",
                    dataTransferred = "Yalnızca abonelik durumu bilgisi"
                )
            ),
            dataPolicy = "IBAN bilgileri ve finansal verileriniz yalnızca cihazınızın yerel depolama alanında saklanır. Hiçbir finansal veri uzak sunuculara gönderilmez."
        ) else AppTransparencyInfo(
            developerName = "TrustEdge Labs",
            privacyGuarantee = "IbansApp is developed by TrustEdge Labs. Your IBAN and financial data is never sent to external servers. All data is stored encrypted on your device.",
            permissionExplanations = mapOf(
                "android.permission.INTERNET" to "Only used for Google Play subscription system. Your IBAN data is never sent externally.",
                "android.permission.CAMERA" to "Used to scan QR codes and IBANs. Camera data is never recorded or exported."
            ),
            networkConnections = listOf(
                NetworkConnection(
                    host = "play.googleapis.com",
                    purpose = "Google Play subscription verification",
                    dataTransferred = "Subscription status only"
                )
            ),
            dataPolicy = "IBAN information and financial data are stored only in your device's local storage. No financial data is sent to remote servers."
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Generic TrustEdge Labs app
    // ══════════════════════════════════════════════════════════════

    private fun getGenericTrustedInfo(isTr: Boolean, packageName: String): AppTransparencyInfo {
        return if (isTr) AppTransparencyInfo(
            developerName = "TrustEdge Labs",
            privacyGuarantee = "Bu uygulama TrustEdge Labs tarafından geliştirilmiştir. Verileriniz dış sunuculara gönderilmez. İnternet bağlantısı yalnızca gerekli servisler (abonelik yönetimi vb.) için kullanılır.",
            permissionExplanations = emptyMap(),
            networkConnections = listOf(
                NetworkConnection(
                    host = "play.googleapis.com",
                    purpose = "Google Play abonelik doğrulama",
                    dataTransferred = "Yalnızca abonelik durumu bilgisi"
                )
            ),
            dataPolicy = "Tüm veriler cihazınızda kalır. Hiçbir kişisel veri uzak sunuculara gönderilmez."
        ) else AppTransparencyInfo(
            developerName = "TrustEdge Labs",
            privacyGuarantee = "This app is developed by TrustEdge Labs. Your data is not sent to external servers. Internet connection is only used for necessary services (subscription management, etc.).",
            permissionExplanations = emptyMap(),
            networkConnections = listOf(
                NetworkConnection(
                    host = "play.googleapis.com",
                    purpose = "Google Play subscription verification",
                    dataTransferred = "Subscription status only"
                )
            ),
            dataPolicy = "All data stays on your device. No personal data is sent to remote servers."
        )
    }
}
