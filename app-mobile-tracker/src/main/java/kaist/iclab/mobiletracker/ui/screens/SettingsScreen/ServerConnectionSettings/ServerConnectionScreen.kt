package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.ServerConnectionSettings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.MainActivity
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.config.SupabaseConfigManager
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog

@Composable
fun ServerConnectionScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val configManager = remember { SupabaseConfigManager(context) }
    
    var url by remember { mutableStateOf(configManager.getCustomUrlRaw()) }
    var anonKey by remember { mutableStateOf(configManager.getCustomAnonKeyRaw()) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isCheckingConnection by remember { mutableStateOf(false) }
    var connectionCheckResult by remember { mutableStateOf<Boolean?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Styles.HEADER_HEIGHT)
                    .padding(horizontal = Styles.HEADER_HORIZONTAL_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = context.getString(R.string.back_desc)
                    )
                }
                Text(
                    text = context.getString(R.string.menu_server_connection),
                    fontWeight = FontWeight.Bold,
                    fontSize = Styles.TITLE_FONT_SIZE
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                Text(
                text = context.getString(R.string.server_config_description),
                color = AppColors.TextSecondary,
                fontSize = Styles.SCREEN_DESCRIPTION_FONT_SIZE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Styles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING,
                        end = Styles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING,
                        bottom = Styles.SCREEN_DESCRIPTION_BOTTOM_PADDING
                    )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Styles.CARD_CONTAINER_HORIZONTAL_PADDING, end = Styles.CARD_CONTAINER_HORIZONTAL_PADDING, bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.White),
                shape = Styles.CARD_SHAPE
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = context.getString(R.string.server_config_current_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val currentUrl = configManager.getUrl()
                    val currentKey = configManager.getAnonKey()
                    val truncatedKey = if (currentKey.length > 20) "${currentKey.take(15)}...${currentKey.takeLast(10)}" else currentKey
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.server_config_current_url, currentUrl),
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Supabase URL", currentUrl)
                                clipboard.setPrimaryClip(clip)
                                AppToast.show(context, context.getString(R.string.copied_to_clipboard))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = context.getString(R.string.copy_url_desc),
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.server_config_current_key, truncatedKey),
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Supabase Anon Key", currentKey)
                                clipboard.setPrimaryClip(clip)
                                AppToast.show(context, context.getString(R.string.copied_to_clipboard))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = context.getString(R.string.copy_key_desc),
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Styles.CARD_CONTAINER_HORIZONTAL_PADDING),
                colors = CardDefaults.cardColors(containerColor = AppColors.White),
                shape = Styles.CARD_SHAPE
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            connectionCheckResult = null
                        },
                        label = { Text(context.getString(R.string.server_config_url_label), fontSize = 12.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(kaist.iclab.mobiletracker.ui.theme.Dimens.CornerRadiusMedium),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.PrimaryColor,
                            unfocusedBorderColor = AppColors.TextSecondary.copy(alpha = 0.5f),
                            errorBorderColor = AppColors.ErrorColor,
                            focusedLabelColor = AppColors.PrimaryColor,
                            unfocusedLabelColor = AppColors.TextSecondary
                        )
                    )

                    OutlinedTextField(
                        value = anonKey,
                        onValueChange = {
                            anonKey = it
                            connectionCheckResult = null
                        },
                        label = { Text(context.getString(R.string.server_config_key_label), fontSize = 12.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(kaist.iclab.mobiletracker.ui.theme.Dimens.CornerRadiusMedium),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.PrimaryColor,
                            unfocusedBorderColor = AppColors.TextSecondary.copy(alpha = 0.5f),
                            errorBorderColor = AppColors.ErrorColor,
                            focusedLabelColor = AppColors.PrimaryColor,
                            unfocusedLabelColor = AppColors.TextSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isCheckingConnection = true
                            connectionCheckResult = null
                            coroutineScope.launch {
                                val success = checkConnection(url, anonKey)
                                connectionCheckResult = success
                                isCheckingConnection = false
                            }
                        },
                        enabled = !isCheckingConnection && url.isNotBlank() && anonKey.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.PrimaryColor,
                            disabledContainerColor = AppColors.BorderLight
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        if (isCheckingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(context.getString(R.string.server_config_testing_connection), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text(context.getString(R.string.server_config_test_connection), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    connectionCheckResult?.let { success ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (success) {
                                context.getString(R.string.server_config_connection_success)
                            } else {
                                context.getString(R.string.server_config_connection_failed)
                            },
                            color = if (success) AppColors.PrimaryColor else AppColors.ErrorColor,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showConfirmationDialog = true
                        },
                        enabled = connectionCheckResult == true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.PrimaryColor,
                            disabledContainerColor = AppColors.BorderLight
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.server_config_save_restart),
                            color = if (connectionCheckResult == true) Color.White else AppColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            }
        }
    }

    if (showConfirmationDialog) {
        PopupDialog(
            title = context.getString(R.string.server_config_restart_confirm_title),
            content = {
                Text(
                    text = context.getString(R.string.server_config_restart_confirm_message),
                    color = AppColors.TextSecondary,
                    fontSize = 15.sp
                )
            },
            primaryButton = DialogButtonConfig(
                text = context.getString(R.string.server_config_restart_confirm_button),
                onClick = {
                    showConfirmationDialog = false
                    configManager.saveCredentials(url, anonKey)
                    val intent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }
            ),
            secondaryButton = DialogButtonConfig(
                text = context.getString(R.string.cancel),
                onClick = { showConfirmationDialog = false },
                isPrimary = false
            ),
            onDismiss = { showConfirmationDialog = false }
        )
    }
}

private suspend fun checkConnection(urlStr: String, anonKey: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val cleanedUrl = urlStr.trim().removeSuffix("/")
        val restUrl = "$cleanedUrl/rest/v1/"
        
        val url = URL(restUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("apikey", anonKey.trim())
        conn.setRequestProperty("Authorization", "Bearer ${anonKey.trim()}")
        
        val responseCode = conn.responseCode
        responseCode == 200
    } catch (e: java.lang.Exception) {
        false
    }
}
