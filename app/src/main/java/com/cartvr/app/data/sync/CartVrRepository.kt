package com.cartvr.app.data.sync

import com.cartvr.app.data.local.dao.CartVrDao
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.StoreEntity
import com.cartvr.app.data.local.entity.SyncStatus
import com.cartvr.app.data.preferences.AppPreferences
import com.cartvr.app.data.remote.SupabaseProvider
import com.cartvr.app.data.remote.dto.CategoryDto
import com.cartvr.app.data.remote.dto.HouseholdMemberDto
import com.cartvr.app.data.remote.dto.RegularItemDto
import com.cartvr.app.data.remote.dto.ShoppingItemDto
import com.cartvr.app.data.remote.dto.StoreDto
import com.cartvr.app.data.remote.dto.toDto
import com.cartvr.app.data.remote.dto.toEntity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

enum class SyncState {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR
}

class CartVrRepository(
    private val dao: CartVrDao,
    private val prefs: AppPreferences,
    private val networkMonitor: NetworkMonitor
) {
    private val supabase = SupabaseProvider.client
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null

    private val _syncState = MutableStateFlow(
        if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val sessionStatus: Flow<SessionStatus> = supabase.auth.sessionStatus

    val stores: Flow<List<StoreEntity>> = dao.observeActiveStores()
    val categories: Flow<List<CategoryEntity>> = dao.observeActiveCategories()
    val allShoppingItems: Flow<List<ShoppingItemEntity>> = dao.observeAllActiveShoppingItems()
    val regularItems: Flow<List<RegularItemEntity>> = dao.observeActiveRegularItems()

    fun observeStoreItems(storeId: String): Flow<List<ShoppingItemEntity>> =
        dao.observeStoreShoppingItems(storeId)

    init {
        networkMonitor.isOnline
            .onEach { online ->
                if (!online) {
                    _syncState.value = SyncState.OFFLINE
                } else if (supabase.auth.currentUserOrNull() != null) {
                    scope.launch {
                        syncAll()
                        ensureRealtimeSubscribed()
                    }
                }
            }
            .launchIn(scope)
    }

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    fun currentUserEmail(): String? = supabase.auth.currentUserOrNull()?.email

    fun currentUserDisplayName(): String {
        val uid = currentUserId()
        return when (uid) {
            "862590df-507f-4c03-9417-f3703b8327c2" -> "Henry"
            "fc1b039d-1b1a-4dce-bbc4-a4f8dd5a94d2" -> "Jolene"
            else -> currentUserEmail()?.substringBefore("@")
                ?.replaceFirstChar { it.uppercase() } ?: "Family"
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        syncAll()
        ensureRealtimeSubscribed()
    }

    suspend fun signOut() {
        runCatching {
            realtimeJob?.cancel()
            realtimeChannel?.unsubscribe()
            realtimeChannel = null
            supabase.auth.signOut()
        }
        dao.clearShoppingItems()
        dao.clearRegularItems()
        dao.clearCategories()
        dao.clearStores()
        prefs.clearSessionData()
    }

    suspend fun syncAll() {
        if (!networkMonitor.isOnline.value) {
            _syncState.value = SyncState.OFFLINE
            return
        }
        val userId = currentUserId() ?: return

        syncMutex.withLock {
            _syncState.value = SyncState.SYNCING
            val result = runCatching {
                val householdId = resolveHouseholdId(userId) ?: return@withLock
                pushPendingChanges()
                pullRemoteData(householdId)
                prefs.setLastSyncedAt(Instant.now().toString())
            }
            _syncState.value = if (result.isSuccess) {
                SyncState.SYNCED
            } else if (!networkMonitor.isOnline.value) {
                SyncState.OFFLINE
            } else {
                SyncState.ERROR
            }
        }
    }

    private suspend fun resolveHouseholdId(userId: String): String? {
        val cached = prefs.householdId.first()
        if (!cached.isNullOrBlank()) return cached

        val members = supabase.postgrest["household_members"]
            .select(columns = Columns.raw("household_id, user_id, role, households(id, name)")) {
                filter { eq("user_id", userId) }
            }
            .decodeList<HouseholdMemberDto>()

        val first = members.firstOrNull() ?: return null
        prefs.setHousehold(
            id = first.householdId,
            name = first.households?.name ?: "Janse van Rensburg"
        )
        return first.householdId
    }

    private suspend fun pushPendingChanges() {
        val pendingStores = dao.getPendingStores()
        if (pendingStores.isNotEmpty()) {
            supabase.postgrest["stores"].upsert(pendingStores.map { it.toDto() })
            dao.upsertStores(pendingStores.map { it.copy(syncStatus = SyncStatus.SYNCED) })
        }

        val pendingCategories = dao.getPendingCategories()
        if (pendingCategories.isNotEmpty()) {
            supabase.postgrest["categories"].upsert(pendingCategories.map { it.toDto() })
            dao.upsertCategories(pendingCategories.map { it.copy(syncStatus = SyncStatus.SYNCED) })
        }

        val pendingItems = dao.getPendingShoppingItems()
        if (pendingItems.isNotEmpty()) {
            supabase.postgrest["shopping_items"].upsert(pendingItems.map { it.toDto() })
            dao.upsertShoppingItems(pendingItems.map { it.copy(syncStatus = SyncStatus.SYNCED) })
        }

        val pendingRegulars = dao.getPendingRegularItems()
        if (pendingRegulars.isNotEmpty()) {
            supabase.postgrest["regular_items"].upsert(pendingRegulars.map { it.toDto() })
            dao.upsertRegularItems(pendingRegulars.map { it.copy(syncStatus = SyncStatus.SYNCED) })
        }
    }

    private suspend fun pullRemoteData(householdId: String) {
        val remoteStores = supabase.postgrest["stores"]
            .select { filter { eq("household_id", householdId) } }
            .decodeList<StoreDto>()
        dao.upsertStores(remoteStores.map { it.toEntity() })

        val remoteCategories = supabase.postgrest["categories"]
            .select { filter { eq("household_id", householdId) } }
            .decodeList<CategoryDto>()
        dao.upsertCategories(remoteCategories.map { it.toEntity() })

        val remoteItems = supabase.postgrest["shopping_items"]
            .select { filter { eq("household_id", householdId) } }
            .decodeList<ShoppingItemDto>()
        dao.upsertShoppingItems(remoteItems.map { it.toEntity() })

        val remoteRegulars = supabase.postgrest["regular_items"]
            .select { filter { eq("household_id", householdId) } }
            .decodeList<RegularItemDto>()
        dao.upsertRegularItems(remoteRegulars.map { it.toEntity() })
    }

    suspend fun ensureRealtimeSubscribed() {
        if (!networkMonitor.isOnline.value || currentUserId() == null) return
        if (realtimeChannel != null) return

        runCatching {
            val channel = supabase.realtime.channel("cartvr-household")
            realtimeChannel = channel

            val itemChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "shopping_items"
            }
            val storeChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "stores"
            }
            val categoryChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "categories"
            }
            val regularChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "regular_items"
            }

            realtimeJob?.cancel()
            realtimeJob = scope.launch {
                launch {
                    itemChanges.collect { action -> handleShoppingItemRealtime(action) }
                }
                launch {
                    storeChanges.collect { action -> handleStoreRealtime(action) }
                }
                launch {
                    categoryChanges.collect { action -> handleCategoryRealtime(action) }
                }
                launch {
                    regularChanges.collect { action -> handleRegularRealtime(action) }
                }
            }

            channel.subscribe()
        }
    }

    private suspend fun handleShoppingItemRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert -> {
                val dto = action.decodeRecord<ShoppingItemDto>()
                dao.upsertShoppingItem(dto.toEntity())
            }
            is PostgresAction.Update -> {
                val dto = action.decodeRecord<ShoppingItemDto>()
                dao.upsertShoppingItem(dto.toEntity())
            }
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return
                dao.hardDeleteShoppingItem(id)
            }
            else -> Unit
        }
    }

    private suspend fun handleStoreRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert -> dao.upsertStore(action.decodeRecord<StoreDto>().toEntity())
            is PostgresAction.Update -> dao.upsertStore(action.decodeRecord<StoreDto>().toEntity())
            else -> Unit
        }
    }

    private suspend fun handleCategoryRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert -> dao.upsertCategory(action.decodeRecord<CategoryDto>().toEntity())
            is PostgresAction.Update -> dao.upsertCategory(action.decodeRecord<CategoryDto>().toEntity())
            else -> Unit
        }
    }

    private suspend fun handleRegularRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert -> dao.upsertRegularItem(action.decodeRecord<RegularItemDto>().toEntity())
            is PostgresAction.Update -> dao.upsertRegularItem(action.decodeRecord<RegularItemDto>().toEntity())
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return
                dao.hardDeleteRegularItem(id)
            }
            else -> Unit
        }
    }

    // --- Local-First Mutations (Instant Room write + background sync) ---

    suspend fun addShoppingItem(
        storeId: String,
        categoryId: String?,
        name: String,
        quantity: String?,
        note: String?,
        saveAsRegular: Boolean = false
    ) {
        val householdId = prefs.householdId.first() ?: return
        val now = Instant.now().toString()
        val item = ShoppingItemEntity(
            id = UUID.randomUUID().toString(),
            householdId = householdId,
            storeId = storeId,
            categoryId = categoryId,
            name = name.trim(),
            quantity = quantity?.trim()?.ifBlank { null },
            note = note?.trim()?.ifBlank { null },
            isChecked = false,
            addedBy = currentUserId(),
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        dao.upsertShoppingItem(item)

        if (saveAsRegular) {
            val regular = RegularItemEntity(
                id = UUID.randomUUID().toString(),
                householdId = householdId,
                defaultStoreId = storeId,
                defaultCategoryId = categoryId,
                name = name.trim(),
                defaultQuantity = quantity?.trim()?.ifBlank { null },
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
            dao.upsertRegularItem(regular)
        }

        scope.launch { syncAll() }
    }

    suspend fun updateShoppingItem(item: ShoppingItemEntity) {
        val now = Instant.now().toString()
        dao.upsertShoppingItem(
            item.copy(
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun toggleItemChecked(item: ShoppingItemEntity) {
        val now = Instant.now().toString()
        dao.upsertShoppingItem(
            item.copy(
                isChecked = !item.isChecked,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun softDeleteItem(item: ShoppingItemEntity) {
        val now = Instant.now().toString()
        dao.upsertShoppingItem(
            item.copy(
                updatedAt = now,
                deletedAt = now,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun clearCompletedInStore(storeId: String) {
        val now = Instant.now().toString()
        val items = dao.observeStoreShoppingItems(storeId).first().filter { it.isChecked }
        if (items.isEmpty()) return
        dao.upsertShoppingItems(
            items.map {
                it.copy(
                    updatedAt = now,
                    deletedAt = now,
                    syncStatus = SyncStatus.PENDING_DELETE
                )
            }
        )
        scope.launch { syncAll() }
    }

    suspend fun addRegularToShoppingList(regular: RegularItemEntity, fallbackStoreId: String) {
        val targetStoreId = regular.defaultStoreId ?: fallbackStoreId
        addShoppingItem(
            storeId = targetStoreId,
            categoryId = regular.defaultCategoryId,
            name = regular.name,
            quantity = regular.defaultQuantity,
            note = null,
            saveAsRegular = false
        )
    }

    suspend fun saveRegularItem(
        name: String,
        defaultQuantity: String?,
        defaultStoreId: String?,
        defaultCategoryId: String?,
        existingId: String? = null
    ) {
        val householdId = prefs.householdId.first() ?: return
        val now = Instant.now().toString()
        val entity = RegularItemEntity(
            id = existingId ?: UUID.randomUUID().toString(),
            householdId = householdId,
            defaultStoreId = defaultStoreId,
            defaultCategoryId = defaultCategoryId,
            name = name.trim(),
            defaultQuantity = defaultQuantity?.trim()?.ifBlank { null },
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        dao.upsertRegularItem(entity)
        scope.launch { syncAll() }
    }

    suspend fun softDeleteRegularItem(regular: RegularItemEntity) {
        val now = Instant.now().toString()
        dao.upsertRegularItem(
            regular.copy(
                updatedAt = now,
                deletedAt = now,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun updateCategoryOrder(reordered: List<CategoryEntity>) {
        val now = Instant.now().toString()
        val updated = reordered.mapIndexed { index, category ->
            category.copy(
                sortOrder = index,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        }
        dao.upsertCategories(updated)
        scope.launch { syncAll() }
    }
}
