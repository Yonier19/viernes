package com.example.assistant

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Proveedor dinámico que orquesta el enrutamiento entre IA externa (Gemini / Genérico)
 * y el motor de reglas locales, respetando las políticas de privacidad y modos seleccionados
 * por el usuario en Ajustes.
 */
class DynamicRoutingAIProvider(
    private val settingsManager: AISettingsManager,
    val conversationManager: ConversationManager = ConversationManager(),
    private val contextBuilder: ContextBuilder = ContextBuilder()
) : AIProvider {

    companion object {
        private const val TAG = "DynamicRoutingAI"
    }

    val localProvider = LocalRuleBasedAIProvider()

    val geminiProvider = GeminiAIProvider(
        apiKeyProvider = { settingsManager.getApiKey() },
        modelProvider = { settingsManager.settingsState.value.selectedModel }
    )

    val genericProvider = GenericChatCompletionsProvider(
        baseUrlProvider = { settingsManager.settingsState.value.genericBaseUrl },
        apiKeyProvider = { settingsManager.getApiKey() },
        modelProvider = { settingsManager.settingsState.value.selectedModel }
    )

    override val id: String
        get() = when (settingsManager.settingsState.value.activeMode) {
            AIActiveMode.SOLO_LOCAL -> localProvider.id
            AIActiveMode.SOLO_EXTERNO -> getActiveExternalProvider().id
            AIActiveMode.AUTO -> "auto_router"
        }

    override val name: String
        get() = when (settingsManager.settingsState.value.activeMode) {
            AIActiveMode.SOLO_LOCAL -> "Reglas Locales"
            AIActiveMode.SOLO_EXTERNO -> when (settingsManager.settingsState.value.externalService) {
                AIExternalService.GEMINI -> "Gemini (${settingsManager.settingsState.value.selectedModel})"
                AIExternalService.GENERIC_CHAT_COMPLETIONS -> "Chat-Completions (${settingsManager.settingsState.value.selectedModel})"
            }
            AIActiveMode.AUTO -> if (settingsManager.settingsState.value.hasSavedApiKey) {
                when (settingsManager.settingsState.value.externalService) {
                    AIExternalService.GEMINI -> "Gemini Auto (${settingsManager.settingsState.value.selectedModel})"
                    AIExternalService.GENERIC_CHAT_COMPLETIONS -> "Chat-Completions Auto"
                }
            } else {
                "Reglas Locales (Auto)"
            }
        }

    override val isAvailable: Boolean
        get() = when (settingsManager.settingsState.value.activeMode) {
            AIActiveMode.SOLO_LOCAL -> true
            AIActiveMode.SOLO_EXTERNO -> getActiveExternalProvider().isAvailable
            AIActiveMode.AUTO -> true
        }

    override val requiresInternet: Boolean
        get() = when (settingsManager.settingsState.value.activeMode) {
            AIActiveMode.SOLO_LOCAL -> false
            AIActiveMode.SOLO_EXTERNO -> true
            AIActiveMode.AUTO -> false
        }

    fun getActiveExternalProvider(): AIProvider {
        return when (settingsManager.settingsState.value.externalService) {
            AIExternalService.GEMINI -> geminiProvider
            AIExternalService.GENERIC_CHAT_COMPLETIONS -> genericProvider
        }
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        val settings = settingsManager.settingsState.value

        return when (settings.activeMode) {
            AIActiveMode.SOLO_LOCAL -> {
                Log.d(TAG, "Ejecutando en modo SOLO_LOCAL.")
                localProvider.generate(request)
            }
            AIActiveMode.SOLO_EXTERNO -> {
                Log.d(TAG, "Ejecutando en modo SOLO_EXTERNO.")
                val external = getActiveExternalProvider()
                external.generate(request)
            }
            AIActiveMode.AUTO -> {
                val external = getActiveExternalProvider()
                if (!external.isAvailable) {
                    // Sin clave configurada -> fallback local explícito
                    Log.d(TAG, "AUTO: Proveedor externo no disponible (sin clave). Fallback a local.")
                    localProvider.generate(request)
                } else {
                    val response = external.generate(request)
                    if (response is AIResponse.Error && response.kind == AIErrorKind.SinInternet) {
                        Log.d(TAG, "AUTO: Sin internet. Fallback a reglas locales.")
                        val localResponse = localProvider.generate(request)
                        if (localResponse is AIResponse.Text) {
                            AIResponse.Text(
                                content = "${localResponse.content}\n\n(Respondido localmente por falta de conexión)",
                                providerId = localProvider.id
                            )
                        } else {
                            localResponse
                        }
                    } else {
                        response
                    }
                }
            }
        }
    }

    override fun stream(request: AIRequest): Flow<AIChunk> = flow {
        val settings = settingsManager.settingsState.value

        when (settings.activeMode) {
            AIActiveMode.SOLO_LOCAL -> {
                localProvider.stream(request).collect { emit(it) }
            }
            AIActiveMode.SOLO_EXTERNO -> {
                getActiveExternalProvider().stream(request).collect { emit(it) }
            }
            AIActiveMode.AUTO -> {
                val external = getActiveExternalProvider()
                if (!external.isAvailable) {
                    localProvider.stream(request).collect { emit(it) }
                } else {
                    var encounteredNetworkError = false
                    try {
                        external.stream(request).collect { chunk ->
                            if (chunk is AIChunk.Failure && chunk.kind == AIErrorKind.SinInternet) {
                                encounteredNetworkError = true
                            } else {
                                emit(chunk)
                            }
                        }
                    } catch (_: Exception) {
                        encounteredNetworkError = true
                    }

                    if (encounteredNetworkError) {
                        emit(AIChunk.Content("\n(Sin conexión. Cambiando a respuesta local...)\n"))
                        localProvider.stream(request).collect { emit(it) }
                    }
                }
            }
        }
    }

    override suspend fun generateResponse(
        message: String,
        context: AssistantContext
    ): String {
        val settings = settingsManager.settingsState.value
        val isExternal = settings.activeMode != AIActiveMode.SOLO_LOCAL

        val systemInstruction = contextBuilder.buildSystemInstruction(
            userQuery = message,
            context = context,
            isExternalProvider = isExternal,
            availableTools = emptyList(),
            allowedCategories = settings.allowedMemoryCategories
        )

        // Registrar en el historial de conversación
        conversationManager.addMessage(AIRole.USER, message)

        val messagesWithHistory = mutableListOf<AIMessage>()
        messagesWithHistory.addAll(conversationManager.getMessages().dropLast(1))
        messagesWithHistory.add(AIMessage(AIRole.USER, message))

        val request = AIRequest(
            messages = messagesWithHistory,
            systemInstruction = systemInstruction
        )

        val response = generate(request)
        return when (response) {
            is AIResponse.Text -> {
                conversationManager.addMessage(AIRole.ASSISTANT, response.content)
                response.content
            }
            is AIResponse.ToolCall -> "Comando solicitado: ${response.name}"
            is AIResponse.Error -> {
                response.userMessage
            }
        }
    }

    /**
     * Realiza una prueba de conexión mínima con el proveedor externo configurado.
     */
    suspend fun testExternalConnection(): ConnectionTestResult {
        val apiKey = settingsManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return ConnectionTestResult.ClaveInvalida
        }

        val testRequest = AIRequest(
            messages = listOf(AIMessage(AIRole.USER, "ping")),
            maxTokens = 10,
            systemInstruction = "Responde 'ok'"
        )

        return try {
            val external = getActiveExternalProvider()
            val response = external.generate(testRequest)
            when (response) {
                is AIResponse.Text -> ConnectionTestResult.Conectado
                is AIResponse.ToolCall -> ConnectionTestResult.Conectado
                is AIResponse.Error -> when (response.kind) {
                    AIErrorKind.SinClave -> ConnectionTestResult.ClaveInvalida
                    AIErrorKind.SinInternet -> ConnectionTestResult.SinInternet
                    AIErrorKind.Timeout -> ConnectionTestResult.TiempoAgotado
                    else -> ConnectionTestResult.Error(response.userMessage)
                }
            }
        } catch (_: Exception) {
            ConnectionTestResult.SinInternet
        }
    }
}
