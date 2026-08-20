package kaist.iclab.mobiletracker.ui.screens.data

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.navigation.Screen
import kaist.iclab.mobiletracker.repository.SensorInfo
import kaist.iclab.mobiletracker.services.upload.SensorUploadResult
import kaist.iclab.mobiletracker.ui.components.popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.ui.theme.Dimens
import kaist.iclab.mobiletracker.ui.utils.getSensorDisplayName
import kaist.iclab.mobiletracker.ui.utils.getSensorIcon
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.viewmodels.data.DataUiEvent
import kaist.iclab.mobiletracker.viewmodels.data.DataViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
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

    // Collect one-shot UI events (toasts) from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DataUiEvent.ShowToast ->
                    AppToast.show(context, event.messageResId)
            }
        }
    }

    var showUploadConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var hideZeroData by remember { mutableStateOf(true) }

    val filteredSensors = if (hideZeroData) {
        uiState.sensors.filter { it.recordCount > 0 }
    } else {
        uiState.sensors
    }

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
                    modifier = Modifier.padding(top = Dimens.SpacingMicro)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.SpacingTiny),
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
                            contentPadding = PaddingValues(bottom = Dimens.ScreenVerticalPadding)
                        ) {
                            item {
                                SummaryCard(
                                    currentTime = uiState.currentTime,
                                    lastWatchData = uiState.lastWatchData,
                                    lastSuccessfulUpload = uiState.lastSuccessfulUpload,
                                    totalRecords = uiState.totalRecords,
                                    isUploading = uiState.uploadProgress?.isComplete == false,
                                    uploadingLabel = uiState.uploadProgress
                                        ?.takeIf { !it.isComplete && it.currentSensorName.isNotBlank() && it.totalBatches > 0 }
                                        ?.let {
                                            stringResource(
                                                R.string.sync_status_uploading_sensor,
                                                it.currentSensorName,
                                                it.currentBatch,
                                                it.totalBatches
                                            )
                                        },
                                    isDeleting = uiState.isDeleting,
                                    isExporting = uiState.isExporting,
                                    onUploadClick = { showUploadConfirm = true },
                                    onDeleteClick = { showDeleteConfirm = true },
                                    onExportClick = { viewModel.exportAllToCsv() }
                                )
                            }

                            // Toggle to hide/show empty sensors
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 0.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.data_screen_hide_empty_sensors),
                                        fontSize = Dimens.FontSizeSmall,
                                        color = AppColors.TextSecondary
                                    )
                                    Switch(
                                        checked = hideZeroData,
                                        onCheckedChange = { hideZeroData = it },
                                        modifier = Modifier.scale(0.75f),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AppColors.White,
                                            checkedTrackColor = AppColors.PrimaryColor,
                                            uncheckedThumbColor = AppColors.White,
                                            uncheckedTrackColor = AppColors.TextSecondary.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }

                            if (filteredSensors.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SensorsOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = AppColors.PrimaryColor
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(R.string.data_screen_no_result),
                                            fontSize = Dimens.FontSizeBody,
                                            color = AppColors.TextSecondary
                                        )
                                    }
                                }
                            } else {
                                items(filteredSensors) { sensor ->
                                    SensorListItem(
                                        sensor = sensor,
                                        onClick = {
                                            navController.navigate(
                                                Screen.SensorDetail.createRoute(
                                                    sensor.sensorId
                                                )
                                            )
                                        }
                                    )
                                }
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
                    text = stringResource(R.string.sensor_upload_data_message).replace(
                        "this sensor",
                        "all sensors"
                    ),
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

    // Upload Summary Dialog — the in-progress state itself is shown inline (non-blocking) in
    // SummaryCard below, not as a modal, so this only fires once the upload finishes. Per-sensor
    // record counts (not just success/fail) so subjects can see how much of what was attempted
    // actually reached the server, rather than a bare pass/fail flag.
    uiState.uploadProgress?.let { progress ->
        if (progress.isComplete) {
            PopupDialog(
                title = stringResource(R.string.upload_complete_title),
                content = {
                    if (progress.results.isEmpty() && progress.upToDateCount == 0) {
                        Text(
                            text = stringResource(R.string.upload_no_data),
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            progress.results.forEach { result ->
                                UploadResultRow(result)
                            }
                            if (progress.upToDateCount > 0) {
                                if (progress.results.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = stringResource(R.string.upload_uptodate_header, progress.upToDateCount),
                                    color = AppColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = progress.upToDateSensors.joinToString(", "),
                                    fontSize = 12.sp,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    }
                },
                primaryButton = DialogButtonConfig(
                    text = stringResource(R.string.ok),
                    onClick = { viewModel.clearUploadProgress() }
                ),
                onDismiss = { viewModel.clearUploadProgress() }
            )
        }
    }
}

@Composable
private fun UploadResultRow(result: SensorUploadResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = result.displayName,
            fontSize = 13.sp,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val percent = result.successRatePercent
        val valueText = if (percent != null) {
            stringResource(
                R.string.upload_result_row_value,
                formatRecordCount(result.succeededCount),
                formatRecordCount(result.attemptedCount),
                percent
            )
        } else {
            stringResource(R.string.upload_result_row_failed)
        }
        // 100% and no error reads as healthy (green); anything less than everything landing —
        // whether an outright error or some data quarantined — is flagged in red so a partial
        // success can't be mistaken for a clean one.
        val valueColor = if (percent == 100 && !result.isError) AppColors.SecondaryColor else AppColors.ErrorColor
        Text(
            text = valueText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun SummaryCard(
    currentTime: String,
    lastWatchData: String?,
    lastSuccessfulUpload: String?,
    totalRecords: Int,
    isUploading: Boolean,
    uploadingLabel: String?,
    isDeleting: Boolean,
    isExporting: Boolean,
    onUploadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = Styles.CARD_ELEVATION),
        shape = Styles.CARD_SHAPE
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.sensor_detail_summary),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Summary rows following SensorDetail style
            SummaryRow(
                label = stringResource(R.string.sync_current_time),
                value = currentTime
            )
            SummaryRow(
                label = stringResource(R.string.sync_last_watch_data),
                value = lastWatchData ?: "--"
            )
            SummaryRow(
                label = stringResource(R.string.sync_last_successful_upload),
                value = lastSuccessfulUpload ?: "--"
            )

            // Lightweight status indicator only — it names the sensor being uploaded and its
            // batch counts, but the progress bar itself lives in DataUploadService's notification
            // so it isn't duplicated in-app. Non-blocking either way: this never covers the screen
            // or stops navigation.
            if (isUploading || isDeleting || isExporting) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AppColors.PrimaryColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isUploading -> uploadingLabel
                                ?: stringResource(R.string.sync_status_uploading)
                            isDeleting -> stringResource(R.string.sync_status_deleting)
                            else -> stringResource(R.string.sync_status_exporting)
                        },
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }

            if (totalRecords > 0) {
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                // Export Button (Full width)
                Button(
                    onClick = onExportClick,
                    enabled = !isExporting && !isDeleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeightSmall),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.SecondaryColor,
                        contentColor = AppColors.White,
                        disabledContainerColor = AppColors.TextSecondary.copy(alpha = 0.3f),
                        disabledContentColor = AppColors.TextSecondary
                    ),
                    shape = RoundedCornerShape(Dimens.ButtonCornerRadiusSmall),
                    contentPadding = PaddingValues(
                        horizontal = Dimens.SpacingMedium,
                        vertical = 0.dp
                    )
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.IconSizeSmall),
                            color = AppColors.TextSecondary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.sensor_export_csv),
                            fontSize = Dimens.FontSizeSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                ) {
                    // Upload button
                    Button(
                        onClick = onUploadClick,
                        enabled = !isUploading && !isDeleting,
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.ButtonHeightSmall),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.PrimaryColor,
                            contentColor = AppColors.White,
                            disabledContainerColor = AppColors.TextSecondary.copy(alpha = 0.3f),
                            disabledContentColor = AppColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(Dimens.ButtonCornerRadiusSmall),
                        contentPadding = PaddingValues(
                            horizontal = Dimens.SpacingMedium,
                            vertical = 0.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.sensor_upload_data),
                            fontSize = Dimens.FontSizeSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Delete button
                    Button(
                        onClick = onDeleteClick,
                        enabled = !isUploading && !isDeleting,
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.ButtonHeightSmall),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.ErrorColor,
                            contentColor = AppColors.White,
                            disabledContainerColor = AppColors.TextSecondary.copy(alpha = 0.3f),
                            disabledContentColor = AppColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(Dimens.ButtonCornerRadiusSmall),
                        contentPadding = PaddingValues(
                            horizontal = Dimens.SpacingMedium,
                            vertical = 0.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.sensor_delete_data),
                            fontSize = Dimens.FontSizeSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.TextSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary
        )
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
                            contentDescription = stringResource(R.string.phone_sensor_badge_description), // Added label
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(Styles.BADGE_SIZE)
                        )
                    }
                    if (sensor.isWatchSensor) {
                        Spacer(modifier = Modifier.width(Styles.BADGE_SPACING))
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = stringResource(R.string.watch_sensor_badge_description), // Added label
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
                sensor.uploadedPercent?.let { percent ->
                    Text(
                        text = stringResource(
                            R.string.data_screen_uploaded_count,
                            formatRecordCount(sensor.uploadedRecordCount),
                            formatRecordCount(sensor.recordCount),
                            percent
                        ),
                        fontSize = Styles.LAST_RECORDED_FONT_SIZE,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            }

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
        diff < TimeUnit.MINUTES.toMillis(1) -> stringResource(R.string.last_recorded_just_now)
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            stringResource(R.string.last_recorded_min_ago, minutes)
        }

        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            pluralStringResource(R.plurals.last_recorded_hour_ago, hours.toInt(), hours)
        }

        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            pluralStringResource(R.plurals.last_recorded_day_ago, days.toInt(), days)
        }

        else -> {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", LocalLocale.current.platformLocale)
            dateFormat.format(Date(timestamp))
        }
    }
}

/**
 * Format record count with K/M suffix for large numbers.
 */
private fun formatRecordCount(count: Int): String = formatRecordCount(count.toLong())

private fun formatRecordCount(count: Long): String {
    return count.toString()
}

