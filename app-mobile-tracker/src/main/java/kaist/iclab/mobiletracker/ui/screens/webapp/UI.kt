package kaist.iclab.mobiletracker.ui.screens.webapp

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.webapp.WebAppActivity
import kaist.iclab.mobiletracker.webapp.WebAppConfig
import androidx.core.net.toUri
import kaist.iclab.mobiletracker.webapp.WebAppIconCache
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable row representing a single WebApp item within the list.
 * Displays the WebApp name (ID), its URL, and a chevron pointing right to indicate clickability.
 *
 * @param webApp Configuration details of the WebApp to display.
 * @param onClick Callback triggered when this row is clicked.
 * @param modifier Layout modifier applied to the row.
 */
@Composable
fun WebAppRow(
    webApp: WebAppConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isPinSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    val iconCache: WebAppIconCache = koinInject()
    val scope = rememberCoroutineScope()

    // Downloaded/decoded icon for this webapp, if it has one set. Falls back to the default
    // Public glyph (below) while loading, on failure, or when no icon is configured.
    var iconBitmap by remember(webApp.iconPath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(webApp.iconPath) {
        iconBitmap = iconCache.getBitmap(webApp.iconPath)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = Styles.CARD_HORIZONTAL_PADDING,
                vertical = Styles.CARD_VERTICAL_PADDING
            )
    ) {
        val bitmap = iconBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = webApp.name,
                modifier = Modifier.size(Styles.ICON_SIZE)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Public,
                contentDescription = webApp.name,
                modifier = Modifier.size(Styles.ICON_SIZE),
                tint = AppColors.IconWebApp
            )
        }
        Spacer(Modifier.width(Styles.ICON_SPACER_WIDTH))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = webApp.name,
                color = AppColors.TextPrimary,
                fontSize = Styles.TEXT_FONT_SIZE,
                lineHeight = Styles.TEXT_LINE_HEIGHT,
                modifier = Modifier.padding(top = Styles.TEXT_TOP_PADDING)
            )
            Text(
                text = webApp.url,
                color = AppColors.TextSecondary,
                fontSize = Styles.CARD_DESCRIPTION_FONT_SIZE,
                lineHeight = Styles.CARD_DESCRIPTION_LINE_HEIGHT,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(
                        top = Styles.CARD_DESCRIPTION_TOP_PADDING,
                        bottom = Styles.CARD_DESCRIPTION_BOTTOM_PADDING
                    )
            )
        }
        
        if (isPinSupported) {
            TextButton(
                onClick = {
                    scope.launch {
                        val intent = Intent(context, WebAppActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            data = "webapp://${webApp.id}".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(WebAppActivity.EXTRA_URL, webApp.url)
                            putExtra(WebAppActivity.EXTRA_WEBAPP_ID, webApp.id)
                        }
                        // Reuse the row's already-downloaded icon when available; otherwise fall
                        // back to the app launcher icon (same default as before this icon exists).
                        val icon = (iconBitmap ?: iconCache.getBitmap(webApp.iconPath))
                            ?.let { IconCompat.createWithBitmap(it) }
                            ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher)
                        val shortcutInfo = ShortcutInfoCompat.Builder(context, "webapp_${webApp.id}")
                            .setShortLabel(webApp.name)
                            .setLongLabel(webApp.name)
                            .setIcon(icon)
                            .setIntent(intent)
                            .build()
                        ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Add to Home Screen",
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.TextSecondary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.web_app_add_to_home),
                    fontSize = Styles.CARD_DESCRIPTION_FONT_SIZE,
                    color = AppColors.TextSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextSecondary
        )
    }
}
