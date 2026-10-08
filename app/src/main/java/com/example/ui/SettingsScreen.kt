package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AIActiveMode
import com.example.assistant.AIExternalService
import com.example.assistant.AISettingsManager
import com.example.assistant.AISettingsState
import com.example.assistant.ConnectionTestResult
import com.example.assistant.MemoryCategory
import com.example.ui.theme.AuraBgDark
import com.example.ui.theme.AuraCardBorder
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraSurfaceDark
import com.example.ui.theme.AuraSurfaceLightDark
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraViolet
import com.example.ui.theme.AuraWarning
import com.example.utils.AppUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isTtsMuted by viewModel.isTtsMuted.collectAsState()
    val isWakeWordContinuous by viewModel.isWakeWordContinuousEnabled.collectAsState()
    val isResidentRunning by viewModel.isResidentServiceRunning.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val commandHistory by viewModel.commandHistory.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AuraBgDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AuraBgDark),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = AuraCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configuración VIERNES",
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AuraCyan
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Target Hardware Profile Card (Redmi Note 13 4G)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraCardBorder, RoundedCornerShape(14.dp)),
                    color = AuraSurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = AuraCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Perfil del Dispositivo Objetivo",
                                style = MaterialTheme.typography.titleMedium,
                                color = AuraCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        InfoRow(label = "Asistente", value = "VIERNES (Nombre Final • Residente)")
                        InfoRow(label = "Modelo", value = "Xiaomi Redmi Note 13 4G (23129RA5FL)")
                        InfoRow(label = "Procesador", value = "Qualcomm Snapdragon 685 (8 cores, 6nm)")
                        InfoRow(label = "Memoria RAM", value = "8 GB LPDDR4X física + memoria extendida")
                        InfoRow(label = "Almacenamiento", value = "256 GB UFS 2.2 (164 GB libres)")
                        InfoRow(label = "Sistema Operativo", value = "Android 15 (HyperOS 2)")
                        InfoRow(label = "Pantalla", value = "6.67\" AMOLED 120Hz (2400 x 1080)")
                    }
                }
            }

            // Asistente Residente & Ventana Flotante
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraCardBorder, RoundedCornerShape(14.dp)),
                    color = AuraSurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = AuraCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Modo Residente y Burbuja Flotante",
                                style = MaterialTheme.typography.titleMedium,
                                color = AuraTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Servicio Residente en Segundo Plano",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuraTextPrimary
                                )
                                Text(
                                    text = "Mantiene a VIERNES lista para responder y activa la ventana flotante.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuraTextMuted
                                )
                            }
                            Switch(
                                checked = isResidentRunning,
                                onCheckedChange = { viewModel.toggleResidentService() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AuraBgDark,
                                    checkedTrackColor = AuraCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estado de Ventana Flotante:",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTextSecondary
                            )
                            Text(
                                text = if (isOverlayActive) "Activa en Pantalla" else "Inactiva / Sin Permiso",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverlayActive) AuraSuccess else AuraTextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!Settings.canDrawOverlays(context)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Habilitar Permiso de Superposición", fontSize = 12.sp, color = AuraCyan)
                            }
                        }
                    }
                }
            }

            // Palabra Clave: «Viernes»
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraCardBorder, RoundedCornerShape(14.dp)),
                    color = AuraSurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = AuraViolet,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Palabra Clave («Viernes»)",
                                style = MaterialTheme.typography.titleMedium,
                                color = AuraTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Detección continua de «Viernes»",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuraTextPrimary
                                )
                                Text(
                                    text = "Reconoce «Viernes, abre WhatsApp» o «Oye Viernes» para activarse.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuraTextMuted
                                )
                            }
                            Switch(
                                checked = isWakeWordContinuous,
                                onCheckedChange = { viewModel.toggleWakeWordContinuousListening() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AuraBgDark,
                                    checkedTrackColor = AuraViolet
                                )
                            )
                        }
                    }
                }
            }

            // Sección de Inteligencia Artificial
            item {
                AISettingsCard(viewModel = viewModel)
            }

            // Voice Engine Settings
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraCardBorder, RoundedCornerShape(14.dp)),
                    color = AuraSurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = AuraViolet,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Respuestas por Voz (Text-to-Speech)",
                                style = MaterialTheme.typography.titleMedium,
                                color = AuraTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reproducir voz automáticamente",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuraTextPrimary
                                )
                                Text(
                                    text = "VIERNES pronunciará las respuestas con el sintetizador nativo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuraTextMuted
                                )
                            }
                            Switch(
                                checked = !isTtsMuted,
                                onCheckedChange = { viewModel.toggleTtsMute() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AuraBgDark,
                                    checkedTrackColor = AuraCyan
                                )
                            )
                        }
                    }
                }
            }

            // Servicio de Accesibilidad
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraCardBorder, RoundedCornerShape(14.dp)),
                    color = AuraSurfaceDark,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AuraCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Servicio de Accesibilidad de VIERNES",
                                style = MaterialTheme.typography.titleMedium,
                                color = AuraTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Estado: ${if (isAccessibilityConnected) "Conectado y Operativo" else "Desconectado"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAccessibilityConnected) AuraSuccess else AuraTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Se utiliza estrictamente para automatizaciones permisibles del sistema y gestos de navegación si el usuario lo activa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Configurar en Ajustes de Accesibilidad", fontSize = 12.sp, color = AuraTextSecondary)
                        }
                    }
                }
            }

            // Command History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = AuraTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Historial de Comandos (${commandHistory.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (commandHistory.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearHistoryDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = AuraError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpiar", color = AuraError, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Command History Items
            items(commandHistory.take(15), key = { it.id }) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.6.dp, AuraCardBorder, RoundedCornerShape(10.dp)),
                    color = AuraSurfaceDark,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.rawText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AuraCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = AppUtils.formatTimestamp(item.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AuraTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Intención: ${item.intent} • ${item.executionResult}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = AuraSurfaceDark,
            title = { Text("¿Eliminar historial?", color = AuraTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Se borrarán los registros locales de comandos anteriores.", color = AuraTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraError)
                ) {
                    Text("Eliminar historial", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancelar", color = AuraTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = AuraTextMuted)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = AuraTextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    tint: Color = AuraCyan
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = tint,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = AuraTextPrimary,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextMuted,
                    lineHeight = 16.sp
                )
            }
        }
        trailingContent()
    }
}

@Composable
private fun AISettingsCard(
    viewModel: AssistantViewModel
) {
    val aiSettings by viewModel.aiSettings.collectAsState()

    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }

    var modelInput by remember(aiSettings.selectedModel) { mutableStateOf(aiSettings.selectedModel) }
    var baseUrlInput by remember(aiSettings.genericBaseUrl) { mutableStateOf(aiSettings.genericBaseUrl) }
    var baseUrlError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AuraCardBorder, RoundedCornerShape(14.dp)),
        color = AuraSurfaceDark,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera de la sección
            SectionHeader(
                icon = Icons.Default.AutoAwesome,
                title = "Inteligencia Artificial",
                tint = AuraCyan
            )

            Text(
                text = "Configura el motor de razonamiento para tu Redmi Note 13. Las claves se guardan cifradas en hardware con Android KeyStore.",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextMuted,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // 1. Proveedor activo: AUTO / SOLO LOCAL / SOLO EXTERNO
            Text(
                text = "Modo de Proveedor Activo",
                style = MaterialTheme.typography.labelLarge,
                color = AuraTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProviderModeOption(
                    title = "AUTO (Recomendado)",
                    description = "Usa IA externa cuando hay conexión y cambia a reglas locales si estás sin conexión.",
                    selected = aiSettings.activeMode == AIActiveMode.AUTO,
                    onClick = { viewModel.setAIActiveMode(AIActiveMode.AUTO) }
                )
                ProviderModeOption(
                    title = "SOLO LOCAL",
                    description = "Todo se procesa en el dispositivo. Máxima privacidad, sin internet.",
                    selected = aiSettings.activeMode == AIActiveMode.SOLO_LOCAL,
                    onClick = { viewModel.setAIActiveMode(AIActiveMode.SOLO_LOCAL) }
                )
                ProviderModeOption(
                    title = "SOLO EXTERNO",
                    description = "Consulta siempre al proveedor configurado en la nube.",
                    selected = aiSettings.activeMode == AIActiveMode.SOLO_EXTERNO,
                    onClick = { viewModel.setAIActiveMode(AIActiveMode.SOLO_EXTERNO) }
                )
            }

            // Los siguientes controles se muestran si no es SOLO LOCAL
            if (aiSettings.activeMode != AIActiveMode.SOLO_LOCAL) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AuraCardBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // 2. Servicio externo: selector entre Gemini y Genérico
                Text(
                    text = "Servicio de IA Externo",
                    style = MaterialTheme.typography.labelLarge,
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isGemini = aiSettings.externalService == AIExternalService.GEMINI
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isGemini) 1.5.dp else 0.8.dp,
                                color = if (isGemini) AuraCyan else AuraCardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.setAIExternalService(AIExternalService.GEMINI) },
                        color = if (isGemini) AuraSurfaceLightDark else AuraSurfaceDark,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Google Gemini", color = if (isGemini) AuraCyan else AuraTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("gemini-2.5-flash", color = AuraTextMuted, fontSize = 10.sp)
                        }
                    }

                    val isGeneric = aiSettings.externalService == AIExternalService.GENERIC_CHAT_COMPLETIONS
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isGeneric) 1.5.dp else 0.8.dp,
                                color = if (isGeneric) AuraViolet else AuraCardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.setAIExternalService(AIExternalService.GENERIC_CHAT_COMPLETIONS) },
                        color = if (isGeneric) AuraSurfaceLightDark else AuraSurfaceDark,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Chat-Completions", color = if (isGeneric) AuraViolet else AuraTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("OpenAI / vLLM / Ollama", color = AuraTextMuted, fontSize = 10.sp)
                        }
                    }
                }

                // 3. URL base (solo para servicio genérico compatible)
                if (aiSettings.externalService == AIExternalService.GENERIC_CHAT_COMPLETIONS) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "URL Base del Servidor (HTTPS requerido)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = {
                            baseUrlInput = it
                            val valid = viewModel.setGenericBaseUrl(it)
                            baseUrlError = !valid
                        },
                        isError = baseUrlError,
                        placeholder = { Text("https://api.openai.com/v1", color = AuraTextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyan,
                            unfocusedBorderColor = AuraCardBorder,
                            focusedTextColor = AuraTextPrimary,
                            unfocusedTextColor = AuraTextPrimary,
                            focusedContainerColor = AuraSurfaceLightDark,
                            unfocusedContainerColor = AuraSurfaceLightDark
                        )
                    )
                    if (baseUrlError) {
                        Text(
                            text = "Debe iniciar con https:// (o http://localhost para depuración).",
                            color = AuraError,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // 4. Nombre del Modelo configurable
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modelo de IA",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextSecondary
                    )
                    // Sugerencia rápida
                    val suggested = if (aiSettings.externalService == AIExternalService.GEMINI) {
                        AISettingsManager.DEFAULT_GEMINI_MODEL
                    } else {
                        AISettingsManager.DEFAULT_GENERIC_MODEL
                    }
                    Text(
                        text = "Sugerido: $suggested",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuraCyan,
                        modifier = Modifier.clickable {
                            modelInput = suggested
                            viewModel.setAIModel(suggested)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = modelInput,
                    onValueChange = {
                        modelInput = it
                        viewModel.setAIModel(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyan,
                        unfocusedBorderColor = AuraCardBorder,
                        focusedTextColor = AuraTextPrimary,
                        unfocusedTextColor = AuraTextPrimary,
                        focusedContainerColor = AuraSurfaceLightDark,
                        unfocusedContainerColor = AuraSurfaceLightDark
                    )
                )

                // 5. Clave API enmascarada con Android KeyStore
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Clave API (Cifrada con Keystore)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = {
                        Text(
                            text = if (aiSettings.hasSavedApiKey) "•••••••••••••••• (Clave guardada)" else "Pega tu clave API aquí…",
                            color = AuraTextMuted,
                            fontSize = 12.sp
                        )
                    },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Ocultar" else "Mostrar temporalmente",
                                tint = AuraTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyan,
                        unfocusedBorderColor = AuraCardBorder,
                        focusedTextColor = AuraTextPrimary,
                        unfocusedTextColor = AuraTextPrimary,
                        focusedContainerColor = AuraSurfaceLightDark,
                        unfocusedContainerColor = AuraSurfaceLightDark
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (apiKeyInput.isNotBlank()) {
                                viewModel.saveApiKey(apiKeyInput)
                                apiKeyInput = "" // Seguridad: Limpiar entrada inmediatamente
                                showApiKey = false
                            }
                        },
                        enabled = apiKeyInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = AuraBgDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar", color = AuraBgDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (aiSettings.hasSavedApiKey) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteApiKey()
                                apiKeyInput = ""
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraError),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = AuraError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Borrar", color = AuraError, fontSize = 12.sp)
                        }
                    }
                }

                // Indicador de estado de clave
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (aiSettings.hasSavedApiKey) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (aiSettings.hasSavedApiKey) AuraSuccess else AuraTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (aiSettings.hasSavedApiKey) "Clave guardada de forma segura (Android KeyStore)" else "Sin clave configurada para este servicio",
                        color = if (aiSettings.hasSavedApiKey) AuraSuccess else AuraTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 7. Botón "Probar conexión"
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.testAIConnection() },
                    enabled = !aiSettings.isTestingConnection && aiSettings.hasSavedApiKey,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (aiSettings.isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AuraCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificando conexión…", fontSize = 12.sp, color = AuraCyan)
                    } else {
                        Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probar conexión con IA", fontSize = 12.sp, color = AuraCyan)
                    }
                }

                // Banner de resultado de prueba
                aiSettings.connectionTestResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val (bannerBg, bannerBorder, bannerIcon, bannerText) = when (result) {
                        ConnectionTestResult.Conectado ->
                            Quadruple(AuraSuccess.copy(alpha = 0.12f), AuraSuccess, Icons.Default.CheckCircle, "Conectado correctamente con el servicio de IA.")
                        ConnectionTestResult.ClaveInvalida ->
                            Quadruple(AuraWarning.copy(alpha = 0.12f), AuraWarning, Icons.Default.Warning, "La clave no fue aceptada. Verifica tus credenciales.")
                        ConnectionTestResult.SinInternet ->
                            Quadruple(AuraError.copy(alpha = 0.12f), AuraError, Icons.Default.Warning, "Sin conexión a internet. Los comandos locales siguen activos.")
                        ConnectionTestResult.TiempoAgotado ->
                            Quadruple(AuraWarning.copy(alpha = 0.12f), AuraWarning, Icons.Default.Warning, "Tiempo de espera agotado al conectar.")
                        is ConnectionTestResult.Error ->
                            Quadruple(AuraError.copy(alpha = 0.12f), AuraError, Icons.Default.Warning, result.message)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, bannerBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        color = bannerBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = bannerIcon, contentDescription = null, tint = bannerBorder, modifier = Modifier.size(16.dp))
                            Text(text = bannerText, style = MaterialTheme.typography.bodySmall, color = AuraTextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 6. Memoria compartible con IA externa
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = AuraCardBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Memoria compartible con IA externa",
                style = MaterialTheme.typography.labelLarge,
                color = AuraTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Si activas una categoría, su contenido se envía al proveedor de IA cuando sea relevante. Todas desactivadas por defecto.",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextMuted,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MemoryCategory.entries.forEach { category ->
                    val isChecked = aiSettings.allowedMemoryCategories.contains(category)
                    SettingsRow(
                        title = category.title,
                        subtitle = category.description,
                        trailingContent = {
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { viewModel.toggleMemoryCategory(category, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AuraBgDark,
                                    checkedTrackColor = AuraCyan
                                )
                            )
                        }
                    )
                }
            }

            // 8. Botón "Borrar historial de conversación"
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = AuraCardBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { viewModel.clearConversationHistory() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraError),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = AuraError, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Borrar historial de conversación", fontSize = 12.sp, color = AuraError)
            }

            // 9. Aviso de privacidad visible
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AuraViolet.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                color = AuraSurfaceLightDark,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = AuraViolet, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aviso de Privacidad y Seguridad", color = AuraViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Qué se envía: Únicamente tu consulta actual y los recuerdos de categorías explícitamente activadas.\n" +
                                "• A quién: Al proveedor seleccionado (Google Gemini o tu endpoint HTTPS).\n" +
                                "• Qué NUNCA se envía: Contraseñas, claves, credenciales, tokens, identificadores de contacto ni datos bancarios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.5.dp else 0.8.dp,
                color = if (selected) AuraCyan else AuraCardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        color = if (selected) AuraSurfaceLightDark else AuraSurfaceDark,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AuraCyan,
                    unselectedColor = AuraTextMuted
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) AuraCyan else AuraTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextMuted,
                    lineHeight = 15.sp,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

