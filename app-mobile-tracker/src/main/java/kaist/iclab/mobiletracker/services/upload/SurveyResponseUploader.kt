package kaist.iclab.mobiletracker.services.upload

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.data.survey.SurveyQuestionResponseInsert
import kaist.iclab.mobiletracker.db.obx.SurveyResponseStore
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.utils.DateTimeFormatter
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/**
 * Drains [SurveyResponseStore] into the `survey_question_response` Supabase table.
 *
 * Mirrors [WebAppLogUploader]: phone survey responses used to go straight from
 * [kaist.iclab.mobiletracker.services.PhoneSensorDataService] into a one-shot Supabase insert with
 * no local durability, so any transient failure (offline, session not restored yet, RLS hiccup)
 * silently dropped the response even though the survey UI had already reported success. They are
 * now written to [SurveyResponseStore] first and only marked synced once Supabase accepts them —
 * same durable-then-upload pattern as webapp logs and watch MicroEMA responses.
 *
 * - **not campaign-gated** — "Survey" has no [kaist.iclab.mobiletracker.di.SensorRegistry] entry
 *   and never rides the campaign-gated [SensorUploadService] path (matches the existing comment in
 *   `PhoneSensorDataService.registerListeners`: survey stays registered regardless of the
 *   campaign's active-sensor config).
 * - **insert, not upsert** — `survey_question_response` has no local-friendly key to watermark
 *   against, so rows are appended and progress is tracked locally via
 *   [kaist.iclab.mobiletracker.db.entity.phone.SurveyResponseEntity.isSynced].
 */
class SurveyResponseUploader(
    private val store: SurveyResponseStore,
    private val supabaseHelper: SupabaseHelper,
    private val syncTimestampService: SyncTimestampService
) {
    /** Serializes flushes so an immediate post-submit flush can't race the periodic one into duplicates. */
    private val flushMutex = Mutex()

    /**
     * Uploads every pending response row, oldest first. Returns the number of rows accepted by
     * Supabase (0 when there is nothing pending, or when no user UUID is available yet — in that
     * case rows stay pending and are retried on the next flush).
     */
    suspend fun flush(): Result<Int> = flushMutex.withLock {
        if (!store.hasUnsynced()) return@withLock Result.Success(0)

        // See SensorUploadService.uploadSensorData's identical guard: don't suspend forever if
        // SessionStatus is ever stuck at Initializing (normally prevented by SupabaseHelper's
        // enableLifecycleCallbacks = false).
        val initialized =
            withTimeoutOrNull(Constants.Network.AUTH_AWAIT_INITIALIZATION_TIMEOUT_MS) {
                supabaseHelper.supabaseClient.auth.awaitInitialization()
            } != null
        if (!initialized) {
            Log.w(
                TAG,
                "Timed out waiting for Supabase auth to initialize; keeping survey response rows pending"
            )
            return@withLock Result.Success(0)
        }
        val userUuid = getUserUuid()
        if (userUuid == null) {
            Log.d(TAG, "No user UUID available; keeping survey response rows pending")
            return@withLock Result.Success(0)
        }

        ErrorClassifier.runClassified(TAG, "upload survey responses") {
            val batchSize = Constants.Network.UPLOAD_BATCH_SIZE
            var uploaded = 0

            while (true) {
                val batch = store.unsynced(batchSize.toLong())
                if (batch.isEmpty()) break

                val rows = batch.map { entity ->
                    SurveyQuestionResponseInsert(
                        questionId = entity.questionId,
                        uuid = userUuid,
                        triggerTime = DateTimeFormatter.formatToIsoOffset(entity.triggerTime),
                        actualTriggerTime = DateTimeFormatter.formatToIsoOffset(entity.actualTriggerTime),
                        surveyStartTime = DateTimeFormatter.formatToIsoOffset(entity.surveyStartTime),
                        responseSubmissionTime = DateTimeFormatter.formatToIsoOffset(entity.responseSubmissionTime),
                        response = Json.parseToJsonElement(entity.responseJson)
                    )
                }
                supabaseHelper.supabaseClient.from(Constants.DB.TABLE_RESPONSE).insert(rows)

                store.markSynced(batch.map { it.id })
                uploaded += batch.size

                if (batch.size < batchSize) break
            }

            Log.d(TAG, "Uploaded $uploaded survey response rows")
            uploaded
        }
    }

    private fun getUserUuid(): String? {
        val fromSession = SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)
        val uuid = fromSession ?: syncTimestampService.getCachedUserUuid()
        return uuid?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val TAG = "SurveyResponseUploader"
    }
}
