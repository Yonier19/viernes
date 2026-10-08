package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: MemoryType = MemoryType.PREFERENCE,
    val key: String, // e.g. "preferred_browser", "favorite_color", "mamá"
    val value: String, // e.g. "Chrome", "azul", "María"
    val source: String = "user_command", // e.g. "user_command", "learned_pattern"
    val confidence: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val active: Boolean = true
)
