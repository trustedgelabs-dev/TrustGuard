# Contributing to TrustGuard

Thank you for your interest in contributing. TrustGuard is an open-source privacy tool and contributions are welcome.

---

## Getting Started

1. Fork the repository at https://github.com/trustedgelabs-dev/TrustGuard
2. Clone your fork (replace `<your-github-username>` with your GitHub handle):
   ```bash
   git clone https://github.com/<your-github-username>/TrustGuard.git
   cd TrustGuard
   ```
3. Copy the properties template:
   ```bash
   cp local.properties.example local.properties
   # Edit local.properties with your Android SDK path
   ```
4. Open in Android Studio and build:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Guidelines

### Privacy First
Any contribution must not introduce data collection, analytics, remote logging, or external network calls that are not essential and transparent.

### No Monetization
TrustGuard is free and open source. Contributions must not add payment flows, ads, or premium feature gating.

### Keep It Local
Features should operate on-device. If a feature requires a remote server, it must be clearly documented, optional, and user-controlled.

---

## Submitting a Pull Request

1. Create a branch: `git checkout -b feature/your-feature`
2. Make your changes
3. Test on a real device if possible
4. Submit a pull request with a clear description of what changed and why

---

## Reporting Bugs

Open a [GitHub Issue](../../issues) with:
- TrustGuard version
- Android version and device
- Steps to reproduce
- Expected vs actual behavior

For security vulnerabilities, see [SECURITY.md](SECURITY.md) — do not open a public issue.

---

## License

By contributing, you agree that your contributions will be licensed under the **GNU General Public License v3.0**.
