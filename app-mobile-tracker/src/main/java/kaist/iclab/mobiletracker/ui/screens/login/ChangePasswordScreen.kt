package kaist.iclab.mobiletracker.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * Shown after a password sign-in while the profile's `reset_password` is still false: the
 * account was set up with an initial password, and the user has to choose their own before
 * going on. Leaving happens through navigation once the profile flag flips, or by logging out.
 *
 * @param errorMessage the latest failure from the server, shown under the form.
 * @param onClearError called when the user edits the form, so a stale error goes away.
 */
@Composable
fun ChangePasswordScreen(
    onChangePassword: (String) -> Unit,
    onLogout: () -> Unit,
    errorMessage: String?,
    onClearError: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    // Only flagged after a submit attempt, so the error doesn't appear while still typing.
    var showMismatch by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (password.isEmpty() || confirm.isEmpty()) return
        if (password != confirm) {
            showMismatch = true
            return
        }
        focusManager.clearFocus()
        onChangePassword(password)
    }

    fun edited() {
        showMismatch = false
        onClearError()
    }

    val shownError = if (showMismatch) {
        stringResource(R.string.change_password_mismatch)
    } else {
        errorMessage
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(Styles.CONTENT_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.change_password_title),
            fontSize = Styles.TITLE_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(Styles.FORM_FIELD_SPACING))
        Text(
            text = stringResource(R.string.change_password_description),
            fontSize = Styles.FORM_LABEL_FONT_SIZE,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Styles.CONTENT_SPACING))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                edited()
            },
            label = { Text(stringResource(R.string.change_password_new_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Styles.FORM_FIELD_SPACING))
        OutlinedTextField(
            value = confirm,
            onValueChange = {
                confirm = it
                edited()
            },
            label = { Text(stringResource(R.string.change_password_confirm_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth()
        )
        if (shownError != null) {
            Spacer(modifier = Modifier.height(Styles.FORM_FIELD_SPACING))
            Text(
                text = shownError,
                color = AppColors.ErrorColor,
                fontSize = Styles.FORM_LABEL_FONT_SIZE,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(Styles.FORM_FIELD_SPACING))
        Button(
            onClick = { submit() },
            enabled = password.isNotEmpty() && confirm.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(Styles.BUTTON_HEIGHT),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.PrimaryColor,
                contentColor = Color.White
            ),
            shape = Styles.BUTTON_SHAPE
        ) {
            Text(
                text = stringResource(R.string.change_password_submit),
                fontSize = Styles.BUTTON_TEXT_FONT_SIZE
            )
        }
        Spacer(modifier = Modifier.height(Styles.FORM_FIELD_SPACING))
        TextButton(onClick = onLogout) {
            Text(
                text = stringResource(R.string.logout_title),
                fontSize = Styles.FORM_LABEL_FONT_SIZE,
                color = AppColors.TextSecondary
            )
        }
    }
}
