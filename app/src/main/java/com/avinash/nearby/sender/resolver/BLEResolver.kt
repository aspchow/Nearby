package com.avinash.nearby.sender.resolver

import com.avinash.nearby.sender.model.BLEDevice
import com.avinash.nearby.sender.model.BLEResolvedDevice
import com.avinash.nearby.utils.printLog
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Created by Avinash Munnangi on 01/07/25.
 */
@ActivityRetainedScoped
class BLEResolver @Inject constructor() {

    private val resolvedDevices = hashMapOf<String, BLEResolvedDevice>()

    suspend fun resolveBLEDevices(bleDevices: List<BLEDevice>): List<BLEResolvedDevice> {
        val alreadyCachedDevices = bleDevices.filter { it.name in resolvedDevices }
        val devicesToGetFromRemote = bleDevices - alreadyCachedDevices.toSet()
        printLog("Requested devices: ${bleDevices.size}, Already cached: ${alreadyCachedDevices.size}, Remote devices: ${devicesToGetFromRemote.size}")
        return resolveBLEDeviceRemote(devicesToGetFromRemote) + alreadyCachedDevices.map { resolvedDevices[it.name]!! }
    }

    private suspend fun resolveBLEDeviceRemote(bleDevices: List<BLEDevice>): List<BLEResolvedDevice> {
        delay(1000L)
        return bleDevices.map {
            val resolvedDevice = BLEResolvedDevice(
                deviceName = it.name + "1",
                imageUrl = "https://example.com/${it.name}.png",
                bleId = it.name
            )
            resolvedDevices[it.name] = resolvedDevice
            resolvedDevice
        }
    }

}