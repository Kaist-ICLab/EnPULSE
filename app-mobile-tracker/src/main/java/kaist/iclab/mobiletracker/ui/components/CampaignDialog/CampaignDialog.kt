package kaist.iclab.mobiletracker.ui.components.CampaignDialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.data.campaign.CampaignData
import kaist.iclab.mobiletracker.ui.components.Popup.DialogButtonConfig
import kaist.iclab.mobiletracker.ui.components.Popup.PopupDialog
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * Campaign selection dialog with radio buttons
 */
@Composable
fun CampaignDialog(
    campaigns: List<CampaignData>,
    selectedCampaignId: String?,
    isLoading: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(selectedCampaignId) }

    PopupDialog(
        title = context.getString(R.string.campaign_dialog_title),
        content = {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppColors.PrimaryColor
                        )
                    }
                }

                error != null -> {
                    Text(
                        text = error,
                        fontSize = Styles.ExperimentNameFontSize,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }

                campaigns.isEmpty() -> {
                    Text(
                        text = context.getString(R.string.campaign_no_campaign_joined),
                        fontSize = Styles.ExperimentNameFontSize,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        campaigns.forEach { campaign ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (selected == campaign.idString),
                                        onClick = { selected = campaign.idString },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selected == campaign.idString),
                                    onClick = { selected = campaign.idString },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AppColors.PrimaryColor
                                    )
                                )
                                Text(
                                    text = campaign.name,
                                    fontSize = Styles.ExperimentNameFontSize,
                                    color = Styles.ExperimentNameColor
                                )
                            }
                        }
                    }
                }
            }
        },
        primaryButton = DialogButtonConfig(
            text = context.getString(R.string.campaign_dialog_select),
            onClick = {
                selected?.let { onSelect(it) }
                onDismiss()
            },
            enabled = selected != null && !isLoading && error == null
        ),
        secondaryButton = DialogButtonConfig(
            text = context.getString(R.string.campaign_dialog_cancel),
            onClick = onDismiss,
            isPrimary = false
        ),
        onDismiss = onDismiss
    )
}

