package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraViolet

/**
 * Insignia de logotipo oficial de VIERNES: polígono geométrico "V" con gradiente neón cian-violeta.
 */
@Composable
fun ViernesLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    borderGlow: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0D152A),
                        Color(0xFF070B18)
                    )
                )
            )
            .then(
                if (borderGlow) {
                    Modifier.border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(AuraCyan, AuraViolet)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_viernes_v_logo_1791414008186),
            contentDescription = "VIERNES Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
        )
    }
}
