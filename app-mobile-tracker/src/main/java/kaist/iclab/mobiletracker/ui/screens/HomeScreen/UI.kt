package kaist.iclab.mobiletracker.ui.screens.HomeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.WatchConnectionStatus
import kaist.iclab.mobiletracker.ui.theme.AppColors

@Composable
fun TrackingStatusCard(
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Styles.STATUS_CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = Styles.STATUS_CARD_ELEVATION)
    ) {
        Column(
            modifier = Modifier
                .padding(Styles.STATUS_CARD_PADDING)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.home_tracking_status),
                    fontSize = Styles.STATUS_TITLE_FONT_SIZE,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                StatusIndicator(isActive = isActive)
            }

        }
    }
}

@Composable
fun GalaxyWatchCard(
    watchStatus: WatchConnectionStatus,
    connectedDevices: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Styles.STATUS_CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = Styles.STATUS_CARD_ELEVATION)
    ) {
        Column(
            modifier = Modifier
                .padding(Styles.STATUS_CARD_PADDING)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.home_watch_status_label),
                    fontSize = Styles.STATUS_TITLE_FONT_SIZE,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                WatchStatusIndicator(status = watchStatus)
            }

            if (connectedDevices.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Styles.STATUS_SUBTITLE_TOP_PADDING),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_watch_name_label),
                        fontSize = Styles.STATUS_SUBTITLE_FONT_SIZE,
                        color = AppColors.TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connectedDevices.first(),
                        fontSize = Styles.STATUS_SUBTITLE_FONT_SIZE,
                        color = AppColors.TextSecondary,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(isActive: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (isActive) Styles.Colors.RUNNING_BG else Styles.Colors.STOPPED_BG,
                shape = Styles.INDICATOR_SHAPE
            )
            .padding(
                horizontal = Styles.INDICATOR_HORIZONTAL_PADDING,
                vertical = Styles.INDICATOR_VERTICAL_PADDING
            )
    ) {
        Text(
            text = if (isActive) stringResource(R.string.home_status_running) else stringResource(R.string.home_status_stopped),
            fontSize = Styles.INDICATOR_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Styles.Colors.RUNNING_TEXT else Styles.Colors.STOPPED_TEXT
        )
    }
}

@Composable
fun WatchStatusIndicator(status: WatchConnectionStatus) {
    val bg = when (status) {
        WatchConnectionStatus.CONNECTED -> Styles.Colors.RUNNING_BG
        WatchConnectionStatus.NOT_INSTALLED -> Styles.Colors.WARNING_BG
        WatchConnectionStatus.DISCONNECTED -> Styles.Colors.STOPPED_BG
    }
    val text = when (status) {
        WatchConnectionStatus.CONNECTED -> Styles.Colors.RUNNING_TEXT
        WatchConnectionStatus.NOT_INSTALLED -> Styles.Colors.WARNING_TEXT
        WatchConnectionStatus.DISCONNECTED -> Styles.Colors.STOPPED_TEXT
    }
    val label = when (status) {
        WatchConnectionStatus.CONNECTED -> stringResource(R.string.home_watch_connected)
        WatchConnectionStatus.NOT_INSTALLED -> stringResource(R.string.home_watch_not_installed)
        WatchConnectionStatus.DISCONNECTED -> stringResource(R.string.home_watch_disconnected)
    }

    Box(
        modifier = Modifier
            .background(color = bg, shape = Styles.INDICATOR_SHAPE)
            .padding(
                horizontal = Styles.INDICATOR_HORIZONTAL_PADDING,
                vertical = Styles.INDICATOR_VERTICAL_PADDING
            )
    ) {
        Text(
            text = label,
            fontSize = Styles.INDICATOR_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            color = text
        )
    }
}

@Composable
fun InsightRow(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Styles.INSIGHT_ROW_SHAPE,
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = Styles.INSIGHT_ROW_PADDING_HORIZONTAL,
                    vertical = Styles.INSIGHT_ROW_PADDING_VERTICAL
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(Styles.INSIGHT_ROW_ICON_SIZE)
            )

            Spacer(modifier = Modifier.width(Styles.INSIGHT_ROW_PADDING_HORIZONTAL))

            Text(
                text = title,
                fontSize = Styles.INSIGHT_ROW_LABEL_FONT_SIZE,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value,
                fontSize = Styles.INSIGHT_ROW_VALUE_FONT_SIZE,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
