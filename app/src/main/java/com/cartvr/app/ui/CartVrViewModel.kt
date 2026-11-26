package com.cartvr.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cartvr.app.CartVRApplication
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.StoreEntity
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

    val lastSyncedAt: StateFlow<String?> = prefs.lastSyncedAt.stateIn(
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

    val stores: StateFlow<List<StoreEntity>> = repository.stores.stateIn(
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

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    repository.syncAll()
                    repository.ensureRealtimeSubscribed()
                }
            }
        }
        checkForUpdates(silent = true)
    }

    fun observeStoreItems(storeId: String): Flow<List<ShoppingItemEntity>> =
        repository.observeStoreItems(storeId)

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

    fun addShoppingItem(
        storeId: String,
        categoryId: String?,
        name: String,
        quantity: String?,
        note: String?,
        saveAsRegular: Boolean
    ) {
        viewModelScope.launch {
            repository.addShoppingItem(storeId, categoryId, name, quantity, note, saveAsRegular)
        }
    }

    fun updateShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.updateShoppingItem(item)
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

    fun clearCompletedInStore(storeId: String) {
        viewModelScope.launch {
            repository.clearCompletedInStore(storeId)
        }
    }

    fun addRegularToList(regular: RegularItemEntity, fallbackStoreId: String) {
        viewModelScope.launch {
            repository.addRegularToShoppingList(regular, fallbackStoreId)
        }
    }

    fun saveRegularItem(
        name: String,
        defaultQuantity: String?,
        defaultStoreId: String?,
        defaultCategoryId: String?,
        existingId: String? = null
    ) {
        viewModelScope.launch {
            repository.saveRegularItem(name, defaultQuantity, defaultStoreId, defaultCategoryId, existingId)
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

    fun clearNoUpdateMessage() {
        _updateState.value = _updateState.value.copy(noUpdateMessage = null)
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
