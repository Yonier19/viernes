package com.example.assistant

import com.example.memory.MemoryEntity
import com.example.memory.MemoryType
import kotlin.random.Random

class ResponseGenerator {

    private val saveSuccessPhrases = listOf(
        "Perfecto, lo recordaré.",
        "Listo, guardé esa preferencia.",
        "Entendido, lo he registrado en mi memoria.",
        "De acuerdo, ya lo tengo presente."
    )

    private val deleteSuccessPhrases = listOf(
        "Listo, he eliminado esa información.",
        "He olvidado esa preferencia.",
        "Dato eliminado de mi memoria.",
        "De acuerdo, lo he borrado."
    )

    fun forAppLaunchSuccess(appName: String): String {
        return "Abriendo $appName."
    }

    fun forAppNotFound(appName: String): String {
        return "No encuentro la aplicación \"$appName\" instalada en tu dispositivo."
    }

    fun forMemorySaveConfirmPrompt(key: String, value: String): String {
        val readableKey = formatKey(key)
        return "¿Quieres que recuerde que tu $readableKey es $value?"
    }

    fun forMemoryConflictPrompt(key: String, oldValue: String, newValue: String): String {
        val readableKey = formatKey(key)
        return "Ya tengo guardado $oldValue como tu $readableKey. ¿Quieres cambiarlo a $newValue?"
    }

    fun forMemorySaved(): String {
        return saveSuccessPhrases[Random.nextInt(saveSuccessPhrases.size)]
    }

    fun forMemoryDeletePrompt(key: String): String {
        val readableKey = formatKey(key)
        return "¿Quieres eliminar la información sobre tu $readableKey?"
    }

    fun forMemoryDeleted(): String {
        return deleteSuccessPhrases[Random.nextInt(deleteSuccessPhrases.size)]
    }

    fun forMemoryFound(key: String, value: String): String {
        val readableKey = formatKey(key)
        return "Tu $readableKey es $value."
    }

    fun forMemoryNotFound(key: String): String {
        val readableKey = formatKey(key)
        return "Aún no tengo información registrada sobre tu $readableKey."
    }

    fun forContactAliasPrompt(alias: String, realName: String): String {
        return "¿Quieres guardar el alias de $alias para $realName?"
    }

    fun forContactAliasSaved(alias: String, realName: String): String {
        return "Perfecto. Cuando digas \"$alias\", sabré que te refieres a $realName."
    }

    fun forCommandAliasPrompt(phrase: String, targetApp: String): String {
        return "Puedo crear ese comando personalizado para abrir $targetApp cuando digas \"$phrase\". ¿Quieres guardarlo?"
    }

    fun forCommandAliasSaved(phrase: String, targetApp: String): String {
        return "Listo. Cuando digas \"$phrase\", abriré $targetApp."
    }

    fun forHabitSuggestion(phrase: String, targetApp: String): String {
        return "He notado que normalmente abres $targetApp cuando dices \"$phrase\". ¿Quieres que aprenda este comando?"
    }

    fun forRoutineSavePrompt(name: String, actionSummary: String): String {
        return "La rutina \"$name\" ejecutará: $actionSummary. ¿Quieres guardarla?"
    }

    fun forRoutineSaved(name: String): String {
        return "Rutina \"$name\" guardada exitosamente."
    }

    fun forRoutineStarted(routineName: String): String {
        return "Activando tu rutina \"$routineName\"."
    }

    fun forRoutineDeleted(routineName: String): String {
        return "Rutina \"$routineName\" eliminada."
    }

    fun forContactDisambiguation(name: String): String {
        return "Encontré varios contactos llamados \"$name\". ¿A cuál te refieres?"
    }

    fun forAllMemoriesStructured(memories: List<MemoryEntity>): String {
        if (memories.isEmpty()) {
            return "Actualmente mi memoria local está vacía. Puedes decirme por ejemplo: \"Recuerda que mi navegador favorito es Chrome\"."
        }

        val preferences = memories.filter { it.type == MemoryType.PREFERENCE || it.type == MemoryType.PERSONAL_INFORMATION }
        val contacts = memories.filter { it.type == MemoryType.CONTACT_ALIAS }
        val commands = memories.filter { it.type == MemoryType.COMMAND_ALIAS }
        val routines = memories.filter { it.type == MemoryType.ROUTINE }

        val builder = StringBuilder("Tus recuerdos:\n\n")

        if (preferences.isNotEmpty()) {
            builder.append("Preferencias:\n")
            preferences.forEach {
                builder.append("• ").append(formatKey(it.key).replaceFirstChar { c -> c.uppercase() }).append(": ").append(it.value).append("\n")
            }
            builder.append("\n")
        }

        if (contacts.isNotEmpty()) {
            builder.append("Contactos:\n")
            contacts.forEach {
                builder.append("• ").append(it.key.replaceFirstChar { c -> c.uppercase() }).append(": ").append(it.value).append("\n")
            }
            builder.append("\n")
        }

        if (commands.isNotEmpty()) {
            builder.append("Comandos:\n")
            commands.forEach {
                builder.append("• \"").append(it.key).append("\": ").append(it.value).append("\n")
            }
            builder.append("\n")
        }

        if (routines.isNotEmpty()) {
            builder.append("Rutinas:\n")
            routines.forEach {
                builder.append("• ").append(it.key.replaceFirstChar { c -> c.uppercase() }).append("\n")
            }
        }

        return builder.toString().trim()
    }

    fun forMessagePreview(contact: String, message: String): String {
        return "He preparado un mensaje para $contact: \"$message\". ¿Confirmas el envío?"
    }

    fun forSearch(query: String): String {
        return "Buscando en la web: \"$query\"."
    }

    fun forHelp(): String {
        return "Soy VIERNES, tu asistente virtual inteligente residente para tu Redmi Note 13.\n" +
                "• \"Abre WhatsApp\" o \"Pon música\"\n" +
                "• \"Recuerda que mi navegador favorito es Chrome\"\n" +
                "• \"¿Cuál es mi navegador favorito?\"\n" +
                "• \"Cuando diga mamá me refiero a María\"\n" +
                "• \"Cuando diga pon música abre Spotify\"\n" +
                "• \"Crea una rutina llamada modo estudio\"\n" +
                "• \"Activa modo estudio\"\n" +
                "• \"¿Qué recuerdas de mí?\""
    }

    fun forUnknown(): String {
        return "No entendí ese comando. Puedes probar diciendo: \"Abre WhatsApp\", \"Recuerda que...\" o \"Ayuda\"."
    }

    private fun formatKey(key: String): String {
        return key.replace("_", " ")
    }
}
