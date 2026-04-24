package kaist.iclab.mobiletracker.repository


import android.content.Context
import android.util.Log
import kaist.iclab.tracker.ema.WatchSurveyConfig
import kotlinx.serialization.json.Json

/**
 * Repository for managing MicroEMA configurations on the phone.
 * Acts as the single source of truth for the study design.
 */
class MicroEmaRepository(
    private val context: Context
) {
    companion object {
        private const val TAG = "MicroEmaRepo"
        private const val ASSET_FILE = "micro_ema_config.json"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private var cachedConfig: WatchSurveyConfig? = null

    /**
     * Load the survey config from the local asset.
     */
    fun loadConfig(): WatchSurveyConfig? {
        cachedConfig?.let { return it }

        return try {
            val jsonString = context.assets.open(ASSET_FILE)
                .bufferedReader()
                .use { it.readText() }
            val config = json.decodeFromString<WatchSurveyConfig>(jsonString)
            cachedConfig = config
            Log.d(TAG, "Loaded MicroEMA config: ${config.title}")
            config
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load MicroEMA config from assets", e)
            null
        }
    }
}
