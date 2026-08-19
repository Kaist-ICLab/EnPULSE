package kaist.iclab.mobiletracker.webapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.services.WebAppService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache of decoded webapp icon [Bitmap]s, keyed by `icon_path` (the object path within
 * the fixed `campaign-webapp-icons` Storage bucket).
 *
 * Icons are downloaded via [WebAppService.downloadIcon] and decoded with [BitmapFactory]. Callers
 * (the WebApps list row, "Add to Home Screen") both need the same bitmap and would otherwise
 * re-hit Supabase Storage on every recomposition/scroll — this caches per path for the process
 * lifetime and de-dupes concurrent requests for the same path.
 *
 * Failures (network error, missing object, corrupt image) are logged and surfaced as `null` so
 * callers can fall back to their existing default icon rather than crash.
 */
class WebAppIconCache(private val webAppService: WebAppService) {
    private val cache = ConcurrentHashMap<String, Bitmap>()
    private val locksByPath = ConcurrentHashMap<String, Mutex>()

    /**
     * Returns the decoded icon bitmap for [iconPath], downloading and caching it on first use.
     * Returns `null` if [iconPath] is blank or the download/decode fails.
     */
    suspend fun getBitmap(iconPath: String?): Bitmap? {
        if (iconPath.isNullOrBlank()) return null
        cache[iconPath]?.let { return it }

        // De-dupe concurrent fetches of the same path (e.g. list row + shortcut pin at once).
        val lock = locksByPath.getOrPut(iconPath) { Mutex() }
        return lock.withLock {
            cache[iconPath]?.let { return@withLock it }

            withContext(Dispatchers.IO) {
                when (val result = webAppService.downloadIcon(iconPath)) {
                    is Result.Success -> {
                        val bytes = result.data
                        val bitmap = try {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to decode icon bitmap for path=$iconPath", e)
                            null
                        }
                        bitmap?.also { cache[iconPath] = it }
                    }

                    is Result.Error -> {
                        Log.e(TAG, "Failed to download icon for path=$iconPath", result.exception)
                        null
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WebAppIconCache"
    }
}
