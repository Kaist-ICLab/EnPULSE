package kaist.iclab.wearabletracker.helpers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.permission.PermissionState

object PermissionHelper {
    fun checkNotificationPermission(
        context: Context,
        permissionManager: AndroidPermissionManager
    ): PermissionCheckResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return PermissionCheckResult.Granted
        }

        permissionManager.registerPermission(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        val permissionFlow =
            permissionManager.getPermissionFlow(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        val permissionState = permissionFlow.value[Manifest.permission.POST_NOTIFICATIONS]
            ?: PermissionState.NOT_REQUESTED

        return when (permissionState) {
            PermissionState.GRANTED, PermissionState.UNSUPPORTED -> PermissionCheckResult.Granted
            PermissionState.PERMANENTLY_DENIED -> PermissionCheckResult.PermanentlyDenied
            PermissionState.NOT_REQUESTED, PermissionState.RATIONALE_REQUIRED -> {
                permissionManager.request(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                PermissionCheckResult.Requested
            }
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }
}
