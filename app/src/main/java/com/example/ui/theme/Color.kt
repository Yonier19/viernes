package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =========================================================================
// SISTEMA DE DISEÑO OFICIAL VIERNES (Optimizado para AMOLED Redmi Note 13)
// =========================================================================

// Fondos y Superficies Espaciales
val ViernesBackground = Color(0xFF04060F)
val ViernesSurfaceDark = Color(0xFF070B1A)
val ViernesSurfaceGlass = Color(0xFF090E20)
val ViernesSurfaceCard = Color(0xFF0C1428)
val ViernesBorderNeon = Color(0xFF1E293B)
val ViernesBorderGlow = Color(0xFF2563EB)

// Paleta Neón Primaria y Secundaria
val ViernesCyan = Color(0xFF00E5FF)
val ViernesElectricBlue = Color(0xFF2563EB)
val ViernesViolet = Color(0xFF8B5CF6)
val ViernesPurple = Color(0xFF6D28D9)
val ViernesNeonPink = Color(0xFFEC4899)

// Estados del Sistema
val ViernesSuccess = Color(0xFF10B981) // Verde para Inactivo y WhatsApp
val ViernesWarning = Color(0xFFF59E0B)
val ViernesError = Color(0xFFEF4444)

// Tipografía y Contraste WCAG AA (> 4.5:1 sobre fondo oscuro)
val ViernesTextPrimary = Color(0xFFF8FAFC)
val ViernesTextSecondary = Color(0xFF94A3B8)
val ViernesTextMuted = Color(0xFF64748B)

// Degradado de Marca VIERNES Oficial (Cian -> Azul Eléctrico -> Violeta)
val ViernesBrandGradient = Brush.linearGradient(
    listOf(
        Color(0xFF00E5FF),
        Color(0xFF2563EB),
        Color(0xFF7C3AED)
    )
)

val ViernesBubbleBorderGradient = Brush.linearGradient(
    listOf(
        Color(0xFF00E5FF),
        Color(0xFF2563EB),
        Color(0xFF7C3AED)
    )
)

val ViernesMicGradient = Brush.linearGradient(
    listOf(
        Color(0xFF00E5FF),
        Color(0xFF3B82F6),
        Color(0xFF8B5CF6)
    )
)

// =========================================================================
// Tokens de compatibilidad con fases anteriores
// =========================================================================
val AuraCyan = ViernesCyan
val AuraCyanDark = Color(0xFF00B4D8)
val AuraViolet = ViernesViolet
val AuraPurpleDeep = ViernesPurple
val AuraNeonPink = ViernesNeonPink
val AuraBgDark = ViernesBackground
val AuraSurfaceDark = ViernesSurfaceGlass
val AuraSurfaceLightDark = ViernesSurfaceCard
val AuraCardBorder = ViernesBorderNeon
val AuraTextPrimary = ViernesTextPrimary
val AuraTextSecondary = ViernesTextSecondary
val AuraTextMuted = ViernesTextMuted
val AuraSuccess = ViernesSuccess
val AuraError = ViernesError
val AuraWarning = ViernesWarning
