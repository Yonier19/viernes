package com.example.assistant

import com.example.memory.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantEngine(
    private val intentParser: IntentParser,
    private val commandRouter: CommandRouter,
    private val memoryRepository: MemoryRepository,
    private val aiProvider: AIProvider = GeminiAIProvider()
) {

    private val _currentState = MutableStateFlow(AssistantState.INACTIVO)
    val currentState: StateFlow<AssistantState> = _currentState.asStateFlow()

    private var pendingExecution: (suspend () -> ExecutionResult)? = null

    suspend fun processCommand(rawInput: String): ExecutionResult {
        if (rawInput.isBlank()) {
            return ExecutionResult(
                success = false,
                responseSpeech = "No se detectó ningún comando.",
                actionSummary = "Entrada vacía"
            )
        }

        _currentState.value = AssistantState.PROCESANDO

        return try {
            val lower = rawInput.trim().lowercase()

            // 1. Check if user is answering a pending confirmation dialog or question
            if (pendingExecution != null) {
                val affirmativeWords = listOf("sí", "si", "guardar", "confirmar", "aprender", "cambiar", "eliminar", "adelante", "ok", "claro")
                val negativeWords = listOf("no", "cancelar", "no guardar", "no aprender", "no cambiar", "detener")

                if (affirmativeWords.any { lower == it || lower.startsWith("$it ") }) {
                    val action = pendingExecution
                    pendingExecution = null
                    val result = action?.invoke() ?: ExecutionResult(
                        success = false,
                        responseSpeech = "La acción ya no se encuentra disponible."
                    )
                    memoryRepository.logCommand(
                        rawText = rawInput,
                        intent = "CONFIRMED_ACTION",
                        target = result.actionSummary,
                        executionResult = result.responseSpeech,
                        isSuccess = result.success
                    )
                    _currentState.value = if (result.success) AssistantState.RESPONDIENDO else AssistantState.ERROR
                    return result
                } else if (negativeWords.any { lower == it || lower.startsWith("$it ") }) {
                    pendingExecution = null
                    val cancelResult = ExecutionResult(
                        success = true,
                        responseSpeech = "Acción cancelada por el usuario.",
                        actionSummary = "Operación cancelada"
                    )
                    _currentState.value = AssistantState.RESPONDIENDO
                    return cancelResult
                }
            }

            // 2. Parse Intent
            val parsedIntent = intentParser.parse(rawInput)

            // 3. Construct Assistant Context
            val activeMemories = memoryRepository.searchMemories("")
            val preferences = memoryRepository.preferences
            val routines = memoryRepository.routines
            val aliases = memoryRepository.contactAliases

            val assistantContext = AssistantContext(
                memories = activeMemories
            )

            // 4. Route Execution or Delegate to Generative AI if unknown / open conversational query
            val result = if (parsedIntent.intent == IntentType.UNKNOWN) {
                val startTime = System.currentTimeMillis()
                val aiReply = aiProvider.generateResponse(rawInput, assistantContext)
                val latency = System.currentTimeMillis() - startTime
                ExecutionResult(
                    success = true,
                    responseSpeech = aiReply,
                    actionSummary = "Razonamiento con IA",
                    provider = aiProvider.name,
                    latencyMs = latency
                )
            } else {
                val localResult = commandRouter.route(parsedIntent, assistantContext)
                localResult.copy(
                    provider = localResult.provider ?: "Motor Local (Reglas)",
                    toolUsed = localResult.toolUsed ?: parsedIntent.appName
                )
            }

            // 5. Stash pending confirmation if required
            if (result.requiresConfirmation && result.pendingConfirmationAction != null) {
                pendingExecution = result.pendingConfirmationAction
            } else {
                pendingExecution = null
            }

            // 6. Log to Room Database
            memoryRepository.logCommand(
                rawText = rawInput,
                intent = parsedIntent.intent.name,
                target = parsedIntent.commandTarget ?: parsedIntent.appName ?: parsedIntent.contactName,
                executionResult = result.responseSpeech,
                isSuccess = result.success
            )

            _currentState.value = if (result.success) AssistantState.RESPONDIENDO else AssistantState.ERROR
            result
        } catch (e: Exception) {
            _currentState.value = AssistantState.ERROR
            ExecutionResult(
                success = false,
                responseSpeech = "Ocurrió un inconveniente al procesar tu solicitud. Intenta nuevamente.",
                actionSummary = "Error en procesamiento"
            )
        }
    }

    suspend fun confirmPendingAction(): ExecutionResult {
        val action = pendingExecution
        pendingExecution = null
        val result = action?.invoke() ?: ExecutionResult(
            success = false,
            responseSpeech = "No hay ninguna acción pendiente de confirmación."
        )
        _currentState.value = if (result.success) AssistantState.RESPONDIENDO else AssistantState.ERROR
        return result
    }

    fun cancelPendingAction() {
        pendingExecution = null
        _currentState.value = AssistantState.INACTIVO
    }

    fun setState(state: AssistantState) {
        _currentState.value = state
    }
}
