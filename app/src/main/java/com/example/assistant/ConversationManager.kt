package com.example.assistant

class ConversationManager(
    private val maxTurns: Int = 8,
    private val maxTotalCharacters: Int = 4000
) {
    private val lock = Any()
    private val history = mutableListOf<AIMessage>()

    fun addMessage(
        role: AIRole,
        content: String,
        toolCallId: String? = null,
        toolName: String? = null
    ) = synchronized(lock) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return@synchronized

        history.add(
            AIMessage(
                role = role,
                content = trimmed,
                toolCallId = toolCallId,
                toolName = toolName
            )
        )

        trimToLimits()
    }

    fun getMessages(): List<AIMessage> = synchronized(lock) {
        history.toList()
    }

    fun clear() = synchronized(lock) {
        history.clear()
    }

    fun getTurnCount(): Int = synchronized(lock) {
        // Un turno suele ser un par usuario-asistente o mensajes individuales
        history.count { it.role == AIRole.USER }
    }

    fun getTotalCharacters(): Int = synchronized(lock) {
        history.sumOf { it.content.length }
    }

    private fun trimToLimits() {
        // 1. Limitar número máximo de mensajes (cada turno representa típicamente 2 mensajes: user y assistant)
        val maxMessages = maxTurns * 2
        while (history.size > maxMessages) {
            history.removeAt(0)
        }

        // 2. Limitar longitud acumulada total en caracteres
        while (history.size > 2 && history.sumOf { it.content.length } > maxTotalCharacters) {
            history.removeAt(0)
        }
    }
}
