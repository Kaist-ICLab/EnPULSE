package kaist.iclab.tracker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TrackerUtil {

    fun Long.formatLocalDateTime(): String {
        val date = Date(this)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREAN)
        return format.format(date)
    }
}