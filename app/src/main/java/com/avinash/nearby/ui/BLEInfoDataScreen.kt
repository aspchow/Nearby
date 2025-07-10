package com.avinash.nearby.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avinash.nearby.PermissionDelegate
import com.avinash.nearby.receiver.model.BLEAdvertisementMeta
import com.avinash.nearby.sender.model.BLEResolvedDevice
import com.avinash.nearby.sender.scanner.ScanState

/**
 * Created by Avinash Munnangi on 02/07/25.
 */

@Composable
fun BLEInfoDataScreen(
    advertising: BLEAdvertisementMeta,
    bleDevices: List<BLEResolvedDevice>,
    isLocationEnabled : Boolean,
    isBLEEnabled : Boolean,
    permissionMeta: PermissionDelegate.PermissionMeta,
    scanningState  : ScanState
) {
    LazyColumn(
        modifier = Modifier.padding(
            top = 30.dp
        )
    ) {
        item {
            Text("Advertising Meta $advertising")
        }

        item {
            Text("The Permission State : $permissionMeta")
        }
        item {
            Text("Is Location Enabled : $isLocationEnabled")
        }

        item {
            Text("is BLE Enabled : $isBLEEnabled")
        }

        item {
            Text("The Scanning State : $scanningState")
        }

        item {
            Text("The Devices count  : ${bleDevices.size}")
        }

        items(bleDevices.toList(), key = {
            it.bleId
        }) { device ->
            Column {
                Text(device.bleId)
            }
        }
    }
}