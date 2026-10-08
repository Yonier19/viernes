package com.example.assistant

import java.text.Normalizer
import java.util.Locale

class IntentParser {

    fun parse(input: String): ParsedIntent {
        val cleanInput = input.trim()
        val normalized = removeAccents(cleanInput.lowercase(Locale.ROOT))

        // Strip wake-words like "aura", "oye aura", "por favor"
        val stripped = stripPrefixes(normalized)
        val strippedCased = stripPrefixes(cleanInput)

        // 1. COMMAND_ALIAS_CREATE: "Cuando diga pon música, abre Spotify"
        if (stripped.startsWith("cuando diga") && (stripped.contains("abre") || stripped.contains("abrir") || stripped.contains("inicia") || stripped.contains("ejecuta"))) {
            val (phrase, target) = extractCommandAlias(strippedCased)
            if (phrase.isNotBlank() && target.isNotBlank()) {
                return ParsedIntent(
                    intent = IntentType.COMMAND_ALIAS_CREATE,
                    rawQuery = cleanInput,
                    commandPhrase = phrase,
                    commandTarget = target,
                    riskLevel = RiskLevel.LEVEL_2_MODERATE
                )
            }
        }

        // 2. CONTACT_ALIAS_CREATE: "Cuando diga mamá, me refiero a María"
        if (stripped.startsWith("cuando diga") && stripped.contains("me refiero a")) {
            val (alias, name) = extractContactAlias(strippedCased)
            if (alias.isNotBlank() && name.isNotBlank()) {
                return ParsedIntent(
                    intent = IntentType.CONTACT_ALIAS_CREATE,
                    rawQuery = cleanInput,
                    contactName = alias,
                    contactDisplayName = name,
                    riskLevel = RiskLevel.LEVEL_2_MODERATE
                )
            }
        }

        // 3. ROUTINE_DELETE: "Elimina la rutina modo estudio"
        if ((stripped.startsWith("elimina la rutina") || stripped.startsWith("borra la rutina") || stripped.startsWith("eliminar rutina")) ) {
            val routineName = stripped.replace(Regex("^(elimina|borra|eliminar|borrar)\\s+(la\\s+)?rutina\\s*"), "").trim()
            return ParsedIntent(
                intent = IntentType.ROUTINE_DELETE,
                rawQuery = cleanInput,
                routineName = routineName,
                riskLevel = RiskLevel.LEVEL_3_HIGH
            )
        }

        // 4. ROUTINE_QUERY: "¿Qué rutinas tengo?"
        if (stripped.contains("que rutinas tengo") || stripped.contains("muestra mis rutinas") ||
            stripped.contains("cuales son mis rutinas") || stripped == "rutinas"
        ) {
            return ParsedIntent(
                intent = IntentType.ROUTINE_QUERY,
                rawQuery = cleanInput,
                riskLevel = RiskLevel.LEVEL_1_LOW
            )
        }

        // 5. ROUTINE_CREATE: "Crea una rutina llamada modo estudio"
        if (stripped.startsWith("crea una rutina llamada") || stripped.startsWith("crea la rutina") ||
            stripped.startsWith("crear rutina") || stripped.startsWith("quiero crear una rutina")
        ) {
            val name = extractRoutineName(strippedCased)
            val actions = extractRoutineActions(strippedCased)
            return ParsedIntent(
                intent = IntentType.ROUTINE_CREATE,
                rawQuery = cleanInput,
                routineName = name,
                routineActions = actions,
                riskLevel = RiskLevel.LEVEL_2_MODERATE
            )
        }

        // 6. ROUTINE_EXECUTE: "Activa modo estudio", "Voy a estudiar"
        if (stripped.startsWith("activa modo") || stripped.startsWith("activar modo") ||
            stripped.startsWith("inicia rutina") || stripped.startsWith("activa la rutina") ||
            stripped == "modo estudio" || stripped == "voy a estudiar"
        ) {
            val routineName = if (stripped == "voy a estudiar") "modo estudio" else {
                stripped.replace(Regex("^(activa|activar|inicia|iniciar)\\s+(la\\s+)?(rutina\\s+)?(modo\\s+)?"), "modo ").trim()
            }
            return ParsedIntent(
                intent = IntentType.ROUTINE_EXECUTE,
                rawQuery = cleanInput,
                routineName = routineName,
                riskLevel = RiskLevel.LEVEL_1_LOW
            )
        }

        // 7. MEMORY_UPDATE: "Mi navegador favorito ahora es Firefox"
        if (stripped.contains("ahora es") || stripped.startsWith("cambia mi") || stripped.startsWith("actualiza mi")) {
            val (key, value) = extractMemoryUpdateKeyValue(strippedCased)
            if (key.isNotBlank() && value.isNotBlank()) {
                return ParsedIntent(
                    intent = IntentType.MEMORY_UPDATE,
                    rawQuery = cleanInput,
                    memoryKey = key,
                    memoryValue = value,
                    riskLevel = RiskLevel.LEVEL_2_MODERATE
                )
            }
        }

        // 8. MEMORY_DELETE: "Olvida mi navegador favorito", "Olvida que mi navegador favorito es Chrome"
        if (stripped.startsWith("olvida que") || stripped.startsWith("olvida lo que") ||
            stripped.startsWith("olvida mi") || stripped.startsWith("olvida ") ||
            stripped.startsWith("borra recuerdo") || stripped.startsWith("elimina lo que recuerdas")
        ) {
            val key = extractMemoryDeleteKey(stripped)
            return ParsedIntent(
                intent = IntentType.MEMORY_DELETE,
                rawQuery = cleanInput,
                memoryKey = key,
                riskLevel = RiskLevel.LEVEL_3_HIGH
            )
        }

        // 9. MEMORY_QUERY: "¿Cuál es mi navegador favorito?", "¿Qué recuerdas de mí?"
        if (stripped.contains("que recuerdas") || stripped.contains("que sabes de mi") ||
            stripped.contains("cual es mi") || stripped.contains("cuales son mis preferencias") ||
            stripped.contains("que memoria tienes") || stripped == "memoria"
        ) {
            val queryKey = extractMemoryQueryKey(stripped)
            return ParsedIntent(
                intent = IntentType.MEMORY_QUERY,
                rawQuery = cleanInput,
                memoryKey = queryKey,
                riskLevel = RiskLevel.LEVEL_1_LOW
            )
        }

        // 10. MEMORY_SAVE - EXPLICIT: "Recuerda que mi navegador favorito es Chrome"
        if (stripped.startsWith("recuerda que") || stripped.startsWith("aprende que") || stripped.startsWith("guarda que")) {
            val (key, value) = extractMemoryKeyValue(strippedCased)
            return ParsedIntent(
                intent = IntentType.MEMORY_SAVE,
                rawQuery = cleanInput,
                memoryKey = key,
                memoryValue = value,
                isExplicitCommand = true,
                riskLevel = RiskLevel.LEVEL_2_MODERATE
            )
        }

        // 11. MEMORY_SAVE - CONVERSATIONAL / IMPLICIT: "Mi navegador favorito es Chrome", "Mi color favorito es azul"
        if (stripped.startsWith("mi ") && (stripped.contains(" favorito es ") || stripped.contains(" favorita es ") || stripped.contains(" preferido es "))) {
            val (key, value) = extractMemoryKeyValue(strippedCased)
            return ParsedIntent(
                intent = IntentType.MEMORY_SAVE,
                rawQuery = cleanInput,
                memoryKey = key,
                memoryValue = value,
                isExplicitCommand = false, // Will ask confirmation!
                riskLevel = RiskLevel.LEVEL_2_MODERATE
            )
        }

        // 12. SEND_MESSAGE: "Escríbele a Juan que llego en diez minutos"
        if (stripped.startsWith("escribele a") || stripped.startsWith("escribe a") ||
            stripped.startsWith("mandale un mensaje a") || stripped.startsWith("manda mensaje a") ||
            stripped.startsWith("enviale un mensaje a") || stripped.startsWith("envia un mensaje a") ||
            stripped.startsWith("enviar mensaje a")
        ) {
            val (contact, message) = extractContactAndMessage(cleanInput)
            return ParsedIntent(
                intent = IntentType.SEND_MESSAGE,
                rawQuery = cleanInput,
                contactName = contact,
                messageBody = message,
                riskLevel = RiskLevel.LEVEL_3_HIGH
            )
        }

        // 13. REMINDER
        if (stripped.startsWith("recuerdame") || stripped.startsWith("recuerda me") ||
            stripped.startsWith("crea un recordatorio") || stripped.startsWith("pon un recordatorio")
        ) {
            val reminderBody = cleanInput.replace(Regex("(?i)^(recu[eé]rdame|crea un recordatorio para|pon un recordatorio para)\\s*"), "").trim()
            return ParsedIntent(
                intent = IntentType.REMINDER,
                rawQuery = cleanInput,
                reminderText = reminderBody,
                riskLevel = RiskLevel.LEVEL_2_MODERATE
            )
        }

        // 14. PLAY_MUSIC: "Pon música"
        if (stripped == "pon musica" || stripped == "reproduce musica" || stripped == "toca musica" ||
            stripped.startsWith("pon musica") || stripped.startsWith("reproduce ")
        ) {
            return ParsedIntent(
                intent = IntentType.PLAY_MUSIC,
                rawQuery = cleanInput,
                appName = "spotify",
                riskLevel = RiskLevel.LEVEL_1_LOW
            )
        }

        // 15. OPEN_APP: "Abre WhatsApp", "Abre mi navegador"
        if (stripped.startsWith("abre ") || stripped.startsWith("abrir ") ||
            stripped.startsWith("inicia ") || stripped.startsWith("iniciar ") ||
            stripped.startsWith("ejecuta ") || stripped.startsWith("lanza ")
        ) {
            val targetApp = stripped.replace(Regex("^(abre|abrir|inicia|iniciar|ejecuta|lanza)\\s+(la\\s+aplicacion\\s+de\\s+|la\\s+app\\s+de\\s+|el\\s+|la\\s+)?"), "").trim()
            return ParsedIntent(
                intent = IntentType.OPEN_APP,
                rawQuery = cleanInput,
                appName = targetApp,
                riskLevel = RiskLevel.LEVEL_1_LOW
            )
        }

        // Direct app names
        val directAppNames = listOf("whatsapp", "youtube", "spotify", "chrome", "instagram", "tiktok", "facebook", "camara", "ajustes", "calculadora")
        if (directAppNames.any { it == stripped || stripped == "abre $it" }) {
            val app = directAppNames.first { it == stripped || stripped == "abre $it" }
            return ParsedIntent(
                intent = IntentType.OPEN_APP,
                rawQuery = cleanInput,
                appName = app,
                riskLevel = RiskLevel.LEVEL_1_LOW
            )
        }

        // 16. SEARCH
        if (stripped.startsWith("busca ") || stripped.startsWith("buscar ") ||
            stripped.startsWith("investiga ") || stripped.startsWith("googlea ")
        ) {
            val query = cleanInput.replace(Regex("(?i)^(busca|buscar|investiga|googlea)\\s+(en\\s+google\\s+|en\\s+internet\\s+)?"), "").trim()
            return ParsedIntent(
                intent = IntentType.SEARCH,
                rawQuery = cleanInput,
                searchQuery = query,
                riskLevel = RiskLevel.LEVEL_1_LOW
            )
        }

        // 17. HELP & WAKE GREETING
        if (stripped.isBlank() || stripped == "viernes" || stripped.contains("ayuda") ||
            stripped.contains("que puedes hacer") || stripped.contains("quien eres") ||
            stripped.contains("como funcionas") || stripped == "hola" || stripped == "buenas"
        ) {
            return ParsedIntent(
                intent = IntentType.HELP,
                rawQuery = cleanInput,
                riskLevel = RiskLevel.LEVEL_1_LOW
            )
        }

        // Fallback: UNKNOWN
        return ParsedIntent(
            intent = IntentType.UNKNOWN,
            rawQuery = cleanInput,
            riskLevel = RiskLevel.LEVEL_1_LOW,
            confidence = 0.4f
        )
    }

    private fun stripPrefixes(input: String): String {
        val withoutWakeWord = com.example.voice.WakeWordNormalizer.normalize(input)
        return withoutWakeWord
            .replace(Regex("(?i)^por\\s+favor\\s+"), "")
            .trim()
    }

    private fun extractCommandAlias(input: String): Pair<String, String> {
        // e.g. "Cuando diga pon música, abre Spotify" or "Cuando diga pon música quiero que abras Spotify"
        val clean = stripPrefixes(input)
        val pattern = Regex("(?i)cuando diga\\s+[\"']?(.+?)[\"']?\\s*[,]?\\s*(?:quiero que\\s+)?(?:abras?|inicies?|ejecutes?|abre|inicia)\\s+[\"']?([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9\\s]+)[\"']?$")
        val match = pattern.find(clean)
        if (match != null) {
            val phrase = match.groupValues[1].trim()
            val target = match.groupValues[2].trim()
            return Pair(phrase, target)
        }
        return Pair("", "")
    }

    private fun extractContactAlias(input: String): Pair<String, String> {
        val clean = stripPrefixes(input)
        val pattern = Regex("(?i)cuando diga\\s+[\"']?([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9\\s]+?)[\"']?\\s*[,]?\\s*me refiero a\\s+[\"']?([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9\\s]+)[\"']?$")
        val match = pattern.find(clean)
        if (match != null) {
            val alias = match.groupValues[1].trim()
            val name = match.groupValues[2].trim()
            return Pair(alias, name)
        }
        return Pair("", "")
    }

    private fun extractMemoryKeyValue(input: String): Pair<String, String> {
        val clean = stripPrefixes(input).replace(Regex("(?i)^(recuerda\\s+que|aprende\\s+que|guarda\\s+que)\\s*"), "")

        // "mi [clave] favorito/a es [valor]"
        val favMatch = Regex("(?i)mi\\s+([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9_\\s]+?)\\s+(?:favorito|favorita|preferido|preferida)\\s+es\\s+(.+)").find(clean)
        if (favMatch != null) {
            val rawKey = favMatch.groupValues[1].trim()
            val key = "preferred_${removeAccents(rawKey).lowercase(Locale.ROOT).replace(" ", "_")}"
            val value = favMatch.groupValues[2].trim()
            return Pair(key, value)
        }

        // "mi [clave] es [valor]"
        val isMatch = Regex("(?i)mi\\s+([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9_\\s]+?)\\s+es\\s+(.+)").find(clean)
        if (isMatch != null) {
            val rawKey = isMatch.groupValues[1].trim()
            val key = removeAccents(rawKey).lowercase(Locale.ROOT).replace(" ", "_")
            val value = isMatch.groupValues[2].trim()
            return Pair(key, value)
        }

        return Pair("nota_general", clean)
    }

    private fun extractMemoryUpdateKeyValue(input: String): Pair<String, String> {
        val clean = stripPrefixes(input)
        // "Mi navegador favorito ahora es Firefox"
        val nowMatch = Regex("(?i)mi\\s+([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9_\\s]+?)\\s+(?:favorito|favorita|preferido|preferida)?\\s*ahora es\\s+(.+)").find(clean)
        if (nowMatch != null) {
            val rawKey = nowMatch.groupValues[1].trim()
            val key = "preferred_${removeAccents(rawKey).lowercase(Locale.ROOT).replace(" ", "_")}"
            val value = nowMatch.groupValues[2].trim()
            return Pair(key, value)
        }

        // "Cambia mi navegador favorito a Firefox"
        val changeMatch = Regex("(?i)(?:cambia|actualiza)\\s+mi\\s+([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9_\\s]+?)\\s+(?:favorito|favorita)?\\s*a\\s+(.+)").find(clean)
        if (changeMatch != null) {
            val rawKey = changeMatch.groupValues[1].trim()
            val key = "preferred_${removeAccents(rawKey).lowercase(Locale.ROOT).replace(" ", "_")}"
            val value = changeMatch.groupValues[2].trim()
            return Pair(key, value)
        }
        return Pair("", "")
    }

    private fun extractMemoryDeleteKey(input: String): String {
        val clean = input.replace(Regex("^(olvida\\s+que|olvida\\s+lo\\s+que|olvida\\s+mi\\s+|olvida\\s+|borra\\s+recuerdo\\s+de\\s+|elimina\\s+lo\\s+que\\s+recuerdas\\s+de\\s*)"), "")
            .replace("mi ", "")
            .replace(" favorito es.*".toRegex(), "")
            .replace(" favorita es.*".toRegex(), "")
            .replace(" favorito".toRegex(), "")
            .replace(" favorita".toRegex(), "")
            .trim()
        val normalizedKey = removeAccents(clean).lowercase(Locale.ROOT).replace(" ", "_")
        return if (!normalizedKey.startsWith("preferred_") && (clean.contains("navegador") || clean.contains("color"))) {
            "preferred_$normalizedKey"
        } else {
            normalizedKey
        }
    }

    private fun extractMemoryQueryKey(input: String): String? {
        val match = Regex("(?i)cual es mi\\s+([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9_\\s]+)").find(input)
        if (match != null) {
            val rawKey = match.groupValues[1].replace("(?i)favorito".toRegex(), "").replace("(?i)favorita".toRegex(), "").trim()
            val cleanKey = removeAccents(rawKey).lowercase(Locale.ROOT).replace(" ", "_")
            return if (!cleanKey.startsWith("preferred_")) "preferred_$cleanKey" else cleanKey
        }
        return null
    }

    private fun extractContactAndMessage(rawInput: String): Pair<String, String> {
        val pattern = Regex("(?i)^(?:escr[ií]bele\\s+a|escribe\\s+a|m[aá]ndale\\s+un\\s+mensaje\\s+a|manda\\s+mensaje\\s+a|env[ií]ale\\s+un\\s+mensaje\\s+a|env[ií]a\\s+un\\s+mensaje\\s+a)\\s+([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9]+)(?:\\s+(?:que|diciendo que|diciendo)\\s+(.+))?$")
        val match = pattern.find(rawInput)
        if (match != null) {
            val contact = match.groupValues[1].trim()
            val msg = match.groupValues.getOrNull(2)?.trim() ?: ""
            return Pair(contact, msg)
        }
        return Pair("Desconocido", "")
    }

    private fun extractRoutineName(input: String): String {
        val clean = stripPrefixes(input)
        val match = Regex("(?i)(?:rutina\\s+llamada|crea\\s+la\\s+rutina|crear\\s+rutina)\\s+[\"']?([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9\\s]+?)[\"']?(?:\\s+(?:con|que)|$)").find(clean)
        return match?.groupValues?.get(1)?.trim() ?: "modo estudio"
    }

    private fun extractRoutineActions(input: String): List<String> {
        val clean = stripPrefixes(input)
        val actions = mutableListOf<String>()
        if (clean.contains("chrome", ignoreCase = true)) actions.add("OPEN_APP:Chrome")
        if (clean.contains("notas", ignoreCase = true)) actions.add("OPEN_APP:Notas")
        if (clean.contains("youtube", ignoreCase = true)) actions.add("OPEN_APP:YouTube")
        if (clean.contains("spotify", ignoreCase = true)) actions.add("OPEN_APP:Spotify")
        if (actions.isEmpty()) {
            actions.add("OPEN_APP:Chrome")
            actions.add("OPEN_APP:Notas")
        }
        return actions
    }

    private fun removeAccents(text: String): String {
        val norm = Normalizer.normalize(text, Normalizer.Form.NFD)
        return norm.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}
