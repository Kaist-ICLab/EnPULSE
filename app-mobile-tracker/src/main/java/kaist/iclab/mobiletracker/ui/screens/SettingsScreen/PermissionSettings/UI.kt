package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.PermissionSettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.tracker.permission.Permission
import kaist.iclab.tracker.permission.PermissionState

@Composable
fun PermissionCard(
    modifier: Modifier = Modifier,
    permission: Permission,
    permissionState: PermissionState,
    onRequest: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Transparent),
        shape = Styles.CARD_SHAPE
    ) {
        PermissionRow(
            permission = permission,
            permissionState = permissionState,
            onRequest = onRequest
        )
    }
}

@Composable
private fun PermissionRow(
    permission: Permission,
    permissionState: PermissionState,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Use localized name from first permission ID for UI
    val localizedPermissionName = getPermissionName(context, permission.ids.first())
    // Get localized description from first permission ID
    val permissionDescription = getPermissionDescription(context, permission.ids.first())
    // Get icon from first permission ID
    val permissionIcon = getPermissionIcon(permission.ids.first())
    
    // Status text and color
    val statusText = getPermissionStatusText(context, permissionState)
    val statusColor = when (permissionState) {
        PermissionState.PERMANENTLY_DENIED -> AppColors.ErrorColor
        PermissionState.UNSUPPORTED -> AppColors.ErrorColor
        PermissionState.NOT_REQUESTED -> AppColors.TextPrimary
        PermissionState.GRANTED -> AppColors.PrimaryColor
        PermissionState.RATIONALE_REQUIRED -> AppColors.ErrorColor
    }
    
    // Card is clickable and shows arrow only if not unsupported
    val isSupported = permissionState != PermissionState.UNSUPPORTED

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSupported) {
                    Modifier.clickable { onRequest() }
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = Styles.CARD_HORIZONTAL_PADDING,
                vertical = Styles.CARD_VERTICAL_PADDING
            )
    ) {
        Icon(
            imageVector = permissionIcon,
            contentDescription = localizedPermissionName,
            modifier = Modifier.size(Styles.ICON_SIZE),
            tint = AppColors.getPermissionColor(permission.name)
        )
        Spacer(Modifier.width(Styles.ICON_SPACER_WIDTH))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = localizedPermissionName,
                color = AppColors.TextPrimary,
                fontSize = Styles.TEXT_FONT_SIZE,
                lineHeight = Styles.TEXT_LINE_HEIGHT,
                modifier = Modifier.padding(top = Styles.TEXT_TOP_PADDING)
            )
            Text(
                text = statusText,
                color = statusColor,
                fontSize = Styles.STATUS_TEXT_FONT_SIZE,
                lineHeight = Styles.STATUS_TEXT_LINE_HEIGHT,
                modifier = Modifier.padding(top = Styles.STATUS_TOP_PADDING)
            )
            Text(
                text = permissionDescription,
                color = AppColors.TextSecondary,
                fontSize = Styles.CARD_DESCRIPTION_FONT_SIZE,
                lineHeight = Styles.CARD_DESCRIPTION_LINE_HEIGHT,
                modifier = Modifier
                    .padding(
                        top = Styles.CARD_DESCRIPTION_TOP_PADDING,
                        bottom = Styles.CARD_DESCRIPTION_BOTTOM_PADDING
                    )
            )
        }
        if (isSupported) {
            Spacer(Modifier.width(Styles.SPACER_WIDTH))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

