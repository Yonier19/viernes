package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ViernesBackground
import com.example.ui.theme.ViernesCyan
import com.example.ui.theme.ViernesViolet
import kotlin.random.Random

/**
 * Fondo oficial de VIERNES acorde al mockup de referencia:
 * - Espacio cósmico profundo casi negro (#04060F a #080D1F).
 * - Constelación de partículas y nodos luminosos estáticos precalculados (sin asignación de memoria por frame).
 * - Suelo/horizonte reflectante inferior con resplandor bioluminiscente azul y violeta estilo agua/cristal.
 * - Rendimiento óptimo en Snapdragon 685 (0 blur, 0 RenderEffect, puro Canvas optimizado).
 */
@Composable
fun ViernesBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 40 partículas y nodos de luz estelares precalculados con semilla fija
    val stars = remember {
        val rand = Random(42)
        List(40) {
            StarPoint(
                relX = rand.nextFloat(),
                relY = rand.nextFloat() * 0.82f, // Mayormente en el 82% superior
                radius = 0.8f + rand.nextFloat() * 2.2f,
                alpha = 0.25f + rand.nextFloat() * 0.65f,
                isViolet = rand.nextBoolean()
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViernesBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Degradado vertical ambiental profundo
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF04060F),
                        Color(0xFF060918),
                        Color(0xFF090E24),
                        Color(0xFF050814)
                    )
                ),
                size = size
            )

            // 2. Resplandor superior sutil (aurora galáctica lejana)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ViernesViolet.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, height * 0.22f),
                    radius = width * 0.75f
                ),
                radius = width * 0.75f,
                center = Offset(width * 0.5f, height * 0.22f)
            )

            // 3. Suelo/Horizonte reflectante inferior (estilo agua oscura con reflejo azul/violeta)
            val horizonTop = height * 0.80f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF0D142E).copy(alpha = 0.40f),
                        Color(0xFF131D40).copy(alpha = 0.60f),
                        Color(0xFF070B18)
                    ),
                    startY = horizonTop,
                    endY = height
                ),
                topLeft = Offset(0f, horizonTop),
                size = Size(width, height - horizonTop)
            )

            // Resplandor elíptico en el horizonte (reflejo de luz bioluminiscente del orbe)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ViernesCyan.copy(alpha = 0.18f),
                        ViernesViolet.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.5f, height * 0.88f),
                    radius = width * 0.65f
                ),
                topLeft = Offset(-width * 0.15f, height * 0.76f),
                size = Size(width * 1.30f, height * 0.25f)
            )

            // 4. Dibujo de nodos y partículas estelares estáticas (cero asignación en onDraw)
            stars.forEach { star ->
                val px = star.relX * width
                val py = star.relY * height
                val color = if (star.isViolet) ViernesViolet else ViernesCyan

                drawCircle(
                    color = color.copy(alpha = star.alpha),
                    radius = star.radius,
                    center = Offset(px, py)
                )
            }
        }

        content()
    }
}

private data class StarPoint(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val alpha: Float,
    val isViolet: Boolean
)
