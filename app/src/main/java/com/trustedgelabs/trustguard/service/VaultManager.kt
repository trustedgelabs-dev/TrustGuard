package com.trustedgelabs.trustguard.service

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class VaultFile(
    val id: String,
    val originalName: String,
    val encryptedPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val addedTimestamp: Long
)

class VaultManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
    private val vaultDir: File = File(context.filesDir, "vault").also { it.mkdirs() }

    companion object {
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_SALT = "password_salt"
        private const val KEY_FILES = "vault_files"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PBKDF2_ITERATIONS = 100_000
    }

    fun isVaultSetup(): Boolean = prefs.getString(KEY_PASSWORD_HASH, null) != null

    fun setupVault(password: String): Boolean {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)
        prefs.edit()
            .putString(KEY_PASSWORD_HASH, hash.toHex())
            .putString(KEY_SALT, salt.toHex())
            .apply()
        return true
    }

    fun verifyPassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val salt = prefs.getString(KEY_SALT, null)?.hexToBytes() ?: return false
        val hash = hashPassword(password, salt)
        return hash.toHex() == storedHash
    }

    fun encryptAndStore(uri: Uri, fileName: String, mimeType: String, password: String): VaultFile? {
        return try {
            val salt = prefs.getString(KEY_SALT, null)?.hexToBytes() ?: return null
            val key = deriveKey(password, salt)

            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val plainBytes = inputStream.readBytes()
            inputStream.close()

            val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val encryptedBytes = cipher.doFinal(plainBytes)

            val fileId = System.currentTimeMillis().toString() + "_" + SecureRandom().nextInt(10000)
            val encFile = File(vaultDir, "$fileId.enc")

            FileOutputStream(encFile).use { fos ->
                fos.write(iv)
                fos.write(encryptedBytes)
            }

            val vaultFile = VaultFile(
                id = fileId,
                originalName = fileName,
                encryptedPath = encFile.absolutePath,
                mimeType = mimeType,
                sizeBytes = plainBytes.size.toLong(),
                addedTimestamp = System.currentTimeMillis()
            )

            saveFileEntry(vaultFile)
            vaultFile
        } catch (e: Exception) {
            android.util.Log.e("VaultManager", "Encrypt failed", e)
            null
        }
    }

    fun decryptFile(vaultFile: VaultFile, password: String): ByteArray? {
        return try {
            val salt = prefs.getString(KEY_SALT, null)?.hexToBytes() ?: return null
            val key = deriveKey(password, salt)

            val encFile = File(vaultFile.encryptedPath)
            if (!encFile.exists()) return null

            val fileBytes = FileInputStream(encFile).readBytes()
            val iv = fileBytes.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedBytes = fileBytes.copyOfRange(GCM_IV_LENGTH, fileBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            cipher.doFinal(encryptedBytes)
        } catch (e: Exception) {
            android.util.Log.e("VaultManager", "Decrypt failed", e)
            null
        }
    }

    fun deleteFile(vaultFile: VaultFile) {
        File(vaultFile.encryptedPath).delete()
        removeFileEntry(vaultFile.id)
    }

    fun getVaultFiles(): List<VaultFile> {
        val data = prefs.getString(KEY_FILES, "") ?: ""
        if (data.isBlank()) return emptyList()

        return data.split("|||").mapNotNull { entry ->
            val parts = entry.split(":::")
            if (parts.size >= 6) {
                VaultFile(
                    id = parts[0],
                    originalName = parts[1],
                    encryptedPath = parts[2],
                    mimeType = parts[3],
                    sizeBytes = parts[4].toLongOrNull() ?: 0,
                    addedTimestamp = parts[5].toLongOrNull() ?: 0
                )
            } else null
        }
    }

    fun getVaultSizeBytes(): Long = getVaultFiles().sumOf { it.sizeBytes }

    private fun saveFileEntry(vaultFile: VaultFile) {
        val existing = prefs.getString(KEY_FILES, "") ?: ""
        val entry = "${vaultFile.id}:::${vaultFile.originalName}:::${vaultFile.encryptedPath}:::${vaultFile.mimeType}:::${vaultFile.sizeBytes}:::${vaultFile.addedTimestamp}"
        val updated = if (existing.isBlank()) entry else "$existing|||$entry"
        prefs.edit().putString(KEY_FILES, updated).apply()
    }

    private fun removeFileEntry(id: String) {
        val data = prefs.getString(KEY_FILES, "") ?: ""
        val entries = data.split("|||").filter { !it.startsWith("$id:::") }
        prefs.edit().putString(KEY_FILES, entries.joinToString("|||")).apply()
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        return factory.generateSecret(spec).encoded
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        val len = length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
        }
        return data
    }
}
