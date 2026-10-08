package com.example.assistant

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AIProviderTest {

    // 1. LocalRuleBasedAIProvider metadata and basic responses
    @Test
    fun testLocalRuleBasedAIProviderContract() = runBlocking {
        val localProvider = LocalRuleBasedAIProvider()
        assertEquals("local_rules", localProvider.id)
        assertTrue(localProvider.isAvailable)
        assertFalse(localProvider.requiresInternet)

        val request = AIRequest(
            messages = listOf(AIMessage(AIRole.USER, "¿Quién eres?"))
        )
        val response = localProvider.generate(request)
        assertTrue(response is AIResponse.Text)
        val text = (response as AIResponse.Text).content
        assertTrue(text.contains("VIERNES"))
    }

    @Test
    fun testLocalRuleBasedHardwareQuery() = runBlocking {
        val localProvider = LocalRuleBasedAIProvider()
        val request = AIRequest(
            messages = listOf(AIMessage(AIRole.USER, "Dime las especificaciones del procesador"))
        )
        val response = localProvider.generate(request)
        assertTrue(response is AIResponse.Text)
        val text = (response as AIResponse.Text).content
        assertTrue(text.contains("Snapdragon 685"))
    }

    // 2. GeminiAIProvider with missing API key returns Error(SinClave)
    @Test
    fun testGeminiAIProviderMissingKeyReturnsSinClave() = runBlocking {
        val geminiProvider = GeminiAIProvider(
            apiKeyProvider = { "" },
            modelProvider = { "gemini-2.5-flash" }
        )
        assertFalse(geminiProvider.isAvailable)
        assertTrue(geminiProvider.requiresInternet)

        val request = AIRequest(
            messages = listOf(AIMessage(AIRole.USER, "Hola"))
        )
        val response = geminiProvider.generate(request)
        assertTrue(response is AIResponse.Error)
        val error = response as AIResponse.Error
        assertEquals(AIErrorKind.SinClave, error.kind)
    }

    // 3. GenericChatCompletionsProvider with missing API key returns Error(SinClave)
    @Test
    fun testGenericProviderMissingKey() = runBlocking {
        val genericProvider = GenericChatCompletionsProvider(
            apiKeyProvider = { null }
        )
        assertFalse(genericProvider.isAvailable)
        val request = AIRequest(
            messages = listOf(AIMessage(AIRole.USER, "¿Qué tiempo hace?"))
        )
        val response = genericProvider.generate(request)
        assertTrue(response is AIResponse.Error)
        assertEquals(AIErrorKind.SinClave, (response as AIResponse.Error).kind)
    }

    // 4. ConversationManager sliding window and character trimming
    @Test
    fun testConversationManagerSlidingWindow() {
        val convManager = ConversationManager(maxTurns = 2, maxTotalCharacters = 500)

        // Adding 3 user turns (6 messages)
        convManager.addMessage(AIRole.USER, "Mensaje 1")
        convManager.addMessage(AIRole.ASSISTANT, "Respuesta 1")
        convManager.addMessage(AIRole.USER, "Mensaje 2")
        convManager.addMessage(AIRole.ASSISTANT, "Respuesta 2")
        convManager.addMessage(AIRole.USER, "Mensaje 3")
        convManager.addMessage(AIRole.ASSISTANT, "Respuesta 3")

        val messages = convManager.getMessages()
        // maxTurns = 2 -> at most 4 messages preserved
        assertEquals(4, messages.size)
        assertEquals("Mensaje 2", messages.first().content)
        assertEquals("Respuesta 3", messages.last().content)

        // Clear
        convManager.clear()
        assertEquals(0, convManager.getMessages().size)
    }

    // 5. ContextBuilder conditional hardware injection and privacy
    @Test
    fun testContextBuilderHardwareInclusion() {
        val builder = ContextBuilder(includeMemoriesByDefault = false)

        // Query WITHOUT hardware mention
        val instructionNoHw = builder.buildSystemInstruction(
            userQuery = "Escribe un poema breve",
            context = null,
            isExternalProvider = true
        )
        assertFalse(instructionNoHw.contains("Snapdragon 685"))

        // Query WITH hardware mention
        val instructionWithHw = builder.buildSystemInstruction(
            userQuery = "¿Cuánta memoria RAM tiene mi teléfono?",
            context = null,
            isExternalProvider = true
        )
        assertTrue(instructionWithHw.contains("Snapdragon 685"))
        assertTrue(instructionWithHw.contains("8 GB LPDDR4X"))
    }

    @Test
    fun testContextBuilderPrivacyByDefault() {
        val builder = ContextBuilder(includeMemoriesByDefault = false)
        val fakeMemory = com.example.memory.MemoryEntity(
            id = 1,
            type = com.example.memory.MemoryType.PERSONAL_INFORMATION,
            key = "secreto",
            value = "clave123",
            source = "test",
            confidence = 1.0f
        )
        val context = AssistantContext(memories = listOf(fakeMemory))

        // External provider should NOT include memory by default
        val externalInstruction = builder.buildSystemInstruction(
            userQuery = "Hola",
            context = context,
            isExternalProvider = true
        )
        assertFalse(externalInstruction.contains("clave123"))

        // Local provider CAN include memory
        val localInstruction = builder.buildSystemInstruction(
            userQuery = "Hola",
            context = context,
            isExternalProvider = false
        )
        assertTrue(localInstruction.contains("clave123"))
    }

    // 6. Streaming default implementation and backwards-compatible helper
    @Test
    fun testStreamingAndHelperBackwardCompatibility() = runBlocking {
        val localProvider = LocalRuleBasedAIProvider()
        val request = AIRequest(
            messages = listOf(AIMessage(AIRole.USER, "gracias"))
        )

        val chunks = localProvider.stream(request).toList()
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.first() is AIChunk.Content)

        // Backwards compatibility helper
        val speech = localProvider.generateResponse("gracias", AssistantContext())
        assertTrue(speech.contains("A la orden"))
    }
}
