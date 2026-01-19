package kaist.iclab.field_tracker.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kaist.iclab.field_tracker.ui.components.Header
import kaist.iclab.field_tracker.ui.components.ListCard
import kaist.iclab.field_tracker.ui.components.SwitchRow
import kaist.iclab.field_tracker.ui.components.SwitchStatus
import kaist.iclab.field_tracker.ui.theme.MainTheme
import kaist.iclab.tracker.permission.Permission
import kaist.iclab.tracker.permission.PermissionState

@Composable
fun PermissionListScreen(
    permissionMap: Map<String, PermissionState>,
    onPermissionRequest: (Array<String>) -> Unit,
    canNavigateBack: Boolean,
    navigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Header(
                title = "Permissions",
                canNavigateBack = canNavigateBack,
                navigateBack = navigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ListCard(
                title = "Permissions",
                rows = Permission.supportedPermissions.map { permission ->
                    {
                        PermissionStateSwitchRow(
                            permission,
                            permissionMap[permission.ids[0]] ?: PermissionState.NOT_REQUESTED,
                            onPermissionRequest
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionStateSwitchRow(
    permission: Permission,
    permissionState: PermissionState,
    onPermissionRequest: (Array<String>) -> Unit
) {
    val subtitleMap = mapOf(
        PermissionState.GRANTED to "Permission Granted",
        PermissionState.PERMANENTLY_DENIED to "Permanently Denied. Please enable in settings.",
        PermissionState.RATIONALE_REQUIRED to "Not Requested. Please turn on by the switch.",
        PermissionState.NOT_REQUESTED to "Not Requested. Please turn on by the switch."
    )
    SwitchRow(
        permission.name,
        subtitle = subtitleMap[permissionState]
            ?: error("Invalid permission state: $permissionState"),
        switchStatus = SwitchStatus(
            isChecked = permissionState == PermissionState.GRANTED,
            onCheckedChange = { if (it) onPermissionRequest(permission.ids) },
            disabled = !setOf(
                PermissionState.NOT_REQUESTED,
                PermissionState.RATIONALE_REQUIRED
            ).contains(permissionState)
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PermissionListScreenPreview() {
    MainTheme {
        PermissionListScreen(
            listOfNotNull(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else null,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BODY_SENSORS,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
                Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.PACKAGE_USAGE_STATS else null,
            ).associateWith { PermissionState.GRANTED }.toMap(),
            onPermissionRequest = {},
            canNavigateBack = true,
            navigateBack = {}
        )
    }
}
