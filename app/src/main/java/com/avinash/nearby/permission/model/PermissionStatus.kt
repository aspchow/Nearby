package com.avinash.nearby.permission.model

sealed interface PermissionStatus {
        data object Unknown : PermissionStatus
        data object Granted : PermissionStatus
        data class Denied(val deniedList: List<String>) : PermissionStatus
    }

    sealed interface PermissionMeta {
        data object Unknown : PermissionMeta
        data class Granted(
            val locationEnabled: SensorEnabledAction,
            val bleEnabled: SensorEnabledAction
        ) : PermissionMeta

        data class Denied(val permissionList: List<String>) : PermissionMeta
    }

    sealed interface SensorEnabledAction {
        data object TurnedOn : SensorEnabledAction
        data object TurnedOff : SensorEnabledAction
        data object Unknown : SensorEnabledAction
    }