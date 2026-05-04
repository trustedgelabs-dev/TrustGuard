package com.trustedgelabs.trustguard.data.repository

import android.content.Context
import com.trustedgelabs.trustguard.data.datasource.MediaStoreRecoveryDataSource
import com.trustedgelabs.trustguard.data.model.RecoverableFile
import com.trustedgelabs.trustguard.data.model.ScanMode

interface RecoveryRepository {
    suspend fun scanFiles(mode: ScanMode): List<RecoverableFile>
    suspend fun recoverFile(file: RecoverableFile): Boolean
    fun getFreeRecoveriesLeft(): Int
    fun useRecovery()
    fun isPremium(): Boolean
}

class RecoveryRepositoryImpl(context: Context) : RecoveryRepository {

    val dataSource = MediaStoreRecoveryDataSource(context)
    private val prefs = context.getSharedPreferences("recovery_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RECOVERIES_USED = "recoveries_used"
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val MAX_FREE_RECOVERIES = 3
    }

    override suspend fun scanFiles(mode: ScanMode): List<RecoverableFile> {
        return dataSource.scanFiles(mode)
    }

    override suspend fun recoverFile(file: RecoverableFile): Boolean {
        return dataSource.recoverFile(file)
    }

    override fun getFreeRecoveriesLeft(): Int {
        return Int.MAX_VALUE
    }

    override fun useRecovery() {
        // Tum ozellikler ucretsiz - limit yok
    }

    override fun isPremium(): Boolean {
        return true
    }
}
