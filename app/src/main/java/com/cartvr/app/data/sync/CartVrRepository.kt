package com.cartvr.app.data.sync

import com.cartvr.app.data.local.dao.CartVrDao
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.ItemHistoryEntity
import com.cartvr.app.data.local.entity.ListMemberEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.local.entity.SyncStatus
import com.cartvr.app.data.preferences.AppPreferences
import com.cartvr.app.data.remote.SupabaseProvider
import com.cartvr.app.data.remote.dto.CategoryDto
import com.cartvr.app.data.remote.dto.HouseholdMemberDto
import com.cartvr.app.data.remote.dto.ItemHistoryDto
import com.cartvr.app.data.remote.dto.ListMemberDto
import com.cartvr.app.data.remote.dto.RegularItemDto
import com.cartvr.app.data.remote.dto.ShoppingItemDto
import com.cartvr.app.data.remote.dto.ShoppingListDto
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.SecureRandom
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
    private val random = SecureRandom()

    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null

    private val _syncState = MutableStateFlow(
        if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val sessionStatus: Flow<SessionStatus> = supabase.auth.sessionStatus

    val lists: Flow<List<ShoppingListEntity>> = dao.observeActiveLists()
    val listMembers: Flow<List<ListMemberEntity>> = dao.observeAllListMembers()
    val categories: Flow<List<CategoryEntity>> = dao.observeActiveCategories()
    val allShoppingItems: Flow<List<ShoppingItemEntity>> = dao.observeAllActiveShoppingItems()
    val regularItems: Flow<List<RegularItemEntity>> = dao.observeActiveRegularItems()
    val itemHistory: Flow<List<ItemHistoryEntity>> = dao.observeItemHistory()

    fun observeListItems(listId: String): Flow<List<ShoppingItemEntity>> =
        dao.observeListShoppingItems(listId)

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
        dao.clearItemHistory()
        dao.clearListMembers()
        dao.clearShoppingLists()
        dao.clearCategories()
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
                val householdId = resolveHouseholdId(userId)
                pushPendingChanges(userId)
                pullRemoteData(userId, householdId)
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

        val members = runCatching {
            supabase.postgrest["household_members"]
                .select(columns = Columns.raw("household_id, user_id, role, households(id, name)")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<HouseholdMemberDto>()
        }.getOrNull() ?: return null

        val first = members.firstOrNull() ?: return null
        prefs.setHousehold(
            id = first.householdId,
            name = first.households?.name ?: "Janse van Rensburg"
        )
        return first.householdId
    }

    private suspend fun pushPendingChanges(userId: String) {
        val pendingLists = dao.getPendingLists()
        if (pendingLists.isNotEmpty()) {
            supabase.postgrest["shopping_lists"].upsert(pendingLists.map { it.toDto() })
            pendingLists.forEach { list ->
                if (!list.isShared && list.ownerId == userId) {
                    runCatching {
                        supabase.postgrest["list_members"].delete {
                            filter {
                                eq("list_id", list.id)
                                neq("user_id", list.ownerId)
                            }
                        }
                    }
                } else if (list.deletedAt == null) {
                    runCatching {
                        supabase.postgrest["list_members"].upsert(
                            ListMemberDto(
                                listId = list.id,
                                userId = list.ownerId,
                                role = "owner",
                                joinedAt = list.createdAt
                            )
                        )
                    }
                }
            }
            dao.upsertLists(pendingLists.map { it.copy(syncStatus = SyncStatus.SYNCED) })
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

        val pendingHistory = dao.getPendingItemHistory()
        if (pendingHistory.isNotEmpty()) {
            supabase.postgrest["item_history"].upsert(pendingHistory.map { it.toDto() }) {
                onConflict = "user_id,normalized_name"
            }
            dao.upsertItemHistory(pendingHistory.map { it.copy(syncStatus = SyncStatus.SYNCED) })
        }
    }

    private suspend fun pullRemoteData(userId: String, householdId: String?) {
        val remoteLists = supabase.postgrest["shopping_lists"]
            .select()
            .decodeList<ShoppingListDto>()

        val remoteListEntities = remoteLists.map { it.toEntity() }
        val activeRemoteIds = remoteListEntities.map { it.id }
        if (activeRemoteIds.isEmpty()) {
            dao.pruneAllSyncedLists()
        } else {
            dao.pruneSyncedListsNotIn(activeRemoteIds)
            dao.upsertLists(remoteListEntities)
        }

        val remoteMembers = supabase.postgrest["list_members"]
            .select()
            .decodeList<ListMemberDto>()
        dao.replaceListMembers(remoteMembers.map { it.toEntity() })

        if (!householdId.isNullOrBlank()) {
            val remoteCategories = supabase.postgrest["categories"]
                .select { filter { eq("household_id", householdId) } }
                .decodeList<CategoryDto>()
            dao.upsertCategories(remoteCategories.map { it.toEntity() })

            val remoteRegulars = supabase.postgrest["regular_items"]
                .select { filter { eq("household_id", householdId) } }
                .decodeList<RegularItemDto>()
            dao.upsertRegularItems(remoteRegulars.map { it.toEntity() })
        }

        val remoteItems = supabase.postgrest["shopping_items"]
            .select()
            .decodeList<ShoppingItemDto>()
            .mapNotNull { it.toEntity() }
        dao.upsertShoppingItems(remoteItems)
        dao.pruneOrphanedSyncedItems()

        val remoteHistory = supabase.postgrest["item_history"]
            .select { filter { eq("user_id", userId) } }
            .decodeList<ItemHistoryDto>()
        dao.upsertItemHistory(remoteHistory.map { it.toEntity() })
    }

    suspend fun ensureRealtimeSubscribed() {
        if (!networkMonitor.isOnline.value || currentUserId() == null) return
        if (realtimeChannel != null) return

        runCatching {
            val channel = supabase.realtime.channel("cartvr-live")
            realtimeChannel = channel

            val itemChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "shopping_items"
            }
            val listChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "shopping_lists"
            }
            val memberChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "list_members"
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
                    listChanges.collect { action -> handleListRealtime(action) }
                }
                launch {
                    memberChanges.collect { syncAll() }
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
                val entity = action.decodeRecord<ShoppingItemDto>().toEntity() ?: return
                dao.upsertShoppingItem(entity)
            }
            is PostgresAction.Update -> {
                val entity = action.decodeRecord<ShoppingItemDto>().toEntity() ?: return
                dao.upsertShoppingItem(entity)
            }
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return
                dao.hardDeleteShoppingItem(id)
            }
            else -> Unit
        }
    }

    private suspend fun handleListRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert,
            is PostgresAction.Update -> {
                val dto = if (action is PostgresAction.Insert) {
                    action.decodeRecord<ShoppingListDto>()
                } else {
                    (action as PostgresAction.Update).decodeRecord<ShoppingListDto>()
                }
                val userId = currentUserId()
                if (!dto.isShared && dto.ownerId != userId) {
                    dao.deleteItemsByListId(dto.id)
                    dao.hardDeleteList(dto.id)
                } else {
                    dao.upsertList(dto.toEntity())
                }
            }
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return
                dao.deleteItemsByListId(id)
                dao.hardDeleteList(id)
            }
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

    // --- List Management (Private vs Shared) ---

    private fun generateShareCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
    }

    suspend fun createList(
        name: String,
        iconKey: String,
        colorHex: String,
        isShared: Boolean
    ): ShoppingListEntity? {
        val userId = currentUserId() ?: return null
        val now = Instant.now().toString()
        val existingCount = dao.observeActiveLists().first().size
        val newList = ShoppingListEntity(
            id = UUID.randomUUID().toString(),
            ownerId = userId,
            name = name.trim(),
            iconKey = iconKey,
            colorHex = colorHex,
            isShared = isShared,
            shareCode = if (isShared) generateShareCode() else null,
            sortOrder = existingCount,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        dao.upsertList(newList)
        dao.upsertListMembers(
            listOf(
                ListMemberEntity(
                    listId = newList.id,
                    userId = userId,
                    role = "owner",
                    joinedAt = now
                )
            )
        )
        scope.launch { syncAll() }
        return newList
    }

    suspend fun updateList(
        list: ShoppingListEntity,
        newName: String,
        iconKey: String,
        colorHex: String
    ) {
        if (list.ownerId != currentUserId()) return
        val now = Instant.now().toString()
        dao.upsertList(
            list.copy(
                name = newName.trim(),
                iconKey = iconKey,
                colorHex = colorHex,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun enableSharing(list: ShoppingListEntity): ShoppingListEntity {
        val now = Instant.now().toString()
        val updated = list.copy(
            isShared = true,
            shareCode = list.shareCode ?: generateShareCode(),
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        dao.upsertList(updated)
        scope.launch { syncAll() }
        return updated
    }

    suspend fun makeListPrivate(list: ShoppingListEntity) {
        if (list.ownerId != currentUserId()) return
        val now = Instant.now().toString()
        dao.removeNonOwnerMembers(list.id, list.ownerId)
        dao.upsertList(
            list.copy(
                isShared = false,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun deleteList(list: ShoppingListEntity) {
        if (list.ownerId != currentUserId()) return
        val now = Instant.now().toString()
        dao.upsertList(
            list.copy(
                updatedAt = now,
                deletedAt = now,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun leaveSharedList(list: ShoppingListEntity): Result<Unit> = runCatching {
        val userId = currentUserId() ?: error("Not signed in")
        if (list.ownerId == userId) error("Owner cannot leave their own list")
        if (networkMonitor.isOnline.value) {
            supabase.postgrest["list_members"].delete {
                filter {
                    eq("list_id", list.id)
                    eq("user_id", userId)
                }
            }
        }
        dao.removeMember(list.id, userId)
        dao.deleteItemsByListId(list.id)
        dao.hardDeleteList(list.id)
    }

    suspend fun joinListByCode(rawCode: String): Result<ShoppingListEntity?> = runCatching {
        val code = rawCode.trim().uppercase()
        require(code.isNotEmpty()) { "Invite code cannot be empty" }
        val joinedListId = supabase.postgrest.rpc(
            function = "join_list_by_code",
            parameters = buildJsonObject { put("p_share_code", code) }
        ).decodeAs<String>().trim('"')

        syncAll()
        dao.getListById(joinedListId)
    }

    // --- Shopping Items & History Learning ---

    suspend fun addShoppingItem(
        listId: String,
        categoryId: String?,
        name: String,
        quantity: String?,
        note: String?,
        saveAsRegular: Boolean = false
    ) {
        val userId = currentUserId()
        val householdId = prefs.householdId.first()
        val now = Instant.now().toString()
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val cleanQty = quantity?.trim()?.ifBlank { null }
        val cleanNote = note?.trim()?.ifBlank { null }

        val item = ShoppingItemEntity(
            id = UUID.randomUUID().toString(),
            listId = listId,
            householdId = householdId,
            categoryId = categoryId,
            name = cleanName,
            quantity = cleanQty,
            note = cleanNote,
            isChecked = false,
            addedBy = userId,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        dao.upsertShoppingItem(item)

        if (userId != null) {
            upsertHistoryInternal(
                userId = userId,
                name = cleanName,
                quantity = cleanQty,
                categoryId = categoryId,
                forceUpdateCategory = false
            )
        }

        if (saveAsRegular && !householdId.isNullOrBlank()) {
            val regular = RegularItemEntity(
                id = UUID.randomUUID().toString(),
                householdId = householdId,
                defaultListId = listId,
                defaultCategoryId = categoryId,
                name = cleanName,
                defaultQuantity = cleanQty,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
            dao.upsertRegularItem(regular)
        }

        scope.launch { syncAll() }
    }

    suspend fun updateShoppingItem(
        item: ShoppingItemEntity,
        learnCategory: Boolean = false
    ) {
        val now = Instant.now().toString()
        dao.upsertShoppingItem(
            item.copy(
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        )
        val userId = currentUserId()
        if (learnCategory && userId != null) {
            upsertHistoryInternal(
                userId = userId,
                name = item.name,
                quantity = item.quantity,
                categoryId = item.categoryId,
                forceUpdateCategory = true
            )
        }
        scope.launch { syncAll() }
    }

    private suspend fun upsertHistoryInternal(
        userId: String,
        name: String,
        quantity: String?,
        categoryId: String?,
        forceUpdateCategory: Boolean
    ) {
        val normalized = name.trim().lowercase()
        if (normalized.isEmpty()) return
        val now = Instant.now().toString()
        val existing = dao.getHistoryByNormalizedName(userId, normalized)
        val updated = if (existing != null) {
            existing.copy(
                displayName = name.trim(),
                defaultQuantity = quantity ?: existing.defaultQuantity,
                defaultCategoryId = if (forceUpdateCategory || existing.defaultCategoryId == null) {
                    categoryId
                } else {
                    existing.defaultCategoryId
                },
                useCount = existing.useCount + if (forceUpdateCategory) 0 else 1,
                lastUsedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        } else {
            ItemHistoryEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                normalizedName = normalized,
                displayName = name.trim(),
                defaultQuantity = quantity,
                defaultCategoryId = categoryId,
                useCount = 1,
                lastUsedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        }
        dao.upsertItemHistoryEntry(updated)
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

    suspend fun clearCompletedInList(listId: String) {
        val now = Instant.now().toString()
        val items = dao.observeListShoppingItems(listId).first().filter { it.isChecked }
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

    suspend fun addRegularToShoppingList(regular: RegularItemEntity, fallbackListId: String) {
        val targetListId = regular.defaultListId ?: fallbackListId
        addShoppingItem(
            listId = targetListId,
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
        defaultListId: String?,
        defaultCategoryId: String?,
        existingId: String? = null
    ) {
        val householdId = prefs.householdId.first() ?: return
        val now = Instant.now().toString()
        val entity = RegularItemEntity(
            id = existingId ?: UUID.randomUUID().toString(),
            householdId = householdId,
            defaultListId = defaultListId,
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
