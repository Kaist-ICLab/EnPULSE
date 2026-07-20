package kaist.iclab.tracker.sensor.phone

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

class MediaSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
) : BaseSensor<MediaSensor.Config, MediaSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class,
    titleResId = R.string.sensor_media,
    descriptionResId = R.string.sensor_desc_media,
    icon = Icons.Default.PlayCircleOutline
) {

    class Config(
        val monitorImages: Boolean = true,
        val monitorVideos: Boolean = true,
        val monitorAudio: Boolean = true,
        val monitorDocuments: Boolean = false,
        val monitorInternalStorage: Boolean = true,
        val monitorExternalStorage: Boolean = true,
        val periodicScanIntervalMinutes: Long = 30L, // Periodic scan interval
        val historicalScanHours: Long = 12L, // How far back to scan for historical data
        val debounceDelayMillis: Long = 1000L // Delay to avoid multiple rapid events
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val operation: String, // "CREATE", "UPDATE", "DELETE"
        val mediaType: String, // "IMAGE", "VIDEO", "AUDIO", "DOCUMENT"
        val storageType: String, // "INTERNAL", "EXTERNAL"
        val uri: String,
        val fileName: String?,
        val mimeType: String?,
        val size: Long?,
        val dateAdded: Long?,
        val dateModified: Long?
    ) : SensorEntity()

    override val permissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            // Android 12 and below use READ_EXTERNAL_STORAGE
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    override val foregroundServiceTypes: Array<Int> = emptyArray()

    private val contentObserver = MediaContentObserver(Handler(Looper.getMainLooper()))
    private var isObserving = false

    // Debouncing mechanism
    private val pendingChanges = mutableMapOf<String, Runnable>()
    private val handler = Handler(Looper.getMainLooper())

    // Periodic scanning
    private var lastTimeInternalPhotoWritten = Long.MIN_VALUE
    private var lastTimeExternalPhotoWritten = Long.MIN_VALUE
    private var lastTimeInternalVideoWritten = Long.MIN_VALUE
    private var lastTimeExternalVideoWritten = Long.MIN_VALUE
    private var lastTimeInternalAudioWritten = Long.MIN_VALUE
    private var lastTimeExternalAudioWritten = Long.MIN_VALUE

    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    private val periodicScanIntent by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_MEDIA_SCAN_REQUEST,
                Intent(ACTION_MEDIA_SCAN_REQUEST),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_MEDIA_SCAN_REQUEST,
                Intent(ACTION_MEDIA_SCAN_REQUEST),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_MEDIA_SCAN_REQUEST) {
                handlePeriodicMediaScan()
            }
        }
    }

    companion object {
        private const val ACTION_MEDIA_SCAN_REQUEST =
            "kaist.iclab.tracker.MediaSensor.ACTION_MEDIA_SCAN_REQUEST"
        private const val REQUEST_CODE_MEDIA_SCAN_REQUEST = 0x12

        // Method to manually trigger periodic scan for testing
        fun triggerPeriodicScan(context: Context) {
            val intent = Intent(ACTION_MEDIA_SCAN_REQUEST)
            context.sendBroadcast(intent)
        }
    }

    init {
        // Monitor state changes
        CoroutineScope(Dispatchers.Main).launch {
            sensorStateFlow.collect { _ ->
                // State monitoring can be added here if needed
            }
        }
    }

    private inner class MediaContentObserver(handler: Handler) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            handleMediaChange(uri)
        }
    }

    private fun handleMediaChange(uri: Uri?) {
        if (uri == null) return

        val config = configStateFlow.value
        val uriString = uri.toString()

        // Cancel any pending change for this URI
        pendingChanges[uriString]?.let { runnable ->
            handler.removeCallbacks(runnable)
        }

        // Create a new debounced handler for this URI
        val debouncedHandler = Runnable {
            processMediaChange(uri)
            pendingChanges.remove(uriString)
        }

        // Store the handler and schedule it with debounce delay
        pendingChanges[uriString] = debouncedHandler
        handler.postDelayed(debouncedHandler, config.debounceDelayMillis)
    }

    private fun processMediaChange(uri: Uri) {
        val config = configStateFlow.value
        val timestamp = System.currentTimeMillis()

        try {
            // Determine the operation, media type, and storage type based on the URI
            val result = determineOperationAndType(uri)
            val operation = result.first
            val mediaType = result.second
            val storageType = result.third

            if (!shouldMonitorMediaType(mediaType, config)) {
                return
            }

            if (!shouldMonitorStorageType(storageType, config)) {
                return
            }

            // Get media file details
            val mediaInfo = getMediaInfo(uri)

            val entity = Entity(
                received = timestamp,
                timestamp = timestamp,
                operation = operation,
                mediaType = mediaType,
                storageType = storageType,
                uri = uri.toString(),
                fileName = mediaInfo.fileName,
                mimeType = mediaInfo.mimeType,
                size = mediaInfo.size,
                dateAdded = mediaInfo.dateAdded,
                dateModified = mediaInfo.dateModified
            )

            listeners.forEach { listener ->
                listener.invoke(entity)
            }
        } catch (e: Exception) {
            Log.e("MediaSensor", "Error processing media change", e)
        }
    }

    private fun determineOperationAndType(uri: Uri): Triple<String, String, String> {
        val uriString = uri.toString()

        // Determine media type and storage type based on URI path
        val (mediaType, storageType) = when {
            uriString.contains(MediaStore.Images.Media.INTERNAL_CONTENT_URI.toString()) -> Pair(
                "IMAGE",
                "INTERNAL"
            )

            uriString.contains(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()) -> Pair(
                "IMAGE",
                "EXTERNAL"
            )

            uriString.contains(MediaStore.Video.Media.INTERNAL_CONTENT_URI.toString()) -> Pair(
                "VIDEO",
                "INTERNAL"
            )

            uriString.contains(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString()) -> Pair(
                "VIDEO",
                "EXTERNAL"
            )

            uriString.contains(MediaStore.Audio.Media.INTERNAL_CONTENT_URI.toString()) -> Pair(
                "AUDIO",
                "INTERNAL"
            )

            uriString.contains(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()) -> Pair(
                "AUDIO",
                "EXTERNAL"
            )

            uriString.contains(
                MediaStore.Files.getContentUri("external").toString()
            ) -> Pair("DOCUMENT", "EXTERNAL")

            uriString.contains(
                MediaStore.Files.getContentUri("internal").toString()
            ) -> Pair("DOCUMENT", "INTERNAL")

            else -> Pair("UNKNOWN", "UNKNOWN")
        }

        // For now, we'll assume all changes are CREATE/UPDATE operations
        // DELETE operations are harder to detect with ContentObserver
        val operation = "UPDATE" // Could be enhanced to detect CREATE vs UPDATE vs DELETE

        return Triple(operation, mediaType, storageType)
    }

    private fun shouldMonitorMediaType(mediaType: String, config: Config): Boolean {
        return when (mediaType) {
            "IMAGE" -> config.monitorImages
            "VIDEO" -> config.monitorVideos
            "AUDIO" -> config.monitorAudio
            "DOCUMENT" -> config.monitorDocuments
            else -> false
        }
    }

    private fun shouldMonitorStorageType(storageType: String, config: Config): Boolean {
        return when (storageType) {
            "INTERNAL" -> config.monitorInternalStorage
            "EXTERNAL" -> config.monitorExternalStorage
            else -> false
        }
    }

    private data class MediaInfo(
        val fileName: String?,
        val mimeType: String?,
        val size: Long?,
        val dateAdded: Long?,
        val dateModified: Long?
    )

    private fun getMediaInfo(uri: Uri): MediaInfo {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    MediaInfo(
                        fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)),
                        mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)),
                        size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                        dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)) * 1000,
                        dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)) * 1000
                    )
                } else {
                    MediaInfo(null, null, null, null, null)
                }
            } ?: MediaInfo(null, null, null, null, null)
        } catch (e: Exception) {
            Log.e("MediaSensor", "Error getting media info for $uri", e)
            MediaInfo(null, null, null, null, null)
        }
    }

    private fun hasMediaPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            val hasImages =
                context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideos =
                context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val hasAudio =
                context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            hasImages || hasVideos || hasAudio
        } else {
            // Android 12 and below use READ_EXTERNAL_STORAGE
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun handlePeriodicMediaScan() {
        val config = configStateFlow.value
        val toTime = System.currentTimeMillis()

        // Calculate time ranges for historical scanning
        val fromTimeInternalPhoto = maxOf(
            toTime - TimeUnit.HOURS.toMillis(config.historicalScanHours),
            lastTimeInternalPhotoWritten
        )
        val fromTimeExternalPhoto = maxOf(
            toTime - TimeUnit.HOURS.toMillis(config.historicalScanHours),
            lastTimeExternalPhotoWritten
        )
        val fromTimeInternalVideo = maxOf(
            toTime - TimeUnit.HOURS.toMillis(config.historicalScanHours),
            lastTimeInternalVideoWritten
        )
        val fromTimeExternalVideo = maxOf(
            toTime - TimeUnit.HOURS.toMillis(config.historicalScanHours),
            lastTimeExternalVideoWritten
        )
        val fromTimeInternalAudio = maxOf(
            toTime - TimeUnit.HOURS.toMillis(config.historicalScanHours),
            lastTimeInternalAudioWritten
        )
        val fromTimeExternalAudio = maxOf(
            toTime - TimeUnit.HOURS.toMillis(config.historicalScanHours),
            lastTimeExternalAudioWritten
        )

        // Scan internal photos
        if (config.monitorImages && config.monitorInternalStorage) {
            scanMediaFiles(
                uri = MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                mediaType = "IMAGE",
                storageType = "INTERNAL",
                fromTime = fromTimeInternalPhoto,
                lastTimeRef = { lastTimeInternalPhotoWritten },
                updateLastTime = { lastTimeInternalPhotoWritten = it }
            )
        }

        // Scan external photos
        if (config.monitorImages && config.monitorExternalStorage) {
            scanMediaFiles(
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                mediaType = "IMAGE",
                storageType = "EXTERNAL",
                fromTime = fromTimeExternalPhoto,
                lastTimeRef = { lastTimeExternalPhotoWritten },
                updateLastTime = { lastTimeExternalPhotoWritten = it }
            )
        }

        // Scan internal videos
        if (config.monitorVideos && config.monitorInternalStorage) {
            scanMediaFiles(
                uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                mediaType = "VIDEO",
                storageType = "INTERNAL",
                fromTime = fromTimeInternalVideo,
                lastTimeRef = { lastTimeInternalVideoWritten },
                updateLastTime = { lastTimeInternalVideoWritten = it }
            )
        }

        // Scan external videos
        if (config.monitorVideos && config.monitorExternalStorage) {
            scanMediaFiles(
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                mediaType = "VIDEO",
                storageType = "EXTERNAL",
                fromTime = fromTimeExternalVideo,
                lastTimeRef = { lastTimeExternalVideoWritten },
                updateLastTime = { lastTimeExternalVideoWritten = it }
            )
        }

        // Scan internal audio
        if (config.monitorAudio && config.monitorInternalStorage) {
            scanMediaFiles(
                uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                mediaType = "AUDIO",
                storageType = "INTERNAL",
                fromTime = fromTimeInternalAudio,
                lastTimeRef = { lastTimeInternalAudioWritten },
                updateLastTime = { lastTimeInternalAudioWritten = it }
            )
        }

        // Scan external audio
        if (config.monitorAudio && config.monitorExternalStorage) {
            scanMediaFiles(
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                mediaType = "AUDIO",
                storageType = "EXTERNAL",
                fromTime = fromTimeExternalAudio,
                lastTimeRef = { lastTimeExternalAudioWritten },
                updateLastTime = { lastTimeExternalAudioWritten = it }
            )
        }

    }

    private fun scanMediaFiles(
        uri: Uri,
        mediaType: String,
        storageType: String,
        fromTime: Long,
        lastTimeRef: () -> Long,
        updateLastTime: (Long) -> Unit
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.MediaColumns.DATE_ADDED} > ?"
        val selectionArgs = arrayOf((fromTime / 1000).toString())

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    var maxTimestamp = lastTimeRef()

                    while (cursor.moveToNext()) {
                        val id =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val fileName =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                        val mimeType =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
                        val size =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                        val dateAdded =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)) * 1000
                        val dateModified =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)) * 1000

                        // Only process files newer than our last scan
                        if (dateAdded > lastTimeRef()) {
                            val fileUri = Uri.withAppendedPath(uri, id.toString())

                            listeners.forEach { listener ->
                                listener.invoke(
                                    Entity(
                                        received = System.currentTimeMillis(),
                                        timestamp = dateAdded,
                                        operation = "CREATE", // Historical scan assumes CREATE
                                        mediaType = mediaType,
                                        storageType = storageType,
                                        uri = fileUri.toString(),
                                        fileName = fileName,
                                        mimeType = mimeType,
                                        size = size,
                                        dateAdded = dateAdded,
                                        dateModified = dateModified
                                    )
                                )
                            }

                        }

                        maxTimestamp = maxOf(maxTimestamp, dateAdded)
                    }

                    updateLastTime(maxTimestamp)
                }
        } catch (e: Exception) {
            Log.e("MediaSensor", "Error scanning media files for $mediaType ($storageType)", e)
        }
    }

    override fun onStart() {
        if (isObserving) {
            return
        }

        if (!hasMediaPermissions()) {
            stateStorage.set(SensorState(SensorState.FLAG.UNAVAILABLE, "Missing media permissions"))
            return
        }

        val config = configStateFlow.value

        try {
            // Register ContentObserver for different media types
            if (config.monitorImages && config.monitorExternalStorage) {
                context.contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    contentObserver
                )
            }

            if (config.monitorImages && config.monitorInternalStorage) {
                context.contentResolver.registerContentObserver(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    true,
                    contentObserver
                )
            }

            if (config.monitorVideos && config.monitorExternalStorage) {
                context.contentResolver.registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    true,
                    contentObserver
                )
            }

            if (config.monitorVideos && config.monitorInternalStorage) {
                context.contentResolver.registerContentObserver(
                    MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                    true,
                    contentObserver
                )
            }

            if (config.monitorAudio && config.monitorExternalStorage) {
                context.contentResolver.registerContentObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    true,
                    contentObserver
                )
            }

            if (config.monitorAudio && config.monitorInternalStorage) {
                context.contentResolver.registerContentObserver(
                    MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                    true,
                    contentObserver
                )
            }

            if (config.monitorDocuments && config.monitorExternalStorage) {
                context.contentResolver.registerContentObserver(
                    MediaStore.Files.getContentUri("external"),
                    true,
                    contentObserver
                )
            }

            if (config.monitorDocuments && config.monitorInternalStorage) {
                context.contentResolver.registerContentObserver(
                    MediaStore.Files.getContentUri("internal"),
                    true,
                    contentObserver
                )
            }

            // Setup periodic scanning
            val filter = IntentFilter().apply {
                addAction(ACTION_MEDIA_SCAN_REQUEST)
            }

            // Handle Android 14+ (API 34+) receiver registration requirements
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }

            val startTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20)
            val interval = TimeUnit.MINUTES.toMillis(config.periodicScanIntervalMinutes)

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                startTime,
                interval,
                periodicScanIntent
            )

            isObserving = true

        } catch (e: Exception) {
            Log.e("MediaSensor", "Error starting media monitoring", e)
            stateStorage.set(
                SensorState(
                    SensorState.FLAG.UNAVAILABLE,
                    "Failed to start media monitoring: ${e.message}"
                )
            )
        }
    }

    // Method to manually trigger periodic scan for testing
    fun triggerPeriodicScanNow() {
        handlePeriodicMediaScan()
    }

    override fun onStop() {
        if (!isObserving) {
            return
        }

        try {
            context.contentResolver.unregisterContentObserver(contentObserver)

            // Stop periodic scanning
            alarmManager.cancel(periodicScanIntent)
            context.unregisterReceiver(receiver)

            // Clear all pending debounced changes
            pendingChanges.values.forEach { runnable ->
                handler.removeCallbacks(runnable)
            }
            pendingChanges.clear()

            isObserving = false

        } catch (e: Exception) {
            Log.e("MediaSensor", "Error stopping media monitoring", e)
        }
    }
}
