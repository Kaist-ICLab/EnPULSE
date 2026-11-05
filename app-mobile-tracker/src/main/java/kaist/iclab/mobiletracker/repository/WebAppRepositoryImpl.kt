package kaist.iclab.mobiletracker.repository

import androidx.core.net.toUri
import kaist.iclab.mobiletracker.data.campaign.WebAppConfigList
import kaist.iclab.mobiletracker.services.WebAppService
import kaist.iclab.mobiletracker.storage.CouchbaseWebAppConfigStorage
import kaist.iclab.mobiletracker.webapp.WebAppConfig
import kotlinx.coroutines.flow.StateFlow

class WebAppRepositoryImpl(
    private val webAppService: WebAppService,
    private val persistentStorage: CouchbaseWebAppConfigStorage
) : WebAppRepository {
    companion object {
        private const val TAG = "WebAppRepo"
    }

    override val webAppsFlow: StateFlow<WebAppConfigList>
        get() = persistentStorage.stateFlow

    override suspend fun fetchAndPersistWebApps(campaignId: Int): Result<Int> {
        return ErrorClassifier.runClassified(TAG, "fetch and persist webapps") {
            when (val result = webAppService.getCampaignWebApps(campaignId)) {
                is Result.Success -> {
                    val webAppsData = result.data
                    if (webAppsData.isNotEmpty()) {
                        val configs = webAppsData.map {
                            WebAppConfig(
                                id = it.id.toString(),
                                name = it.name,
                                url = it.url,
                                allowedOrigin = extractOrigin(it.url),
                                iconPath = it.icon_path
                            )
                        }
                        persistentStorage.set(WebAppConfigList(configs))
                    } else {
                        clearWebApps()
                    }
                    webAppsData.size
                }

                is Result.Error -> throw result.exception
            }
        }
    }

    override fun clearWebApps() {
        persistentStorage.set(WebAppConfigList())
    }

    /**
     * Extracts the scheme and host from a URL (e.g. "https://example.com/path" -> "https://example.com")
     * WebViewCompat.addWebMessageListener requires the allowedOrigin to be precisely formatted.
     */
    private fun extractOrigin(url: String): String {
        return try {
            val uri = url.toUri()
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: ""
            val port = uri.port

            if (port != -1) {
                "$scheme://$host:$port"
            } else {
                "$scheme://$host"
            }
        } catch (_: Exception) {
            "*" // Fallback to allow all if parsing fails, though it's not recommended.
        }
    }
}
