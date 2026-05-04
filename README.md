# TrustGuard

**TrustGuard is a free, open-source, local-first Android privacy and security toolkit.
It is not monetized, does not include ads, does not require an account, and is not distributed through Google Play.**

---

## Download

**[Download the latest APK from Releases](../../releases/latest)**

> Installation: Enable "Install from unknown sources" in Android settings, then open the APK.

---

## Privacy & Monetization

TrustGuard is completely free and open source.

- No ads
- No subscriptions
- No in-app purchases
- No account required
- No analytics
- No personal data collection or selling
- No remote monitoring dashboard
- No cloud sync

All processing happens locally on your device.

---

## What TrustGuard Does Not Do

- It does not spy on users.
- It does not upload browsing history.
- It does not read private messages.
- It does not hide itself from the user.
- It does not provide covert parental surveillance.
- It does not sell or share any data.
- It does not send traffic to a remote VPN server.

---

## Features

- **App Permission Analyzer** — Inspect installed apps and understand their permission risk levels
- **Local VPN-based DNS Blocker** — Block ads and trackers via local DNS filtering (no remote VPN server)
- **Firewall** — Control which apps can access the internet
- **Wi-Fi Security Checker** — Analyze the security of connected networks
- **Suspicious App Indicators** — Detect adware and bloatware patterns
- **Device Hygiene Checks** — Root detection, USB debug status, unknown sources
- **Local Network Activity Monitor** — Observe network connections on device
- **Privacy Tools** — App vault, fake identity generator, app locking
- **File Recovery** — Recover deleted files from local storage
- **System Optimization** — Battery and storage analysis
- **Parental Controls** — App time limits and content filtering (transparent, user-controlled)
- **Family Shield** — Configurable per-profile restrictions

---

## Technical Details

| | |
|---|---|
| Platform | Android 7.0+ (API 24) |
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Package | `com.trustedgelabs.trustguard` |
| Distribution | GitHub Releases only |

---

## Building from Source

### Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 35

### Setup

```bash
git clone https://github.com/trustedgelabs-dev/TrustGuard.git
cd TrustGuard
cp local.properties.example local.properties
# Edit local.properties with your SDK path
./gradlew assembleDebug
```

See [local.properties.example](local.properties.example) for signing configuration.

---

## Verifying the APK

You can verify the integrity of the downloaded APK using its SHA-256 checksum published in each [release](../../releases).

```bash
# Linux / macOS
sha256sum app-release.apk

# Windows (PowerShell)
certutil -hashfile app-release.apk SHA256
```

---

## Repository Files

| File | Purpose |
|---|---|
| [PERMISSIONS.md](PERMISSIONS.md) | Explanation of every Android permission requested |
| [PRIVACY.md](PRIVACY.md) | Full privacy policy |
| [SECURITY.md](SECURITY.md) | How to report vulnerabilities |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to contribute |
| [CHANGELOG.md](CHANGELOG.md) | Version history |

---

## License

This project is licensed under the **GNU General Public License v3.0**.
See [LICENSE](LICENSE) for details.

Forks and derivatives must remain open source under the same license.

---

Developer: **TrustEdge Labs**
