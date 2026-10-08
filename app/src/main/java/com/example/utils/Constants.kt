package com.example.utils

object Constants {
    const val DATABASE_NAME = "viernes_memory_database"
    const val PREFS_NAME = "viernes_settings_prefs"
    
    // Assistant identity
    const val ASSISTANT_NAME = "VIERNES"
    const val TARGET_DEVICE = "Xiaomi Redmi Note 13 4G (Snapdragon 685 • HyperOS 2)"

    // Wake words for resident continuous detection
    val WAKE_WORDS = listOf(
        "viernes",
        "oye viernes",
        "hola viernes",
        "hey viernes",
        "ok viernes"
    )

    // Resident Service & Notification IDs
    const val RESIDENT_CHANNEL_ID = "viernes_resident_channel"
    const val RESIDENT_NOTIFICATION_ID = 1001

    // Default package mapping for quick resolution
    val POPULAR_APPS = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "spotify" to "com.spotify.music",
        "chrome" to "com.android.chrome",
        "instagram" to "com.instagram.android",
        "telegram" to "org.telegram.messenger",
        "facebook" to "com.facebook.katana",
        "tiktok" to "com.zhiliaoapp.musically",
        "maps" to "com.google.android.apps.maps",
        "camara" to "com.android.camera",
        "cámara" to "com.android.camera",
        "ajustes" to "com.android.settings",
        "configuracion" to "com.android.settings",
        "configuración" to "com.android.settings",
        "reloj" to "com.google.android.deskclock",
        "calculadora" to "com.google.android.calculator",
        "galeria" to "com.miui.gallery",
        "galería" to "com.miui.gallery",
        "mensajes" to "com.google.android.apps.messaging",
        "gmail" to "com.google.android.gm"
    )

    // Category Aliases
    val CATEGORY_ALIASES = mapOf(
        "mensajería" to "whatsapp",
        "mensajeria" to "whatsapp",
        "chat" to "whatsapp",
        "música" to "spotify",
        "musica" to "spotify",
        "canciones" to "spotify",
        "navegador" to "chrome",
        "internet" to "chrome",
        "videos" to "youtube",
        "vídeos" to "youtube",
        "correo" to "gmail",
        "fotos" to "galeria"
    )
}
