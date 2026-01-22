package kaist.iclab.wearabletracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kaist.iclab.wearabletracker.R
import kaist.iclab.wearabletracker.data.DeviceInfo
import kaist.iclab.wearabletracker.theme.AppSpacing
import kaist.iclab.wearabletracker.theme.DeviceNameText
import kaist.iclab.wearabletracker.theme.SyncStatusText

@Composable
fun DeviceStatusInfo(
    deviceInfo: DeviceInfo,
    lastSyncTimestamp: Long?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = AppSpacing.deviceInfoBottom,
                top = AppSpacing.deviceInfoTop
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DeviceNameText(text = deviceInfo.name)
        SyncStatusText(
            text = if (lastSyncTimestamp != null) {
                stringResource(R.string.last_sync_format, formatSyncTimestamp(lastSyncTimestamp))
            } else {
                stringResource(R.string.last_sync_placeholder)
            }
        )
    }
}

/**
 * Format the sync timestamp to "Last Sync: YYYYMMDD HH.mm" format.
 */
private fun formatSyncTimestamp(timestamp: Long): String {
    val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd HH.mm", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(timestamp))
}
