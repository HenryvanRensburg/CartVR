package com.cartvr.app.ui.loyalty

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.LoyaltyCardEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.ui.components.ListVisuals
import com.cartvr.app.ui.scanner.BarcodeScannerDialog

private data class StorePreset(
    val storeName: String,
    val cardName: String,
    val colorHex: String,
    val defaultFormat: String = "CODE_128"
)

private val southAfricanPresets = listOf(
    StorePreset("Checkers", "Xtra Savings", "#007A78", "EAN_13"),
    StorePreset("Woolworths", "WRewards", "#1F2937", "EAN_13"),
    StorePreset("Pick n Pay", "Smart Shopper", "#2563EB", "EAN_13"),
    StorePreset("Makro", "mCard", "#E11D48", "CODE_128"),
    StorePreset("Dis-Chem", "Benefit Card", "#16A34A", "CODE_128"),
    StorePreset("Clicks", "ClubCard", "#0284C7", "CODE_128"),
    StorePreset("Spar", "SPAR Rewards", "#DC2626", "EAN_13")
)

private val cardColorPalette = listOf(
    "#007A78",
    "#2563EB",
    "#1F2937",
    "#E11D48",
    "#16A34A",
    "#0284C7",
    "#D97706",
    "#8B5CF6"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyWalletScreen(
    cards: List<LoyaltyCardEntity>,
    lists: List<ShoppingListEntity>,
    onBack: () -> Unit,
    onSaveCard: (
        existingId: String?,
        storeName: String,
        cardName: String,
        cardNumber: String,
        barcodeFormat: String,
        accentColor: String,
        notes: String?,
        linkedListId: String?,
        isShared: Boolean
    ) -> Unit,
    onDeleteCard: (LoyaltyCardEntity) -> Unit
) {
    var activeDisplayCard by remember { mutableStateOf<LoyaltyCardEntity?>(null) }
    var editingCard by remember { mutableStateOf<LoyaltyCardEntity?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var pendingDeleteCard by remember { mutableStateOf<LoyaltyCardEntity?>(null) }

    val listNameById = remember(lists) { lists.associate { it.id to it.name } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Loyalty Card Wallet")
                        Text(
                            text = if (cards.isEmpty()) "Store cards ready at the till" else "${cards.size} saved cards",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCard = null
                    showCreateSheet = true
                }
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Loyalty Card")
            }
        }
    ) { innerPadding ->
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Text(
                        text = "Your Till Cards, Right Inside CartVR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Save Checkers Xtra Savings, Woolworths WRewards, Pick n Pay Smart Shopper, Makro, Dis-Chem, or Clicks cards so you can flash them straight at the till.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            editingCard = null
                            showCreateSheet = true
                        }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add First Card")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cards, key = { it.id }) { card ->
                    val accent = ListVisuals.parseColor(card.accentColor)
                    val linkedName = card.linkedListId?.let { listNameById[it] }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { activeDisplayCard = card },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(92.dp)
                                    .background(accent)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = card.storeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (card.isShared) Icons.Rounded.People else Icons.Rounded.Lock,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (card.isShared) "Shared" else "Private",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = card.cardName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = BarcodeUtils.formatSpacedDisplayNumber(card.cardNumber),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (linkedName != null) {
                                        Text(
                                            text = "• $linkedName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Row(modifier = Modifier.padding(end = 6.dp)) {
                                IconButton(
                                    onClick = {
                                        editingCard = card
                                        showCreateSheet = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit card"
                                    )
                                }
                                IconButton(onClick = { pendingDeleteCard = card }) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete card",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeDisplayCard?.let { card ->
        LoyaltyCardDisplayDialog(
            card = card,
            onDismiss = { activeDisplayCard = null },
            onEdit = { toEdit ->
                editingCard = toEdit
                showCreateSheet = true
            }
        )
    }

    if (showCreateSheet) {
        AddEditLoyaltyCardSheet(
            initialCard = editingCard,
            lists = lists,
            onDismiss = {
                showCreateSheet = false
                editingCard = null
            },
            onSave = { storeName, cardName, cardNumber, format, color, notes, linkedListId, isShared ->
                onSaveCard(
                    editingCard?.id,
                    storeName,
                    cardName,
                    cardNumber,
                    format,
                    color,
                    notes,
                    linkedListId,
                    isShared
                )
                showCreateSheet = false
                editingCard = null
            }
        )
    }

    pendingDeleteCard?.let { card ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCard = null },
            title = { Text("Delete ${card.storeName} card?") },
            text = {
                Text(
                    if (card.isShared) {
                        "This shared card will be removed for all household members."
                    } else {
                        "This private card will be removed from your wallet."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCard(card)
                        pendingDeleteCard = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCard = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoyaltyCardSheet(
    initialCard: LoyaltyCardEntity?,
    lists: List<ShoppingListEntity>,
    onDismiss: () -> Unit,
    onSave: (
        storeName: String,
        cardName: String,
        cardNumber: String,
        barcodeFormat: String,
        accentColor: String,
        notes: String?,
        linkedListId: String?,
        isShared: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var storeName by remember(initialCard?.id) { mutableStateOf(initialCard?.storeName.orEmpty()) }
    var cardName by remember(initialCard?.id) { mutableStateOf(initialCard?.cardName.orEmpty()) }
    var cardNumber by remember(initialCard?.id) { mutableStateOf(initialCard?.cardNumber.orEmpty()) }
    var barcodeFormat by remember(initialCard?.id) { mutableStateOf(initialCard?.barcodeFormat ?: "CODE_128") }
    var accentColor by remember(initialCard?.id) { mutableStateOf(initialCard?.accentColor ?: "#2563EB") }
    var notes by remember(initialCard?.id) { mutableStateOf(initialCard?.notes.orEmpty()) }
    var linkedListId by remember(initialCard?.id) { mutableStateOf(initialCard?.linkedListId) }
    var isShared by remember(initialCard?.id) { mutableStateOf(initialCard?.isShared ?: true) }
    var showScanner by remember { mutableStateOf(false) }

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
            Text(
                text = if (initialCard == null) "Add Loyalty Card" else "Edit Loyalty Card",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (initialCard == null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick South African Presets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        southAfricanPresets.forEach { preset ->
                            FilterChip(
                                selected = storeName.equals(preset.storeName, ignoreCase = true),
                                onClick = {
                                    storeName = preset.storeName
                                    cardName = preset.cardName
                                    accentColor = preset.colorHex
                                    barcodeFormat = preset.defaultFormat
                                },
                                label = { Text("${preset.storeName} ${preset.cardName}") }
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { showScanner = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Physical Card Barcode")
            }

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Store name *") },
                placeholder = { Text("e.g. Checkers") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cardName,
                onValueChange = { cardName = it },
                label = { Text("Card program name") },
                placeholder = { Text("e.g. Xtra Savings") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Card number / barcode value *") },
                placeholder = { Text("Scan or type digits") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Barcode Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BarcodeUtils.supportedFormats.forEach { (key, label) ->
                        FilterChip(
                            selected = barcodeFormat.equals(key, ignoreCase = true),
                            onClick = { barcodeFormat = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Card Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    cardColorPalette.forEach { hex ->
                        val color = ListVisuals.parseColor(hex)
                        val selected = accentColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { accentColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected color",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (lists.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Link to Shopping List (optional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = linkedListId == null,
                            onClick = { linkedListId = null },
                            label = { Text("None") }
                        )
                        lists.forEach { list ->
                            FilterChip(
                                selected = linkedListId == list.id,
                                onClick = { linkedListId = list.id },
                                label = { Text(list.name) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Share with Household",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (isShared) {
                            "Visible in Henry & Jolene's wallets"
                        } else {
                            "Private — only visible on your account"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isShared,
                    onCheckedChange = { isShared = it }
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. Link card or till PIN reminder") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            storeName.trim(),
                            cardName.trim().ifBlank { storeName.trim() },
                            cardNumber.trim(),
                            barcodeFormat,
                            accentColor,
                            notes.trim().ifBlank { null },
                            linkedListId,
                            isShared
                        )
                    },
                    enabled = storeName.isNotBlank() && cardNumber.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (initialCard == null) "Save Card" else "Update Card")
                }
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            title = "Scan Loyalty Card",
            subtitle = "Point your camera at the barcode on your store card",
            isPaused = false,
            onDismiss = { showScanner = false },
            onBarcodeDetected = { rawValue, detectedFormat ->
                cardNumber = rawValue
                barcodeFormat = detectedFormat
                showScanner = false
            }
        )
    }
}
