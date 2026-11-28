package com.cartvr.app.data.remote.dto

import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.ItemHistoryEntity
import com.cartvr.app.data.local.entity.ListMemberEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
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
data class ShoppingListDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    @SerialName("icon_key") val iconKey: String = "cart",
    @SerialName("color_hex") val colorHex: String = "#0D9488",
    @SerialName("is_shared") val isShared: Boolean = false,
    @SerialName("share_code") val shareCode: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class ListMemberDto(
    @SerialName("list_id") val listId: String,
    @SerialName("user_id") val userId: String,
    val role: String = "member",
    @SerialName("joined_at") val joinedAt: String
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
    @SerialName("list_id") val listId: String? = null,
    @SerialName("household_id") val householdId: String? = null,
    @SerialName("store_id") val storeId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val name: String,
    val quantity: String? = null,
    val note: String? = null,
    @SerialName("is_checked") val isChecked: Boolean = false,
    @SerialName("estimated_price") val estimatedPrice: Double? = null,
    @SerialName("added_by") val addedBy: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class RegularItemDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("default_list_id") val defaultListId: String? = null,
    @SerialName("default_category_id") val defaultCategoryId: String? = null,
    val name: String,
    @SerialName("default_quantity") val defaultQuantity: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class ItemHistoryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("normalized_name") val normalizedName: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("default_quantity") val defaultQuantity: String? = null,
    @SerialName("default_category_id") val defaultCategoryId: String? = null,
    @SerialName("use_count") val useCount: Int = 1,
    @SerialName("last_used_at") val lastUsedAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val email: String,
    @SerialName("accent_color") val accentColor: String = "#0D9488"
)

@Serializable
data class FamilyMemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val email: String,
    val role: String,
    @SerialName("joined_at") val joinedAt: String
)

@Serializable
data class FamilyInviteDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    val code: String,
    @SerialName("invited_name") val invitedName: String,
    val role: String,
    @SerialName("auto_list_ids") val autoListIds: List<String> = emptyList(),
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class FamilyInviteInsertDto(
    @SerialName("household_id") val householdId: String,
    val code: String,
    @SerialName("invited_name") val invitedName: String,
    val role: String,
    @SerialName("auto_list_ids") val autoListIds: List<String>,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class InvitePreviewResponse(
    val valid: Boolean = false,
    @SerialName("invited_name") val invitedName: String? = null,
    @SerialName("household_name") val householdName: String? = null,
    val role: String? = null,
    val error: String? = null
)

@Serializable
data class FamilyOverviewResponse(
    val members: List<FamilyMemberDto> = emptyList(),
    val invites: List<FamilyInviteDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class EdgeActionResponse(
    val success: Boolean = false,
    val error: String? = null,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("user_id") val userId: String? = null
)

fun ShoppingListDto.toEntity() = ShoppingListEntity(
    id = id,
    ownerId = ownerId,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    isShared = isShared,
    shareCode = shareCode,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun ShoppingListEntity.toDto() = ShoppingListDto(
    id = id,
    ownerId = ownerId,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    isShared = isShared,
    shareCode = shareCode,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun ListMemberDto.toEntity() = ListMemberEntity(
    listId = listId,
    userId = userId,
    role = role,
    joinedAt = joinedAt
)

fun ListMemberEntity.toDto() = ListMemberDto(
    listId = listId,
    userId = userId,
    role = role,
    joinedAt = joinedAt
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

fun ShoppingItemDto.toEntity(): ShoppingItemEntity? {
    val resolvedListId = listId ?: return null
    return ShoppingItemEntity(
        id = id,
        listId = resolvedListId,
        householdId = householdId,
        categoryId = categoryId,
        name = name,
        quantity = quantity,
        note = note,
        isChecked = isChecked,
        estimatedPrice = estimatedPrice,
        addedBy = addedBy,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncStatus = SyncStatus.SYNCED
    )
}

fun ShoppingItemEntity.toDto() = ShoppingItemDto(
    id = id,
    listId = listId,
    householdId = householdId,
    storeId = null,
    categoryId = categoryId,
    name = name,
    quantity = quantity,
    note = note,
    isChecked = isChecked,
    estimatedPrice = estimatedPrice,
    addedBy = addedBy,
    updatedBy = updatedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun RegularItemDto.toEntity() = RegularItemEntity(
    id = id,
    householdId = householdId,
    defaultListId = defaultListId,
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
    defaultListId = defaultListId,
    defaultCategoryId = defaultCategoryId,
    name = name,
    defaultQuantity = defaultQuantity,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun ItemHistoryDto.toEntity() = ItemHistoryEntity(
    id = id,
    userId = userId,
    normalizedName = normalizedName,
    displayName = displayName,
    defaultQuantity = defaultQuantity,
    defaultCategoryId = defaultCategoryId,
    useCount = useCount,
    lastUsedAt = lastUsedAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun ItemHistoryEntity.toDto() = ItemHistoryDto(
    id = id,
    userId = userId,
    normalizedName = normalizedName,
    displayName = displayName,
    defaultQuantity = defaultQuantity,
    defaultCategoryId = defaultCategoryId,
    useCount = useCount,
    lastUsedAt = lastUsedAt,
    updatedAt = updatedAt
)
