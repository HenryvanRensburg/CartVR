package com.cartvr.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cartvr.app.CartVRApplication
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.ItemHistoryEntity
import com.cartvr.app.data.local.entity.ListMemberEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.notifications.ListSyncWorker
import com.cartvr.app.data.notifications.LiveSyncService
import com.cartvr.app.data.notifications.NotificationHelper
import com.cartvr.app.data.preferences.AppPreferences
import com.cartvr.app.data.preferences.ThemeMode
import com.cartvr.app.data.remote.dto.FamilyInviteDto
import com.cartvr.app.data.remote.dto.FamilyMemberDto
import com.cartvr.app.data.remote.dto.InvitePreviewResponse
import com.cartvr.app.data.sync.CartVrRepository
import com.cartvr.app.data.sync.ItemMutationResult
import com.cartvr.app.data.sync.SyncState
import com.cartvr.app.data.updater.GitHubUpdater
import com.cartvr.app.data.updater.UpdateInfo
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class UpdateUiState(
    val isChecking: Boolean = false,
    val availableUpdate: UpdateInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null,
    val needsUnknownSourcesPermission: Boolean = false,
    val noUpdateMessage: String? = null
)

data class JoinListUiState(
    val showDialog: Boolean = false,
    val isJoining: Boolean = false,
    val errorMessage: String? = null
)

data class FamilyUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val members: List<FamilyMemberDto> = emptyList(),
    val invites: List<FamilyInviteDto> = emptyList(),
    val errorMessage: String? = null
)

data class InviteFlowState(
    val showAcceptInvite: Boolean = false,
    val initialCode: String = "",
    val preview: InvitePreviewResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CartVrViewModel(
    private val appContext: Context,
    private val repository: CartVrRepository,
    private val prefs: AppPreferences,
    private val updater: GitHubUpdater
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemeMode.SYSTEM
    )

    val householdName: StateFlow<String?> = prefs.householdName.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "Janse van Rensburg"
    )

    val isAdmin: StateFlow<Boolean> = repository.isAdmin.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val displayName: StateFlow<String?> = repository.displayName.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val sessionStatus: StateFlow<SessionStatus> = repository.sessionStatus.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SessionStatus.Initializing
    )

    val syncState: StateFlow<SyncState> = repository.syncState
    val lastSyncError: StateFlow<String?> = repository.lastSyncError

    val lists: StateFlow<List<ShoppingListEntity>> = repository.lists.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val listMembers: StateFlow<List<ListMemberEntity>> = repository.listMembers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val categories: StateFlow<List<CategoryEntity>> = repository.categories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allShoppingItems: StateFlow<List<ShoppingItemEntity>> = repository.allShoppingItems.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val regularItems: StateFlow<List<RegularItemEntity>> = repository.regularItems.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val itemHistory: StateFlow<List<ItemHistoryEntity>> = repository.itemHistory.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val notificationsEnabled: StateFlow<Boolean> = prefs.notificationsEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val notifyOnChecked: StateFlow<Boolean> = prefs.notifyOnChecked.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val liveModeDurationHours: StateFlow<Int> = prefs.liveModeDurationHours.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LiveSyncService.DEFAULT_DURATION_HOURS
    )

    val liveModeStopAtMs: StateFlow<Long> = prefs.liveModeStopAtMs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0L
    )

    val liveModeEnabled: StateFlow<Boolean> = combine(
        LiveSyncService.isRunning,
        prefs.liveModeEnabled,
        prefs.liveModeStopAtMs
    ) { running, saved, stopAt ->
        running || (saved && stopAt > System.currentTimeMillis())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LiveSyncService.isRunning.value
    )

    private val _pendingNotificationListId = MutableStateFlow<String?>(null)
    val pendingNotificationListId: StateFlow<String?> = _pendingNotificationListId.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _joinState = MutableStateFlow(JoinListUiState())
    val joinState: StateFlow<JoinListUiState> = _joinState.asStateFlow()

    private val _pendingDeepLinkCode = MutableStateFlow<String?>(null)
    private val _joinedListEvent = MutableStateFlow<ShoppingListEntity?>(null)
    val joinedListEvent: StateFlow<ShoppingListEntity?> = _joinedListEvent.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _familyState = MutableStateFlow(FamilyUiState())
    val familyState: StateFlow<FamilyUiState> = _familyState.asStateFlow()

    private val _inviteFlowState = MutableStateFlow(InviteFlowState())
    val inviteFlowState: StateFlow<InviteFlowState> = _inviteFlowState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    repository.syncAll()
                    repository.ensureRealtimeSubscribed()
                    val code = _pendingDeepLinkCode.value
                    if (!code.isNullOrBlank()) {
                        _pendingDeepLinkCode.value = null
                        joinListByCode(code)
                    }
                }
            }
        }
        checkForUpdates(silent = true)
    }

    fun observeListItems(listId: String): Flow<List<ShoppingItemEntity>> =
        repository.observeListItems(listId)

    fun currentUserId(): String? = repository.currentUserId()
    fun userDisplayName(): String = repository.currentUserDisplayName()
    fun userEmail(): String? = repository.currentUserEmail()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val res = repository.signIn(email, password)
            _authLoading.value = false
            res.onFailure {
                _authError.value = it.localizedMessage ?: "Sign in failed. Please check your email and password."
            }
        }
    }

    fun signOut() {
        LiveSyncService.stop(appContext)
        ListSyncWorker.WorkScheduler.cancel(appContext)
        viewModelScope.launch {
            repository.signOut()
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNotificationsEnabled(enabled)
            if (enabled) {
                if (prefs.lastNotifiedAt.first().isNullOrBlank()) {
                    prefs.setLastNotifiedAt(Instant.now().toString())
                }
                ListSyncWorker.WorkScheduler.schedule(appContext)
            } else {
                ListSyncWorker.WorkScheduler.cancel(appContext)
                LiveSyncService.stop(appContext)
                prefs.setLiveMode(enabled = false, stopAtMs = 0L)
            }
        }
    }

    fun setNotifyOnChecked(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNotifyOnChecked(enabled)
        }
    }

    fun setLiveModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                prefs.setNotificationsEnabled(true)
                if (prefs.lastNotifiedAt.first().isNullOrBlank()) {
                    prefs.setLastNotifiedAt(Instant.now().toString())
                }
                ListSyncWorker.WorkScheduler.schedule(appContext)
                val hours = prefs.liveModeDurationHours.first()
                LiveSyncService.start(appContext, hours)
            } else {
                LiveSyncService.stop(appContext)
                prefs.setLiveMode(enabled = false, stopAtMs = 0L)
            }
        }
    }

    fun setLiveModeDurationHours(hours: Int) {
        viewModelScope.launch {
            val clamped = hours.coerceIn(1, 4)
            prefs.setLiveModeDurationHours(clamped)
            if (LiveSyncService.isRunning.value) {
                LiveSyncService.start(appContext, clamped)
            }
        }
    }

    fun previewNotificationChime() {
        NotificationHelper.previewChime(appContext)
    }

    fun openNotificationChannelSettings() {
        NotificationHelper.openChannelSoundSettings(appContext)
    }

    fun openListFromNotification(listId: String) {
        _pendingNotificationListId.value = listId
    }

    fun consumePendingNotificationListId() {
        _pendingNotificationListId.value = null
    }

    fun setActiveOpenListId(listId: String?) {
        repository.setActiveOpenListId(listId)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    fun toggleQuickTheme(currentlyDark: Boolean) {
        viewModelScope.launch {
            prefs.setThemeMode(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            repository.syncAll(force = true)
            repository.ensureRealtimeSubscribed()
        }
    }

    // --- Lists ---

    fun createList(
        name: String,
        iconKey: String,
        colorHex: String,
        isShared: Boolean,
        onCreated: (ShoppingListEntity) -> Unit
    ) {
        viewModelScope.launch {
            val created = repository.createList(name, iconKey, colorHex, isShared) ?: return@launch
            onCreated(created)
        }
    }

    fun updateList(list: ShoppingListEntity, name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            repository.updateList(list, name, iconKey, colorHex)
        }
    }

    fun prepareShare(list: ShoppingListEntity, onReady: (ShoppingListEntity) -> Unit) {
        viewModelScope.launch {
            val shared = if (list.isShared && !list.shareCode.isNullOrBlank()) {
                list
            } else {
                repository.enableSharing(list)
            }
            onReady(shared)
        }
    }

    fun makeListPrivate(list: ShoppingListEntity) {
        viewModelScope.launch {
            repository.makeListPrivate(list)
            _statusBanner.value = "\"${list.name}\" is now private"
        }
    }

    fun deleteList(list: ShoppingListEntity) {
        viewModelScope.launch {
            repository.deleteList(list)
            _statusBanner.value = "Deleted \"${list.name}\""
        }
    }

    fun leaveSharedList(list: ShoppingListEntity, onLeft: () -> Unit = {}) {
        viewModelScope.launch {
            repository.leaveSharedList(list)
                .onSuccess {
                    _statusBanner.value = "Left \"${list.name}\""
                    onLeft()
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Could not leave list"
                }
        }
    }

    fun openJoinDialog() {
        _joinState.value = JoinListUiState(showDialog = true)
    }

    fun dismissJoinDialog() {
        _joinState.value = JoinListUiState(showDialog = false)
    }

    fun handleIncomingShareCode(code: String) {
        val clean = code.trim().uppercase()
        if (clean.isEmpty()) return
        if (sessionStatus.value is SessionStatus.Authenticated) {
            joinListByCode(clean)
        } else {
            _pendingDeepLinkCode.value = clean
        }
    }

    fun joinListByCode(code: String) {
        viewModelScope.launch {
            _joinState.value = _joinState.value.copy(isJoining = true, errorMessage = null)
            repository.joinListByCode(code)
                .onSuccess { joined ->
                    _joinState.value = JoinListUiState(showDialog = false)
                    if (joined != null) {
                        _joinedListEvent.value = joined
                        _statusBanner.value = "Joined \"${joined.name}\""
                    } else {
                        _statusBanner.value = "Joined shared list"
                    }
                }
                .onFailure { err ->
                    _joinState.value = _joinState.value.copy(
                        isJoining = false,
                        errorMessage = err.localizedMessage ?: "Invalid invite code or offline."
                    )
                    _statusBanner.value = "Could not join list: ${err.localizedMessage ?: "check code or connection"}"
                }
        }
    }

    fun consumeJoinedListEvent() {
        _joinedListEvent.value = null
    }

    fun consumeStatusBanner() {
        _statusBanner.value = null
    }

    // --- Items ---

    fun addShoppingItem(
        listId: String,
        categoryId: String?,
        name: String,
        quantity: String?,
        note: String?,
        saveAsRegular: Boolean = false
    ) {
        viewModelScope.launch {
            when (val res = repository.addShoppingItem(listId, categoryId, name, quantity, note, saveAsRegular)) {
                is ItemMutationResult.UpdatedExisting ->
                    _statusBanner.value = "Updated ${res.itemName} on ${res.listName} (already on list)"
                is ItemMutationResult.AlreadyExists ->
                    _statusBanner.value = "${res.itemName} is already on ${res.listName}"
                is ItemMutationResult.RestoredFromCompleted ->
                    _statusBanner.value = "Moved ${res.itemName} back to ${res.listName}"
                is ItemMutationResult.DuplicateBlocked ->
                    _statusBanner.value = res.message
                is ItemMutationResult.Added -> Unit
            }
        }
    }

    fun updateShoppingItem(item: ShoppingItemEntity, learnCategory: Boolean) {
        viewModelScope.launch {
            val res = repository.updateShoppingItem(item, learnCategory)
            if (res is ItemMutationResult.DuplicateBlocked) {
                _statusBanner.value = res.message
            }
        }
    }

    fun toggleItemChecked(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.toggleItemChecked(item)
        }
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.softDeleteItem(item)
        }
    }

    fun clearCompletedInList(listId: String) {
        viewModelScope.launch {
            repository.clearCompletedInList(listId)
        }
    }

    fun addRegularToList(regular: RegularItemEntity, fallbackListId: String) {
        viewModelScope.launch {
            when (val res = repository.addRegularToShoppingList(regular, fallbackListId)) {
                is ItemMutationResult.Added ->
                    _statusBanner.value = "Added ${res.itemName} to ${res.listName ?: "list"}"
                is ItemMutationResult.UpdatedExisting ->
                    _statusBanner.value = "Updated ${res.itemName} on ${res.listName} (already on list)"
                is ItemMutationResult.AlreadyExists ->
                    _statusBanner.value = "${res.itemName} is already on ${res.listName}"
                is ItemMutationResult.RestoredFromCompleted ->
                    _statusBanner.value = "Moved ${res.itemName} back to ${res.listName}"
                is ItemMutationResult.DuplicateBlocked ->
                    _statusBanner.value = res.message
            }
        }
    }

    fun saveRegularItem(
        name: String,
        defaultQuantity: String?,
        defaultListId: String?,
        defaultCategoryId: String?,
        existingId: String? = null
    ) {
        viewModelScope.launch {
            val res = repository.saveRegularItem(name, defaultQuantity, defaultListId, defaultCategoryId, existingId)
            if (res is ItemMutationResult.DuplicateBlocked) {
                _statusBanner.value = res.message
            }
        }
    }

    fun deleteRegularItem(regular: RegularItemEntity) {
        viewModelScope.launch {
            repository.softDeleteRegularItem(regular)
        }
    }

    fun updateCategoryOrder(reordered: List<CategoryEntity>) {
        viewModelScope.launch {
            repository.updateCategoryOrder(reordered)
        }
    }

    // --- App Updates ---

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isChecking = !silent,
                errorMessage = null,
                noUpdateMessage = null
            )
            val result = updater.checkForUpdate()
            result.onSuccess { info ->
                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    availableUpdate = info,
                    noUpdateMessage = if (info == null && !silent) "You're on the latest version!" else null
                )
            }.onFailure { err ->
                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    errorMessage = if (!silent) (err.localizedMessage ?: "Could not check for updates.") else null
                )
            }
        }
    }

    fun startUpdateDownload() {
        val info = _updateState.value.availableUpdate ?: return
        if (!updater.canInstallUnknownApps()) {
            _updateState.value = _updateState.value.copy(needsUnknownSourcesPermission = true)
            return
        }
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                errorMessage = null,
                needsUnknownSourcesPermission = false
            )
            val result = updater.downloadAndInstall(info) { progress ->
                _updateState.value = _updateState.value.copy(downloadProgress = progress)
            }
            result.onSuccess {
                _updateState.value = _updateState.value.copy(
                    isDownloading = false,
                    availableUpdate = null
                )
            }.onFailure { err ->
                _updateState.value = _updateState.value.copy(
                    isDownloading = false,
                    errorMessage = err.localizedMessage ?: "Failed to download or install update."
                )
            }
        }
    }

    fun openInstallUnknownAppsSettings() {
        updater.openUnknownAppSourcesSettings()
        _updateState.value = _updateState.value.copy(needsUnknownSourcesPermission = false)
    }

    fun dismissUpdateDialog() {
        _updateState.value = _updateState.value.copy(
            availableUpdate = null,
            errorMessage = null,
            needsUnknownSourcesPermission = false
        )
    }

    // --- Family Admin & Invites ---

    fun loadFamilyOverview() {
        viewModelScope.launch {
            _familyState.value = _familyState.value.copy(isLoading = true, errorMessage = null)
            repository.fetchFamilyOverview()
                .onSuccess { res ->
                    _familyState.value = _familyState.value.copy(
                        isLoading = false,
                        members = res.members,
                        invites = res.invites,
                        errorMessage = res.error
                    )
                }
                .onFailure { err ->
                    _familyState.value = _familyState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Could not load family members."
                    )
                }
        }
    }

    fun createFamilyInvite(
        invitedName: String,
        role: String,
        autoListIds: List<String>,
        onCreated: (FamilyInviteDto) -> Unit
    ) {
        viewModelScope.launch {
            _familyState.value = _familyState.value.copy(isActionInProgress = true, errorMessage = null)
            repository.createFamilyInvite(invitedName, role, autoListIds)
                .onSuccess { invite ->
                    _familyState.value = _familyState.value.copy(isActionInProgress = false)
                    loadFamilyOverview()
                    onCreated(invite)
                }
                .onFailure { err ->
                    _familyState.value = _familyState.value.copy(
                        isActionInProgress = false,
                        errorMessage = err.localizedMessage ?: "Failed to create invite."
                    )
                }
        }
    }

    fun createMemberDirectly(
        displayName: String,
        email: String,
        password: String,
        role: String,
        autoListIds: List<String>
    ) {
        viewModelScope.launch {
            _familyState.value = _familyState.value.copy(isActionInProgress = true, errorMessage = null)
            repository.createMemberDirectly(displayName, email, password, role, autoListIds)
                .onSuccess {
                    _familyState.value = _familyState.value.copy(isActionInProgress = false)
                    _statusBanner.value = "Added $displayName to family"
                    loadFamilyOverview()
                }
                .onFailure { err ->
                    _familyState.value = _familyState.value.copy(
                        isActionInProgress = false,
                        errorMessage = err.localizedMessage ?: "Failed to create member."
                    )
                }
        }
    }

    fun revokeFamilyInvite(invite: FamilyInviteDto) {
        viewModelScope.launch {
            _familyState.value = _familyState.value.copy(isActionInProgress = true, errorMessage = null)
            repository.revokeFamilyInvite(invite.id)
                .onSuccess {
                    _familyState.value = _familyState.value.copy(isActionInProgress = false)
                    _statusBanner.value = "Revoked invite for ${invite.invitedName}"
                    loadFamilyOverview()
                }
                .onFailure { err ->
                    _familyState.value = _familyState.value.copy(
                        isActionInProgress = false,
                        errorMessage = err.localizedMessage ?: "Failed to revoke invite."
                    )
                }
        }
    }

    fun updateFamilyMemberRole(member: FamilyMemberDto, newRole: String) {
        viewModelScope.launch {
            _familyState.value = _familyState.value.copy(isActionInProgress = true, errorMessage = null)
            repository.updateFamilyMemberRole(member.userId, newRole)
                .onSuccess {
                    _familyState.value = _familyState.value.copy(isActionInProgress = false)
                    _statusBanner.value = "${member.displayName} is now ${newRole.replaceFirstChar { it.uppercase() }}"
                    loadFamilyOverview()
                }
                .onFailure { err ->
                    _familyState.value = _familyState.value.copy(
                        isActionInProgress = false,
                        errorMessage = err.localizedMessage ?: "Failed to update role."
                    )
                }
        }
    }

    fun removeFamilyMember(member: FamilyMemberDto) {
        viewModelScope.launch {
            _familyState.value = _familyState.value.copy(isActionInProgress = true, errorMessage = null)
            repository.removeFamilyMember(member.userId)
                .onSuccess {
                    _familyState.value = _familyState.value.copy(isActionInProgress = false)
                    _statusBanner.value = "Removed ${member.displayName}"
                    loadFamilyOverview()
                }
                .onFailure { err ->
                    _familyState.value = _familyState.value.copy(
                        isActionInProgress = false,
                        errorMessage = err.localizedMessage ?: "Failed to remove member."
                    )
                }
        }
    }

    fun openAcceptInviteScreen(initialCode: String = "") {
        _inviteFlowState.value = InviteFlowState(
            showAcceptInvite = true,
            initialCode = initialCode.trim().uppercase()
        )
    }

    fun closeAcceptInviteScreen() {
        _inviteFlowState.value = InviteFlowState(showAcceptInvite = false)
    }

    fun handleIncomingFamilyInviteCode(code: String) {
        val clean = code.trim().uppercase()
        if (clean.isEmpty()) return
        if (sessionStatus.value is SessionStatus.Authenticated) {
            _statusBanner.value = "You're already signed in. Sign out first to accept a family invite."
        } else {
            openAcceptInviteScreen(clean)
        }
    }

    fun previewFamilyInvite(code: String) {
        viewModelScope.launch {
            _inviteFlowState.value = _inviteFlowState.value.copy(isLoading = true, errorMessage = null)
            repository.previewFamilyInvite(code)
                .onSuccess { preview ->
                    _inviteFlowState.value = _inviteFlowState.value.copy(
                        isLoading = false,
                        preview = preview,
                        errorMessage = if (!preview.valid) (preview.error ?: "Invalid invite code") else null
                    )
                }
                .onFailure { err ->
                    _inviteFlowState.value = _inviteFlowState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Could not verify invite code."
                    )
                }
        }
    }

    fun acceptFamilyInvite(code: String, email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _inviteFlowState.value = _inviteFlowState.value.copy(isLoading = true, errorMessage = null)
            repository.acceptFamilyInvite(code, email, password, displayName)
                .onSuccess {
                    _inviteFlowState.value = InviteFlowState(showAcceptInvite = false)
                }
                .onFailure { err ->
                    _inviteFlowState.value = _inviteFlowState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Failed to create account."
                    )
                }
        }
    }

    companion object {
        fun factory(app: CartVRApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CartVrViewModel(
                        app.applicationContext,
                        app.repository,
                        app.preferences,
                        app.updater
                    ) as T
                }
            }
    }
}
