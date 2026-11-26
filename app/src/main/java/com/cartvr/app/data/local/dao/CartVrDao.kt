package com.cartvr.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.ItemHistoryEntity
import com.cartvr.app.data.local.entity.ListMemberEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CartVrDao {

    // --- Shopping Lists ---
    @Query("SELECT * FROM shopping_lists WHERE deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun observeActiveLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getListById(id: String): ShoppingListEntity?

    @Query("SELECT * FROM shopping_lists WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingLists(): List<ShoppingListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLists(lists: List<ShoppingListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun hardDeleteList(id: String)

    @Query("DELETE FROM shopping_lists WHERE syncStatus = '${SyncStatus.SYNCED}' AND id NOT IN (:activeIds)")
    suspend fun pruneSyncedListsNotIn(activeIds: List<String>)

    @Query("DELETE FROM shopping_lists WHERE syncStatus = '${SyncStatus.SYNCED}'")
    suspend fun pruneAllSyncedLists()

    // --- List Members ---
    @Query("SELECT * FROM list_members")
    fun observeAllListMembers(): Flow<List<ListMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListMembers(members: List<ListMemberEntity>)

    @Query("DELETE FROM list_members WHERE listId = :listId AND userId != :ownerId")
    suspend fun removeNonOwnerMembers(listId: String, ownerId: String)

    @Query("DELETE FROM list_members WHERE listId = :listId AND userId = :userId")
    suspend fun removeMember(listId: String, userId: String)

    @Query("DELETE FROM list_members")
    suspend fun clearListMembers()

    @Transaction
    suspend fun replaceListMembers(members: List<ListMemberEntity>) {
        clearListMembers()
        if (members.isNotEmpty()) {
            upsertListMembers(members)
        }
    }

    // --- Categories ---
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY sortOrder ASC, name ASC")
    fun observeActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    // --- Shopping Items ---
    @Query("SELECT * FROM shopping_items WHERE deletedAt IS NULL ORDER BY isChecked ASC, createdAt DESC")
    fun observeAllActiveShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND deletedAt IS NULL ORDER BY isChecked ASC, createdAt DESC")
    fun observeListShoppingItems(listId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingShoppingItems(): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoppingItems(items: List<ShoppingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun hardDeleteShoppingItem(id: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteItemsByListId(listId: String)

    @Query("DELETE FROM shopping_items WHERE syncStatus = '${SyncStatus.SYNCED}' AND listId NOT IN (SELECT id FROM shopping_lists)")
    suspend fun pruneOrphanedSyncedItems()

    // --- Regular Items ---
    @Query("SELECT * FROM regular_items WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeActiveRegularItems(): Flow<List<RegularItemEntity>>

    @Query("SELECT * FROM regular_items WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingRegularItems(): List<RegularItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegularItems(items: List<RegularItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegularItem(item: RegularItemEntity)

    @Query("DELETE FROM regular_items WHERE id = :id")
    suspend fun hardDeleteRegularItem(id: String)

    // --- Item History (Smart Suggestions & Aisle Learning) ---
    @Query("SELECT * FROM item_history ORDER BY useCount DESC, lastUsedAt DESC")
    fun observeItemHistory(): Flow<List<ItemHistoryEntity>>

    @Query("SELECT * FROM item_history WHERE userId = :userId AND normalizedName = :normalizedName LIMIT 1")
    suspend fun getHistoryByNormalizedName(userId: String, normalizedName: String): ItemHistoryEntity?

    @Query("SELECT * FROM item_history WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingItemHistory(): List<ItemHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItemHistory(items: List<ItemHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItemHistoryEntry(item: ItemHistoryEntity)

    // --- Cleanup ---
    @Query("DELETE FROM shopping_lists")
    suspend fun clearShoppingLists()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM shopping_items")
    suspend fun clearShoppingItems()

    @Query("DELETE FROM regular_items")
    suspend fun clearRegularItems()

    @Query("DELETE FROM item_history")
    suspend fun clearItemHistory()
}
