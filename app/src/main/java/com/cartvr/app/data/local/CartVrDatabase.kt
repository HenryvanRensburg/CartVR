package com.cartvr.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cartvr.app.data.local.dao.CartVrDao
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.ItemHistoryEntity
import com.cartvr.app.data.local.entity.ListMemberEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity

@Database(
    entities = [
        ShoppingListEntity::class,
        ListMemberEntity::class,
        CategoryEntity::class,
        ShoppingItemEntity::class,
        RegularItemEntity::class,
        ItemHistoryEntity::class
    ],
    version = 2,
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
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
