package com.avinash.nearby.permission

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

    private val _systemStateUpdate = MutableSharedFlow<String>()
    val systemStateUpdate = _systemStateUpdate.asSharedFlow()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        GlobalScope.launch { _systemStateUpdate.emit(action) }
    }
}