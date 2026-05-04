# Permissions

TrustGuard requests only the permissions required for its stated features. Every permission is explained below.

**TrustGuard does not secretly monitor users.**

---

## Network & VPN

### `INTERNET`
Used exclusively for local DNS filtering via the VPN interface. TrustGuard does not send personal data, analytics, telemetry, or usage history to TrustEdge Labs servers. DNS filtering may forward DNS queries to the configured upstream resolver (e.g. Cloudflare 1.1.1.1) as required for normal internet resolution.

### `BIND_VPN_SERVICE`
Required to create the local VPN tunnel used for DNS-based ad and tracker blocking. All traffic filtering happens on-device — no traffic is routed to TrustEdge Labs or any third-party VPN provider. Non-blocked DNS queries are forwarded to the configured upstream resolver as required for normal internet resolution.

### `CHANGE_NETWORK_STATE` / `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE`
Used to read Wi-Fi connection details for the security analysis feature. No data leaves the device.

### `CHANGE_WIFI_STATE`
Used only if the user explicitly interacts with Wi-Fi settings through the app.

---

## App Inspection

### `QUERY_ALL_PACKAGES`
Required to list all installed apps and analyze their declared permissions. Used exclusively for the permission risk analyzer and suspicious app detection features. No app data is transmitted externally.

### `REQUEST_DELETE_PACKAGES`
Used to trigger the standard Android uninstall dialog for apps flagged as bloatware. TrustGuard cannot uninstall apps silently — the user must confirm via the system dialog.

---

## Storage

### `READ_EXTERNAL_STORAGE` / `READ_MEDIA_*`
Used only for the file recovery scanner, which reads deleted file metadata from MediaStore. Files are scanned locally; no file content is uploaded anywhere.

### `MANAGE_EXTERNAL_STORAGE`
Required for deeper recovery scans. This is a sensitive permission — TrustGuard uses it only when the user explicitly initiates a scan.

---

## Notifications

### `POST_NOTIFICATIONS`
Used to display VPN status, active blocking summaries, and optional security alerts. Notifications are generated locally and not triggered by any remote service.

### `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`
Required to keep the VPN/firewall service alive while the app is in the background. This is a standard Android requirement for local VPN services.

---

## Device State

### `RECEIVE_BOOT_COMPLETED`
Optional. Used to restart the VPN or firewall service after a device reboot, only if the user has enabled auto-start in settings.

### `PACKAGE_USAGE_STATS`
Used for the screen time and parental control features, to measure how long the user spends in each app. This data never leaves the device.

### `SYSTEM_ALERT_WINDOW`
Used by the parental control feature to display a time-limit overlay when an app's daily usage limit is reached. This is always visible and user-controlled.

---

## Accessibility

### `BIND_ACCESSIBILITY_SERVICE`
Used only if the user enables the optional app-blocking feature. This service detects when a blocked app is opened and shows an overlay. It does not read screen content or transmit any data.

---

## Notes

- All permissions are used on-device only.
- No data collected through these permissions is transmitted externally.
- No permission is used for advertising, analytics, or tracking.
- Sensitive permissions (storage, accessibility, VPN) are only activated when the user explicitly enables the corresponding feature.
