package com.cartvr.app.ui.loyalty

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.cartvr.app.data.local.entity.LoyaltyCardEntity
import com.cartvr.app.ui.components.ListVisuals

/**
 * High-contrast, till-scanner-ready dialog for displaying a loyalty card barcode.
 *
 * Boosts window brightness to full (1.0f) and keeps the screen awake while visible, then restores
 * the previous brightness and window flags on dismiss/dispose. Always renders the barcode on pure
 * white with a generous quiet zone even when Dark Theme is enabled.
 */
@Composable
fun LoyaltyCardDisplayDialog(
    card: LoyaltyCardEntity,
    onDismiss: () -> Unit,
    onEdit: ((LoyaltyCardEntity) -> Unit)? = null,
    onSwitchCard: (() -> Unit)? = null
) {
    val accent = ListVisuals.parseColor(card.accentColor)
    val renderResult = remember(card.cardNumber, card.barcodeFormat) {
        BarcodeUtils.encodeBarcodeBitmap(
            rawValue = card.cardNumber,
            formatName = card.barcodeFormat,
            widthPx = 1000,
            heightPx = 340
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        val context = LocalContext.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        DisposableEffect(dialogWindow, context) {
            val activityWindow = context.findActivity()?.window
            val targetWindow = dialogWindow ?: activityWindow
            val originalBrightness = targetWindow?.attributes?.screenBrightness
                ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            val hadKeepScreenOn = targetWindow != null &&
                (targetWindow.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0

            if (targetWindow != null) {
                val attrs = targetWindow.attributes
                attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                targetWindow.attributes = attrs
                targetWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            onDispose {
                if (targetWindow != null) {
                    val attrs = targetWindow.attributes
                    attrs.screenBrightness = originalBrightness
                    targetWindow.attributes = attrs
                    if (!hadKeepScreenOn) {
                        targetWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Branded store header banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accent)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = card.storeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = card.cardName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (card.isShared) Icons.Rounded.People else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (card.isShared) "Shared" else "Private",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                            if (onEdit != null) {
                                IconButton(
                                    onClick = {
                                        onDismiss()
                                        onEdit(card)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit card",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Pure white till-scanner barcode frame
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (renderResult) {
                                is BarcodeRenderResult.Rendered -> {
                                    val is2D = BarcodeUtils.is2DFormat(renderResult.usedFormat)
                                    Image(
                                        bitmap = renderResult.bitmap.asImageBitmap(),
                                        contentDescription = "${card.storeName} barcode",
                                        contentScale = ContentScale.Fit,
                                        modifier = if (is2D) {
                                            Modifier
                                                .size(200.dp)
                                                .aspectRatio(1f)
                                        } else {
                                            Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                        }
                                    )
                                    Text(
                                        text = BarcodeUtils.formatSpacedDisplayNumber(card.cardNumber),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    if (renderResult.usedFallbackFormat) {
                                        Text(
                                            text = "Shown in Code 128 format for till compatibility",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF4B5563),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                is BarcodeRenderResult.FallbackTextOnly -> {
                                    Text(
                                        text = renderResult.displayNumber.ifBlank { card.cardNumber },
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = renderResult.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4B5563),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Screen brightness boosted for till scanner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    card.notes?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (onSwitchCard != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onSwitchCard()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("All Cards")
                            }
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Done")
                            }
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick picker sheet shown inside a shopping list, pinning list-linked cards at the top
 * while keeping all other loyalty cards directly accessible underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedCardsPickerSheet(
    linkedCards: List<LoyaltyCardEntity>,
    otherCards: List<LoyaltyCardEntity>,
    listName: String,
    onSelectCard: (LoyaltyCardEntity) -> Unit,
    onOpenWallet: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Choose Loyalty Card",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap any card to flash at the till",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenWallet()
                    }
                ) {
                    Text("Open Wallet")
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (linkedCards.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pinned to $listName",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(linkedCards, key = { it.id }) { card ->
                        LoyaltyPickerCardRow(
                            card = card,
                            onClick = {
                                onDismiss()
                                onSelectCard(card)
                            }
                        )
                    }
                }

                if (otherCards.isNotEmpty()) {
                    item {
                        Text(
                            text = if (linkedCards.isNotEmpty()) "Other Loyalty Cards" else "Your Loyalty Cards",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = if (linkedCards.isNotEmpty()) 6.dp else 0.dp)
                        )
                    }
                    items(otherCards, key = { it.id }) { card ->
                        LoyaltyPickerCardRow(
                            card = card,
                            onClick = {
                                onDismiss()
                                onSelectCard(card)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoyaltyPickerCardRow(
    card: LoyaltyCardEntity,
    onClick: () -> Unit
) {
    val accent = ListVisuals.parseColor(card.accentColor)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${card.cardName} • ${BarcodeUtils.formatSpacedDisplayNumber(card.cardNumber)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (card.isShared) Icons.Rounded.People else Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
