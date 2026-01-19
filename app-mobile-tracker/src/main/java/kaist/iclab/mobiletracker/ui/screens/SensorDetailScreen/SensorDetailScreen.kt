package kaist.iclab.mobiletracker.ui.screens.SensorDetailScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.DateFilter
import kaist.iclab.mobiletracker.repository.PageSize
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.SortOrder
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.ui.theme.Dimens
import kaist.iclab.mobiletracker.ui.utils.getSensorDisplayName
import kaist.iclab.mobiletracker.viewmodels.data.SensorDetailUiState
import kaist.iclab.mobiletracker.viewmodels.data.SensorDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sensor Detail screen - displays raw data for a specific sensor.
 */
@Composable
fun SensorDetailScreen(
    viewModel: SensorDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var showUploadDialog by remember { mutableStateOf(false) }
    var showDeleteResultDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Load more when reaching end of list

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // Header
        SensorDetailHeader(
            title = uiState.sensorInfo?.let { getSensorDisplayName(it.sensorId) } ?: "",
            isWatchSensor = uiState.sensorInfo?.isWatchSensor == true,
            isPhoneSensor = uiState.sensorInfo?.isPhoneSensor == true,
            onNavigateBack = onNavigateBack
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.PrimaryColor)
                }
            }

            uiState.error != null -> {
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
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = Styles.SCREEN_HORIZONTAL_PADDING),
                    verticalArrangement = Arrangement.spacedBy(Styles.ITEM_SPACING)
                ) {
                    // Summary Card
                    item {
                        Spacer(modifier = Modifier.height(Styles.SECTION_SPACING))
                        SummaryCard(
                            uiState = uiState,
                            onUploadClick = { showUploadDialog = true },
                            onDeleteAllClick = { showDeleteAllDialog = true },
                            onExportClick = { viewModel.exportToCsv() }
                        )
                    }

                    // Filter and Sort Row
                    item {
                        FilterSortRow(
                            dateFilter = uiState.dateFilter,
                            sortOrder = uiState.sortOrder,
                            pageSize = uiState.pageSize,
                            customStartDate = uiState.customStartDate,
                            customEndDate = uiState.customEndDate,
                            onDateFilterChange = { viewModel.setDateFilter(it) },
                            onCustomDateRangeSelected = { start, end ->
                                viewModel.setCustomDateRange(
                                    start,
                                    end
                                )
                            },
                            onPageSizeChange = { viewModel.setPageSize(it) },
                            onSortOrderToggle = { viewModel.toggleSortOrder() }
                        )
                    }

                    // Section header
                    item {
                        Text(
                            text = stringResource(R.string.sensor_detail_raw_data),
                            fontSize = Styles.SECTION_TITLE_FONT_SIZE,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.padding(vertical = Styles.SECTION_TITLE_VERTICAL_PADDING)
                        )
                    }

                    // Records
                    if (uiState.records.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Styles.EMPTY_STATE_VERTICAL_PADDING),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.sensor_detail_no_records),
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    } else {
                        items(uiState.records, key = { it.id }) { record ->
                            RecordCard(
                                record = record,
                                onDelete = { viewModel.deleteRecord(record.id) }
                            )
                        }
                    }


                    // Bottom spacer
                    item {
                        Spacer(modifier = Modifier.height(Styles.BOTTOM_SPACER_HEIGHT))
                    }
                }

                // Pagination Controls
                if (uiState.totalPages > 1) {
                    PaginationControls(
                        currentPage = uiState.currentPage,
                        totalPages = uiState.totalPages,
                        onPageChange = { viewModel.loadPage(it) }
                    )
                }
            }
        }
    }

    // Upload confirmation dialog
    if (showUploadDialog) {
        PopupDialog(
            title = stringResource(R.string.sensor_upload_data_confirm),
            content = {
                Text(
                    text = stringResource(R.string.sensor_upload_data_message),
                    fontSize = Dimens.FontSizeBody,
                    color = AppColors.TextPrimary
                )
            },
            primaryButton = DialogButtonConfig(
                text = stringResource(R.string.sensor_upload_data),
                onClick = {
                    viewModel.uploadSensorData()
                    showUploadDialog = false
                },
                enabled = !uiState.isUploading
            ),
            secondaryButton = DialogButtonConfig(
                text = stringResource(R.string.sync_clear_data_cancel),
                onClick = { showUploadDialog = false },
                isPrimary = false
            ),
            onDismiss = { showUploadDialog = false }
        )
    }

    // Delete all confirmation dialog
    if (showDeleteAllDialog) {
        PopupDialog(
            title = stringResource(R.string.sensor_delete_data_confirm),
            content = {
                Text(
                    text = stringResource(R.string.sensor_delete_data_message),
                    fontSize = Dimens.FontSizeBody,
                    color = AppColors.TextPrimary
                )
            },
            primaryButton = DialogButtonConfig(
                text = stringResource(R.string.sensor_delete_data),
                onClick = {
                    viewModel.deleteAllSensorData()
                    showDeleteAllDialog = false
                },
                enabled = !uiState.isDeleting
            ),
            secondaryButton = DialogButtonConfig(
                text = stringResource(R.string.sync_clear_data_cancel),
                onClick = { showDeleteAllDialog = false },
                isPrimary = false
            ),
            onDismiss = { showDeleteAllDialog = false }
        )
    }
}

@Composable
private fun SensorDetailHeader(
    title: String,
    isWatchSensor: Boolean,
    isPhoneSensor: Boolean = false,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Styles.HEADER_HORIZONTAL_PADDING,
                vertical = Styles.HEADER_VERTICAL_PADDING
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.sensor_detail_back),
                tint = AppColors.TextPrimary
            )
        }
        Text(
            text = title,
            fontSize = Styles.HEADER_TITLE_FONT_SIZE,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        if (isPhoneSensor) {
            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = "Phone sensor",
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(Styles.WATCH_BADGE_SIZE)
            )
        }
        if (isWatchSensor) {
            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
            Icon(
                imageVector = Icons.Default.Watch,
                contentDescription = stringResource(R.string.sensor_watch_badge_desc),
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(Styles.WATCH_BADGE_SIZE)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(
    uiState: SensorDetailUiState,
    onUploadClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        shape = RoundedCornerShape(Styles.CARD_CORNER_RADIUS)
    ) {
        Column(modifier = Modifier.padding(Styles.CARD_PADDING)) {
            Text(
                text = stringResource(R.string.sensor_detail_summary),
                fontSize = Styles.CARD_TITLE_FONT_SIZE,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(Styles.SUMMARY_CONTENT_SPACING))

            SummaryRow(
                label = stringResource(R.string.sensor_detail_total_records),
                value = formatNumber(uiState.sensorInfo?.totalRecords ?: 0)
            )
            SummaryRow(
                label = stringResource(R.string.sensor_detail_today),
                value = formatNumber(uiState.sensorInfo?.todayRecords ?: 0)
            )
            SummaryRow(
                label = stringResource(R.string.sensor_detail_last_recorded),
                value = formatDateTime(uiState.sensorInfo?.lastRecordedTime)
            )
            SummaryRow(
                label = stringResource(R.string.sensor_last_sync_server),
                value = formatDateTime(uiState.sensorInfo?.lastSyncTimestamp)
            )

            if ((uiState.sensorInfo?.totalRecords ?: 0) > 0) {
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                // Export button (Full width)
                Button(
                    onClick = onExportClick,
                    enabled = !uiState.isExporting && !uiState.isUploading && !uiState.isDeleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Styles.SMALL_BUTTON_HEIGHT),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.SecondaryColor,
                        contentColor = AppColors.White,
                        disabledContainerColor = AppColors.TextSecondary.copy(alpha = 0.3f),
                        disabledContentColor = AppColors.TextSecondary
                    ),
                    shape = RoundedCornerShape(Styles.SMALL_BUTTON_CORNER_RADIUS),
                    contentPadding = PaddingValues(
                        horizontal = Styles.SMALL_BUTTON_PADDING_HORIZONTAL,
                        vertical = Styles.SMALL_BUTTON_PADDING_VERTICAL
                    )
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.IconSizeSmall),
                            color = AppColors.TextSecondary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.sensor_export_csv),
                            fontSize = Styles.SMALL_BUTTON_FONT_SIZE
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Upload button
                    Button(
                        onClick = onUploadClick,
                        enabled = !uiState.isUploading && !uiState.isDeleting,
                        modifier = Modifier
                            .weight(1f)
                            .height(Styles.SMALL_BUTTON_HEIGHT),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.PrimaryColor,
                            contentColor = AppColors.White,
                            disabledContainerColor = AppColors.TextSecondary.copy(alpha = 0.3f),
                            disabledContentColor = AppColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(Styles.SMALL_BUTTON_CORNER_RADIUS),
                        contentPadding = PaddingValues(
                            horizontal = Styles.SMALL_BUTTON_PADDING_HORIZONTAL,
                            vertical = Styles.SMALL_BUTTON_PADDING_VERTICAL
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.sensor_upload_data),
                            fontSize = Styles.SMALL_BUTTON_FONT_SIZE,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Delete button
                    Button(
                        onClick = onDeleteAllClick,
                        enabled = !uiState.isDeleting && !uiState.isUploading,
                        modifier = Modifier
                            .weight(1f)
                            .height(Styles.SMALL_BUTTON_HEIGHT),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.ErrorColor,
                            contentColor = AppColors.White,
                            disabledContainerColor = AppColors.TextSecondary.copy(alpha = 0.3f),
                            disabledContentColor = AppColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(Styles.SMALL_BUTTON_CORNER_RADIUS),
                        contentPadding = PaddingValues(
                            horizontal = Styles.SMALL_BUTTON_PADDING_HORIZONTAL,
                            vertical = Styles.SMALL_BUTTON_PADDING_VERTICAL
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.sensor_delete_data),
                            fontSize = Styles.SMALL_BUTTON_FONT_SIZE,
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
            .padding(vertical = Styles.SUMMARY_ROW_VERTICAL_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = Styles.SUMMARY_LABEL_FONT_SIZE,
            color = AppColors.TextSecondary
        )
        Text(
            text = value,
            fontSize = Styles.SUMMARY_VALUE_FONT_SIZE,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary
        )
    }
}

@Composable
private fun FilterSortRow(
    dateFilter: DateFilter,
    sortOrder: SortOrder,
    pageSize: PageSize,
    customStartDate: Long?,
    customEndDate: Long?,
    onDateFilterChange: (DateFilter) -> Unit,
    onCustomDateRangeSelected: (Long, Long) -> Unit,
    onPageSizeChange: (PageSize) -> Unit,
    onSortOrderToggle: () -> Unit
) {
    var showDateFilterMenu by remember { mutableStateOf(false) }
    var showPageSizeMenu by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = Styles.FILTER_ROW_VERTICAL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(Styles.FILTER_BUTTON_SPACING)
    ) {
        // Date Filter Button
        Box {
            Card(
                onClick = { showDateFilterMenu = true },
                colors = CardDefaults.cardColors(containerColor = AppColors.White),
                shape = RoundedCornerShape(Styles.FILTER_BUTTON_CORNER_RADIUS)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = Styles.FILTER_BUTTON_HORIZONTAL_PADDING,
                        vertical = Styles.FILTER_BUTTON_VERTICAL_PADDING
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(Styles.FILTER_ICON_SIZE),
                        tint = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(Styles.FILTER_ICON_TEXT_SPACING))
                    Text(
                        text = if (dateFilter == DateFilter.CUSTOM && customStartDate != null && customEndDate != null) {
                            formatDateRange(customStartDate, customEndDate)
                        } else {
                            getDateFilterLabel(dateFilter)
                        },
                        fontSize = Styles.FILTER_BUTTON_FONT_SIZE,
                        color = AppColors.TextPrimary
                    )
                }
            }

            DropdownMenu(
                expanded = showDateFilterMenu,
                onDismissRequest = { showDateFilterMenu = false },
                containerColor = AppColors.White
            ) {
                DateFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = getDateFilterLabel(filter),
                                color = AppColors.TextPrimary
                            )
                        },
                        onClick = {
                            showDateFilterMenu = false
                            if (filter == DateFilter.CUSTOM) {
                                showDateRangePicker = true
                            } else {
                                onDateFilterChange(filter)
                            }
                        }
                    )
                }
            }
        }

        // Page Size Button
        Box {
            Card(
                onClick = { showPageSizeMenu = true },
                colors = CardDefaults.cardColors(containerColor = AppColors.White),
                shape = RoundedCornerShape(Styles.FILTER_BUTTON_CORNER_RADIUS)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = Styles.FILTER_BUTTON_HORIZONTAL_PADDING,
                        vertical = Styles.FILTER_BUTTON_VERTICAL_PADDING
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sensor_detail_page_size, pageSize.value),
                        fontSize = Styles.FILTER_BUTTON_FONT_SIZE,
                        color = AppColors.TextPrimary
                    )
                }
            }

            DropdownMenu(
                expanded = showPageSizeMenu,
                onDismissRequest = { showPageSizeMenu = false },
                containerColor = AppColors.White
            ) {
                PageSize.entries.forEach { size ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.sensor_detail_page_size, size.value),
                                color = AppColors.TextPrimary
                            )
                        },
                        onClick = {
                            onPageSizeChange(size)
                            showPageSizeMenu = false
                        }
                    )
                }
            }
        }

        // Sort Button
        Card(
            onClick = onSortOrderToggle,
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            shape = RoundedCornerShape(Styles.FILTER_BUTTON_CORNER_RADIUS)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = Styles.FILTER_BUTTON_HORIZONTAL_PADDING,
                    vertical = Styles.FILTER_BUTTON_VERTICAL_PADDING
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(Styles.FILTER_ICON_SIZE),
                    tint = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.width(Styles.FILTER_ICON_TEXT_SPACING))
                Text(
                    text = getSortOrderLabel(sortOrder),
                    fontSize = Styles.FILTER_BUTTON_FONT_SIZE,
                    color = AppColors.TextPrimary
                )
            }
        }
    }

    // Date Range Picker Dialog
    if (showDateRangePicker) {
        DateRangePickerDialog(
            initialStartDate = customStartDate,
            initialEndDate = customEndDate,
            onDismiss = { showDateRangePicker = false },
            onConfirm = { start, end ->
                onCustomDateRangeSelected(start, end)
                showDateRangePicker = false
            }
        )
    }
}

@Composable
private fun RecordCard(
    record: SensorRecord,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        shape = RoundedCornerShape(Styles.CARD_CORNER_RADIUS)
    ) {
        Column(modifier = Modifier.padding(Styles.RECORD_CARD_PADDING)) {
            // Timestamp header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDateTime(record.timestamp),
                    fontSize = Styles.RECORD_TIMESTAMP_FONT_SIZE,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(Styles.DELETE_BUTTON_SIZE)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.sensor_detail_delete),
                        tint = AppColors.ErrorColor,
                        modifier = Modifier.size(Styles.DELETE_ICON_SIZE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Styles.RECORD_CONTENT_SPACING))

            // Fields
            record.fields.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Styles.FIELD_ROW_VERTICAL_PADDING)
                ) {
                    Text(
                        text = "$key:",
                        fontSize = Styles.FIELD_LABEL_FONT_SIZE,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.width(Styles.FIELD_LABEL_WIDTH)
                    )
                    Text(
                        text = value,
                        fontSize = Styles.FIELD_VALUE_FONT_SIZE,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        PopupDialog(
            title = stringResource(R.string.sensor_detail_delete_title),
            content = {
                Text(
                    text = stringResource(R.string.sensor_detail_delete_message),
                    fontSize = Dimens.FontSizeBody,
                    color = AppColors.TextPrimary
                )
            },
            primaryButton = DialogButtonConfig(
                text = stringResource(R.string.sensor_detail_delete_confirm),
                onClick = {
                    onDelete()
                    showDeleteDialog = false
                }
            ),
            secondaryButton = DialogButtonConfig(
                text = stringResource(R.string.sensor_detail_delete_cancel),
                onClick = { showDeleteDialog = false },
                isPrimary = false
            ),
            onDismiss = { showDeleteDialog = false }
        )
    }
}

private fun formatNumber(count: Int): String {
    return count.toString()
}

private fun formatDateTime(timestamp: Long?): String {
    if (timestamp == null) return "-"
    val dateFormat = SimpleDateFormat("HH:mm:ss · MMM d, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@Composable
private fun getDateFilterLabel(filter: DateFilter): String {
    return when (filter) {
        DateFilter.TODAY -> stringResource(R.string.sensor_detail_filter_today)
        DateFilter.LAST_7_DAYS -> stringResource(R.string.sensor_detail_filter_7_days)
        DateFilter.LAST_30_DAYS -> stringResource(R.string.sensor_detail_filter_30_days)
        DateFilter.ALL_TIME -> stringResource(R.string.sensor_detail_filter_all)
        DateFilter.CUSTOM -> stringResource(R.string.sensor_detail_filter_custom)
    }
}

@Composable
private fun getSortOrderLabel(sortOrder: SortOrder): String {
    return when (sortOrder) {
        SortOrder.NEWEST_FIRST -> stringResource(R.string.sensor_detail_sort_newest)
        SortOrder.OLDEST_FIRST -> stringResource(R.string.sensor_detail_sort_oldest)
    }
}

@Composable
private fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.SpacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        IconButton(
            onClick = { onPageChange(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.pagination_previous),
                modifier = Modifier.size(Dimens.ButtonHeightSmall),
                tint = if (currentPage > 1) AppColors.PrimaryColor else AppColors.TextSecondary.copy(
                    alpha = 0.3f
                )
            )
        }

        // Page Indicator
        Text(
            text = stringResource(R.string.pagination_page_format, currentPage, totalPages),
            fontSize = Dimens.FontSizeBody,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary
        )

        // Next Button
        IconButton(
            onClick = { onPageChange(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.pagination_next),
                modifier = Modifier.size(Dimens.ButtonHeightSmall),
                tint = if (currentPage < totalPages) AppColors.PrimaryColor else AppColors.TextSecondary.copy(
                    alpha = 0.3f
                )
            )
        }
    }
}

private fun formatDateRange(startDate: Long, endDate: Long): String {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
}

@Composable
private fun DateRangePickerDialog(
    initialStartDate: Long?,
    initialEndDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = java.util.Calendar.getInstance()

    // Initialize with current values or today
    var startDate by remember {
        mutableStateOf(initialStartDate ?: System.currentTimeMillis())
    }
    var endDate by remember {
        mutableStateOf(initialEndDate ?: System.currentTimeMillis())
    }

    PopupDialog(
        title = stringResource(R.string.sensor_detail_date_range_title),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
            ) {
                // Start Date Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sensor_detail_start_date),
                        color = AppColors.TextSecondary,
                        fontSize = Dimens.FontSizeBody
                    )
                    Card(
                        onClick = {
                            calendar.timeInMillis = startDate
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(year, month, dayOfMonth, 0, 0, 0)
                                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                                    startDate = calendar.timeInMillis
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = CardDefaults.cardColors(containerColor = AppColors.Background),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(
                                Date(
                                    startDate
                                )
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = AppColors.TextPrimary,
                            fontSize = Dimens.FontSizeBody
                        )
                    }
                }

                // End Date Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sensor_detail_end_date),
                        color = AppColors.TextSecondary,
                        fontSize = Dimens.FontSizeBody
                    )
                    Card(
                        onClick = {
                            calendar.timeInMillis = endDate
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(year, month, dayOfMonth, 23, 59, 59)
                                    calendar.set(java.util.Calendar.MILLISECOND, 999)
                                    endDate = calendar.timeInMillis
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = CardDefaults.cardColors(containerColor = AppColors.Background),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(
                                Date(
                                    endDate
                                )
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = AppColors.TextPrimary,
                            fontSize = Dimens.FontSizeBody
                        )
                    }
                }
            }
        },
        primaryButton = DialogButtonConfig(
            text = stringResource(R.string.sensor_detail_apply),
            onClick = {
                // Ensure start <= end
                val finalStart = minOf(startDate, endDate)
                val finalEnd = maxOf(startDate, endDate)
                onConfirm(finalStart, finalEnd)
            }
        ),
        secondaryButton = DialogButtonConfig(
            text = stringResource(R.string.sensor_detail_delete_cancel),
            onClick = onDismiss,
            isPrimary = false
        ),
        onDismiss = onDismiss
    )
}
