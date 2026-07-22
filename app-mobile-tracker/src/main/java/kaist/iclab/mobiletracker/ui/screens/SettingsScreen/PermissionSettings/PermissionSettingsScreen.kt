package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.PermissionSettings

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.viewmodels.settings.SettingsViewModel
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.permission.Permission
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.permission.getPermissionState
import kaist.iclab.tracker.sensor.controller.ControllerState
import org.koin.androidx.compose.koinViewModel

/**
 * Permission settings screen
 * Displays all supported permissions with their current state and allows requesting them
 */
@Composable
fun PermissionSettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    permissionManager: AndroidPermissionManager,
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val permissions = Permission.supportedPermissions.toList()
    val allPermissionIds = permissions.flatMap { it.ids.toList() }
    val permissionStateMap = permissionManager.getPermissionFlow(allPermissionIds.toTypedArray())
        .collectAsState().value
    val controllerState = settingsViewModel.controllerState.collectAsState().value
    val isCollecting = controllerState.flag == ControllerState.FLAG.RUNNING

    // State for Restricted Settings Dialog
    var showRestrictedDialog by remember { mutableStateOf(false) }
    var pendingPermissionId by remember { mutableStateOf<String?>(null) }

    // State for Instruction Dialog
    var pendingPermissionForInstruction by remember { mutableStateOf<Permission?>(null) }

    val handlePermissionRequest: (Permission, PermissionState) -> Unit = { permission, state ->
        when (state) {
            PermissionState.GRANTED, PermissionState.PERMANENTLY_DENIED -> {
                // Open settings to allow user to revoke/change permission or for permanently denied
                permissionManager.openPermissionSettings(permission.ids.first())
            }
            else -> {
                // Handle Android 13+ Restricted Settings for special accessibility/notification permissions
                val id = permission.ids.first()
                if (Build.VERSION.SDK_INT >= 33 &&
                    (id == Manifest.permission.BIND_ACCESSIBILITY_SERVICE ||
                            id == Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                ) {
                    pendingPermissionId = id
                    showRestrictedDialog = true
                } else {
                    // Request permission normally
                    permissionManager.request(permission.ids)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Styles.HEADER_HEIGHT)
                    .padding(horizontal = Styles.HEADER_HORIZONTAL_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = context.getString(R.string.menu_permission),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }

            // Description text
            Text(
                text = stringResource(R.string.permission_screen_description),
                color = AppColors.TextSecondary,
                fontSize = Styles.SCREEN_DESCRIPTION_FONT_SIZE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Styles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING,
                        end = Styles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING,
                        bottom = Styles.SCREEN_DESCRIPTION_BOTTOM_PADDING
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Styles.CARD_CONTAINER_HORIZONTAL_PADDING)
                    .padding(bottom = Styles.SETTING_CONTAINER_BOTTOM_PADDING)
                    .clip(Styles.CONTAINER_SHAPE)
                    .background(AppColors.White)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = permissions,
                        key = { _, permission -> permission.name }
                    ) { index, permission ->
                        val isLast = index == permissions.size - 1
                        val permissionAggregatedState =
                            permission.getPermissionState(permissionStateMap)

                        PermissionCard(
                            permission = permission,
                            permissionState = permissionAggregatedState,
                            onRequest = {
                                if (isCollecting) {
                                    // Show toast if trying to change permission while collecting
                                    AppToast.show(context, R.string.turn_off_data_collection_first)
                                } else {
                                    // Allow permission request if not collecting
                                    if (permission.instructionResId != null && permissionAggregatedState != PermissionState.GRANTED) {
                                        pendingPermissionForInstruction = permission
                                    } else {
                                        handlePermissionRequest(permission, permissionAggregatedState)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Add horizontal divider between cards (not after the last one)
                        if (!isLast) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                HorizontalDivider(
                                    color = AppColors.BorderDark,
                                    thickness = 0.dp,
                                    modifier = Modifier.fillMaxWidth(Styles.DIVIDER_WIDTH_RATIO)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestrictedDialog && pendingPermissionId != null) {
        PopupDialog(
            title = stringResource(R.string.restricted_settings_title),
            content = {
                Text(
                    text = stringResource(R.string.restricted_settings_desc),
                    color = AppColors.TextSecondary,
                    fontSize = Styles.SCREEN_DESCRIPTION_FONT_SIZE
                )
            },
            primaryButton = DialogButtonConfig(
                text = stringResource(R.string.restricted_settings_got_it),
                onClick = {
                    showRestrictedDialog = false
                    permissionManager.openPermissionSettings(pendingPermissionId!!)
                    pendingPermissionId = null
                }
            ),
            secondaryButton = DialogButtonConfig(
                text = stringResource(R.string.restricted_settings_cancel),
                onClick = {
                    showRestrictedDialog = false
                    pendingPermissionId = null
                },
                isPrimary = false
            ),
            onDismiss = {
                showRestrictedDialog = false
                pendingPermissionId = null
            }
        )
    }

    pendingPermissionForInstruction?.let { permission ->
        PopupDialog(
            title = stringResource(R.string.permission_instruction_title),
            content = {
                Text(
                    text = stringResource(permission.instructionResId!!),
                    color = AppColors.TextSecondary,
                    fontSize = Styles.SCREEN_DESCRIPTION_FONT_SIZE
                )
            },
            primaryButton = DialogButtonConfig(
                text = stringResource(R.string.ok),
                onClick = {
                    val state = permission.getPermissionState(permissionStateMap)
                    pendingPermissionForInstruction = null
                    handlePermissionRequest(permission, state)
                }
            ),
            secondaryButton = DialogButtonConfig(
                text = stringResource(R.string.cancel),
                onClick = {
                    pendingPermissionForInstruction = null
                },
                isPrimary = false
            ),
            onDismiss = {
                pendingPermissionForInstruction = null
            }
        )
    }
}
