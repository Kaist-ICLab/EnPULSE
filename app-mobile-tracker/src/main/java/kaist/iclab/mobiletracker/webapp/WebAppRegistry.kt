package kaist.iclab.mobiletracker.webapp
import androidx.core.net.toUri
import kaist.iclab.mobiletracker.storage.CouchbaseWebAppConfigStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

/**
 * Metadata for a registered EnPULSE webapp.
 */
@Serializable
data class WebAppConfig(
    val id: String,
    val name: String,
    val url: String,

    /** Origin (scheme + host [+ port]) this webapp is allowed to load and bridge from. */
    val allowedOrigin: String,

    /** Path of the uploaded icon within the `campaign-webapp-icons` Storage bucket, if set. */
    val iconPath: String? = null
)

/**
 * Lookup for registered webapps, keyed by the `webapp_id` used in trigger configs.
 */
interface WebAppRegistry {
    fun get(webAppId: String): WebAppConfig?
    fun list(): List<WebAppConfig>

    /**
     * Find the registered webapp whose [WebAppConfig.allowedOrigin] host matches [url]'s host,
     * if any. Used to decide whether an arbitrary, dashboard-authored url (e.g. a `notification`
     * trigger action's url, possibly relayed back from a tapped watch notification) is trusted
     * enough to open in [WebAppActivity] (bridge-enabled) rather than [SimpleWebViewActivity]
     * (bridge-less fallback for untrusted urls).
     */
    fun findByUrl(url: String): WebAppConfig?

    /** Observable list of all registered webapps. Emits whenever storage changes. */
    val webAppsFlow: StateFlow<List<WebAppConfig>>
}

/**
 * Phase 2 implementation — a Supabase-backed registry that reads from local Couchbase storage
 * synced from the `campaign_webapp` table.
 */
class PersistentWebAppRegistry(private val storage: CouchbaseWebAppConfigStorage) : WebAppRegistry {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun get(webAppId: String) = storage.get().configs.find { it.id == webAppId }
    override fun list() = storage.get().configs

    override fun findByUrl(url: String): WebAppConfig? {
        val host = try { url.toUri().host } catch (_: Exception) { null } ?: return null
        return storage.get().configs.find { config ->
            val allowedHost = try { config.allowedOrigin.toUri().host } catch (_: Exception) { null }
            allowedHost != null && allowedHost == host
        }
    }

    override val webAppsFlow: StateFlow<List<WebAppConfig>> = storage.stateFlow
        .map { it.configs }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = storage.get().configs
        )
}
