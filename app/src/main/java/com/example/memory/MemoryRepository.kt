package com.example.memory

import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val db: MemoryDatabase) {

    val activeMemories: Flow<List<MemoryEntity>> = db.memoryDao().getAllActiveMemories()
    val preferences: Flow<List<PreferenceEntity>> = db.preferenceDao().getAllPreferences()
    val contactAliases: Flow<List<ContactAliasEntity>> = db.contactAliasDao().getAllAliases()
    val commandHistory: Flow<List<CommandHistoryEntity>> = db.commandHistoryDao().getRecentHistory(100)
    val routines: Flow<List<RoutineEntity>> = db.routineDao().getAllActiveRoutines()

    val learningEngine = LearningEngine(db.memoryDao(), db.commandHistoryDao(), minOccurrencesThreshold = 3)

    fun getMemoriesByType(type: MemoryType): Flow<List<MemoryEntity>> {
        return db.memoryDao().getMemoriesByType(type)
    }

    suspend fun saveMemory(
        key: String,
        value: String,
        type: MemoryType = MemoryType.PREFERENCE,
        source: String = "user_command",
        confidence: Float = 1.0f
    ): Long {
        val normalizedKey = key.trim().lowercase()
        val existing = db.memoryDao().getMemoryByKey(normalizedKey)
        val entity = if (existing != null) {
            existing.copy(
                value = value.trim(),
                type = type,
                source = source,
                confidence = confidence,
                updatedAt = System.currentTimeMillis(),
                active = true
            )
        } else {
            MemoryEntity(
                key = normalizedKey,
                value = value.trim(),
                type = type,
                source = source,
                confidence = confidence,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        val id = db.memoryDao().insertMemory(entity)

        // Also sync to preferences or aliases if applicable
        if (type == MemoryType.PREFERENCE) {
            savePreference(normalizedKey, value.trim())
        }
        return id
    }

    suspend fun updateMemory(memory: MemoryEntity) {
        db.memoryDao().updateMemory(memory.copy(updatedAt = System.currentTimeMillis()))
        if (memory.type == MemoryType.PREFERENCE) {
            savePreference(memory.key, memory.value)
        }
    }

    suspend fun getMemory(key: String): MemoryEntity? {
        return db.memoryDao().getMemoryByKey(key.trim().lowercase())
    }

    suspend fun getMemoriesByTypeSync(type: MemoryType): List<MemoryEntity> {
        return db.memoryDao().getMemoriesByTypeSync(type)
    }

    suspend fun searchMemories(keyword: String): List<MemoryEntity> {
        return db.memoryDao().searchMemories(keyword.trim().lowercase())
    }

    suspend fun deleteMemory(memory: MemoryEntity) {
        db.memoryDao().deleteMemory(memory)
        if (memory.type == MemoryType.PREFERENCE) {
            db.preferenceDao().deletePreference(memory.key)
        } else if (memory.type == MemoryType.CONTACT_ALIAS) {
            db.contactAliasDao().deleteAlias(memory.key)
        }
    }

    suspend fun deleteMemoryByKey(key: String) {
        val normalized = key.trim().lowercase()
        db.memoryDao().deleteMemoryByKey(normalized)
        db.preferenceDao().deletePreference(normalized)
        db.contactAliasDao().deleteAlias(normalized)
    }

    suspend fun deactivateMemoryByKey(key: String) {
        val normalized = key.trim().lowercase()
        db.memoryDao().deactivateMemoryByKey(normalized)
        db.preferenceDao().deletePreference(normalized)
    }

    suspend fun clearAllMemories() {
        db.memoryDao().deleteAllMemories()
        db.contactAliasDao().deleteAllAliases()
        db.routineDao().deleteAllRoutines()
    }

    suspend fun savePreference(key: String, value: String, category: String = "general") {
        db.preferenceDao().insertPreference(
            PreferenceEntity(
                key = key.trim().lowercase(),
                value = value.trim(),
                category = category,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getPreference(key: String): String? {
        return db.preferenceDao().getPreference(key.trim().lowercase())?.value
    }

    suspend fun saveContactAlias(
        alias: String,
        displayName: String,
        contactId: String? = null,
        phone: String? = null
    ) {
        val normalizedAlias = alias.trim().lowercase()
        db.contactAliasDao().insertAlias(
            ContactAliasEntity(
                alias = normalizedAlias,
                displayName = displayName.trim(),
                contactId = contactId,
                phoneNumber = phone,
                updatedAt = System.currentTimeMillis()
            )
        )
        // Also save to memories table for unified querying
        saveMemory(
            key = normalizedAlias,
            value = displayName.trim(),
            type = MemoryType.CONTACT_ALIAS
        )
    }

    suspend fun getContactAlias(alias: String): ContactAliasEntity? {
        return db.contactAliasDao().getAlias(alias.trim().lowercase())
    }

    suspend fun saveCommandAlias(phrase: String, targetApp: String) {
        val normalizedPhrase = phrase.trim().lowercase()
        saveMemory(
            key = normalizedPhrase,
            value = targetApp.trim(),
            type = MemoryType.COMMAND_ALIAS
        )
    }

    suspend fun getCommandAlias(phrase: String): String? {
        val normalizedPhrase = phrase.trim().lowercase()
        val mem = db.memoryDao().getMemoryByKey(normalizedPhrase)
        return if (mem?.type == MemoryType.COMMAND_ALIAS && mem.active) mem.value else null
    }

    suspend fun logCommand(
        rawText: String,
        intent: String,
        target: String? = null,
        executionResult: String,
        isSuccess: Boolean
    ) {
        db.commandHistoryDao().insertCommand(
            CommandHistoryEntity(
                rawText = rawText.trim(),
                intent = intent,
                target = target,
                executionResult = executionResult,
                isSuccess = isSuccess,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() {
        db.commandHistoryDao().clearHistory()
    }

    suspend fun findRoutine(name: String): RoutineEntity? {
        return db.routineDao().findRoutineByName(name.trim().lowercase())
    }

    suspend fun saveRoutine(name: String, description: String = "", actionsJson: String): Long {
        val normalized = name.trim().lowercase()
        val id = db.routineDao().insertRoutine(
            RoutineEntity(
                name = normalized,
                description = description,
                actionsJson = actionsJson,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        saveMemory(
            key = normalized,
            value = actionsJson,
            type = MemoryType.ROUTINE
        )
        return id
    }

    suspend fun deleteRoutine(name: String) {
        val normalized = name.trim().lowercase()
        db.routineDao().deleteRoutineByName(normalized)
        deleteMemoryByKey(normalized)
    }

    suspend fun detectHabitPatterns(): List<LearningSuggestion> {
        return learningEngine.detectPatterns()
    }
}

// Extension helpers for DAO cleanup
private suspend fun ContactAliasDao.clearAliases() {
    // Delete in bulk
}
private suspend fun RoutineDao.clearRoutines() {
    // Delete in bulk
}
