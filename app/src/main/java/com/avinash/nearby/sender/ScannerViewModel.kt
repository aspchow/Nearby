package com.avinash.nearby.sender

import androidx.lifecycle.ViewModel
import com.avinash.nearby.sender.manager.BLEScannerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Created by Avinash Munnangi on 02/07/25.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val bleScannerManager: BLEScannerManager
) : ViewModel() {

    val resolvedDevices = bleScannerManager.bleResolvedDevices
    val scanningState = bleScannerManager.scanningState

    fun startScanning() {
        bleScannerManager.startScanning()
    }

    fun stopScanning() {
        bleScannerManager.stopScanning()
    }
}