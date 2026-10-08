package kaist.iclab.mobiletracker.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.config.SupabaseConfigManager
import kaist.iclab.mobiletracker.helpers.ImageAsset
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.AppToast

/**
 * @param showEmailLogin whether the email/password fallback is offered. Google stays the
 *   primary path; the form is only revealed on request, for networks where Google's
 *   identity endpoints are unreachable but the configured backend is not.
 * @param errorMessage the latest sign-in error, shown under the email form.
 */
@Composable
fun LoginScreen(
    onSignInWithGoogle: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToServerConnection: () -> Unit,
    showEmailLogin: Boolean = false,
    onSignInWithEmail: (String, String) -> Unit = { _, _ -> },
    errorMessage: String? = null
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val configManager = remember { SupabaseConfigManager(context) }

    var emailFormVisible by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Hides an error once the user edits the form or it predates opening the form.
    var errorHidden by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Both sign-in paths talk to the configured backend, so neither is worth attempting
    // before a server is set. Sends the user to the server screen instead.
    fun ifConfigured(action: () -> Unit) {
        if (!configManager.isConfigured()) {
            AppToast.show(context, R.string.login_not_configured_error)
            onNavigateToServerConnection()
        } else {
            action()
        }
    }

    // Closes the keyboard first: it would otherwise cover both the toast and the error line.
    fun submitEmail() {
        if (email.isBlank() || password.isEmpty()) return
        focusManager.clearFocus()
        errorHidden = false
        ifConfigured { onSignInWithEmail(email, password) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // Settings dropdown at top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Styles.LANGUAGE_DROPDOWN_PADDING)
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = AppColors.TextSecondary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = AppColors.Background
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_language)) },
                    onClick = {
                        expanded = false
                        onNavigateToLanguage()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.server_config_title)) },
                    onClick = {
                        expanded = false
                        onNavigateToServerConnection()
                    }
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Styles.CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ImageAsset(
                    assetPath = "icon.png",
                    contentDescription = stringResource(R.string.mobile_tracker_logo),
                    modifier = Modifier.size(Styles.LOGO_SIZE)
                )
                Spacer(modifier = Modifier.width(Styles.LOGO_TITLE_SPACING))
                Text(
                    text = stringResource(R.string.mobile_tracker),
                    fontSize = Styles.TITLE_FONT_SIZE,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.Black,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            Spacer(modifier = Modifier.height(Styles.CONTENT_SPACING))
            // One sign-in method at a time: the Google button is hidden while the email form is
            // open, and "Use Google instead" closes the form to bring it back.
            if (!emailFormVisible) Button(
                onClick = { ifConfigured { onSignInWithGoogle() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Styles.BUTTON_HEIGHT),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                border = BorderStroke(Styles.BUTTON_BORDER_WIDTH, AppColors.BorderLight),
                shape = Styles.BUTTON_SHAPE
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google "G" logo
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(Styles.BUTTON_ICON_SIZE),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(Styles.BUTTON_ICON_TITLE_SPACING))
                    Text(
                        text = stringResource(R.string.sign_in_with_google),
                        fontSize = Styles.BUTTON_TEXT_FONT_SIZE,
                        color = Color.Black
                    )
                }
            }

            if (showEmailLogin) {
                if (!emailFormVisible) {
                    Spacer(modifier = Modifier.height(Styles.FORM_SECTION_SPACING))
                    TextButton(onClick = {
                        emailFormVisible = true
                        errorHidden = true
                    }) {
                        Text(
                            text = stringResource(R.string.sign_in_with_email),
                            fontSize = Styles.FORM_LABEL_FONT_SIZE,
                            color = AppColors.PrimaryColor
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorHidden = true
                        },
                        label = { Text(stringResource(R.string.login_email_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Styles.FORM_FIELD_SPACING))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorHidden = true
                        },
                        label = { Text(stringResource(R.string.login_password_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitEmail() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null && !errorHidden) {
                        Spacer(modifier = Modifier.height(Styles.FORM_FIELD_SPACING))
                        Text(
                            text = errorMessage,
                            color = AppColors.ErrorColor,
                            fontSize = Styles.FORM_LABEL_FONT_SIZE,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(Styles.FORM_FIELD_SPACING))
                    Button(
                        onClick = { submitEmail() },
                        enabled = email.isNotBlank() && password.isNotEmpty(),
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
                            text = stringResource(R.string.login_email_submit),
                            fontSize = Styles.BUTTON_TEXT_FONT_SIZE
                        )
                    }
                    TextButton(onClick = {
                        emailFormVisible = false
                        password = ""
                    }) {
                        Text(
                            text = stringResource(R.string.login_use_google_instead),
                            fontSize = Styles.FORM_LABEL_FONT_SIZE,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

