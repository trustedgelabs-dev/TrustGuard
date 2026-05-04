# TrustGuard v3.0 — Kaynak Kod Seffaflik Raporu

Asagidaki kod bloklari, TrustGuard'in kullanici verilerini TOPLAMADIGI, 
disariya GONDERMEDIGINI ve tuem islemlerin YEREL olarak cihazda yapildigini 
kanitlamak amaciyla paylasilan acik kaynak parcalaridir.

---

## 1. Bagimliliklat (build.gradle.kts) — Izleyici / Analitik YOK

Uygulamanin tum ucuncu parti kutuphaneleri asagidadir.
Google Analytics, Firebase, Facebook SDK, AppsFlyer veya
herhangi bir izleyici/reklam kutuphanesi YOKTUR.

```kotlin
// app/build.gradle.kts — TAM DOSYA

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.trustedgelabs.trustguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.trustedgelabs.trustguard"
        minSdk = 24
        targetSdk = 35
        versionCode = 14
        versionName = "3.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Android Jetpack — standart UI ve yasam dongusu kutuphaneleri
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

    // Coil — sadece yerel dosya onizleme icin (ag erisimi YOK)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")

    // ❌ Google Analytics — YOK
    // ❌ Firebase — YOK
    // ❌ Facebook SDK — YOK
    // ❌ AppsFlyer — YOK
    // ❌ Adjust — YOK
    // ❌ OkHttp — YOK (v3.0'da kaldirildi)
    // ❌ Retrofit — YOK
    // ❌ Herhangi bir ag istemcisi — YOK

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

```kotlin
// build.gradle.kts (root) — TAM DOSYA
// Hicbir Google Services veya ucuncu parti eklentisi yok

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

**Sonuc:** Uygulamada hicbir ag istemci kutuphanesi (OkHttp, Retrofit, Volley vs.)
bulunmamaktadir. Uygulama disari veri gonderecek bir mekanizmaya sahip degildir.

---

## 2. Uygulama Baslangici (Application Class) — Telemetri / Crash Reporting YOK

Uygulama baslatildiginda hangi servislerin calistigini gosteren ana sinif.
Hicbir analitik, telemetri veya cokme raporlama sistemi yoktur.

```kotlin
// TrustGuardApp.kt — TAM DOSYA

package com.trustedgelabs.trustguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.trustedgelabs.trustguard.billing.BillingManager
import com.trustedgelabs.trustguard.service.AppBlockerService
import com.trustedgelabs.trustguard.util.FamilyShieldManager
import com.trustedgelabs.trustguard.util.LocaleManager

class TrustGuardApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.applyLocale(base))
    }

    lateinit var billingManager: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()   // Sadece bildirim kanali
        initBilling()                   // Yerel faturalama
        ensureFamilyShieldService()     // Ebeveyn kontrolu servisi
    }

    // ❌ initAnalytics() — YOK
    // ❌ CrashReporter — YOK
    // ❌ Firebase.initialize() — YOK
    // ❌ Herhangi bir uzak sunucuya baglanti — YOK

    private fun ensureFamilyShieldService() {
        try {
            if (FamilyShieldManager.isEnabled(this)) {
                AppBlockerService.start(this)
            }
        } catch (_: Exception) { /* kritik degil */ }
    }

    private fun initBilling() {
        billingManager = BillingManager(this)
        billingManager.initialize()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vpnChannel = NotificationChannel(
                "trustguard_vpn",
                getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_notification_title)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(vpnChannel)
        }
    }
}
```

**Sonuc:** Uygulama basladiginda SADECE bildirim kanali, yerel faturalama ve
ebeveyn kontrolu servisi baslatilir. Hicbir uzak sunucuya baglanti yapilmaz.

---

## 3. VPN Servisi — Tum Trafik YEREL Islenir

VPN servisi, Android'in VpnService API'sini kullanarak DNS sorgularini
YEREL OLARAK filtreler. Engellenen alan adlari icin sahte yanit uretilir,
engellenmeyenler icin sorgu Cloudflare (1.1.1.1), Google (8.8.8.8) veya 
Quad9 (9.9.9.9) DNS sunucularina iletilir.

**ONEMLI:** Hicbir kullanici verisi, tarama gecmisi veya DNS sorgusu 
TrustGuard sunucularina gonderilmez. Uygulamamizin sunucusu YOKTUR.

```kotlin
// TrustGuardVpnService.kt — AG KATMANI (kritik bolumler)

class TrustGuardVpnService : VpnService() {

    // VPN arayuzu — tum islem cihaz uzerinde yerel olarak yapilir
    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile
    private var isRunning = false

    // ...

    // DNS sorgusu isleme — YEREL filtreleme
    private fun handleDnsQuery(
        packet: ByteArray,
        length: Int,
        ipHeaderLen: Int,
        outputStream: FileOutputStream
    ) {
        val udpHeaderLen = 8
        val dnsOffset = ipHeaderLen + udpHeaderLen
        if (dnsOffset >= length) return

        val dnsPayload = packet.copyOfRange(dnsOffset, length)
        val dnsPacket = DnsPacket.parse(dnsPayload) ?: return
        val domain = dnsPacket.questionDomain

        // Istatistik sadece YEREL sayac — disari gonderilmez
        statsRepository.recordQuery()

        if (blocklistRepository.isDomainBlocked(domain)) {
            // ENGELLENEN alan adi: sahte (0.0.0.0) yanit uret
            // Hicbir yere log gonderilmez
            val responsePayload = DnsResponseBuilder.buildBlockedResponse(
                dnsPayload, dnsPacket
            )
            val responsePacket = IpPacketBuilder.buildResponse(
                packet, ipHeaderLen, responsePayload
            )

            synchronized(outputStream) {
                outputStream.write(responsePacket)
                outputStream.flush()
            }

            // Yerel sayac guncelle (SharedPreferences)
            statsRepository.recordBlock(domain)
        } else {
            // IZIN VERILEN alan adi: gercek DNS sunucusuna ilet
            forwardDnsQuery(packet, ipHeaderLen, dnsPayload, outputStream)
        }
    }

    // Gercek DNS sunuculari — hepsi guvenilir, herkese acik DNS servisleri
    // TrustGuard'a ait HICBIR sunucu kullanilmaz
    private val dnsServers = listOf(
        "1.1.1.1",   // Cloudflare DNS
        "8.8.8.8",   // Google DNS
        "9.9.9.9"    // Quad9 DNS
    )

    private fun forwardDnsQuery(
        originalPacket: ByteArray,
        ipHeaderLen: Int,
        dnsPayload: ByteArray,
        outputStream: FileOutputStream
    ) {
        for (dns in dnsServers) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                protect(socket)  // VPN tunelinden cikar (sonsuz dongu onle)

                socket.soTimeout = 3000
                val address = InetAddress.getByName(dns)

                // DNS sorgusunu DOGRUDAN herkese acik DNS'e gonder
                // Arada TrustGuard proxy'si veya sunucusu YOK
                val sendPacket = DatagramPacket(
                    dnsPayload, dnsPayload.size, address, 53
                )
                socket.send(sendPacket)

                // Yaniti al ve kullaniciya ilet
                val responseBuffer = ByteArray(4096)
                val receivePacket = DatagramPacket(
                    responseBuffer, responseBuffer.size
                )
                socket.receive(receivePacket)

                val dnsResponse = responseBuffer.copyOf(receivePacket.length)
                val responseIpPacket = IpPacketBuilder.buildResponse(
                    originalPacket, ipHeaderLen, dnsResponse
                )

                synchronized(outputStream) {
                    outputStream.write(responseIpPacket)
                    outputStream.flush()
                }
                return
            } catch (e: Exception) {
                // Sunucu yanitlamadiysa siradakini dene
            } finally {
                socket?.close()
            }
        }
    }
}
```

**Sonuc:** 
- DNS sorgulari SADECE Cloudflare/Google/Quad9'a iletilir
- TrustGuard'a ait hicbir proxy veya sunucu YOKTUR
- Engelleme karari tamamen cihaz uzerinde YEREL olarak verilir
- Tarama gecmisi veya DNS loglari hicbir yere GONDERILMEZ

---

## 4. Sifreli Kasa — AES-256-GCM Sifreleme (Yerel)

Kullanici dosyalari askeri sinif sifreleme ile cihaz uzerinde sifrelenir.
Sifreli dosyalar SADECE cihazda saklanir, hicbir bulut servisine yuklenmez.

```kotlin
// VaultManager.kt — SIFRELEME KATMANI

class VaultManager(private val context: Context) {

    // Tum veriler cihazin yerel depolamasinda saklanir
    private val vaultDir: File = File(context.filesDir, "vault")

    companion object {
        private const val AES_KEY_SIZE = 256        // AES-256 bit anahtar
        private const val GCM_IV_LENGTH = 12        // 96-bit IV (NIST onerisi)
        private const val GCM_TAG_LENGTH = 128      // 128-bit dogrulama etiketi
        private const val PBKDF2_ITERATIONS = 100_000  // Kaba kuvvet korumasi
    }

    // Sifre dogrulama — PBKDF2 ile hash'lenir, duz metin saklanmaz
    fun verifyPassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val salt = prefs.getString(KEY_SALT, null)?.hexToBytes() ?: return false
        val hash = hashPassword(password, salt)
        return hash.toHex() == storedHash
    }

    // Dosya sifreleme — AES-256-GCM
    fun encryptAndStore(
        uri: Uri, 
        fileName: String, 
        mimeType: String, 
        password: String
    ): VaultFile? {
        return try {
            val salt = prefs.getString(KEY_SALT, null)?.hexToBytes() 
                ?: return null
            val key = deriveKey(password, salt)

            // Dosyayi oku
            val inputStream = context.contentResolver.openInputStream(uri) 
                ?: return null
            val plainBytes = inputStream.readBytes()
            inputStream.close()

            // Rastgele IV uret (her dosya icin benzersiz)
            val iv = ByteArray(GCM_IV_LENGTH).also { 
                SecureRandom().nextBytes(it) 
            }

            // AES-256-GCM ile sifrele
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE, key, 
                GCMParameterSpec(GCM_TAG_LENGTH, iv)
            )
            val encryptedBytes = cipher.doFinal(plainBytes)

            // IV + sifreli veri olarak YEREL dosyaya yaz
            // ❌ Hicbir bulut servisine YUKLENMEZ
            FileOutputStream(encFile).use { fos ->
                fos.write(iv)             // IV (12 byte)
                fos.write(encryptedBytes) // Sifreli veri + GCM tag
            }

            // ...
        } catch (e: Exception) { null }
    }

    // Dosya cozme — ayni AES-256-GCM
    fun decryptFile(vaultFile: VaultFile, password: String): ByteArray? {
        return try {
            val salt = prefs.getString(KEY_SALT, null)?.hexToBytes() 
                ?: return null
            val key = deriveKey(password, salt)

            val fileBytes = FileInputStream(
                File(vaultFile.encryptedPath)
            ).readBytes()
            
            val iv = fileBytes.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedBytes = fileBytes.copyOfRange(
                GCM_IV_LENGTH, fileBytes.size
            )

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE, key, 
                GCMParameterSpec(GCM_TAG_LENGTH, iv)
            )
            cipher.doFinal(encryptedBytes)
        } catch (e: Exception) { null }
    }

    // Anahtar turetme — PBKDF2 (100.000 tur)
    // Kaba kuvvet saldirilarina karsi koruma
    private fun deriveKey(
        password: String, 
        salt: ByteArray
    ): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(
            "PBKDF2WithHmacSHA256"
        )
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
```

**Sonuc:**
- AES-256-GCM: ABD hukumeti ve askeri standart sifreleme
- PBKDF2 100.000 tur: Kaba kuvvet saldirilarina karsi koruma
- Rastgele IV: Her dosya icin benzersiz sifreleme
- YEREL depolama: Dosyalar sadece cihazda saklanir, buluta YUKLENMEZ
- Sifreleme anahtari cihazda tutulmaz, her seferinde sifreden turetilir

---

## 5. INTERNET Izni Analizi

TrustGuard'in AndroidManifest.xml dosyasinda INTERNET izni vardir.
Bu izin SADECE asagidaki amaclar icin kullanilir:

| Kullanim | Aciklama |
|----------|----------|
| DNS Sorgulari | Engellenmemis alan adlarini 1.1.1.1 / 8.8.8.8 / 9.9.9.9'a iletmek |
| VPN Tuneli | Android VpnService API gerekliligi |

**INTERNET izni ile YAPILMAYANLAR:**
- ❌ Kullanici verisi toplama
- ❌ Telemetri / analitik gonderme
- ❌ TrustGuard sunucusuna baglanti (sunucumuz yoktur)
- ❌ Reklam gosterme
- ❌ Ucuncu parti servislere veri aktarimi

---

## Dogrulama Yontemleri

Bu kodlari dogrulamak isteyen teknik kullanicilar icin:

1. **APK Decompile:** jadx veya apktool ile APK'yi acibirsltirin,
   tum sinif dosyalarini inceleyebilirsiniz
2. **Ag Trafigi Izleme:** Wireshark veya mitmproxy ile 
   uygulamanin ag trafikini izleyin — TrustGuard sunucusuna
   hicbir baglanti gormeyeceksiniz
3. **Bagimlnlik Taramasi:** `./gradlew dependencies` komutu ile
   tum ucuncu parti kutuphaneleri listeleyebilirsiniz

---

*Bu belge TrustGuard v3.0 (versionCode 14) icin gecerlidir.*
*Son guncelleme: Nisan 2026*
*Trusted Edge Labs — trustedgelabs.dev*
