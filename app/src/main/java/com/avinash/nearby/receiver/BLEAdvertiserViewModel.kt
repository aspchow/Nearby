package com.avinash.nearby.receiver

import androidx.lifecycle.ViewModel
import com.avinash.nearby.receiver.manager.BLEAdvertiserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Created by Avinash Munnangi on 09/07/25.
 */
@HiltViewModel
class BLEAdvertiserViewModel @Inject constructor() : ViewModel() {

    private lateinit var manager: BLEAdvertiserManager
    val advertisingState by lazy {
        require(::manager.isInitialized) {
            "BLEAdvertiserManager is not initialized. Call onActivityCreated first."
        }
        manager.serviceMeta
    }

    fun onActivityCreated(manager: BLEAdvertiserManager) {
        this.manager = manager
    }

    fun startAdvertising() {
        // Logic to start advertising
        manager.startAdvertising("Jai BABU")
    }

    fun stopAdvertising() {
        // Logic to stop advertising
        manager.stopAdvertising()
    }
}