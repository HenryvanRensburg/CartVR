package com.cartvr.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cartvr.app.data.local.entity.CalendarEventEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.local.entity.TaskEntity
import com.cartvr.app.data.local.entity.TaskListEntity
import com.cartvr.app.ui.auth.AcceptInviteScreen
import com.cartvr.app.ui.auth.LoginScreen
import com.cartvr.app.ui.calendar.CalendarScreen
import com.cartvr.app.ui.calendar.EventEditorSheet
import com.cartvr.app.ui.components.CreateListSheet
import com.cartvr.app.ui.components.JoinListDialog
import com.cartvr.app.ui.components.ListVisuals
import com.cartvr.app.ui.components.UpdateDialog
import com.cartvr.app.ui.family.ManageFamilyScreen
import com.cartvr.app.ui.home.FamilyDashboardScreen
import com.cartvr.app.ui.home.QuickAddShoppingItemDialog
import com.cartvr.app.ui.list.ListDetailScreen
import com.cartvr.app.ui.loyalty.LoyaltyWalletScreen
import com.cartvr.app.ui.scanner.BarcodeScannerDialog
import com.cartvr.app.ui.scanner.ScannedItemSheet
import com.cartvr.app.ui.settings.SettingsScreen
import com.cartvr.app.ui.shopping.ShoppingHubScreen
import com.cartvr.app.ui.shopping.ShoppingSubTab
import com.cartvr.app.ui.tasks.TaskEditorSheet
import com.cartvr.app.ui.tasks.TaskFolderScreen
import com.cartvr.app.ui.tasks.TaskFolderSheet
import com.cartvr.app.ui.tasks.TasksHubScreen
import com.cartvr.app.ui.theme.CartVRTheme
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class TopTab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Home", Icons.Rounded.Home),
    SHOPPING("shopping", "Shopping", Icons.Rounded.ShoppingCart),
    TASKS("tasks", "Tasks", Icons.Rounded.TaskAlt),
    CALENDAR("calendar", "Calendar", Icons.Rounded.CalendarMonth),
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
    val morningSummaryEnabled by viewModel.morningSummaryEnabled.collectAsStateWithLifecycle()
    val morningSummaryHour by viewModel.morningSummaryHour.collectAsStateWithLifecycle()
    val morningSummaryMinute by viewModel.morningSummaryMinute.collectAsStateWithLifecycle()
    val defaultEventReminderMinutes by viewModel.defaultEventReminderMinutes.collectAsStateWithLifecycle()
    val notifyTaskAssignments by viewModel.notifyTaskAssignments.collectAsStateWithLifecycle()
    val notifySharedCalendar by viewModel.notifySharedCalendar.collectAsStateWithLifecycle()
    val pendingDashboardEvent by viewModel.pendingDashboardEvent.collectAsStateWithLifecycle()
    val pendingNotificationTaskId by viewModel.pendingNotificationTaskId.collectAsStateWithLifecycle()
    val pendingNotificationEvent by viewModel.pendingNotificationEvent.collectAsStateWithLifecycle()
    val householdProfiles by viewModel.householdProfiles.collectAsStateWithLifecycle()
    val taskLists by viewModel.taskLists.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val taskSubtasks by viewModel.taskSubtasks.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val loyaltyCards by viewModel.loyaltyCards.collectAsStateWithLifecycle()
    val isLookingUpBarcode by viewModel.isLookingUpBarcode.collectAsStateWithLifecycle()
    val scannedBarcodeResult by viewModel.scannedBarcodeResult.collectAsStateWithLifecycle()
    val keepScreenAwakeOnList by viewModel.keepScreenAwakeOnList.collectAsStateWithLifecycle()
    val keepScreenAwakeInLiveMode by viewModel.keepScreenAwakeInLiveMode.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var selectedShoppingSubTab by rememberSaveable { mutableStateOf(ShoppingSubTab.LISTS) }
    var selectedCalendarDateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedCalendarDate = remember(selectedCalendarDateIso) {
        runCatching { LocalDate.parse(selectedCalendarDateIso) }.getOrDefault(LocalDate.now())
    }

    var dashboardQuickAddExpanded by remember { mutableStateOf(false) }
    var showQuickAddShoppingDialog by remember { mutableStateOf(false) }

    var showListSheet by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<ShoppingListEntity?>(null) }

    var showTaskFolderSheet by remember { mutableStateOf(false) }
    var editingTaskFolder by remember { mutableStateOf<TaskListEntity?>(null) }
    var showTaskEditorSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var taskEditorFolderId by remember { mutableStateOf("") }
    var taskEditorInitialStatus by remember { mutableStateOf("todo") }

    var showEventSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var editingEventOccurrenceDate by remember { mutableStateOf(LocalDate.now()) }

    var showBarcodeScanner by remember { mutableStateOf(false) }
    var scannerTargetListId by remember { mutableStateOf<String?>(null) }

    fun navigateToTopTab(tab: TopTab) {
        dashboardQuickAddExpanded = false
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showBottomBar = currentRoute in TopTab.entries.map { it.route }
    val isDashboard = currentRoute == TopTab.DASHBOARD.route
    val showShoppingFab = currentRoute == TopTab.SHOPPING.route && selectedShoppingSubTab == ShoppingSubTab.LISTS
    val showTasksFab = currentRoute == TopTab.TASKS.route && taskLists.isNotEmpty()
    val showCalendarFab = currentRoute == TopTab.CALENDAR.route
    val showFab = isDashboard || showShoppingFab || showTasksFab || showCalendarFab

    LaunchedEffect(currentRoute, backStackEntry) {
        if (currentRoute != TopTab.DASHBOARD.route) {
            dashboardQuickAddExpanded = false
        }
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

    LaunchedEffect(pendingDashboardEvent) {
        if (!pendingDashboardEvent) return@LaunchedEffect
        viewModel.consumePendingDashboardEvent()
        navigateToTopTab(TopTab.DASHBOARD)
    }

    LaunchedEffect(pendingNotificationTaskId, tasks) {
        val targetTaskId = pendingNotificationTaskId ?: return@LaunchedEffect
        val targetTask = tasks.firstOrNull { it.id == targetTaskId } ?: return@LaunchedEffect
        viewModel.consumePendingNotificationTaskId()
        editingTask = targetTask
        taskEditorFolderId = targetTask.taskListId
        taskEditorInitialStatus = targetTask.status
        showTaskEditorSheet = true
    }

    LaunchedEffect(pendingNotificationEvent, calendarEvents) {
        val pair = pendingNotificationEvent ?: return@LaunchedEffect
        val targetEvent = calendarEvents.firstOrNull { it.id == pair.first } ?: return@LaunchedEffect
        viewModel.consumePendingNotificationEvent()
        val occDate = pair.second?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        selectedCalendarDateIso = occDate.toString()
        navigateToTopTab(TopTab.CALENDAR)
        editingEvent = targetEvent
        editingEventOccurrenceDate = occDate
        showEventSheet = true
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
                            onClick = { navigateToTopTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    text = tab.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                if (isDashboard) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(visible = dashboardQuickAddExpanded) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 2.dp
                                    ) {
                                        Text(
                                            text = "Shopping Item",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    SmallFloatingActionButton(
                                        onClick = {
                                            dashboardQuickAddExpanded = false
                                            if (lists.isNotEmpty()) {
                                                showQuickAddShoppingDialog = true
                                            } else {
                                                editingList = null
                                                showListSheet = true
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Rounded.ShoppingCart, contentDescription = "Add shopping item")
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 2.dp
                                    ) {
                                        Text(
                                            text = "New Task",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    SmallFloatingActionButton(
                                        onClick = {
                                            dashboardQuickAddExpanded = false
                                            if (taskLists.isNotEmpty()) {
                                                editingTask = null
                                                taskEditorFolderId = taskLists.first().id
                                                taskEditorInitialStatus = "todo"
                                                showTaskEditorSheet = true
                                            } else {
                                                editingTaskFolder = null
                                                showTaskFolderSheet = true
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Rounded.TaskAlt, contentDescription = "Add task")
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 2.dp
                                    ) {
                                        Text(
                                            text = "Calendar Event",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    SmallFloatingActionButton(
                                        onClick = {
                                            dashboardQuickAddExpanded = false
                                            editingEvent = null
                                            editingEventOccurrenceDate = LocalDate.now()
                                            showEventSheet = true
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Event, contentDescription = "Add event")
                                    }
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = { dashboardQuickAddExpanded = !dashboardQuickAddExpanded },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = if (dashboardQuickAddExpanded) Icons.Rounded.Close else Icons.Rounded.Add,
                                contentDescription = "Quick Add"
                            )
                        }
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = {
                            when {
                                showTasksFab -> {
                                    editingTask = null
                                    taskEditorFolderId = taskLists.firstOrNull()?.id ?: ""
                                    taskEditorInitialStatus = "todo"
                                    showTaskEditorSheet = true
                                }
                                showCalendarFab -> {
                                    editingEvent = null
                                    editingEventOccurrenceDate = selectedCalendarDate
                                    showEventSheet = true
                                }
                                else -> {
                                    editingList = null
                                    showListSheet = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = {
                            Text(
                                when {
                                    showTasksFab -> "New Task"
                                    showCalendarFab -> "New Event"
                                    else -> "New List"
                                }
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
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
                FamilyDashboardScreen(
                    displayName = resolvedDisplayName,
                    syncState = syncState,
                    themeMode = themeMode,
                    lists = lists,
                    allShoppingItems = allItems,
                    taskLists = taskLists,
                    tasks = tasks,
                    calendarEvents = calendarEvents,
                    profiles = householdProfiles,
                    onToggleTheme = { isDark -> viewModel.toggleQuickTheme(isDark) },
                    onSyncClick = { viewModel.syncNow() },
                    onOpenShoppingTab = { navigateToTopTab(TopTab.SHOPPING) },
                    onOpenShoppingList = { list -> navController.navigate("list/${list.id}") },
                    onOpenTasksTab = { navigateToTopTab(TopTab.TASKS) },
                    onToggleTaskDone = { task ->
                        viewModel.setTaskStatus(task, if (task.status == "done") "todo" else "done")
                    },
                    onOpenTask = { task ->
                        editingTask = task
                        taskEditorFolderId = task.taskListId
                        taskEditorInitialStatus = task.status
                        showTaskEditorSheet = true
                    },
                    onOpenCalendarTab = { navigateToTopTab(TopTab.CALENDAR) },
                    onOpenEvent = { event, occDate ->
                        editingEvent = event
                        editingEventOccurrenceDate = occDate
                        showEventSheet = true
                    }
                )
            }
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
                    onOpenLoyaltyWallet = { navController.navigate("loyalty_cards") },
                    onOpenBarcodeScanner = {
                        scannerTargetListId = lists.firstOrNull()?.id
                        viewModel.clearScannedBarcode()
                        showBarcodeScanner = true
                    },
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

            composable(TopTab.TASKS.route) {
                TasksHubScreen(
                    displayName = resolvedDisplayName,
                    currentUserId = viewModel.currentUserId(),
                    syncState = syncState,
                    themeMode = themeMode,
                    folders = taskLists,
                    tasks = tasks,
                    subtasks = taskSubtasks,
                    profiles = householdProfiles,
                    onToggleTheme = { isDark -> viewModel.toggleQuickTheme(isDark) },
                    onSyncClick = { viewModel.syncNow() },
                    onFolderClick = { folder -> navController.navigate("task_folder/${folder.id}") },
                    onCreateFolder = {
                        editingTaskFolder = null
                        showTaskFolderSheet = true
                    },
                    onEditFolder = { folder ->
                        editingTaskFolder = folder
                        showTaskFolderSheet = true
                    },
                    onToggleFolderPrivacy = { folder ->
                        viewModel.saveTaskList(
                            existing = folder,
                            name = folder.name,
                            iconKey = folder.iconKey,
                            colorHex = folder.colorHex,
                            isShared = !folder.isShared,
                            defaultView = folder.defaultView
                        )
                    },
                    onDeleteFolder = { folder -> viewModel.deleteTaskList(folder) },
                    onToggleTaskDone = { task ->
                        viewModel.setTaskStatus(task, if (task.status == "done") "todo" else "done")
                    },
                    onTaskClick = { task ->
                        editingTask = task
                        taskEditorFolderId = task.taskListId
                        taskEditorInitialStatus = task.status
                        showTaskEditorSheet = true
                    }
                )
            }

            composable(
                route = "task_folder/{folderId}",
                arguments = listOf(navArgument("folderId") { type = NavType.StringType })
            ) { entry ->
                val folderId = entry.arguments?.getString("folderId") ?: return@composable
                val folder = taskLists.firstOrNull { it.id == folderId }
                if (folder != null) {
                    val folderTasks = tasks.filter { it.taskListId == folder.id }
                    TaskFolderScreen(
                        folder = folder,
                        tasks = folderTasks,
                        subtasks = taskSubtasks,
                        profiles = householdProfiles,
                        onBack = { navController.popBackStack() },
                        onToggleViewMode = { view -> viewModel.updateTaskListDefaultView(folder, view) },
                        onEditFolder = {
                            editingTaskFolder = folder
                            showTaskFolderSheet = true
                        },
                        onDeleteFolder = {
                            viewModel.deleteTaskList(folder)
                            navController.popBackStack()
                        },
                        onCreateTask = { initialStatus ->
                            editingTask = null
                            taskEditorFolderId = folder.id
                            taskEditorInitialStatus = initialStatus
                            showTaskEditorSheet = true
                        },
                        onEditTask = { task ->
                            editingTask = task
                            taskEditorFolderId = task.taskListId
                            taskEditorInitialStatus = task.status
                            showTaskEditorSheet = true
                        },
                        onSetTaskStatus = { task, status ->
                            viewModel.setTaskStatus(task, status)
                        }
                    )
                }
            }

            composable(TopTab.CALENDAR.route) {
                CalendarScreen(
                    syncState = syncState,
                    themeMode = themeMode,
                    selectedDate = selectedCalendarDate,
                    onSelectDate = { selectedCalendarDateIso = it.toString() },
                    events = calendarEvents,
                    tasks = tasks,
                    taskLists = taskLists,
                    subtasks = taskSubtasks,
                    profiles = householdProfiles,
                    onToggleTheme = { isDark -> viewModel.toggleQuickTheme(isDark) },
                    onSyncClick = { viewModel.syncNow() },
                    onEventClick = { event, occDate ->
                        editingEvent = event
                        editingEventOccurrenceDate = occDate
                        showEventSheet = true
                    },
                    onTaskClick = { task ->
                        editingTask = task
                        taskEditorFolderId = task.taskListId
                        taskEditorInitialStatus = task.status
                        showTaskEditorSheet = true
                    },
                    onToggleTaskDone = { task ->
                        viewModel.setTaskStatus(task, if (task.status == "done") "todo" else "done")
                    }
                )
            }

            composable(TopTab.SETTINGS.route) {
                val myAccentColor = householdProfiles
                    .firstOrNull { it.userId == viewModel.currentUserId() }
                    ?.accentColor
                    ?: "#0D9488"

                SettingsScreen(
                    displayName = resolvedDisplayName,
                    userEmail = viewModel.userEmail(),
                    householdName = householdName ?: "Janse van Rensburg",
                    isAdmin = isAdmin,
                    themeMode = themeMode,
                    myAccentColor = myAccentColor,
                    lastSyncError = lastSyncError,
                    updateState = updateState,
                    notificationsEnabled = notificationsEnabled,
                    notifyOnChecked = notifyOnChecked,
                    liveModeEnabled = liveModeEnabled,
                    liveModeDurationHours = liveModeDurationHours,
                    liveModeStopAtMs = liveModeStopAtMs,
                    morningSummaryEnabled = morningSummaryEnabled,
                    morningSummaryHour = morningSummaryHour,
                    morningSummaryMinute = morningSummaryMinute,
                    defaultEventReminderMinutes = defaultEventReminderMinutes,
                    notifyTaskAssignments = notifyTaskAssignments,
                    notifySharedCalendar = notifySharedCalendar,
                    keepScreenAwakeOnList = keepScreenAwakeOnList,
                    keepScreenAwakeInLiveMode = keepScreenAwakeInLiveMode,
                    onThemeSelected = { viewModel.setThemeMode(it) },
                    onAccentColorSelected = { viewModel.updateMyAccentColor(it) },
                    onNotificationsEnabledChanged = { viewModel.setNotificationsEnabled(it) },
                    onNotifyOnCheckedChanged = { viewModel.setNotifyOnChecked(it) },
                    onLiveModeEnabledChanged = { viewModel.setLiveModeEnabled(it) },
                    onLiveModeDurationChanged = { viewModel.setLiveModeDurationHours(it) },
                    onPreviewChime = { viewModel.previewNotificationChime() },
                    onOpenSoundSettings = { viewModel.openNotificationChannelSettings() },
                    onMorningSummaryEnabledChanged = { viewModel.setMorningSummaryEnabled(it) },
                    onMorningSummaryTimeChanged = { h, m -> viewModel.setMorningSummaryTime(h, m) },
                    onDefaultEventReminderChanged = { viewModel.setDefaultEventReminderMinutes(it) },
                    onNotifyTaskAssignmentsChanged = { viewModel.setNotifyTaskAssignments(it) },
                    onNotifySharedCalendarChanged = { viewModel.setNotifySharedCalendar(it) },
                    onKeepScreenAwakeOnListChanged = { viewModel.setKeepScreenAwakeOnList(it) },
                    onKeepScreenAwakeInLiveModeChanged = { viewModel.setKeepScreenAwakeInLiveMode(it) },
                    onOpenExactAlarmSettings = { viewModel.openExactAlarmSettings() },
                    onOpenBatteryOptimizationSettings = { viewModel.openBatteryOptimizationSettings() },
                    onManageFamily = { navController.navigate("manage_family") },
                    onSyncNow = {
                        viewModel.syncNow()
                        scope.launch { snackbarHostState.showSnackbar("Syncing lists...") }
                    },
                    onCheckForUpdates = { viewModel.checkForUpdates(silent = false) },
                    onSignOut = { viewModel.signOut() }
                )
            }

            composable("loyalty_cards") {
                LoyaltyWalletScreen(
                    cards = loyaltyCards,
                    lists = lists,
                    onBack = { navController.popBackStack() },
                    onSaveCard = { existingId, storeName, cardName, cardNumber, format, color, notes, linkedListId, isShared ->
                        viewModel.saveLoyaltyCard(
                            existingId = existingId,
                            storeName = storeName,
                            cardName = cardName,
                            cardNumber = cardNumber,
                            barcodeFormat = format,
                            accentColor = color,
                            notes = notes,
                            linkedListId = linkedListId,
                            isShared = isShared
                        )
                    },
                    onDeleteCard = { card -> viewModel.deleteLoyaltyCard(card) }
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
                        allLoyaltyCards = loyaltyCards,
                        defaultKeepAwakeOnList = keepScreenAwakeOnList,
                        keepAwakeInLiveMode = keepScreenAwakeInLiveMode,
                        liveModeEnabled = liveModeEnabled,
                        onBack = { navController.popBackStack() },
                        onQuickAddItem = { name, qty, note, catId ->
                            viewModel.addShoppingItem(list.id, catId, name, qty, note)
                        },
                        onOpenScanBarcode = {
                            scannerTargetListId = list.id
                            viewModel.clearScannedBarcode()
                            showBarcodeScanner = true
                        },
                        onOpenLoyaltyWallet = { navController.navigate("loyalty_cards") },
                        onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
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

    if (showTaskFolderSheet) {
        val currentFolder = editingTaskFolder
        TaskFolderSheet(
            editingFolder = currentFolder,
            canEditPrivacy = currentFolder == null || currentFolder.ownerId == viewModel.currentUserId(),
            onDismiss = {
                showTaskFolderSheet = false
                editingTaskFolder = null
            },
            onSave = { name, iconKey, colorHex, isShared, defaultView ->
                viewModel.saveTaskList(
                    existing = currentFolder,
                    name = name,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    isShared = isShared,
                    defaultView = defaultView
                ) { saved ->
                    if (currentFolder == null) {
                        navController.navigate("task_folder/${saved.id}")
                    }
                }
                showTaskFolderSheet = false
                editingTaskFolder = null
            }
        )
    }

    if (showTaskEditorSheet) {
        val liveEditingTask = editingTask?.let { target ->
            tasks.firstOrNull { it.id == target.id } ?: target
        }
        val activeSubtasks = liveEditingTask?.let { t ->
            taskSubtasks.filter { it.taskId == t.id }
        }.orEmpty()

        TaskEditorSheet(
            editingTask = liveEditingTask,
            initialFolderId = taskEditorFolderId,
            initialStatus = taskEditorInitialStatus,
            folders = taskLists,
            profiles = householdProfiles,
            existingSubtasks = activeSubtasks,
            shoppingLists = lists,
            onDismiss = {
                showTaskEditorSheet = false
                editingTask = null
            },
            onSave = { listId, title, notes, assignedTo, status, priority, dueDate, dueTime, recurrenceRule, initialSubs ->
                viewModel.saveTask(
                    existing = liveEditingTask,
                    taskListId = listId,
                    title = title,
                    notes = notes,
                    assignedTo = assignedTo,
                    status = status,
                    priority = priority,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    reminderMinutesBefore = liveEditingTask?.reminderMinutesBefore,
                    recurrenceRule = recurrenceRule,
                    initialSubtasks = initialSubs
                )
                showTaskEditorSheet = false
                editingTask = null
            },
            onDeleteTask = { target ->
                viewModel.deleteTask(target)
                showTaskEditorSheet = false
                editingTask = null
            },
            onAddSubtask = { target, subTitle -> viewModel.addSubtask(target, subTitle) },
            onToggleSubtask = { sub -> viewModel.toggleSubtask(sub) },
            onDeleteSubtask = { sub -> viewModel.deleteSubtask(sub) },
            onSendToShoppingList = { listId, itemName, qty ->
                viewModel.sendTaskItemToShoppingList(listId, itemName, qty)
            }
        )
    }

    if (showEventSheet) {
        val liveEditingEvent = editingEvent?.let { target ->
            calendarEvents.firstOrNull { it.id == target.id } ?: target
        }
        EventEditorSheet(
            editingEvent = liveEditingEvent,
            initialDate = selectedCalendarDate,
            occurrenceDate = editingEventOccurrenceDate,
            profiles = householdProfiles,
            onDismiss = {
                showEventSheet = false
                editingEvent = null
            },
            onSave = { title, location, notes, date, isAllDay, startTime, endTime, isShared, assignedIds, colorHex, rrule, reminderMin ->
                viewModel.saveCalendarEvent(
                    existing = liveEditingEvent,
                    title = title,
                    location = location,
                    notes = notes,
                    date = date,
                    isAllDay = isAllDay,
                    startTime = startTime,
                    endTime = endTime,
                    isShared = isShared,
                    assignedMemberIds = assignedIds,
                    colorHex = colorHex,
                    recurrenceRule = rrule,
                    reminderMinutesBefore = reminderMin
                )
                selectedCalendarDateIso = date.toString()
                showEventSheet = false
                editingEvent = null
            },
            onDeleteSeries = { target ->
                viewModel.deleteCalendarEvent(target)
                showEventSheet = false
                editingEvent = null
            },
            onStopRepeatingFromDate = { target, fromDate ->
                viewModel.stopRepeatingEventFromDate(target, fromDate)
                showEventSheet = false
                editingEvent = null
            }
        )
    }

    if (showQuickAddShoppingDialog && lists.isNotEmpty()) {
        QuickAddShoppingItemDialog(
            shoppingLists = lists,
            onDismiss = { showQuickAddShoppingDialog = false },
            onAdd = { listId, rawText ->
                viewModel.quickAddShoppingItemFromDashboard(listId, rawText)
                showQuickAddShoppingDialog = false
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan Product Barcode",
            subtitle = "Point your camera at an item barcode to add it",
            isPaused = isLookingUpBarcode || scannedBarcodeResult != null,
            isLookingUp = isLookingUpBarcode,
            onDismiss = {
                showBarcodeScanner = false
                viewModel.clearScannedBarcode()
            },
            onBarcodeDetected = { rawValue, _ ->
                viewModel.lookupBarcode(rawValue)
            }
        )
    }

    scannedBarcodeResult?.let { lookup ->
        ScannedItemSheet(
            lookupResult = lookup,
            lists = lists,
            initialListId = scannerTargetListId,
            categories = categories,
            history = history,
            isAdmin = isAdmin,
            showListSelector = scannerTargetListId == null || currentRoute != "list/{listId}",
            onDismiss = {
                viewModel.clearScannedBarcode()
            },
            onConfirm = { barcode, name, qty, catId, brand, source, targetListId, saveAsRegular, scanNext ->
                viewModel.confirmScannedItem(
                    barcode = barcode,
                    name = name,
                    quantity = qty,
                    categoryId = catId,
                    brand = brand,
                    source = source,
                    listId = targetListId,
                    saveAsRegular = saveAsRegular
                )
                viewModel.clearScannedBarcode()
                if (!scanNext) {
                    showBarcodeScanner = false
                }
            }
        )
    }
}
