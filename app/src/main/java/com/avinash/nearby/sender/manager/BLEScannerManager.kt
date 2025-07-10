package com.avinash.nearby.sender.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.avinash.nearby.sender.model.BLEError
import com.avinash.nearby.sender.model.BLEResolvedDevice
import com.avinash.nearby.sender.model.BLEResult
import com.avinash.nearby.sender.resolver.BLEResolver
import com.avinash.nearby.sender.scanner.BLEDeviceScanner
import com.avinash.nearby.utils.emitOnChuckedOrDebounce
import com.avinash.nearby.utils.printLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Avinash Munnangi on 01/07/25.
 */
class BLEScannerManager @Inject constructor(
    private val bleDeviceScanner: BLEDeviceScanner,
    private val bleResolver: BLEResolver,
    @ApplicationContext private val context: Context
) {
    private val _bleResolvedDevices = MutableStateFlow(emptyList<BLEResolvedDevice>())
    val bleResolvedDevices = _bleResolvedDevices.asStateFlow()

    val scanningState = bleDeviceScanner.scanningState

    init {
        GlobalScope.launch {
            collectBLEDevicesFromScanner()
        }
    }

    private suspend fun collectBLEDevicesFromScanner() {
        bleDeviceScanner.bleDevices
            .collect { bleDevices ->
                printLog("Collected BLE Devices: Total $bleDevices")
                _bleResolvedDevices.update { resolvedDevices ->
                    val acc =
                        resolvedDevices + bleResolver.resolveBLEDevices(bleDevices = bleDevices)
                    acc.distinct()
                }
            }
    }

    fun startScanning(): BLEResult<Boolean> {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return BLEResult.failure(error = BLEError.PermissionDenied(Manifest.permission.BLUETOOTH_SCAN))
        }
        bleDeviceScanner.scanDevices()
        return BLEResult.success(true)
    }

    fun stopScanning(): BLEResult<Boolean> {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return BLEResult.failure(error = BLEError.PermissionDenied(Manifest.permission.BLUETOOTH_SCAN))
        }
        bleDeviceScanner.stopScan()
        return BLEResult.success(true)
    }
}