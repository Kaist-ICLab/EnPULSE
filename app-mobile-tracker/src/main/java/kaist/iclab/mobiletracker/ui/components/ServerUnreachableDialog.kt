package kaist.iclab.mobiletracker.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.components.popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.popup.PopupDialog

/**
 * Shown when the study config can't be downloaded because the server is unreachable.
 * With a cached config the user can dismiss it and keep working offline; without one the app
 * has nothing to run, so the only choices are retrying or exiting.
 */
@Composable
fun ServerUnreachableDialog(
    hasCachedConfig: Boolean,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    onContinueOffline: () -> Unit
) {
    PopupDialog(
        title = stringResource(R.string.server_unreachable_title),
        content = {
            Text(
                text = stringResource(
                    if (hasCachedConfig) R.string.server_unreachable_message_cached
                    else R.string.server_unreachable_message
                ),
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )
        },
        primaryButton = DialogButtonConfig(
            text = stringResource(R.string.server_unreachable_retry),
            onClick = onRetry
        ),
        secondaryButton = DialogButtonConfig(
            text = stringResource(R.string.server_unreachable_exit),
            onClick = onExit,
            isPrimary = false
        ),
        onDismiss = onContinueOffline,
        dismissible = hasCachedConfig
    )
}
