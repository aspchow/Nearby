package com.avinash.nearby.permission

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.avinash.nearby.utils.printLog
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Created by Avinash Munnangi on 02/07/25.
 */
@ActivityScoped
class PermissionDelegate @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val locationManager: LocationManager,
    private val activity: ComponentActivity,
    private val locationPermissionProvider: SystemStateReceiver
) {

    enum class PermissionStatus {
        Unknown,
        Granted,
        Denied
    }

    sealed interface PermissionMeta {
        data object Unknown : PermissionMeta
        data class Granted(val locationEnabled: Boolean, val bleEnabled: Boolean) : PermissionMeta
        data object Denied : PermissionMeta
    }

    private var isReceiverRegistered = false

    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled = _isLocationEnabled.asStateFlow()

    private val _isBLEEnabled = MutableStateFlow(false)
    val isBLEEnabled = _isBLEEnabled.asStateFlow()


    private val _permissionState = MutableStateFlow(PermissionStatus.Unknown)

    val permissionState = combine(
        _permissionState,
        isBLEEnabled,
        isLocationEnabled
    ) { permissionStatus, isBLEEnabled, isLocationEnabled ->
        when (permissionStatus) {
            PermissionStatus.Unknown -> PermissionMeta.Unknown
            PermissionStatus.Granted -> {
                PermissionMeta.Granted(
                    locationEnabled = isLocationEnabled,
                    bleEnabled = isBLEEnabled
                )
            }
            PermissionStatus.Denied -> PermissionMeta.Denied
        }
    }

    private val requestBluetoothPermissionsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle the permission results
        val granted = permissions.entries.all { it.value }
        if (granted.not()) {
            _permissionState.value = PermissionStatus.Denied
            return@registerForActivityResult
        }
        enableBluetoothIfNotEnabled()
        _permissionState.value = PermissionStatus.Granted
    }

    private val enableBluetoothLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // do nothing
    }

    private fun requestALLBleRelatedPermissions() {
        val permissionsToRequest = getRequiredPermissions()
        if (permissionsToRequest.isEmpty()) {
            enableBluetoothIfNotEnabled()
            _permissionState.value = PermissionStatus.Granted
            return
        }
        val permissionsDeniedPermanently = permissionsToRequest
            .filter { permission -> activity.shouldShowRequestPermissionRationale(permission) }

        if (permissionsDeniedPermanently.isNotEmpty()) {
            // At least one permission is permanently denied, show settings dialog
            _permissionState.value = PermissionStatus.Denied
            showSettingsDialog(permissionsDeniedPermanently.first())
            return
        }
        requestBluetoothPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun enableBluetoothIfNotEnabled() {
        if (bluetoothAdapter.isEnabled) return
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.filter { permission ->
            activity.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun onResume() {
        printLog("onResume called in PermissionDelegate")
        checkPermissionAreEnabled()
        requestALLBleRelatedPermissions()
        observeSystemState()
    }

    fun onPause() {
        printLog("onPause called in PermissionDelegate")
        // No specific actions needed on pause for permissions
        if (!isReceiverRegistered){
            return
        }
        activity.unregisterReceiver(locationPermissionProvider)
        isReceiverRegistered = false
    }

    private fun checkAndUpdateIfLocationEnabled() {
        printLog("Checking if location is enabled")
        _isLocationEnabled.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun checkIfBLEEnabled() {
        printLog("Checking if Bluetooth is enabled")
        _isBLEEnabled.value = bluetoothAdapter.isEnabled
    }

    private fun showSettingsDialog(permission: String) {
        printLog("Need to show settings dialog to user $permission")
    }

    private fun checkPermissionAreEnabled() {
        checkIfBLEEnabled()
        checkAndUpdateIfLocationEnabled()
    }

    private fun observeSystemState() {
        val intentFilter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        activity.lifecycleScope.launch {
            locationPermissionProvider.systemStateUpdate.collect {
                checkPermissionAreEnabled()
            }
        }
        activity.registerReceiver(locationPermissionProvider, intentFilter)
        isReceiverRegistered = true
    }
}