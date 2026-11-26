package com.cartvr.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cartvr.app.data.local.dao.CartVrDao
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.StoreEntity

@Database(
    entities = [
        StoreEntity::class,
        CategoryEntity::class,
        ShoppingItemEntity::class,
        RegularItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CartVrDatabase : RoomDatabase() {
    abstract fun cartVrDao(): CartVrDao

    companion object {
        @Volatile
        private var INSTANCE: CartVrDatabase? = null

        fun getInstance(context: Context): CartVrDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CartVrDatabase::class.java,
                    "cartvr.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
