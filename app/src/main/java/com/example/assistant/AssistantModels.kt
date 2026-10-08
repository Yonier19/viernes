package com.example.assistant

import com.example.memory.ContactAliasEntity
import com.example.memory.MemoryEntity
import com.example.memory.PreferenceEntity
import com.example.memory.RoutineEntity
import java.util.UUID

enum class AssistantState {
    INACTIVO,     // IDLE
    ESCUCHANDO,   // LISTENING
    PROCESANDO,   // PROCESSING
    RESPONDIENDO, // SPEAKING / RESPONDING
    ERROR         // ERROR
}

enum class IntentType {
    OPEN_APP,
    PLAY_MUSIC,
    SEARCH,
    REMINDER,
    SEND_MESSAGE,
    MEMORY_SAVE,
    MEMORY_QUERY,
    MEMORY_UPDATE,
    MEMORY_DELETE,
    CONTACT_ALIAS_CREATE,
    CONTACT_RESOLVE,
    COMMAND_ALIAS_CREATE,
    ROUTINE_CREATE,
    ROUTINE_QUERY,
    ROUTINE_EXECUTE,
    ROUTINE_DELETE,
    LEARNING_SUGGESTION,
    HELP,
    UNKNOWN
}

enum class RiskLevel {
    LEVEL_1_LOW,       // Abrir apps, consultas, lectura
    LEVEL_2_MODERATE,  // Recordatorios, guardar preferencias y rutinas
    LEVEL_3_HIGH       // Enviar mensajes, eliminar memoria/historial
}

data class AssistantContext(
    val memories: List<MemoryEntity> = emptyList(),
    val preferences: List<PreferenceEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val contactAliases: List<ContactAliasEntity> = emptyList()
)

data class ParsedIntent(
    val intent: IntentType,
    val rawQuery: String,
    val appName: String? = null,
    val contactName: String? = null,
    val contactDisplayName: String? = null,
    val messageBody: String? = null,
    val searchQuery: String? = null,
    val reminderText: String? = null,
    val reminderTime: String? = null,
    val memoryKey: String? = null,
    val memoryValue: String? = null,
    val routineName: String? = null,
    val routineActions: List<String> = emptyList(),
    val commandPhrase: String? = null,
    val commandTarget: String? = null,
    val isExplicitCommand: Boolean = true,
    val riskLevel: RiskLevel = RiskLevel.LEVEL_1_LOW,
    val confidence: Float = 1.0f
)

data class ExecutionResult(
    val success: Boolean,
    val responseSpeech: String,
    val actionSummary: String? = null,
    val riskLevel: RiskLevel = RiskLevel.LEVEL_1_LOW,
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null,
    val confirmationTitle: String? = null,
    val disambiguationOptions: List<String> = emptyList(),
    val provider: String? = null,
    val latencyMs: Long? = null,
    val toolUsed: String? = null,
    val pendingConfirmationAction: (suspend () -> ExecutionResult)? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionSummary: String? = null,
    val isError: Boolean = false,
    val hasConfirmation: Boolean = false,
    val confirmationTitle: String? = null,
    val disambiguationOptions: List<String> = emptyList(),
    val provider: String? = null,
    val isStreaming: Boolean = false,
    val toolUsed: String? = null,
    val errorMessage: String? = null,
    val latencyMs: Long? = null
)
