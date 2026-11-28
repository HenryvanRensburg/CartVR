package com.cartvr.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_barcodes",
    indices = [Index(value = ["householdId", "barcode"], unique = true)]
)
data class ProductBarcodeEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val barcode: String,
    val name: String,
    val defaultQuantity: String? = null,
    val categoryName: String? = null,
    val brand: String? = null,
    val source: String = "manual",
    val createdBy: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(
    tableName = "loyalty_cards",
    indices = [
        Index("householdId"),
        Index("linkedListId")
    ]
)
data class LoyaltyCardEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val ownerId: String,
    val storeName: String,
    val cardName: String,
    val cardNumber: String,
    val barcodeFormat: String = "CODE_128",
    val accentColor: String = "#2563EB",
    val notes: String? = null,
    val linkedListId: String? = null,
    val isShared: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncStatus: String = SyncStatus.SYNCED
)
