package kaist.iclab.mobiletracker.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog

@Composable
fun FullScreenIntentPermissionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        PopupDialog(
            title = stringResource(R.string.perm_fullscreen_intent_title),
            content = {
                Text(
                    text = stringResource(R.string.perm_fullscreen_intent_message),
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            },
            primaryButton = DialogButtonConfig(
                text = stringResource(R.string.perm_fullscreen_intent_action),
                onClick = onConfirm
            ),
            secondaryButton = DialogButtonConfig(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                isPrimary = false
            ),
            onDismiss = onDismiss
        )
    }
}
