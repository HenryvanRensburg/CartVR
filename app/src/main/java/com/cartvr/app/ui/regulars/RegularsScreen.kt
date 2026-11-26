package com.cartvr.app.ui.regulars

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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
    onAddToShoppingList: (RegularItemEntity) -> Unit,
    onCreateRegular: (name: String, qty: String?, listId: String?, categoryId: String?) -> Unit,
    onDeleteRegular: (RegularItemEntity) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    val listMap = remember(lists) { lists.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Regulars",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Weekly staples for one-tap adding",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(" New")
                }
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
                            text = "Save items you buy every week (like milk, bread, or rusks) for one-tap re-adding.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(regulars, key = { it.id }) { regular ->
            val targetListId = regular.defaultListId ?: lists.firstOrNull()?.id
            val listName = listMap[regular.defaultListId]?.name ?: lists.firstOrNull()?.name ?: "Default List"
            val categoryName = categoryMap[regular.defaultCategoryId]?.name
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                        val subtitle = listOfNotNull(listName, categoryName).joinToString(" • ")
                        Text(
                            text = subtitle,
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

                    IconButton(onClick = { onDeleteRegular(regular) }) {
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

    if (showCreateDialog) {
        val existingRegularNames = remember(regulars) {
            regulars.map { it.name.trim().replace(Regex("\\s+"), " ").lowercase() }.toSet()
        }
        CreateRegularDialog(
            lists = lists,
            categories = categories,
            existingRegularNames = existingRegularNames,
            onDismiss = { showCreateDialog = false },
            onSave = { name, qty, listId, categoryId ->
                onCreateRegular(name, qty, listId, categoryId)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateRegularDialog(
    lists: List<ShoppingListEntity>,
    categories: List<CategoryEntity>,
    existingRegularNames: Set<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: String?, listId: String?, categoryId: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectedListId by remember { mutableStateOf(lists.firstOrNull()?.id) }
    var selectedCategoryId by remember { mutableStateOf(categories.lastOrNull()?.id) }
    val normalizedInput = name.trim().replace(Regex("\\s+"), " ").lowercase()
    val isDuplicate = normalizedInput.isNotEmpty() && normalizedInput in existingRegularNames

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Regular Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name (e.g. Full cream milk)") },
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
