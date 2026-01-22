package kaist.iclab.wearabletracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.theme.AppSizes
import kaist.iclab.wearabletracker.theme.AppTypography

@Composable
fun FlushConfirmationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        Dialog(
            showDialog = showDialog,
            onDismissRequest = onDismiss
        ) {
            Alert(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.flush_dialog_title),
                            style = AppTypography.dialogTitle,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.flush_dialog_message),
                            style = AppTypography.dialogBody,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                negativeButton = {
                    Button(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            modifier = Modifier.size(AppSizes.iconMedium)
                        )
                    }
                },
                positiveButton = {
                    Button(
                        onClick = onConfirm,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.confirm),
                            modifier = Modifier.size(AppSizes.iconMedium)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionPermanentlyDeniedDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (showDialog) {
        Dialog(
            showDialog = showDialog,
            onDismissRequest = onDismiss
        ) {
            Alert(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.permission_required_title),
                            style = AppTypography.dialogTitle,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.permission_required_message),
                            style = AppTypography.dialogBody,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                negativeButton = {
                    Button(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            modifier = Modifier.size(AppSizes.iconMedium)
                        )
                    }
                },
                positiveButton = {
                    Button(
                        onClick = onOpenSettings,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.confirm),
                            modifier = Modifier.size(AppSizes.iconMedium)
                        )
                    }
                }
            )
        }
    }
}
