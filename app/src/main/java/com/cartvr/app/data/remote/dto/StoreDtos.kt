package com.cartvr.app.data.remote.dto

import com.cartvr.app.data.local.entity.LoyaltyCardEntity
import com.cartvr.app.data.local.entity.ProductBarcodeEntity
import com.cartvr.app.data.local.entity.SyncStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductBarcodeDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    val barcode: String,
    val name: String,
    @SerialName("default_quantity") val defaultQuantity: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    val brand: String? = null,
    val source: String = "manual",
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class LoyaltyCardDto(
    val id: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("card_name") val cardName: String,
    @SerialName("card_number") val cardNumber: String,
    @SerialName("barcode_format") val barcodeFormat: String = "CODE_128",
    @SerialName("accent_color") val accentColor: String = "#2563EB",
    val notes: String? = null,
    @SerialName("linked_list_id") val linkedListId: String? = null,
    @SerialName("is_shared") val isShared: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

fun ProductBarcodeDto.toEntity() = ProductBarcodeEntity(
    id = id,
    householdId = householdId,
    barcode = barcode,
    name = name,
    defaultQuantity = defaultQuantity,
    categoryName = categoryName,
    brand = brand,
    source = source,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun ProductBarcodeEntity.toDto() = ProductBarcodeDto(
    id = id,
    householdId = householdId,
    barcode = barcode,
    name = name,
    defaultQuantity = defaultQuantity,
    categoryName = categoryName,
    brand = brand,
    source = source,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun LoyaltyCardDto.toEntity() = LoyaltyCardEntity(
    id = id,
    householdId = householdId,
    ownerId = ownerId,
    storeName = storeName,
    cardName = cardName,
    cardNumber = cardNumber,
    barcodeFormat = barcodeFormat,
    accentColor = accentColor,
    notes = notes,
    linkedListId = linkedListId,
    isShared = isShared,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun LoyaltyCardEntity.toDto() = LoyaltyCardDto(
    id = id,
    householdId = householdId,
    ownerId = ownerId,
    storeName = storeName,
    cardName = cardName,
    cardNumber = cardNumber,
    barcodeFormat = barcodeFormat,
    accentColor = accentColor,
    notes = notes,
    linkedListId = linkedListId,
    isShared = isShared,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
