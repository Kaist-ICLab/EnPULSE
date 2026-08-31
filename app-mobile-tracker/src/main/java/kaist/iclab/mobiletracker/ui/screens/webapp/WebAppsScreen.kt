package kaist.iclab.mobiletracker.ui.screens.webapp

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.ui.theme.Dimens
import kaist.iclab.mobiletracker.webapp.WebAppActivity
import kaist.iclab.mobiletracker.webapp.WebAppConfig
import kaist.iclab.mobiletracker.webapp.WebAppRegistry
import org.koin.compose.koinInject

/**
 * Top-level tab listing webapps registered in [WebAppRegistry].
 *
 * Header layout matches the DataScreen / HomeScreen style:
 * a top spacer, bold title, and secondary description text.
 *
 * @param modifier Layout modifier applied to the screen root Column.
 * @param webAppRegistry Registry holding config options for registered WebApps.
 */
@Composable
fun WebAppsScreen(
    modifier: Modifier = Modifier,
    webAppRegistry: WebAppRegistry = koinInject()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val webApps by webAppRegistry.webAppsFlow.collectAsState()

    fun launchWebApp(webApp: WebAppConfig) {
        val intent = Intent(context, WebAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = "webapp://${webApp.id}".toUri()
            putExtra(WebAppActivity.EXTRA_URL, webApp.url)
            putExtra(WebAppActivity.EXTRA_WEBAPP_ID, webApp.id)
        }
        context.startActivity(intent)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // Header — same layout as DataScreen / HomeScreen
        Column(
            modifier = Modifier.padding(horizontal = Styles.SCREEN_HORIZONTAL_PADDING)
        ) {
            Spacer(modifier = Modifier.height(Styles.TOP_SPACER_HEIGHT))

            Text(
                text = stringResource(R.string.web_apps_screen_title),
                fontSize = Styles.TITLE_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.web_apps_screen_description),
                fontSize = Styles.DESCRIPTION_FONT_SIZE,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = Dimens.SpacingMicro)
            )

            Spacer(modifier = Modifier.height(Styles.CARD_CONTAINER_TOP_PADDING))
        }

        // WebApp card container
        Box(
            modifier = Modifier
                .padding(horizontal = Styles.SCREEN_HORIZONTAL_PADDING)
                .weight(1f, fill = false)
                .clip(Styles.CONTAINER_SHAPE)
                .background(AppColors.White)
        ) {
            if (webApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.web_apps_empty_config),
                        textAlign = TextAlign.Center,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(
                        items = webApps,
                        key = { _, webApp -> webApp.id }
                    ) { index, webApp ->
                        val isLast = index == webApps.size - 1

                        WebAppRow(
                            webApp = webApp,
                            onClick = { launchWebApp(webApp) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!isLast) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                HorizontalDivider(
                                    color = AppColors.BorderDark,
                                    thickness = 0.dp,
                                    modifier = Modifier.fillMaxWidth(Styles.DIVIDER_WIDTH_RATIO)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
