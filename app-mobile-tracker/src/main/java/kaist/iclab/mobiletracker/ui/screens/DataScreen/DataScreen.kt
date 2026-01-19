package kaist.iclab.mobiletracker.ui.screens.DataScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.navigation.Screen
import kaist.iclab.mobiletracker.repository.SensorInfo
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.ui.utils.getSensorDisplayName
import kaist.iclab.mobiletracker.ui.utils.getSensorIcon
import kaist.iclab.mobiletracker.ui.theme.Dimens
import kaist.iclab.mobiletracker.viewmodels.data.DataViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Data screen - displays a list of all sensors with their record counts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    navController: NavController,
    viewModel: DataViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showUploadConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullToRefreshState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Styles.SCREEN_HORIZONTAL_PADDING)
        ) {
            Spacer(modifier = Modifier.height(Styles.TOP_SPACER_HEIGHT))

            // Header with title
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.data_screen_title),
                    fontSize = Styles.TITLE_FONT_SIZE,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.data_screen_description),
                    fontSize = Styles.DESCRIPTION_FONT_SIZE,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = Styles.SUBTITLE_TOP_PADDING)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.data_screen_subtitle, uiState.totalRecords),
                        fontSize = Styles.SUBTITLE_FONT_SIZE,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(Styles.SECTION_SPACING))
        }

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.weight(1f),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = uiState.isLoading,
                    containerColor = AppColors.White,
                    color = AppColors.PrimaryColor,
                    state = pullRefreshState
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Styles.SCREEN_HORIZONTAL_PADDING)
            ) {
                when {
                    !uiState.isLoading && uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.error ?: "",
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(Styles.ITEM_SPACING),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Dimens.ScreenVerticalPadding)
                        ) {
                            item {
                                BulkActionButtons(
                                    isUploading = uiState.isUploading,
                                    isDeleting = uiState.isDeleting,
                                    isExporting = uiState.isExporting,
                                    onUploadClick = { showUploadConfirm = true },
                                    onDeleteClick = { showDeleteConfirm = true },
                                    onExportClick = { viewModel.exportAllToCsv() }
                                )
                            }

                            items(uiState.sensors) { sensor ->
                                SensorListItem(
                                    sensor = sensor,
                                    onClick = { 
                                        navController.navigate(Screen.SensorDetail.createRoute(sensor.sensorId))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showUploadConfirm) {
        PopupDialog(
            title = stringResource(R.string.sensor_upload_data_confirm),
            content = {
                Text(
                    text = stringResource(R.string.sensor_upload_data_message).replace("this sensor", "all sensors"),
                    fontSize = Dimens.FontSizeBody,
                    color = AppColors.TextPrimary
                )
            },
            primaryButton = DialogButtonConfig(
                text = stringResource(R.string.logout_confirm),
                onClick = {
                    viewModel.uploadAllData()
                    showUploadConfirm = false
                }
            ),
            secondaryButton = DialogButtonConfig(
                text = stringResource(R.string.logout_close),
                onClick = { showUploadConfirm = false },
                isPrimary = false
            ),
            onDismiss = { showUploadConfirm = false }
        )
    }

    if (showDeleteConfirm) {
        PopupDialog(
            title = stringResource(R.string.sync_clear_data_title),
            content = {
                Text(
                    text = stringResource(R.string.sync_clear_data_message),
                    fontSize = Dimens.FontSizeBody,
                    color = AppColors.TextPrimary
                )
            },
            primaryButton = DialogButtonConfig(
                text = stringResource(R.string.sync_clear_data_confirm),
                onClick = {
                    viewModel.deleteAllData()
                    showDeleteConfirm = false
                }
            ),
            secondaryButton = DialogButtonConfig(
                text = stringResource(R.string.sync_clear_data_cancel),
                onClick = { showDeleteConfirm = false },
                isPrimary = false
            ),
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun BulkActionButtons(
    isUploading: Boolean,
    isDeleting: Boolean,
    isExporting: Boolean,
    onUploadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = Styles.CARD_ELEVATION),
        shape = Styles.CARD_SHAPE
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Export Button
            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.SecondaryColor),
                enabled = !isUploading && !isDeleting && !isExporting,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AppColors.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.sensor_export_csv), fontSize = Dimens.FontSizeBody)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = onUploadClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryColor),
                    enabled = !isUploading && !isDeleting && !isExporting,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppColors.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.sync_start_data_upload), fontSize = Dimens.FontSizeBody)
                        }
                    }
                }

                Button(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.ErrorColor.copy(alpha = 0.1f)),
                    enabled = !isUploading && !isDeleting,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppColors.ErrorColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp),
                                tint = AppColors.ErrorColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sync_delete_data), 
                                color = AppColors.ErrorColor,
                                fontSize = Dimens.FontSizeBody
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorListItem(
    sensor: SensorInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Styles.CARD_SHAPE)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = Styles.CARD_ELEVATION)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Styles.CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with colored background
            Box(
                modifier = Modifier
                    .size(Styles.ICON_CONTAINER_SIZE)
                    .clip(RoundedCornerShape(Styles.ICON_CORNER_RADIUS))
                    .background(AppColors.getSensorColor(sensor.sensorId).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getSensorIcon(sensor.sensorId),
                    contentDescription = getSensorDisplayName(sensor.sensorId),
                    tint = AppColors.getSensorColor(sensor.sensorId),
                    modifier = Modifier.size(Styles.ICON_SIZE)
                )
            }

            Spacer(modifier = Modifier.width(Styles.ICON_TEXT_SPACING))

            // Sensor info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getSensorDisplayName(sensor.sensorId),
                        fontSize = Styles.SENSOR_NAME_FONT_SIZE,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (sensor.isPhoneSensor) {
                        Spacer(modifier = Modifier.width(Styles.BADGE_SPACING))
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "Phone sensor",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(Styles.BADGE_SIZE)
                        )
                    }
                    if (sensor.isWatchSensor) {
                        Spacer(modifier = Modifier.width(Styles.BADGE_SPACING))
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = "Watch sensor",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(Styles.BADGE_SIZE)
                        )
                    }
                }
                Text(
                    text = formatLastRecorded(sensor.lastRecordedTime),
                    fontSize = Styles.LAST_RECORDED_FONT_SIZE,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            }

            // Record count
            Text(
                text = formatRecordCount(sensor.recordCount),
                fontSize = Styles.RECORD_COUNT_FONT_SIZE,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.width(Styles.CHEVRON_SPACING))

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(Styles.CHEVRON_SIZE)
            )
        }
    }
}

/**
 * Format the last recorded time as a relative string.
 */
@Composable
private fun formatLastRecorded(timestamp: Long?): String {
    if (timestamp == null) return stringResource(R.string.data_screen_last_recorded_none)
    
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$minutes min ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours hour${if (hours > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "$days day${if (days > 1) "s" else ""} ago"
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

/**
 * Format record count with K/M suffix for large numbers.
 */
private fun formatRecordCount(count: Int): String {
    return count.toString()
}


