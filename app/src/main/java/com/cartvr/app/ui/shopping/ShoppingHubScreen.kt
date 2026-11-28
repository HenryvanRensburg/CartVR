package com.cartvr.app.ui.shopping

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.ListMemberEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.preferences.ThemeMode
import com.cartvr.app.data.sync.SyncState
import com.cartvr.app.ui.components.SyncStatusPill
import com.cartvr.app.ui.dashboard.DashboardScreen
import com.cartvr.app.ui.regulars.RegularsScreen

enum class ShoppingSubTab(val label: String) {
    LISTS("Lists"),
    REGULARS("Regulars")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingHubScreen(
    selectedSubTab: ShoppingSubTab,
    onSubTabSelected: (ShoppingSubTab) -> Unit,
    displayName: String,
    currentUserId: String?,
    isAdmin: Boolean,
    syncState: SyncState,
    themeMode: ThemeMode,
    lists: List<ShoppingListEntity>,
    listMembers: List<ListMemberEntity>,
    allItems: List<ShoppingItemEntity>,
    regulars: List<RegularItemEntity>,
    categories: List<CategoryEntity>,
    onToggleTheme: (Boolean) -> Unit,
    onSyncClick: () -> Unit,
    onOpenJoinDialog: () -> Unit,
    onOpenLoyaltyWallet: () -> Unit = {},
    onOpenBarcodeScanner: () -> Unit = {},
    onListClick: (ShoppingListEntity) -> Unit,
    onEditList: (ShoppingListEntity) -> Unit,
    onShareList: (ShoppingListEntity) -> Unit,
    onMakePrivate: (ShoppingListEntity) -> Unit,
    onDeleteList: (ShoppingListEntity) -> Unit,
    onLeaveList: (ShoppingListEntity) -> Unit,
    onAddRegularToList: (RegularItemEntity) -> Unit,
    onSaveRegular: (name: String, qty: String?, listId: String?, categoryId: String?, existingId: String?) -> Unit,
    onDeleteRegular: (RegularItemEntity) -> Unit
) {
    val isCurrentlyDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Shared Shopping Hub Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hi, $displayName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (selectedSubTab == ShoppingSubTab.LISTS) {
                        "Your shopping lists"
                    } else {
                        "Household regular staples"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SyncStatusPill(syncState = syncState, onClick = onSyncClick)
                IconButton(onClick = onOpenBarcodeScanner) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = "Scan barcode",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onOpenLoyaltyWallet) {
                    Icon(
                        imageVector = Icons.Rounded.CreditCard,
                        contentDescription = "Loyalty Card Wallet",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (selectedSubTab == ShoppingSubTab.LISTS) {
                    IconButton(onClick = onOpenJoinDialog) {
                        Icon(
                            imageVector = Icons.Rounded.GroupAdd,
                            contentDescription = "Join shared list",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                IconButton(onClick = { onToggleTheme(isCurrentlyDark) }) {
                    Icon(
                        imageVector = if (isCurrentlyDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Top Lists | Regulars Segmented Switch
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            ShoppingSubTab.entries.forEachIndexed { index, subTab ->
                SegmentedButton(
                    selected = selectedSubTab == subTab,
                    onClick = { onSubTabSelected(subTab) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ShoppingSubTab.entries.size
                    ),
                    icon = {
                        Icon(
                            imageVector = if (subTab == ShoppingSubTab.LISTS) {
                                Icons.Rounded.Checklist
                            } else {
                                Icons.Rounded.Repeat
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text(subTab.label) }
                )
            }
        }

        when (selectedSubTab) {
            ShoppingSubTab.LISTS -> {
                DashboardScreen(
                    displayName = displayName,
                    currentUserId = currentUserId,
                    syncState = syncState,
                    themeMode = themeMode,
                    lists = lists,
                    listMembers = listMembers,
                    allItems = allItems,
                    onToggleTheme = onToggleTheme,
                    onSyncClick = onSyncClick,
                    onOpenJoinDialog = onOpenJoinDialog,
                    onListClick = onListClick,
                    onEditList = onEditList,
                    onShareList = onShareList,
                    onMakePrivate = onMakePrivate,
                    onDeleteList = onDeleteList,
                    onLeaveList = onLeaveList,
                    embeddedInShoppingHub = true
                )
            }

            ShoppingSubTab.REGULARS -> {
                RegularsScreen(
                    regulars = regulars,
                    lists = lists,
                    allItems = allItems,
                    categories = categories,
                    isAdmin = isAdmin,
                    onAddToShoppingList = onAddRegularToList,
                    onSaveRegular = onSaveRegular,
                    onDeleteRegular = onDeleteRegular,
                    embeddedInShoppingHub = true
                )
            }
        }
    }
}
