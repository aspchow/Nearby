package com.avinash.nearby.permission.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.ContextThemeWrapper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.avinash.nearby.permission.model.PermissionMeta

/**
 * Created by Avinash Munnangi on 13/07/25.
 */


@Composable
fun PermissionEducationDialog(permissionMeta: PermissionMeta.Denied) {
    val activity = getActivity()
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Permission Required") },
        text = {
            Text("You have denied the ${permissionMeta.permissionList.first()} Permission. Please go to settings and enable the required permissions.")
        },
        confirmButton = {
            Button(onClick = {
                // Handle the action to go to settings
                activity.openAppSettings()
            }) {
                Text("Go to Settings")
            }
        },
        dismissButton = null
    )
}

private fun Activity.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}

@Composable
fun getActivity(): Activity {
    return when (val context = LocalContext.current) {
        is Activity -> {
            context
        }

        is ContextThemeWrapper -> {
            (context.baseContext as ContextThemeWrapper).baseContext as Activity
        }

        else -> {
            throw IllegalStateException("Context is not an Activity")
        }
    }
}