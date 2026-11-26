package com.cartvr.app.ui.list

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.ItemHistoryEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.domain.SmartItemParser
import com.cartvr.app.ui.components.EditItemSheet

private data class SuggestionCandidate(
    val displayName: String,
    val defaultQuantity: String?,
    val categoryId: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    list: ShoppingListEntity,
    isOwner: Boolean,
    memberCount: Int,
    items: List<ShoppingItemEntity>,
    categories: List<CategoryEntity>,
    history: List<ItemHistoryEntity>,
    regulars: List<RegularItemEntity>,
    onBack: () -> Unit,
    onQuickAddItem: (name: String, quantity: String?, note: String?, categoryId: String?) -> Unit,
    onToggleChecked: (ShoppingItemEntity) -> Unit,
    onUpdateItem: (updated: ShoppingItemEntity, categoryChanged: Boolean) -> Unit,
    onDeleteItem: (ShoppingItemEntity) -> Unit,
    onClearCompleted: () -> Unit,
    onSaveCategoryOrder: (List<CategoryEntity>) -> Unit,
    onShareList: () -> Unit,
    onMakePrivate: () -> Unit,
    onDeleteList: () -> Unit,
    onLeaveList: () -> Unit
) {
    var completedExpanded by remember { mutableStateOf(true) }
    var showReorderDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMakePrivateConfirm by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    // Bottom Quick-Add bar state
    var inputText by remember { mutableStateOf("") }
    var showExtrasRow by remember { mutableStateOf(false) }
    var extraQty by remember { mutableStateOf("") }
    var extraNote by remember { mutableStateOf("") }

    val unchecked = items.filter { !it.isChecked }
    val checked = items.filter { it.isChecked }

    val groupedUnchecked = remember(unchecked, categories) {
        val grouped = unchecked.groupBy { it.categoryId }
        buildList {
            categories.forEach { cat ->
                val listInCat = grouped[cat.id].orEmpty()
                if (listInCat.isNotEmpty()) add(cat to listInCat)
            }
            val uncategorized = grouped[null].orEmpty()
            if (uncategorized.isNotEmpty()) add(null to uncategorized)
        }
    }

    val suggestions = remember(inputText, history, regulars) {
        val query = inputText.trim().lowercase()
        if (query.isEmpty()) {
            emptyList()
        } else {
            val fromHistory = history
                .filter { it.normalizedName.contains(query) }
                .map {
                    SuggestionCandidate(
                        displayName = it.displayName,
                        defaultQuantity = it.defaultQuantity,
                        categoryId = it.defaultCategoryId
                    )
                }
            val fromRegulars = regulars
                .filter { it.name.lowercase().contains(query) }
                .map {
                    SuggestionCandidate(
                        displayName = it.name,
                        defaultQuantity = it.defaultQuantity,
                        categoryId = it.defaultCategoryId
                    )
                }
            (fromHistory + fromRegulars)
                .distinctBy { it.displayName.lowercase() }
                .take(8)
        }
    }

    fun submitQuickAdd() {
        val raw = inputText.trim()
        if (raw.isEmpty()) return
        val parsed = SmartItemParser.parse(raw, categories, history)
        val finalQty = extraQty.trim().ifBlank { null } ?: parsed.quantity
        val finalNote = extraNote.trim().ifBlank { null } ?: parsed.note
        val fallbackCategoryId = parsed.suggestedCategoryId
            ?: categories.firstOrNull { it.name.equals("Other", ignoreCase = true) }?.id
            ?: categories.lastOrNull()?.id

        onQuickAddItem(parsed.name, finalQty, finalNote, fallbackCategoryId)
        inputText = ""
        extraQty = ""
        extraNote = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val visibilityText = if (list.isShared) "Shared ($memberCount)" else "Private"
                        Text(
                            text = "$visibilityText • ${unchecked.size} left",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShareList) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = if (list.isShared) "Send Invite Link" else "Share List"
                        )
                    }
                    IconButton(onClick = { showReorderDialog = true }) {
                        Icon(Icons.Rounded.Reorder, contentDescription = "Reorder Aisles")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (checked.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Clear Completed") },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onClearCompleted()
                                    }
                                )
                            }
                            if (isOwner) {
                                if (list.isShared) {
                                    DropdownMenuItem(
                                        text = { Text("Make Private") },
                                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            showMakePrivateConfirm = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete List", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteConfirm = true
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Leave List", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.ExitToApp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showLeaveConfirm = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            BottomQuickAddBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                showExtrasRow = showExtrasRow,
                onToggleExtras = { showExtrasRow = !showExtrasRow },
                extraQty = extraQty,
                onExtraQtyChange = { extraQty = it },
                extraNote = extraNote,
                onExtraNoteChange = { extraNote = it },
                suggestions = suggestions,
                onSuggestionClick = { suggestion ->
                    val resolvedCategory = suggestion.categoryId
                        ?: SmartItemParser.parse(suggestion.displayName, categories, history).suggestedCategoryId
                        ?: categories.lastOrNull()?.id
                    onQuickAddItem(
                        suggestion.displayName,
                        extraQty.trim().ifBlank { null } ?: suggestion.defaultQuantity,
                        extraNote.trim().ifBlank { null },
                        resolvedCategory
                    )
                    inputText = ""
                    extraQty = ""
                    extraNote = ""
                },
                onSubmit = { submitQuickAdd() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (unchecked.isEmpty() && checked.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${list.name} is empty",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Type below (e.g. \"2x Milk - low fat\") and press Enter to add items.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            groupedUnchecked.forEach { (category, catItems) ->
                item(key = "header_${category?.id ?: "other"}") {
                    Text(
                        text = category?.name ?: "Other",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                items(catItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onToggle = { onToggleChecked(item) },
                        onClickText = { editingItem = item },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }

            if (checked.isNotEmpty()) {
                item(key = "completed_header") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { completedExpanded = !completedExpanded }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (completedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Completed (${checked.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = onClearCompleted) {
                            Text("Clear Completed")
                        }
                    }
                }

                if (completedExpanded) {
                    items(checked, key = { it.id }) { item ->
                        AnimatedVisibility(visible = true) {
                            ShoppingItemRow(
                                item = item,
                                onToggle = { onToggleChecked(item) },
                                onClickText = { editingItem = item },
                                onDelete = { onDeleteItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { current ->
        EditItemSheet(
            item = current,
            categories = categories,
            onDismiss = { editingItem = null },
            onSave = { updated, categoryChanged ->
                onUpdateItem(updated, categoryChanged)
                editingItem = null
            }
        )
    }

    if (showReorderDialog) {
        ReorderAislesDialog(
            categories = categories,
            onDismiss = { showReorderDialog = false },
            onSave = { reordered ->
                onSaveCategoryOrder(reordered)
                showReorderDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${list.name}\"?") },
            text = {
                Text(
                    if (list.isShared) {
                        "This will permanently delete this shared list and all its items for everyone."
                    } else {
                        "This will permanently delete this private list and all its items."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showMakePrivateConfirm) {
        AlertDialog(
            onDismissRequest = { showMakePrivateConfirm = false },
            title = { Text("Make \"${list.name}\" Private?") },
            text = { Text("Other members will lose access and this list will stay only on your phone.") },
            confirmButton = {
                Button(onClick = {
                    showMakePrivateConfirm = false
                    onMakePrivate()
                }) { Text("Make Private") }
            },
            dismissButton = {
                TextButton(onClick = { showMakePrivateConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave \"${list.name}\"?") },
            text = { Text("The list will be removed from your phone, while other members keep it.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirm = false
                        onLeaveList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Leave List") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BottomQuickAddBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    showExtrasRow: Boolean,
    onToggleExtras: () -> Unit,
    extraQty: String,
    onExtraQtyChange: (String) -> Unit,
    extraNote: String,
    onExtraNoteChange: (String) -> Unit,
    suggestions: List<SuggestionCandidate>,
    onSuggestionClick: (SuggestionCandidate) -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Smart Suggestions Strip
            if (suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        val label = if (!suggestion.defaultQuantity.isNullOrBlank()) {
                            "${suggestion.defaultQuantity} ${suggestion.displayName}"
                        } else {
                            suggestion.displayName
                        }
                        AssistChip(
                            onClick = { onSuggestionClick(suggestion) },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Optional Inline Qty + Note Row
            AnimatedVisibility(visible = showExtrasRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = extraQty,
                        onValueChange = onExtraQtyChange,
                        placeholder = { Text("Qty (e.g. 2x)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.36f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = extraNote,
                        onValueChange = onExtraNoteChange,
                        placeholder = { Text("Note (e.g. low-fat)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.64f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Main Always-Open Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onToggleExtras,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Qty and note",
                        tint = if (showExtrasRow) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { Text("Add item (e.g. 2x Milk - low fat)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )

                FilledIconButton(
                    onClick = onSubmit,
                    enabled = inputText.isNotBlank(),
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add item")
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemEntity,
    onToggle: () -> Unit,
    onClickText: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isChecked) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox ticks item
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (item.isChecked) {
                        Icons.Rounded.CheckBox
                    } else {
                        Icons.Rounded.CheckBoxOutlineBlank
                    },
                    contentDescription = if (item.isChecked) "Uncheck" else "Tick off",
                    tint = if (item.isChecked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            // Text area opens quick edit
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClickText)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!item.quantity.isNullOrBlank()) {
                        Text(
                            text = item.quantity,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isChecked) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!item.note.isNullOrBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReorderAislesDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (List<CategoryEntity>) -> Unit
) {
    val workingList = remember(categories) {
        mutableStateListOf<CategoryEntity>().apply { addAll(categories) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reorder Aisles") },
        text = {
            LazyColumn(
                modifier = Modifier.height(340.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(workingList.size, key = { workingList[it].id }) { index ->
                    val cat = workingList[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${index + 1}. ${cat.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val item = workingList.removeAt(index)
                                        workingList.add(index - 1, item)
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                            }
                            IconButton(
                                onClick = {
                                    if (index < workingList.lastIndex) {
                                        val item = workingList.removeAt(index)
                                        workingList.add(index + 1, item)
                                    }
                                },
                                enabled = index < workingList.lastIndex
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(workingList.toList()) }) {
                Text("Save Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
