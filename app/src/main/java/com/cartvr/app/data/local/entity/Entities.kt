package com.cartvr.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING_UPSERT = "PENDING_UPSERT"
    const val PENDING_DELETE = "PENDING_DELETE"
}

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val iconKey: String = "cart",
    val colorHex: String = "#0D9488",
    val isShared: Boolean = false,
    val shareCode: String? = null,
    val sortOrder: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(
    tableName = "list_members",
    primaryKeys = ["listId", "userId"],
    indices = [Index("userId")]
)
data class ListMemberEntity(
    val listId: String,
    val userId: String,
    val role: String = "member",
    val joinedAt: String
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
        Index("listId"),
        Index("categoryId")
    ]
)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val householdId: String? = null,
    val categoryId: String? = null,
    val name: String,
    val quantity: String? = null,
    val note: String? = null,
    val isChecked: Boolean = false,
    val estimatedPrice: Double? = null,
    val addedBy: String? = null,
    val updatedBy: String? = null,
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
    val defaultListId: String? = null,
    val defaultCategoryId: String? = null,
    val name: String,
    val defaultQuantity: String? = null,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(
    tableName = "item_history",
    indices = [Index(value = ["userId", "normalizedName"], unique = true)]
)
data class ItemHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val normalizedName: String,
    val displayName: String,
    val defaultQuantity: String? = null,
    val defaultCategoryId: String? = null,
    val useCount: Int = 1,
    val lastUsedAt: String,
    val updatedAt: String,
    val syncStatus: String = SyncStatus.SYNCED
)
