# TrustGuard - Kapsamli Uygulama Dokumantasyonu

**Gelistirici:** TrustEdge Labs
**Paket Adi:** `com.trustedgelabs.trustguard`
**Versiyon:** 3.0.0 (versionCode: 14)
**Platform:** Android
**Minimum SDK:** 24 (Android 7.0 Nougat)
**Hedef SDK:** 35 (Android 15)
**Derleme SDK:** 35

---

## Icindekiler

1. [Genel Bakis](#1-genel-bakis)
2. [Teknoloji Yigini ve Bagimliliklar](#2-teknoloji-yigini-ve-bagimliliklar)
3. [Mimari Yapi](#3-mimari-yapi)
4. [Ozellikler ve Ekranlar](#4-ozellikler-ve-ekranlar)
5. [VPN ve DNS Engelleme Sistemi](#5-vpn-ve-dns-engelleme-sistemi)
6. [Adware Tespit Motoru](#6-adware-tespit-motoru)
7. [Izin Siniflandirma ve Risk Analizi](#7-izin-siniflandirma-ve-risk-analizi)
8. [Dosya Kurtarma Sistemi](#8-dosya-kurtarma-sistemi)
9. [Optimizasyon Motoru](#9-optimizasyon-motoru)
10. [Guvenlik Onlemleri](#10-guvenlik-onlemleri)
13. [Android Izinleri](#13-android-izinleri)
14. [Yerellesltirme](#14-yerellestirme)
15. [Tema ve Tasarim Sistemi](#15-tema-ve-tasarim-sistemi)
16. [Derleme Yapilandirmasi](#16-derleme-yapilandirmasi)
17. [Kaynak Dosyalari](#17-kaynak-dosyalari)
18. [Dosya Yapisi](#18-dosya-yapisi)

---

## 1. Genel Bakis

TrustGuard, Android cihazlar icin gelistirilmis kapsamli bir gizlilik ve guvenlik uygulamasidir. Kullanicilarin yuklu uygulamalarini tarayarak izin risklerini analiz eder, reklam izleyicileri DNS seviyesinde engeller, adware tespit eder, dosya kurtarma ve cihaz optimizasyonu saglar.

### Temel Deger Onerileri

| Ozellik | Aciklama | Ucretli/Ucretsiz |
|---------|----------|-------------------|
| Izin Analizi | Tum uygulamalarin izinlerini tarar ve risk puanlar | Ucretsiz |
| DNS Engelleme (VPN) | Reklam ve izleyicileri DNS seviyesinde engeller | Ucretsiz |
| Adware Tespiti | SDK imzalari ve izin kombinasyonlari ile supheli uygulama tespiti | Ucretsiz |
| Wi-Fi Guvenlik Taramasi | Bagli Wi-Fi aginin guvenlik analizini yapar | Ucretsiz |
| Pil Saglik Analizi | Pil durumu, sicaklik ve ust tuketicileri gosterir | Ucretsiz |
| Depolama Analizi | Depolama kullanim dagilimini kategorize eder | Ucretsiz |
| Dosya Kurtarma | Silinen dosyalari 9 farkli strateji ile kurtarir | Ucretsiz |
| Cihaz Optimizasyonu | Cop dosya, onbellek, kopya temizligi | Ucretsiz |
| Uygulama Butunlugu | Yukleme kaynagi dogrulama (Play Store vs Sideload) | Premium |
| Ag Izleme | Uygulama bazli ag trafik izlemesi | Premium |
| Aile Kalkani | Cocuklarin cihazlari icin gercek zamanli koruma | Premium |

---

## 2. Teknoloji Yigini ve Bagimliliklar

### Cekirdek Teknolojiler

| Teknoloji | Versiyon | Amac |
|-----------|----------|------|
| Kotlin | 2.0.21 | Ana programlama dili |
| Jetpack Compose | BOM 2024.09.00 | %100 Compose tabanli UI |
| Material 3 | 1.3.1 | Material Design 3 bilesenleri |
| Android Gradle Plugin | 8.9.0 | Derleme sistemi |
| Java Uyumlulugu | VERSION_11 | JVM hedefi |

### AndroidX Kutuphaneleri

| Kutuphane | Versiyon | Amac |
|-----------|----------|------|
| `androidx.core:core-ktx` | 1.15.0 | Android Core KTX uzantilari |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.7 | Yasam dongusu yonetimi |
| `androidx.activity:activity-compose` | 1.10.1 | Compose Activity entegrasyonu |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | ViewModel Compose entegrasyonu |
| `androidx.navigation:navigation-compose` | 2.8.8 | Ekran arasi navigasyon |
| `androidx.compose.material:material-icons-extended` | BOM | Genisletilmis Material ikonlari |

### Ucuncu Parti Kutuphaneler

| Kutuphane | Versiyon | Amac |
|-----------|----------|------|
| `com.android.billingclient:billing-ktx` | 7.1.1 | Google Play faturalandirma (abonelik) |
| `io.coil-kt:coil-compose` | 2.6.0 | Goruntu yukleme (uygulama ikonlari) |
| `io.coil-kt:coil-video` | 2.6.0 | Video thumbnail yukleme |

### Test Kutuphaneleri

| Kutuphane | Versiyon |
|-----------|----------|
| `junit:junit` | 4.13.2 |
| `androidx.test.ext:junit` | 1.2.1 |
| `androidx.test.espresso:espresso-core` | 3.6.1 |
| `androidx.compose.ui:ui-test-junit4` | BOM |

---

## 3. Mimari Yapi

### Katmanli MVVM Mimarisi

```
Sunum Katmani (UI)
    Compose Ekranlar + Material 3 Bilesenler
    ViewModel'ler (StateFlow ile reaktif durum yonetimi)
        |
Kullanim Senaryolari (Use Cases)
    ScanAppsUseCase, AnalyzePermissionsUseCase
        |
Repository Katmani
    AppRepositoryImpl, BlocklistRepositoryImpl, BlockingStatsRepository
        |
Veri Kaynagi Katmani (Data Sources)
    PackageManagerDataSource, WifiSecurityDataSource, BatteryDataSource,
    StorageDataSource, AppIntegrityDataSource, OptimizationDataSource,
    MediaStoreRecoveryDataSource, BlocklistDataSource
        |
Alan Mantigi (Domain)
    AdwareDetector, PermissionClassifier, DailyLimitManager
        |
Servis Katmani
    TrustGuardVpnService, NotificationAdDetector, VpnControlManager
        |
Android Sistem API'leri
    PackageManager, VpnService, WifiManager, BatteryManager,
    StorageStatsManager, UsageStatsManager, TrafficStats, MediaStore
```

### Durum Yonetimi

- **MutableStateFlow / StateFlow:** Tum ViewModel'lerde reaktif durum yonetimi
- **collectAsState():** Compose UI'da Flow'lari dinleme
- **SharedPreferences:** Kalici veri depolama (gunluk limit, VPN filtre ayarlari)
- **@Volatile:** Thread-safe boolean bayraklari (VPN dongusu)

### Navigasyon Yapisi

`NavGraph.kt` ile 14 ekran arasi navigasyon:

```kotlin
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AppList : Screen("app_list")
    data object Detail : Screen("detail/{packageName}")
    data object DnsBlocking : Screen("dns_blocking")
    data object Recovery : Screen("recovery")
    data object Adware : Screen("adware")
    data object Premium : Screen("premium")
    data object Optimization : Screen("optimization")
    data object WifiSecurity : Screen("wifi_security")
    data object BatteryHealth : Screen("battery_health")
    data object StorageAnalyzer : Screen("storage_analyzer")
    data object AppIntegrity : Screen("app_integrity")      // Premium
    data object NetworkMonitor : Screen("network_monitor")  // Premium
    data object Settings : Screen("settings")
}
```

Premium ekranlar (AppIntegrity, NetworkMonitor) `BillingManager.isPremium` kontrolu ile korunur; ucretsiz kullanicilar Premium ekranina yonlendirilir.

---

## 4. Ozellikler ve Ekranlar

### 4.1 Dashboard (Ana Ekran)

**Dosya:** `ui/screens/dashboard/DashboardScreen.kt`
**ViewModel:** `DashboardViewModel.kt`

Ana kontrol merkezi. Ucretsiz ve Premium kullanicilar farkli gorunumler gorur:

**Ucretsiz Kullanicilar:**
- Guvenlik Puan Arki (0-100 animasyonlu yay)
- Kalan tarama hakki gostergesi
- Trafik Isigi Satiri (Kirmizi/Sari/Yesil risk sayilari)
- VPN Hizli Aksiyon Cubugu
- Guvenlik Araclari: Izin Analizi, DNS Engelleme, Adware Tespiti, Wi-Fi Guvenlik
- Optimizasyon Araclari: Pil, Depolama, Temizlik, Kurtarma
- Pro Araclari (kilitli onizleme): Uygulama Butunlugu, Ag Izleme
- En Riskli 3 Uygulama listesi

**Premium Kullanicilar:**
- ProDashboardHeader (gradient kart: puan + engellenen tehdit + guvenlik olaylari)
- Tum araclar acik ve genisletilmis
- Gelismis Araclar bolumu (kilitli degil)

**ViewModel Ozellikleri:**
- Uygulama baslatildiginda otomatik tarama
- VPN durumu gozleme (StateFlow)
- Adware tarama
- Gunluk limit kontrolu ve dialog yonetimi
- Guvenlik puani hesaplama algoritmasi

### 4.2 Uygulama Listesi

**Dosya:** `ui/screens/applist/AppListScreen.kt`
**ViewModel:** `AppListViewModel.kt`

- Yuklu uygulamalarin filtrelenebilir listesi
- Risk filtresi: Tumu / Yuksek / Orta / Dusuk
- Sistem uygulamalarini goster/gizle secenegi
- Uygulama adina gore arama
- Her uygulama icin risk rozeti

### 4.3 Uygulama Detay Ekrani

**Dosya:** `ui/screens/detail/DetailScreen.kt`
**ViewModel:** `DetailViewModel.kt`

- Uygulama ikonu, adi ve risk seviyesi rozeti
- Genel puan ve tehlikeli izin sayisi
- Izin listesi (gruplu, risk gostergeleri ile)
- Guvenilir uygulama seffaflik bilgisi (TrustEdge Labs uygulamalari icin)
- Uygulama boyutu ve yukleme kaynagi
- Kaldirma ve acma butonlari

### 4.4 DNS Engelleme Ekrani

**Dosya:** `ui/screens/blocking/DnsBlockingScreen.kt`

- Bugun engellenen toplam sayi
- Toplam sorgu sayisi ve engelleme yuzdesi
- En cok engellenen domain listesi
- VPN acma/kapama kontrolu
- Engelleme listeleri yonetimi (etkinlestirme/devre disi birakma)

### 4.5 Wi-Fi Guvenlik Taramasi

**Dosya:** `ui/screens/wifi/WifiSecurityScreen.kt`

- 4 durum yonetimi: tarama, konum izni gerekli, hata, bagli degil
- Bagli Wi-Fi SSID'si
- Guvenlik protokolu (WPA3/WPA2/WEP/Acik)
- Guvenlik seviyesi gostergesi (Guvenli/Uyari/Tehlike)
- Sinyal gucu, IP adresi, Gateway, DNS bilgileri
- Guvenlik onerileri

**Veri Kaynagi:** `WifiSecurityDataSource.kt`
- `WifiManager` ile ag taramasi
- SecurityException ve genel Exception yakalama
- Hata durumunda null donusu (cokme onleme)

### 4.6 Pil Saglik Analizi

**Dosya:** `ui/screens/battery/BatteryHealthScreen.kt`

- Pil seviyesi dairesi ve sarj durumu
- Saglik durumu (Iyi/Asiri Isinma/Oldu vs.)
- Sicaklik ve voltaj bilgileri
- Son 24 saatte en cok pil tuketen uygulamalar (ilerleme cubugu ile)
- Pil tasarrufu ipuclari

**Veri Kaynagi:** `BatteryDataSource.kt`
- `BatteryManager` intent ile pil bilgileri
- `UsageStatsManager` ile uygulama bazli kullanim

### 4.7 Depolama Analizi

**Dosya:** `ui/screens/storage/StorageAnalyzerScreen.kt`

- Donut grafik ile depolama dagilimi
- Kullanim cubugu
- Kategori bazli dagılım (Fotoğraflar, Videolar, Ses, Belgeler, Uygulamalar)
- Bos alan, kullanilan alan, toplam alan

**Veri Kaynagi:** `StorageDataSource.kt`
- `StatFs` ile gercek depolama istatistikleri
- Medya turune gore boyut hesaplama
- `StorageStatsManager` ile uygulama depolama bilgisi

### 4.8 Adware Tespit Ekrani

**Dosya:** `ui/screens/adware/AdwareScreen.kt`
**ViewModel:** `AdwareViewModel.kt`

- Supheli uygulamalar listesi (suphe puani ile)
- Sebep kartlari: overlay reklamlar, otomatik baslama, bilinen SDK, agresif arka plan
- Her uygulama icin risk gostergesi ve kaldirma butonu

### 4.9 Dosya Kurtarma Ekrani

**Dosya:** `ui/screens/recovery/RecoveryScreen.kt`
**ViewModel:** `RecoveryViewModel.kt`

- 3 tarama modu: Hizli / Derin / Root
- Tarama ilerleme gostergesi
- Kurtarilabilir dosyalar listesi (ture gore gruplu: Foto/Video/Ses/Belge)
- Secili dosyalari kurtarma
- Varsayilan kurtarma dizini: `Pictures/TrustGuard_Recovered`

### 4.10 Optimizasyon Ekrani

**Dosya:** `ui/screens/optimization/OptimizationScreen.kt`
**ViewModel:** `OptimizationViewModel.kt`

- Tarama turleri: Sistem Cop, Kopya Fotograflar, Buyuk Dosyalar, Uygulama Onbellegi, E-posta Onbellegi
- Ilerleme gostergesi
- Kategoriye gore gruplu dosyalar
- Hizli silme ve alan tasarrufu tahmini

### 4.11 Uygulama Butunlugu (Premium)

**Dosya:** `ui/screens/integrity/AppIntegrityScreen.kt`
**ViewModel:** `AppIntegrityViewModel.kt`

- Tum uygulamalarin yukleme kaynagini tarar
- Play Store, Sideloaded, System, Unknown olarak siniflandirir
- Sideloaded uygulamalar basta siralama
- Yukleme kaynagi rozeti

**Veri Kaynagi:** `AppIntegrityDataSource.kt`
- `PackageManager.getInstallSourceInfo` ile kaynak tespiti

### 4.12 Ag Izleme (Premium)

**Dosya:** `ui/screens/network/NetworkMonitorScreen.kt`

- Toplam gonderilen/alinan veri kartlari
- Uygulama bazli veri tuketim listesi (ilerleme cubugu ile)
- `TrafficStats` API kullanimi

### 4.13 Premium Ekrani

**Dosya:** `ui/screens/premium/PremiumScreen.kt`

- Play Store'dan dinamik fiyat gosterimi
- Aile Kalkani duygusal hook bolumu (cocuklarin reklam tiklama senaryosu)
- Pro ozellik kartlari (ikonlu)
- 2x2 guven rozeti gridi
- Kisaltilmis guven mesaji bolumu
- Fiyat ve satin alma karti

### 4.14 Ayarlar Ekrani

**Dosya:** `ui/screens/settings/SettingsScreen.kt`

- Uygulama filtre modu (Tumu vs Secili)
- Harici tutulan uygulamalar listesi
- Hakkinda, Versiyon, Gizlilik Politikasi
- Gelistirici bilgileri (TrustEdge Labs)

---

## 5. VPN ve DNS Engelleme Sistemi

### 5.1 Genel Mimari

TrustGuard, Android'in `VpnService` API'sini kullanarak yerel bir DNS filtreleme sistemi uygular. Tum internet trafigini degil, yalnizca DNS sorgularini yakalayarak reklam ve izleyici domainlerini engeller.

### 5.2 Servis Uygulamasi

**Dosya:** `service/TrustGuardVpnService.kt`

```
Kullanici DNS sorgusu gonderir
    |
VPN arabirimi sorguyu yakalar (port 53 UDP)
    |
DNS Paket Ayristirma (DnsPacket.kt)
    |
Domain engelleme listesinde mi?
    |
  EVET --> 0.0.0.0 yaniti dondur (DnsResponseBuilder.kt)
    |
  HAYIR --> Gercek DNS sunucusuna ilet
```

**Teknik Detaylar:**
- Sahte DNS sunucu IP'leri kullanir: `198.18.0.1`, `198.18.0.2` (yonlendirilemez adres araligi)
- Sadece bu IP'leri VPN tunelinden gecirerek tum trafigi yakalamaktan kacinir
- Ozel paket isleme thread'i: `TrustGuard-VPN-Loop`
- `@Volatile` bayragi ile thread-safe `isRunning` durumu
- Foreground servis zorunlulugu (Android 14+: 5 saniye icinde `startForeground()` cagrisi)
- TrustGuard uygulamasini kendi VPN'inden haric tutar (sonsuz dongu onleme)
- `protect(socket)` cagrisi DNS iletme isteklerinin VPN'den gecmesini onler

### 5.3 DNS Paket Isleme

**Dosyalar:**
- `data/dns/DnsPacket.kt` - DNS sorgu ayristirma
- `data/dns/DnsResponseBuilder.kt` - Engelleme yaniti olusturma
- `data/dns/IpPacketBuilder.kt` - IP/UDP paket insasi

**DNS Sorgu Ayristirma:**
- Ham baytlardan DNS paketlerini ayristirir
- Transaction ID, bayraklar, soru sayisi cikarir
- Domain adini label'lar halinde (uzunluk-on-ekli) ozyinelemeli ayristirir
- Domain'leri kucuk harfe normalize eder
- Bozuk paketlerde `null` dondurur
- Minimum paket boyutu dogrulamasi: 12 bayt

**Engelleme Yaniti:**
- A-tipi sorgular icin 0.0.0.0 IP adresi dondurur
- Orjinal sorgudan Transaction ID ve bayraklari kopyalar
- QR=1 (yanit), AA=1 (yetkili), RD=1 (ozyineleme istegi) ayarlar
- TTL: 300 saniye
- DNS isaretci sikistirmasi kullanir (0xC0 0x0C)

**IP Paket Insasi:**
- Kaynak/hedef IP adreslerini degistirir
- Dogru IP baslik checksum hesaplar (16-bit one's complement)
- UDP kaynak/hedef portlarini degistirir
- TTL: 64, Don't Fragment bayragi
- IPv4 UDP checksum: 0 (IPv4 icin istege bagli)

### 5.4 Engelleme Listeleri

**Dosya:** `data/datasource/BlocklistDataSource.kt`

3 yerlesik engelleme listesi (assets klasorunde):
1. **adware** - Reklam domain'leri
2. **trackers** (Premium) - Izleyici domain'leri
3. **inapp_ads** - Uygulama ici reklam domain'leri

**Domain Eslestirme Mantigi:**
- Domain'leri normalize eder (kucuk harf, www. onekini kaldirir)
- Sorgu domain'ini VE tum ust domain'leri kontrol eder
  - Ornek: `tracker.example.com` → `example.com` → `com`

### 5.5 Uygulama Bazli Filtreleme

**Dosya:** `service/AppFilterManager.kt`

- `FilterMode`: ALL_APPS (varsayilan) veya SELECTED_APPS
- Kullanici uygulama bazinda VPN filtresi acabilir/kapatabilir
- `SharedPreferences` ile kalici depolama
- `getInstalledUserApps()` ile haric tutma durumlu uygulama listesi

### 5.6 VPN Durum Yonetimi

**Dosya:** `service/VpnControlManager.kt`

```kotlin
object VpnControlManager {
    val isVpnActive: StateFlow<Boolean>     // VPN aktif/pasif durumu
    val blockingStats: StateFlow<BlockingStats>  // Engelleme istatistikleri

    fun startVpn(context)   // Servisi baslat (Android 8+ icin startForegroundService)
    fun stopVpn(context)    // Servisi durdur
}
```

- Singleton pattern ile global erisim
- StateFlow ile reaktif durum guncellemesi
- Her 10 engellemede bildirim guncelleme

---

## 6. Adware Tespit Motoru

**Dosya:** `domain/AdwareDetector.kt`

### Tespit Yontemleri ve Puanlama

| Tespit | Puan | Kosul |
|--------|------|-------|
| Overlay Reklam Yetenegii | 30 | SYSTEM_ALERT_WINDOW + INTERNET |
| Otomatik Baslama | 20 | BOOT_COMPLETED + INTERNET + OVERLAY |
| Bilinen Reklam SDK'lari | 25x (maks 50) | 27+ bilinen SDK imzasi |
| Agresif Arka Plan | 15 | WAKE_LOCK + FOREGROUND_SERVICE + BATTERY_OPT + INTERNET + BOOT |
| Uygulama Yukleme Yetenegii | 20 | REQUEST_INSTALL_PACKAGES + INTERNET |
| Bilinen Adware Paketleri | 25 | Paket oneki eslesmesi |
| Supheli Izin Kombinasyonu | 15 | 4+ supheli izin bir arada |

### Bilinen Adware SDK Imzalari (27+)

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

### Bilinen Adware Paket Onekleri

```
com.cleanmaster, com.ksmobile, com.nqmobile, com.apus,
com.uc.browser, com.dianxinos, com.duapps, com.dolphin
```

### Suphe Esigi

- `suspicionScore >= 40` → Supheli
- `suspicionScore >= 70` → Yuksek derecede supheli

### Guvenlik Notlari

- Uygulamalari "adware" veya "malware" olarak ETIKETLEMEZ (hukuki risk)
- Tarafsiz dil kullanir: "supheli reklam davranisi tespit edildi"
- Guvenilir uygulamalari atlar (`PermissionClassifier.isTrustedApp()`)
- Sistem uygulamalarini atlar

---

## 7. Izin Siniflandirma ve Risk Analizi

**Dosya:** `domain/PermissionClassifier.kt`

### Yuksek Riskli Izinler (31 adet, Puan: 3)

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

### Orta Riskli Izinler (33 adet, Puan: 1)

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

### Guvenilir Uygulama Beyaz Listesi

**Tam Esleme:**
```
com.trustedgelabs.trustguard, com.trustedgelabs.ibans,
com.trustedgelabs.ghostnotify, com.cihan.ghostnotify, com.cihan.ibans
```

**Onek Eslesmesi:**
```
com.trustedgelabs.*, com.cihan.*
```

Guvenilir uygulamalar her zaman `RiskLevel.TRUSTED` ve 0 puan alir.

### Uygulama Risk Siniflandirmasi

```
Herhangi bir YUKSEK risk izni varsa → RiskLevel.HIGH
Aksi halde ORTA risk izni varsa    → RiskLevel.MEDIUM
Aksi halde                         → RiskLevel.LOW
```

### Guvenlik Puani Hesaplama (Dashboard)

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

## 8. Dosya Kurtarma Sistemi

**Dosya:** `data/datasource/MediaStoreRecoveryDataSource.kt`

### 9 Kurtarma Stratejisi

| # | Strateji | Aciklama | Android Surumu |
|---|----------|----------|----------------|
| 1 | MediaStore IS_TRASHED | Cop kutusundaki dosyalar | Android 11+ |
| 2 | Dosya Sistemi Cop Dizinleri | Fiziksel cop klasorleri | Tumu |
| 3 | MediaStore Yol-Tabanli Sorgular | Silinen yollardaki dosyalar | Tumu |
| 4 | .trashed- / .pending- Onekli | Gecici silme isaretli dosyalar | Tumu |
| 5 | Kucuk Resim Avciligi | Silinen fotograflarin thumbnail'lari | Tumu |
| 6 | .nomedia Gizli Dizinler | Galeriden gizlenmis dosyalar | Tumu |
| 7 | Uygulama Onbellek Medyasi | WhatsApp, Telegram, Signal, Instagram vs. | Tumu |
| 8 | Mesajlasma Uygulamasi Medyasi | Mesajlasma uygulamalarindaki medya | Tumu |
| 9 | Root Derin Tarama | Root erisimli dosya sistemi taramasi | Root gerekli |

### Tarama Modlari

- **Hizli (QUICK):** Strateji 1-4
- **Derin (DEEP):** Strateji 1-8
- **Root (ROOT):** Strateji 1-9 (root erisim gerektirir)

### Kurtarma Yontemleri

- MediaStore guncelleme (IS_TRASHED=0)
- Dosya sistemi kopyalama
- Root kopyalama (`su` komutu ile)
- Varsayilan hedef: `Pictures/TrustGuard_Recovered`

### Desteklenen Dosya Turleri

- **Foto:** JPEG, PNG, GIF, BMP, WebP
- **Video:** MP4, AVI, MKV, MOV, 3GP
- **Ses:** MP3, WAV, OGG, FLAC, AAC
- **Belge:** PDF, DOC, DOCX, TXT, XLS

---

## 9. Optimizasyon Motoru

**Dosya:** `data/datasource/OptimizationDataSource.kt`

### Tarama Kategorileri

| Kategori | Aciklama |
|----------|----------|
| SYSTEM_CACHE | Sistem onbellek dosyalari |
| APP_CACHE | Uygulama bazli onbellek |
| TEMP_FILES | Gecici dosyalar |
| APK_FILES | Indirilen APK dosyalari |
| THUMBNAILS | Kucuk resim onbellegi |
| EMPTY_FOLDERS | Bos klasorler |
| LARGE_FILE | 100MB ustu dosyalar |
| DUPLICATE_PHOTO | MD5 hash tabanli kopya fotograflar |
| EMAIL_CACHE | E-posta uygulama onbellekleri (Gmail, Outlook, Yahoo Mail) |

### Kopya Fotograf Tespiti

- Her dosyanin ilk 8KB + son 8KB + boyut ile MD5 hash olusturur
- Ayni hash'e sahip dosyalari gruplar
- Performans icin tam dosya hash'i yerine kismi hash kullanir

### Ilerleme Geri Bildirimi

- `onScanProgress` callback ile gercek zamanli ilerleme guncelleme

---

## 10. Guvenlik Onlemleri

### 12.1 Istisna Yonetimi Desenleri

**Sessiz Yakalama (Kritik Olmayan):**
```kotlin
try { builder.addDisallowedApplication(excluded) }
catch (_: Exception) {}
```

**Loglu Yakalama (Kritik):**
```kotlin
catch (e: Exception) {
    Log.e("TrustGuardVPN", "Failed to establish VPN", e)
}
```

**Yedek Degerlli Yakalama:**
```kotlin
val appName = try {
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
} catch (_: Exception) { packageName }
```

Tum yeni ekranlar (Wi-Fi, Pil, Depolama, Butunluk, Ag) try-catch ile sarilmistir. Hicbir ekran dogrudan cokme uretmez; kullaniciya basit aciklamalar gosterilir.

### 12.2 Esitlesme (Concurrency) Guvenligi

| Desen | Kullanim | Dosya |
|-------|----------|-------|
| `@Volatile` | Thread-safe boolean bayraklar | TrustGuardVpnService.kt |
| `synchronized` | Paylasilan kaynak korumasi | VPN cikis akimi yazimi |
| `MutableStateFlow` | Thread-safe gozlemlenebilir durum | VpnControlManager.kt |
| `SupervisorJob + Dispatchers.IO` | Coroutine yasam dongusu | TrustGuardVpnService.kt |
| `serviceScope.cancel()` | Temiz kaynak serbest birakma | onDestroy() |

### 12.3 Veri Dogrulama

**DNS Paket Dogrulama:**
- Minimum paket boyutu: 12 bayt
- Soru sayisi kontrolu: >= 1
- Offset sinir kontrolu: `offset + 4 > data.size`
- Label uzunluk kontrolu: `offset + labelLength + 1 > data.size`

**IP Paket Baslik Dogrulama:**
- IP surumu kontrolu: sadece IPv4 (version == 4)
- Protokol kontrolu: sadece UDP (protocol == 17)
- Port kontrolu: sadece DNS (destPort == 53)

**Girdi Temizleme:**
- Domain normalizasyonu: `lowercase().removePrefix("www.")`
- SharedPreferences deger dogrulama: `valueOf()` ile try-catch
- JSON ayristirma: `JSONObject` ile try-catch ve bos liste yedegi

### 12.4 Veri Koruma

**backup_rules.xml:**
- Bulut yedekleme icerik kurallari (bos - yedekleme devre disi)

**data_extraction_rules.xml:**
- Bulut yedekleme dislamalari: kok domain haric
- Cihaz aktarim dislamalari: kok domain haric
- Hassas uygulama verilerini bulut senkronizasyonundan korur

### 12.5 R8/ProGuard Kurallari

- Tum Compose siniflarini korur
- Material Icons Extended siniflarini korur
- Google Play Billing siniflarini korur
- Veri modelleri ve VPN Servis uygulamasini korur
- Kotlin Coroutines'i korur
- Release derlemelerinde log ciktilarini kaldirir (`Log.v`, `Log.d`, `Log.i`)
- Annotation attribute'larini ve ic siniflari korur

---

## 13. Android Izinleri

### Uygulama/Paket Tarama

| Izin | Amac | Tehlike Seviyesi |
|------|------|-----------------|
| `QUERY_ALL_PACKAGES` | Android 11+ tum yuklu uygulamalari taramak icin | Ozel |

### Wi-Fi ve Ag

| Izin | Amac |
|------|------|
| `ACCESS_WIFI_STATE` | Wi-Fi baglanti durumunu okuma |
| `ACCESS_NETWORK_STATE` | Ag baglanti durumunu okuma |
| `ACCESS_FINE_LOCATION` | Wi-Fi SSID okumak icin (Android 8.1+) |
| `ACCESS_COARSE_LOCATION` | Wi-Fi SSID okumak icin yedek |
| `INTERNET` | DNS sorgulari icin ag erisimi |

### On Plan Servisleri

| Izin | Amac |
|------|------|
| `FOREGROUND_SERVICE` | VPN servisi icin on plan bildirimi |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ ozel kullanim tipi |
| `POST_NOTIFICATIONS` | Kullaniciya bildirim gonderme |

### Uygulama Aktivite Izleme

| Izin | Amac |
|------|------|
| `PACKAGE_USAGE_STATS` | Uygulama ag aktivitesini izleme (Korunmus) |

### Dosya Kurtarma

| Izin | Amac | Android Surumu |
|------|------|----------------|
| `MANAGE_EXTERNAL_STORAGE` | Genis dosya erisimi | Android 11+ |
| `READ_MEDIA_IMAGES` | Goruntu dosyalarina erisim | Android 13+ |
| `READ_MEDIA_VIDEO` | Video dosyalarina erisim | Android 13+ |
| `READ_MEDIA_AUDIO` | Ses dosyalarina erisim | Android 13+ |
| `READ_EXTERNAL_STORAGE` | Eski depolama erisimi | maxSdkVersion=32 |

### Manifest Bilesenleri

| Bilesen | Tur | Izin | Ozellikler |
|---------|-----|------|------------|
| `MainActivity` | Activity | - | exported=true, portrait, LAUNCHER |
| `TrustGuardVpnService` | Service | BIND_VPN_SERVICE | exported=false, specialUse, always-on destekli |
| `NotificationAdDetector` | Service | BIND_NOTIFICATION_LISTENER_SERVICE | exported=false |

---

## 14. Yerellestirme

### Desteklenen Diller

| Dil | Klasor | String Sayisi |
|-----|--------|---------------|
| Ingilizce (varsayilan) | `values/strings.xml` | ~423 |
| Turkce | `values-tr/strings.xml` | ~423 (tam eslik) |

### String Kategorileri (28 kategori)

1. Uygulama (3) - app_name, company_name, by_company
2. Dashboard (5) - bolum basliklari
3. Trafik Isigi/Risk (4) - high_risk, medium_risk, safe, trusted
4. Uygulama Listesi (6)
5. Uygulama Karti (3)
6. Detay Ekrani (9)
7. Tarama (1)
8. Ayarlar (9)
9. DNS Engelleme (27)
10. Uygulama Bazli Filtreleme (7)
11. Bildirim Reklam Tespiti (6)
12. Dosya Kurtarma (35)
13. Kurtarma Tarama Modlari (6)
14. Kurtarma Kaynaklari (5)
15. Tarama Istatistikleri (5)
16. Adware Tespiti (13)
17. Premium Abonelik (25)
18. Optimizasyon (16)
19. Cop Kategorisi (5)
20. Kopyalar (3)
21. Buyuk Dosyalar (3)
22. Uygulama Onbellek (4)
23. E-posta (4)
24. Guvenilir Uygulama Seffafligi (11)
25. Uygulama Aktivite Izleme (14)
26. Wi-Fi Guvenlik (29)
27. Pil Analizi (25)
28. Depolama / Butunluk / Ag / Gunluk Limit / Premium V2 / Aile Kalkani (50+)

### Yerel Ayar Yonetimi

- `LocaleManager` sinifi ile dil degistirme
- `MainActivity` ve `TrustGuardApp`'da `attachBaseContext` ile uygulama

---

## 15. Tema ve Tasarim Sistemi

### Renk Paleti

**Birincil Renkler:**
| Renk | Hex | Kullanim |
|------|-----|----------|
| TrustGreen | #00E676 | Ana vurgu, guvenli durum |
| TrustTeal | #00BFA5 | Ikincil vurgu |

**Risk Trafik Isigi:**
| Renk | Hex | Kullanim |
|------|-----|----------|
| RiskRed | #FF1744 | Yuksek risk |
| RiskYellow | #FFD600 | Orta risk |
| RiskGreen | #00E676 | Dusuk risk |
| TrustedBlue | #448AFF | Guvenilir |

**Karanlik Tema:**
| Renk | Hex | Kullanim |
|------|-----|----------|
| DarkBackground | #0A0E14 | Ana arka plan |
| DarkSurface | #131A24 | Yuzey |
| DarkCard | #1E2A3A | Kart arka plani |

**Ozellik Renkleri:**
| Renk | Hex | Kullanim |
|------|-----|----------|
| SecurityBlue | - | Guvenlik araclari |
| WifiPurple | - | Wi-Fi ozelllikleri |
| BatteryGreen | - | Pil ozellikleri |
| StorageCyan | - | Depolama ozellikleri |
| IntegrityIndigo | - | Butunluk ozellikleri |
| NetworkTeal | - | Ag ozellikleri |
| OptimizationOrange | - | Optimizasyon ozellikleri |
| ProGold | #FFD700 | Premium/Pro rozetler |

**Metin Renkleri:**
| Renk | Hex | Kullanim |
|------|-----|----------|
| TextPrimary | #E8EAED | Birincil metin |
| TextSecondary | #9AA0A6 | Ikincil metin |

### Material 3 Entegrasyonu

- %100 karanlik tema
- Her ozellik kategorisi icin ozel vurgu renkleri
- `Theme.TrustGuard`: Material NoActionBar temasi

---

## 16. Derleme Yapilandirmasi

### build.gradle.kts (Uygulama)

```
compileSdk = 35
minSdk = 24
targetSdk = 35
versionCode = 5
versionName = "1.2.0"
applicationId = "com.trustedgelabs.trustguard"

// Release derleme
isMinifyEnabled = true    // R8 kucultme aktif
isShrinkResources = true  // Kaynak daraltma aktif
proguardFiles = [proguard-android-optimize.txt, proguard-rules.pro]

// Java/Kotlin
javaVersion = VERSION_11
jvmTarget = "11"
kotlinCompilerExtensionVersion = Kotlin 2.0.21
```

### gradle.properties

```
JVM Argumanlari: -Xmx2048m
Dosya Kodlamasi: UTF-8
AndroidX: Aktif
Kotlin Kod Stili: Resmi
Gecissiz R Sinifi: Aktif
```

### Imzalama Yapilandirmasi (Release)

- Anahtar Deposu: `release-key.jks`
- Anahtar Takma Adi: `trustguard`

---

## 17. Kaynak Dosyalari

### Cizimler (drawable/)

| Dosya | Aciklama |
|-------|----------|
| `ic_launcher_background.xml` | Gradyan arka plan (koyu lacivert) + tekno desen |
| `ic_launcher_foreground.xml` | Kalkan ikonu (yesil) |
| `ic_shield.xml` | Alternatif kalkan varyanti |

### Uyarlanabilir Ikonlar (mipmap-anydpi-v26/)

| Dosya | Aciklama |
|-------|----------|
| `ic_launcher.xml` | Uyarlanabilir ikon tanimi |
| `ic_launcher_round.xml` | Yuvarlak varyant |

### XML Yapilandirmalar (xml/)

| Dosya | Aciklama |
|-------|----------|
| `backup_rules.xml` | Bulut yedekleme devre disi |
| `data_extraction_rules.xml` | Veri cikarma kurallari (kok domain haric) |

---

## 18. Dosya Yapisi

```
app/src/main/java/com/trustedgelabs/trustguard/
|
├── TrustGuardApp.kt                    # Application sinifi
├── MainActivity.kt                      # Ana Activity
|
├── billing/
│   └── BillingManager.kt               # Google Play faturalandirma
|
├── data/
│   ├── datasource/
│   │   ├── PackageManagerDataSource.kt  # Uygulama tarama
│   │   ├── WifiSecurityDataSource.kt    # Wi-Fi guvenlik taramasi
│   │   ├── BatteryDataSource.kt         # Pil bilgileri
│   │   ├── StorageDataSource.kt         # Depolama analizi
│   │   ├── AppIntegrityDataSource.kt    # Yukleme kaynagi tespiti
│   │   ├── BlocklistDataSource.kt       # DNS engelleme listesi yukleme
│   │   ├── OptimizationDataSource.kt    # Cop dosya tarama motoru
│   │   └── MediaStoreRecoveryDataSource.kt  # 9 stratejili dosya kurtarma
│   │
│   ├── dns/
│   │   ├── DnsPacket.kt                 # DNS sorgu ayristirma
│   │   ├── DnsResponseBuilder.kt        # Engelleme yaniti olusturma
│   │   └── IpPacketBuilder.kt           # IP/UDP paket insasi
│   │
│   ├── model/
│   │   ├── AppInfo.kt                   # Uygulama veri modeli
│   │   ├── PermissionInfo.kt            # Izin modeli
│   │   ├── RiskLevel.kt                 # Risk seviyesi enum
│   │   ├── BatteryInfo.kt               # Pil bilgi modeli
│   │   ├── StorageInfo.kt               # Depolama modeli
│   │   ├── WifiSecurityInfo.kt          # Wi-Fi guvenlik modeli
│   │   ├── AppIntegrityInfo.kt          # Uygulama butunlugu modeli
│   │   ├── BlockingStats.kt             # Engelleme istatistikleri
│   │   ├── BlocklistInfo.kt             # Engelleme listesi metadata
│   │   ├── RecoverableFile.kt           # Kurtarilabilir dosya modeli
│   │   ├── CleanableFile.kt             # Temizlenebilir dosya modeli
│   │   └── JunkScanResult.kt            # Cop tarama sonucu
│   │
│   └── repository/
│       ├── AppRepositoryImpl.kt         # Uygulama veri deposu
│       ├── BlocklistRepositoryImpl.kt   # Domain engelleme yonetimi
│       └── BlockingStatsRepository.kt   # Engelleme istatistik deposu
|
├── domain/
│   ├── AdwareDetector.kt                # Adware tespit motoru
│   ├── PermissionClassifier.kt          # Izin risk siniflandirma
│   ├── TrustedAppTransparency.kt        # Guvenilir uygulama seffafligi
│   └── usecase/
│       ├── ScanAppsUseCase.kt           # Uygulama tarama kullanim senaryosu
│       └── AnalyzePermissionsUseCase.kt # Izin analizi kullanim senaryosu
|
├── service/
│   ├── TrustGuardVpnService.kt          # VPN servisi (DNS engelleme)
│   ├── VpnControlManager.kt             # VPN durum yonetimi
│   ├── VpnNotificationManager.kt        # VPN bildirimi
│   ├── AppFilterManager.kt              # Uygulama bazli filtreleme
│   └── NotificationAdDetector.kt        # Bildirim reklam tespiti
|
├── ui/
│   ├── components/
│   │   ├── SecurityScoreArc.kt          # Animasyonlu guvenlik puani arki
│   │   ├── TrafficLightRow.kt           # Risk trafik isigi satiri
│   │   ├── ScanProgressIndicator.kt     # Tarama ilerleme gostergesi
│   │   ├── AppCard.kt                   # Uygulama karti bileseni
│   │   ├── RiskBadge.kt                 # Risk rozeti
│   │   ├── ToolCard.kt                  # Arac karti + Grid + SectionHeader
│   │   ├── VpnStatusCard.kt             # VPN durum karti
│   │   ├── ProStatusBar.kt              # Pro baslik + QuickActionBar
│   │   └── DailyLimitDialog.kt          # Gunluk limit dialog
│   │
│   ├── navigation/
│   │   ├── Screen.kt                    # Ekran rotalari (sealed class)
│   │   └── NavGraph.kt                  # Navigasyon grafigi
│   │
│   ├── screens/
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt       # Ana ekran
│   │   │   └── DashboardViewModel.kt    # Ana ekran ViewModel
│   │   ├── applist/
│   │   │   ├── AppListScreen.kt
│   │   │   └── AppListViewModel.kt
│   │   ├── detail/
│   │   │   ├── DetailScreen.kt
│   │   │   └── DetailViewModel.kt
│   │   ├── blocking/
│   │   │   └── DnsBlockingScreen.kt
│   │   ├── recovery/
│   │   │   ├── RecoveryScreen.kt
│   │   │   └── RecoveryViewModel.kt
│   │   ├── adware/
│   │   │   ├── AdwareScreen.kt
│   │   │   └── AdwareViewModel.kt
│   │   ├── optimization/
│   │   │   ├── OptimizationScreen.kt
│   │   │   └── OptimizationViewModel.kt
│   │   ├── wifi/
│   │   │   └── WifiSecurityScreen.kt
│   │   ├── battery/
│   │   │   └── BatteryHealthScreen.kt
│   │   ├── storage/
│   │   │   └── StorageAnalyzerScreen.kt
│   │   ├── integrity/
│   │   │   └── AppIntegrityScreen.kt
│   │   ├── network/
│   │   │   └── NetworkMonitorScreen.kt
│   │   ├── premium/
│   │   │   └── PremiumScreen.kt
│   │   └── settings/
│   │       └── SettingsScreen.kt
│   │
│   └── theme/
│       ├── Color.kt                     # Renk tanimlari
│       ├── Theme.kt                     # Material 3 tema
│       └── Type.kt                      # Tipografi
|
└── util/
    ├── DailyLimitManager.kt             # Anti-tamper gunluk limit
    ├── RootDetector.kt                  # Root tespit + komut calistirma
    ├── PermissionDescriptions.kt        # Izin aciklamalari (EN/TR)
    ├── AppIconLoader.kt                 # Uygulama ikonu Compose donusturucu
    └── LocaleManager.kt                 # Dil yonetimi
```

---

## Belge Bilgileri

- **Olusturma Tarihi:** 29 Mart 2026
- **Uygulama Versiyonu:** 1.2.0
- **Gelistirici:** TrustEdge Labs
- **Belgeyi Olusturan:** TrustEdge Labs
