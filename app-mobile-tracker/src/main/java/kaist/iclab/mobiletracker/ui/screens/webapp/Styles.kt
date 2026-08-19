package kaist.iclab.mobiletracker.ui.screens.webapp

import androidx.compose.foundation.shape.RoundedCornerShape
import kaist.iclab.mobiletracker.ui.theme.Dimens

/**
 * WebApps screen style constants
 */
object Styles {
    // Layout
    val SCREEN_HORIZONTAL_PADDING = Dimens.ScreenHorizontalPadding
    val TOP_SPACER_HEIGHT = Dimens.SpacingMedium
    val CARD_CONTAINER_TOP_PADDING = Dimens.SpacingLarge

    // Header
    val TITLE_FONT_SIZE = Dimens.FontSizeLargeHeader
    val DESCRIPTION_FONT_SIZE = Dimens.FontSizeBody

    // Card container
    val CONTAINER_SHAPE = RoundedCornerShape(Dimens.CornerRadiusMedium)
    val CARD_HORIZONTAL_PADDING = Dimens.SpacingLarge
    val CARD_VERTICAL_PADDING = Dimens.SpacingMedium

    // Row
    val ICON_SIZE = Dimens.IconSizeStandard
    val ICON_SPACER_WIDTH = Dimens.SpacingMedium
    val TEXT_FONT_SIZE = Dimens.FontSizeSubtitle
    val TEXT_LINE_HEIGHT = Dimens.FontSizeSubtitle
    val TEXT_TOP_PADDING = Dimens.SpacingMicro
    val CARD_DESCRIPTION_FONT_SIZE = Dimens.FontSizeSmall
    val CARD_DESCRIPTION_LINE_HEIGHT = Dimens.FontSizeSmall
    val CARD_DESCRIPTION_TOP_PADDING = Dimens.SpacingMicro
    val CARD_DESCRIPTION_BOTTOM_PADDING = Dimens.SpacingMicro
    val DIVIDER_WIDTH_RATIO = 0.92f
}
