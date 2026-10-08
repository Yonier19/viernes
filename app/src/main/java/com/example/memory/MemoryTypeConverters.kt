package com.example.memory

import androidx.room.TypeConverter

class MemoryTypeConverters {

    @TypeConverter
    fun fromMemoryType(type: MemoryType): String {
        return type.name
    }

    @TypeConverter
    fun toMemoryType(value: String): MemoryType {
        return try {
            MemoryType.valueOf(value)
        } catch (_: Exception) {
            MemoryType.PREFERENCE
        }
    }
}
