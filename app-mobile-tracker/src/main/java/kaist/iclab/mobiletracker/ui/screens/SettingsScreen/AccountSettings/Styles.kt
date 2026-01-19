package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.AccountSettings

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles as CommonStyles

/**
 * Account settings screen style constants
 * Uses common styles from SettingsScreen.Styles for header
 */
object Styles {
    // Content spacing
    val CONTENT_TOP_PADDING = 32.dp
    val BUTTON_TOP_PADDING = 16.dp
    val INFO_TOP_PADDING = 32.dp

    // User name
    val NAME_FONT_SIZE = 24.sp

    // Sign out button
    val BUTTON_WIDTH_RATIO = 0.3f
    val BUTTON_CORNER_RADIUS = 20.dp
    val BUTTON_TEXT_FONT_SIZE = 14.sp

    // Info card
    val INFO_CARD_WIDTH_RATIO = 0.9f
    val INFO_CARD_PADDING = 16.dp
    val INFO_ROW_VERTICAL_PADDING = 12.dp
    val INFO_LABEL_FONT_SIZE = 14.sp
    val INFO_VALUE_FONT_SIZE = 14.sp

    // Common styles (delegated to shared styles)
    val HEADER_HEIGHT = CommonStyles.HEADER_HEIGHT
    val HEADER_HORIZONTAL_PADDING = CommonStyles.HEADER_HORIZONTAL_PADDING
    val TITLE_FONT_SIZE = CommonStyles.TITLE_FONT_SIZE
}

