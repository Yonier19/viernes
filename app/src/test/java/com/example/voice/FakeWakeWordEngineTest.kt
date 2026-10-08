package com.example.voice

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeWakeWordEngineTest {

    @Test
    fun testCicloDeVidaBasico() = runTest {
        val engine = FakeWakeWordEngine()
        assertEquals(EngineState.Idle, engine.state.value)

        engine.start()
        assertEquals(EngineState.Listening, engine.state.value)
        assertEquals(1, engine.startCount)

        engine.pause()
        assertEquals(EngineState.Paused, engine.state.value)
        assertEquals(1, engine.pauseCount)

        engine.resume()
        assertEquals(EngineState.Listening, engine.state.value)
        assertEquals(1, engine.resumeCount)

        engine.stop(StopReason.AppBackgrounded)
        assertEquals(EngineState.Idle, engine.state.value)
        assertEquals(1, engine.stopCount)
        assertEquals(StopReason.AppBackgrounded, engine.lastStopReason)

        engine.release()
        assertEquals(1, engine.releaseCount)
    }

    @Test
    fun testSetSensitivity() {
        val engine = FakeWakeWordEngine()
        engine.setSensitivity("viernes", 0.75f)
        assertEquals(0.75f, engine.getSensitivity("viernes"), 0.001f)
        engine.setSensitivity("oye viernes", 1.5f) // coerceIn
        assertEquals(1.0f, engine.getSensitivity("oye viernes"), 0.001f)
    }

    @Test
    fun testDeteccionDePalabraClaveEmiteEvento() = runTest {
        val engine = FakeWakeWordEngine()
        val detectedEvents = mutableListOf<WakeWordEvent.Detected>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            engine.events.collect { event ->
                if (event is WakeWordEvent.Detected) {
                    detectedEvents.add(event)
                }
            }
        }

        engine.start()
        engine.simulateDetection("viernes", 0.94f)

        assertEquals(1, detectedEvents.size)
        assertEquals("viernes", detectedEvents[0].phrase)
        assertEquals(0.94f, detectedEvents[0].confidence, 0.001f)

        job.cancel()
    }

    @Test
    fun testReglaDePrivacidadEnWakeWordErrorNoExponeDetallesTecnicosEnUserMessage() {
        // Validación de la regla crítica: Los campos 'detail' y 'message' solo se registran en Log
        // y nunca deben contaminar el mensaje que lee el usuario en la UI.
        val errorLicencia = WakeWordError.LicenseOrKeyInvalid(
            detail = "HTTP 401: Porcupine AccessKey expired or device limit reached"
        )
        // El userMessage NO debe contener "HTTP 401", ni "AccessKey", ni "Porcupine"
        assertEquals("La activación por voz requiere conexión para verificar la licencia.", errorLicencia.userMessage)
        assertEquals("HTTP 401: Porcupine AccessKey expired or device limit reached", errorLicencia.detail)

        val errorNativo = WakeWordError.NativeEngineError(
            message = "SIGSEGV in libpv_porcupine.so at 0x7f83a"
        )
        assertEquals("Ocurrió un error en el motor de detección por voz.", errorNativo.userMessage)
        assertEquals("SIGSEGV in libpv_porcupine.so at 0x7f83a", errorNativo.message)
    }
}
