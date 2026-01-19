package kaist.iclab.mobiletracker.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility object for formatting timestamps
 */
object DateTimeFormatter {
    /**
     * Format Unix timestamp (milliseconds) to "YYYY-MM-DD HH:mm:ss" format
     *
     * @param timestampMillis Unix timestamp in milliseconds
     * @return Formatted timestamp string in "YYYY-MM-DD HH:mm:ss" format (UTC timezone)
     */
    fun formatTimestamp(timestampMillis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date(timestampMillis))
    }

    /**
     * Format Unix timestamp (milliseconds) to "MM-DD HH:mm:ss" format
     *
     * @param timestampMillis Unix timestamp in milliseconds
     * @return Formatted timestamp string in "MM-DD HH:mm:ss" format (local timezone)
     */
    fun formatTimestampShort(timestampMillis: Long): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
        return dateFormat.format(Date(timestampMillis))
    }

    /**
     * Get current time formatted as "MM-DD HH:mm:ss"
     *
     * @return Formatted current time string in "MM-DD HH:mm:ss" format (local timezone)
     */
    fun getCurrentTimeFormatted(): String {
        return formatTimestampShort(System.currentTimeMillis())
    }

    /**
     * Parse timestamp string from "YYYY-MM-DD HH:mm:ss" format back to Unix timestamp (milliseconds)
     *
     * @param timestampString Timestamp string in "YYYY-MM-DD HH:mm:ss" format (UTC timezone)
     * @return Unix timestamp in milliseconds, or null if parsing fails
     */
    fun parseTimestamp(timestampString: String): Long {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            dateFormat.parse(timestampString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

