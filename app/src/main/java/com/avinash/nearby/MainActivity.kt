package com.avinash.nearby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.avinash.nearby.receiver.manager.BLEAdvertiserManager
import com.avinash.nearby.receiver.BLEAdvertiserViewModel
import com.avinash.nearby.sender.ScannerViewModel
import com.avinash.nearby.ui.BLEInfoDataScreen
import com.avinash.nearby.ui.theme.NearbyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val scannerViewModel by viewModels<ScannerViewModel>()

    private val receiverViewModel by viewModels<BLEAdvertiserViewModel>()

    @Inject
    lateinit var manager: BLEAdvertiserManager

    @Inject
    lateinit var permissionDelegate: PermissionDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        // check for location and bluetooth permissions
        super.onCreate(savedInstanceState)
        receiverViewModel.onActivityCreated(manager = manager)
        enableEdgeToEdge()
        setContent {
            NearbyTheme {
                val bleResolvedDevices by scannerViewModel.resolvedDevices.collectAsState(emptyList())
                val permissionState by permissionDelegate.permissionState.collectAsState(
                    PermissionDelegate.PermissionMeta.Unknown
                )
                val scanningState by scannerViewModel.scanningState.collectAsState()
                val isLocationEnabled by permissionDelegate.isLocationEnabled.collectAsState()
                val isBlEEnabled by permissionDelegate.isBLEEnabled.collectAsState()
                val advertisingMeta by receiverViewModel.advertisingState.collectAsState()

                BLEInfoDataScreen(
                    advertising = advertisingMeta,
                    bleDevices = bleResolvedDevices,
                    permissionMeta = permissionState,
                    scanningState = scanningState,
                    isLocationEnabled = isLocationEnabled,
                    isBLEEnabled = isBlEEnabled
                )
            }
        }
        observeTheState()
    }

    private fun observeTheState() {
        lifecycleScope.launch {
            permissionDelegate.permissionState.collectLatest { permissionState ->
                when (permissionState) {
                    is PermissionDelegate.PermissionMeta.Granted -> {
                        val (locationEnabled, bleEnabled) = permissionState
                        if (locationEnabled && bleEnabled) {
                            scannerViewModel.startScanning()
                            receiverViewModel.startAdvertising()
                        } else {
                            scannerViewModel.stopScanning()
                            receiverViewModel.stopAdvertising()
                        }
                    }

                    else -> {
                        scannerViewModel.stopScanning()
                        // Handle the denied state if needed
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionDelegate.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        scannerViewModel.stopScanning()
    }
}

