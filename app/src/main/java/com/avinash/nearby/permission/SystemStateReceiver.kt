package com.avinash.nearby.permission

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import com.avinash.nearby.utils.printLog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Created by Avinash Munnangi on 10/07/25.
 */
class SystemStateReceiver @Inject constructor(): BroadcastReceiver() {

    init {
        printLog("New SystemStateReceiver Created")
    }

    private val _systemStateUpdate = MutableSharedFlow<Unit>()
    val systemStateUpdate = _systemStateUpdate.asSharedFlow()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
            // Location providers have changed, check the new state
            // You can now react to the new state
            GlobalScope.launch { _systemStateUpdate.emit(Unit) }
            return
        }

        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            // Bluetooth state has changed, check the new state
            // You can now react to the new state
            GlobalScope.launch { _systemStateUpdate.emit(Unit) }
            _systemStateUpdate.tryEmit(Unit)
        }
    }
}