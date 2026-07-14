package kaist.iclab.mobiletracker.repository

import android.util.Log
import kotlinx.coroutines.TimeoutCancellationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Utility for classifying raw exceptions into typed [AppError] subtypes
 * and executing operations with automatic classification and logging.
 */
object ErrorClassifier {

    /**
     * Classify a raw [Throwable] into the most appropriate [AppError] subtype.
     *
     * @param e        The original exception.
     * @param context  A human-readable label describing where the error occurred
     *                 (e.g. "upsert Battery", "fetch campaigns").
     */
    fun classify(e: Throwable, context: String = ""): AppError {
        // Already classified — pass through.
        if (e is AppError) return e

        val prefix = if (context.isNotBlank()) "$context: " else ""
        val msg = "$prefix${e.message ?: "Unknown error"}"

        return when (e) {
            // Timeout (from withTimeout in SupabaseLoadingInterceptor)
            is TimeoutCancellationException ->
                AppError.Timeout(msg, e)

            // Network errors
            is UnknownHostException,
            is SocketTimeoutException,
            is ConnectException ->
                AppError.Network(msg, e)

            // "Not found" semantics
            is NoSuchElementException ->
                AppError.NotFound(msg, e)

            // Invalid arguments / validation
            is IllegalArgumentException ->
                AppError.Validation(msg, e)

            // IllegalStateException used for "no data" in upload handlers
            is IllegalStateException ->
                AppError.Validation(msg, e)

            else -> {
                // Supabase auth errors (detected by class name)
                val className = e.javaClass.name
                when {
                    className.contains("Auth", ignoreCase = true) ->
                        AppError.Auth(msg, e)

                    className.contains("UnauthorizedRestException", ignoreCase = true) ->
                        AppError.Auth(msg, e)

                    className.contains("SQLite", ignoreCase = true) ->
                        AppError.Database(msg, e)

                    else ->
                        AppError.Unknown(msg, e)
                }
            }
        }
    }

    /**
     * Execute [block] and wrap the outcome in a [Result], automatically
     * classifying any thrown exception into an [AppError] and logging it.
     *
     * This replaces the common pattern of:
     * ```
     * runCatchingSuspend {
     *     try { ... }
     *     catch (e: Exception) { Log.e(tag, ...); throw e }
     * }
     * ```
     *
     * @param tag      Android log tag (typically the class companion `TAG`).
     * @param context  Human-readable label for log messages.
     * @param block    The suspend lambda to execute.
     */
    suspend inline fun <T> runClassified(
        tag: String,
        context: String,
        crossinline block: suspend () -> T
    ): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: TimeoutCancellationException) {
            // A real timeout (from withTimeout) — classify as Timeout, not cancellation.
            val classified = classify(e, context)
            Log.e(tag, classified.message, classified)
            Result.Error(classified)
        } catch (e: CancellationException) {
            // Cooperative coroutine cancellation (e.g. the viewModelScope being
            // cleared). Must propagate so structured concurrency works correctly,
            // and must NOT be logged as an application error.
            throw e
        } catch (e: Throwable) {
            val classified = classify(e, context)
            Log.e(tag, classified.message, classified)
            Result.Error(classified)
        }
    }
}
