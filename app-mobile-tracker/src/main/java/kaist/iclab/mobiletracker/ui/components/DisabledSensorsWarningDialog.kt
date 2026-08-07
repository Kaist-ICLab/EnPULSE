package kaist.iclab.mobiletracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.components.popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.popup.PopupDialog

/**
 * Confirmation dialog shown when the user starts data collection while one or more of the
 * active campaign's sensors are not enabled (permission missing or manually disabled).
 * Lists the affected sensors by their localized display name and lets the user either start
 * anyway or cancel and go fix sensor settings first.
 */
@Composable
fun DisabledSensorsWarningDialog(
    showDialog: Boolean,
    sensorNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        PopupDialog(
            title = stringResource(R.string.disabled_sensors_warning_title),
            content = {
                Column {
                    Text(
                        text = stringResource(R.string.disabled_sensors_warning_message),
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                    sensorNames.forEach { name ->
                        Text(
                            text = "• $name",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            primaryButton = DialogButtonConfig(
                text = stringResource(R.string.disabled_sensors_warning_start_anyway),
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
