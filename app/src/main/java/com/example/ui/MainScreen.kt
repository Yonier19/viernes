package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantState
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.ViernesCelestialOrb
import com.example.ui.components.ViernesLogoBadge
import com.example.ui.theme.AuraBgDark
import com.example.ui.theme.AuraCardBorder
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraNeonPink
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraSurfaceDark
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val soundLevel by viewModel.soundLevel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isTtsMuted by viewModel.isTtsMuted.collectAsState()

    var inputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll al final en nuevos mensajes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04060E))
    ) {
        // Fondo cósmico con tenue horizonte bioluminiscente en el fondo
        CosmicHorizonBackground()

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            containerColor = Color.Transparent,
            topBar = {
                ViernesTopAppBar(
                    state = assistantState,
                    isTtsMuted = isTtsMuted,
                    onToggleTts = { viewModel.toggleTtsMute() },
                    onNavigateMemory = { viewModel.navigateTo(AuraNavScreen.MEMORY) },
                    onNavigatePermissions = { viewModel.navigateTo(AuraNavScreen.PERMISSIONS) },
                    onNavigateSettings = { viewModel.navigateTo(AuraNavScreen.SETTINGS) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Orbe Celestial interactivo con movimiento continuo y reactivo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ViernesCelestialOrb(
                        state = assistantState,
                        soundLevel = soundLevel,
                        size = 205.dp,
                        onClick = {
                            if (assistantState == AssistantState.ESCUCHANDO) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening()
                            }
                        }
                    )
                }

                // 2. Fila horizontal de Chips de Acciones Rápidas (WhatsApp, YouTube, Música, Chat)
                ViernesQuickActionsRow(
                    onActionSelected = { command ->
                        viewModel.executeCommand(command)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Lista de Mensajes y Burbuja de Chat
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onSpeakClick = { text -> viewModel.speakText(text) },
                            onConfirmClick = { viewModel.confirmPendingAction() },
                            onCancelClick = { viewModel.cancelPendingAction() },
                            onDisambiguateClick = { contact -> viewModel.selectDisambiguation(contact) }
                        )
                    }
                }

                // 4. Muelle Inferior de Entrada y Micrófono Neón
                ViernesBottomCommandDock(
                    inputText = inputQuery,
                    onInputTextChanged = { inputQuery = it },
                    onSend = {
                        if (inputQuery.isNotBlank()) {
                            viewModel.executeCommand(inputQuery)
                            inputQuery = ""
                        }
                    },
                    assistantState = assistantState,
                    onMicClick = {
                        if (assistantState == AssistantState.ESCUCHANDO) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                    },
                    onAddClick = {
                        viewModel.executeCommand("¿Qué puedes hacer?")
                    }
                )
            }
        }
    }
}

/**
 * Barra superior futurista de VIERNES exactamente como en el diseño de referencia:
 * - Logo 'V' facetado poligonal con gradiente cian-violeta.
 * - Título 'VIERNES' + subtítulo 'TU ASISTENTE INTELIGENTE'.
 * - Píldora de estado reactiva (● INACTIVO).
 * - 4 botones de acción en píldoras circulares de cristal oscuro.
 */
@Composable
private fun ViernesTopAppBar(
    state: AssistantState,
    isTtsMuted: Boolean,
    onToggleTts: () -> Unit,
    onNavigateMemory: () -> Unit,
    onNavigatePermissions: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Bloque Izquierdo: Logotipo 'V' + VIERNES + Subtítulo + Estado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ViernesLogoBadge(
                size = 38.dp,
                borderGlow = true
            )

            Column {
                Text(
                    text = "VIERNES",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontSize = 19.sp
                    ),
                    color = Color(0xFF00E5FF)
                )
                Text(
                    text = "TU ASISTENTE INTELIGENTE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Píldora de estado reactiva
            ViernesStatusPill(state = state)
        }

        // Bloque Derecho: Iconos de acción en contenedores de cristal circular
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HeaderGlassIconButton(
                icon = if (isTtsMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Voz TTS",
                tint = if (isTtsMuted) AuraTextMuted else Color(0xFFE2E8F0),
                onClick = onToggleTts,
                testTag = "toggle_tts_btn"
            )

            HeaderGlassIconButton(
                icon = Icons.Default.Psychology,
                contentDescription = "Memoria",
                tint = Color(0xFFC084FC),
                onClick = onNavigateMemory,
                testTag = "nav_memory_btn"
            )

            HeaderGlassIconButton(
                icon = Icons.Default.Security,
                contentDescription = "Permisos",
                tint = Color(0xFF60A5FA),
                onClick = onNavigatePermissions,
                testTag = "nav_permissions_btn"
            )

            HeaderGlassIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Configuración",
                tint = Color(0xFFE2E8F0),
                onClick = onNavigateSettings,
                testTag = "nav_settings_btn"
            )
        }
    }
}

@Composable
private fun ViernesStatusPill(state: AssistantState) {
    val (label, dotColor) = when (state) {
        AssistantState.INACTIVO -> "INACTIVO" to Color(0xFF10B981)
        AssistantState.ESCUCHANDO -> "ESCUCHANDO" to AuraCyan
        AssistantState.PROCESANDO -> "PROCESANDO" to AuraViolet
        AssistantState.RESPONDIENDO -> "RESPONDIENDO" to AuraNeonPink
        AssistantState.ERROR -> "ERROR" to AuraError
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0C1326),
        modifier = Modifier.border(0.8.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun HeaderGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF0C1326))
            .border(0.8.dp, Color(0xFF1E293B), CircleShape)
            .clickable(onClick = onClick)
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

/**
 * Fila de chips de acceso rápido exactamente como en el diseño de referencia:
 * 1. Abrir WhatsApp > (con borde destacado en cian)
 * 2. Abrir YouTube >
 * 3. Pon música >
 * 4. Abrir Chat >
 */
@Composable
private fun ViernesQuickActionsRow(
    onActionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // WhatsApp (Destacado)
        QuickActionChip(
            title = "Abrir WhatsApp >",
            icon = Icons.Default.Whatsapp,
            iconBadgeBg = Color(0xFF10B981),
            isFeatured = true,
            onClick = { onActionSelected("Abre WhatsApp") }
        )

        // YouTube
        QuickActionChip(
            title = "Abrir YouTube >",
            icon = Icons.Default.PlayArrow,
            iconBadgeBg = Color(0xFFEF4444),
            isFeatured = false,
            onClick = { onActionSelected("Abre YouTube") }
        )

        // Música
        QuickActionChip(
            title = "Pon música >",
            icon = Icons.Default.MusicNote,
            iconBadgeBg = Color(0xFFD946EF),
            isFeatured = false,
            onClick = { onActionSelected("Pon música") }
        )

        // Chat
        QuickActionChip(
            title = "Abrir Chat >",
            icon = Icons.Default.ChatBubble,
            iconBadgeBg = Color(0xFF3B82F6),
            isFeatured = false,
            onClick = { onActionSelected("¿Qué puedes hacer?") }
        )
    }
}

@Composable
private fun QuickActionChip(
    title: String,
    icon: ImageVector,
    iconBadgeBg: Color,
    isFeatured: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF090E1F),
        modifier = Modifier
            .then(
                if (isFeatured) {
                    Modifier.border(
                        width = 1.3.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFF2563EB))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier.border(0.8.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                }
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(iconBadgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Muelle inferior de comando con botón "+" a la izquierda, campo de texto en el centro
 * y botón de micrófono flotante con gradiente radiante a la derecha.
 */
@Composable
private fun ViernesBottomCommandDock(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    assistantState: AssistantState,
    onMicClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dock_mic_glow")
    val isListening = assistantState == AssistantState.ESCUCHANDO

    val micPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 700 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF1E293B),
                        Color(0xFF2563EB).copy(alpha = 0.5f),
                        Color(0xFF1E293B)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        color = Color(0xFF090E20),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón Circular "+"
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF101730))
                    .border(1.dp, Color(0xFF6D28D9).copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onAddClick)
                    .testTag("add_options_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Opciones",
                    tint = Color(0xFFC084FC),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Separador vertical sutil
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0xFF334155))
            )

            // Campo de texto de comando
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = {
                    Text(
                        text = "Di o escribe un comando...",
                        color = Color(0xFF64748B),
                        fontSize = 12.5.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = AuraTextPrimary,
                    unfocusedTextColor = AuraTextPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true
            )

            // Botón de Enviar (visible cuando se escribe texto)
            AnimatedVisibility(
                visible = inputText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AuraCyan, AuraViolet)
                            )
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color(0xFF04060E),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Botón Micrófono Principal Neón con Gradiente Radiante
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .then(
                        if (isListening) {
                            Modifier.border(
                                width = 2.dp,
                                color = AuraCyan.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                        } else {
                            Modifier.border(
                                width = 1.2.dp,
                                color = Color(0xFF3B82F6).copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                        }
                    )
                    .background(
                        Brush.linearGradient(
                            if (isListening) {
                                listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                            } else {
                                listOf(Color(0xFF00E5FF), Color(0xFF3B82F6), Color(0xFF8B5CF6))
                            }
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 28.dp),
                        onClick = onMicClick
                    )
                    .testTag("mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Detener escucha" else "Escuchar",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * Fondo espacial de VIERNES:
 * - Tonos casi negro con azul marino y violeta oscuro.
 * - Partículas y nodos luminosos pequeños azules y violetas dispersos en el espacio cósmico.
 * - Suelo oscuro inferior con reflejos especulares de luz azul y violeta como agua o cristal.
 */
@Composable
private fun CosmicHorizonBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_bg_particles")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_twinkle"
    )

    // Nodos estelares dispersos
    val backgroundStars = remember {
        List(30) { index ->
            val xFactor = (index * 37 % 100) / 100f
            val yFactor = (index * 53 % 80) / 100f
            val radius = 1.2f + (index % 3) * 0.8f
            val isViolet = index % 2 == 0
            Triple(Offset(xFactor, yFactor), radius, isViolet)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Degradado vertical cósmico base (espacio exterior)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF03050C),
                    Color(0xFF050816),
                    Color(0xFF070B1F),
                    Color(0xFF090D24)
                ),
                startY = 0f,
                endY = height * 0.82f
            ),
            size = Size(width, height * 0.82f)
        )

        // 2. Partículas y nodos luminosos dispersos
        backgroundStars.forEachIndexed { i, (normPos, r, isViolet) ->
            val px = normPos.x * width
            val py = normPos.y * height
            val starAlpha = (0.25f + 0.5f * ((twinkle + (i * 0.08f)) % 1f)).coerceIn(0.15f, 0.85f)
            val pColor = if (isViolet) Color(0xFFA855F7) else Color(0xFF38BDF8)

            drawCircle(
                color = pColor.copy(alpha = starAlpha),
                radius = r.dp.toPx(),
                center = Offset(px, py)
            )
        }

        // 3. Suelo reflectante oscuro en la parte inferior (estilo agua/cristal)
        val floorStartY = height * 0.82f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0C132E),
                    Color(0xFF060917),
                    Color(0xFF03050B)
                ),
                startY = floorStartY,
                endY = height
            ),
            topLeft = Offset(0f, floorStartY),
            size = Size(width, height - floorStartY)
        )

        // 4. Línea de horizonte bioluminiscente sutil con reflejos azules/violetas
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF2563EB).copy(alpha = 0.35f),
                    Color(0xFF00E5FF).copy(alpha = 0.65f),
                    Color(0xFF9333EA).copy(alpha = 0.45f),
                    Color.Transparent
                )
            ),
            start = Offset(width * 0.10f, floorStartY),
            end = Offset(width * 0.90f, floorStartY),
            strokeWidth = 1.2.dp.toPx()
        )

        // 5. Reflejo especular elíptico en el suelo reflectante
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.12f),
                    Color(0xFF8B5CF6).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(width / 2f, floorStartY + (height - floorStartY) * 0.35f),
                radius = width * 0.55f
            ),
            topLeft = Offset(width * 0.05f, floorStartY),
            size = Size(width * 0.90f, (height - floorStartY) * 0.80f)
        )
    }
}
