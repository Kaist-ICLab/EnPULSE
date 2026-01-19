package kaist.iclab.mobiletracker.services.upload.handlers

import kaist.iclab.mobiletracker.repository.Result

/**
 * Interface for handling sensor-specific upload operations.
 * Each sensor type has its own handler implementation that encapsulates
 * DAO, mapper, and service references for uploading data to Supabase.
 */
interface SensorUploadHandler {
    /** Unique identifier for the sensor (e.g., "Location", "Battery") */
    val sensorId: String

    /**
     * Check if there is data available to upload.
     * @param lastUploadTimestamp The timestamp of the last successful upload
     * @return true if there is new data to upload
     */
    suspend fun hasDataToUpload(lastUploadTimestamp: Long): Boolean

    /**
     * Upload sensor data to Supabase.
     * @param userUuid The UUID of the current user
     * @param lastUploadTimestamp The timestamp of the last successful upload
     * @return Result containing the max timestamp of uploaded data on success
     */
    suspend fun uploadData(userUuid: String, lastUploadTimestamp: Long): Result<Long>
}
