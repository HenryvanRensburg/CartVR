package com.cartvr.app.ui.scanner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.cartvr.app.data.local.entity.ItemHistoryEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.sync.BarcodeLookupResult
import com.cartvr.app.data.sync.BarcodeLookupSource
import com.cartvr.app.domain.SmartItemParser

/**
 * Confirmation and quick-edit sheet displayed after scanning a product barcode.
 *
 * Allows confirming or fixing the name/quantity/aisle (which also updates the household
 * `product_barcodes` library per Safeguard #4) and offers "Add & Scan Next" for fast multi-item scanning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedItemSheet(
    lookupResult: BarcodeLookupResult,
    lists: List<ShoppingListEntity>,
    initialListId: String?,
    categories: List<CategoryEntity>,
    history: List<ItemHistoryEntity>,
    isAdmin: Boolean,
    showListSelector: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        barcode: String,
        name: String,
        quantity: String?,
        categoryId: String?,
        brand: String?,
        source: String,
        listId: String,
        saveAsRegular: Boolean,
        scanNext: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(lookupResult.barcode) { mutableStateOf(lookupResult.suggestedName) }
    var quantity by remember(lookupResult.barcode) { mutableStateOf(lookupResult.suggestedQuantity.orEmpty()) }
    var selectedCategoryId by remember(lookupResult.barcode) {
        mutableStateOf(lookupResult.suggestedCategoryId)
    }
    var userPickedCategory by remember(lookupResult.barcode) {
        mutableStateOf(lookupResult.suggestedCategoryId != null)
    }
    var selectedListId by remember(lookupResult.barcode, initialListId) {
        mutableStateOf(
            initialListId?.takeIf { id -> lists.any { it.id == id } }
                ?: lists.firstOrNull()?.id.orEmpty()
        )
    }
    var saveAsRegular by remember(lookupResult.barcode) { mutableStateOf(false) }

    val (badgeText, badgeIcon, badgeContainer, badgeContent) = when (lookupResult.source) {
        BarcodeLookupSource.HOUSEHOLD_LIBRARY -> Quad(
            "In Household Library (Offline)",
            Icons.Rounded.BookmarkAdd,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        BarcodeLookupSource.OPEN_FOOD_FACTS -> Quad(
            "Found Online (Open Food Facts)",
            Icons.Rounded.CloudDone,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        BarcodeLookupSource.UNKNOWN -> Quad(
            "New Product — Saved Once, Remembered Forever",
            Icons.Rounded.QrCode2,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    val saveSourceTag = when (lookupResult.source) {
        BarcodeLookupSource.HOUSEHOLD_LIBRARY -> "manual"
        BarcodeLookupSource.OPEN_FOOD_FACTS -> "openfoodfacts"
        BarcodeLookupSource.UNKNOWN -> "manual"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = badgeContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeContent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeContent
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (lookupResult.source == BarcodeLookupSource.UNKNOWN) {
                        "Name This Scanned Product"
                    } else {
                        "Confirm Scanned Item"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Barcode: ${lookupResult.barcode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { updated ->
                    name = updated
                    if (!userPickedCategory) {
                        selectedCategoryId = SmartItemParser.parse(updated, categories, history)
                            .suggestedCategoryId
                    }
                },
                label = { Text("Item name *") },
                placeholder = { Text("e.g. Clover Full Cream Milk") },
                supportingText = if (lookupResult.source == BarcodeLookupSource.HOUSEHOLD_LIBRARY) {
                    { Text("Fixing a typo updates your saved household barcode too.") }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity (optional)") },
                placeholder = { Text("e.g. 2L, 500g, 6pk") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Aisle / Category",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = selectedCategoryId == cat.id
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedCategoryId = if (selected) null else cat.id
                                    userPickedCategory = true
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
            }

            if ((showListSelector || initialListId == null) && lists.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Add to Shopping List",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lists.forEach { list ->
                            FilterChip(
                                selected = selectedListId == list.id,
                                onClick = { selectedListId = list.id },
                                label = { Text(list.name) }
                            )
                        }
                    }
                }
            }

            if (isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = saveAsRegular,
                        onCheckedChange = { saveAsRegular = it }
                    )
                    Column {
                        Text(
                            text = "Also save to Regulars",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Keep this item handy in your household staples list",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val canSubmit = name.isNotBlank() && selectedListId.isNotBlank()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onConfirm(
                            lookupResult.barcode,
                            name.trim(),
                            quantity.trim().ifBlank { null },
                            selectedCategoryId,
                            lookupResult.brand,
                            saveSourceTag,
                            selectedListId,
                            saveAsRegular,
                            true
                        )
                    },
                    enabled = canSubmit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add & Next")
                }

                Button(
                    onClick = {
                        onConfirm(
                            lookupResult.barcode,
                            name.trim(),
                            quantity.trim().ifBlank { null },
                            selectedCategoryId,
                            lookupResult.brand,
                            saveSourceTag,
                            selectedListId,
                            saveAsRegular,
                            false
                        )
                    },
                    enabled = canSubmit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add & Done")
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
