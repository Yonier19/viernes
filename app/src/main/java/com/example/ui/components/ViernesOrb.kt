package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.assistant.AssistantState
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraNeonPink
import com.example.ui.theme.AuraViolet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Orbe celestial cuántico de VIERNES con movimiento continuo y reactivo al estado del sistema.
 *
 * Incluye:
 * - Esfera planetaria holográfica central con halo volumétrico.
 * - Anillos orbitales giratorios elípticos inclinados (con sweep gradients y partículas de luz).
 * - Polvo estelar y partículas cósmicas flotantes con pulsación fluida.
 * - Reacción acústica a decibelios del micrófono (soundLevel) y transiciones según AssistantState.
 */
@Composable
fun ViernesCelestialOrb(
    state: AssistantState,
    soundLevel: Float = 0f,
    size: Dp = 230.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "viernes_orb_motion")

    // 1. Respiración pulsante idle
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breathing"
    )

    // 2. Rotación continua de anillo orbital exterior
    val outerRingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.PROCESANDO) 2000 else 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring_rotation"
    )

    // 3. Contra-rotación de anillo orbital intermedio
    val innerRingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.PROCESANDO) 1600 else 7500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring_rotation"
    )

    // 4. Parpadeo sutil de partículas estelares
    val starGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_glow"
    )

    // Colores según estado del asistente
    val (primaryColor, secondaryColor) = when (state) {
        AssistantState.INACTIVO -> AuraCyan to AuraViolet
        AssistantState.ESCUCHANDO -> AuraCyan to Color(0xFF00F5D4)
        AssistantState.PROCESANDO -> AuraViolet to AuraCyan
        AssistantState.RESPONDIENDO -> AuraNeonPink to AuraViolet
        AssistantState.ERROR -> AuraError to Color(0xFFFF6B6B)
    }

    // Escala dinámica combinada
    val audioScale = when (state) {
        AssistantState.ESCUCHANDO -> 1.0f + (soundLevel.coerceIn(0f, 1f) * 0.40f)
        AssistantState.RESPONDIENDO -> 1.0f + ((breathingPulse - 0.96f) * 0.9f)
        AssistantState.PROCESANDO -> 1.02f
        AssistantState.INACTIVO -> breathingPulse
        AssistantState.ERROR -> 1.0f
    }

    // Puntos fijos de partículas en el espacio orbital (precalculados)
    val particles = remember {
        List(24) { index ->
            val angle = (index * (360f / 24f)) * (Math.PI / 180.0)
            val distanceFactor = 0.75f + (index % 5) * 0.08f
            val particleSize = 2.5f + (index % 3) * 1.5f
            Triple(angle, distanceFactor, particleSize)
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Lienzo para anillos orbitales dinámicos y polvo de estrellas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = this.size.minDimension / 2.3f * audioScale

            // 1. Resplandor halo exterior radial
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f * starGlow),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.5f
                ),
                radius = baseRadius * 1.5f,
                center = center
            )

            // 2. Anillo exterior inclinado (Eclíptica primaria)
            rotate(degrees = -18f, pivot = center) {
                rotate(degrees = outerRingRotation, pivot = center) {
                    drawOval(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.95f),
                                secondaryColor.copy(alpha = 0.20f),
                                Color(0xFF00E5FF).copy(alpha = 0.85f),
                                primaryColor.copy(alpha = 0.15f),
                                primaryColor.copy(alpha = 0.95f)
                            )
                        ),
                        topLeft = Offset(center.x - baseRadius * 1.25f, center.y - baseRadius * 0.48f),
                        size = Size(baseRadius * 2.5f, baseRadius * 0.96f),
                        style = Stroke(width = 2.8.dp.toPx())
                    )
                }
            }

            // 3. Anillo intermedio inclinado (Eclíptica secundaria con inclinación contraria)
            rotate(degrees = 22f, pivot = center) {
                rotate(degrees = innerRingRotation, pivot = center) {
                    drawOval(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.9f),
                                primaryColor.copy(alpha = 0.15f),
                                AuraNeonPink.copy(alpha = 0.8f),
                                secondaryColor.copy(alpha = 0.9f)
                            )
                        ),
                        topLeft = Offset(center.x - baseRadius * 1.10f, center.y - baseRadius * 0.40f),
                        size = Size(baseRadius * 2.2f, baseRadius * 0.80f),
                        style = Stroke(width = 2.0.dp.toPx())
                    )
                }
            }

            // 4. Anillo de órbita rápida de partículas
            rotate(degrees = -5f, pivot = center) {
                rotate(degrees = outerRingRotation * 1.4f, pivot = center) {
                    drawOval(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.9f),
                                primaryColor.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.8f)
                            )
                        ),
                        topLeft = Offset(center.x - baseRadius * 0.95f, center.y - baseRadius * 0.32f),
                        size = Size(baseRadius * 1.9f, baseRadius * 0.64f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }

            // 5. Partículas cósmicas flotantes
            particles.forEachIndexed { i, (angle, distanceFactor, pSize) ->
                val dynamicAngle = angle + (outerRingRotation * (0.015f * (if (i % 2 == 0) 1 else -1)))
                val r = baseRadius * distanceFactor
                val px = (center.x + r * cos(dynamicAngle)).toFloat()
                val py = (center.y + (r * 0.42f) * sin(dynamicAngle)).toFloat()

                val alpha = (0.3f + (0.7f * ((starGlow + (i * 0.1f)) % 1f))).coerceIn(0.1f, 1f)
                val pColor = if (i % 3 == 0) primaryColor else if (i % 3 == 1) secondaryColor else Color.White

                drawCircle(
                    color = pColor.copy(alpha = alpha),
                    radius = pSize.dp.toPx() * (if (state == AssistantState.ESCUCHANDO) 1.3f else 1.0f),
                    center = Offset(px, py)
                )
            }
        }

        // Esfera celestial central holográfica con textura planetaria
        Box(
            modifier = Modifier
                .size(size * 0.58f * audioScale)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_viernes_celestial_orb_1791413994970),
                contentDescription = "VIERNES Orbe Celestial",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Velo de tinte dinámico reactivo al estado
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.25f),
                            secondaryColor.copy(alpha = 0.45f)
                        ),
                        center = center,
                        radius = this.size.minDimension / 2f
                    )
                )
            }
        }
    }
}
