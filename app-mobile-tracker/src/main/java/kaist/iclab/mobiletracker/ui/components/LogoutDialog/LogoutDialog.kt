package kaist.iclab.mobiletracker.ui.components.LogoutDialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog

/**
 * Logout confirmation dialog
 */
@Composable
fun LogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current

    PopupDialog(
        title = context.getString(R.string.logout_title),
        content = {
            Text(
                text = context.getString(R.string.logout_message),
                fontSize = Styles.MessageFontSize,
                color = Styles.MessageColor,
                textAlign = TextAlign.Start
            )
        },
        primaryButton = DialogButtonConfig(
            text = context.getString(R.string.logout_confirm),
            onClick = {
                onDismiss()
                onConfirm()
            }
        ),
        secondaryButton = DialogButtonConfig(
            text = context.getString(R.string.logout_close),
            onClick = onDismiss,
            isPrimary = false
        ),
        onDismiss = onDismiss
    )
}

