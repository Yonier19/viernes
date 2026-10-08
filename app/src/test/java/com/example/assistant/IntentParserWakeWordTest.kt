package com.example.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentParserWakeWordTest {

    private val parser = IntentParser()

    @Test
    fun testAbreYouTube() {
        val parsed = parser.parse("abre YouTube")
        assertEquals(IntentType.OPEN_APP, parsed.intent)
        assertEquals("youtube", parsed.appName?.lowercase())
    }

    @Test
    fun testViernesAbreYouTube() {
        val parsed = parser.parse("viernes, abre YouTube")
        assertEquals(IntentType.OPEN_APP, parsed.intent)
        assertEquals("youtube", parsed.appName?.lowercase())
    }

    @Test
    fun testOyeViernes() {
        val parsed = parser.parse("oye viernes")
        // Invoca saludo / ayuda del asistente
        assertEquals(IntentType.HELP, parsed.intent)
    }

    @Test
    fun testSoloViernes() {
        val parsed = parser.parse("viernes")
        assertEquals(IntentType.HELP, parsed.intent)
    }

    @Test
    fun testRecordatorioConViernesEnMedioConservaSemantica() {
        val parsed = parser.parse("recuérdame el viernes pagar la luz")
        assertEquals(IntentType.REMINDER, parsed.intent)
        // No debe haber eliminado "el viernes" del cuerpo del recordatorio
        assertTrue(
            parsed.reminderText?.contains("viernes", ignoreCase = true) == true
        )
    }
}
