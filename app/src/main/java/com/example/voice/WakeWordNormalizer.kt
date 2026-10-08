package com.example.voice

/**
 * Normalizador centralizado y unificado para la palabra clave ("viernes" y variantes).
 * Evita la duplicación de lógica en SpeechRecognizerManager, IntentParser o ViewModel.
 */
object WakeWordNormalizer {

    /**
     * Expresión regular para detectar invocaciones de la palabra clave ("viernes", "oye viernes", etc. y "aura").
     *
     * REGLAS ESTRICTAS DE NORMALIZACIÓN:
     * 1. Detecta al inicio de la frase prefijos ("oye", "hola", "hey", "ok") seguidos de "viernes" o "aura".
     * 2. Si es invocación compuesta (ej. "oye viernes", "hola viernes", "hey aura"), va seguida de [,: ] o fin de texto.
     * 3. Si es simplemente "viernes" o "aura" sin prefijo de llamada:
     *    ÚNICAMENTE se considera invocación si va seguido inmediatamente de coma (,), dos puntos (:),
     *    espacio seguido de coma/dos puntos, o fin de texto (ej. "viernes", "viernes, abre YouTube", "viernes: ayuda").
     *    Frases como "viernes pagué la luz" o "viernes salgo tarde" CONSERVAN la palabra intacta
     *    porque no son una invocación delimitada por pausa/puntuación o prefijo.
     */
    private val WAKE_WORD_PREFIX_REGEX = Regex(
        "(?i)^\\s*(?:(?:oye|hola|hey|ok)\\s+(?:viernes|aura)(?:[,:\\s]+|$)|(?:viernes|aura)(?:\\s*[,:]\\s*|$))"
    )

    /**
     * Normaliza el texto de entrada removiendo únicamente la invocación de la palabra clave
     * si se encuentra al principio de la frase bajo las reglas de puntuación o prefijo.
     *
     * @param input Entrada sin procesar (reconocimiento de voz o texto escrito).
     * @param fromRealWakeWordDetection Si es true, indica que un motor acústico de wake word
     *        ya confirmó la detección previa del wake word, permitiendo limpiar el wake word al inicio
     *        incluso si la transcripción automática no insertó comas.
     * @return Texto limpio de la invocación inicial, sin espacios sobrantes.
     */
    fun normalize(input: String, fromRealWakeWordDetection: Boolean = false): String {
        if (input.isBlank()) return ""
        val trimmed = input.trim()
        if (fromRealWakeWordDetection) {
            val relaxedRegex = Regex("(?i)^\\s*(?:(?:oye|hola|hey|ok)\\s+)?(?:viernes|aura)(?:[,:\\s]+|$)")
            return trimmed.replace(relaxedRegex, "").trim()
        }
        return trimmed.replace(WAKE_WORD_PREFIX_REGEX, "").trim()
    }

    /**
     * Determina si la entrada comienza con la invocación de la palabra clave.
     */
    fun startsWithWakeWord(input: String, fromRealWakeWordDetection: Boolean = false): Boolean {
        if (input.isBlank()) return false
        if (fromRealWakeWordDetection) {
            val relaxedRegex = Regex("(?i)^\\s*(?:(?:oye|hola|hey|ok)\\s+)?(?:viernes|aura)(?:[,:\\s]+|$)")
            return relaxedRegex.containsMatchIn(input)
        }
        return WAKE_WORD_PREFIX_REGEX.containsMatchIn(input)
    }

    /**
     * Extrae el comando útil tras la palabra clave. Si la orden consistía únicamente
     * en la llamada (ej: "viernes" o "oye viernes"), devuelve cadena vacía para que el
     * asistente salude o espere la instrucción.
     */
    fun extractCommand(input: String, fromRealWakeWordDetection: Boolean = false): String {
        return normalize(input, fromRealWakeWordDetection)
    }
}
