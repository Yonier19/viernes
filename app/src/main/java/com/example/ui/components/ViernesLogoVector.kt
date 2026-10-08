package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ViernesCyan
import com.example.ui.theme.ViernesElectricBlue
import com.example.ui.theme.ViernesViolet

/**
 * Isotipo vectorial oficial de VIERNES.
 *
 * Dibuja un emblema poligonal facetado "V" en Canvas con trazados limpios y
 * degradados neón (cian a violeta eléctrico), idéntico al mockup de referencia,
 * escalable a cualquier tamaño sin pérdida ni compresión de bitmaps.
 */
@Composable
fun ViernesVectorLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    showContainer: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showContainer) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF070B1A))
                        .border(
                            width = 1.0.dp,
                            brush = Brush.linearGradient(
                                listOf(ViernesCyan.copy(alpha = 0.8f), ViernesViolet.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(size * 0.75f)
        ) {
            val w = this.size.width
            val h = this.size.height

            // Ala izquierda del isotipo "V" (facetada)
            val leftWingPath = Path().apply {
                moveTo(w * 0.05f, h * 0.12f)
                lineTo(w * 0.38f, h * 0.12f)
                lineTo(w * 0.50f, h * 0.88f)
                lineTo(w * 0.32f, h * 0.88f)
                close()
            }
            drawPath(
                path = leftWingPath,
                brush = Brush.linearGradient(
                    colors = listOf(ViernesCyan, ViernesElectricBlue),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.5f, h)
                )
            )

            // Ala derecha del isotipo "V" (facetada)
            val rightWingPath = Path().apply {
                moveTo(w * 0.95f, h * 0.12f)
                lineTo(w * 0.62f, h * 0.12f)
                lineTo(w * 0.50f, h * 0.88f)
                lineTo(w * 0.68f, h * 0.88f)
                close()
            }
            drawPath(
                path = rightWingPath,
                brush = Brush.linearGradient(
                    colors = listOf(ViernesViolet, Color(0xFF6D28D9)),
                    start = Offset(w, 0f),
                    end = Offset(w * 0.5f, h)
                )
            )

            // Faceta interior triangular reflectante (profundidad 3D)
            val innerFacetPath = Path().apply {
                moveTo(w * 0.38f, h * 0.12f)
                lineTo(w * 0.62f, h * 0.12f)
                lineTo(w * 0.50f, h * 0.48f)
                close()
            }
            drawPath(
                path = innerFacetPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF1E1B4B))
                )
            )
        }
    }
}
