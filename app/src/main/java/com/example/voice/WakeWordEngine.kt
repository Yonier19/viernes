package com.example.voice

import android.util.Log
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Estados del ciclo de vida del motor de detección de palabra clave ("wake word").
 */
sealed class EngineState {
    object Idle : EngineState()
    object Listening : EngineState()
    object Paused : EngineState()
    data class Error(val error: WakeWordError) : EngineState()
}

/**
 * Eventos emitidos por el motor de detección de palabra clave.
 */
sealed class WakeWordEvent {
    data class Detected(
        val phrase: String,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis()
    ) : WakeWordEvent()

    data class StateChanged(
        val newState: EngineState
    ) : WakeWordEvent()

    data class Stopped(
        val reason: StopReason
    ) : WakeWordEvent()

    data class Failure(
        val error: WakeWordError
    ) : WakeWordEvent()
}

/**
 * Motivos de detención del motor de detección.
 */
sealed class StopReason {
    object UserRequested : StopReason()
    object MicrophoneLost : StopReason()
    object AudioRecordingConflict : StopReason()
    object AppBackgrounded : StopReason()
    object PowerSaveActive : StopReason()
    data class Other(val description: String) : StopReason()
}

/**
 * Errores fuertemente tipados para el motor de detección.
 *
 * REGLA CRÍTICA DE SEGURIDAD / PRIVACIDAD:
 * Los campos 'detail' y 'message' de [LicenseOrKeyInvalid] y [NativeEngineError]
 * contienen trazas técnicas o de licencia que ÚNICAMENTE se registran internamente
 * en android.util.Log (Logcat). NUNCA se exponen al usuario en la UI ni en cadenas legibles.
 */
sealed class WakeWordError(open val userMessage: String) {
    data class MicrophoneInUse(
        override val userMessage: String = "El micrófono está siendo usado por otra aplicación."
    ) : WakeWordError(userMessage)

    data class PermissionMissing(
        override val userMessage: String = "Se requiere el permiso de grabación de audio para escuchar."
    ) : WakeWordError(userMessage)

    data class HardwareUnavailable(
        override val userMessage: String = "El micrófono del dispositivo no está disponible en este momento."
    ) : WakeWordError(userMessage)

    data class LicenseOrKeyInvalid(
        override val userMessage: String = "La activación por voz requiere conexión para verificar la licencia.",
        val detail: String? = null
    ) : WakeWordError(userMessage) {
        init {
            if (detail != null) {
                // Registrar exclusivamente en Logcat, nunca propagar a la interfaz
                try {
                    Log.e("WakeWordEngine", "License/Key validation issue: $detail")
                } catch (_: Throwable) {
                    // Ignorado en pruebas JVM puras sin entorno Android
                }
            }
        }
    }

    data class NativeEngineError(
        override val userMessage: String = "Ocurrió un error en el motor de detección por voz.",
        val message: String? = null
    ) : WakeWordError(userMessage) {
        init {
            if (message != null) {
                // Registrar exclusivamente en Logcat, nunca propagar a la interfaz
                try {
                    Log.e("WakeWordEngine", "Native engine error: $message")
                } catch (_: Throwable) {
                    // Ignorado en pruebas JVM puras sin entorno Android
                }
            }
        }
    }
}

/**
 * Contrato formal para motores de detección de palabra clave ("wake word").
 * Métodos del ciclo de vida suspendibles para sincronización no bloqueante.
 * Completamente aislado de UI, TTS o persistencia Room.
 */
interface WakeWordEngine {
    val state: StateFlow<EngineState>
    val events: SharedFlow<WakeWordEvent>

    /**
     * Inicia el flujo de captura de audio y análisis de la palabra clave de forma asíncrona.
     */
    suspend fun start()

    /**
     * Pausa temporalmente la detección (sin destruir buffers ni estructuras nativas).
     */
    suspend fun pause()

    /**
     * Reanuda la detección tras haber estado en pausa.
     */
    suspend fun resume()

    /**
     * Detiene el motor y libera el hardware de captura de audio indicando el motivo.
     */
    suspend fun stop(reason: StopReason = StopReason.UserRequested)

    /**
     * Libera de forma definitiva todos los recursos nativos y de audio.
     */
    suspend fun release()

    /**
     * Configura la sensibilidad del motor para una frase determinada (de 0.0f a 1.0f).
     */
    fun setSensitivity(phrase: String, value: Float)
}
