package com.avinash.nearby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.avinash.nearby.sender.ScannerViewModel
import com.avinash.nearby.ui.ReaderScreen
import com.avinash.nearby.ui.theme.NearbyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<ScannerViewModel>()

    @Inject
    lateinit var permissionDelegate: PermissionDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        // check for location and bluetooth permissions
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NearbyTheme {
                val bleResolvedDevices by viewModel.resolvedDevices.collectAsState()
                val permissionState by permissionDelegate.permissionState.collectAsState(PermissionDelegate.PermissionMeta.Unknown)
                val scanningState by viewModel.scanningState.collectAsState()
                val isLocationEnabled by permissionDelegate.isLocationEnabled.collectAsState()
                val isBlEEnabled by permissionDelegate.isBLEEnabled.collectAsState()
                ReaderScreen(
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
                            viewModel.startScanning()
                        } else {
                            viewModel.stopScanning()
                        }
                    }

                    else -> {
                        viewModel.stopScanning()
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
        viewModel.stopScanning()
    }
}

