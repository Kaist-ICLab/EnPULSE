package kaist.iclab.mobiletracker.ui.components.campaign

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.ui.components.popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.ui.theme.Dimens
import kaist.iclab.mobiletracker.utils.AppToast
import kotlinx.coroutines.launch

/**
 * Dialog for entering campaign password
 */
@Composable
fun PasswordDialog(
    campaignName: String,
    onDismiss: () -> Unit,
    onVerify: suspend (String) -> Result<Boolean>,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    PopupDialog(
        title = stringResource(R.string.campaign_password_title),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.campaign_password_description),
                    fontSize = Styles.ExperimentNameFontSize,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = { Text(stringResource(R.string.campaign_password_input_label)) },
                    singleLine = true,
                    isError = passwordError != null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (passwordError != null) AppColors.ErrorColor else AppColors.PrimaryColor
                        )
                    },
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Default.Visibility
                        else Icons.Default.VisibilityOff

                        val description = if (passwordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = description,
                                tint = AppColors.TextSecondary
                            )
                        }
                    },
                    supportingText = {
                        if (passwordError != null) {
                            Text(
                                text = passwordError!!,
                                color = AppColors.ErrorColor,
                                fontSize = Dimens.FontSizeMicro
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
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
            text = stringResource(R.string.campaign_password_verify),
            onClick = {
                if (password.isNotEmpty()) {
                    isVerifying = true
                    passwordError = null

                    coroutineScope.launch {
                        try {
                            val result = onVerify(password)
                            if (result is Result.Success && result.data) {
                                onSuccess()
                            } else {
                                val errorMsg = if (result is Result.Error) {
                                    val exception = result.exception
                                    val extracted = ErrorClassifier.extractErrorMessage(exception)
                                    val raw = exception.message
                                        ?: resources.getString(R.string.campaign_password_invalid)
                                    val cleanMsg = if (raw.startsWith("joinCampaign: ")) {
                                        raw.removePrefix("joinCampaign: ")
                                    } else {
                                        raw
                                    }
                                    extracted ?: cleanMsg
                                } else {
                                    resources.getString(R.string.campaign_password_invalid)
                                }
                                passwordError = errorMsg
                                AppToast.show(context, errorMsg)
                            }
                        } catch (e: Exception) {
                            val extracted = ErrorClassifier.extractErrorMessage(e)
                            val raw =
                                e.message ?: resources.getString(R.string.campaign_password_error)
                            val cleanMsg = if (raw.startsWith("joinCampaign: ")) {
                                raw.removePrefix("joinCampaign: ")
                            } else {
                                raw
                            }
                            val errorMsg = extracted ?: cleanMsg
                            passwordError = errorMsg
                            AppToast.show(context, errorMsg)
                        } finally {
                            isVerifying = false
                        }
                    }
                }
            },
            enabled = !isVerifying && password.isNotEmpty()
        ),
        secondaryButton = DialogButtonConfig(
            text = stringResource(R.string.campaign_dialog_cancel),
            onClick = {
                onDismiss()
            },
            isPrimary = false
        ),
        onDismiss = onDismiss
    )
}
