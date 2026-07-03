package kaist.iclab.mobiletracker.services.supabase

import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.ErrorClassifier
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kotlinx.serialization.Serializable

/**
 * Single generic replacement for the 23 per-sensor `*SensorService` classes.
 *
 * Those wrappers only existed because Kotlin's `reified` type parameters can't be virtual — but at
 * every call site the concrete `@Serializable` Supabase row type is known, so one reified upsert
 * parameterized by table name serves every sensor.
 */
class SupabaseUploadService(supabaseHelper: SupabaseHelper) {
    @PublishedApi
    internal val supabaseClient = supabaseHelper.supabaseClient

    suspend inline fun <reified T : @Serializable Any> upsertBatch(
        tableName: String,
        sensorName: String,
        dataList: List<T>
    ): Result<Unit> {
        if (dataList.isEmpty()) return Result.Success(Unit)
        return SupabaseLoadingInterceptor.withLoading {
            ErrorClassifier.runClassified(
                sensorName,
                "upsert ${dataList.size} $sensorName entries"
            ) {
                supabaseClient.from(tableName).upsert(dataList)
                Unit
            }
        }
    }
}
