package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String, // e.g. "pon música"
    val intent: String, // e.g. "OPEN_APP"
    val target: String? = null, // e.g. "Spotify"
    val executionResult: String,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
