package com.cartvr.app.data.remote.dto

import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.StoreEntity
import com.cartvr.app.data.local.entity.SyncStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HouseholdDto(
    val id: String,
    val name: String
)

@Serializable
data class HouseholdMemberDto(
    @SerialName("household_id") val householdId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    val households: HouseholdDto? = null
)

@Serializable
data class StoreDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class CategoryDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class ShoppingItemDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("category_id") val categoryId: String? = null,
    val name: String,
    val quantity: String? = null,
    val note: String? = null,
    @SerialName("is_checked") val isChecked: Boolean = false,
    @SerialName("estimated_price") val estimatedPrice: Double? = null,
    @SerialName("added_by") val addedBy: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class RegularItemDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("default_store_id") val defaultStoreId: String? = null,
    @SerialName("default_category_id") val defaultCategoryId: String? = null,
    val name: String,
    @SerialName("default_quantity") val defaultQuantity: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

fun StoreDto.toEntity() = StoreEntity(
    id = id,
    householdId = householdId,
    name = name,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun StoreEntity.toDto() = StoreDto(
    id = id,
    householdId = householdId,
    name = name,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun CategoryDto.toEntity() = CategoryEntity(
    id = id,
    householdId = householdId,
    name = name,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun CategoryEntity.toDto() = CategoryDto(
    id = id,
    householdId = householdId,
    name = name,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun ShoppingItemDto.toEntity() = ShoppingItemEntity(
    id = id,
    householdId = householdId,
    storeId = storeId,
    categoryId = categoryId,
    name = name,
    quantity = quantity,
    note = note,
    isChecked = isChecked,
    estimatedPrice = estimatedPrice,
    addedBy = addedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun ShoppingItemEntity.toDto() = ShoppingItemDto(
    id = id,
    householdId = householdId,
    storeId = storeId,
    categoryId = categoryId,
    name = name,
    quantity = quantity,
    note = note,
    isChecked = isChecked,
    estimatedPrice = estimatedPrice,
    addedBy = addedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun RegularItemDto.toEntity() = RegularItemEntity(
    id = id,
    householdId = householdId,
    defaultStoreId = defaultStoreId,
    defaultCategoryId = defaultCategoryId,
    name = name,
    defaultQuantity = defaultQuantity,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun RegularItemEntity.toDto() = RegularItemDto(
    id = id,
    householdId = householdId,
    defaultStoreId = defaultStoreId,
    defaultCategoryId = defaultCategoryId,
    name = name,
    defaultQuantity = defaultQuantity,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
