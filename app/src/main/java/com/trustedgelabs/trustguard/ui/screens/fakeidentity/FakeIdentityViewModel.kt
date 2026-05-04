package com.trustedgelabs.trustguard.ui.screens.fakeidentity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.trustedgelabs.trustguard.service.FakeIdentityManager
import kotlinx.coroutines.flow.StateFlow

class FakeIdentityViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = FakeIdentityManager(application)
    val identity: StateFlow<com.trustedgelabs.trustguard.service.FakeIdentity> = manager.currentIdentity

    fun hasActiveIdentity(): Boolean = manager.hasActiveIdentity()

    fun generateNew() {
        manager.generateNewIdentity()
    }

    fun clear() {
        manager.clearIdentity()
    }
}
