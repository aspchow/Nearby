package com.avinash.nearby.permission

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.avinash.nearby.permission.model.PermissionMeta
import com.avinash.nearby.permission.model.PermissionStatus
import com.avinash.nearby.permission.model.SensorEnabledAction
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    private var isReceiverRegistered = false
    private var permissionsRequested = false

    private val _isLocationEnabled =
        MutableStateFlow<SensorEnabledAction>(SensorEnabledAction.Unknown)
    val isLocationEnabled = _isLocationEnabled.asStateFlow()

    private val _isBLEEnabled = MutableStateFlow<SensorEnabledAction>(SensorEnabledAction.Unknown)
    val isBLEEnabled = _isBLEEnabled.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionStatus>(PermissionStatus.Unknown)

    // Combine different states into a unified PermissionMeta state
    val permissionMeta = combine(
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

            is PermissionStatus.Denied -> PermissionMeta.Denied(
                permissionList = permissionStatus.deniedList
            )
        }
    }.stateIn(
        scope = activity.lifecycleScope,
        initialValue = PermissionMeta.Unknown,
        started = WhileSubscribed(),
    )

    private val requestBluetoothPermissionsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(), ::handlePermissionsResult
    )

    private val enableBluetoothLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // do nothing
    }

    fun onCreate() {
        activity.lifecycleScope.launch {
            systemStateReceiver.systemStateUpdate.collect { action ->
                when (action) {
                    LocationManager.PROVIDERS_CHANGED_ACTION, BluetoothAdapter.ACTION_STATE_CHANGED -> checkPermissionStates()
                }
            }
        }
    }

    fun onResume() {
        checkPermissionStates()
        requestAllBleRelatedPermissions()
        observeSystemState()
    }

    fun onPause() {
        unregisterSystemStateReceiver()
    }

    private fun requestAllBleRelatedPermissions() {
        val permissionsToRequest = getPendingPermissions()
        if (permissionsToRequest.isEmpty()) {
            _permissionState.value = PermissionStatus.Granted
            return
        }

        if (permissionsRequested) {
            _permissionState.value = PermissionStatus.Denied(permissionsToRequest)
            return
        }
        permissionsRequested = true
        requestBluetoothPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
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
            _permissionState.value = PermissionStatus.Granted
            return
        }
        _permissionState.value = PermissionStatus.Denied(getPendingPermissions())
    }

    private fun checkPermissionStates() {
        _isLocationEnabled.value =
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) SensorEnabledAction.TurnedOn else SensorEnabledAction.TurnedOff
        _isBLEEnabled.value =
            if (bluetoothAdapter.isEnabled) SensorEnabledAction.TurnedOn else SensorEnabledAction.TurnedOff
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

    // Launcher for the location settings resolution dialog
    private val resolutionForResult: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // User agreed to turn on location services
                Toast.makeText(activity, "Location is now enabled!", Toast.LENGTH_SHORT).show()
                // You can now proceed to request location updates or get last known location
            } else {
                // User declined to turn on location services
            }
        }


    fun onRequestTurnOnBLE() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    fun onRequestTurnOnLocation() {
        val client = LocationServices.getSettingsClient(activity)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_LOW_POWER, // Or BALANCED_POWER_ACCURACY, LOW_POWER
            Long.MAX_VALUE
        ).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Show the dialog even if location is off
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. You can now start requesting location updates.
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    resolutionForResult.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.

                }
            } else if (exception is ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // Location settings are not satisfied, and no way to fix it.
                        // For example, GPS hardware not available on the device.

                        Toast.makeText(
                            activity,
                            "Location settings are unavailable.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

}