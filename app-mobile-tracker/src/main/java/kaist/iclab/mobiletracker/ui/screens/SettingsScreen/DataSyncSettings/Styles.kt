package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.DataSyncSettings

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.Styles as CommonStyles

/**
 * Auto Sync Settings screen style constants
 * Uses common styles from SettingsScreen.Styles for header
 */
object Styles {
    // Card
    val CARD_CONTENT_PADDING = 16.dp
    val PHONE_SENSOR_CARD_PADDING = 12.dp
    val TEXT_SPACING = 6.dp

    // Buttons
    val BUTTON_SPACING = 16.dp
    val BUTTON_ROW_SPACING = 12.dp
    val BUTTON_HEIGHT = 48.dp
    val BUTTON_CORNER_RADIUS = 8.dp
    val BUTTON_TEXT_SIZE = 14.sp
    val BUTTON_ICON_SIZE = 20.dp
    val BUTTON_ICON_SPACING = 8.dp

    // Sensor Card
    val SENSOR_CARD_SPACING = 8.dp
    val SENSOR_CARD_TITLE_FONT_SIZE = 16.sp
    val SENSOR_CARD_TIMESTAMP_FONT_SIZE = 12.sp
    val SENSOR_CARD_ICON_SIZE = 20.dp
    val SENSOR_CARD_ROW_SPACING = 8.dp
    val SECTION_TITLE_FONT_SIZE = 18.sp
    val SECTION_TITLE_SPACING = 16.dp
    val SECTION_DESCRIPTION_FONT_SIZE = 13.sp
    val SECTION_DESCRIPTION_SPACING = 8.dp
    val DELETE_BUTTON_SIZE = 18.dp

    // Small action buttons
    val SMALL_BUTTON_HEIGHT = 32.dp
    val SMALL_BUTTON_PADDING_HORIZONTAL = 12.dp
    val SMALL_BUTTON_PADDING_VERTICAL = 6.dp
    val SMALL_BUTTON_FONT_SIZE = 12.sp
    val SMALL_BUTTON_CORNER_RADIUS = 6.dp

    // Status text (between title and description) - screen-specific
    val STATUS_TEXT_FONT_SIZE = 13.sp
    val STATUS_TEXT_LINE_HEIGHT = 16.sp
    val STATUS_TOP_PADDING = 3.dp

    // Screen-specific spacing
    val SETTING_CONTAINER_BOTTOM_PADDING = 16.dp

    // Common styles (delegated to shared styles)
    val HEADER_HEIGHT = CommonStyles.HEADER_HEIGHT
    val HEADER_HORIZONTAL_PADDING = CommonStyles.HEADER_HORIZONTAL_PADDING
    val TITLE_FONT_SIZE = CommonStyles.TITLE_FONT_SIZE
    val CARD_CORNER_RADIUS = CommonStyles.CARD_CORNER_RADIUS
    val CARD_VERTICAL_PADDING = CommonStyles.CARD_VERTICAL_PADDING
    val CARD_HORIZONTAL_PADDING = CommonStyles.CARD_HORIZONTAL_PADDING
    val CARD_CONTAINER_HORIZONTAL_PADDING = CommonStyles.CARD_CONTAINER_HORIZONTAL_PADDING
    val CONTAINER_SHAPE = CommonStyles.CONTAINER_SHAPE
    val CARD_SHAPE = CommonStyles.INDIVIDUAL_CARD_SHAPE
    val SCREEN_DESCRIPTION_FONT_SIZE = CommonStyles.SCREEN_DESCRIPTION_FONT_SIZE
    val SCREEN_DESCRIPTION_HORIZONTAL_PADDING = CommonStyles.SCREEN_DESCRIPTION_HORIZONTAL_PADDING
    val SCREEN_DESCRIPTION_BOTTOM_PADDING = CommonStyles.SCREEN_DESCRIPTION_BOTTOM_PADDING
    val TEXT_FONT_SIZE = CommonStyles.TEXT_FONT_SIZE
    val TEXT_LINE_HEIGHT = CommonStyles.TEXT_LINE_HEIGHT
    val TEXT_TOP_PADDING = CommonStyles.TEXT_TOP_PADDING
    val CARD_DESCRIPTION_FONT_SIZE = CommonStyles.CARD_DESCRIPTION_FONT_SIZE
    val CARD_DESCRIPTION_LINE_HEIGHT = CommonStyles.CARD_DESCRIPTION_LINE_HEIGHT
    val CARD_DESCRIPTION_TOP_PADDING = CommonStyles.CARD_DESCRIPTION_TOP_PADDING
    val CARD_DESCRIPTION_BOTTOM_PADDING = CommonStyles.CARD_DESCRIPTION_BOTTOM_PADDING
    val SPACER_WIDTH = CommonStyles.SPACER_WIDTH
    val ICON_SPACER_WIDTH = CommonStyles.ICON_SPACER_WIDTH
    val DIVIDER_WIDTH_RATIO = CommonStyles.DIVIDER_WIDTH_RATIO
    val ICON_SIZE = CommonStyles.ICON_SIZE
}
