package com.trustedgelabs.trustguard.domain

import java.util.Locale

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

    fun hasTransparencyInfo(packageName: String): Boolean {
        return getTransparencyInfo(packageName) != null
    }

    fun getTransparencyInfo(packageName: String): AppTransparencyInfo? {
        val isTr = Locale.getDefault().language == "tr"
        return when {
            packageName == "com.trustedgelabs.trustguard" -> getTrustGuardInfo(isTr)
            else -> null
        }
    }

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
            dataPolicy = "Tüm veriler cihazınızda kalır. Uygulama izin analizi, dosya tarama sonuçları ve engelleme istatistikleri yalnızca cihazınızın yerel depolama alanında saklanır. Hiçbir veri uzak sunuculara gönderilmez."
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
            dataPolicy = "All data stays on your device. Permission analysis, file scan results, and blocking statistics are stored only in your device's local storage. No data is sent to remote servers."
        )
    }
}
