package com.cartvr.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING_UPSERT = "PENDING_UPSERT"
    const val PENDING_DELETE = "PENDING_DELETE"
}

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val sortOrder: Int,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val sortOrder: Int,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(
    tableName = "shopping_items",
    indices = [
        Index("storeId"),
        Index("categoryId"),
        Index("householdId")
    ]
)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val storeId: String,
    val categoryId: String? = null,
    val name: String,
    val quantity: String? = null,
    val note: String? = null,
    val isChecked: Boolean = false,
    val estimatedPrice: Double? = null,
    val addedBy: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(
    tableName = "regular_items",
    indices = [Index("householdId")]
)
data class RegularItemEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val defaultStoreId: String? = null,
    val defaultCategoryId: String? = null,
    val name: String,
    val defaultQuantity: String? = null,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)
