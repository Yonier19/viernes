package com.example.assistant

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface AIProvider {
    val id: String
    val name: String get() = id
    val isAvailable: Boolean
    val requiresInternet: Boolean

    suspend fun generate(request: AIRequest): AIResponse

    fun stream(request: AIRequest): Flow<AIChunk> = flow {
        when (val response = generate(request)) {
            is AIResponse.Text -> emit(AIChunk.Content(response.content))
            is AIResponse.ToolCall -> emit(AIChunk.ToolRequested(response.name, response.argsJson))
            is AIResponse.Error -> emit(AIChunk.Failure(response.kind, response.userMessage))
        }
    }

    // Helper de compatibilidad retroactiva estricta con Fases 1, 2 y 3.1
    suspend fun generateResponse(
        message: String,
        context: AssistantContext
    ): String {
        val request = AIRequest(
            messages = listOf(AIMessage(AIRole.USER, message))
        )
        return when (val response = generate(request)) {
            is AIResponse.Text -> response.content
            is AIResponse.ToolCall -> "Comando solicitado: ${response.name}"
            is AIResponse.Error -> response.userMessage
        }
    }
}

class LocalRuleBasedAIProvider : AIProvider {
    override val id: String = "local_rules"
    override val isAvailable: Boolean = true
    override val requiresInternet: Boolean = false

    override suspend fun generate(request: AIRequest): AIResponse {
        val lastUserMessage = request.messages.lastOrNull { it.role == AIRole.USER }?.content ?: ""
        val reply = evaluateRules(lastUserMessage)
        return AIResponse.Text(content = reply, providerId = id)
    }

    fun evaluateRules(message: String): String {
        val lower = message.trim().lowercase(Locale.ROOT)

        return when {
            lower.contains("quien eres") || lower.contains("quién eres") || lower.contains("tu nombre") -> {
                "Soy VIERNES, tu asistente personal inteligente residente diseñada específicamente para tu Xiaomi Redmi Note 13 4G."
            }
            lower.contains("estado") || lower.contains("sistema") || lower.contains("diagnostico") -> {
                "Todos los sistemas en línea. Snapdragon 685 operando eficientemente, base de datos local Room activa y lista para órdenes."
            }
            lower.contains("procesador") || lower.contains("snapdragon") || lower.contains("cpu") -> {
                "Tu Redmi Note 13 cuenta con un procesador Qualcomm Snapdragon 685 de 8 núcleos a 2.80 GHz (4x Cortex-A73 y 4x Cortex-A53) con GPU Adreno 610."
            }
            lower.contains("ram") || lower.contains("memoria ram") -> {
                "Tu equipo cuenta con 8 GB de RAM física LPDDR4X más hasta 8 GB de extensión de memoria virtual en HyperOS 2."
            }
            lower.contains("almacenamiento") || lower.contains("disco") || lower.contains("espacio") -> {
                "El dispositivo cuenta con 256 GB de almacenamiento UFS 2.2 de alta velocidad."
            }
            lower.contains("bateria") || lower.contains("batería") || lower.contains("ahorrar") -> {
                "Para optimizar la batería en el Snapdragon 685: desactiva la extensión de RAM innecesaria, mantén la tasa de refresco adaptativa y cierra aplicaciones pesadas en segundo plano."
            }
            lower.contains("modelo") || lower.contains("telefono") || lower.contains("teléfono") -> {
                "Tu modelo es el Xiaomi Redmi Note 13 4G (23129RA5FL) ejecutando HyperOS 2 basado en Android 15."
            }
            lower.contains("hora") -> {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                "Son las $time."
            }
            lower.contains("fecha") || lower.contains("dia") || lower.contains("día") -> {
                val date = SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES")).format(Date())
                "Hoy es $date."
            }
            lower.contains("gracias") -> {
                "A la orden. Siempre lista para ayudarte."
            }
            lower.contains("hola") || lower.contains("buenos") || lower.contains("buenas") -> {
                "Hola. Lista para lo que necesites. Puedes decir «Viernes» o pedirme abrir una aplicación, consultar recuerdos o automatizar tareas."
            }
            lower.contains("que puedes hacer") || lower.contains("funciones") || lower.contains("ayuda") -> {
                "Puedo abrir aplicaciones («Viernes, abre WhatsApp»), recordar tus preferencias («Recuerda que mi navegador es Chrome»), resolver alias («Cuando diga mamá es María»), ejecutar rutinas («Activa modo estudio») y responder tus consultas con IA."
            }
            lower.contains("recuerdas") || lower.contains("memoria") || lower.contains("sabes de mi") -> {
                "Tengo recuerdos y preferencias registrados en mi base de datos local protegida. Puedes consultarlos o administrarlos en cualquier momento."
            }
            else -> {
                "Comprendido. He analizado tu solicitud a través del motor inteligente local de VIERNES. ¿Deseas ejecutar alguna acción o buscar información adicional?"
            }
        }
    }
}
