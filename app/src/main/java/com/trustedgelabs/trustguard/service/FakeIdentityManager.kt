package com.trustedgelabs.trustguard.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class FakeIdentity(
    val deviceModel: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val androidId: String = "",
    val imei: String = "",
    val macAddress: String = "",
    val serialNumber: String = "",
    val buildFingerprint: String = ""
)

class FakeIdentityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("fake_identity_prefs", Context.MODE_PRIVATE)
    private val _currentIdentity = MutableStateFlow(FakeIdentity())
    val currentIdentity: StateFlow<FakeIdentity> = _currentIdentity.asStateFlow()

    private val deviceModels = listOf(
        "Pixel 8 Pro" to "Google",
        "Pixel 7a" to "Google",
        "Galaxy S24 Ultra" to "Samsung",
        "Galaxy S23" to "Samsung",
        "Galaxy A54" to "Samsung",
        "OnePlus 12" to "OnePlus",
        "OnePlus Nord 3" to "OnePlus",
        "Xiaomi 14" to "Xiaomi",
        "Redmi Note 13 Pro" to "Xiaomi",
        "POCO F6 Pro" to "Xiaomi",
        "Xperia 1 V" to "Sony",
        "Find X7 Ultra" to "OPPO",
        "V30 Pro" to "vivo",
        "Magic 6 Pro" to "Honor",
        "Edge 50 Pro" to "Motorola"
    )

    private val androidVersions = listOf(
        "14" to "UP1A.231005.007",
        "13" to "TQ3A.230901.001",
        "12" to "SP1A.210812.016",
        "14" to "AP2A.240605.024",
        "13" to "TP1A.220624.014"
    )

    init {
        loadSavedIdentity()
    }

    private fun loadSavedIdentity() {
        val saved = prefs.getString("saved_identity", null)
        if (saved != null) {
            _currentIdentity.value = deserializeIdentity(saved)
        }
    }

    fun generateNewIdentity(): FakeIdentity {
        val (model, manufacturer) = deviceModels.random()
        val (version, build) = androidVersions.random()

        val identity = FakeIdentity(
            deviceModel = model,
            manufacturer = manufacturer,
            androidVersion = version,
            androidId = generateHexString(16),
            imei = generateImei(),
            macAddress = generateMacAddress(),
            serialNumber = generateSerialNumber(),
            buildFingerprint = "$manufacturer/$model:$version/$build:user/release-keys"
        )

        _currentIdentity.value = identity
        saveIdentity(identity)
        return identity
    }

    fun clearIdentity() {
        _currentIdentity.value = FakeIdentity()
        prefs.edit().remove("saved_identity").apply()
    }

    fun hasActiveIdentity(): Boolean {
        return _currentIdentity.value.androidId.isNotEmpty()
    }

    private fun generateImei(): String {
        val tac = listOf("35", "86", "01", "35").random()
        val digits = StringBuilder(tac)
        repeat(12 - tac.length) {
            digits.append((0..9).random())
        }
        // Calculate Luhn check digit
        val checkDigit = calculateLuhnCheckDigit(digits.toString())
        digits.append(checkDigit)
        return digits.toString()
    }

    private fun calculateLuhnCheckDigit(number: String): Int {
        var sum = 0
        for (i in number.indices) {
            var digit = number[number.length - 1 - i].digitToInt()
            if (i % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }

    private fun generateMacAddress(): String {
        val prefixes = listOf("00:1A:2B", "AC:DE:48", "F4:CE:46", "D8:BB:C1", "38:F9:D3")
        val prefix = prefixes.random()
        val suffix = (1..3).joinToString(":") {
            String.format("%02X", (0..255).random())
        }
        return "$prefix:$suffix"
    }

    private fun generateSerialNumber(): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..12).map { chars.random() }.joinToString("")
    }

    private fun generateHexString(length: Int): String {
        val chars = "0123456789abcdef"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun saveIdentity(identity: FakeIdentity) {
        val serialized = listOf(
            identity.deviceModel,
            identity.manufacturer,
            identity.androidVersion,
            identity.androidId,
            identity.imei,
            identity.macAddress,
            identity.serialNumber,
            identity.buildFingerprint
        ).joinToString("|||")
        prefs.edit().putString("saved_identity", serialized).apply()
    }

    private fun deserializeIdentity(data: String): FakeIdentity {
        val parts = data.split("|||")
        if (parts.size < 8) return FakeIdentity()
        return FakeIdentity(
            deviceModel = parts[0],
            manufacturer = parts[1],
            androidVersion = parts[2],
            androidId = parts[3],
            imei = parts[4],
            macAddress = parts[5],
            serialNumber = parts[6],
            buildFingerprint = parts[7]
        )
    }
}
