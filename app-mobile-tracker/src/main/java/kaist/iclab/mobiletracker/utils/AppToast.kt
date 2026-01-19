package kaist.iclab.mobiletracker.utils

import android.content.Context
import android.widget.Toast
import kaist.iclab.mobiletracker.helpers.LanguageHelper

/**
 * Reusable Toast utility component for displaying toast messages.
 * Provides a simple API with configurable parameters.
 */
object AppToast {
    /**
     * Toast duration options
     */
    enum class Duration {
        SHORT,
        LONG
    }

    /**
     * Toast gravity/position options
     */
    enum class Gravity {
        TOP,
        CENTER,
        BOTTOM,
        DEFAULT
    }

    /**
     * Show a toast message with default settings (SHORT duration, BOTTOM position)
     *
     * @param context The context to show the toast
     * @param text The text message to display
     */
    fun show(context: Context, text: String) {
        show(context, text, Duration.SHORT, Gravity.DEFAULT)
    }

    /**
     * Show a toast message with custom duration
     *
     * @param context The context to show the toast
     * @param text The text message to display
     * @param duration The duration of the toast (SHORT or LONG)
     */
    fun show(context: Context, text: String, duration: Duration) {
        show(context, text, duration, Gravity.DEFAULT)
    }

    /**
     * Show a toast message with custom duration and gravity
     *
     * @param context The context to show the toast
     * @param text The text message to display
     * @param duration The duration of the toast (SHORT or LONG)
     * @param gravity The position of the toast (TOP, CENTER, BOTTOM, DEFAULT)
     */
    fun show(
        context: Context,
        text: String,
        duration: Duration,
        gravity: Gravity
    ) {
        val toastDuration = when (duration) {
            Duration.SHORT -> Toast.LENGTH_SHORT
            Duration.LONG -> Toast.LENGTH_LONG
        }

        val toast = Toast.makeText(context, text, toastDuration)

        // Set gravity if specified
        when (gravity) {
            Gravity.TOP -> toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 100)
            Gravity.CENTER -> toast.setGravity(android.view.Gravity.CENTER, 0, 0)
            Gravity.BOTTOM -> toast.setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL, 0, 100)
            Gravity.DEFAULT -> {
                // Use default gravity (bottom)
            }
        }

        toast.show()
    }

    /**
     * Show a toast message with custom gravity and default duration
     *
     * @param context The context to show the toast
     * @param text The text message to display
     * @param gravity The position of the toast (TOP, CENTER, BOTTOM, DEFAULT)
     */
    fun show(context: Context, text: String, gravity: Gravity) {
        show(context, text, Duration.SHORT, gravity)
    }

    /**
     * Show a toast message with all parameters including custom gravity offsets
     *
     * @param context The context to show the toast
     * @param text The text message to display
     * @param duration The duration of the toast (SHORT or LONG)
     * @param gravity The position of the toast (TOP, CENTER, BOTTOM, DEFAULT)
     * @param xOffset Horizontal offset in pixels (only used when gravity is not DEFAULT)
     * @param yOffset Vertical offset in pixels (only used when gravity is not DEFAULT)
     */
    fun show(
        context: Context,
        text: String,
        duration: Duration,
        gravity: Gravity,
        xOffset: Int,
        yOffset: Int
    ) {
        val toastDuration = when (duration) {
            Duration.SHORT -> Toast.LENGTH_SHORT
            Duration.LONG -> Toast.LENGTH_LONG
        }

        val toast = Toast.makeText(context, text, toastDuration)

        // Set gravity with offsets if specified
        when (gravity) {
            Gravity.TOP -> toast.setGravity(
                android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL,
                xOffset,
                yOffset
            )
            Gravity.CENTER -> toast.setGravity(android.view.Gravity.CENTER, xOffset, yOffset)
            Gravity.BOTTOM -> toast.setGravity(
                android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL,
                xOffset,
                yOffset
            )
            Gravity.DEFAULT -> {
                // Use default gravity (bottom)
            }
        }

        toast.show()
    }

    /**
     * Show a toast message from a string resource ID
     *
     * @param context The context to show the toast
     * @param textResId The string resource ID
     * @param duration The duration of the toast (SHORT or LONG)
     */
    fun show(context: Context, textResId: Int, duration: Duration = Duration.SHORT) {
        // Apply language configuration to get localized string
        val languageHelper = LanguageHelper(context)
        val localizedContext = languageHelper.applyLanguage(context)
        val localizedText = localizedContext.getString(textResId)
        show(context, localizedText, duration)
    }

    /**
     * Show a toast message from a string resource ID with gravity
     *
     * @param context The context to show the toast
     * @param textResId The string resource ID
     * @param duration The duration of the toast (SHORT or LONG)
     * @param gravity The position of the toast (TOP, CENTER, BOTTOM, DEFAULT)
     */
    fun show(
        context: Context,
        textResId: Int,
        duration: Duration,
        gravity: Gravity
    ) {
        // Apply language configuration to get localized string
        val languageHelper = LanguageHelper(context)
        val localizedContext = languageHelper.applyLanguage(context)
        val localizedText = localizedContext.getString(textResId)
        show(context, localizedText, duration, gravity)
    }

    /**
     * Show a toast message from a string resource ID with format arguments
     *
     * @param context The context to show the toast
     * @param textResId The string resource ID
     * @param formatArgs The format arguments for the string
     */
    fun show(context: Context, textResId: Int, vararg formatArgs: Any) {
        // Apply language configuration to get localized string
        val languageHelper = LanguageHelper(context)
        val localizedContext = languageHelper.applyLanguage(context)
        val localizedText = localizedContext.getString(textResId, *formatArgs)
        show(context, localizedText, Duration.SHORT)
    }
}

