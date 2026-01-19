package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.LanguageSettings

import androidx.compose.ui.unit.dp
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles as CommonStyles

/**
 * Language screen style constants
 * Uses common styles from SettingsScreen.Styles
 */
object Styles {
    // Container
    val CONTAINER_CORNER_RADIUS = CommonStyles.CARD_CORNER_RADIUS
    val CONTAINER_HORIZONTAL_PADDING = CommonStyles.CARD_CONTAINER_HORIZONTAL_PADDING
    val CONTAINER_BOTTOM_PADDING = 16.dp
    val CONTAINER_SHAPE = CommonStyles.CONTAINER_SHAPE

    // Language item
    val ITEM_HORIZONTAL_PADDING = 16.dp
    val ITEM_VERTICAL_PADDING = 16.dp
    val NUMBER_WIDTH = 24.dp

    // Icon
    val CHECKMARK_SIZE = CommonStyles.ICON_SIZE

    // Common styles (delegated to shared styles)
    val HEADER_HEIGHT = CommonStyles.HEADER_HEIGHT
    val HEADER_HORIZONTAL_PADDING = CommonStyles.HEADER_HORIZONTAL_PADDING
    val TITLE_FONT_SIZE = CommonStyles.TITLE_FONT_SIZE
    val SCREEN_DESCRIPTION_FONT_SIZE = CommonStyles.SCREEN_DESCRIPTION_FONT_SIZE
    val SCREEN_DESCRIPTION_HORIZONTAL_PADDING = CommonStyles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING
    val SCREEN_DESCRIPTION_BOTTOM_PADDING = CommonStyles.SCREEN_DESCRIPTION_BOTTOM_PADDING
    val TEXT_FONT_SIZE = CommonStyles.TEXT_FONT_SIZE
    val ICON_SPACER_WIDTH = CommonStyles.ICON_SPACER_WIDTH
    val DIVIDER_WIDTH_RATIO = CommonStyles.DIVIDER_WIDTH_RATIO
}
