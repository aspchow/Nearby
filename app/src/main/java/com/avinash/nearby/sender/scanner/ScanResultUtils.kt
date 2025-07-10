package com.avinash.nearby.sender.scanner

import android.Manifest
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission
import com.avinash.nearby.sender.model.BLEDevice

/**
 * Created by Avinash Munnangi on 01/07/25.
 */

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun ScanResult?.getBLEDevice(): BLEDevice? {
    if (this == null) return null
    return BLEDevice(
        device.name ?: return null
    )
}