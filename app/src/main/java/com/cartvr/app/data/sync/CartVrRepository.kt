package com.cartvr.app.data.sync

import com.cartvr.app.data.local.dao.CartVrDao
import com.cartvr.app.data.local.dao.OrganizerDao
import com.cartvr.app.data.local.entity.CalendarEventEntity
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.data.local.entity.ItemHistoryEntity
import com.cartvr.app.data.local.entity.ListMemberEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.local.entity.SyncStatus
import com.cartvr.app.data.local.entity.TaskEntity
import com.cartvr.app.data.local.entity.TaskListEntity
import com.cartvr.app.data.local.entity.TaskSubtaskEntity
import com.cartvr.app.data.notifications.ItemChangeKind
import com.cartvr.app.data.notifications.NotificationBundler
import com.cartvr.app.data.notifications.PendingListChange
import com.cartvr.app.data.preferences.AppPreferences
import com.cartvr.app.data.remote.FamilyApiClient
import com.cartvr.app.data.remote.SupabaseProvider
import com.cartvr.app.data.remote.dto.CalendarEventDto
import com.cartvr.app.data.remote.dto.CategoryDto
import com.cartvr.app.data.remote.dto.EdgeActionResponse
import com.cartvr.app.data.remote.dto.FamilyInviteDto
import com.cartvr.app.data.remote.dto.FamilyInviteInsertDto
import com.cartvr.app.data.remote.dto.FamilyOverviewResponse
import com.cartvr.app.data.remote.dto.HouseholdMemberDto
import com.cartvr.app.data.remote.dto.InvitePreviewResponse
import com.cartvr.app.data.remote.dto.ItemHistoryDto
import com.cartvr.app.data.remote.dto.ListMemberDto
import com.cartvr.app.data.remote.dto.ProfileDto
import com.cartvr.app.data.remote.dto.RegularItemDto
import com.cartvr.app.data.remote.dto.ShoppingItemDto
import com.cartvr.app.data.remote.dto.ShoppingListDto
import com.cartvr.app.data.remote.dto.TaskDto
import com.cartvr.app.data.remote.dto.TaskListDto
import com.cartvr.app.data.remote.dto.TaskSubtaskDto
import com.cartvr.app.data.remote.dto.toDto
import com.cartvr.app.data.remote.dto.toEntity
import com.cartvr.app.domain.TaskRecurrence
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class SyncState {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR
}

sealed class ItemMutationResult {
    data class Added(val itemName: String, val listName: String? = null) : ItemMutationResult()
    data class UpdatedExisting(val itemName: String, val listName: String) : ItemMutationResult()
    data class AlreadyExists(val itemName: String, val listName: String) : ItemMutationResult()
    data class RestoredFromCompleted(val itemName: String, val listName: String) : ItemMutationResult()
    data class DuplicateBlocked(val message: String) : ItemMutationResult()
}

class CartVrRepository(
    private val dao: CartVrDao,
    private val organizerDao: OrganizerDao,
    private val prefs: AppPreferences,
    private val networkMonitor: NetworkMonitor,
    private val notificationBundler: NotificationBundler? = null
) {
    private val supabase = SupabaseProvider.client
    private val familyApi = FamilyApiClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val random = SecureRandom()
    private val profileNameCache = ConcurrentHashMap<String, String>()

    @Volatile
    private var activeOpenListId: String? = null

    @Volatile
    private var isAppInForeground: Boolean = false

    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null

    private val _syncState = MutableStateFlow(
        if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val sessionStatus: Flow<SessionStatus> = supabase.auth.sessionStatus
    val isAdmin: Flow<Boolean> = prefs.isAdmin
    val displayName: Flow<String?> = prefs.displayName

    val lists: Flow<List<ShoppingListEntity>> = dao.observeActiveLists()
    val listMembers: Flow<List<ListMemberEntity>> = dao.observeAllListMembers()
    val categories: Flow<List<CategoryEntity>> = dao.observeActiveCategories()
    val allShoppingItems: Flow<List<ShoppingItemEntity>> = dao.observeAllActiveShoppingItems()
    val regularItems: Flow<List<RegularItemEntity>> = dao.observeActiveRegularItems()
    val itemHistory: Flow<List<ItemHistoryEntity>> = dao.observeItemHistory()

    val householdProfiles: Flow<List<HouseholdProfileEntity>> = organizerDao.observeHouseholdProfiles()
    val taskLists: Flow<List<TaskListEntity>> = organizerDao.observeActiveTaskLists()
    val tasks: Flow<List<TaskEntity>> = organizerDao.observeActiveTasks()
    val taskSubtasks: Flow<List<TaskSubtaskEntity>> = organizerDao.observeActiveSubtasks()
    val calendarEvents: Flow<List<CalendarEventEntity>> = organizerDao.observeActiveCalendarEvents()

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

    fun setActiveOpenListId(listId: String?) {
        activeOpenListId = listId
    }

    fun setAppForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
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
        organizerDao.clearAll()
        prefs.clearSessionData()
    }

    suspend fun syncAll(force: Boolean = false) {
        val online = networkMonitor.refresh()
        if (!force && !online) {
            _syncState.value = SyncState.OFFLINE
            return
        }
        val userId = currentUserId() ?: return

        syncMutex.withLock {
            _syncState.value = SyncState.SYNCING
            _lastSyncError.value = null
            val result = runCatching {
                val householdId = resolveHouseholdId(userId)
                pushPendingChanges(userId)
                pullRemoteData(userId, householdId)
                prefs.setLastSyncedAt(Instant.now().toString())
            }
            if (result.isSuccess) {
                networkMonitor.markOnline()
                _syncState.value = SyncState.SYNCED
                scope.launch { ensureRealtimeSubscribed() }
            } else {
                val err = result.exceptionOrNull()
                _lastSyncError.value = err?.localizedMessage ?: err?.javaClass?.simpleName ?: "Unknown sync error"
                _syncState.value = if (!networkMonitor.refresh() || err is java.io.IOException) {
                    SyncState.OFFLINE
                } else {
                    SyncState.ERROR
                }
            }
        }
    }

    private suspend fun resolveHouseholdId(userId: String): String? {
        val members = runCatching {
            supabase.postgrest["household_members"]
                .select(columns = Columns.raw("household_id, user_id, role, households(id, name)")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<HouseholdMemberDto>()
        }.getOrNull()

        val first = members?.firstOrNull()
        if (first != null) {
            prefs.setHousehold(
                id = first.householdId,
                name = first.households?.name ?: "Janse van Rensburg"
            )
            val profile = runCatching {
                supabase.postgrest["profiles"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<ProfileDto>()
                    .firstOrNull()
            }.getOrNull()
            val resolvedName = profile?.displayName ?: currentUserDisplayName()
            prefs.setUserProfile(displayName = resolvedName, role = first.role)
            return first.householdId
        }

        return prefs.householdId.first()
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
            val batchResult = runCatching {
                supabase.postgrest["shopping_items"].upsert(pendingItems.map { it.toDto() })
            }
            if (batchResult.isSuccess) {
                dao.upsertShoppingItems(pendingItems.map { it.copy(syncStatus = SyncStatus.SYNCED) })
            } else {
                pendingItems.forEach { item ->
                    val singleResult = runCatching {
                        supabase.postgrest["shopping_items"].upsert(item.toDto())
                    }
                    if (singleResult.isSuccess) {
                        dao.upsertShoppingItem(item.copy(syncStatus = SyncStatus.SYNCED))
                    } else if (item.deletedAt == null) {
                        // Offline duplicate conflict: merge onto existing active remote row
                        val remoteListItems = runCatching {
                            supabase.postgrest["shopping_items"]
                                .select { filter { eq("list_id", item.listId) } }
                                .decodeList<ShoppingItemDto>()
                        }.getOrNull().orEmpty()
                        val match = remoteListItems.firstOrNull {
                            it.deletedAt == null && normalizedKey(it.name) == normalizedKey(item.name)
                        }
                        if (match != null) {
                            val merged = match.copy(
                                quantity = item.quantity ?: match.quantity,
                                note = item.note ?: match.note,
                                isChecked = false,
                                updatedBy = userId,
                                updatedAt = Instant.now().toString()
                            )
                            runCatching {
                                supabase.postgrest["shopping_items"].upsert(merged)
                            }
                            dao.hardDeleteShoppingItem(item.id)
                        }
                    }
                }
            }
        }

        val pendingRegulars = dao.getPendingRegularItems()
        if (pendingRegulars.isNotEmpty()) {
            val batchResult = runCatching {
                supabase.postgrest["regular_items"].upsert(pendingRegulars.map { it.toDto() })
            }
            if (batchResult.isSuccess) {
                dao.upsertRegularItems(pendingRegulars.map { it.copy(syncStatus = SyncStatus.SYNCED) })
            } else {
                pendingRegulars.forEach { reg ->
                    val singleResult = runCatching {
                        supabase.postgrest["regular_items"].upsert(reg.toDto())
                    }
                    if (singleResult.isSuccess) {
                        dao.upsertRegularItem(reg.copy(syncStatus = SyncStatus.SYNCED))
                    } else if (reg.deletedAt == null) {
                        // Redundant offline regular; drop local duplicate so pull adopts canonical remote row
                        dao.hardDeleteRegularItem(reg.id)
                    }
                }
            }
        }

        val pendingHistory = dao.getPendingItemHistory()
        if (pendingHistory.isNotEmpty()) {
            supabase.postgrest["item_history"].upsert(pendingHistory.map { it.toDto() }) {
                onConflict = "user_id,normalized_name"
            }
            dao.upsertItemHistory(pendingHistory.map { it.copy(syncStatus = SyncStatus.SYNCED) })
        }

        val pendingTaskLists = organizerDao.getPendingTaskLists()
        if (pendingTaskLists.isNotEmpty()) {
            supabase.postgrest["task_lists"].upsert(pendingTaskLists.map { it.toDto() })
            organizerDao.upsertTaskLists(pendingTaskLists.map { it.copy(syncStatus = SyncStatus.SYNCED) })
        }

        val pendingTasks = organizerDao.getPendingTasks()
        if (pendingTasks.isNotEmpty()) {
            val batchResult = runCatching {
                supabase.postgrest["tasks"].upsert(pendingTasks.map { it.toDto() })
            }
            if (batchResult.isSuccess) {
                organizerDao.upsertTasks(pendingTasks.map { it.copy(syncStatus = SyncStatus.SYNCED) })
            } else {
                pendingTasks.forEach { task ->
                    val singleResult = runCatching {
                        supabase.postgrest["tasks"].upsert(task.toDto())
                    }
                    if (singleResult.isSuccess) {
                        organizerDao.upsertTask(task.copy(syncStatus = SyncStatus.SYNCED))
                    } else if (task.recurrenceSourceId != null && task.deletedAt == null) {
                        // Duplicate recurring next occurrence created offline; let canonical remote row win
                        organizerDao.hardDeleteTask(task.id)
                    }
                }
            }
        }

        val pendingSubtasks = organizerDao.getPendingSubtasks()
        if (pendingSubtasks.isNotEmpty()) {
            supabase.postgrest["task_subtasks"].upsert(pendingSubtasks.map { it.toDto() })
            organizerDao.upsertSubtasks(pendingSubtasks.map { it.copy(syncStatus = SyncStatus.SYNCED) })
        }

        val pendingEvents = organizerDao.getPendingCalendarEvents()
        if (pendingEvents.isNotEmpty()) {
            supabase.postgrest["calendar_events"].upsert(pendingEvents.map { it.toDto() })
            organizerDao.upsertCalendarEvents(pendingEvents.map { it.copy(syncStatus = SyncStatus.SYNCED) })
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

            runCatching {
                val remoteProfiles = supabase.postgrest["profiles"]
                    .select()
                    .decodeList<ProfileDto>()
                remoteProfiles.forEach { profile ->
                    if (profile.displayName.isNotBlank()) {
                        profileNameCache[profile.userId] = profile.displayName.trim()
                    }
                }
                organizerDao.upsertProfiles(remoteProfiles.map { it.toEntity() })
            }
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

        val remoteTaskLists = supabase.postgrest["task_lists"]
            .select()
            .decodeList<TaskListDto>()
            .map { it.toEntity() }
        val activeTaskListIds = remoteTaskLists.map { it.id }
        if (activeTaskListIds.isEmpty()) {
            organizerDao.pruneAllSyncedTaskLists()
        } else {
            organizerDao.pruneSyncedTaskListsNotIn(activeTaskListIds)
            organizerDao.upsertTaskLists(remoteTaskLists)
        }

        val remoteTasks = supabase.postgrest["tasks"]
            .select()
            .decodeList<TaskDto>()
            .map { it.toEntity() }
        val activeTaskIds = remoteTasks.map { it.id }
        if (activeTaskIds.isEmpty()) {
            organizerDao.pruneAllSyncedTasks()
        } else {
            organizerDao.pruneSyncedTasksNotIn(activeTaskIds)
            organizerDao.upsertTasks(remoteTasks)
        }

        val remoteSubtasks = supabase.postgrest["task_subtasks"]
            .select()
            .decodeList<TaskSubtaskDto>()
            .map { it.toEntity() }
        val activeSubtaskIds = remoteSubtasks.map { it.id }
        if (activeSubtaskIds.isEmpty()) {
            organizerDao.pruneAllSyncedSubtasks()
        } else {
            organizerDao.pruneSyncedSubtasksNotIn(activeSubtaskIds)
            organizerDao.upsertSubtasks(remoteSubtasks)
        }

        val remoteEvents = supabase.postgrest["calendar_events"]
            .select()
            .decodeList<CalendarEventDto>()
            .map { it.toEntity() }
        val activeEventIds = remoteEvents.map { it.id }
        if (activeEventIds.isEmpty()) {
            organizerDao.pruneAllSyncedCalendarEvents()
        } else {
            organizerDao.pruneSyncedCalendarEventsNotIn(activeEventIds)
            organizerDao.upsertCalendarEvents(remoteEvents)
        }
    }

    suspend fun ensureRealtimeSubscribed(forceReconnect: Boolean = false) {
        if (!networkMonitor.refresh() || currentUserId() == null) return
        if (realtimeChannel != null && !forceReconnect) return

        if (forceReconnect) {
            runCatching {
                realtimeJob?.cancel()
                realtimeChannel?.unsubscribe()
            }
            realtimeChannel = null
        }

        val subscribeResult = runCatching {
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
            val taskListChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "task_lists"
            }
            val taskChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "tasks"
            }
            val subtaskChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "task_subtasks"
            }
            val eventChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "calendar_events"
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
                launch {
                    taskListChanges.collect { action -> handleTaskListRealtime(action) }
                }
                launch {
                    taskChanges.collect { action -> handleTaskRealtime(action) }
                }
                launch {
                    subtaskChanges.collect { action -> handleSubtaskRealtime(action) }
                }
                launch {
                    eventChanges.collect { action -> handleCalendarEventRealtime(action) }
                }
            }

            channel.subscribe()
        }

        if (subscribeResult.isFailure) {
            realtimeJob?.cancel()
            realtimeChannel = null
        }
    }

    private suspend fun handleShoppingItemRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert -> {
                val entity = action.decodeRecord<ShoppingItemDto>().toEntity() ?: return
                val previous = dao.getShoppingItemById(entity.id)
                dao.upsertShoppingItem(entity)
                maybeEmitRealtimeNotification(previous = previous, current = entity, isInsert = true)
            }
            is PostgresAction.Update -> {
                val entity = action.decodeRecord<ShoppingItemDto>().toEntity() ?: return
                val previous = dao.getShoppingItemById(entity.id)
                dao.upsertShoppingItem(entity)
                maybeEmitRealtimeNotification(previous = previous, current = entity, isInsert = false)
            }
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return
                dao.hardDeleteShoppingItem(id)
            }
            else -> Unit
        }
    }

    private suspend fun maybeEmitRealtimeNotification(
        previous: ShoppingItemEntity?,
        current: ShoppingItemEntity,
        isInsert: Boolean
    ) {
        val bundler = notificationBundler ?: return
        if (!prefs.notificationsEnabled.first()) return
        val currentUid = currentUserId() ?: return
        val actorId = current.updatedBy ?: current.addedBy ?: return
        if (actorId == currentUid || current.deletedAt != null) return

        val list = dao.getListById(current.listId) ?: return
        if (!list.isShared || list.deletedAt != null) return

        val notifyOnChecked = prefs.notifyOnChecked.first()
        val kind = when {
            isInsert && !current.isChecked -> ItemChangeKind.ADDED
            !isInsert && !current.isChecked && (previous == null || previous.isChecked) -> ItemChangeKind.ADDED
            current.isChecked && (previous == null || !previous.isChecked) -> {
                if (notifyOnChecked) ItemChangeKind.CHECKED else return
            }
            else -> return
        }

        if (isAppInForeground && activeOpenListId == current.listId) {
            bundler.advanceLastNotifiedAt(current.updatedAt)
            return
        }

        val actorName = resolveActorName(actorId)
        bundler.enqueueRealtimeChange(
            PendingListChange(
                itemId = current.id,
                listId = current.listId,
                listName = list.name,
                itemName = current.name,
                quantity = current.quantity,
                actorName = actorName,
                kind = kind,
                updatedAt = current.updatedAt
            )
        )
    }

    private suspend fun resolveActorName(userId: String): String {
        profileNameCache[userId]?.let { return it }

        val fetched = runCatching {
            supabase.postgrest["profiles"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<ProfileDto>()
                .firstOrNull()
                ?.displayName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()

        if (fetched != null) {
            profileNameCache[userId] = fetched
            return fetched
        }

        return when (userId) {
            "862590df-507f-4c03-9417-f3703b8327c2" -> "Henry"
            "fc1b039d-1b1a-4dce-bbc4-a4f8dd5a94d2" -> "Jolene"
            else -> "Family"
        }
    }

    suspend fun awaitAuthReady(): String? {
        withTimeoutOrNull(8_000L) {
            sessionStatus.first { it !is SessionStatus.Initializing }
        }
        return currentUserId()
    }

    suspend fun pollAndNotifyBackgroundChanges() {
        val bundler = notificationBundler ?: return
        val currentUid = awaitAuthReady() ?: return
        if (!prefs.notificationsEnabled.first()) return

        val sinceIso = prefs.lastNotifiedAt.first()
        if (sinceIso.isNullOrBlank()) {
            syncAll(force = true)
            prefs.setLastNotifiedAt(Instant.now().toString())
            return
        }

        val previousItemsById = dao.observeAllActiveShoppingItems().first().associateBy { it.id }
        syncAll(force = true)
        if (_syncState.value == SyncState.OFFLINE) return

        val sinceInstant = runCatching { Instant.parse(sinceIso) }.getOrNull() ?: Instant.now()
        val sharedLists = dao.observeActiveLists().first()
            .filter { it.isShared && it.deletedAt == null }
            .associateBy { it.id }
        if (sharedLists.isEmpty()) {
            bundler.advanceLastNotifiedAt(Instant.now().toString())
            return
        }

        val notifyOnChecked = prefs.notifyOnChecked.first()
        val allActiveItems = dao.observeAllActiveShoppingItems().first()
        val changes = mutableListOf<PendingListChange>()
        var latestSeenIso: String? = null

        for (item in allActiveItems) {
            val list = sharedLists[item.listId] ?: continue
            val updatedInstant = runCatching { Instant.parse(item.updatedAt) }.getOrNull() ?: continue
            if (!updatedInstant.isAfter(sinceInstant)) continue

            if (latestSeenIso == null || updatedInstant.isAfter(Instant.parse(latestSeenIso))) {
                latestSeenIso = item.updatedAt
            }

            if (isAppInForeground && activeOpenListId == item.listId) continue

            val actorId = item.updatedBy ?: item.addedBy ?: continue
            if (actorId == currentUid) continue

            val prev = previousItemsById[item.id]
            val kind = if (item.isChecked) {
                if (!notifyOnChecked || prev?.isChecked == true) continue
                ItemChangeKind.CHECKED
            } else {
                val createdInstant = runCatching { Instant.parse(item.createdAt) }.getOrNull()
                val isNewlyCreated = createdInstant != null && createdInstant.isAfter(sinceInstant)
                val isNewlyAppeared = prev == null || prev.isChecked
                if (!isNewlyCreated && !isNewlyAppeared) continue
                ItemChangeKind.ADDED
            }

            changes += PendingListChange(
                itemId = item.id,
                listId = item.listId,
                listName = list.name,
                itemName = item.name,
                quantity = item.quantity,
                actorName = resolveActorName(actorId),
                kind = kind,
                updatedAt = item.updatedAt
            )
        }

        if (changes.isNotEmpty()) {
            bundler.postImmediateBatch(changes)
        }
        bundler.advanceLastNotifiedAt(latestSeenIso ?: Instant.now().toString())
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

    private suspend fun handleTaskListRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert,
            is PostgresAction.Update -> {
                val dto = if (action is PostgresAction.Insert) {
                    action.decodeRecord<TaskListDto>()
                } else {
                    (action as PostgresAction.Update).decodeRecord<TaskListDto>()
                }
                if (!dto.isShared && dto.ownerId != currentUserId()) {
                    organizerDao.deleteSubtasksByListId(dto.id)
                    organizerDao.deleteTasksByListId(dto.id)
                    organizerDao.hardDeleteTaskList(dto.id)
                } else {
                    organizerDao.upsertTaskList(dto.toEntity())
                }
            }
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return
                organizerDao.deleteSubtasksByListId(id)
                organizerDao.deleteTasksByListId(id)
                organizerDao.hardDeleteTaskList(id)
            }
            else -> Unit
        }
    }

    private suspend fun handleTaskRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert,
            is PostgresAction.Update -> {
                val dto = if (action is PostgresAction.Insert) {
                    action.decodeRecord<TaskDto>()
                } else {
                    (action as PostgresAction.Update).decodeRecord<TaskDto>()
                }
                if (!dto.isShared && dto.ownerId != currentUserId()) {
                    organizerDao.deleteSubtasksByTaskId(dto.id)
                    organizerDao.hardDeleteTask(dto.id)
                } else {
                    organizerDao.upsertTask(dto.toEntity())
                }
            }
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return
                organizerDao.deleteSubtasksByTaskId(id)
                organizerDao.hardDeleteTask(id)
            }
            else -> Unit
        }
    }

    private suspend fun handleSubtaskRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert,
            is PostgresAction.Update -> {
                val dto = if (action is PostgresAction.Insert) {
                    action.decodeRecord<TaskSubtaskDto>()
                } else {
                    (action as PostgresAction.Update).decodeRecord<TaskSubtaskDto>()
                }
                if (!dto.isShared && dto.ownerId != currentUserId()) {
                    organizerDao.hardDeleteSubtask(dto.id)
                } else {
                    organizerDao.upsertSubtask(dto.toEntity())
                }
            }
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return
                organizerDao.hardDeleteSubtask(id)
            }
            else -> Unit
        }
    }

    private suspend fun handleCalendarEventRealtime(action: PostgresAction) {
        when (action) {
            is PostgresAction.Insert,
            is PostgresAction.Update -> {
                val dto = if (action is PostgresAction.Insert) {
                    action.decodeRecord<CalendarEventDto>()
                } else {
                    (action as PostgresAction.Update).decodeRecord<CalendarEventDto>()
                }
                if (!dto.isShared && dto.ownerId != currentUserId()) {
                    organizerDao.hardDeleteCalendarEvent(dto.id)
                } else {
                    organizerDao.upsertCalendarEvent(dto.toEntity())
                }
            }
            is PostgresAction.Delete -> {
                val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return
                organizerDao.hardDeleteCalendarEvent(id)
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

    private fun normalizeName(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ")

    private fun normalizedKey(raw: String): String =
        normalizeName(raw).lowercase()

    suspend fun addShoppingItem(
        listId: String,
        categoryId: String?,
        name: String,
        quantity: String?,
        note: String?,
        saveAsRegular: Boolean = false
    ): ItemMutationResult {
        val userId = currentUserId()
        val householdId = prefs.householdId.first()
        val now = Instant.now().toString()
        val cleanName = normalizeName(name)
        if (cleanName.isEmpty()) return ItemMutationResult.DuplicateBlocked("Item name cannot be empty")
        val cleanQty = quantity?.trim()?.ifBlank { null }
        val cleanNote = note?.trim()?.ifBlank { null }

        val listName = dao.getListById(listId)?.name ?: "list"
        val existing = dao.getActiveShoppingItemsForList(listId)
            .firstOrNull { normalizedKey(it.name) == normalizedKey(cleanName) }

        if (existing != null) {
            if (existing.isChecked) {
                val restored = existing.copy(
                    quantity = cleanQty ?: existing.quantity,
                    note = cleanNote ?: existing.note,
                    categoryId = categoryId ?: existing.categoryId,
                    isChecked = false,
                    updatedBy = userId,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING_UPSERT
                )
                dao.upsertShoppingItem(restored)
                if (userId != null) {
                    upsertHistoryInternal(
                        userId = userId,
                        name = restored.name,
                        quantity = restored.quantity,
                        categoryId = restored.categoryId,
                        forceUpdateCategory = false
                    )
                }
                scope.launch { syncAll() }
                return ItemMutationResult.RestoredFromCompleted(existing.name, listName)
            }

            val hasNewDetails = (cleanQty != null && cleanQty != existing.quantity) ||
                (cleanNote != null && cleanNote != existing.note)
            if (hasNewDetails) {
                val updated = existing.copy(
                    quantity = cleanQty ?: existing.quantity,
                    note = cleanNote ?: existing.note,
                    updatedBy = userId,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING_UPSERT
                )
                dao.upsertShoppingItem(updated)
                scope.launch { syncAll() }
                return ItemMutationResult.UpdatedExisting(existing.name, listName)
            }
            return ItemMutationResult.AlreadyExists(existing.name, listName)
        }

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
            updatedBy = userId,
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
            val existingRegular = dao.getActiveRegularItems()
                .any { normalizedKey(it.name) == normalizedKey(cleanName) }
            if (!existingRegular) {
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
        }

        scope.launch { syncAll() }
        return ItemMutationResult.Added(cleanName, listName)
    }

    suspend fun updateShoppingItem(
        item: ShoppingItemEntity,
        learnCategory: Boolean = false
    ): ItemMutationResult {
        val cleanName = normalizeName(item.name)
        if (cleanName.isEmpty()) return ItemMutationResult.DuplicateBlocked("Item name cannot be empty")
        val duplicate = dao.getActiveShoppingItemsForList(item.listId)
            .any { it.id != item.id && normalizedKey(it.name) == normalizedKey(cleanName) }
        if (duplicate) {
            return ItemMutationResult.DuplicateBlocked("\"$cleanName\" is already on this list")
        }

        val now = Instant.now().toString()
        val userId = currentUserId()
        val normalizedItem = item.copy(
            name = cleanName,
            quantity = item.quantity?.trim()?.ifBlank { null },
            note = item.note?.trim()?.ifBlank { null },
            updatedBy = userId,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        dao.upsertShoppingItem(normalizedItem)
        if (learnCategory && userId != null) {
            upsertHistoryInternal(
                userId = userId,
                name = normalizedItem.name,
                quantity = normalizedItem.quantity,
                categoryId = normalizedItem.categoryId,
                forceUpdateCategory = true
            )
        }
        scope.launch { syncAll() }
        return ItemMutationResult.Added(cleanName)
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
                updatedBy = currentUserId(),
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
                updatedBy = currentUserId(),
                updatedAt = now,
                deletedAt = now,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun clearCompletedInList(listId: String) {
        val now = Instant.now().toString()
        val userId = currentUserId()
        val items = dao.observeListShoppingItems(listId).first().filter { it.isChecked }
        if (items.isEmpty()) return
        dao.upsertShoppingItems(
            items.map {
                it.copy(
                    updatedBy = userId,
                    updatedAt = now,
                    deletedAt = now,
                    syncStatus = SyncStatus.PENDING_DELETE
                )
            }
        )
        scope.launch { syncAll() }
    }

    suspend fun addRegularToShoppingList(
        regular: RegularItemEntity,
        fallbackListId: String
    ): ItemMutationResult {
        val targetListId = regular.defaultListId ?: fallbackListId
        return addShoppingItem(
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
    ): ItemMutationResult {
        val householdId = prefs.householdId.first()
            ?: return ItemMutationResult.DuplicateBlocked("Household not ready yet")
        val cleanName = normalizeName(name)
        if (cleanName.isEmpty()) return ItemMutationResult.DuplicateBlocked("Item name cannot be empty")

        val duplicate = dao.getActiveRegularItems()
            .any { it.id != existingId && normalizedKey(it.name) == normalizedKey(cleanName) }
        if (duplicate) {
            return ItemMutationResult.DuplicateBlocked("\"$cleanName\" is already in Regulars")
        }

        val cleanQty = defaultQuantity?.trim()?.ifBlank { null }
        val now = Instant.now().toString()
        val entity = RegularItemEntity(
            id = existingId ?: UUID.randomUUID().toString(),
            householdId = householdId,
            defaultListId = defaultListId,
            defaultCategoryId = defaultCategoryId,
            name = cleanName,
            defaultQuantity = cleanQty,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        dao.upsertRegularItem(entity)
        val userId = currentUserId()
        if (userId != null) {
            upsertHistoryInternal(
                userId = userId,
                name = cleanName,
                quantity = cleanQty,
                categoryId = defaultCategoryId,
                forceUpdateCategory = true
            )
        }
        scope.launch { syncAll() }
        return ItemMutationResult.Added(cleanName)
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

    // --- Task Manager (Folders, Tasks & Subtasks) ---

    suspend fun saveTaskList(
        existing: TaskListEntity?,
        name: String,
        iconKey: String,
        colorHex: String,
        isShared: Boolean,
        defaultView: String
    ): TaskListEntity? {
        val userId = currentUserId() ?: return null
        val householdId = prefs.householdId.first() ?: return null
        val cleanName = normalizeName(name)
        if (cleanName.isEmpty()) return null
        val now = Instant.now().toString()

        val list = if (existing == null) {
            val count = organizerDao.observeActiveTaskLists().first().size
            TaskListEntity(
                id = UUID.randomUUID().toString(),
                householdId = householdId,
                ownerId = userId,
                name = cleanName,
                iconKey = iconKey,
                colorHex = colorHex,
                isShared = isShared,
                defaultView = defaultView,
                sortOrder = count,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        } else {
            existing.copy(
                name = cleanName,
                iconKey = iconKey,
                colorHex = colorHex,
                isShared = isShared,
                defaultView = defaultView,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        }

        organizerDao.upsertTaskList(list)
        if (existing != null && existing.isShared != isShared) {
            organizerDao.updateTasksSharedForList(list.id, isShared, now)
            organizerDao.updateSubtasksSharedForList(list.id, isShared, now)
        }
        scope.launch { syncAll() }
        return list
    }

    suspend fun updateTaskListDefaultView(list: TaskListEntity, defaultView: String) {
        if (list.defaultView == defaultView) return
        val now = Instant.now().toString()
        organizerDao.upsertTaskList(
            list.copy(
                defaultView = defaultView,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun softDeleteTaskList(list: TaskListEntity) {
        val now = Instant.now().toString()
        val tasksInList = organizerDao.getActiveTasksForList(list.id)
        if (tasksInList.isNotEmpty()) {
            organizerDao.upsertTasks(
                tasksInList.map {
                    it.copy(
                        updatedBy = currentUserId(),
                        updatedAt = now,
                        deletedAt = now,
                        syncStatus = SyncStatus.PENDING_DELETE
                    )
                }
            )
        }
        organizerDao.upsertTaskList(
            list.copy(
                updatedAt = now,
                deletedAt = now,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun saveTask(
        existing: TaskEntity?,
        taskListId: String,
        title: String,
        notes: String?,
        assignedTo: String?,
        status: String,
        priority: String,
        dueDate: String?,
        dueTime: String?,
        reminderMinutesBefore: Int?,
        recurrenceRule: String?,
        initialSubtasks: List<String> = emptyList()
    ): Pair<TaskEntity?, String?> {
        val userId = currentUserId() ?: return null to null
        val householdId = prefs.householdId.first() ?: return null to null
        val cleanTitle = normalizeName(title)
        if (cleanTitle.isEmpty()) return null to null

        val parentList = organizerDao.getTaskListById(taskListId)
        val inheritedShared = parentList?.isShared ?: existing?.isShared ?: true
        val now = Instant.now().toString()
        val wasDone = existing?.status == "done"
        val isNowDone = status == "done"

        val saved = if (existing == null) {
            TaskEntity(
                id = UUID.randomUUID().toString(),
                householdId = householdId,
                taskListId = taskListId,
                ownerId = userId,
                assignedTo = assignedTo,
                title = cleanTitle,
                notes = notes?.trim()?.ifBlank { null },
                status = status,
                priority = priority,
                isShared = inheritedShared,
                dueDate = dueDate?.ifBlank { null },
                dueTime = if (dueDate.isNullOrBlank()) null else dueTime?.ifBlank { null },
                reminderMinutesBefore = if (dueDate.isNullOrBlank()) null else reminderMinutesBefore,
                recurrenceRule = recurrenceRule?.ifBlank { null },
                completedAt = if (isNowDone) now else null,
                updatedBy = userId,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        } else {
            existing.copy(
                taskListId = taskListId,
                assignedTo = assignedTo,
                title = cleanTitle,
                notes = notes?.trim()?.ifBlank { null },
                status = status,
                priority = priority,
                isShared = inheritedShared,
                dueDate = dueDate?.ifBlank { null },
                dueTime = if (dueDate.isNullOrBlank()) null else dueTime?.ifBlank { null },
                reminderMinutesBefore = if (dueDate.isNullOrBlank()) null else reminderMinutesBefore,
                recurrenceRule = recurrenceRule?.ifBlank { null },
                completedAt = if (isNowDone) (existing.completedAt ?: now) else null,
                updatedBy = userId,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        }

        organizerDao.upsertTask(saved)

        if (existing != null && existing.isShared != inheritedShared) {
            organizerDao.updateSubtasksSharedForTask(saved.id, inheritedShared, now)
        }

        if (existing == null && initialSubtasks.isNotEmpty()) {
            val subtasks = initialSubtasks
                .map { normalizeName(it) }
                .filter { it.isNotEmpty() }
                .mapIndexed { idx, subTitle ->
                    TaskSubtaskEntity(
                        id = UUID.randomUUID().toString(),
                        taskId = saved.id,
                        householdId = householdId,
                        ownerId = userId,
                        isShared = inheritedShared,
                        title = subTitle,
                        isCompleted = false,
                        sortOrder = idx,
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING_UPSERT
                    )
                }
            if (subtasks.isNotEmpty()) {
                organizerDao.upsertSubtasks(subtasks)
            }
        }

        val nextDue = if (isNowDone && !wasDone) {
            spawnNextRecurringTaskIfNeeded(saved)
        } else {
            null
        }

        scope.launch { syncAll() }
        return saved to nextDue
    }

    suspend fun setTaskStatus(task: TaskEntity, newStatus: String): String? {
        if (task.status == newStatus) return null
        val userId = currentUserId() ?: return null
        val now = Instant.now().toString()
        val wasDone = task.status == "done"
        val isNowDone = newStatus == "done"

        val updated = task.copy(
            status = newStatus,
            completedAt = if (isNowDone) now else null,
            updatedBy = userId,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        organizerDao.upsertTask(updated)

        val nextDue = if (isNowDone && !wasDone) {
            spawnNextRecurringTaskIfNeeded(updated)
        } else {
            null
        }

        scope.launch { syncAll() }
        return nextDue
    }

    private suspend fun spawnNextRecurringTaskIfNeeded(completedTask: TaskEntity): String? {
        val rule = completedTask.recurrenceRule?.takeIf { it.isNotBlank() } ?: return null
        if (organizerDao.hasTaskWithRecurrenceSource(completedTask.id)) return null

        val nextDueDate = TaskRecurrence.computeNextDueDate(
            rule = rule,
            currentDueDateIso = completedTask.dueDate
        ) ?: return null

        val userId = currentUserId() ?: completedTask.ownerId
        val now = Instant.now().toString()
        val nextTask = TaskEntity(
            id = UUID.randomUUID().toString(),
            householdId = completedTask.householdId,
            taskListId = completedTask.taskListId,
            ownerId = completedTask.ownerId,
            assignedTo = completedTask.assignedTo,
            title = completedTask.title,
            notes = completedTask.notes,
            status = "todo",
            priority = completedTask.priority,
            isShared = completedTask.isShared,
            dueDate = nextDueDate,
            dueTime = completedTask.dueTime,
            reminderMinutesBefore = completedTask.reminderMinutesBefore,
            recurrenceRule = rule,
            recurrenceSourceId = completedTask.id,
            completedAt = null,
            updatedBy = userId,
            sortOrder = completedTask.sortOrder,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        organizerDao.upsertTask(nextTask)

        val existingSubtasks = organizerDao.getActiveSubtasksForTask(completedTask.id)
        if (existingSubtasks.isNotEmpty()) {
            organizerDao.upsertSubtasks(
                existingSubtasks.mapIndexed { idx, sub ->
                    TaskSubtaskEntity(
                        id = UUID.randomUUID().toString(),
                        taskId = nextTask.id,
                        householdId = completedTask.householdId,
                        ownerId = completedTask.ownerId,
                        isShared = completedTask.isShared,
                        title = sub.title,
                        isCompleted = false,
                        sortOrder = idx,
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING_UPSERT
                    )
                }
            )
        }

        return nextDueDate
    }

    suspend fun softDeleteTask(task: TaskEntity) {
        val now = Instant.now().toString()
        val subtasks = organizerDao.getActiveSubtasksForTask(task.id)
        if (subtasks.isNotEmpty()) {
            organizerDao.upsertSubtasks(
                subtasks.map {
                    it.copy(
                        updatedAt = now,
                        deletedAt = now,
                        syncStatus = SyncStatus.PENDING_DELETE
                    )
                }
            )
        }
        organizerDao.upsertTask(
            task.copy(
                updatedBy = currentUserId(),
                updatedAt = now,
                deletedAt = now,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun addSubtask(task: TaskEntity, title: String) {
        val userId = currentUserId() ?: return
        val cleanTitle = normalizeName(title)
        if (cleanTitle.isEmpty()) return
        val now = Instant.now().toString()
        val existing = organizerDao.getActiveSubtasksForTask(task.id)
        val subtask = TaskSubtaskEntity(
            id = UUID.randomUUID().toString(),
            taskId = task.id,
            householdId = task.householdId,
            ownerId = userId,
            isShared = task.isShared,
            title = cleanTitle,
            isCompleted = false,
            sortOrder = existing.size,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_UPSERT
        )
        organizerDao.upsertSubtask(subtask)
        scope.launch { syncAll() }
    }

    suspend fun toggleSubtask(subtask: TaskSubtaskEntity) {
        val now = Instant.now().toString()
        organizerDao.upsertSubtask(
            subtask.copy(
                isCompleted = !subtask.isCompleted,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT
            )
        )
        scope.launch { syncAll() }
    }

    suspend fun softDeleteSubtask(subtask: TaskSubtaskEntity) {
        val now = Instant.now().toString()
        organizerDao.upsertSubtask(
            subtask.copy(
                updatedAt = now,
                deletedAt = now,
                syncStatus = SyncStatus.PENDING_DELETE
            )
        )
        scope.launch { syncAll() }
    }

    // --- Family Admin & Invites ---

    private fun generateFamilyInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = buildString(6) {
            repeat(6) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
        return "FAM-$suffix"
    }

    suspend fun fetchFamilyOverview(): Result<FamilyOverviewResponse> {
        val token = supabase.auth.currentSessionOrNull()?.accessToken
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return familyApi.fetchFamilyOverview(token)
    }

    suspend fun createFamilyInvite(
        invitedName: String,
        role: String,
        autoListIds: List<String>
    ): Result<FamilyInviteDto> = runCatching {
        val userId = currentUserId() ?: error("Not signed in")
        val householdId = resolveHouseholdId(userId) ?: error("Household not found")
        val code = generateFamilyInviteCode()
        val payload = FamilyInviteInsertDto(
            householdId = householdId,
            code = code,
            invitedName = invitedName.trim(),
            role = role,
            autoListIds = autoListIds,
            createdBy = userId
        )
        supabase.postgrest["family_invites"]
            .insert(payload) { select() }
            .decodeSingle<FamilyInviteDto>()
    }

    suspend fun revokeFamilyInvite(inviteId: String): Result<Unit> = runCatching {
        supabase.postgrest["family_invites"].update({
            set("revoked_at", Instant.now().toString())
        }) {
            filter { eq("id", inviteId) }
        }
    }

    suspend fun createMemberDirectly(
        displayName: String,
        email: String,
        password: String,
        role: String,
        autoListIds: List<String>
    ): Result<EdgeActionResponse> {
        val token = supabase.auth.currentSessionOrNull()?.accessToken
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return familyApi.createMemberDirectly(
            accessToken = token,
            displayName = displayName,
            email = email,
            password = password,
            role = role,
            autoListIds = autoListIds
        )
    }

    suspend fun removeFamilyMember(targetUserId: String): Result<Unit> {
        val token = supabase.auth.currentSessionOrNull()?.accessToken
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return familyApi.removeMember(token, targetUserId)
    }

    suspend fun updateFamilyMemberRole(targetUserId: String, role: String): Result<Unit> {
        val token = supabase.auth.currentSessionOrNull()?.accessToken
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return familyApi.updateMemberRole(token, targetUserId, role)
    }

    suspend fun previewFamilyInvite(code: String): Result<InvitePreviewResponse> =
        familyApi.previewInvite(code)

    suspend fun acceptFamilyInvite(
        code: String,
        email: String,
        password: String,
        displayName: String
    ): Result<Unit> {
        val res = familyApi.acceptInvite(code, email, password, displayName)
        return res.fold(
            onSuccess = { signIn(email, password) },
            onFailure = { Result.failure(it) }
        )
    }
}
