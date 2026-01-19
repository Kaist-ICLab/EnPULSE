package kaist.iclab.mobiletracker.ui.screens.SettingsScreen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.ui.theme.AppColors

@Composable
fun SettingsMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    description: String? = null,
    iconTint: Color = AppColors.PrimaryColor
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = Styles.MENU_ITEM_HORIZONTAL_PADDING,
                vertical = Styles.MENU_ITEM_VERTICAL_PADDING
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Styles.ICON_SIZE)
        )
        Spacer(Modifier.width(Styles.ICON_SPACER_WIDTH))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = Styles.TEXT_FONT_SIZE,
            )
            if (description != null) {
                Text(
                    text = description,
                    color = AppColors.PrimaryColor,
                    fontSize = Styles.DESCRIPTION_FONT_SIZE,
                    modifier = Modifier.padding(top = Styles.DESCRIPTION_TOP_PADDING)
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

/**
 * Gets the current language display name for the language menu item
 */
fun getLanguageDisplayName(context: Context): String {
    val languageHelper = LanguageHelper(context)
    val currentLanguage = languageHelper.getLanguage()
    return when (currentLanguage) {
        "ko" -> context.getString(R.string.language_korean_full)
        "en" -> context.getString(R.string.language_english_full)
        else -> context.getString(R.string.language_english_full)
    }
}

/**
 * Composable that renders a menu item with an optional divider below it
 */
@Composable
fun SettingsMenuItemWithDivider(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    description: String? = null,
    showDivider: Boolean = true,
    iconTint: Color = AppColors.PrimaryColor
) {
    SettingsMenuItem(
        title = title,
        icon = icon,
        onClick = onClick,
        description = description,
        iconTint = iconTint
    )
    if (showDivider) {
        HorizontalDivider(
            color = AppColors.BorderDark,
            thickness = 0.dp
        )
    }
}
