package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Controlador de escucha centralizado para VIERNES.
 *
 * Responsabilidades:
 * 1. Garantiza la exclusividad mutua del micrófono:
 *    El motor de palabra clave (WakeWordEngine) y el reconocedor de comandos (SpeechRecognizer)
 *    NUNCA compiten por el hardware de audio.
 * 2. Gestiona transiciones de modo (PULSACION, FRASE_CLAVE, RESIDENTE, DESACTIVADO).
 * 3. Pausa la escucha durante la locución de texto (TTS) para evitar que VIERNES se auto-invoque.
 * 4. Detecta llamadas entrantes y conflictos de micrófono mediante AudioFocus y AudioRecordingCallback.
 * 5. Pausa la escucha en modo FRASE_CLAVE al pasar la aplicación a segundo plano.
 */
class ListeningController(
    private val context: Context,
    private val wakeWordEngine: WakeWordEngine,
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val onWakeWordTriggered: (phrase: String, confidence: Float) -> Unit,
    private val onCommandRecognized: (command: String) -> Unit,
    private val onError: (message: String) -> Unit,
    private val onStateChanged: (isActiveListening: Boolean) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _currentMode = MutableStateFlow(ListeningMode.MANUAL)
    val currentMode: StateFlow<ListeningMode> = _currentMode.asStateFlow()

    private val _isAppForeground = MutableStateFlow(true)
    val isAppForeground: StateFlow<Boolean> = _isAppForeground.asStateFlow()

    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    private var audioRecordingCallback: AudioManager.AudioRecordingCallback? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d("ListeningController", "Audio focus lost. Pausing wake word detection.")
                pauseForExternalAudioConflict()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("ListeningController", "Audio focus regained.")
                resumeFromExternalAudioConflict()
            }
        }
    }

    init {
        observeWakeWordEvents()
        setupAudioRecordingCallback()
    }

    private fun observeWakeWordEvents() {
        scope.launch {
            wakeWordEngine.events.collect { event ->
                when (event) {
                    is WakeWordEvent.Detected -> {
                        handleWakeWordDetected(event.phrase, event.confidence)
                    }
                    is WakeWordEvent.Stopped -> {
                        Log.d("ListeningController", "WakeWordEngine stopped: ${event.reason}")
                    }
                    is WakeWordEvent.StateChanged -> {
                        Log.d("ListeningController", "WakeWordEngine state changed: ${event.newState}")
                    }
                    is WakeWordEvent.Failure -> {
                        onError(event.error.userMessage)
                    }
                }
            }
        }

        scope.launch {
            wakeWordEngine.state.collect { state ->
                when (state) {
                    is EngineState.Error -> {
                        // REGLA: Los campos detail/message de errores de licencia/nativos NO se muestran al usuario
                        onError(state.error.userMessage)
                    }
                    is EngineState.Listening -> {
                        onStateChanged(true)
                    }
                    is EngineState.Idle,
                    is EngineState.Paused -> {
                        onStateChanged(false)
                    }
                }
            }
        }
    }

    private fun setupAudioRecordingCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && audioManager != null) {
            val callback = object : AudioManager.AudioRecordingCallback() {
                override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>?) {
                    super.onRecordingConfigChanged(configs)
                    val otherRecordings = configs?.filter {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            it.clientAudioSessionId != 0 && it.clientAudioSource != 0
                        } else {
                            true
                        }
                    }
                    if (!otherRecordings.isNullOrEmpty() && otherRecordings.size > 1) {
                        Log.d("ListeningController", "Conflicto de grabación de audio detectado.")
                    }
                }
            }
            audioRecordingCallback = callback
            try {
                audioManager.registerAudioRecordingCallback(callback, mainHandler)
            } catch (e: Exception) {
                Log.w("ListeningController", "No se pudo registrar AudioRecordingCallback: ${e.message}")
            }
        }
    }

    /**
     * Cambia el modo de escucha activo.
     */
    fun setListeningMode(mode: ListeningMode) {
        if (_currentMode.value == mode) return
        _currentMode.value = mode

        scope.launch {
            when (mode) {
                ListeningMode.MANUAL,
                ListeningMode.AHORRO -> {
                    wakeWordEngine.stop(StopReason.UserRequested)
                    speechRecognizerManager.stopListening()
                }
                ListeningMode.FRASE_CLAVE -> {
                    if (_isAppForeground.value && !_isTtsSpeaking.value) {
                        startWakeWordIfAllowed()
                    } else {
                        wakeWordEngine.stop(StopReason.AppBackgrounded)
                    }
                }
                ListeningMode.RESIDENTE -> {
                    // Delegado al servicio residente para ejecución en segundo plano
                    wakeWordEngine.stop(StopReason.UserRequested)
                }
            }
        }
    }

    /**
     * Inicia la captura manual al pulsar el botón del micrófono (Push-to-Talk).
     */
    fun onManualMicPressed() {
        scope.launch {
            // Detener motor de palabra clave para liberar el hardware
            wakeWordEngine.stop(StopReason.UserRequested)
            speechRecognizerManager.startListening(isWakeWordDetection = false)
        }
    }

    /**
     * Detiene la captura manual.
     */
    fun onManualMicReleased() {
        speechRecognizerManager.stopListening()
        if (_currentMode.value == ListeningMode.FRASE_CLAVE && _isAppForeground.value && !_isTtsSpeaking.value) {
            startWakeWordIfAllowed()
        }
    }

    /**
     * Se invoca cuando el motor de palabra clave detecta "viernes" o una variante.
     */
    private fun handleWakeWordDetected(phrase: String, confidence: Float) {
        Log.d("ListeningController", "Palabra clave detectada: $phrase con confianza $confidence")
        scope.launch {
            // 1. Pausar el motor de palabra clave para que no consuma el micrófono
            wakeWordEngine.pause()

            // 2. Notificar a la UI para feedback háptico y visual
            onWakeWordTriggered(phrase, confidence)

            // 3. Iniciar el reconocedor de voz estándar para capturar la orden del usuario
            speechRecognizerManager.startListening(isWakeWordDetection = false)
        }
    }

    /**
     * Debe llamarse cuando el motor TTS de síntesis de voz comienza a hablar.
     * Evita que VIERNES se autoactive al pronunciar su propio nombre.
     */
    fun onTtsStarted() {
        _isTtsSpeaking.value = true
        scope.launch {
            wakeWordEngine.pause()
        }
        speechRecognizerManager.stopListening()
    }

    /**
     * Debe llamarse cuando el motor TTS finaliza su locución.
     */
    fun onTtsFinished() {
        _isTtsSpeaking.value = false
        if (_currentMode.value == ListeningMode.FRASE_CLAVE && _isAppForeground.value) {
            // Reanudar detección con pequeño margen de 300ms para asegurar liberación del buffer de audio
            mainHandler.postDelayed({
                if (!_isTtsSpeaking.value && _isAppForeground.value && _currentMode.value == ListeningMode.FRASE_CLAVE) {
                    scope.launch {
                        wakeWordEngine.resume()
                    }
                }
            }, 300)
        }
    }

    /**
     * Ciclo de vida: La app pasa a primer plano.
     */
    fun onAppForegrounded() {
        _isAppForeground.value = true
        if (_currentMode.value == ListeningMode.FRASE_CLAVE && !_isTtsSpeaking.value) {
            startWakeWordIfAllowed()
        }
    }

    /**
     * Ciclo de vida: La app pasa a segundo plano.
     */
    fun onAppBackgrounded() {
        _isAppForeground.value = false
        if (_currentMode.value == ListeningMode.FRASE_CLAVE) {
            scope.launch {
                wakeWordEngine.stop(StopReason.AppBackgrounded)
            }
        }
    }

    private fun startWakeWordIfAllowed() {
        speechRecognizerManager.stopListening()
        scope.launch {
            wakeWordEngine.start()
        }
    }

    private fun pauseForExternalAudioConflict() {
        scope.launch {
            wakeWordEngine.pause()
        }
        speechRecognizerManager.stopListening()
    }

    private fun resumeFromExternalAudioConflict() {
        if (_currentMode.value == ListeningMode.FRASE_CLAVE && _isAppForeground.value && !_isTtsSpeaking.value) {
            scope.launch {
                wakeWordEngine.resume()
            }
        }
    }

    /**
     * Libera todos los observadores y callbacks de audio.
     */
    fun destroy() {
        scope.launch {
            wakeWordEngine.release()
        }
        speechRecognizerManager.destroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && audioRecordingCallback != null && audioManager != null) {
            try {
                audioManager.unregisterAudioRecordingCallback(audioRecordingCallback!!)
            } catch (_: Exception) {}
        }
    }
}
