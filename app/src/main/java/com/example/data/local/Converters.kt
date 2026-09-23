package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.MemoryType

class Converters {
    @TypeConverter
    fun fromMemoryType(type: MemoryType): String = type.name

    @TypeConverter
    fun toMemoryType(value: String): MemoryType = MemoryType.fromString(value)
}
