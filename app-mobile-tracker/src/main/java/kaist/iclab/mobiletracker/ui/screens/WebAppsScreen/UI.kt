package kaist.iclab.mobiletracker.ui.screens.WebAppsScreen

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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.webapp.WebAppConfig

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
        Icon(
            imageVector = Icons.Filled.Public,
            contentDescription = webApp.name,
            modifier = Modifier.size(Styles.ICON_SIZE),
            tint = AppColors.IconWebApp
        )
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
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextSecondary
        )
    }
}
