package kaist.iclab.mobiletracker.ui.components.Popup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kaist.iclab.mobiletracker.ui.theme.AppColors

/**
 * Configuration for a dialog button
 */
data class DialogButtonConfig(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val isPrimary: Boolean = true,
    val width: androidx.compose.ui.unit.Dp = Styles.ButtonDefaultWidth,
    val height: androidx.compose.ui.unit.Dp = Styles.ButtonDefaultHeight
)

/**
 * Reusable popup dialog component
 *
 * @param title The title text of the dialog
 * @param content The content composable of the dialog
 * @param primaryButton Configuration for the primary button (usually on the right)
 * @param secondaryButton Configuration for the secondary button (usually on the left, optional)
 * @param onDismiss Callback when dialog is dismissed
 * @param containerColor Background color of the dialog
 * @param titleColor Color of the title text
 * @param titleFontSize Font size of the title
 * @param titleFontWeight Font weight of the title
 * @param maxWidth Maximum width of the dialog
 */
@Composable
fun PopupDialog(
    title: String,
    content: @Composable () -> Unit,
    primaryButton: DialogButtonConfig,
    secondaryButton: DialogButtonConfig? = null,
    onDismiss: () -> Unit,
    containerColor: Color = Styles.ContainerColor,
    titleColor: Color = Styles.TitleColor,
    titleFontSize: androidx.compose.ui.unit.TextUnit = Styles.TitleFontSize,
    titleFontWeight: FontWeight = Styles.TitleFontWeight,
    maxWidth: androidx.compose.ui.unit.Dp = Styles.MaxWidth
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.width(maxWidth),
                shape = RoundedCornerShape(Styles.CardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = containerColor)
            ) {
                Column(
                    modifier = Modifier.padding(Styles.CardPadding)
                ) {
                    // Title
                    Text(
                        text = title,
                        fontSize = titleFontSize,
                        fontWeight = titleFontWeight,
                        color = titleColor,
                        modifier = Modifier.padding(bottom = Styles.TitleBottomPadding)
                    )

                    // Content
                    content()

                    // Buttons
                    Spacer(modifier = Modifier.height(Styles.ContentButtonSpacing))
                    if (secondaryButton != null) {
                        // Two buttons: stretch equally across full width
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Styles.ButtonSpacing)
                        ) {
                            Button(
                                onClick = primaryButton.onClick,
                                enabled = primaryButton.enabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(primaryButton.height),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (primaryButton.isPrimary) AppColors.PrimaryColor else Color.Gray
                                ),
                                shape = Styles.ButtonCornerRadius
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = primaryButton.text,
                                        color = Color.White,
                                        fontSize = Styles.ButtonTextFontSize,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = secondaryButton.onClick,
                                enabled = secondaryButton.enabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(secondaryButton.height),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Black
                                ),
                                border = BorderStroke(
                                    Styles.ButtonBorderWidth,
                                    Styles.ButtonBorderColor
                                ),
                                shape = Styles.OutlinedButtonCornerRadius
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = secondaryButton.text,
                                        fontSize = Styles.ButtonTextFontSize,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Single button: stretch to full width
                        Button(
                            onClick = primaryButton.onClick,
                            enabled = primaryButton.enabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(primaryButton.height),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (primaryButton.isPrimary) AppColors.PrimaryColor else Color.Gray
                            ),
                            shape = Styles.ButtonCornerRadius
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = primaryButton.text,
                                    color = Color.White,
                                    fontSize = Styles.ButtonTextFontSize,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}