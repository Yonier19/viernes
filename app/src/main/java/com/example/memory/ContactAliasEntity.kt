package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_aliases")
data class ContactAliasEntity(
    @PrimaryKey
    val alias: String, // e.g. "mamá"
    val contactId: String? = null,
    val displayName: String, // e.g. "María"
    val phoneNumber: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
