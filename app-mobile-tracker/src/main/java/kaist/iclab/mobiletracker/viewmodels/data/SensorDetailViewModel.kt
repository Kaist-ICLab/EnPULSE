package kaist.iclab.mobiletracker.viewmodels.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.DateFilter
import kaist.iclab.mobiletracker.repository.PageSize
import kaist.iclab.mobiletracker.repository.SensorDetailInfo
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.SortOrder
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.utils.CsvExportHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Sensor Detail screen.
 */
data class SensorDetailUiState(
    val isLoading: Boolean = true,
    val sensorInfo: SensorDetailInfo? = null,
    val records: List<SensorRecord> = emptyList(),
    val filteredCount: Int = 0,
    val dateFilter: DateFilter = DateFilter.ALL_TIME,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val pageSize: PageSize = PageSize.SIZE_25,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val error: String? = null,
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val isExporting: Boolean = false
)

/**
 * ViewModel for the Sensor Detail screen.
 */
class SensorDetailViewModel(
    private val dataRepository: DataRepository,
    private val sensorId: String,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SensorDetailUiState())
    val uiState: StateFlow<SensorDetailUiState> = _uiState.asStateFlow()

    init {
        loadSensorDetail()
    }

    /**
     * Load sensor detail info and initial records (Page 1).
     */
    fun loadSensorDetail() {
        loadPage(1, loadInfo = true)
    }

    /**
     * Load a specific page of records.
     */
    fun loadPage(page: Int, loadInfo: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dateFilter = _uiState.value.dateFilter
                val pageSize = _uiState.value.pageSize.value
                
                // Load info and count if needed (or if not loaded yet)
                var info = _uiState.value.sensorInfo
                var filteredCount = _uiState.value.filteredCount
                
                if (loadInfo || info == null) {
                    info = dataRepository.getSensorDetailInfo(sensorId)
                    filteredCount = dataRepository.getSensorRecordCount(sensorId, dateFilter)
                }
                
                // Calculate total pages
                val totalPages = if (filteredCount > 0) {
                    (filteredCount + pageSize - 1) / pageSize
                } else {
                    1
                }
                
                // Validate page
                val safePage = page.coerceIn(1, totalPages)
                val offset = (safePage - 1) * pageSize
                
                val records = dataRepository.getSensorRecords(
                    sensorId = sensorId,
                    dateFilter = dateFilter,
                    sortOrder = _uiState.value.sortOrder,
                    limit = pageSize,
                    offset = offset
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sensorInfo = info,
                    records = records,
                    filteredCount = filteredCount,
                    currentPage = safePage,
                    totalPages = totalPages
                )
            } catch (e: Exception) {
                _uiState.value = SensorDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load sensor data"
                )
            }
        }
    }

    /**
     * Change date filter.
     */
    fun setDateFilter(filter: DateFilter) {
        if (filter == _uiState.value.dateFilter && filter != DateFilter.CUSTOM) return
        // Clear custom dates if not using custom filter
        if (filter != DateFilter.CUSTOM) {
            _uiState.value = _uiState.value.copy(
                dateFilter = filter,
                customStartDate = null,
                customEndDate = null
            )
        } else {
            _uiState.value = _uiState.value.copy(dateFilter = filter)
        }
        loadSensorDetail()
    }
    
    /**
     * Set custom date range for CUSTOM filter.
     */
    fun setCustomDateRange(startDate: Long, endDate: Long) {
        _uiState.value = _uiState.value.copy(
            dateFilter = DateFilter.CUSTOM,
            customStartDate = startDate,
            customEndDate = endDate
        )
        loadSensorDetail()
    }
    
    /**
     * Change page size.
     */
    fun setPageSize(size: PageSize) {
        if (size == _uiState.value.pageSize) return
        _uiState.value = _uiState.value.copy(pageSize = size)
        loadSensorDetail()
    }

    /**
     * Change sort order.
     */
    fun setSortOrder(order: SortOrder) {
        if (order == _uiState.value.sortOrder) return
        _uiState.value = _uiState.value.copy(sortOrder = order)
        loadSensorDetail()
    }

    /**
     * Toggle sort order.
     */
    fun toggleSortOrder() {
        val newOrder = if (_uiState.value.sortOrder == SortOrder.NEWEST_FIRST) {
            SortOrder.OLDEST_FIRST
        } else {
            SortOrder.NEWEST_FIRST
        }
        setSortOrder(newOrder)
    }

    /**
     * Delete a specific record.
     */
    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                dataRepository.deleteRecord(sensorId, recordId)
                // Remove from local list and refresh counts
                val updatedRecords = _uiState.value.records.filter { it.id != recordId }
                _uiState.value = _uiState.value.copy(
                    records = updatedRecords,
                    filteredCount = _uiState.value.filteredCount - 1
                )
                // Also update sensor info
                val updatedInfo = _uiState.value.sensorInfo?.copy(
                    totalRecords = (_uiState.value.sensorInfo?.totalRecords ?: 1) - 1
                )
                _uiState.value = _uiState.value.copy(sensorInfo = updatedInfo)
                // Show success toast
                AppToast.show(context, R.string.record_deleted_success)
            } catch (e: Exception) {
                // Show error toast
                AppToast.show(context, R.string.record_deleted_error)
            }
        }
    }

    /**
     * Upload all data for the current sensor.
     */
    fun uploadSensorData() {
        if (_uiState.value.isUploading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true)
            try {
                val result = dataRepository.uploadSensorData(sensorId)
                when {
                    result > 0 -> {
                        AppToast.show(context, R.string.toast_sensor_data_uploaded)
                        // Refresh to update sync timestamp
                        loadSensorDetail()
                    }
                    result == 0 -> {
                        AppToast.show(context, R.string.toast_no_data_to_upload)
                    }
                    else -> {
                        AppToast.show(context, R.string.toast_sensor_data_upload_error)
                    }
                }
            } catch (e: Exception) {
                Log.e("SensorDetailViewModel", "Error uploading data", e)
                AppToast.show(context, R.string.toast_sensor_data_upload_error)
            } finally {
                _uiState.value = _uiState.value.copy(isUploading = false)
            }
        }
    }

    /**
     * Delete all data for the current sensor.
     */
    fun deleteAllSensorData() {
        if (_uiState.value.isDeleting) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            try {
                dataRepository.deleteAllSensorData(sensorId)
                AppToast.show(context, R.string.toast_sensor_data_deleted)
                // Refresh list
                loadSensorDetail()
            } catch (e: Exception) {
                Log.e("SensorDetailViewModel", "Error deleting data", e)
                AppToast.show(context, R.string.toast_error_generic)
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = false)
            }
        }
    }
    
    /**
     * Export sensor data to CSV and share.
     */
    fun exportToCsv() {
        if (_uiState.value.isExporting) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val records = dataRepository.getAllSensorRecordsForExport(
                    sensorId = sensorId,
                    dateFilter = _uiState.value.dateFilter
                )
                
                if (records.isEmpty()) {
                    AppToast.show(context, R.string.toast_no_data_to_export)
                } else {
                    val sensorName = _uiState.value.sensorInfo?.displayName ?: sensorId
                    val uri = CsvExportHelper.exportToCsv(context, sensorName, records)
                    
                    if (uri != null) {
                        CsvExportHelper.shareCsv(context, uri, sensorName)
                    } else {
                        AppToast.show(context, R.string.toast_export_failed)
                    }
                }
            } catch (e: Exception) {
                Log.e("SensorDetailViewModel", "Error exporting CSV", e)
                AppToast.show(context, R.string.toast_export_failed)
            } finally {
                _uiState.value = _uiState.value.copy(isExporting = false)
            }
        }
    }
}
