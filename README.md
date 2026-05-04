# TrustGuard

Android için kapsamlı güvenlik ve gizlilik uygulaması.

## İndir

**[Releases sayfasından en son APK'yı indir](../../releases/latest)**

> Kurulum: APK'yı indirdikten sonra Android ayarlarından "Bilinmeyen kaynaklardan yükleme"ye izin ver ve dosyayı aç.

---

## Özellikler

- **VPN & DNS Engelleme** — Reklam ve tracker'ları VPN tabanlı DNS filtresiyle engelle
- **Uygulama Güvenliği** — Yüklü uygulamaların izinlerini analiz et, risk seviyesini gör
- **Güvenlik Duvarı** — Uygulamaların internet erişimini kontrol et
- **Adware Tespiti** — Reklam yazılımı barındıran uygulamaları tespit et
- **Gizlilik Araçları** — Sahte kimlik, uygulama kilitleme, gizli kasa
- **Dosya Kurtarma** — Silinen dosyaları geri getir
- **Sistem Optimizasyonu** — Pil ve depolama analizi
- **Şişkinlik Yazılımı Tespiti** — Ön yüklü gereksiz uygulamaları tespit et
- **Paket Dinleyici** — Ağ trafiğini izle
- **Ebeveyn Kontrolü** — Uygulama zaman sınırları ve içerik filtresi
- **Wi-Fi Güvenlik Analizi** — Bağlı ağın güvenliğini kontrol et

## Teknik Bilgiler

| | |
|---|---|
| Platform | Android 7.0+ (API 24) |
| Dil | Kotlin |
| UI | Jetpack Compose |
| Mimari | MVVM + Clean Architecture |
| Paket | `com.trustedgelabs.trustguard` |

## Geliştirme Ortamı

### Gereksinimler
- Android Studio Hedgehog veya üzeri
- JDK 17
- Android SDK 35

### Kurulum

```bash
git clone https://github.com/YOUR_USERNAME/TrustGuard.git
cd TrustGuard
cp local.properties.example local.properties
# local.properties dosyasını kendi SDK yolunla düzenle
```

### Release Build (isteğe bağlı)

Release APK almak için `local.properties` dosyasına kendi keystore bilgilerini ekle:

```
RELEASE_STORE_FILE=../your-key.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

```bash
./gradlew assembleRelease
```

## Lisans

Bu proje MIT lisansı ile lisanslanmıştır.

---

Geliştirici: **TrustEdge Labs**
