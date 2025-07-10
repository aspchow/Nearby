package com.avinash.nearby.sender.scanner

import android.Manifest
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission
import com.avinash.nearby.sender.model.BLEDevice
import com.avinash.nearby.utils.printLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Created by Avinash Munnangi on 01/07/25.
 */
class BLEDeviceScanner @Inject constructor(
    private val bleScanner: BluetoothLeScanner?,
    private val bleScanFilterProvider: BLEScanFilterProvider,
    private val bleScanSettingsProvider: BLEScanSettingsProvider
) {

    private val _bleDevices = MutableSharedFlow<Set<BLEDevice>>(extraBufferCapacity = 15)
    val bleDevices = _bleDevices.asSharedFlow()

    private val _scanningState = MutableStateFlow<ScanState>(ScanState.IDLE)
    val scanningState = _scanningState.asStateFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private val scanCallback = object : ScanCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            printLog("The Single Scan Result: $result")
            super.onScanResult(callbackType, result)
            val device = result?.getBLEDevice() ?: return
            _bleDevices.tryEmit(setOf(device))
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            printLog("The Batch Scan Result: ${results?.size}")
            super.onBatchScanResults(results)
            val devices = results?.mapNotNull { it.getBLEDevice() } ?: return
            _bleDevices.tryEmit(devices.toSet())
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _scanningState.value = ScanState.Error("Scan failed with error code: $errorCode")
        }
    }

    /**
     * Scans for Bluetooth Low Energy (BLE) devices.*
     * @return true if the scan was initiated successfully, false otherwise.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun scanDevices() {
        if (scanningState.value == ScanState.Scanning) {
            return
        }
        val scanFilter = bleScanFilterProvider.provideScanFilter()
        val scanSettings = bleScanSettingsProvider.provideScanSettings()
        printLog("Starting BLE scan with filter: $scanFilter and settings: $scanSettings")
        if (bleScanner == null) {
            _scanningState.value = ScanState.Error("BluetoothLeScanner is not available")
            return
        }
        bleScanner?.startScan(
            scanFilter,
            scanSettings,
            scanCallback
        )
        _scanningState.value = ScanState.Scanning
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        if (scanningState.value != ScanState.Scanning) {
            return
        }
        bleScanner?.stopScan(scanCallback)
        _scanningState.value = ScanState.Stopped
    }
}