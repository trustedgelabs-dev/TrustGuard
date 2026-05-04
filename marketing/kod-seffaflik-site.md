Kod Parçası 1

Dosya Adı: build.gradle.kts
Dil: Kotlin (Gradle)
Bu kod ne yapar?: Uygulamanın tüm bağımlılıklarını listeler. Google Analytics, Firebase, Facebook SDK, OkHttp veya herhangi bir izleyici/reklam kütüphanesi YOKTUR. Uygulama dışarıya veri gönderecek bir ağ istemcisine sahip değildir.

Kod:

dependencies {
    // Android Jetpack — standart UI kütüphaneleri
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material.icons.extended)

    // Coil — sadece yerel dosya önizleme (ağ erişimi YOK)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")

    // ❌ Google Analytics — YOK
    // ❌ Firebase — YOK
    // ❌ Facebook SDK — YOK
    // ❌ AppsFlyer / Adjust — YOK
    // ❌ OkHttp / Retrofit — YOK
    // ❌ Herhangi bir ağ istemcisi — YOK
}

---

Kod Parçası 2

Dosya Adı: TrustGuardApp.kt
Dil: Kotlin
Bu kod ne yapar?: Uygulama başlatıldığında çalışan ana sınıf. Hiçbir analitik, telemetri veya çökme raporlama sistemi başlatılmaz. Uzak sunucuya bağlantı yapılmaz.

Kod:

class TrustGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()   // Sadece bildirim kanalı oluşturur
        initBilling()                   // Yerel faturalama başlatır
        ensureFamilyShieldService()     // Ebeveyn kontrolü servisi

        // ❌ initAnalytics() — YOK
        // ❌ CrashReporter — YOK
        // ❌ Firebase.initialize() — YOK
        // ❌ Uzak sunucuya bağlantı — YOK
    }

    private fun ensureFamilyShieldService() {
        try {
            if (FamilyShieldManager.isEnabled(this)) {
                AppBlockerService.start(this)
            }
        } catch (_: Exception) { }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vpnChannel = NotificationChannel(
                "trustguard_vpn",
                getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(vpnChannel)
        }
    }
}

---

Kod Parçası 3

Dosya Adı: TrustGuardVpnService.kt
Dil: Kotlin
Bu kod ne yapar?: VPN üzerinden geçen DNS sorgularını YEREL olarak filtreler. Engellenen alan adlarına sahte yanıt üretilir, diğerleri doğrudan Cloudflare/Google/Quad9 DNS sunucularına iletilir. TrustGuard'a ait hiçbir sunucu veya proxy kullanılmaz.

Kod:

// Kullanılan DNS sunucuları — hepsi herkese açık güvenilir servisler
// TrustGuard'a ait HİÇBİR sunucu yoktur
private val dnsServers = listOf(
    "1.1.1.1",   // Cloudflare DNS
    "8.8.8.8",   // Google DNS
    "9.9.9.9"    // Quad9 DNS
)

// DNS sorgusu işleme — tüm işlem cihaz üzerinde YEREL olarak yapılır
private fun handleDnsQuery(packet: ByteArray, length: Int,
    ipHeaderLen: Int, outputStream: FileOutputStream) {

    val dnsPayload = packet.copyOfRange(dnsOffset, length)
    val dnsPacket = DnsPacket.parse(dnsPayload) ?: return
    val domain = dnsPacket.questionDomain

    // Yerel sayaç — dışarıya gönderilmez
    statsRepository.recordQuery()

    if (blocklistRepository.isDomainBlocked(domain)) {
        // ENGELLENEN alan adı: sahte yanıt üret (0.0.0.0)
        // Hiçbir yere log gönderilmez
        val responsePayload = DnsResponseBuilder
            .buildBlockedResponse(dnsPayload, dnsPacket)
        val responsePacket = IpPacketBuilder
            .buildResponse(packet, ipHeaderLen, responsePayload)

        synchronized(outputStream) {
            outputStream.write(responsePacket)
            outputStream.flush()
        }

        // Yerel engelleme sayacı (SharedPreferences)
        statsRepository.recordBlock(domain)
    } else {
        // İZİN VERİLEN alan adı: gerçek DNS sunucusuna ilet
        forwardDnsQuery(packet, ipHeaderLen, dnsPayload, outputStream)
    }
}

// DNS sorgusunu gerçek DNS sunucusuna iletme
private fun forwardDnsQuery(originalPacket: ByteArray,
    ipHeaderLen: Int, dnsPayload: ByteArray,
    outputStream: FileOutputStream) {

    for (dns in dnsServers) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            protect(socket) // VPN tünelinden çıkar (sonsuz döngü önlenir)
            socket.soTimeout = 3000

            // DNS sorgusunu DOĞRUDAN herkese açık DNS'e gönder
            // Arada TrustGuard proxy'si veya sunucusu YOK
            val address = InetAddress.getByName(dns)
            socket.send(DatagramPacket(dnsPayload, dnsPayload.size, address, 53))

            // Yanıtı al ve kullanıcıya ilet
            val responseBuffer = ByteArray(4096)
            val receivePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(receivePacket)

            val dnsResponse = responseBuffer.copyOf(receivePacket.length)
            val responseIpPacket = IpPacketBuilder
                .buildResponse(originalPacket, ipHeaderLen, dnsResponse)

            synchronized(outputStream) {
                outputStream.write(responseIpPacket)
                outputStream.flush()
            }
            return
        } catch (e: Exception) {
            // Bu sunucu yanıtlamadıysa sıradakini dene
        } finally {
            socket?.close()
        }
    }
}

---

Kod Parçası 4

Dosya Adı: VaultManager.kt
Dil: Kotlin
Bu kod ne yapar?: Kullanıcının dosyalarını AES-256-GCM askeri sınıf şifreleme ile cihaz üzerinde şifreler. Şifreleme anahtarı PBKDF2 ile 100.000 turda türetilir. Şifreli dosyalar SADECE cihazda saklanır, hiçbir bulut servisine yüklenmez.

Kod:

class VaultManager(private val context: Context) {

    // Tüm şifreli dosyalar cihazın yerel depolamasında saklanır
    private val vaultDir: File = File(context.filesDir, "vault")

    companion object {
        private const val AES_KEY_SIZE = 256          // AES-256 bit anahtar
        private const val GCM_IV_LENGTH = 12          // 96-bit IV (NIST önerisi)
        private const val GCM_TAG_LENGTH = 128        // 128-bit doğrulama etiketi
        private const val PBKDF2_ITERATIONS = 100_000 // Kaba kuvvet koruması
    }

    // Dosya şifreleme — AES-256-GCM
    fun encryptAndStore(uri: Uri, fileName: String,
        mimeType: String, password: String): VaultFile? {

        val salt = prefs.getString(KEY_SALT, null)?.hexToBytes() ?: return null
        val key = deriveKey(password, salt)

        // Dosyayı oku
        val plainBytes = context.contentResolver
            .openInputStream(uri)?.readBytes() ?: return null

        // Her dosya için benzersiz rastgele IV üret
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }

        // AES-256-GCM ile şifrele
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val encryptedBytes = cipher.doFinal(plainBytes)

        // IV + şifreli veri olarak YEREL dosyaya yaz
        // ❌ Hiçbir bulut servisine YÜKLENMEZ
        FileOutputStream(encFile).use { fos ->
            fos.write(iv)             // IV (12 byte)
            fos.write(encryptedBytes) // Şifreli veri + GCM tag
        }

        return vaultFile
    }

    // Anahtar türetme — PBKDF2 ile 100.000 tur
    // Kaba kuvvet saldırılarına karşı güçlü koruma
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,  // 100.000 tur
            AES_KEY_SIZE         // 256 bit
        )
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }
}

---

Kod Parçası 5

Dosya Adı: FakeIdentityManager.kt
Dil: Kotlin
Bu kod ne yapar?: Sahte kimlik üretici tamamen cihazda çalışır. Gerçek cihaz bilgileri okunmaz. Üretilen veriler sadece SharedPreferences'ta saklanır, hiçbir sunucuya gönderilmez.

Kod:

// Sahte kimlik üretimi — sunucu bağlantısı YOKTUR
// Gerçek IMEI/MAC/Android ID okunmaz
fun generateNewIdentity(): FakeIdentity {
    val (model, manufacturer) = deviceModels.random()
    val (version, build) = androidVersions.random()

    return FakeIdentity(
        deviceModel = model,
        manufacturer = manufacturer,
        androidVersion = version,
        androidId = generateHexString(16),
        imei = generateImei(),
        macAddress = generateMacAddress(),
        serialNumber = generateSerialNumber(),
        buildFingerprint = "$manufacturer/$model:$version/$build:user/release-keys"
    )
}

// IMEI Luhn algoritması ile geçerli formatta üretilir
private fun generateImei(): String {
    val tac = listOf("35", "86", "01", "35").random()
    val digits = StringBuilder(tac)
    repeat(12 - tac.length) { digits.append((0..9).random()) }
    digits.append(calculateLuhnCheckDigit(digits.toString()))
    return digits.toString()
}

// MAC adresi rastgele üretilir — gerçek MAC okunmaz
private fun generateMacAddress(): String {
    val prefixes = listOf("00:1A:2B", "AC:DE:48", "F4:CE:46", "D8:BB:C1")
    val prefix = prefixes.random()
    val suffix = (1..3).joinToString(":") {
        String.format("%02X", (0..255).random())
    }
    return "$prefix:$suffix"
}

---

Kod Parçası 6

Dosya Adı: build.gradle.kts (Root)
Dil: Kotlin (Gradle)
Bu kod ne yapar?: Projenin kök yapılandırma dosyası. Google Services, Firebase veya herhangi bir üçüncü parti izleyici eklentisi YOKTUR.

Kod:

// Proje kök yapılandırması — TAM DOSYA
// Hiçbir izleyici veya analitik eklentisi yok

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // ❌ com.google.gms.google-services — YOK
    // ❌ com.google.firebase.crashlytics — YOK
    // ❌ com.google.firebase.perf — YOK
}
