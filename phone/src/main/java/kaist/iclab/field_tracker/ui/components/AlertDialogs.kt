package kaist.iclab.field_tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kaist.iclab.field_tracker.ui.components.userInputs.FilledButton
import kaist.iclab.field_tracker.ui.components.userInputs.OutlinedButton
import kaist.iclab.field_tracker.ui.theme.MainTheme

@Composable
fun BaseAlertDialog(
    showDialog: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    child: @Composable () -> Unit
) {
    if (showDialog) {
        Dialog(onDismissRequest = { onDismiss() }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .background(Color.White, shape = RoundedCornerShape(4.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    child()

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledButton(
                            modifier = Modifier.weight(1f),
                            text = "Confirm",
                            onClick = onConfirm
                        )
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            text = "Cancel",
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BaseAlertDialogPreview() {
    MainTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            BaseAlertDialog(
                showDialog = true,
                title = "Dialog Title",
                onDismiss = {},
                onConfirm = {}) {
                Text(
                    "This is a dialog message",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}