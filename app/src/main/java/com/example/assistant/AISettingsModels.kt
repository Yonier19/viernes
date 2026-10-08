package com.example.assistant

enum class AIActiveMode {
    AUTO,
    SOLO_LOCAL,
    SOLO_EXTERNO
}

enum class AIExternalService {
    GEMINI,
    GENERIC_CHAT_COMPLETIONS
}

enum class MemoryCategory(
    val id: String,
    val title: String,
    val description: String
) {
    PREFERENCES(
        id = "preferences",
        title = "Preferencias",
        description = "Preferencias aprendidas sobre apps, música y configuración."
    ),
    PERSONAL_INFO(
        id = "personal_info",
        title = "Información personal",
        description = "Datos sobre tu identidad y contexto que hayas guardado."
    ),
    CONTACT_ALIASES(
        id = "contact_aliases",
        title = "Alias de contactos",
        description = "Asociaciones como «mamá» hacia el contacto real «María»."
    ),
    COMMANDS(
        id = "commands",
        title = "Comandos",
        description = "Atajos de comandos y frases personalizadas."
    ),
    HABITS(
        id = "habits",
        title = "Hábitos",
        description = "Patrones aprendidos por el motor de aprendizaje continuo."
    ),
    ROUTINES(
        id = "routines",
        title = "Rutinas",
        description = "Acciones encadenadas y rutinas automáticas."
    )
}

sealed interface ConnectionTestResult {
    data object Conectado : ConnectionTestResult
    data object ClaveInvalida : ConnectionTestResult
    data object SinInternet : ConnectionTestResult
    data object TiempoAgotado : ConnectionTestResult
    data class Error(val message: String) : ConnectionTestResult
}

data class AISettingsState(
    val activeMode: AIActiveMode = AIActiveMode.AUTO,
    val externalService: AIExternalService = AIExternalService.GEMINI,
    val genericBaseUrl: String = "https://api.openai.com/v1",
    val selectedModel: String = "gemini-2.5-flash",
    val hasSavedApiKey: Boolean = false,
    val allowedMemoryCategories: Set<MemoryCategory> = emptySet(),
    val isTestingConnection: Boolean = false,
    val connectionTestResult: ConnectionTestResult? = null
)
