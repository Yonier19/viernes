package com.example.voice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación simulada (Fake) de [WakeWordEngine] para pruebas unitarias,
 * bancos de evaluación y previsualizaciones sin depender de librerías nativas de audio.
 */
class FakeWakeWordEngine : WakeWordEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Idle)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<WakeWordEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<WakeWordEvent> = _events.asSharedFlow()

    private val sensitivities = mutableMapOf<String, Float>()

    var startCount: Int = 0
        private set
    var stopCount: Int = 0
        private set
    var pauseCount: Int = 0
        private set
    var resumeCount: Int = 0
        private set
    var releaseCount: Int = 0
        private set
    var lastStopReason: StopReason? = null
        private set

    override suspend fun start() {
        startCount++
        _state.value = EngineState.Listening
        _events.tryEmit(WakeWordEvent.StateChanged(EngineState.Listening))
    }

    override suspend fun pause() {
        pauseCount++
        if (_state.value is EngineState.Listening) {
            _state.value = EngineState.Paused
            _events.tryEmit(WakeWordEvent.StateChanged(EngineState.Paused))
        }
    }

    override suspend fun resume() {
        resumeCount++
        if (_state.value is EngineState.Paused) {
            _state.value = EngineState.Listening
            _events.tryEmit(WakeWordEvent.StateChanged(EngineState.Listening))
        }
    }

    override suspend fun stop(reason: StopReason) {
        stopCount++
        lastStopReason = reason
        _state.value = EngineState.Idle
        _events.tryEmit(WakeWordEvent.Stopped(reason))
        _events.tryEmit(WakeWordEvent.StateChanged(EngineState.Idle))
    }

    override suspend fun release() {
        releaseCount++
        _state.value = EngineState.Idle
        _events.tryEmit(WakeWordEvent.StateChanged(EngineState.Idle))
    }

    override fun setSensitivity(phrase: String, value: Float) {
        sensitivities[phrase] = value.coerceIn(0.0f, 1.0f)
    }

    fun getSensitivity(phrase: String): Float = sensitivities[phrase] ?: 0.5f

    /**
     * Simula la detección de una frase ("viernes", "oye viernes") con la confianza dada.
     */
    fun simulateDetection(phrase: String = "viernes", confidence: Float = 0.95f) {
        if (_state.value is EngineState.Listening) {
            _events.tryEmit(
                WakeWordEvent.Detected(
                    phrase = phrase,
                    confidence = confidence,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Simula un fallo de hardware o licencia.
     */
    fun simulateError(error: WakeWordError) {
        _state.value = EngineState.Error(error)
        _events.tryEmit(WakeWordEvent.Failure(error))
        _events.tryEmit(WakeWordEvent.StateChanged(EngineState.Error(error)))
    }

    /**
     * Reinicia contadores de prueba.
     */
    fun resetCounters() {
        startCount = 0
        stopCount = 0
        pauseCount = 0
        resumeCount = 0
        releaseCount = 0
        lastStopReason = null
        sensitivities.clear()
        _state.value = EngineState.Idle
    }
}
