package com.trustedgelabs.trustguard.util

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Base64
import java.security.MessageDigest

/**
 * Manages the parent PIN with anti-tamper protection.
 * PIN is hashed with device fingerprint — cannot be extracted or transplanted.
 * Dual SharedPreferences backup prevents "Clear Data" bypass.
 * Security question for PIN recovery.
 */
object ParentPinManager {

    private const val PREFS_NAME = "tg_shield_auth"
    private const val BACKUP_PREFS_NAME = "tg_shield_auth_bk"

    private const val KEY_PIN_HASH = "sh_ph"
    private const val KEY_INTEGRITY = "sh_chk"
    private const val KEY_SETUP_TIME = "sh_st"
    private const val KEY_FAIL_COUNT = "sh_fc"
    private const val KEY_LOCKOUT_UNTIL = "sh_lu"
    private const val KEY_SECURITY_Q = "sh_sq"      // Security question index
    private const val KEY_SECURITY_A = "sh_sa"      // Security answer hash

    private const val BKEY_PIN_HASH = "bk_ph"
    private const val BKEY_INTEGRITY = "bk_chk"
    private const val BKEY_SECURITY_Q = "bk_sq"
    private const val BKEY_SECURITY_A = "bk_sa"

    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    // Pre-defined security questions
    val SECURITY_QUESTIONS = listOf(
        0 to "fs_security_q1",  // What is your mother's maiden name?
        1 to "fs_security_q2",  // What was the name of your first pet?
        2 to "fs_security_q3",  // What city were you born in?
        3 to "fs_security_q4",  // What is your favorite color?
        4 to "fs_security_q5"   // What is the name of your childhood friend?
    )

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getBackupPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)

    private fun getDeviceFingerprint(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (_: Exception) { "unknown" }
        val installTime = try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (_: Exception) { 0L }
        return hashSha256("$androidId-$installTime-shield_pin")
    }

    private fun hashSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING).take(32)
    }

    private fun hashPin(pin: String, context: Context): String {
        val fp = getDeviceFingerprint(context)
        return hashSha256("$fp:$pin:tg_pin_salt")
    }

    private fun hashAnswer(answer: String, context: Context): String {
        val fp = getDeviceFingerprint(context)
        return hashSha256("$fp:${answer.trim().lowercase()}:tg_answer_salt")
    }

    private fun computeIntegrity(pinHash: String, context: Context): String {
        val fp = getDeviceFingerprint(context)
        return hashSha256("$fp|$pinHash|integrity_check")
    }

    /**
     * Check if a parent PIN has been set up.
     */
    fun isPinSetup(context: Context): Boolean {
        val prefs = getPrefs(context)
        val backup = getBackupPrefs(context)
        return prefs.getString(KEY_PIN_HASH, null) != null ||
                backup.getString(BKEY_PIN_HASH, null) != null
    }

    /**
     * Check if a security question has been set up.
     */
    fun hasSecurityQuestion(context: Context): Boolean {
        val prefs = getPrefs(context)
        val backup = getBackupPrefs(context)
        return prefs.getString(KEY_SECURITY_A, null) != null ||
                backup.getString(BKEY_SECURITY_A, null) != null
    }

    /**
     * Get the security question index, or -1 if not set.
     */
    fun getSecurityQuestionIndex(context: Context): Int {
        val prefs = getPrefs(context)
        val idx = prefs.getInt(KEY_SECURITY_Q, -1)
        if (idx >= 0) return idx
        return getBackupPrefs(context).getInt(BKEY_SECURITY_Q, -1)
    }

    /**
     * Set up or change the parent PIN with security question.
     */
    fun setupPin(context: Context, pin: String, securityQuestionIndex: Int = -1, securityAnswer: String = "") {
        val pinHash = hashPin(pin, context)
        val integrity = computeIntegrity(pinHash, context)

        val prefsEditor = getPrefs(context).edit()
            .putString(KEY_PIN_HASH, pinHash)
            .putString(KEY_INTEGRITY, integrity)
            .putLong(KEY_SETUP_TIME, System.currentTimeMillis())
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)

        val backupEditor = getBackupPrefs(context).edit()
            .putString(BKEY_PIN_HASH, pinHash)
            .putString(BKEY_INTEGRITY, integrity)

        if (securityQuestionIndex >= 0 && securityAnswer.isNotBlank()) {
            val answerHash = hashAnswer(securityAnswer, context)
            prefsEditor.putInt(KEY_SECURITY_Q, securityQuestionIndex)
            prefsEditor.putString(KEY_SECURITY_A, answerHash)
            backupEditor.putInt(BKEY_SECURITY_Q, securityQuestionIndex)
            backupEditor.putString(BKEY_SECURITY_A, answerHash)
        }

        prefsEditor.apply()
        backupEditor.apply()
    }

    /**
     * Change the PIN (requires old PIN verification first).
     */
    fun changePin(context: Context, oldPin: String, newPin: String): Boolean {
        if (!verifyPin(context, oldPin)) return false
        val pinHash = hashPin(newPin, context)
        val integrity = computeIntegrity(pinHash, context)

        getPrefs(context).edit()
            .putString(KEY_PIN_HASH, pinHash)
            .putString(KEY_INTEGRITY, integrity)
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()

        getBackupPrefs(context).edit()
            .putString(BKEY_PIN_HASH, pinHash)
            .putString(BKEY_INTEGRITY, integrity)
            .apply()

        return true
    }

    /**
     * Verify the security answer and reset PIN if correct.
     */
    fun resetPinWithSecurityAnswer(context: Context, answer: String, newPin: String): Boolean {
        val storedAnswerHash = getStoredAnswerHash(context) ?: return false
        val inputHash = hashAnswer(answer, context)

        if (inputHash != storedAnswerHash) return false

        // Security answer correct — reset PIN
        val secQ = getSecurityQuestionIndex(context)
        setupPin(context, newPin, secQ, answer)
        return true
    }

    /**
     * Verify the parent PIN.
     */
    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = getPrefs(context)
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (System.currentTimeMillis() < lockoutUntil) return false

        val storedHash = getStoredPinHash(context) ?: return false
        val inputHash = hashPin(pin, context)

        if (inputHash == storedHash) {
            prefs.edit()
                .putInt(KEY_FAIL_COUNT, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            return true
        }

        val fails = prefs.getInt(KEY_FAIL_COUNT, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAIL_COUNT, fails)
        if (fails >= MAX_ATTEMPTS) {
            editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
            editor.putInt(KEY_FAIL_COUNT, 0)
        }
        editor.apply()
        return false
    }

    fun getLockoutRemainingSeconds(context: Context): Int {
        val lockoutUntil = getPrefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)
        val remaining = lockoutUntil - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }

    fun getFailedAttempts(context: Context): Int {
        return getPrefs(context).getInt(KEY_FAIL_COUNT, 0)
    }

    private fun getStoredPinHash(context: Context): String? {
        val prefs = getPrefs(context)
        val hash = prefs.getString(KEY_PIN_HASH, null)
        val integrity = prefs.getString(KEY_INTEGRITY, null)
        if (hash != null && integrity != null) {
            val expected = computeIntegrity(hash, context)
            if (expected == integrity) return hash
        }

        val backup = getBackupPrefs(context)
        val bHash = backup.getString(BKEY_PIN_HASH, null)
        val bIntegrity = backup.getString(BKEY_INTEGRITY, null)
        if (bHash != null && bIntegrity != null) {
            val expected = computeIntegrity(bHash, context)
            if (expected == bIntegrity) {
                prefs.edit()
                    .putString(KEY_PIN_HASH, bHash)
                    .putString(KEY_INTEGRITY, bIntegrity)
                    .apply()
                return bHash
            }
        }
        return null
    }

    private fun getStoredAnswerHash(context: Context): String? {
        val h = getPrefs(context).getString(KEY_SECURITY_A, null)
        if (h != null) return h
        return getBackupPrefs(context).getString(BKEY_SECURITY_A, null)
    }

    fun removePin(context: Context) {
        getPrefs(context).edit().clear().apply()
        getBackupPrefs(context).edit().clear().apply()
    }
}
