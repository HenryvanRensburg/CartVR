package com.cartvr.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.cartvr.app.ui.auth.AcceptInviteScreen
import com.cartvr.app.ui.auth.LoginScreen
import com.cartvr.app.ui.components.CreateListSheet
import com.cartvr.app.ui.components.JoinListDialog
import com.cartvr.app.ui.components.ListVisuals
import com.cartvr.app.ui.components.UpdateDialog
import com.cartvr.app.ui.family.ManageFamilyScreen
import com.cartvr.app.ui.list.ListDetailScreen
import com.cartvr.app.ui.settings.SettingsScreen
import com.cartvr.app.ui.shopping.ShoppingHubScreen
import com.cartvr.app.ui.shopping.ShoppingSubTab
import com.cartvr.app.ui.theme.CartVRTheme
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

private enum class TopTab(val route: String, val label: String, val icon: ImageVector) {
    SHOPPING("shopping", "Shopping", Icons.Rounded.ShoppingCart),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings)
}

@Composable
fun CartVrApp(viewModel: CartVrViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()
    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val inviteFlowState by viewModel.inviteFlowState.collectAsStateWithLifecycle()

    CartVRTheme(themeMode = themeMode) {
        when (sessionStatus) {
            is SessionStatus.Initializing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is SessionStatus.NotAuthenticated,
            is SessionStatus.RefreshFailure -> {
                if (inviteFlowState.showAcceptInvite) {
                    AcceptInviteScreen(
                        state = inviteFlowState,
                        onPreviewCode = { viewModel.previewFamilyInvite(it) },
                        onAcceptInvite = { code, email, password, name ->
                            viewModel.acceptFamilyInvite(code, email, password, name)
                        },
                        onBackToLogin = { viewModel.closeAcceptInviteScreen() }
                    )
                } else {
                    LoginScreen(
                        isLoading = authLoading,
                        errorMessage = authError,
                        onSignIn = { email, password -> viewModel.signIn(email, password) },
                        onOpenAcceptInvite = { viewModel.openAcceptInviteScreen() }
                    )
                }
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
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val cachedDisplayName by viewModel.displayName.collectAsStateWithLifecycle()
    val resolvedDisplayName = cachedDisplayName ?: viewModel.userDisplayName()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val lastSyncError by viewModel.lastSyncError.collectAsStateWithLifecycle()
    val familyState by viewModel.familyState.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val listMembers by viewModel.listMembers.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allItems by viewModel.allShoppingItems.collectAsStateWithLifecycle()
    val regulars by viewModel.regularItems.collectAsStateWithLifecycle()
    val history by viewModel.itemHistory.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val joinState by viewModel.joinState.collectAsStateWithLifecycle()
    val joinedListEvent by viewModel.joinedListEvent.collectAsStateWithLifecycle()
    val pendingNotificationListId by viewModel.pendingNotificationListId.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val notifyOnChecked by viewModel.notifyOnChecked.collectAsStateWithLifecycle()
    val liveModeEnabled by viewModel.liveModeEnabled.collectAsStateWithLifecycle()
    val liveModeDurationHours by viewModel.liveModeDurationHours.collectAsStateWithLifecycle()
    val liveModeStopAtMs by viewModel.liveModeStopAtMs.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var selectedShoppingSubTab by rememberSaveable { mutableStateOf(ShoppingSubTab.LISTS) }
    var showListSheet by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<ShoppingListEntity?>(null) }

    val showBottomBar = currentRoute in TopTab.entries.map { it.route }
    val showFab = currentRoute == TopTab.SHOPPING.route && selectedShoppingSubTab == ShoppingSubTab.LISTS

    LaunchedEffect(currentRoute, backStackEntry) {
        val activeListId = if (currentRoute == "list/{listId}") {
            backStackEntry?.arguments?.getString("listId")
        } else {
            null
        }
        viewModel.setActiveOpenListId(activeListId)
    }

    LaunchedEffect(pendingNotificationListId) {
        val targetListId = pendingNotificationListId ?: return@LaunchedEffect
        viewModel.consumePendingNotificationListId()
        navController.navigate("list/$targetListId") {
            launchSingleTop = true
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.imePadding()) },
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
            startDestination = TopTab.SHOPPING.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(TopTab.SHOPPING.route) {
                ShoppingHubScreen(
                    selectedSubTab = selectedShoppingSubTab,
                    onSubTabSelected = { selectedShoppingSubTab = it },
                    displayName = resolvedDisplayName,
                    currentUserId = viewModel.currentUserId(),
                    isAdmin = isAdmin,
                    syncState = syncState,
                    themeMode = themeMode,
                    lists = lists,
                    listMembers = listMembers,
                    allItems = allItems,
                    regulars = regulars,
                    categories = categories,
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
                    onLeaveList = { list -> viewModel.leaveSharedList(list) },
                    onAddRegularToList = { regular ->
                        val fallbackList = lists.firstOrNull() ?: return@ShoppingHubScreen
                        viewModel.addRegularToList(regular, fallbackList.id)
                    },
                    onSaveRegular = { name, qty, listId, categoryId, existingId ->
                        viewModel.saveRegularItem(name, qty, listId, categoryId, existingId)
                    },
                    onDeleteRegular = { regular ->
                        viewModel.deleteRegularItem(regular)
                    }
                )
            }

            composable(TopTab.SETTINGS.route) {
                SettingsScreen(
                    displayName = resolvedDisplayName,
                    userEmail = viewModel.userEmail(),
                    householdName = householdName ?: "Janse van Rensburg",
                    isAdmin = isAdmin,
                    themeMode = themeMode,
                    lastSyncError = lastSyncError,
                    updateState = updateState,
                    notificationsEnabled = notificationsEnabled,
                    notifyOnChecked = notifyOnChecked,
                    liveModeEnabled = liveModeEnabled,
                    liveModeDurationHours = liveModeDurationHours,
                    liveModeStopAtMs = liveModeStopAtMs,
                    onThemeSelected = { viewModel.setThemeMode(it) },
                    onNotificationsEnabledChanged = { viewModel.setNotificationsEnabled(it) },
                    onNotifyOnCheckedChanged = { viewModel.setNotifyOnChecked(it) },
                    onLiveModeEnabledChanged = { viewModel.setLiveModeEnabled(it) },
                    onLiveModeDurationChanged = { viewModel.setLiveModeDurationHours(it) },
                    onPreviewChime = { viewModel.previewNotificationChime() },
                    onOpenSoundSettings = { viewModel.openNotificationChannelSettings() },
                    onManageFamily = { navController.navigate("manage_family") },
                    onSyncNow = {
                        viewModel.syncNow()
                        scope.launch { snackbarHostState.showSnackbar("Syncing lists...") }
                    },
                    onCheckForUpdates = { viewModel.checkForUpdates(silent = false) },
                    onSignOut = { viewModel.signOut() }
                )
            }

            composable("manage_family") {
                ManageFamilyScreen(
                    currentUserId = viewModel.currentUserId(),
                    familyState = familyState,
                    sharedLists = lists.filter { it.isShared },
                    onBack = { navController.popBackStack() },
                    onRefresh = { viewModel.loadFamilyOverview() },
                    onCreateInvite = { name, role, autoListIds ->
                        viewModel.createFamilyInvite(name, role, autoListIds) { invite ->
                            ListVisuals.launchFamilyInviteShareSheet(
                                context,
                                invite.invitedName,
                                invite.code
                            )
                        }
                    },
                    onCreateMemberDirectly = { name, email, password, role, autoListIds ->
                        viewModel.createMemberDirectly(name, email, password, role, autoListIds)
                    },
                    onShareInvite = { invite ->
                        ListVisuals.launchFamilyInviteShareSheet(
                            context,
                            invite.invitedName,
                            invite.code
                        )
                    },
                    onRevokeInvite = { invite -> viewModel.revokeFamilyInvite(invite) },
                    onUpdateRole = { member, newRole ->
                        viewModel.updateFamilyMemberRole(member, newRole)
                    },
                    onRemoveMember = { member -> viewModel.removeFamilyMember(member) }
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
