package com.cartvr.app.ui.regulars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity

@Composable
fun RegularsScreen(
    regulars: List<RegularItemEntity>,
    lists: List<ShoppingListEntity>,
    allItems: List<ShoppingItemEntity>,
    categories: List<CategoryEntity>,
    isAdmin: Boolean,
    onAddToShoppingList: (RegularItemEntity) -> Unit,
    onSaveRegular: (name: String, qty: String?, listId: String?, categoryId: String?, existingId: String?) -> Unit,
    onDeleteRegular: (RegularItemEntity) -> Unit,
    embeddedInShoppingHub: Boolean = false
) {
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingRegular by remember { mutableStateOf<RegularItemEntity?>(null) }
    var confirmDeleteTarget by remember { mutableStateOf<RegularItemEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val listMap = remember(lists) { lists.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val groupedRegulars = remember(regulars, categories, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            regulars
        } else {
            regulars.filter { reg ->
                val catName = categoryMap[reg.defaultCategoryId]?.name?.lowercase().orEmpty()
                reg.name.lowercase().contains(q) || catName.contains(q)
            }
        }
        val grouped = filtered.groupBy { it.defaultCategoryId }
        buildList {
            categories.forEach { cat ->
                val itemsInCat = grouped[cat.id].orEmpty().sortedBy { it.name.lowercase() }
                if (itemsInCat.isNotEmpty()) add(cat to itemsInCat)
            }
            val knownIds = categories.map { it.id }.toSet()
            val uncategorized = filtered
                .filter { it.defaultCategoryId == null || it.defaultCategoryId !in knownIds }
                .sortedBy { it.name.lowercase() }
            if (uncategorized.isNotEmpty()) add(null to uncategorized)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = if (embeddedInShoppingHub) 6.dp else 16.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (!embeddedInShoppingHub) {
                        Text(
                            text = "Regulars",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "${regulars.size} staples • sorted by aisle A–Z",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isAdmin) {
                    OutlinedButton(
                        onClick = {
                            editingRegular = null
                            showEditorDialog = true
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(" New")
                    }
                }
            }
        }

        if (regulars.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search regulars (e.g. melk, brood, wors)...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        if (regulars.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No regulars yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Weekly staples (like melk, brood, or beskuit) will appear here for one-tap adding.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (groupedRegulars.isEmpty()) {
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
                            text = "No matches for \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        groupedRegulars.forEach { (category, catRegulars) ->
            item(key = "reg_header_${category?.id ?: "other"}") {
                Text(
                    text = category?.name ?: "Other",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            items(catRegulars, key = { it.id }) { regular ->
                val targetListId = regular.defaultListId ?: lists.firstOrNull()?.id
                val listName = listMap[regular.defaultListId]?.name ?: lists.firstOrNull()?.name ?: "Default List"
                val normalizedRegName = regular.name.trim().replace(Regex("\\s+"), " ").lowercase()
                val isOnList = targetListId != null && allItems.any {
                    it.listId == targetListId &&
                        !it.isChecked &&
                        it.name.trim().replace(Regex("\\s+"), " ").lowercase() == normalizedRegName
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isAdmin) {
                                        Modifier.clickable {
                                            editingRegular = regular
                                            showEditorDialog = true
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            val title = if (!regular.defaultQuantity.isNullOrBlank()) {
                                "${regular.defaultQuantity} ${regular.name}"
                            } else {
                                regular.name
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isOnList) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "On list",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = listName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { onAddToShoppingList(regular) },
                            enabled = lists.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = "Add to shopping list"
                            )
                        }

                        if (isAdmin) {
                            IconButton(
                                onClick = {
                                    editingRegular = regular
                                    showEditorDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit regular",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { confirmDeleteTarget = regular }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete regular",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog && isAdmin) {
        val current = editingRegular
        val existingRegularNames = remember(regulars, current?.id) {
            regulars
                .filter { it.id != current?.id }
                .map { it.name.trim().replace(Regex("\\s+"), " ").lowercase() }
                .toSet()
        }
        RegularEditorDialog(
            editingRegular = current,
            lists = lists,
            categories = categories,
            existingRegularNames = existingRegularNames,
            onDismiss = {
                showEditorDialog = false
                editingRegular = null
            },
            onSave = { name, qty, listId, categoryId ->
                onSaveRegular(name, qty, listId, categoryId, current?.id)
                showEditorDialog = false
                editingRegular = null
            }
        )
    }

    confirmDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteTarget = null },
            title = { Text("Delete \"${target.name}\"?") },
            text = { Text("This will remove \"${target.name}\" from your family's Regulars list.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRegular(target)
                        confirmDeleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RegularEditorDialog(
    editingRegular: RegularItemEntity?,
    lists: List<ShoppingListEntity>,
    categories: List<CategoryEntity>,
    existingRegularNames: Set<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: String?, listId: String?, categoryId: String?) -> Unit
) {
    var name by remember(editingRegular?.id) { mutableStateOf(editingRegular?.name ?: "") }
    var quantity by remember(editingRegular?.id) { mutableStateOf(editingRegular?.defaultQuantity ?: "") }
    var selectedListId by remember(editingRegular?.id) {
        mutableStateOf(editingRegular?.defaultListId ?: lists.firstOrNull()?.id)
    }
    var selectedCategoryId by remember(editingRegular?.id) {
        mutableStateOf(editingRegular?.defaultCategoryId ?: categories.lastOrNull()?.id)
    }
    val normalizedInput = name.trim().replace(Regex("\\s+"), " ").lowercase()
    val isDuplicate = normalizedInput.isNotEmpty() && normalizedInput in existingRegularNames

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingRegular == null) "New Regular Item" else "Edit Regular Item")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name (e.g. Melk)") },
                    singleLine = true,
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text("Already in Regulars") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Default quantity (e.g. 2L)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (lists.isNotEmpty()) {
                    Text("Default List", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        lists.forEach { list ->
                            FilterChip(
                                selected = list.id == selectedListId,
                                onClick = { selectedListId = list.id },
                                label = { Text(list.name) }
                            )
                        }
                    }
                }
                Text("Default Aisle", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = cat.id == selectedCategoryId,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        quantity.trim().ifBlank { null },
                        selectedListId,
                        selectedCategoryId
                    )
                },
                enabled = name.isNotBlank() && !isDuplicate
            ) {
                Text(if (editingRegular == null) "Save" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
