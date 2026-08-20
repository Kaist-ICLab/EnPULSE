package kaist.iclab.mobiletracker.services.upload

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kaist.iclab.mobiletracker.db.obx.JsonStringElementSerializer
import kaist.iclab.mobiletracker.db.obx.WebAppLogStore
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of the `web_app_log` table. Kept separate from
 * [kaist.iclab.mobiletracker.db.entity.phone.WebAppLogEntity] because the local entity carries
 * bookkeeping the table doesn't have (`id`, `isSynced`) and the table carries a column the entity
 * doesn't (`uuid`, stamped from the session at upload time).
 */
@Serializable
data class WebAppLogRow(
    val uuid: String,
    @Serializable(with = EpochMillisIsoSerializer::class)
    val timestamp: Long,
    @SerialName("web_app_id")
    val webAppId: String,
    @SerialName("event_type")
    val eventType: String,
    @Serializable(with = JsonStringElementSerializer::class)
    val properties: String?
)

/**
 * Drains [WebAppLogStore] into the `web_app_log` Supabase table.
 *
 * Intentionally independent of the sensor upload machinery:
 * - **not campaign-gated** — unlike [SensorUploadService.uploadSensorData], there is no
 *   `campaign_table` active-sensor check, so a webapp's logs upload as soon as it emits them;
 * - **not collection-gated** — [kaist.iclab.mobiletracker.services.AutoSyncService] flushes this
 *   on its own loop, outside the `getDataCollectionStarted()` guard, so logging keeps working
 *   while sensor collection is stopped;
 * - **insert, not upsert** — `web_app_log` has no primary key, so rows are appended and progress
 *   is tracked locally via [WebAppLogEntity.isSynced] instead of a timestamp watermark.
 */
class WebAppLogUploader(
    private val store: WebAppLogStore,
    private val supabaseHelper: SupabaseHelper,
    private val syncTimestampService: SyncTimestampService
) {
    /** Serializes flushes so an immediate post-log flush can't race the periodic one into duplicates. */
    private val flushMutex = Mutex()

    /**
     * Uploads every pending log row, oldest first. Returns the number of rows accepted by Supabase
     * (0 when there is nothing pending, or when no user UUID is available yet — in that case rows
     * stay pending and are retried on the next flush).
     */
    suspend fun flush(): Result<Int> = flushMutex.withLock {
        if (!store.hasUnsynced()) return@withLock Result.Success(0)

        supabaseHelper.supabaseClient.auth.awaitInitialization()
        val userUuid = getUserUuid()
        if (userUuid == null) {
            Log.d(TAG, "No user UUID available; keeping webapp log rows pending")
            return@withLock Result.Success(0)
        }

        ErrorClassifier.runClassified(TAG, "upload webapp logs") {
            val batchSize = Constants.Network.UPLOAD_BATCH_SIZE
            var uploaded = 0

            while (true) {
                val batch = store.unsynced(batchSize.toLong())
                if (batch.isEmpty()) break

                val rows = batch.map { entity ->
                    WebAppLogRow(
                        uuid = userUuid,
                        timestamp = entity.timestamp,
                        webAppId = entity.webAppId,
                        eventType = entity.eventType,
                        properties = entity.propertiesJson
                    )
                }
                supabaseHelper.supabaseClient.from(AppConfig.SupabaseTables.WEB_APP_LOG)
                    .insert(rows)

                store.markSynced(batch.map { it.id })
                uploaded += batch.size

                if (batch.size < batchSize) break
            }

            Log.d(TAG, "Uploaded $uploaded webapp log rows")
            uploaded
        }
    }

    private fun getUserUuid(): String? {
        val fromSession = SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)
        val uuid = fromSession ?: syncTimestampService.getCachedUserUuid()
        return uuid?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val TAG = "WebAppLogUploader"
    }
}
