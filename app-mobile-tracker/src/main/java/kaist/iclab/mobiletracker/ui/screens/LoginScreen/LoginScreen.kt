package kaist.iclab.mobiletracker.ui.screens.LoginScreen

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.config.SupabaseConfigManager
import kaist.iclab.mobiletracker.helpers.ImageAsset
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.AppToast

@Composable
fun LoginScreen(
    onSignInWithGoogle: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToServerConnection: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val configManager = remember { SupabaseConfigManager(context) }

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
                    text = { Text(context.getString(R.string.menu_language)) },
                    onClick = {
                        expanded = false
                        onNavigateToLanguage()
                    }
                )
                DropdownMenuItem(
                    text = { Text(context.getString(R.string.server_config_title)) },
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
                    contentDescription = context.getString(R.string.mobile_tracker_logo),
                    modifier = Modifier.size(Styles.LOGO_SIZE)
                )
                Spacer(modifier = Modifier.width(Styles.LOGO_TITLE_SPACING))
                Text(
                    text = context.getString(R.string.mobile_tracker),
                    fontSize = Styles.TITLE_FONT_SIZE,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.Black,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            Spacer(modifier = Modifier.height(Styles.CONTENT_SPACING))
            Button(
                onClick = {
                    if (!configManager.isConfigured()) {
                        AppToast.show(context, R.string.login_not_configured_error)
                        onNavigateToServerConnection()
                    } else {
                        onSignInWithGoogle()
                    }
                },
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
                        text = context.getString(R.string.sign_in_with_google),
                        fontSize = Styles.BUTTON_TEXT_FONT_SIZE,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

