package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Sistema de formas geométricas oficiales de VIERNES
val ViernesAssistantBubbleShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 24.dp,
    bottomStart = 24.dp,
    bottomEnd = 24.dp
)

val ViernesUserBubbleShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 4.dp,
    bottomStart = 24.dp,
    bottomEnd = 24.dp
)

val ViernesPillShape = RoundedCornerShape(28.dp)
val ViernesChipShape = RoundedCornerShape(20.dp)
val ViernesCardShape = RoundedCornerShape(16.dp)
val ViernesBadgeShape = RoundedCornerShape(12.dp)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
