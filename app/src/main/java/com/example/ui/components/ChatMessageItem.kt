package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.ChatMessage
import com.example.ui.theme.AuraCardBorder
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraNeonPink
import com.example.ui.theme.AuraSurfaceDark
import com.example.ui.theme.AuraSurfaceLightDark
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraViolet
import com.example.utils.AppUtils

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onSpeakClick: (String) -> Unit = {},
    onConfirmClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onDisambiguateClick: (String) -> Unit = {}
) {
    val annotatedText = remember(message.text) {
        if (!message.isFromUser) formatAssistantText(message.text) else null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "streaming_cursor_trans")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 3 }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(if (message.isFromUser) 0.85f else 0.95f),
                horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                // Avatar insignia de VIERNES en mensajes del asistente
                if (!message.isFromUser) {
                    ViernesLogoBadge(
                        size = 38.dp,
                        borderGlow = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (message.isFromUser) 24.dp else 4.dp,
                        topEnd = 24.dp,
                        bottomStart = 24.dp,
                        bottomEnd = if (message.isFromUser) 4.dp else 24.dp
                    ),
                    color = if (message.isFromUser) {
                        Color(0xFF131B32)
                    } else {
                        Color(0xFF090E20)
                    },
                    modifier = Modifier
                        .border(
                            width = if (!message.isFromUser) 1.2.dp else 1.dp,
                            brush = if (!message.isFromUser) {
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF2563EB),
                                        Color(0xFF7C3AED)
                                    )
                                )
                            } else {
                                Brush.linearGradient(listOf(AuraViolet.copy(alpha = 0.5f), AuraCardBorder))
                            },
                            shape = RoundedCornerShape(
                                topStart = if (message.isFromUser) 24.dp else 4.dp,
                                topEnd = 24.dp,
                                bottomStart = 24.dp,
                                bottomEnd = if (message.isFromUser) 4.dp else 24.dp
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Proveedor de IA / Herramienta usada (Transparencia para el usuario)
                        if (!message.isFromUser && (message.provider != null || message.toolUsed != null)) {
                            Row(
                                modifier = Modifier.padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (message.provider != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E293B).copy(alpha = 0.65f),
                                        border = BorderStroke(0.6.dp, Color(0xFF334155))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (message.provider.contains("Gemini", ignoreCase = true)) AuraCyan else AuraViolet
                                                    )
                                            )
                                            Text(
                                                text = if (message.latencyMs != null) "${message.provider} • ${message.latencyMs}ms" else message.provider,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                                if (message.toolUsed != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AuraCyan.copy(alpha = 0.12f),
                                        border = BorderStroke(0.6.dp, AuraCyan.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "⚡ ${message.toolUsed}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = AuraCyan,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (message.actionSummary != null && message.toolUsed == null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AuraCyan.copy(alpha = 0.12f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "⚡ ${message.actionSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuraCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (message.confirmationTitle != null) {
                            Text(
                                text = message.confirmationTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = AuraViolet,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Texto de contenido enriquecido o con error amable
                        if (message.isError) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AuraError.copy(alpha = 0.10f),
                                border = BorderStroke(0.8.dp, AuraError.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = AuraError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = message.text,
                                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                            color = Color(0xFFFFB4AB)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tip: Si usas IA externa, revisa tu conexión o la clave en Ajustes > Inteligencia Artificial.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = AuraTextMuted
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.Bottom) {
                                if (annotatedText != null) {
                                    Text(
                                        text = annotatedText,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        color = AuraTextPrimary
                                    )
                                } else {
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        color = AuraTextPrimary
                                    )
                                }

                                if (message.isStreaming) {
                                    Text(
                                        text = " ▋",
                                        color = AuraCyan.copy(alpha = cursorAlpha),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        // Opciones de desambiguación si existen
                        if (message.disambiguationOptions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Contactos disponibles:",
                                style = MaterialTheme.typography.labelSmall,
                                color = AuraTextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                message.disambiguationOptions.forEach { option ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AuraSurfaceLightDark,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, AuraCardBorder, RoundedCornerShape(8.dp))
                                            .clickable { onDisambiguateClick(option) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Person,
                                                contentDescription = null,
                                                tint = AuraCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AuraTextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Fila inferior de timestamp y altavoz TTS
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = AppUtils.formatTimestamp(message.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )

                            if (!message.isFromUser) {
                                IconButton(
                                    onClick = { onSpeakClick(message.text) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("speak_button_${message.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Reproducir voz",
                                        tint = AuraCyan.copy(alpha = 0.85f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        // Barra de confirmación (Nivel 2 y 3)
                        if (message.hasConfirmation) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onConfirmClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("confirm_action_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AuraSurfaceDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sí, guardar", color = AuraSurfaceDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = onCancelClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cancel_action_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = AuraTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("No", color = AuraTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Destaca palabras clave ("VIERNES", "Redmi Note 13", comillas y órdenes)
 * con colores de acento neón acordes al diseño de la interfaz.
 */
private fun formatAssistantText(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0

        // Palabras clave destacadas
        val highlightMap = listOf(
            "VIERNES" to SpanStyle(color = Color(0xFFA855F7), fontWeight = FontWeight.Bold),
            "Redmi Note 13" to SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold),
            "\"Abrir WhatsApp\"" to SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold),
            "\"Pon música\"" to SpanStyle(color = Color(0xFFEC4899), fontWeight = FontWeight.Bold),
            "«Abrir WhatsApp»" to SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold),
            "«Pon música»" to SpanStyle(color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
        )

        val fullText = text
        var remaining = fullText

        // Regex para capturar comillas dobles o angulares
        val quoteRegex = Regex("(\"[^\"]+\"|«[^»]+»|\\*\\*[^\\*]+\\*\\*)")
        val matches = quoteRegex.findAll(fullText).toList()

        if (matches.isEmpty() && !fullText.contains("VIERNES") && !fullText.contains("Redmi Note 13")) {
            append(fullText)
            return@buildAnnotatedString
        }

        // Construcción inteligente de tramos anotados
        var lastIndex = 0
        val tokens = Regex("(\"[^\"]+\"|«[^»]+»|\\*\\*[^*]+\\*\\*|VIERNES|Redmi Note 13)")
            .findAll(fullText)

        tokens.forEach { match ->
            if (match.range.first > lastIndex) {
                append(fullText.substring(lastIndex, match.range.first))
            }
            val token = match.value
            when {
                token == "VIERNES" -> {
                    withStyle(SpanStyle(color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)) {
                        append("VIERNES")
                    }
                }
                token == "Redmi Note 13" -> {
                    withStyle(SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)) {
                        append("Redmi Note 13")
                    }
                }
                token.contains("WhatsApp", ignoreCase = true) -> {
                    val clean = token.replace("\"", "").replace("«", "").replace("»", "").replace("**", "")
                    withStyle(SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)) {
                        append("\"$clean\"")
                    }
                }
                token.contains("música", ignoreCase = true) || token.contains("musica", ignoreCase = true) -> {
                    val clean = token.replace("\"", "").replace("«", "").replace("»", "").replace("**", "")
                    withStyle(SpanStyle(color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)) {
                        append("\"$clean\"")
                    }
                }
                token.startsWith("**") && token.endsWith("**") -> {
                    val clean = token.removeSurrounding("**")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))) {
                        append(clean)
                    }
                }
                else -> {
                    withStyle(SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)) {
                        append(token)
                    }
                }
            }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < fullText.length) {
            append(fullText.substring(lastIndex))
        }
    }
}

