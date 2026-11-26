package com.cartvr.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.StoreEntity
import com.cartvr.app.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CartVrDao {

    // --- Stores ---
    @Query("SELECT * FROM stores WHERE deletedAt IS NULL ORDER BY sortOrder ASC, name ASC")
    fun observeActiveStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE id = :id LIMIT 1")
    suspend fun getStoreById(id: String): StoreEntity?

    @Query("SELECT * FROM stores WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingStores(): List<StoreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStores(stores: List<StoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStore(store: StoreEntity)

    // --- Categories ---
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY sortOrder ASC, name ASC")
    fun observeActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    // --- Shopping Items ---
    @Query("SELECT * FROM shopping_items WHERE deletedAt IS NULL ORDER BY isChecked ASC, createdAt DESC")
    fun observeAllActiveShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE storeId = :storeId AND deletedAt IS NULL ORDER BY isChecked ASC, createdAt DESC")
    fun observeStoreShoppingItems(storeId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE id = :id LIMIT 1")
    suspend fun getShoppingItemById(id: String): ShoppingItemEntity?

    @Query("SELECT * FROM shopping_items WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingShoppingItems(): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoppingItems(items: List<ShoppingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun hardDeleteShoppingItem(id: String)

    // --- Regular Items ---
    @Query("SELECT * FROM regular_items WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeActiveRegularItems(): Flow<List<RegularItemEntity>>

    @Query("SELECT * FROM regular_items WHERE id = :id LIMIT 1")
    suspend fun getRegularItemById(id: String): RegularItemEntity?

    @Query("SELECT * FROM regular_items WHERE syncStatus != '${SyncStatus.SYNCED}'")
    suspend fun getPendingRegularItems(): List<RegularItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegularItems(items: List<RegularItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegularItem(item: RegularItemEntity)

    @Query("DELETE FROM regular_items WHERE id = :id")
    suspend fun hardDeleteRegularItem(id: String)

    // --- Cleanup ---
    @Query("DELETE FROM stores")
    suspend fun clearStores()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM shopping_items")
    suspend fun clearShoppingItems()

    @Query("DELETE FROM regular_items")
    suspend fun clearRegularItems()
}
