package fr.berliat.hskwidget.data.store

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date
import java.util.Locale

class TypeConverters {
    class DefinitionsConverter {
        companion object {
            @TypeConverter
            @JvmStatic
            fun fromStringMap(value: Map<Locale, String>?): String? {
                return Gson().toJson(value)
            }

            @TypeConverter
            @JvmStatic
            fun fromString(s: String?): Map<Locale, String>? {
                val mapType = object : TypeToken<Map<Locale, String>>() {}.type
                return Gson().fromJson(s, mapType)
            }
        }
    }

    class DateConverter {
        companion object {
            @TypeConverter
            @JvmStatic
            fun toDate(dateLong: Long?): Date? {
                return if (dateLong == null) null else Date(dateLong)
            }

            @TypeConverter
            @JvmStatic
            fun fromDate(date: Date?): Long? {
                return if (date == null) null else date.getTime()
            }
        }
    }
}