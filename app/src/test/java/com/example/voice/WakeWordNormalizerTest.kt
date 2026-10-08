package com.example.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordNormalizerTest {

    @Test
    fun testComandoSinPalabraClave() {
        val input = "abre YouTube"
        val normalized = WakeWordNormalizer.normalize(input)
        assertEquals("abre YouTube", normalized)
        assertFalse(WakeWordNormalizer.startsWithWakeWord(input))
    }

    @Test
    fun testViernesConComaAbreYouTube() {
        val input = "viernes, abre YouTube"
        val normalized = WakeWordNormalizer.normalize(input)
        assertEquals("abre YouTube", normalized)
        assertTrue(WakeWordNormalizer.startsWithWakeWord(input))
    }

    @Test
    fun testOyeViernes() {
        val input = "oye viernes"
        val normalized = WakeWordNormalizer.normalize(input)
        assertEquals("", normalized)
        assertTrue(WakeWordNormalizer.startsWithWakeWord(input))
    }

    @Test
    fun testSoloViernes() {
        val input = "viernes"
        val normalized = WakeWordNormalizer.normalize(input)
        assertEquals("", normalized)
        assertTrue(WakeWordNormalizer.startsWithWakeWord(input))
    }

    @Test
    fun testRecordatorioConViernesEnMedioNoDebeFiltrar() {
        val input = "recuérdame el viernes pagar la luz"
        val normalized = WakeWordNormalizer.normalize(input)
        // La palabra "viernes" forma parte de la fecha/semántica del recordatorio,
        // no es una invocación al inicio de la frase.
        assertEquals("recuérdame el viernes pagar la luz", normalized)
        assertFalse(WakeWordNormalizer.startsWithWakeWord(input))
    }

    @Test
    fun testViernesPagueLaLuzNoDebePerderPalabra() {
        val input = "viernes pagué la luz"
        // Sin coma ni delimitador de invocación, "viernes" no debe eliminarse por defecto
        val normalized = WakeWordNormalizer.normalize(input)
        assertEquals("viernes pagué la luz", normalized)
        assertFalse(WakeWordNormalizer.startsWithWakeWord(input))

        // Si provino de una detección acústica real del motor de palabra clave, sí se extrae la orden
        val fromDetection = WakeWordNormalizer.normalize(input, fromRealWakeWordDetection = true)
        assertEquals("pagué la luz", fromDetection)
        assertTrue(WakeWordNormalizer.startsWithWakeWord(input, fromRealWakeWordDetection = true))
    }

    @Test
    fun testAuraWakeWord() {
        assertEquals("enciende el foco", WakeWordNormalizer.normalize("oye aura, enciende el foco"))
        assertEquals("enciende el foco", WakeWordNormalizer.normalize("aura: enciende el foco"))
        assertEquals("", WakeWordNormalizer.normalize("aura"))
        assertTrue(WakeWordNormalizer.startsWithWakeWord("aura"))
        assertTrue(WakeWordNormalizer.startsWithWakeWord("oye aura"))
    }

    @Test
    fun testVariantesDeInvocacionConCaseInsensitive() {
        assertEquals("pon música", WakeWordNormalizer.normalize("VIERNES, pon música"))
        assertEquals("cómo estás", WakeWordNormalizer.normalize("Oye Viernes, cómo estás"))
        assertEquals("silencio", WakeWordNormalizer.normalize("hola viernes: silencio"))
        assertEquals("apaga la luz", WakeWordNormalizer.normalize("hey viernes apaga la luz"))
        assertEquals("siguiente canción", WakeWordNormalizer.normalize("ok viernes siguiente canción"))
    }
}
