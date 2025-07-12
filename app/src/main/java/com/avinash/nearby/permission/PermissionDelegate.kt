package com.avinash.nearby.permission

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.avinash.nearby.utils.printLog
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val systemStateReceiver: SystemStateReceiver
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
    private var permissionsRequested = false

    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled = _isLocationEnabled.asStateFlow()

    private val _isBLEEnabled = MutableStateFlow(false)
    val isBLEEnabled = _isBLEEnabled.asStateFlow()

    private val _showDialog = MutableSharedFlow<String>()
    private val _permissionState = MutableStateFlow(PermissionStatus.Unknown)

    // Combine different states into a unified PermissionMeta state
    val permissionState = combine(
        _permissionState,
        isBLEEnabled,
        isLocationEnabled
    ) { permissionStatus, isBLEEnabled, isLocationEnabled ->
        when (permissionStatus) {
            PermissionStatus.Unknown -> PermissionMeta.Unknown
            PermissionStatus.Granted -> PermissionMeta.Granted(
                locationEnabled = isLocationEnabled,
                bleEnabled = isBLEEnabled
            )

            PermissionStatus.Denied -> PermissionMeta.Denied
        }
    }

    init {
        observeDialogState()
    }

    private val requestBluetoothPermissionsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(), ::handlePermissionsResult
    )

    private val enableBluetoothLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // No action needed after enabling Bluetooth
    }

    fun onCreate() {
        logDebug("onCreate called in PermissionDelegate")
        activity.lifecycleScope.launch {
            systemStateReceiver.systemStateUpdate.collect { action ->
                when (action) {
                    LocationManager.PROVIDERS_CHANGED_ACTION, BluetoothAdapter.ACTION_STATE_CHANGED -> checkPermissionStates()
                }
            }
        }
    }

    fun onResume() {
        logDebug("onResume called in PermissionDelegate")
        checkPermissionStates()
        requestAllBleRelatedPermissions()
        observeSystemState()
    }

    fun onPause() {
        logDebug("onPause called in PermissionDelegate")
        unregisterSystemStateReceiver()
    }

    private fun requestAllBleRelatedPermissions() {
        val permissionsToRequest = getPendingPermissions()
        if (permissionsToRequest.isEmpty()) {
            enableBluetoothIfNotEnabled()
            _permissionState.value = PermissionStatus.Granted
            return
        }

        if (permissionsRequested) {
            logDebug("Permissions already requested, skipping request.")
            handlePermanentDenial(permissionsToRequest)
            return
        }
        permissionsRequested = true
        requestBluetoothPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun enableBluetoothIfNotEnabled() {
        if (bluetoothAdapter.isEnabled) return
       /* val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)*/
        showDialogToTurnOnBluetooth()
    }

    private fun getPendingPermissions(): List<String> {
        val permissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        return permissions.filter {
            activity.checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        val granted = permissions.all { it.value }
        if (granted) {
            enableBluetoothIfNotEnabled()
            _permissionState.value = PermissionStatus.Granted
            return
        }
        val firstDeniedPermission = permissions.entries.firstOrNull { !it.value }?.key
        if (firstDeniedPermission != null &&
            activity.shouldShowRequestPermissionRationale(firstDeniedPermission)
        ) {
            showSettingsDialog(firstDeniedPermission)
        }
        _permissionState.value = PermissionStatus.Denied
    }

    private fun handlePermanentDenial(permissions: List<String>) {
        val firstDeniedPermission = permissions.firstOrNull() ?: return
        showSettingsDialog(firstDeniedPermission)
    }

    private fun checkPermissionStates() {
        _isLocationEnabled.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        _isBLEEnabled.value = bluetoothAdapter.isEnabled
    }

    private fun observeSystemState() {
        if (isReceiverRegistered) return

        val intentFilter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        activity.registerReceiver(systemStateReceiver, intentFilter)
        isReceiverRegistered = true
    }

    private fun unregisterSystemStateReceiver() {
        if (isReceiverRegistered) {
            activity.unregisterReceiver(systemStateReceiver)
            isReceiverRegistered = false
        }
    }

    private fun observeDialogState() {
        activity.lifecycleScope.launch {
            _showDialog.collectLatest { permission ->
                showSettingsDialogInternal(permission)
            }
        }
    }

    private fun showSettingsDialog(permission: String) {
        if (activity.isFinishing || activity.isDestroyed) {
            logDebug("Activity is finishing or destroyed, cannot show settings dialog.")
            return
        }
        activity.lifecycleScope.launch {
            _showDialog.emit(permission)
        }
    }

    private var isAlreadyDialogShown = false
    private fun showSettingsDialogInternal(permission: String) {
        if (isAlreadyDialogShown) return
        printLog("Showing dialog to the user for permission: $permission")
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("This feature requires $permission that you have permanently denied. Please grant it in the app settings.")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                dialog.dismiss()
                isAlreadyDialogShown = false
                openAppSettings()
            }
            .show()
            .setCancelable(false)
        isAlreadyDialogShown = true
    }


    private fun showDialogToTurnOnBluetooth() {
        AlertDialog.Builder(activity)
            .setTitle("Turn on Bluetooth")
            .setMessage("This feature requires bluetooth to be turned on. Please turn it on in the settings.")
            .setPositiveButton("Turn on Bluetooth") { dialog, _ ->
                dialog.dismiss()
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            }
            .show()
            .setCancelable(false)
        isAlreadyDialogShown = true
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    private fun logDebug(message: String) {
        // Replace with a proper logging library like Timber if needed
        Log.d("PermissionDelegate", message)
    }
}