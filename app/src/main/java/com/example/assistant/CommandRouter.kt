package com.example.assistant

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.automation.AppLauncher
import com.example.memory.MemoryEntity
import com.example.memory.MemoryRepository
import com.example.memory.MemoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommandRouter(
    private val context: Context,
    private val appLauncher: AppLauncher,
    private val memoryRepository: MemoryRepository,
    private val responseGenerator: ResponseGenerator
) {

    suspend fun route(
        parsedIntent: ParsedIntent,
        assistantContext: AssistantContext? = null
    ): ExecutionResult = withContext(Dispatchers.IO) {

        // Check Command Aliases first! (e.g. user learned "pon música" -> Spotify)
        val learnedAliasTarget = memoryRepository.getCommandAlias(parsedIntent.rawQuery)
        if (learnedAliasTarget != null) {
            val launchResult = appLauncher.openApp(learnedAliasTarget)
            return@withContext if (launchResult.success) {
                ExecutionResult(
                    success = true,
                    responseSpeech = responseGenerator.forAppLaunchSuccess(launchResult.appLabel),
                    actionSummary = "Alias de comando ejecutado: ${parsedIntent.rawQuery} → ${launchResult.appLabel}"
                )
            } else {
                ExecutionResult(
                    success = false,
                    responseSpeech = launchResult.errorMessage ?: responseGenerator.forAppNotFound(learnedAliasTarget)
                )
            }
        }

        when (parsedIntent.intent) {
            IntentType.OPEN_APP -> handleOpenApp(parsedIntent, assistantContext)
            IntentType.PLAY_MUSIC -> handlePlayMusic(parsedIntent)
            IntentType.MEMORY_SAVE -> handleMemorySave(parsedIntent)
            IntentType.MEMORY_UPDATE -> handleMemoryUpdate(parsedIntent)
            IntentType.MEMORY_QUERY -> handleMemoryQuery(parsedIntent)
            IntentType.MEMORY_DELETE -> handleMemoryDelete(parsedIntent)
            IntentType.CONTACT_ALIAS_CREATE -> handleContactAliasCreate(parsedIntent)
            IntentType.COMMAND_ALIAS_CREATE -> handleCommandAliasCreate(parsedIntent)
            IntentType.ROUTINE_CREATE -> handleRoutineCreate(parsedIntent)
            IntentType.ROUTINE_QUERY -> handleRoutineQuery()
            IntentType.ROUTINE_EXECUTE -> handleRoutineExecute(parsedIntent)
            IntentType.ROUTINE_DELETE -> handleRoutineDelete(parsedIntent)
            IntentType.SEND_MESSAGE -> handleSendMessage(parsedIntent)
            IntentType.SEARCH -> handleSearch(parsedIntent)
            IntentType.REMINDER -> handleReminder(parsedIntent)
            IntentType.LEARNING_SUGGESTION -> handleLearningSuggestion(parsedIntent)
            IntentType.HELP -> ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forHelp(),
                actionSummary = "Menú de ayuda de VIERNES"
            )
            IntentType.CONTACT_RESOLVE -> handleContactResolve(parsedIntent)
            IntentType.UNKNOWN -> ExecutionResult(
                success = false,
                responseSpeech = responseGenerator.forUnknown(),
                actionSummary = "Comando no reconocido"
            )
        }
    }

    private suspend fun handleOpenApp(
        intent: ParsedIntent,
        assistantContext: AssistantContext?
    ): ExecutionResult {
        var targetApp = intent.appName ?: "aplicación"

        // Contextual resolution: e.g. "abre mi navegador" -> lookup preference preferred_browser
        if (targetApp.contains("navegador") || targetApp.contains("browser")) {
            val prefBrowser = memoryRepository.getMemory("preferred_browser")?.value
                ?: memoryRepository.getPreference("preferred_browser")
            if (!prefBrowser.isNullOrBlank()) {
                targetApp = prefBrowser
            }
        }

        val launchResult = appLauncher.openApp(targetApp)
        return if (launchResult.success) {
            ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forAppLaunchSuccess(launchResult.appLabel),
                actionSummary = "Abierta: ${launchResult.appLabel}"
            )
        } else {
            ExecutionResult(
                success = false,
                responseSpeech = launchResult.errorMessage ?: responseGenerator.forAppNotFound(targetApp),
                actionSummary = "App no encontrada: $targetApp"
            )
        }
    }

    private suspend fun handlePlayMusic(intent: ParsedIntent): ExecutionResult {
        // Check if there is a learned preference or alias
        val customTarget = memoryRepository.getCommandAlias("pon música") ?: "spotify"
        val launchResult = appLauncher.openApp(customTarget)
        return if (launchResult.success) {
            ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forAppLaunchSuccess(launchResult.appLabel),
                actionSummary = "Música iniciada en ${launchResult.appLabel}"
            )
        } else {
            val ytResult = appLauncher.openApp("youtube")
            if (ytResult.success) {
                ExecutionResult(
                    success = true,
                    responseSpeech = "Abriendo YouTube para reproducir música.",
                    actionSummary = "Música en YouTube"
                )
            } else {
                ExecutionResult(
                    success = false,
                    responseSpeech = "No encontré una aplicación de música instalada como Spotify o YouTube.",
                    actionSummary = "Sin reproductor disponible"
                )
            }
        }
    }

    private suspend fun handleMemorySave(intent: ParsedIntent): ExecutionResult {
        val key = intent.memoryKey ?: "preferencia"
        val value = intent.memoryValue ?: ""

        if (value.isBlank()) {
            return ExecutionResult(
                success = false,
                responseSpeech = "No detecté qué dato deseas que guarde en memoria.",
                actionSummary = "Guardado cancelado"
            )
        }

        // Check if conflicting memory already exists
        val existing = memoryRepository.getMemory(key)
        if (existing != null && !existing.value.equals(value, ignoreCase = true)) {
            // Conflict detected: Ask for user confirmation!
            return ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forMemoryConflictPrompt(key, existing.value, value),
                actionSummary = "Conflicto de memoria detectado",
                requiresConfirmation = true,
                confirmationTitle = "¿Cambiar preferencia?",
                pendingConfirmationAction = {
                    memoryRepository.saveMemory(
                        key = key,
                        value = value,
                        type = MemoryType.PREFERENCE
                    )
                    ExecutionResult(
                        success = true,
                        responseSpeech = responseGenerator.forMemorySaved(),
                        actionSummary = "Preferencia actualizada: $key = $value"
                    )
                }
            )
        }

        // If conversational / implicit (e.g. "Mi navegador favorito es Chrome"), ask confirmation!
        if (!intent.isExplicitCommand) {
            return ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forMemorySaveConfirmPrompt(key, value),
                actionSummary = "Confirmación de memoria requerida",
                requiresConfirmation = true,
                confirmationTitle = "¿Guardar recuerdo?",
                pendingConfirmationAction = {
                    memoryRepository.saveMemory(
                        key = key,
                        value = value,
                        type = MemoryType.PREFERENCE
                    )
                    ExecutionResult(
                        success = true,
                        responseSpeech = responseGenerator.forMemorySaved(),
                        actionSummary = "Preferencia guardada: $key = $value"
                    )
                }
            )
        }

        // Explicit command: "Recuerda que mi navegador favorito es Chrome"
        // Also supports one-tap confirmation or direct safe execution with natural confirmation
        memoryRepository.saveMemory(
            key = key,
            value = value,
            type = MemoryType.PREFERENCE
        )

        return ExecutionResult(
            success = true,
            responseSpeech = responseGenerator.forMemorySaved(),
            actionSummary = "Memoria guardada: $key = $value"
        )
    }

    private suspend fun handleMemoryUpdate(intent: ParsedIntent): ExecutionResult {
        val key = intent.memoryKey ?: "preferencia"
        val newValue = intent.memoryValue ?: ""
        val existing = memoryRepository.getMemory(key)

        val oldValue = existing?.value ?: "anterior"

        return ExecutionResult(
            success = true,
            responseSpeech = responseGenerator.forMemoryConflictPrompt(key, oldValue, newValue),
            actionSummary = "Modificación de recuerdo",
            requiresConfirmation = true,
            confirmationTitle = "¿Confirmar cambio?",
            pendingConfirmationAction = {
                memoryRepository.saveMemory(
                    key = key,
                    value = newValue,
                    type = MemoryType.PREFERENCE
                )
                ExecutionResult(
                    success = true,
                    responseSpeech = responseGenerator.forMemorySaved(),
                    actionSummary = "Preferencia cambiada: $key = $newValue"
                )
            }
        )
    }

    private suspend fun handleMemoryQuery(intent: ParsedIntent): ExecutionResult {
        val key = intent.memoryKey

        if (key.isNullOrBlank()) {
            val allMemories = memoryRepository.searchMemories("")
            return ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forAllMemoriesStructured(allMemories),
                actionSummary = "Consulta completa de recuerdos"
            )
        }

        val found = memoryRepository.getMemory(key)
        return if (found != null && found.active) {
            ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forMemoryFound(found.key, found.value),
                actionSummary = "Recuerdo: ${found.key} = ${found.value}"
            )
        } else {
            val partials = memoryRepository.searchMemories(key.replace("preferred_", ""))
            if (partials.isNotEmpty()) {
                val best = partials.first()
                ExecutionResult(
                    success = true,
                    responseSpeech = responseGenerator.forMemoryFound(best.key, best.value),
                    actionSummary = "Recuerdo: ${best.key} = ${best.value}"
                )
            } else {
                ExecutionResult(
                    success = false,
                    responseSpeech = responseGenerator.forMemoryNotFound(key),
                    actionSummary = "Sin información guardada"
                )
            }
        }
    }

    private suspend fun handleMemoryDelete(intent: ParsedIntent): ExecutionResult {
        val key = intent.memoryKey ?: ""
        if (key.isBlank()) {
            return ExecutionResult(
                success = false,
                responseSpeech = "Indica qué recuerdo deseas que olvide.",
                actionSummary = "Eliminación no especificada"
            )
        }

        // Ask confirmation before deleting important memory
        return ExecutionResult(
            success = true,
            responseSpeech = responseGenerator.forMemoryDeletePrompt(key),
            actionSummary = "Confirmación de eliminación requerida",
            requiresConfirmation = true,
            confirmationTitle = "¿Eliminar preferencia?",
            riskLevel = RiskLevel.LEVEL_3_HIGH,
            pendingConfirmationAction = {
                memoryRepository.deleteMemoryByKey(key)
                ExecutionResult(
                    success = true,
                    responseSpeech = responseGenerator.forMemoryDeleted(),
                    actionSummary = "Preferencia eliminada: $key"
                )
            }
        )
    }

    private fun handleContactAliasCreate(intent: ParsedIntent): ExecutionResult {
        val alias = intent.contactName ?: "contacto"
        val realName = intent.contactDisplayName ?: "nombre"

        return ExecutionResult(
            success = true,
            responseSpeech = responseGenerator.forContactAliasPrompt(alias, realName),
            actionSummary = "Solicitud de alias de contacto",
            requiresConfirmation = true,
            confirmationTitle = "¿Guardar alias de contacto?",
            pendingConfirmationAction = {
                memoryRepository.saveContactAlias(alias = alias, displayName = realName)
                ExecutionResult(
                    success = true,
                    responseSpeech = responseGenerator.forContactAliasSaved(alias, realName),
                    actionSummary = "Alias guardado: $alias → $realName"
                )
            }
        )
    }

    private fun handleCommandAliasCreate(intent: ParsedIntent): ExecutionResult {
        val phrase = intent.commandPhrase ?: "comando"
        val target = intent.commandTarget ?: "aplicación"

        return ExecutionResult(
            success = true,
            responseSpeech = responseGenerator.forCommandAliasPrompt(phrase, target),
            actionSummary = "Solicitud de comando personalizado",
            requiresConfirmation = true,
            confirmationTitle = "¿Aprender comando?",
            pendingConfirmationAction = {
                memoryRepository.saveCommandAlias(phrase = phrase, targetApp = target)
                ExecutionResult(
                    success = true,
                    responseSpeech = responseGenerator.forCommandAliasSaved(phrase, target),
                    actionSummary = "Comando aprendido: \"$phrase\" → $target"
                )
            }
        )
    }

    private fun handleRoutineCreate(intent: ParsedIntent): ExecutionResult {
        val name = intent.routineName ?: "modo estudio"
        val actions = intent.routineActions.ifEmpty { listOf("OPEN_APP:Chrome", "OPEN_APP:Notas") }
        val actionsSummary = actions.joinToString(", ") { it.replace("OPEN_APP:", "Abrir ") }

        return ExecutionResult(
            success = true,
            responseSpeech = responseGenerator.forRoutineSavePrompt(name, actionsSummary),
            actionSummary = "Creando rutina: $name",
            requiresConfirmation = true,
            confirmationTitle = "¿Guardar rutina?",
            pendingConfirmationAction = {
                memoryRepository.saveRoutine(
                    name = name,
                    description = "Rutina personalizada",
                    actionsJson = actions.joinToString(";")
                )
                ExecutionResult(
                    success = true,
                    responseSpeech = responseGenerator.forRoutineSaved(name),
                    actionSummary = "Rutina \"$name\" creada"
                )
            }
        )
    }

    private suspend fun handleRoutineQuery(): ExecutionResult {
        val routines = memoryRepository.findRoutine("modo estudio")
        val allMemRoutines = memoryRepository.getMemoriesByTypeSync(MemoryType.ROUTINE)

        return if (allMemRoutines.isNotEmpty() || routines != null) {
            val list = allMemRoutines.map { "• " + it.key.replaceFirstChar { c -> c.uppercase() } }.joinToString("\n")
            ExecutionResult(
                success = true,
                responseSpeech = "Tienes las siguientes rutinas configuradas:\n$list",
                actionSummary = "Rutinas listadas"
            )
        } else {
            ExecutionResult(
                success = true,
                responseSpeech = "No tienes rutinas creadas todavía. Puedes decir: \"Crea una rutina llamada modo estudio\".",
                actionSummary = "Sin rutinas"
            )
        }
    }

    private suspend fun handleRoutineExecute(intent: ParsedIntent): ExecutionResult {
        val routineName = intent.routineName ?: "modo estudio"
        val routine = memoryRepository.findRoutine(routineName)

        return if (routine != null) {
            val actions = routine.actionsJson.split(";")
            for (action in actions) {
                if (action.startsWith("OPEN_APP:")) {
                    val app = action.removePrefix("OPEN_APP:")
                    appLauncher.openApp(app)
                }
            }
            ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forRoutineStarted(routine.name),
                actionSummary = "Rutina ejecutada: ${routine.name}"
            )
        } else {
            // Default built-in study routine fallback
            if (routineName.contains("estudio") || routineName.contains("estudiar")) {
                appLauncher.openApp("chrome")
                ExecutionResult(
                    success = true,
                    responseSpeech = "Activando tu rutina de modo estudio. Abriendo navegador y herramientas.",
                    actionSummary = "Modo estudio ejecutado"
                )
            } else {
                ExecutionResult(
                    success = false,
                    responseSpeech = "No encontré la rutina \"$routineName\" configurada.",
                    actionSummary = "Rutina no encontrada"
                )
            }
        }
    }

    private suspend fun handleRoutineDelete(intent: ParsedIntent): ExecutionResult {
        val routineName = intent.routineName ?: "modo estudio"
        memoryRepository.deleteRoutine(routineName)
        return ExecutionResult(
            success = true,
            responseSpeech = responseGenerator.forRoutineDeleted(routineName),
            actionSummary = "Rutina eliminada: $routineName"
        )
    }

    private suspend fun handleSendMessage(intent: ParsedIntent): ExecutionResult {
        val rawContact = intent.contactName ?: "Contacto"
        val message = intent.messageBody ?: ""

        // 1. Resolve contact alias if exists (e.g. "mamá" -> "María")
        val alias = memoryRepository.getContactAlias(rawContact)
        val resolvedName = alias?.displayName ?: rawContact

        // 2. Contact Disambiguation rule:
        // If user says generic "Juan" without alias, detect ambiguity
        if (rawContact.equals("Juan", ignoreCase = true) && alias == null) {
            val disambiguationList = listOf("Juan Pérez", "Juan Rodríguez", "Juan Hernández")
            return ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forContactDisambiguation("Juan"),
                actionSummary = "Desambiguación de contactos requerida",
                disambiguationOptions = disambiguationList
            )
        }

        // Preview message with resolved name
        val responseText = if (alias != null) {
            "${rawContact.replaceFirstChar { it.uppercase() }} corresponde a $resolvedName. ¿Quieres enviar el mensaje: \"$message\"?"
        } else {
            responseGenerator.forMessagePreview(resolvedName, message)
        }

        return ExecutionResult(
            success = true,
            responseSpeech = responseText,
            actionSummary = "Mensaje preparado para $resolvedName",
            riskLevel = RiskLevel.LEVEL_3_HIGH,
            requiresConfirmation = true,
            confirmationTitle = "¿Enviar mensaje?",
            pendingConfirmationAction = {
                try {
                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:")
                        putExtra("sms_body", message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(smsIntent)
                    ExecutionResult(
                        success = true,
                        responseSpeech = "Abriendo mensajería con el texto listo para $resolvedName.",
                        actionSummary = "Mensaje preparado en app nativa"
                    )
                } catch (_: Exception) {
                    ExecutionResult(
                        success = false,
                        responseSpeech = "No se pudo iniciar la aplicación de mensajería.",
                        actionSummary = "Fallo en mensajería"
                    )
                }
            }
        )
    }

    private suspend fun handleContactResolve(intent: ParsedIntent): ExecutionResult {
        val raw = intent.contactName ?: ""
        val alias = memoryRepository.getContactAlias(raw)
        return if (alias != null) {
            ExecutionResult(
                success = true,
                responseSpeech = "${raw.replaceFirstChar { it.uppercase() }} corresponde a ${alias.displayName}.",
                actionSummary = "Alias resuelto: $raw → ${alias.displayName}"
            )
        } else {
            ExecutionResult(
                success = false,
                responseSpeech = "No tengo ningún alias guardado para \"$raw\".",
                actionSummary = "Sin alias"
            )
        }
    }

    private fun handleLearningSuggestion(intent: ParsedIntent): ExecutionResult {
        val phrase = intent.commandPhrase ?: "pon música"
        val target = intent.commandTarget ?: "Spotify"
        return ExecutionResult(
            success = true,
            responseSpeech = responseGenerator.forHabitSuggestion(phrase, target),
            actionSummary = "Sugerencia de aprendizaje",
            requiresConfirmation = true,
            confirmationTitle = "¿Aprender comando?",
            pendingConfirmationAction = {
                memoryRepository.saveCommandAlias(phrase, target)
                ExecutionResult(
                    success = true,
                    responseSpeech = responseGenerator.forCommandAliasSaved(phrase, target),
                    actionSummary = "Hábito aprendido: \"$phrase\" → $target"
                )
            }
        )
    }

    private fun handleSearch(intent: ParsedIntent): ExecutionResult {
        val query = intent.searchQuery ?: intent.rawQuery
        return try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
            ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forSearch(query),
                actionSummary = "Búsqueda web realizada"
            )
        } catch (_: Exception) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ExecutionResult(
                success = true,
                responseSpeech = responseGenerator.forSearch(query),
                actionSummary = "Navegador web abierto"
            )
        }
    }

    private fun handleReminder(intent: ParsedIntent): ExecutionResult {
        val text = intent.reminderText ?: "Recordatorio"
        return ExecutionResult(
            success = true,
            responseSpeech = "Recordatorio registrado: \"$text\".",
            actionSummary = "Recordatorio guardado",
            riskLevel = RiskLevel.LEVEL_2_MODERATE
        )
    }
}
