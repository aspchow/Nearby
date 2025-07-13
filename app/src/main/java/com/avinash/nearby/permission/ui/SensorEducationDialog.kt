package com.avinash.nearby.permission.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.avinash.nearby.permission.model.PermissionMeta
import com.avinash.nearby.permission.model.SensorEnabledAction

/**
 * Created by Avinash Munnangi on 13/07/25.
 */


@Composable
fun SensorEducationDialog(
    permissionMeta: PermissionMeta.Granted,
    onRequestTurnOnBLE: () -> Unit,
    onRequestTurnOnLocation: () -> Unit
) {

    val isBLEDisabled = permissionMeta.bleEnabled is SensorEnabledAction.TurnedOff
    val isLocationDisabled = permissionMeta.locationEnabled is SensorEnabledAction.TurnedOff
    if (isBLEDisabled) {
        AlertDialogForPermission(
            title = "Turn on Bluetooth",
            subText = "This feature requires bluetooth to be turned on.",
            ctaText = "Turn on Bluetooth",
            onClick = {
                onRequestTurnOnBLE.invoke()
            }
        )
        return
    }

    if (isLocationDisabled) {
        AlertDialogForPermission(
            title = "Turn on Location",
            subText = "This feature requires location to be turned on.",
            ctaText = "Turn on Location",
            onClick = {
                onRequestTurnOnLocation.invoke()
            }
        )
        return
    }
}


@Composable
fun AlertDialogForPermission(
    title: String,
    subText: String,
    ctaText: String,
    onClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(title) },
        text = {
            Text(subText)
        },
        confirmButton = {
            Button(onClick = {
                onClick.invoke()
            }) {
                Text(ctaText)
            }
        },
        dismissButton = null
    )
}
