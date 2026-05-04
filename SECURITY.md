# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in TrustGuard, please report it responsibly.

**Do not open a public GitHub issue for security vulnerabilities.**

### How to Report

Send a description of the vulnerability to:

**security@trustedgelabs.dev**

Include the following in your report:
- A description of the vulnerability
- Steps to reproduce
- Potential impact
- TrustGuard version affected

### What to Expect

- We will acknowledge your report within 72 hours.
- We will investigate and aim to release a fix within 14 days for critical issues.
- We will credit you in the release notes if you wish.

### Scope

The following are in scope:

- TrustGuard Android app (`com.trustedgelabs.trustguard`)
- Source code in this repository

The following are out of scope:

- Third-party Android system vulnerabilities
- Issues requiring physical access to an unlocked device
- Issues in older versions that are fixed in the latest release

### No Bug Bounty

No bug bounty or financial reward is currently offered. We are an open-source project maintained by a small team.

---

## Security Design Notes

- TrustGuard does not communicate with any remote server during normal operation.
- The VPN service operates entirely on-device.
- No user data is collected, stored remotely, or transmitted.
- The app does not have a backend infrastructure to exploit.
