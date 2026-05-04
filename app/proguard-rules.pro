# ═══════════════════════════════════════════════
#  TrustGuard ProGuard / R8 Rules
# ═══════════════════════════════════════════════

# ── Keep Compose ──
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ── Keep Material Icons Extended (used dynamically) ──
-keep class androidx.compose.material.icons.** { *; }

# ── Google Play Billing ──
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── Keep data classes (used with StateFlow) ──
-keep class com.trustedgelabs.trustguard.data.model.** { *; }
-keep class com.trustedgelabs.trustguard.domain.** { *; }
-keep class com.trustedgelabs.trustguard.util.AppActivityTracker$** { *; }
-keep class com.trustedgelabs.trustguard.billing.BillingManager { *; }

# ── Keep VPN Service ──
-keep class com.trustedgelabs.trustguard.service.TrustGuardVpnService { *; }

# ── Kotlin Coroutines ──
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ── Kotlin Serialization (if used later) ──
-keepattributes *Annotation*
-keepattributes InnerClasses

# ── Remove logging in release ──
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
