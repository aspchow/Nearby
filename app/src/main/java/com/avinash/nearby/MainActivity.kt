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
                val permissionState by permissionDelegate.permissionState.collectAsState()
                val scanningState by viewModel.scanningState.collectAsState()
                ReaderScreen(
                    bleDevices = bleResolvedDevices,
                    permissionState = permissionState,
                    scanningState = scanningState
                )
            }
        }
        observeTheState()
    }


    private fun observeTheState() {
        permissionDelegate.onResume()
        lifecycleScope.launch {
            permissionDelegate.permissionState.collectLatest { permissionState ->
                when (permissionState) {
                    PermissionDelegate.PermissionState.Denied -> {

                    }

                    PermissionDelegate.PermissionState.Granted -> {
                        viewModel.startScanning()
                    }

                    PermissionDelegate.PermissionState.Idle -> {

                    }
                }
            }
        }
    }

    override fun onResume() {
        viewModel.resolvedDevices
        super.onResume()
    }

}

