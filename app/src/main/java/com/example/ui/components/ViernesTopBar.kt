package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantState
import com.example.ui.theme.ViernesBorderNeon
import com.example.ui.theme.ViernesCyan
import com.example.ui.theme.ViernesError
import com.example.ui.theme.ViernesNeonPink
import com.example.ui.theme.ViernesSuccess
import com.example.ui.theme.ViernesTextMuted
import com.example.ui.theme.ViernesTextPrimary
import com.example.ui.theme.ViernesTextSecondary
import com.example.ui.theme.ViernesViolet

/**
 * Barra superior oficial de VIERNES acorde al mockup de referencia:
 * - Isotipo vectorial 'V' + Tipografía geométrica espaciada 'VIERNES'.
 * - Subtítulo 'TU ASISTENTE INTELIGENTE' con ajuste responsivo.
 * - StatusChip translúcido con punto luminoso de estado (TalkBack accesible).
 * - 4 botones de acción circulares con objetivo táctil de 48 dp x 48 dp.
 */
@Composable
fun ViernesTopBar(
    state: AssistantState,
    isTtsMuted: Boolean,
    onToggleTts: () -> Unit,
    onNavigateMemory: () -> Unit,
    onNavigatePermissions: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val isNarrowScreen = maxWidth < 380.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lado Izquierdo: Logotipo + Identidad de Marca + StatusChip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                ViernesVectorLogo(
                    size = 36.dp,
                    showContainer = true
                )

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "VIERNES",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.0.sp,
                            fontSize = if (isNarrowScreen) 17.sp else 19.sp
                        ),
                        color = ViernesCyan
                    )

                    if (!isNarrowScreen) {
                        Text(
                            text = "TU ASISTENTE INTELIGENTE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ViernesTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Chip de Estado reactivo
                ViernesStatusChip(state = state)
            }

            // Lado Derecho: 4 botones circulares con área táctil accesible de 48 dp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ViernesCircleActionButton(
                    icon = if (isTtsMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isTtsMuted) "Activar voz del asistente" else "Silenciar voz del asistente",
                    tint = if (isTtsMuted) ViernesTextMuted else Color(0xFFE2E8F0),
                    onClick = onToggleTts,
                    testTag = "toggle_tts_btn"
                )

                ViernesCircleActionButton(
                    icon = Icons.Default.Psychology,
                    contentDescription = "Abrir pantalla de memoria y aprendizaje",
                    tint = Color(0xFFC084FC),
                    onClick = onNavigateMemory,
                    testTag = "nav_memory_btn"
                )

                ViernesCircleActionButton(
                    icon = Icons.Default.Security,
                    contentDescription = "Abrir permisos y privacidad",
                    tint = Color(0xFF60A5FA),
                    onClick = onNavigatePermissions,
                    testTag = "nav_permissions_btn"
                )

                ViernesCircleActionButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Abrir ajustes y configuración de IA",
                    tint = Color(0xFFE2E8F0),
                    onClick = onNavigateSettings,
                    testTag = "nav_settings_btn"
                )
            }
        }
    }
}

/**
 * Chip de estado translúcido de VIERNES.
 * Muestra el estado actual con texto claro y punto luminoso reactivo.
 */
@Composable
fun ViernesStatusChip(
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val (label, dotColor) = when (state) {
        AssistantState.INACTIVO -> "INACTIVO" to ViernesSuccess
        AssistantState.ESCUCHANDO -> "ESCUCHANDO" to ViernesCyan
        AssistantState.PROCESANDO -> "PROCESANDO" to ViernesViolet
        AssistantState.RESPONDIENDO -> "RESPONDIENDO" to ViernesNeonPink
        AssistantState.ERROR -> "ERROR" to ViernesError
    }

    val accessibilityDesc = "Estado del asistente: $label"

    Box(
        modifier = modifier
            .semantics { contentDescription = accessibilityDesc }
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0C1326))
            .border(0.8.dp, ViernesBorderNeon, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.5.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                color = ViernesTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Botón de acción circular translúcido con tamaño visual refinado y área táctil accesible de 48 dp.
 */
@Composable
fun ViernesCircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize() // Garantiza área táctil >= 48dp x 48dp (WCAG AA)
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF0C1326))
            .border(0.8.dp, ViernesBorderNeon, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}
