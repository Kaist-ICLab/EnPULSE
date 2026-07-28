package kaist.iclab.mobiletracker.data.campaign

import kaist.iclab.mobiletracker.webapp.WebAppConfig
import kotlinx.serialization.Serializable

/**
 * Wrapper for a list of WebAppConfig items for persistent storage.
 */
@Serializable
data class WebAppConfigList(
    val configs: List<WebAppConfig> = emptyList()
)
