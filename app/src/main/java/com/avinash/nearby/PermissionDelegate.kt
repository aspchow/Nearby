package com.avinash.nearby

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


/**
 * Created by Avinash Munnangi on 02/07/25.
 */
@ActivityScoped
class PermissionDelegate @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val activity: ComponentActivity
) {

    sealed interface PermissionState {
        data object Idle : PermissionState
        data object Granted : PermissionState
        data object Denied : PermissionState
    }

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Idle)
    val permissionState = _permissionState.asStateFlow()


    private val requestBluetoothPermissionsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle the permission results
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Toast.makeText(activity, "Bluetooth permissions granted!", Toast.LENGTH_SHORT).show()
            // Permissions granted, proceed with BLE scan or other operations
            // startBleScan() // You would call your scan function here
            checkAndRequestPermissionsAndBluetooth()
            _permissionState.value = PermissionState.Granted
        } else {
            Toast.makeText(activity, "Bluetooth permissions denied.", Toast.LENGTH_SHORT).show()
            // Handle permission denial (e.g., show explanation, disable BLE features)
            _permissionState.value = PermissionState.Denied
            // Check if any permission was permanently denied
            val permanentlyDeniedPermissions =
                permissions.entries.filter { (permission, isGranted) ->
                    !isGranted && !activity.shouldShowRequestPermissionRationale(
                        permission
                    )
                }.map { it.key }

            if (permanentlyDeniedPermissions.isNotEmpty()) {
                // At least one permission was permanently denied, guide user to settings
                showSettingsDialog()
            } else {
                // Some permissions denied, but not permanently (user can be asked again)
                Toast.makeText(activity, "Some Bluetooth permissions denied.", Toast.LENGTH_LONG)
                    .show()
                // You might show a rationale again or disable functionality
            }

        }
    }

    private val enableBluetoothLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(activity, "Bluetooth enabled!", Toast.LENGTH_SHORT).show()
            // Bluetooth is now enabled, proceed to request permissions or scan
        } else {
            Toast.makeText(activity, "Bluetooth not enabled.", Toast.LENGTH_SHORT).show()
            // User did not enable Bluetooth
        }
    }

    private fun requestALLBleRelatedPermissions() {
        val permissionsToRequest = getRequiredPermissions()
        if (permissionsToRequest.isNotEmpty()) {
            requestBluetoothPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkAndRequestPermissionsAndBluetooth()
            // All necessary permissions are already granted, proceed with BLE scan
            _permissionState.value = PermissionState.Granted
            Toast.makeText(
                activity,
                "Permissions already granted, ready to scan!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkAndRequestPermissionsAndBluetooth() {
        if (bluetoothAdapter.isEnabled) return
        // Bluetooth is not available or not enabled, prompt user to enable it
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)

    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return permissions
    }

    fun onResume() {
        requestALLBleRelatedPermissions()
    }


    private fun showSettingsDialog() {

    }
}