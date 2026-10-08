package com.example.memory

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories WHERE active = 1 ORDER BY updatedAt DESC")
    fun getAllActiveMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE type = :type AND active = 1 ORDER BY updatedAt DESC")
    fun getMemoriesByType(type: MemoryType): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE type = :type AND active = 1")
    suspend fun getMemoriesByTypeSync(type: MemoryType): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE `key` = :key AND active = 1 LIMIT 1")
    suspend fun getMemoryByKey(key: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE `key` LIKE '%' || :keyword || '%' OR `value` LIKE '%' || :keyword || '%'")
    suspend fun searchMemories(keyword: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("UPDATE memories SET active = 0 WHERE `key` = :key")
    suspend fun deactivateMemoryByKey(key: String)

    @Query("DELETE FROM memories WHERE `key` = :key")
    suspend fun deleteMemoryByKey(key: String)

    @Query("DELETE FROM memories")
    suspend fun deleteAllMemories()
}

@Dao
interface PreferenceDao {

    @Query("SELECT * FROM preferences")
    fun getAllPreferences(): Flow<List<PreferenceEntity>>

    @Query("SELECT * FROM preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreference(key: String): PreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: PreferenceEntity)

    @Query("DELETE FROM preferences WHERE `key` = :key")
    suspend fun deletePreference(key: String)
}

@Dao
interface ContactAliasDao {

    @Query("SELECT * FROM contact_aliases")
    fun getAllAliases(): Flow<List<ContactAliasEntity>>

    @Query("SELECT * FROM contact_aliases WHERE LOWER(alias) = LOWER(:alias) LIMIT 1")
    suspend fun getAlias(alias: String): ContactAliasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: ContactAliasEntity)

    @Query("DELETE FROM contact_aliases WHERE LOWER(alias) = LOWER(:alias)")
    suspend fun deleteAlias(alias: String)

    @Query("DELETE FROM contact_aliases")
    suspend fun deleteAllAliases()
}

@Dao
interface CommandHistoryDao {

    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistorySync(limit: Int = 100): List<CommandHistoryEntity>

    @Insert
    suspend fun insertCommand(history: CommandHistoryEntity)

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getAllActiveRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findRoutineByName(name: String): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Query("DELETE FROM routines WHERE LOWER(name) = LOWER(:name)")
    suspend fun deleteRoutineByName(name: String)

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()
}
