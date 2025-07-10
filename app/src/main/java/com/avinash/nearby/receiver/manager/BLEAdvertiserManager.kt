package com.avinash.nearby.receiver.manager

import android.app.Activity
import android.content.Intent
import android.os.Build
import com.avinash.nearby.receiver.service.AdvertisingServiceRepository
import com.avinash.nearby.receiver.service.BLEAdvertiseService
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

/**
 * Created by Avinash Munnangi on 03/07/25.
 */

@ActivityScoped
class BLEAdvertiserManager @Inject constructor(
    private val activity: Activity,
    serviceRepository: AdvertisingServiceRepository
) {
    val serviceMeta = serviceRepository.serviceMeta

    /**
     * Starts BLE advertising with the given device name.
     * @param name The name of the device to advertise.
     */
    fun startAdvertising(name: String) {
        val serviceIntent = Intent(activity, BLEAdvertiseService::class.java).apply {
            action = BLEAdvertiseService.ACTION_START
            putExtra(BLEAdvertiseService.BLE_NAME, name)
        }
        // Implementation for starting BLE advertising
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(
                serviceIntent
            )
        } else {
            activity.startService(
                serviceIntent
            )
        }

    }

    fun stopAdvertising() {
        val serviceIntent = Intent(activity, BLEAdvertiseService::class.java).apply {
            action = BLEAdvertiseService.ACTION_STOP
        }
        // Implementation for stopping BLE advertising
        activity.stopService(
            // Intent to stop the advertising service
            serviceIntent
        )
    }
}