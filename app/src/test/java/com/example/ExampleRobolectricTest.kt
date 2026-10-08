package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.assistant.CommandRouter
import com.example.assistant.IntentParser
import com.example.assistant.IntentType
import com.example.assistant.ResponseGenerator
import com.example.automation.AppLauncher
import com.example.memory.LearningEngine
import com.example.memory.MemoryDatabase
import com.example.memory.MemoryRepository
import com.example.memory.MemoryType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: MemoryDatabase
    private lateinit var repository: MemoryRepository
    private lateinit var parser: IntentParser
    private lateinit var router: CommandRouter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = MemoryDatabase.getDatabase(context)
        repository = MemoryRepository(db)
        parser = IntentParser()
        val appLauncher = AppLauncher(context)
        val responseGen = ResponseGenerator()
        router = CommandRouter(context, appLauncher, repository, responseGen)
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("VIERNES", appName)
    }

    // 1. MEMORIA: GUARDAR Y CONSULTAR PREFERENCIA
    @Test
    fun `memory save and query preference`() = runBlocking {
        val parsedSave = parser.parse("VIERNES, recuerda que mi navegador favorito es Chrome")
        assertEquals(IntentType.MEMORY_SAVE, parsedSave.intent)
        assertEquals("preferred_navegador", parsedSave.memoryKey)
        assertEquals("Chrome", parsedSave.memoryValue)

        repository.saveMemory(
            key = parsedSave.memoryKey!!,
            value = parsedSave.memoryValue!!,
            type = MemoryType.PREFERENCE
        )

        val parsedQuery = parser.parse("VIERNES, ¿cuál es mi navegador favorito?")
        assertEquals(IntentType.MEMORY_QUERY, parsedQuery.intent)
        assertEquals("preferred_navegador", parsedQuery.memoryKey)

        val memory = repository.getMemory(parsedQuery.memoryKey!!)
        assertNotNull(memory)
        assertEquals("Chrome", memory?.value)
    }

    // 2. MEMORIA: MODIFICAR PREFERENCIA
    @Test
    fun `memory update detection`() = runBlocking {
        repository.saveMemory("preferred_navegador", "Chrome", MemoryType.PREFERENCE)

        val parsedUpdate = parser.parse("Mi navegador favorito ahora es Firefox")
        assertEquals(IntentType.MEMORY_UPDATE, parsedUpdate.intent)
        assertEquals("preferred_navegador", parsedUpdate.memoryKey)
        assertEquals("Firefox", parsedUpdate.memoryValue)

        val existing = repository.getMemory("preferred_navegador")
        assertEquals("Chrome", existing?.value)

        // Execute update
        repository.saveMemory("preferred_navegador", "Firefox", MemoryType.PREFERENCE)
        val updated = repository.getMemory("preferred_navegador")
        assertEquals("Firefox", updated?.value)
    }

    // 3. MEMORIA: ELIMINAR PREFERENCIA
    @Test
    fun `memory delete preference`() = runBlocking {
        repository.saveMemory("preferred_navegador", "Chrome", MemoryType.PREFERENCE)
        val parsedDelete = parser.parse("Olvida mi navegador favorito")
        assertEquals(IntentType.MEMORY_DELETE, parsedDelete.intent)
        assertEquals("preferred_navegador", parsedDelete.memoryKey)

        repository.deleteMemoryByKey("preferred_navegador")
        val deleted = repository.getMemory("preferred_navegador")
        assertTrue(deleted == null || !deleted.active)
    }

    // 4. CONTACTOS: ALIAS Y RESOLUCIÓN
    @Test
    fun `contact alias creation and resolution`() = runBlocking {
        val parsedAlias = parser.parse("Cuando diga mamá, me refiero a María")
        assertEquals(IntentType.CONTACT_ALIAS_CREATE, parsedAlias.intent)
        assertEquals("mamá", parsedAlias.contactName)
        assertEquals("María", parsedAlias.contactDisplayName)

        repository.saveContactAlias(
            alias = parsedAlias.contactName!!,
            displayName = parsedAlias.contactDisplayName!!
        )

        val alias = repository.getContactAlias("mamá")
        assertNotNull(alias)
        assertEquals("María", alias?.displayName)

        // Resolving message with alias
        val parsedMsg = parser.parse("Escríbele a mamá que ya llegué")
        assertEquals(IntentType.SEND_MESSAGE, parsedMsg.intent)
        assertEquals("mamá", parsedMsg.contactName)
    }

    // 5. CONTACTOS: DESAMBIGUACIÓN DE JUAN
    @Test
    fun `contact disambiguation for common name`() = runBlocking {
        val parsedMsg = parser.parse("Escríbele a Juan")
        assertEquals(IntentType.SEND_MESSAGE, parsedMsg.intent)

        val result = router.route(parsedMsg)
        assertTrue(result.disambiguationOptions.isNotEmpty())
        assertTrue(result.disambiguationOptions.contains("Juan Pérez"))
    }

    // 6. COMANDOS: ALIAS PERSONALIZADO
    @Test
    fun `command alias learning and execution`() = runBlocking {
        val parsedCommand = parser.parse("Cuando diga pon música, abre Spotify")
        assertEquals(IntentType.COMMAND_ALIAS_CREATE, parsedCommand.intent)
        assertEquals("pon música", parsedCommand.commandPhrase)
        assertEquals("Spotify", parsedCommand.commandTarget)

        repository.saveCommandAlias("pon música", "Spotify")
        val target = repository.getCommandAlias("pon música")
        assertEquals("Spotify", target)
    }

    // 7. APRENDIZAJE DE HÁBITOS: DETECCIÓN CON UMBRAL >= 3
    @Test
    fun `habit learning threshold behavior`() = runBlocking {
        val engine = LearningEngine(db.memoryDao(), db.commandHistoryDao(), minOccurrencesThreshold = 3)

        // 1 & 2 occurrences: no suggestion
        repository.logCommand(rawText = "escuchar rock", intent = "OPEN_APP", target = "Spotify", executionResult = "ok", isSuccess = true)
        var patterns = engine.detectPatterns()
        assertTrue(patterns.isEmpty())

        repository.logCommand(rawText = "escuchar rock", intent = "OPEN_APP", target = "Spotify", executionResult = "ok", isSuccess = true)
        patterns = engine.detectPatterns()
        assertTrue(patterns.isEmpty())

        // 3rd occurrence: pattern detected
        repository.logCommand(rawText = "escuchar rock", intent = "OPEN_APP", target = "Spotify", executionResult = "ok", isSuccess = true)
        patterns = engine.detectPatterns()
        assertFalse(patterns.isEmpty())
        assertEquals("escuchar rock", patterns.first().phrase)
        assertEquals("Spotify", patterns.first().target)
        assertTrue(patterns.first().confidence >= 0.70f)
    }

    // 8. RUTINAS: CREACIÓN, CONSULTA Y EJECUCIÓN
    @Test
    fun `routine creation query and execute`() = runBlocking {
        val parsedRoutine = parser.parse("Crea una rutina llamada modo estudio")
        assertEquals(IntentType.ROUTINE_CREATE, parsedRoutine.intent)
        assertEquals("modo estudio", parsedRoutine.routineName)

        repository.saveRoutine(
            name = "modo estudio",
            description = "Rutina para estudiar",
            actionsJson = "OPEN_APP:Chrome;OPEN_APP:Notas"
        )

        val routine = repository.findRoutine("modo estudio")
        assertNotNull(routine)
        assertEquals("modo estudio", routine?.name)

        val parsedExec = parser.parse("Activa modo estudio")
        assertEquals(IntentType.ROUTINE_EXECUTE, parsedExec.intent)
        assertEquals("modo estudio", parsedExec.routineName)
    }

    // 9. PALABRA CLAVE Y APERTURA DE APPS CON VIERNES
    @Test
    fun `wake word stripping and open app intent`() {
        val parsed = parser.parse("Viernes, abre WhatsApp")
        assertEquals(IntentType.OPEN_APP, parsed.intent)
        assertEquals("whatsapp", parsed.appName?.lowercase())

        val parsedOye = parser.parse("Oye Viernes, pon música")
        assertEquals(IntentType.PLAY_MUSIC, parsedOye.intent)
    }

    // 10. MOTOR DE RAZONAMIENTO E IA LOCAL PARA SNAPDRAGON 685
    @Test
    fun `local ai reasoning provider fallback`() = runBlocking {
        val localAi = com.example.assistant.LocalRuleBasedAIProvider()
        val context = com.example.assistant.AssistantContext(emptyList())

        val replyIdentity = localAi.generateResponse("¿Quién eres?", context)
        assertTrue(replyIdentity.contains("VIERNES"))

        val replyStatus = localAi.generateResponse("Estado del sistema", context)
        assertTrue(replyStatus.contains("Snapdragon 685"))
    }
}
