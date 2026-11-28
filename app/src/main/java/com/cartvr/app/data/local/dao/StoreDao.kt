package com.cartvr.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cartvr.app.data.local.entity.LoyaltyCardEntity
import com.cartvr.app.data.local.entity.ProductBarcodeEntity
import com.cartvr.app.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {

    @Query("SELECT * FROM product_barcodes WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeAllActiveProductBarcodes(): Flow<List<ProductBarcodeEntity>>

    @Query(
        """
        SELECT * FROM product_barcodes
        WHERE householdId = :householdId AND barcode = :barcode AND deletedAt IS NULL
        LIMIT 1
        """
    )
    suspend fun findProductBarcode(householdId: String, barcode: String): ProductBarcodeEntity?

    @Query(
        """
        SELECT * FROM product_barcodes
        WHERE householdId = :householdId AND barcode = :barcode
        LIMIT 1
        """
    )
    suspend fun findProductBarcodeAnyState(householdId: String, barcode: String): ProductBarcodeEntity?

    @Query("SELECT * FROM product_barcodes WHERE syncStatus != :synced")
    suspend fun getPendingProductBarcodes(synced: String = SyncStatus.SYNCED): List<ProductBarcodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductBarcode(entity: ProductBarcodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductBarcodes(entities: List<ProductBarcodeEntity>)

    @Query("DELETE FROM product_barcodes WHERE id = :id")
    suspend fun hardDeleteProductBarcode(id: String)

    @Query("DELETE FROM product_barcodes WHERE syncStatus = :synced")
    suspend fun pruneAllSyncedProductBarcodes(synced: String = SyncStatus.SYNCED)

    @Query("DELETE FROM product_barcodes WHERE syncStatus = :synced AND id NOT IN (:activeIds)")
    suspend fun pruneSyncedProductBarcodesNotIn(activeIds: List<String>, synced: String = SyncStatus.SYNCED)

    @Query("SELECT * FROM loyalty_cards WHERE deletedAt IS NULL ORDER BY storeName ASC, cardName ASC")
    fun observeAllActiveLoyaltyCards(): Flow<List<LoyaltyCardEntity>>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id LIMIT 1")
    suspend fun getLoyaltyCardById(id: String): LoyaltyCardEntity?

    @Query("SELECT * FROM loyalty_cards WHERE syncStatus != :synced")
    suspend fun getPendingLoyaltyCards(synced: String = SyncStatus.SYNCED): List<LoyaltyCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoyaltyCard(entity: LoyaltyCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoyaltyCards(entities: List<LoyaltyCardEntity>)

    @Query("DELETE FROM loyalty_cards WHERE id = :id")
    suspend fun hardDeleteLoyaltyCard(id: String)

    @Query("DELETE FROM loyalty_cards WHERE syncStatus = :synced")
    suspend fun pruneAllSyncedLoyaltyCards(synced: String = SyncStatus.SYNCED)

    @Query("DELETE FROM loyalty_cards WHERE syncStatus = :synced AND id NOT IN (:activeIds)")
    suspend fun pruneSyncedLoyaltyCardsNotIn(activeIds: List<String>, synced: String = SyncStatus.SYNCED)

    @Query("DELETE FROM product_barcodes")
    suspend fun clearProductBarcodes()

    @Query("DELETE FROM loyalty_cards")
    suspend fun clearLoyaltyCards()
}
