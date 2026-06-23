package eu.dotshell.pelo.generic.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

val LocalNotificationPermissionRequested = compositionLocalOf { mutableStateOf(false) }

@Composable
fun NotificationPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val notificationPermissionRequested = remember { mutableStateOf(false) }
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        notificationPermissionRequested.value = true
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                notificationPermissionRequested.value = true
            }
        } else {
            notificationPermissionRequested.value = true
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalNotificationPermissionRequested provides notificationPermissionRequested
    ) {
        content()
    }
}
