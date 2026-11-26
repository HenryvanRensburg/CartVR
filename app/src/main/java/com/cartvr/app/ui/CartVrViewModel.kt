package com.cartvr.app.ui

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
import com.cartvr.app.data.preferences.AppPreferences
import com.cartvr.app.data.preferences.ThemeMode
import com.cartvr.app.data.sync.CartVrRepository
import com.cartvr.app.data.sync.SyncState
import com.cartvr.app.data.updater.GitHubUpdater
import com.cartvr.app.data.updater.UpdateInfo
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

class CartVrViewModel(
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

    val sessionStatus: StateFlow<SessionStatus> = repository.sessionStatus.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SessionStatus.Initializing
    )

    val syncState: StateFlow<SyncState> = repository.syncState

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
        viewModelScope.launch {
            repository.signOut()
        }
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
            repository.syncAll()
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
            repository.addShoppingItem(listId, categoryId, name, quantity, note, saveAsRegular)
        }
    }

    fun updateShoppingItem(item: ShoppingItemEntity, learnCategory: Boolean) {
        viewModelScope.launch {
            repository.updateShoppingItem(item, learnCategory)
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
            repository.addRegularToShoppingList(regular, fallbackListId)
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
            repository.saveRegularItem(name, defaultQuantity, defaultListId, defaultCategoryId, existingId)
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

    companion object {
        fun factory(app: CartVRApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CartVrViewModel(app.repository, app.preferences, app.updater) as T
                }
            }
    }
}
