package kaist.iclab.mobiletracker.services.supabase

import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

/**
 * Single generic replacement for the 23 per-sensor `*SensorService` classes.
 *
 * Those wrappers only existed because Kotlin's `reified` type parameters can't be virtual — but at
 * every call site the concrete `@Serializable` Supabase row type is known, so one reified upsert
 * parameterized by table name serves every sensor.
 *
 * Writes deliberately bypass [kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor]: they run
 * in the background from [kaist.iclab.mobiletracker.services.upload.DataUploadService], which reports
 * its own progress, so they must never raise the blocking global loading overlay. They keep the
 * same per-request timeout as every other Supabase call.
 */
class SupabaseBatchUploader(private val supabaseHelper: SupabaseHelper) {
    @PublishedApi
    internal val supabaseClient get() = supabaseHelper.supabaseClient

    suspend inline fun <reified T : @Serializable Any> upsertBatch(
        tableName: String,
        sensorName: String,
        dataList: List<T>
    ): Result<Unit> {
        if (dataList.isEmpty()) return Result.Success(Unit)
        return withTimeout(Constants.Network.SUPABASE_REQUEST_TIMEOUT_MS.milliseconds) {
            ErrorClassifier.runClassified(
                sensorName,
                "upsert ${dataList.size} $sensorName entries"
            ) {
                supabaseClient.from(tableName).upsert(dataList)
                Unit
            }
        }
    }

    /** Plain insert, for tables with no key to upsert against (e.g. `survey_question_response`). */
    suspend inline fun <reified T : @Serializable Any> insertBatch(
        tableName: String,
        sensorName: String,
        dataList: List<T>
    ): Result<Unit> {
        if (dataList.isEmpty()) return Result.Success(Unit)
        return withTimeout(Constants.Network.SUPABASE_REQUEST_TIMEOUT_MS.milliseconds) {
            ErrorClassifier.runClassified(
                sensorName,
                "insert ${dataList.size} $sensorName entries"
            ) {
                supabaseClient.from(tableName).insert(dataList)
                Unit
            }
        }
    }
}
