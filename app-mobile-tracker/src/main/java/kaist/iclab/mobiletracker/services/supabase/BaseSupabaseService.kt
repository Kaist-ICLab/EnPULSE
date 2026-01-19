package kaist.iclab.mobiletracker.services.supabase

import android.util.Log
import io.github.jan.supabase.postgrest.from
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.runCatchingSuspend
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kotlinx.serialization.Serializable

/**
 * Base service class for handling sensor data uploads to Supabase.
 * 
 * This class provides common functionality for uploading sensor data
 * to the remote Supabase database. All sensor services extend this class.
 * 
 * @param T The sensor data type (must be @Serializable and have uuid field)
 * @param supabaseHelper The SupabaseHelper instance (injected via DI)
 * @param tableName The Supabase table name
 * @param sensorName The sensor name for logging purposes
 */
abstract class BaseSupabaseService<T : @Serializable Any>(
    protected val supabaseHelper: SupabaseHelper,
    protected val tableName: String,
    protected val sensorName: String
) {
    protected val supabaseClient = supabaseHelper.supabaseClient

    /**
     * Prepare data for insertion by adding UUID if needed.
     * Each child class implements this to call the appropriate copy() method.
     * Note: UUID should already be set to the logged-in user's UUID by the mapper.
     */
    protected abstract fun prepareData(data: T): T
    
    /**
     * Insert a single data record to Supabase with concrete type.
     * Uses suspend function for proper coroutine scope management.
     * Returns Result type for explicit error handling.
     * Each service should implement insert methods that call this with the concrete type.
     */
    protected suspend inline fun <reified TSerializable : @Serializable Any> upsertToSupabase(
        data: TSerializable
    ): Result<Unit> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                try {
                    supabaseClient.from(tableName).upsert(data)
                    Unit // Explicitly return Unit
                } catch (e: Exception) {
                    Log.e(AppConfig.LogTags.PHONE_SUPABASE, "Error upserting $sensorName sensor data: ${e.message}", e)
                    throw e
                }
            }
        }
    }

    /**
     * Insert multiple data records to Supabase with concrete type.
     * Uses suspend function for proper coroutine scope management.
     * Returns Result type for explicit error handling.
     * Each service should implement insert batch methods that call this with the concrete type.
     */
    protected suspend inline fun <reified TSerializable : @Serializable Any> upsertBatchToSupabase(
        dataList: List<TSerializable>
    ): Result<Unit> {
        return SupabaseLoadingInterceptor.withLoading {
            runCatchingSuspend {
                if (dataList.isEmpty()) {
                    return@runCatchingSuspend
                }

                try {
                    supabaseClient.from(tableName).upsert(dataList)
                    Unit // Explicitly return Unit
                } catch (e: Exception) {
                    Log.e(AppConfig.LogTags.PHONE_SUPABASE, "Error upserting ${dataList.size} $sensorName sensor data entries: ${e.message}", e)
                    throw e
                }
            }
        }
    }
}

