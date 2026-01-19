package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.LanguageSettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kaist.iclab.mobiletracker.ui.theme.AppColors

@Composable
fun LanguageItem(
    number: Int,
    languageName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(
                horizontal = Styles.ITEM_HORIZONTAL_PADDING,
                vertical = Styles.ITEM_VERTICAL_PADDING
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number
        Text(
            text = number.toString(),
            color = AppColors.PrimaryColor,
            fontSize = Styles.TEXT_FONT_SIZE,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(Styles.NUMBER_WIDTH)
        )
        Spacer(Modifier.width(Styles.ICON_SPACER_WIDTH))

        // Language name
        Text(
            text = languageName,
            color = AppColors.TextPrimary,
            fontSize = Styles.TEXT_FONT_SIZE,
            modifier = Modifier.weight(1f)
        )

        // Checkmark icon if selected
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = AppColors.PrimaryColor,
                modifier = Modifier.size(Styles.CHECKMARK_SIZE)
            )
        }
    }
}
