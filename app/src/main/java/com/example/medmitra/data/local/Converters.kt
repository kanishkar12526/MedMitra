package com.example.medmitra.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromScheduleStatus(value: ScheduleStatus): String {
        return value.name
    }

    @TypeConverter
    fun toScheduleStatus(value: String): ScheduleStatus {
        return enumValueOf<ScheduleStatus>(value)
    }
}
