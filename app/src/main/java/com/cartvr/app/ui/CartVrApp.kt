package com.cartvr.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.ui.auth.LoginScreen
import com.cartvr.app.ui.components.AddItemBottomSheet
import com.cartvr.app.ui.components.UpdateDialog
import com.cartvr.app.ui.dashboard.DashboardScreen
import com.cartvr.app.ui.regulars.RegularsScreen
import com.cartvr.app.ui.settings.SettingsScreen
import com.cartvr.app.ui.store.StoreListScreen
import com.cartvr.app.ui.theme.CartVRTheme
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

private enum class TopTab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Rounded.Dashboard),
    REGULARS("regulars", "Regulars", Icons.Rounded.Repeat),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings)
}

@Composable
fun CartVrApp(viewModel: CartVrViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()
    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    CartVRTheme(themeMode = themeMode) {
        when (sessionStatus) {
            is SessionStatus.Initializing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is SessionStatus.NotAuthenticated,
            is SessionStatus.RefreshFailure -> {
                LoginScreen(
                    isLoading = authLoading,
                    errorMessage = authError,
                    onSignIn = { email, password -> viewModel.signIn(email, password) }
                )
            }

            is SessionStatus.Authenticated -> {
                AuthenticatedApp(viewModel = viewModel)
            }
        }

        if (updateState.availableUpdate != null) {
            UpdateDialog(
                state = updateState,
                onUpdateNow = { viewModel.startUpdateDownload() },
                onOpenSettings = { viewModel.openInstallUnknownAppsSettings() },
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }
    }
}

@Composable
private fun AuthenticatedApp(viewModel: CartVrViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val householdName by viewModel.householdName.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val stores by viewModel.stores.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allItems by viewModel.allShoppingItems.collectAsStateWithLifecycle()
    val regulars by viewModel.regularItems.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentStoreId = backStackEntry?.arguments?.getString("storeId")

    var showAddSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }

    val showBottomBar = currentRoute in TopTab.entries.map { it.route }
    val showFab = currentRoute == TopTab.DASHBOARD.route || currentRoute?.startsWith("store/") == true

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    TopTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab && stores.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingItem = null
                        showAddSheet = true
                    },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add Item") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopTab.DASHBOARD.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(TopTab.DASHBOARD.route) {
                DashboardScreen(
                    displayName = viewModel.userDisplayName(),
                    householdName = householdName ?: "Janse van Rensburg",
                    syncState = syncState,
                    themeMode = themeMode,
                    stores = stores,
                    allItems = allItems,
                    regulars = regulars,
                    onToggleTheme = { isDark -> viewModel.toggleQuickTheme(isDark) },
                    onSyncClick = { viewModel.syncNow() },
                    onStoreClick = { store ->
                        navController.navigate("store/${store.id}")
                    },
                    onQuickAddRegular = { regular ->
                        val fallbackStore = stores.firstOrNull() ?: return@DashboardScreen
                        viewModel.addRegularToList(regular, fallbackStore.id)
                        val targetStoreName = stores.firstOrNull { it.id == regular.defaultStoreId }?.name
                            ?: fallbackStore.name
                        scope.launch {
                            snackbarHostState.showSnackbar("Added ${regular.name} to $targetStoreName")
                        }
                    }
                )
            }

            composable(TopTab.REGULARS.route) {
                RegularsScreen(
                    regulars = regulars,
                    stores = stores,
                    categories = categories,
                    onAddToShoppingList = { regular ->
                        val fallbackStore = stores.firstOrNull() ?: return@RegularsScreen
                        viewModel.addRegularToList(regular, fallbackStore.id)
                        val targetStoreName = stores.firstOrNull { it.id == regular.defaultStoreId }?.name
                            ?: fallbackStore.name
                        scope.launch {
                            snackbarHostState.showSnackbar("Added ${regular.name} to $targetStoreName")
                        }
                    },
                    onCreateRegular = { name, qty, storeId, categoryId ->
                        viewModel.saveRegularItem(name, qty, storeId, categoryId)
                    },
                    onDeleteRegular = { regular ->
                        viewModel.deleteRegularItem(regular)
                    }
                )
            }

            composable(TopTab.SETTINGS.route) {
                SettingsScreen(
                    displayName = viewModel.userDisplayName(),
                    userEmail = viewModel.userEmail(),
                    householdName = householdName ?: "Janse van Rensburg",
                    themeMode = themeMode,
                    updateState = updateState,
                    onThemeSelected = { viewModel.setThemeMode(it) },
                    onSyncNow = {
                        viewModel.syncNow()
                        scope.launch { snackbarHostState.showSnackbar("Syncing with household...") }
                    },
                    onCheckForUpdates = { viewModel.checkForUpdates(silent = false) },
                    onSignOut = { viewModel.signOut() }
                )
            }

            composable(
                route = "store/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { entry ->
                val storeId = entry.arguments?.getString("storeId") ?: return@composable
                val store = stores.firstOrNull { it.id == storeId }
                val storeItems by viewModel.observeStoreItems(storeId)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                if (store != null) {
                    StoreListScreen(
                        store = store,
                        items = storeItems,
                        categories = categories,
                        onBack = { navController.popBackStack() },
                        onToggleChecked = { viewModel.toggleItemChecked(it) },
                        onEditItem = { item ->
                            editingItem = item
                            showAddSheet = true
                        },
                        onDeleteItem = { viewModel.deleteShoppingItem(it) },
                        onClearCompleted = { viewModel.clearCompletedInStore(storeId) },
                        onSaveCategoryOrder = { viewModel.updateCategoryOrder(it) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddItemBottomSheet(
            stores = stores,
            categories = categories,
            initialStoreId = currentStoreId,
            editingItem = editingItem,
            onDismiss = {
                showAddSheet = false
                editingItem = null
            },
            onSave = { storeId, categoryId, name, quantity, note, saveAsRegular ->
                val existing = editingItem
                if (existing == null) {
                    viewModel.addShoppingItem(storeId, categoryId, name, quantity, note, saveAsRegular)
                } else {
                    viewModel.updateShoppingItem(
                        existing.copy(
                            storeId = storeId,
                            categoryId = categoryId,
                            name = name,
                            quantity = quantity,
                            note = note
                        )
                    )
                }
                showAddSheet = false
                editingItem = null
            }
        )
    }
}
