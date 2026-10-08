package com.example.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isRequiredForVoice: Boolean = false
)

class PermissionManager(private val context: Context) {

    fun getPermissionsState(): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone
        list.add(
            PermissionItem(
                permission = Manifest.permission.RECORD_AUDIO,
                title = "Micrófono",
                description = "Permite a VIERNES escuchar tus órdenes por voz y activar comandos sin teclear.",
                isGranted = isGranted(Manifest.permission.RECORD_AUDIO),
                isRequiredForVoice = true
            )
        )

        // 2. Contacts
        list.add(
            PermissionItem(
                permission = Manifest.permission.READ_CONTACTS,
                title = "Contactos",
                description = "Permite a VIERNES identificar a quién deseas enviar mensajes (ej. 'Escríbele a Juan').",
                isGranted = isGranted(Manifest.permission.READ_CONTACTS)
            )
        )

        // 3. SMS
        list.add(
            PermissionItem(
                permission = Manifest.permission.SEND_SMS,
                title = "Mensajes SMS",
                description = "Permite enviar mensajes de texto directamente con tu confirmación previa.",
                isGranted = isGranted(Manifest.permission.SEND_SMS)
            )
        )

        // 4. Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItem(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    title = "Notificaciones",
                    description = "Muestra alertas para recordatorios y avisos del asistente en segundo plano.",
                    isGranted = isGranted(Manifest.permission.POST_NOTIFICATIONS)
                )
            )
        }

        return list
    }

    fun isRecordAudioGranted(): Boolean {
        return isGranted(Manifest.permission.RECORD_AUDIO)
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
