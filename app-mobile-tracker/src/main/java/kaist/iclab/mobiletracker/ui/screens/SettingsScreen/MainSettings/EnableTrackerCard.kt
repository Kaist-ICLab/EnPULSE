package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.MainSettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.components.Toggle.Toggle
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * Composable for the Enable Tracker card with toggle switch
 */
@Composable
fun EnableTrackerCard(
    isCollecting: Boolean,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Styles.CARD_CONTAINER_HORIZONTAL_PADDING),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        shape = Styles.CARD_SHAPE
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = isEnabled,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        onToggle(!isCollecting)
                    }
                )
                .padding(
                    horizontal = Styles.MENU_ITEM_HORIZONTAL_PADDING,
                    vertical = Styles.ENABLE_TRACKER_VERTICAL_PADDING
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = context.getString(R.string.enable_tracker_title),
                    color = AppColors.TextPrimary,
                    fontSize = Styles.TEXT_FONT_SIZE,
                    lineHeight = Styles.TEXT_LINE_HEIGHT,
                    modifier = Modifier.padding(top = Styles.TEXT_TOP_PADDING)
                )
                Text(
                    text = context.getString(R.string.enable_tracker_description),
                    color = AppColors.TextSecondary,
                    fontSize = Styles.DESCRIPTION_FONT_SIZE,
                    lineHeight = Styles.DESCRIPTION_LINE_HEIGHT,
                    modifier = Modifier.padding(top = Styles.DESCRIPTION_TOP_PADDING)
                )
            }
            Spacer(Modifier.width(Styles.ICON_SPACER_WIDTH))
            Toggle(
                checked = isCollecting,
                onCheckedChange = onToggle,
                enabled = isEnabled
            )
        }
    }
}

