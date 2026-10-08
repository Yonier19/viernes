package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.assistant.AssistantState
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraNeonPink
import com.example.ui.theme.AuraViolet

@Composable
fun AuraAvatar(
    state: AssistantState,
    soundLevel: Float = 0f,
    size: Dp = 140.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_animations")

    // Idle breathing pulse
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    // Fast rotation for processing
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.PROCESANDO) 1200 else 6000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val primaryColor = when (state) {
        AssistantState.INACTIVO -> AuraCyan
        AssistantState.ESCUCHANDO -> AuraCyan
        AssistantState.PROCESANDO -> AuraViolet
        AssistantState.RESPONDIENDO -> AuraNeonPink
        AssistantState.ERROR -> AuraError
    }

    val secondaryColor = when (state) {
        AssistantState.INACTIVO -> AuraViolet
        AssistantState.ESCUCHANDO -> Color(0xFF00B4D8)
        AssistantState.PROCESANDO -> AuraCyan
        AssistantState.RESPONDIENDO -> AuraViolet
        AssistantState.ERROR -> Color(0xFFB91C1C)
    }

    // Dynamic scale factor according to state and mic audio
    val scaleFactor = when (state) {
        AssistantState.ESCUCHANDO -> (1.0f + (soundLevel * 0.45f)).coerceIn(1.0f, 1.45f)
        AssistantState.PROCESANDO -> 1.0f
        AssistantState.RESPONDIENDO -> (1.0f + (idlePulse - 0.95f) * 0.6f)
        AssistantState.INACTIVO -> idlePulse
        AssistantState.ERROR -> 1.0f
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
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = this.size.minDimension / 2.7f

            // 1. Outermost Ambient Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha * 0.35f),
                        secondaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.6f * scaleFactor
                ),
                radius = baseRadius * 1.6f * scaleFactor,
                center = center
            )

            // 2. Outer Rotating Orbital Ring
            rotate(degrees = rotationAngle, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.9f),
                            secondaryColor.copy(alpha = 0.2f),
                            primaryColor.copy(alpha = 0.9f)
                        )
                    ),
                    radius = baseRadius * 1.15f * scaleFactor,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 3. Counter-Rotating Inner Ring
            rotate(degrees = -rotationAngle * 1.3f, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.8f),
                            primaryColor.copy(alpha = 0.1f),
                            secondaryColor.copy(alpha = 0.8f)
                        )
                    ),
                    radius = baseRadius * 0.9f * scaleFactor,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 4. Central Holographic Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor.copy(alpha = 0.85f),
                        Color(0xFF070A12)
                    ),
                    center = center,
                    radius = baseRadius * 0.65f * scaleFactor
                ),
                radius = baseRadius * 0.65f * scaleFactor,
                center = center
            )

            // 5. Sparkling Center Star
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 4.dp.toPx() * (if (state == AssistantState.ESCUCHANDO) 1.5f else 1.0f),
                center = center
            )
        }
    }
}
