package kaist.iclab.wearabletracker.db

import androidx.room.TypeConverter
import com.google.gson.Gson

class Converter {
    @TypeConverter
    fun listToJson(value: List<Int>): String? = Gson().toJson(value)

    @TypeConverter
    fun jsonToList(value: String?): List<Int> {
        if (value == null) return emptyList()
        val array = Gson().fromJson(value, Array<Int>::class.java)
        return array?.toList() ?: emptyList()
    }
}