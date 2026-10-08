package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognizerManager(
    private val context: Context,
    private val onResultRecognized: (String) -> Unit,
    private val onErrorOccurred: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onWakeWordDetected: ((String) -> Unit)? = null
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    var isWakeWordMode: Boolean = false
        private set

    init {
        _isAvailable.value = SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(isWakeWordDetection: Boolean = false) {
        if (!_isAvailable.value) {
            onErrorOccurred("El reconocimiento de voz no está disponible en este dispositivo.")
            return
        }

        isWakeWordMode = isWakeWordDetection

        try {
            destroy()
            // Uso de reconocimiento local en el dispositivo si la versión es Android 12+ (API 31)
            // de acuerdo con la documentación oficial: https://developer.android.com/reference/android/speech/SpeechRecognizer#createOnDeviceSpeechRecognizer(android.content.Context)
            speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }.apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            onListeningStateChanged(true)
        } catch (_: Exception) {
            onListeningStateChanged(false)
            onErrorOccurred("No se pudo iniciar el micrófono.")
        }
    }

    fun stopListening() {
        isWakeWordMode = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        onListeningStateChanged(false)
        _soundLevel.value = 0f
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _soundLevel.value = 0f
    }

    private fun restartWakeWordListeningAfterDelay() {
        if (!isWakeWordMode) return
        mainHandler.postDelayed({
            if (isWakeWordMode) {
                startListening(isWakeWordDetection = true)
            }
        }, 1200)
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningStateChanged(true)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = (rmsdB / 10f).coerceIn(0f, 1f)
                _soundLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                onListeningStateChanged(false)
                _soundLevel.value = 0f
            }

            override fun onError(error: Int) {
                onListeningStateChanged(false)
                _soundLevel.value = 0f

                if (isWakeWordMode) {
                    // In continuous wake-word mode, restart silently without annoying error alerts
                    restartWakeWordListeningAfterDelay()
                    return
                }

                val friendlyMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No alcancé a escucharte bien. Intenta de nuevo."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de escucha agotado."
                    SpeechRecognizer.ERROR_AUDIO -> "Error con el audio del dispositivo."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Se requiere permiso de micrófono para escuchar."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Problema de conexión en el reconocimiento de voz."
                    else -> "No pude procesar el audio."
                }
                onErrorOccurred(friendlyMessage)
            }

            override fun onResults(results: Bundle?) {
                onListeningStateChanged(false)
                _soundLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim()

                if (!recognizedText.isNullOrBlank()) {
                    val startsWithWake = WakeWordNormalizer.startsWithWakeWord(recognizedText)

                    if (startsWithWake) {
                        onWakeWordDetected?.invoke(recognizedText)
                        val commandText = WakeWordNormalizer.normalize(recognizedText)
                        if (commandText.isNotBlank()) {
                            onResultRecognized(commandText)
                        } else {
                            onResultRecognized("Viernes")
                        }
                    } else {
                        onResultRecognized(recognizedText)
                    }

                    if (isWakeWordMode) {
                        restartWakeWordListeningAfterDelay()
                    }
                } else {
                    if (isWakeWordMode) {
                        restartWakeWordListeningAfterDelay()
                    } else {
                        onErrorOccurred("No se detectó ninguna palabra.")
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: return
                if (WakeWordNormalizer.startsWithWakeWord(partial)) {
                    // Instant trigger on partial match for zero-latency response
                    onWakeWordDetected?.invoke(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
