package com.example.assistant

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContextBuilder(
    private val includeMemoriesByDefault: Boolean = false
) {

    fun buildSystemInstruction(
        userQuery: String,
        context: AssistantContext? = null,
        isExternalProvider: Boolean = true,
        availableTools: List<AIToolDefinition> = emptyList(),
        allowedCategories: Set<MemoryCategory> = emptySet()
    ): String {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val dateFormat = SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES")).format(now)

        val sb = StringBuilder()
        sb.appendLine("Eres VIERNES, una asistente virtual personal inteligente y concisa.")
        sb.appendLine("Fecha y hora actual: $dateFormat, $timeFormat.")
        sb.appendLine("Tono: Directa, profesional, perspicaz y amigable. Responde en español en un máximo de 2 a 3 oraciones.")

        // Inclusión condicional de perfil de hardware SOLO si la consulta trata del teléfono
        val lowerQuery = userQuery.lowercase(Locale.ROOT)
        val hardwareKeywords = listOf(
            "teléfono", "telefono", "dispositivo", "equipo", "celular", "móvil", "movil",
            "hardware", "snapdragon", "procesador", "cpu", "gpu", "ram", "memoria ram",
            "almacenamiento", "batería", "bateria", "pantalla", "amoled", "hyperos", "xiaomi", "redmi"
        )

        val queryMentionsHardware = hardwareKeywords.any { lowerQuery.contains(it) }
        if (queryMentionsHardware) {
            sb.appendLine("\n[Información de Hardware del Dispositivo]")
            sb.appendLine("- Modelo: Xiaomi Redmi Note 13 4G (23129RA5FL)")
            sb.appendLine("- SoC: Qualcomm Snapdragon 685 (8 núcleos a 2.80 GHz) con Adreno 610")
            sb.appendLine("- Memoria: 8 GB LPDDR4X física (+ extensión virtual)")
            sb.appendLine("- Almacenamiento: 256 GB UFS 2.2")
            sb.appendLine("- Sistema Operativo: Android 15 con HyperOS 2")
            sb.appendLine("- Pantalla: AMOLED de 6.67 pulgadas, 120 Hz, 2400 x 1080")
        }

        // Inclusión de recuerdos: Por defecto NO se comparten con proveedores externos salvo categorías autorizadas
        context?.let { ctx ->
            val memoriesToShare = if (!isExternalProvider || includeMemoriesByDefault) {
                ctx.memories
            } else {
                ctx.memories.filter { mem ->
                    when (mem.type) {
                        com.example.memory.MemoryType.PREFERENCE -> allowedCategories.contains(MemoryCategory.PREFERENCES)
                        com.example.memory.MemoryType.PERSONAL_INFORMATION -> allowedCategories.contains(MemoryCategory.PERSONAL_INFO)
                        com.example.memory.MemoryType.CONTACT_ALIAS -> allowedCategories.contains(MemoryCategory.CONTACT_ALIASES)
                        com.example.memory.MemoryType.COMMAND_ALIAS -> allowedCategories.contains(MemoryCategory.COMMANDS)
                        com.example.memory.MemoryType.HABIT -> allowedCategories.contains(MemoryCategory.HABITS)
                        com.example.memory.MemoryType.ROUTINE -> allowedCategories.contains(MemoryCategory.ROUTINES)
                    }
                }
            }

            if (memoriesToShare.isNotEmpty()) {
                val sample = memoriesToShare.take(5).joinToString("; ") { "${it.key}: ${it.value}" }
                sb.appendLine("\n[Recuerdos del usuario autorizados]: $sample")
            }
        }

        // Catálogo de herramientas si están presentes
        if (availableTools.isNotEmpty()) {
            sb.appendLine("\n[Herramientas del sistema disponibles]")
            for (tool in availableTools) {
                sb.appendLine("- ${tool.name}: ${tool.description}")
            }
            sb.appendLine("Si la solicitud del usuario requiere interactuar con el teléfono o sus herramientas, invoca la herramienta correspondiente en lugar de inventar la respuesta.")
        }

        return sb.toString().trim()
    }
}
