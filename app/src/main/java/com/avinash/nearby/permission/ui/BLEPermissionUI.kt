package com.avinash.nearby.permission.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.avinash.nearby.permission.PermissionDelegate
import com.avinash.nearby.permission.model.PermissionMeta

/**
 * Created by Avinash Munnangi on 13/07/25.
 */

@Composable
fun BLEPermissionUI(permissionDelegate: PermissionDelegate) {
    val permissionMeta: PermissionMeta by permissionDelegate.permissionMeta.collectAsState(
        PermissionMeta.Unknown
    )
    val permissionState: PermissionMeta = permissionMeta

    when (permissionState) {
        is PermissionMeta.Denied -> {
            PermissionEducationDialog(permissionMeta as PermissionMeta.Denied)
        }

        is PermissionMeta.Granted -> {
            SensorEducationDialog(
                permissionMeta as PermissionMeta.Granted, onRequestTurnOnBLE = {
                    permissionDelegate.onRequestTurnOnBLE()
                },
                onRequestTurnOnLocation = {
                    permissionDelegate.onRequestTurnOnLocation()
                })
        }

        PermissionMeta.Unknown -> {
            // Do nothing, waiting for the permission state to be updated
        }

    }

}