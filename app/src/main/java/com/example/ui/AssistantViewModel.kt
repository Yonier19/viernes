package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assistant.AssistantEngine
import com.example.assistant.AssistantState
import com.example.assistant.ChatMessage
import com.example.assistant.CommandRouter
import com.example.assistant.ExecutionResult
import com.example.assistant.GeminiAIProvider
import com.example.assistant.IntentParser
import com.example.assistant.ResponseGenerator
import com.example.automation.AppLauncher
import com.example.automation.ViernesAccessibilityService
import com.example.memory.CommandHistoryEntity
import com.example.memory.ContactAliasEntity
import com.example.memory.LearningSuggestion
import com.example.memory.MemoryDatabase
import com.example.memory.MemoryEntity
import com.example.memory.MemoryRepository
import com.example.memory.MemoryType
import com.example.memory.RoutineEntity
import com.example.security.PermissionItem
import com.example.security.PermissionManager
import com.example.service.ViernesResidentService
import com.example.utils.AppUtils
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AuraNavScreen {
    HOME,
    MEMORY,
    PERMISSIONS,
    SETTINGS
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Core Dependencies
    private val db = MemoryDatabase.getDatabase(context)
    val memoryRepository = MemoryRepository(db)
    private val appLauncher = AppLauncher(context)
    private val responseGenerator = ResponseGenerator()
    private val intentParser = IntentParser()
    private val commandRouter = CommandRouter(context, appLauncher, memoryRepository, responseGenerator)
    val aiSettingsManager = com.example.assistant.AISettingsManager(context)
    val routingAIProvider = com.example.assistant.DynamicRoutingAIProvider(aiSettingsManager)
    private val assistantEngine = AssistantEngine(intentParser, commandRouter, memoryRepository, routingAIProvider)
    val permissionManager = PermissionManager(context)

    val aiSettings: StateFlow<com.example.assistant.AISettingsState> = aiSettingsManager.settingsState

    // Voice Managers
    val fakeWakeWordEngine = com.example.voice.FakeWakeWordEngine()
    lateinit var listeningController: com.example.voice.ListeningController
        private set

    var textToSpeechManager: TextToSpeechManager = TextToSpeechManager(context) {
        if (_assistantState.value == AssistantState.RESPONDIENDO) {
            _assistantState.value = AssistantState.INACTIVO
        }
        if (::listeningController.isInitialized) {
            listeningController.onTtsFinished()
        }
    }

    private var speechRecognizerManager: SpeechRecognizerManager? = null

    // UI States
    private val _currentScreen = MutableStateFlow(AuraNavScreen.HOME)
    val currentScreen: StateFlow<AuraNavScreen> = _currentScreen.asStateFlow()

    private val _assistantState = MutableStateFlow(AssistantState.INACTIVO)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _isTtsMuted = MutableStateFlow(false)
    val isTtsMuted: StateFlow<Boolean> = _isTtsMuted.asStateFlow()

    private val _isWakeWordContinuousEnabled = MutableStateFlow(false)
    val isWakeWordContinuousEnabled: StateFlow<Boolean> = _isWakeWordContinuousEnabled.asStateFlow()

    val isResidentServiceRunning: StateFlow<Boolean> = ViernesResidentService.isServiceRunning
    val isOverlayActive: StateFlow<Boolean> = ViernesResidentService.isOverlayActive
    val isAccessibilityConnected: StateFlow<Boolean> = ViernesAccessibilityService.isServiceConnected

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hola, soy VIERNES, tu asistente inteligente para tu Redmi Note 13. ¿Qué deseas hacer hoy? Puedes decir por ejemplo: \"Abrir WhatsApp\" o \"Pon música\".",
                isFromUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _permissionsState = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionsState: StateFlow<List<PermissionItem>> = _permissionsState.asStateFlow()

    private val _habitSuggestions = MutableStateFlow<List<LearningSuggestion>>(emptyList())
    val habitSuggestions: StateFlow<List<LearningSuggestion>> = _habitSuggestions.asStateFlow()

    // Room reactive streams
    val activeMemories: StateFlow<List<MemoryEntity>> = memoryRepository.activeMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactAliases: StateFlow<List<ContactAliasEntity>> = memoryRepository.contactAliases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineEntity>> = memoryRepository.routines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandHistory: StateFlow<List<CommandHistoryEntity>> = memoryRepository.commandHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        initSpeechRecognizer()
        refreshPermissions()
        refreshHabitSuggestions()
    }

    private fun initSpeechRecognizer() {
        val recognizer = SpeechRecognizerManager(
            context = context,
            onResultRecognized = { recognizedText ->
                _assistantState.value = AssistantState.PROCESANDO
                executeCommand(recognizedText)
            },
            onErrorOccurred = { errorMsg ->
                _assistantState.value = AssistantState.ERROR
                addAssistantMessage(errorMsg, isError = true)
            },
            onListeningStateChanged = { isListening ->
                _assistantState.value = if (isListening) AssistantState.ESCUCHANDO else AssistantState.INACTIVO
            },
            onWakeWordDetected = { _ ->
                AppUtils.vibrateShort(context)
                _assistantState.value = AssistantState.ESCUCHANDO
            }
        )
        speechRecognizerManager = recognizer

        listeningController = com.example.voice.ListeningController(
            context = context,
            wakeWordEngine = fakeWakeWordEngine,
            speechRecognizerManager = recognizer,
            onWakeWordTriggered = { phrase, _ ->
                AppUtils.vibrateShort(context)
                _assistantState.value = AssistantState.ESCUCHANDO
            },
            onCommandRecognized = { command ->
                _assistantState.value = AssistantState.PROCESANDO
                executeCommand(command)
            },
            onError = { errorMsg ->
                _assistantState.value = AssistantState.ERROR
                addAssistantMessage(errorMsg, isError = true)
            },
            onStateChanged = { isActiveListening ->
                _assistantState.value = if (isActiveListening) AssistantState.ESCUCHANDO else AssistantState.INACTIVO
            }
        )

        viewModelScope.launch {
            recognizer.soundLevel.collect { level ->
                _soundLevel.value = level
            }
        }
    }

    val currentListeningMode: StateFlow<com.example.voice.ListeningMode>
        get() = listeningController.currentMode

    fun setListeningMode(mode: com.example.voice.ListeningMode) {
        if (!permissionManager.isRecordAudioGranted() && mode != com.example.voice.ListeningMode.AHORRO) {
            addAssistantMessage("Se requiere permiso de micrófono para cambiar al modo ${mode.title}.", isError = true)
            return
        }
        listeningController.setListeningMode(mode)
        addAssistantMessage("Modo de escucha cambiado a: ${mode.title}.")
    }

    fun startListening() {
        AppUtils.vibrateShort(context)
        if (!permissionManager.isRecordAudioGranted()) {
            _assistantState.value = AssistantState.ERROR
            addAssistantMessage("Necesito permiso de micrófono para escucharte. Por favor habilítalo en la sección de Permisos.", isError = true)
            return
        }
        textToSpeechManager.stop()
        listeningController.onManualMicPressed()
    }

    fun stopListening() {
        listeningController.onManualMicReleased()
        if (_assistantState.value == AssistantState.ESCUCHANDO) {
            _assistantState.value = AssistantState.INACTIVO
        }
    }

    fun toggleWakeWordContinuousListening() {
        val next = !_isWakeWordContinuousEnabled.value
        _isWakeWordContinuousEnabled.value = next
        if (next) {
            setListeningMode(com.example.voice.ListeningMode.FRASE_CLAVE)
        } else {
            setListeningMode(com.example.voice.ListeningMode.MANUAL)
        }
    }

    fun toggleResidentService() {
        if (isResidentServiceRunning.value) {
            ViernesResidentService.stop(context)
        } else {
            ViernesResidentService.start(context)
        }
    }

    fun executeCommand(rawCommand: String) {
        if (rawCommand.isBlank()) return

        val userMsg = ChatMessage(text = rawCommand, isFromUser = true)
        _messages.value = _messages.value + userMsg

        _assistantState.value = AssistantState.PROCESANDO

        viewModelScope.launch {
            val result = assistantEngine.processCommand(rawCommand)
            handleExecutionResult(result)
            refreshHabitSuggestions()
        }
    }

    private fun handleExecutionResult(result: ExecutionResult) {
        val assistantMsg = ChatMessage(
            text = result.responseSpeech,
            isFromUser = false,
            actionSummary = result.actionSummary,
            isError = !result.success,
            hasConfirmation = result.requiresConfirmation,
            confirmationTitle = result.confirmationTitle,
            disambiguationOptions = result.disambiguationOptions,
            provider = result.provider,
            latencyMs = result.latencyMs,
            toolUsed = result.toolUsed
        )
        _messages.value = _messages.value + assistantMsg

        if (!_isTtsMuted.value) {
            _assistantState.value = AssistantState.RESPONDIENDO
            listeningController.onTtsStarted()
            textToSpeechManager.speak(result.responseSpeech)
        } else {
            _assistantState.value = if (result.success) AssistantState.INACTIVO else AssistantState.ERROR
        }
    }

    fun confirmPendingAction() {
        viewModelScope.launch {
            val result = assistantEngine.confirmPendingAction()
            handleExecutionResult(result)
            refreshHabitSuggestions()
        }
    }

    fun cancelPendingAction() {
        assistantEngine.cancelPendingAction()
        addAssistantMessage("Acción cancelada.")
    }

    fun selectDisambiguation(contactName: String) {
        executeCommand("Escríbele a $contactName")
    }

    fun speakText(text: String) {
        _assistantState.value = AssistantState.RESPONDIENDO
        textToSpeechManager.speak(text)
    }

    fun toggleTtsMute() {
        val newMuted = !_isTtsMuted.value
        _isTtsMuted.value = newMuted
        textToSpeechManager.isTtsEnabled = !newMuted
        if (newMuted) {
            textToSpeechManager.stop()
        }
    }

    private fun addAssistantMessage(text: String, isError: Boolean = false) {
        val msg = ChatMessage(text = text, isFromUser = false, isError = isError)
        _messages.value = _messages.value + msg
        if (!_isTtsMuted.value && !isError) {
            textToSpeechManager.speak(text)
        }
    }

    fun navigateTo(screen: AuraNavScreen) {
        _currentScreen.value = screen
        if (screen == AuraNavScreen.MEMORY) {
            refreshHabitSuggestions()
        }
    }

    fun saveMemoryItem(key: String, value: String, type: MemoryType = MemoryType.PREFERENCE) {
        viewModelScope.launch {
            memoryRepository.saveMemory(key, value, type)
            addAssistantMessage(responseGenerator.forMemorySaved())
        }
    }

    fun updateMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            memoryRepository.updateMemory(memory)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(memory)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepository.clearAllMemories()
            addAssistantMessage("Toda la memoria local ha sido eliminada.")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            memoryRepository.clearHistory()
            _messages.value = listOf(
                ChatMessage(
                    text = "Historial limpiado. ¿En qué más puedo ayudarte?",
                    isFromUser = false
                )
            )
        }
    }

    fun refreshHabitSuggestions() {
        viewModelScope.launch {
            _habitSuggestions.value = memoryRepository.detectHabitPatterns()
        }
    }

    fun acceptHabitSuggestion(suggestion: LearningSuggestion) {
        viewModelScope.launch {
            memoryRepository.saveCommandAlias(suggestion.phrase, suggestion.target)
            _habitSuggestions.value = _habitSuggestions.value.filterNot { it.phrase == suggestion.phrase }
            addAssistantMessage(responseGenerator.forCommandAliasSaved(suggestion.phrase, suggestion.target))
        }
    }

    fun dismissHabitSuggestion(suggestion: LearningSuggestion) {
        _habitSuggestions.value = _habitSuggestions.value.filterNot { it.phrase == suggestion.phrase }
    }

    fun refreshPermissions() {
        _permissionsState.value = permissionManager.getPermissionsState()
    }

    // AI Settings & Secure Key management
    fun setAIActiveMode(mode: com.example.assistant.AIActiveMode) {
        aiSettingsManager.setActiveMode(mode)
    }

    fun setAIExternalService(service: com.example.assistant.AIExternalService) {
        aiSettingsManager.setExternalService(service)
    }

    fun setGenericBaseUrl(url: String): Boolean {
        return aiSettingsManager.setGenericBaseUrl(url)
    }

    fun setAIModel(model: String) {
        aiSettingsManager.setSelectedModel(model)
    }

    fun saveApiKey(apiKey: String): Boolean {
        return aiSettingsManager.saveApiKey(apiKey)
    }

    fun deleteApiKey(): Boolean {
        return aiSettingsManager.deleteApiKey()
    }

    fun toggleMemoryCategory(category: com.example.assistant.MemoryCategory, enabled: Boolean) {
        aiSettingsManager.toggleMemoryCategory(category, enabled)
    }

    fun clearConversationHistory() {
        routingAIProvider.conversationManager.clear()
        addAssistantMessage("Historial de conversación con la IA vaciado con éxito.")
    }

    fun testAIConnection() {
        viewModelScope.launch {
            aiSettingsManager.setTestingConnection(true)
            val result = routingAIProvider.testExternalConnection()
            aiSettingsManager.updateConnectionTestResult(result)
        }
    }

    fun clearConnectionTestResult() {
        aiSettingsManager.updateConnectionTestResult(null)
    }

    override fun onCleared() {
        super.onCleared()
        listeningController.destroy()
        textToSpeechManager.shutdown()
    }
}
