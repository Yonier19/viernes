package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memory.LearningSuggestion
import com.example.memory.MemoryEntity
import com.example.memory.MemoryType
import com.example.ui.theme.AuraBgDark
import com.example.ui.theme.AuraCardBorder
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraSurfaceDark
import com.example.ui.theme.AuraSurfaceLightDark
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraViolet
import com.example.utils.AppUtils

enum class MemoryCategoryTab(val title: String, val icon: String, val memoryType: MemoryType?) {
    ALL("Todas", "🧠", null),
    PREFERENCES("Preferencias", "❤️", MemoryType.PREFERENCE),
    CONTACTS("Contactos", "👤", MemoryType.CONTACT_ALIAS),
    COMMANDS("Comandos", "⚡", MemoryType.COMMAND_ALIAS),
    HABITS("Hábitos", "🔄", MemoryType.HABIT),
    ROUTINES("Rutinas", "🤖", MemoryType.ROUTINE)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val memories by viewModel.activeMemories.collectAsState()
    val habitSuggestions by viewModel.habitSuggestions.collectAsState()

    var selectedTab by remember { mutableStateOf(MemoryCategoryTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    var memoryToDelete by remember { mutableStateOf<MemoryEntity?>(null) }
    var memoryToEdit by remember { mutableStateOf<MemoryEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredMemories = remember(memories, selectedTab, searchQuery) {
        memories.filter { mem ->
            val matchesCategory = when (selectedTab) {
                MemoryCategoryTab.ALL -> true
                MemoryCategoryTab.PREFERENCES -> mem.type == MemoryType.PREFERENCE || mem.type == MemoryType.PERSONAL_INFORMATION
                MemoryCategoryTab.CONTACTS -> mem.type == MemoryType.CONTACT_ALIAS
                MemoryCategoryTab.COMMANDS -> mem.type == MemoryType.COMMAND_ALIAS
                MemoryCategoryTab.HABITS -> mem.type == MemoryType.HABIT
                MemoryCategoryTab.ROUTINES -> mem.type == MemoryType.ROUTINE
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                mem.key.contains(searchQuery, ignoreCase = true) ||
                        mem.value.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AuraBgDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AuraBgDark),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = AuraViolet,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "MI MEMORIA",
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("memory_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AuraCyan
                        )
                    }
                },
                actions = {
                    if (memories.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier.testTag("delete_all_memories_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Eliminar toda la memoria",
                                tint = AuraError
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AuraViolet,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_memory_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir recuerdo")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar: "Buscar en mi memoria..."
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar en mi memoria...", color = AuraTextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = AuraTextMuted)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("search_memory_field"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuraCyan,
                    unfocusedBorderColor = AuraCardBorder,
                    focusedTextColor = AuraTextPrimary,
                    unfocusedTextColor = AuraTextPrimary,
                    focusedContainerColor = AuraSurfaceDark,
                    unfocusedContainerColor = AuraSurfaceDark
                ),
                singleLine = true
            )

            // Category Tabs: Preferencias, Contactos, Comandos, Hábitos, Rutinas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemoryCategoryTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AuraViolet else AuraSurfaceDark,
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isSelected) AuraViolet else AuraCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = tab }
                    ) {
                        Text(
                            text = "${tab.icon} ${tab.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else AuraTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Summary Counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredMemories.size} registros en ${selectedTab.title.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraTextSecondary
                )
                Text(
                    text = "Room SQLite • Control Local",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraCyan
                )
            }

            // Content List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // In HABITS tab: Display detected learning suggestions from LearningEngine
                if (selectedTab == MemoryCategoryTab.HABITS || selectedTab == MemoryCategoryTab.ALL) {
                    if (habitSuggestions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Sugerencias de Aprendizaje Detectadas",
                                style = MaterialTheme.typography.labelMedium,
                                color = AuraCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(habitSuggestions, key = { "suggestion_${it.phrase}" }) { suggestion ->
                            HabitSuggestionCard(
                                suggestion = suggestion,
                                onAccept = { viewModel.acceptHabitSuggestion(suggestion) },
                                onDismiss = { viewModel.dismissHabitSuggestion(suggestion) }
                            )
                        }
                    }
                }

                if (filteredMemories.isEmpty() && (selectedTab != MemoryCategoryTab.HABITS || habitSuggestions.isEmpty())) {
                    item {
                        EmptyMemoryPlaceholder(searchQuery = searchQuery, tab = selectedTab)
                    }
                } else {
                    items(filteredMemories, key = { it.id }) { mem ->
                        MemoryItemCard(
                            memory = mem,
                            onEdit = { memoryToEdit = mem },
                            onDelete = { memoryToDelete = mem }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog before deleting an individual memory item
    if (memoryToDelete != null) {
        val target = memoryToDelete!!
        AlertDialog(
            onDismissRequest = { memoryToDelete = null },
            containerColor = AuraSurfaceDark,
            title = {
                Text(
                    text = "¿Seguro que quieres olvidar esta información?",
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "AURA eliminará de su memoria: \"${target.key.replace("_", " ")}: ${target.value}\".",
                    color = AuraTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMemory(target)
                        memoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraError)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { memoryToDelete = null }) {
                    Text("Cancelar", color = AuraTextSecondary)
                }
            }
        )
    }

    // Dialog for Editing a Memory Item
    if (memoryToEdit != null) {
        val target = memoryToEdit!!
        var editValue by remember { mutableStateOf(target.value) }

        AlertDialog(
            onDismissRequest = { memoryToEdit = null },
            containerColor = AuraSurfaceDark,
            title = {
                Text(
                    text = "Modificar recuerdo",
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Concepto: ${target.key.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraCyan
                    )
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text("Nuevo valor") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyan,
                            unfocusedBorderColor = AuraCardBorder,
                            focusedTextColor = AuraTextPrimary,
                            unfocusedTextColor = AuraTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editValue.isNotBlank()) {
                            viewModel.updateMemory(target.copy(value = editValue.trim()))
                            memoryToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan)
                ) {
                    Text("Guardar cambios", color = AuraBgDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memoryToEdit = null }) {
                    Text("Cancelar", color = AuraTextSecondary)
                }
            }
        )
    }

    // Double-confirmation Dialog for Clearing All Memories
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = AuraSurfaceDark,
            title = {
                Text(
                    text = "¿Eliminar toda la memoria?",
                    color = AuraError,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Esta acción eliminará todos los recuerdos, preferencias, alias de contactos y rutinas locales. ¿Confirmas la eliminación total?",
                    color = AuraTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMemories()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraError)
                ) {
                    Text("Eliminar todo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancelar", color = AuraTextSecondary)
                }
            }
        )
    }

    // Dialog to Add a Memory Manually
    if (showAddDialog) {
        var keyInput by remember { mutableStateOf("") }
        var valueInput by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(MemoryType.PREFERENCE) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = AuraSurfaceDark,
            title = {
                Text(
                    text = "Añadir Nuevo Recuerdo",
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Type selector chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(MemoryType.PREFERENCE, MemoryType.CONTACT_ALIAS, MemoryType.COMMAND_ALIAS, MemoryType.ROUTINE).forEach { type ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedType == type) AuraViolet else AuraSurfaceLightDark,
                                modifier = Modifier.clickable { selectedType = type }
                            ) {
                                Text(
                                    text = type.name,
                                    fontSize = 10.sp,
                                    color = if (selectedType == type) Color.White else AuraTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Clave / Concepto (ej. preferred_browser)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyan,
                            unfocusedBorderColor = AuraCardBorder,
                            focusedTextColor = AuraTextPrimary,
                            unfocusedTextColor = AuraTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = valueInput,
                        onValueChange = { valueInput = it },
                        label = { Text("Valor (ej. Chrome)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyan,
                            unfocusedBorderColor = AuraCardBorder,
                            focusedTextColor = AuraTextPrimary,
                            unfocusedTextColor = AuraTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyInput.isNotBlank() && valueInput.isNotBlank()) {
                            viewModel.saveMemoryItem(keyInput.trim(), valueInput.trim(), selectedType)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan)
                ) {
                    Text("Guardar", color = AuraBgDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = AuraTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MemoryItemCard(
    memory: MemoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AuraCardBorder, RoundedCornerShape(12.dp)),
        color = AuraSurfaceDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AuraViolet.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = memory.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = AuraViolet,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (memory.confidence < 1.0f) {
                        Text(
                            text = "${(memory.confidence * 100).toInt()}% conf.",
                            style = MaterialTheme.typography.labelSmall,
                            color = AuraCyan,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = memory.key.replace("preferred_", "").replace("_", " ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    color = AuraCyan,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = memory.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuraTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Actualizado: ${AppUtils.formatTimestamp(memory.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraTextMuted,
                    fontSize = 10.sp
                )
            }

            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar recuerdo",
                        tint = AuraTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_memory_${memory.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Olvidar recuerdo",
                        tint = AuraTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitSuggestionCard(
    suggestion: LearningSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AuraCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        color = AuraSurfaceLightDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Patrón Detectado (${(suggestion.confidence * 100).toInt()}% confianza)",
                    style = MaterialTheme.typography.labelMedium,
                    color = AuraCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${suggestion.occurrences} veces",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraTextMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "He notado que normalmente abres ${suggestion.target} cuando dices \"${suggestion.phrase}\". ¿Quieres que aprenda este comando?",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextPrimary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AuraBgDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aprender", color = AuraBgDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AuraTextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("No aprender", color = AuraTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyMemoryPlaceholder(searchQuery: String, tab: MemoryCategoryTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = AuraCardBorder,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No se encontraron coincidencias" else "Sin registros en ${tab.title}",
                color = AuraTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dile: \"Recuerda que mi navegador favorito es Chrome\", \"Cuando diga mamá me refiero a María\" o \"Cuando diga pon música abre Spotify\".",
                color = AuraTextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
