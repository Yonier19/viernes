package com.example.voice

/**
 * Modos de escucha para VIERNES en el dispositivo.
 */
enum class ListeningMode(
    val title: String,
    val description: String
) {
    /**
     * Modo manual por pulsación (Push-to-Talk).
     * El micrófono solo se activa cuando el usuario pulsa el botón del orbe o muelle.
     * Consumo de batería: 0% en reposo.
     */
    MANUAL(
        title = "Manual",
        description = "Toca el botón o el orbe para hablar. Cero consumo en reposo."
    ),

    /**
     * Modo palabra clave con aplicación visible.
     * Escucha continua de \"viernes\" únicamente mientras la app está abierta en pantalla.
     * Se pausa automáticamente al pasar a segundo plano.
     */
    FRASE_CLAVE(
        title = "Palabra Clave",
        description = "Di \"viernes\" mientras la aplicación esté en pantalla."
    ),

    /**
     * Modo residente en segundo plano.
     * Escucha mediante servicio en primer plano optimizado con VAD y gestión de energía.
     */
    RESIDENTE(
        title = "Residente",
        description = "Reconoce \"viernes\" incluso con la pantalla apagada o en otras apps."
    ),

    /**
     * Modo ahorro de energía.
     * Micrófono desactivado o limitado a interacciones manuales sin procesos de fondo.
     */
    AHORRO(
        title = "Ahorro",
        description = "Ahorro de batería estricto. Escucha continua desactivada."
    )
}
