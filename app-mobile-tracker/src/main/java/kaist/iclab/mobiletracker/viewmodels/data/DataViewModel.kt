package kaist.iclab.mobiletracker.viewmodels.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.SensorInfo
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.utils.CsvExportHelper
import kaist.iclab.mobiletracker.repository.DateFilter
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Data screen.
 */
data class DataUiState(
    val isLoading: Boolean = true,
    val sensors: List<SensorInfo> = emptyList(),
    val totalRecords: Int = 0,
    val error: String? = null,
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val isExporting: Boolean = false
)

/**
 * ViewModel for the Data screen.
 * Provides sensor list with record counts and last recorded times.
 */
class DataViewModel(
    private val dataRepository: DataRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataUiState())
    val uiState: StateFlow<DataUiState> = _uiState.asStateFlow()

    init {
        loadSensorInfo()
    }

    /**
     * Load sensor information from the repository.
     */
    fun loadSensorInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val sensors = dataRepository.getAllSensorInfo()
                val totalRecords = sensors.sumOf { it.recordCount }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sensors = sensors,
                    totalRecords = totalRecords
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sensor data"
                )
            }
        }
    }

    /**
     * Refresh the sensor list.
     */
    fun refresh() {
        loadSensorInfo()
    }

    /**
     * Upload all data for all sensors.
     */
    fun uploadAllData() {
        if (_uiState.value.isUploading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true)
            try {
                val successCount = dataRepository.uploadAllData()
                if (successCount > 0) {
                    AppToast.show(context, R.string.toast_upload_all_summary, successCount)
                } else if (successCount == 0) {
                    AppToast.show(context, R.string.toast_no_data_to_upload)
                } else {
                    AppToast.show(context, R.string.toast_sensor_data_upload_error)
                }
                loadSensorInfo()
            } catch (e: Exception) {
                AppToast.show(context, R.string.toast_sensor_data_upload_error)
            } finally {
                _uiState.value = _uiState.value.copy(isUploading = false)
            }
        }
    }

    /**
     * Delete all data for all sensors.
     */
    fun deleteAllData() {
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            try {
                dataRepository.deleteAllAllData()
                AppToast.show(context, R.string.toast_data_deleted)
                loadSensorInfo()
            } catch (e: Exception) {
                AppToast.show(context, R.string.toast_error_generic)
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = false)
            }
            }
        }

    
    /**
     * Export all sensor data to CSV files.
     */
    fun exportAllToCsv() {
        if (_uiState.value.isExporting) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                // Collect data for all active sensors
                val allSensors = dataRepository.getAllSensorInfo()
                val sensorsWithData = allSensors.filter { it.recordCount > 0 }
                
                if (sensorsWithData.isEmpty()) {
                    AppToast.show(context, R.string.toast_no_data_to_export)
                    _uiState.value = _uiState.value.copy(isExporting = false)
                    return@launch
                }
                
                val uris = mutableListOf<android.net.Uri>()
                
                for (sensor in sensorsWithData) {
                    val records = dataRepository.getAllSensorRecordsForExport(
                        sensorId = sensor.sensorId,
                        dateFilter = DateFilter.ALL_TIME
                    )
                    
                    if (records.isNotEmpty()) {
                        val uri = CsvExportHelper.exportToCsv(context, sensor.displayName, records)
                        if (uri != null) {
                            uris.add(uri)
                        }
                    }
                }
                
                if (uris.isNotEmpty()) {
                    CsvExportHelper.shareMultipleCsv(context, uris, "All Sensor Data Export")
                } else {
                    AppToast.show(context, R.string.toast_export_failed)
                }
                
            } catch (e: Exception) {
                Log.e("DataViewModel", "Error exporting all data", e)
                AppToast.show(context, R.string.toast_export_failed)
            } finally {
                _uiState.value = _uiState.value.copy(isExporting = false)
            }
        }
    }
}
