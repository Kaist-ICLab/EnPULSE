package kaist.iclab.mobiletracker.ui.components.server

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.config.SupabaseConfigManager
import kaist.iclab.mobiletracker.ui.components.campaign.Styles
import kaist.iclab.mobiletracker.ui.components.popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.ui.theme.Dimens

@Composable
fun SupabaseConfigDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    val configManager = remember { SupabaseConfigManager(context) }

    var url by remember { mutableStateOf(configManager.getCustomUrlRaw()) }
    var anonKey by remember { mutableStateOf(configManager.getCustomAnonKeyRaw()) }

    PopupDialog(
        title = stringResource(R.string.server_config_title),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.server_config_description),
                    fontSize = Styles.ExperimentNameFontSize,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.server_config_url_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.PrimaryColor,
                        unfocusedBorderColor = AppColors.TextSecondary.copy(alpha = 0.5f),
                        errorBorderColor = AppColors.ErrorColor,
                        focusedLabelColor = AppColors.PrimaryColor,
                        unfocusedLabelColor = AppColors.TextSecondary
                    )
                )

                OutlinedTextField(
                    value = anonKey,
                    onValueChange = { anonKey = it },
                    label = { Text(stringResource(R.string.server_config_key_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.PrimaryColor,
                        unfocusedBorderColor = AppColors.TextSecondary.copy(alpha = 0.5f),
                        errorBorderColor = AppColors.ErrorColor,
                        focusedLabelColor = AppColors.PrimaryColor,
                        unfocusedLabelColor = AppColors.TextSecondary
                    )
                )
            }
        },
        primaryButton = DialogButtonConfig(
            text = stringResource(R.string.server_config_save),
            onClick = { onSave(url, anonKey) },
            enabled = url.isNotBlank() && anonKey.isNotBlank()
        ),
        secondaryButton = DialogButtonConfig(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
            isPrimary = false
        ),
        onDismiss = onDismiss
    )
}
