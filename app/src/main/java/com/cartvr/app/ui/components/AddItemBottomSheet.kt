package com.cartvr.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.StoreEntity
import com.cartvr.app.domain.SmartItemParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemBottomSheet(
    stores: List<StoreEntity>,
    categories: List<CategoryEntity>,
    initialStoreId: String?,
    editingItem: ShoppingItemEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        storeId: String,
        categoryId: String?,
        name: String,
        quantity: String?,
        note: String?,
        saveAsRegular: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var rawInput by remember { mutableStateOf(editingItem?.name ?: "") }
    var quantity by remember { mutableStateOf(editingItem?.quantity ?: "") }
    var note by remember { mutableStateOf(editingItem?.note ?: "") }
    var selectedStoreId by remember {
        mutableStateOf(editingItem?.storeId ?: initialStoreId ?: stores.firstOrNull()?.id.orEmpty())
    }
    var selectedCategoryId by remember {
        mutableStateOf(editingItem?.categoryId ?: categories.lastOrNull()?.id)
    }
    var userManuallyPickedCategory by remember { mutableStateOf(editingItem != null) }
    var saveAsRegular by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editingItem == null) "Add Item" else "Edit Item",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = rawInput,
                onValueChange = { input ->
                    rawInput = input
                    if (editingItem == null) {
                        val parsed = SmartItemParser.parse(input, categories)
                        if (parsed.quantity != null && quantity.isBlank()) {
                            quantity = parsed.quantity
                        }
                        if (!userManuallyPickedCategory && parsed.suggestedCategoryId != null) {
                            selectedCategoryId = parsed.suggestedCategoryId
                        }
                    }
                },
                label = { Text("Item (e.g. 2L full cream milk)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Qty (optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(0.4f),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. on special)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (rawInput.isNotBlank() && selectedStoreId.isNotBlank()) {
                                val finalParsed = if (editingItem == null) {
                                    SmartItemParser.parse(rawInput, categories)
                                } else null
                                val finalName = if (editingItem == null && finalParsed != null && finalParsed.quantity != null) {
                                    finalParsed.name
                                } else rawInput.trim()
                                val finalQty = quantity.ifBlank { finalParsed?.quantity ?: "" }.ifBlank { null }
                                onSave(selectedStoreId, selectedCategoryId, finalName, finalQty, note.ifBlank { null }, saveAsRegular)
                            }
                        }
                    ),
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Text(
                text = "Store",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stores.forEach { store ->
                    FilterChip(
                        selected = store.id == selectedStoreId,
                        onClick = { selectedStoreId = store.id },
                        label = { Text(store.name) }
                    )
                }
            }

            Text(
                text = "Aisle / Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = category.id == selectedCategoryId,
                        onClick = {
                            selectedCategoryId = category.id
                            userManuallyPickedCategory = true
                        },
                        label = { Text(category.name) }
                    )
                }
            }

            if (editingItem == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = saveAsRegular,
                        onCheckedChange = { saveAsRegular = it }
                    )
                    Text(
                        text = "Also save to Regulars (Quick Add)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val finalParsed = if (editingItem == null) {
                        SmartItemParser.parse(rawInput, categories)
                    } else null
                    val finalName = if (editingItem == null && finalParsed != null && finalParsed.quantity != null) {
                        finalParsed.name
                    } else rawInput.trim()
                    val finalQty = quantity.ifBlank { finalParsed?.quantity ?: "" }.ifBlank { null }
                    onSave(
                        selectedStoreId,
                        selectedCategoryId,
                        finalName,
                        finalQty,
                        note.ifBlank { null },
                        saveAsRegular
                    )
                },
                enabled = rawInput.isNotBlank() && selectedStoreId.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (editingItem == null) "Add to List" else "Save Changes")
            }
        }
    }
}
