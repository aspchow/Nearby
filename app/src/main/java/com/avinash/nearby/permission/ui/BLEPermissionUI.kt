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

    (permissionMeta as? PermissionMeta.Denied)?.let {
        PermissionEducationDialog(it)
        return
    }

    (permissionMeta as? PermissionMeta.Granted)?.let {
        SensorEducationDialog(
            it, onRequestTurnOnBLE = {
                permissionDelegate.onRequestTurnOnBLE()
            },
            onRequestTurnOnLocation = {
                permissionDelegate.onRequestTurnOnLocation()
            })
        return
    }

}