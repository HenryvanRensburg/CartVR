package com.cartvr.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.ui.auth.LoginScreen
import com.cartvr.app.ui.components.CreateListSheet
import com.cartvr.app.ui.components.JoinListDialog
import com.cartvr.app.ui.components.ListVisuals
import com.cartvr.app.ui.components.UpdateDialog
import com.cartvr.app.ui.dashboard.DashboardScreen
import com.cartvr.app.ui.list.ListDetailScreen
import com.cartvr.app.ui.regulars.RegularsScreen
import com.cartvr.app.ui.settings.SettingsScreen
import com.cartvr.app.ui.theme.CartVRTheme
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

private enum class TopTab(val route: String, val label: String, val icon: ImageVector) {
    LISTS("lists", "Lists", Icons.Rounded.Checklist),
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
    val context = LocalContext.current

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val householdName by viewModel.householdName.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val listMembers by viewModel.listMembers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allItems by viewModel.allShoppingItems.collectAsStateWithLifecycle()
    val regulars by viewModel.regularItems.collectAsStateWithLifecycle()
    val history by viewModel.itemHistory.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val joinState by viewModel.joinState.collectAsStateWithLifecycle()
    val joinedListEvent by viewModel.joinedListEvent.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var showListSheet by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<ShoppingListEntity?>(null) }

    val showBottomBar = currentRoute in TopTab.entries.map { it.route }
    val showFab = currentRoute == TopTab.LISTS.route

    LaunchedEffect(joinedListEvent) {
        val joined = joinedListEvent ?: return@LaunchedEffect
        viewModel.consumeJoinedListEvent()
        navController.navigate("list/${joined.id}")
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner ?: return@LaunchedEffect
        viewModel.consumeStatusBanner()
        snackbarHostState.showSnackbar(msg)
    }

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
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingList = null
                        showListSheet = true
                    },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("New List") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopTab.LISTS.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(TopTab.LISTS.route) {
                DashboardScreen(
                    displayName = viewModel.userDisplayName(),
                    currentUserId = viewModel.currentUserId(),
                    syncState = syncState,
                    themeMode = themeMode,
                    lists = lists,
                    listMembers = listMembers,
                    allItems = allItems,
                    onToggleTheme = { isDark -> viewModel.toggleQuickTheme(isDark) },
                    onSyncClick = { viewModel.syncNow() },
                    onOpenJoinDialog = { viewModel.openJoinDialog() },
                    onListClick = { list -> navController.navigate("list/${list.id}") },
                    onEditList = { list ->
                        editingList = list
                        showListSheet = true
                    },
                    onShareList = { list ->
                        viewModel.prepareShare(list) { shared ->
                            val code = shared.shareCode ?: return@prepareShare
                            ListVisuals.launchShareSheet(context, shared.name, code)
                        }
                    },
                    onMakePrivate = { list -> viewModel.makeListPrivate(list) },
                    onDeleteList = { list -> viewModel.deleteList(list) },
                    onLeaveList = { list -> viewModel.leaveSharedList(list) }
                )
            }

            composable(TopTab.REGULARS.route) {
                RegularsScreen(
                    regulars = regulars,
                    lists = lists,
                    categories = categories,
                    onAddToShoppingList = { regular ->
                        val fallbackList = lists.firstOrNull() ?: return@RegularsScreen
                        viewModel.addRegularToList(regular, fallbackList.id)
                        val targetName = lists.firstOrNull { it.id == regular.defaultListId }?.name
                            ?: fallbackList.name
                        scope.launch {
                            snackbarHostState.showSnackbar("Added ${regular.name} to $targetName")
                        }
                    },
                    onCreateRegular = { name, qty, listId, categoryId ->
                        viewModel.saveRegularItem(name, qty, listId, categoryId)
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
                        scope.launch { snackbarHostState.showSnackbar("Syncing lists...") }
                    },
                    onCheckForUpdates = { viewModel.checkForUpdates(silent = false) },
                    onSignOut = { viewModel.signOut() }
                )
            }

            composable(
                route = "list/{listId}",
                arguments = listOf(navArgument("listId") { type = NavType.StringType })
            ) { entry ->
                val listId = entry.arguments?.getString("listId") ?: return@composable
                val list = lists.firstOrNull { it.id == listId }
                val listItems by viewModel.observeListItems(listId)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                if (list != null) {
                    val isOwner = list.ownerId == viewModel.currentUserId()
                    val memberCount = listMembers.count { it.listId == list.id }.coerceAtLeast(1)

                    ListDetailScreen(
                        list = list,
                        isOwner = isOwner,
                        memberCount = memberCount,
                        items = listItems,
                        categories = categories,
                        history = history,
                        regulars = regulars,
                        onBack = { navController.popBackStack() },
                        onQuickAddItem = { name, qty, note, catId ->
                            viewModel.addShoppingItem(list.id, catId, name, qty, note)
                        },
                        onToggleChecked = { viewModel.toggleItemChecked(it) },
                        onUpdateItem = { updated, categoryChanged ->
                            viewModel.updateShoppingItem(updated, learnCategory = categoryChanged)
                        },
                        onDeleteItem = { viewModel.deleteShoppingItem(it) },
                        onClearCompleted = { viewModel.clearCompletedInList(list.id) },
                        onSaveCategoryOrder = { viewModel.updateCategoryOrder(it) },
                        onShareList = {
                            viewModel.prepareShare(list) { shared ->
                                val code = shared.shareCode ?: return@prepareShare
                                ListVisuals.launchShareSheet(context, shared.name, code)
                            }
                        },
                        onMakePrivate = { viewModel.makeListPrivate(list) },
                        onDeleteList = {
                            viewModel.deleteList(list)
                            navController.popBackStack()
                        },
                        onLeaveList = {
                            viewModel.leaveSharedList(list) {
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showListSheet) {
        CreateListSheet(
            editingList = editingList,
            onDismiss = {
                showListSheet = false
                editingList = null
            },
            onSave = { name, iconKey, colorHex, isShared ->
                val existing = editingList
                if (existing == null) {
                    viewModel.createList(name, iconKey, colorHex, isShared) { created ->
                        if (created.isShared && !created.shareCode.isNullOrBlank()) {
                            ListVisuals.launchShareSheet(context, created.name, created.shareCode)
                        }
                        navController.navigate("list/${created.id}")
                    }
                } else {
                    viewModel.updateList(existing, name, iconKey, colorHex)
                }
                showListSheet = false
                editingList = null
            }
        )
    }

    if (joinState.showDialog) {
        JoinListDialog(
            isJoining = joinState.isJoining,
            errorMessage = joinState.errorMessage,
            onDismiss = { viewModel.dismissJoinDialog() },
            onJoin = { code -> viewModel.joinListByCode(code) }
        )
    }
}
