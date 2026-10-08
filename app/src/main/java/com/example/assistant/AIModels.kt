package com.example.assistant

enum class AIRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

data class AIMessage(
    val role: AIRole,
    val content: String,
    val toolCallId: String? = null,
    val toolName: String? = null
)

data class AIToolDefinition(
    val name: String,
    val description: String,
    val parametersJsonSchema: String = "{}"
)

data class AIRequest(
    val messages: List<AIMessage>,
    val tools: List<AIToolDefinition> = emptyList(),
    val temperature: Double = 0.4,
    val maxTokens: Int = 300,
    val systemInstruction: String? = null
)

enum class AIErrorKind {
    SinInternet,
    SinClave,
    Timeout,
    LimiteExcedido,
    RespuestaInvalida,
    Desconocido
}

sealed interface AIResponse {
    data class Text(
        val content: String,
        val providerId: String = ""
    ) : AIResponse

    data class ToolCall(
        val name: String,
        val argsJson: String,
        val callId: String = "",
        val providerId: String = ""
    ) : AIResponse

    data class Error(
        val kind: AIErrorKind,
        val userMessage: String,
        val providerId: String = ""
    ) : AIResponse
}

sealed interface AIChunk {
    data class Content(val text: String) : AIChunk
    data class ToolRequested(val name: String, val argsJson: String) : AIChunk
    data class Failure(val kind: AIErrorKind, val userMessage: String) : AIChunk
}
