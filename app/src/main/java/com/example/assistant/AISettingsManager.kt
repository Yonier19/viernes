package com.example.assistant

import android.content.Context
import com.example.security.SecureKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI

class AISettingsManager(
    private val context: Context,
    private val secureKeyStore: SecureKeyStore = SecureKeyStore(context)
) {

    companion object {
        private const val PREFS_NAME = "viernes_ai_config"
        private const val KEY_ACTIVE_MODE = "ai_active_mode"
        private const val KEY_EXTERNAL_SERVICE = "ai_external_service"
        private const val KEY_GENERIC_BASE_URL = "ai_generic_base_url"
        private const val KEY_SELECTED_MODEL = "ai_selected_model"
        private const val KEY_ALLOWED_CATEGORIES = "ai_allowed_memory_categories"

        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        const val DEFAULT_GENERIC_MODEL = "gpt-4o-mini"
        const val DEFAULT_GENERIC_BASE_URL = "https://api.openai.com/v1"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadInitialState())
    val settingsState: StateFlow<AISettingsState> = _settingsState.asStateFlow()

    private fun loadInitialState(): AISettingsState {
        val modeStr = prefs.getString(KEY_ACTIVE_MODE, AIActiveMode.AUTO.name) ?: AIActiveMode.AUTO.name
        val activeMode = try { AIActiveMode.valueOf(modeStr) } catch (_: Exception) { AIActiveMode.AUTO }

        val serviceStr = prefs.getString(KEY_EXTERNAL_SERVICE, AIExternalService.GEMINI.name) ?: AIExternalService.GEMINI.name
        val externalService = try { AIExternalService.valueOf(serviceStr) } catch (_: Exception) { AIExternalService.GEMINI }

        val genericBaseUrl = prefs.getString(KEY_GENERIC_BASE_URL, DEFAULT_GENERIC_BASE_URL) ?: DEFAULT_GENERIC_BASE_URL
        val defaultModel = if (externalService == AIExternalService.GEMINI) DEFAULT_GEMINI_MODEL else DEFAULT_GENERIC_MODEL
        val selectedModel = prefs.getString(KEY_SELECTED_MODEL, defaultModel) ?: defaultModel

        val savedCategories = prefs.getStringSet(KEY_ALLOWED_CATEGORIES, emptySet()) ?: emptySet()
        val allowedCategories = savedCategories.mapNotNull { id ->
            MemoryCategory.entries.find { it.id == id }
        }.toSet()

        val hasKey = secureKeyStore.hasKey()

        return AISettingsState(
            activeMode = activeMode,
            externalService = externalService,
            genericBaseUrl = genericBaseUrl,
            selectedModel = selectedModel,
            hasSavedApiKey = hasKey,
            allowedMemoryCategories = allowedCategories
        )
    }

    fun setActiveMode(mode: AIActiveMode) {
        prefs.edit().putString(KEY_ACTIVE_MODE, mode.name).apply()
        _settingsState.value = _settingsState.value.copy(activeMode = mode)
    }

    fun setExternalService(service: AIExternalService) {
        val currentModel = _settingsState.value.selectedModel
        // Si el modelo actual era el por defecto del otro servicio, sugerir el nuevo
        val newModel = if (service == AIExternalService.GEMINI && currentModel == DEFAULT_GENERIC_MODEL) {
            DEFAULT_GEMINI_MODEL
        } else if (service == AIExternalService.GENERIC_CHAT_COMPLETIONS && currentModel == DEFAULT_GEMINI_MODEL) {
            DEFAULT_GENERIC_MODEL
        } else {
            currentModel
        }

        prefs.edit()
            .putString(KEY_EXTERNAL_SERVICE, service.name)
            .putString(KEY_SELECTED_MODEL, newModel)
            .apply()

        _settingsState.value = _settingsState.value.copy(
            externalService = service,
            selectedModel = newModel
        )
    }

    fun validateBaseUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.lowercase() ?: ""
            if (scheme == "https") {
                true
            } else if (scheme == "http" && (host == "localhost" || host == "127.0.0.1" || host.startsWith("192.168."))) {
                // Permitido para pruebas y depuración local
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun setGenericBaseUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!validateBaseUrl(trimmed)) return false
        prefs.edit().putString(KEY_GENERIC_BASE_URL, trimmed).apply()
        _settingsState.value = _settingsState.value.copy(genericBaseUrl = trimmed)
        return true
    }

    fun setSelectedModel(model: String) {
        val trimmed = model.trim()
        if (trimmed.isBlank()) return
        prefs.edit().putString(KEY_SELECTED_MODEL, trimmed).apply()
        _settingsState.value = _settingsState.value.copy(selectedModel = trimmed)
    }

    fun saveApiKey(apiKey: String): Boolean {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) return false
        val success = secureKeyStore.save(trimmed)
        if (success) {
            _settingsState.value = _settingsState.value.copy(hasSavedApiKey = true)
        }
        return success
    }

    fun deleteApiKey(): Boolean {
        val success = secureKeyStore.delete()
        if (success) {
            _settingsState.value = _settingsState.value.copy(hasSavedApiKey = false)
        }
        return success
    }

    fun getApiKey(): String? {
        return secureKeyStore.read()
    }

    fun hasApiKey(): Boolean {
        return secureKeyStore.hasKey()
    }

    fun toggleMemoryCategory(category: MemoryCategory, enabled: Boolean) {
        val current = _settingsState.value.allowedMemoryCategories.toMutableSet()
        if (enabled) {
            current.add(category)
        } else {
            current.remove(category)
        }
        val idSet = current.map { it.id }.toSet()
        prefs.edit().putStringSet(KEY_ALLOWED_CATEGORIES, idSet).apply()
        _settingsState.value = _settingsState.value.copy(allowedMemoryCategories = current)
    }

    fun isCategoryAllowed(category: MemoryCategory): Boolean {
        return _settingsState.value.allowedMemoryCategories.contains(category)
    }

    fun updateConnectionTestResult(result: ConnectionTestResult?) {
        _settingsState.value = _settingsState.value.copy(
            connectionTestResult = result,
            isTestingConnection = false
        )
    }

    fun setTestingConnection(testing: Boolean) {
        _settingsState.value = _settingsState.value.copy(isTestingConnection = testing)
    }
}
