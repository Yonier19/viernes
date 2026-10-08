package com.example.assistant

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.SecureKeyStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AISettingsManagerTest {

    private lateinit var context: Context
    private lateinit var settingsManager: AISettingsManager
    private lateinit var secureKeyStore: SecureKeyStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        secureKeyStore = SecureKeyStore(context)
        secureKeyStore.delete()

        // Limpiar SharedPreferences de configuración
        context.getSharedPreferences("viernes_ai_config", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        settingsManager = AISettingsManager(context, secureKeyStore)
    }

    @Test
    fun testDefaultSettings() {
        val state = settingsManager.settingsState.value
        assertEquals(AIActiveMode.AUTO, state.activeMode)
        assertEquals(AIExternalService.GEMINI, state.externalService)
        assertEquals("gemini-2.5-flash", state.selectedModel)
        assertEquals("https://api.openai.com/v1", state.genericBaseUrl)
        assertFalse(state.hasSavedApiKey)
        assertTrue("Por defecto ninguna categoría de memoria se comparte", state.allowedMemoryCategories.isEmpty())
    }

    @Test
    fun testModeSwitching() {
        settingsManager.setActiveMode(AIActiveMode.SOLO_LOCAL)
        assertEquals(AIActiveMode.SOLO_LOCAL, settingsManager.settingsState.value.activeMode)

        settingsManager.setActiveMode(AIActiveMode.SOLO_EXTERNO)
        assertEquals(AIActiveMode.SOLO_EXTERNO, settingsManager.settingsState.value.activeMode)
    }

    @Test
    fun testServiceSwitchingUpdatesSuggestedModel() {
        // Inicial es Gemini con gemini-2.5-flash
        assertEquals("gemini-2.5-flash", settingsManager.settingsState.value.selectedModel)

        // Al cambiar a Genérico, debe sugerir gpt-4o-mini
        settingsManager.setExternalService(AIExternalService.GENERIC_CHAT_COMPLETIONS)
        assertEquals(AIExternalService.GENERIC_CHAT_COMPLETIONS, settingsManager.settingsState.value.externalService)
        assertEquals("gpt-4o-mini", settingsManager.settingsState.value.selectedModel)

        // Al regresar a Gemini, debe sugerir gemini-2.5-flash
        settingsManager.setExternalService(AIExternalService.GEMINI)
        assertEquals("gemini-2.5-flash", settingsManager.settingsState.value.selectedModel)
    }

    @Test
    fun testBaseUrlValidation() {
        // HTTPS válido
        assertTrue(settingsManager.validateBaseUrl("https://api.groq.com/openai/v1"))
        assertTrue(settingsManager.setGenericBaseUrl("https://api.groq.com/openai/v1"))
        assertEquals("https://api.groq.com/openai/v1", settingsManager.settingsState.value.genericBaseUrl)

        // Localhost permitido para pruebas locales
        assertTrue(settingsManager.validateBaseUrl("http://localhost:11434/v1"))
        assertTrue(settingsManager.validateBaseUrl("http://127.0.0.1:8000/v1"))

        // HTTP inseguro no local debe fallar
        assertFalse(settingsManager.validateBaseUrl("http://api.insecure.com/v1"))
        assertFalse(settingsManager.setGenericBaseUrl("http://api.insecure.com/v1"))

        // Cadena vacía
        assertFalse(settingsManager.validateBaseUrl(""))
    }

    @Test
    fun testMemoryCategoryToggling() {
        assertFalse(settingsManager.isCategoryAllowed(MemoryCategory.PREFERENCES))

        settingsManager.toggleMemoryCategory(MemoryCategory.PREFERENCES, true)
        assertTrue(settingsManager.isCategoryAllowed(MemoryCategory.PREFERENCES))
        assertTrue(settingsManager.settingsState.value.allowedMemoryCategories.contains(MemoryCategory.PREFERENCES))

        // Desactivar
        settingsManager.toggleMemoryCategory(MemoryCategory.PREFERENCES, false)
        assertFalse(settingsManager.isCategoryAllowed(MemoryCategory.PREFERENCES))
    }

    @Test
    fun testApiKeyPersistenceIntegration() {
        val testKey = "test_key_abc_123"
        val saved = settingsManager.saveApiKey(testKey)
        assertTrue(saved)
        assertTrue(settingsManager.hasApiKey())
        assertTrue(settingsManager.settingsState.value.hasSavedApiKey)
        assertEquals(testKey, settingsManager.getApiKey())

        val deleted = settingsManager.deleteApiKey()
        assertTrue(deleted)
        assertFalse(settingsManager.hasApiKey())
        assertFalse(settingsManager.settingsState.value.hasSavedApiKey)
    }
}
