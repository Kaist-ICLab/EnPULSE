package kaist.iclab.mobiletracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * Standard Application Header Component
 */
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Styles.HEADER_HEIGHT)
            .padding(start = Styles.HEADER_START_PADDING, end = Styles.HEADER_END_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = Styles.HEADER_FONT_SIZE
        )
    }
}

/**
 * Standard Application Menu Item Component
 */
@Composable
fun AppMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    iconTint: Color = AppColors.PrimaryColor,
    trailingContent: @Composable (() -> Unit)? = null
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
        Text(
            text = title,
            color = AppColors.TextPrimary,
            fontSize = Styles.TEXT_FONT_SIZE,
            modifier = Modifier.weight(1f)
        )
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextSecondary
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            color = AppColors.BorderDark,
            thickness = 0.dp
        )
    }
}
