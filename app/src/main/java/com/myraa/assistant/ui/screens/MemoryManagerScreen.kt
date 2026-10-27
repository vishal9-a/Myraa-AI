package com.myraa.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myraa.assistant.memory.FactMemory
import com.myraa.assistant.memory.MemoryCategory
import com.myraa.assistant.memory.MemoryStore
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.nightSky
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryManagerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current
    val memoryStore = remember { MemoryStore.get(context) }
    val facts by memoryStore.factsFlow.collectAsState()
    val episodes by memoryStore.episodicFlow.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<MemoryCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filteredFacts = remember(facts, searchQuery, selectedCategory) {
        facts.filter { fact ->
            val matchesCategory = selectedCategory == null || fact.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    fact.key.contains(searchQuery, ignoreCase = true) ||
                    fact.value.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Myraa's Memory (${facts.size})",
                onBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { scope.launch { memoryStore.consolidate() } },
                        modifier = Modifier.testTag("memory_consolidate_button")
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = "Consolidate memories",
                            tint = colors.accent
                        )
                    }
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.testTag("memory_clear_all_button")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Forget all",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.accent,
                contentColor = Color(0xFF0F172A),
                shape = CircleShape,
                modifier = Modifier.testTag("add_memory_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Remember new fact")
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier.nightSky()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search remembered facts, names, routines...", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.accent) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color.White)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0x331E293B),
                        unfocusedContainerColor = Color(0x220F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_search_input")
                )
            }

            item {
                // Category Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = { selectedCategory = null },
                        label = { Text("All (${facts.size})") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedCategory == null) colors.accent else Color(0x331E293B),
                            labelColor = if (selectedCategory == null) Color(0xFF0F172A) else Color.White
                        ),
                        border = null
                    )
                    MemoryCategory.values().forEach { cat ->
                        val count = facts.count { it.category == cat }
                        if (count > 0 || cat == MemoryCategory.PERSONAL || cat == MemoryCategory.PREFERENCE) {
                            AssistChip(
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text("${cat.name.lowercase().capitalize()} ($count)") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selectedCategory == cat) colors.accent else Color(0x331E293B),
                                    labelColor = if (selectedCategory == cat) Color(0xFF0F172A) else Color(0xFFE2E8F0)
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            if (episodes.isNotEmpty() && selectedCategory == null && searchQuery.isBlank()) {
                item {
                    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = colors.accentGlow)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Latest Episodic Memory (${episodes.first().dateIso})",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.accentGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            episodes.first().summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            if (filteredFacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No memories found matching this filter.\nMyraa automatically learns facts as you speak with her!",
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredFacts, key = { it.id }) { fact ->
                    MemoryItemCard(
                        fact = fact,
                        onDelete = {
                            scope.launch { memoryStore.deleteFact(fact.id) }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { key, value, category, importance ->
                scope.launch {
                    memoryStore.saveFact(key, value, category, importance)
                    showAddDialog = false
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Forget All Memories?", color = Color.White) },
            text = { Text("This will permanently wipe all facts, personal details, and episodic conversation summaries remembered by Myraa on this device.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            memoryStore.clearAllMemories()
                            showClearConfirm = false
                        }
                    }
                ) {
                    Text("Wipe All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun MemoryItemCard(
    fact: FactMemory,
    onDelete: () -> Unit
) {
    val colors = LocalNovaColors.current
    NovaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_item_${fact.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.accent.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = fact.category.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "★ ${fact.importance}/10",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = fact.key.replace("_", " ").capitalizeWords(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = fact.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE2E8F0)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_memory_${fact.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Forget fact",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (key: String, value: String, category: MemoryCategory, importance: Int) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MemoryCategory.PERSONAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remember Fact", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Topic / Key (e.g., Mom's Birthday)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Detail / Fact (e.g., May 14th, loves roses)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            NovaButton(
                text = "Save",
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onConfirm(key, value, category, 8)
                    }
                },
                enabled = key.isNotBlank() && value.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
