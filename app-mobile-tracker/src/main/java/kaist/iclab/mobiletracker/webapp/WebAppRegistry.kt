package kaist.iclab.mobiletracker.webapp
/**
 * Metadata for a registered EnPULSE webapp.
 */
data class WebAppConfig(
    val id: String,
    val url: String,

    /** Origin (scheme + host [+ port]) this webapp is allowed to load and bridge from. */
    val allowedOrigin: String
)

/**
 * Lookup for registered webapps, keyed by the `webapp_id` used in trigger configs.
 *
 * Phase 1 stub; Phase 2 replaces [StaticWebAppRegistry] with a Supabase-backed implementation
 * (the `campaign_webapp` table), synced the same way [kaist.iclab.mobiletracker.repository.SurveyRepositoryImpl]
 * syncs surveys.
 */
interface WebAppRegistry {
    fun get(webAppId: String): WebAppConfig?
    fun list(): List<WebAppConfig>
}

/**
 * Phase 1 implementation — a hardcoded, in-memory registry. Populate [configs] with the webapps
 * available to this build until the Phase 2 Supabase-backed registry replaces it.
 */
class StaticWebAppRegistry(private val configs: Map<String, WebAppConfig>) : WebAppRegistry {
    override fun get(webAppId: String) = configs[webAppId]
    override fun list() = configs.values.toList()
}
