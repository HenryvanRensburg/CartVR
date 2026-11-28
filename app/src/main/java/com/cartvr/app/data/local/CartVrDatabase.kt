package com.cartvr.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cartvr.app.data.local.dao.CartVrDao
import com.cartvr.app.data.local.dao.OrganizerDao
import com.cartvr.app.data.local.entity.CalendarEventEntity
import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.HouseholdProfileEntity
import com.cartvr.app.data.local.entity.ItemHistoryEntity
import com.cartvr.app.data.local.entity.ListMemberEntity
import com.cartvr.app.data.local.entity.RegularItemEntity
import com.cartvr.app.data.local.entity.ShoppingItemEntity
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.local.entity.TaskEntity
import com.cartvr.app.data.local.entity.TaskListEntity
import com.cartvr.app.data.local.entity.TaskSubtaskEntity

@Database(
    entities = [
        ShoppingListEntity::class,
        ListMemberEntity::class,
        CategoryEntity::class,
        ShoppingItemEntity::class,
        RegularItemEntity::class,
        ItemHistoryEntity::class,
        HouseholdProfileEntity::class,
        TaskListEntity::class,
        TaskEntity::class,
        TaskSubtaskEntity::class,
        CalendarEventEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CartVrDatabase : RoomDatabase() {
    abstract fun cartVrDao(): CartVrDao
    abstract fun organizerDao(): OrganizerDao

    companion object {
        @Volatile
        private var INSTANCE: CartVrDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN updatedBy TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `household_profiles` (
                        `userId` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `accentColor` TEXT NOT NULL,
                        PRIMARY KEY(`userId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_lists` (
                        `id` TEXT NOT NULL,
                        `householdId` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `isShared` INTEGER NOT NULL,
                        `defaultView` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        `deletedAt` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_lists_householdId` ON `task_lists` (`householdId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` TEXT NOT NULL,
                        `householdId` TEXT NOT NULL,
                        `taskListId` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `assignedTo` TEXT,
                        `title` TEXT NOT NULL,
                        `notes` TEXT,
                        `status` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `isShared` INTEGER NOT NULL,
                        `dueDate` TEXT,
                        `dueTime` TEXT,
                        `reminderMinutesBefore` INTEGER,
                        `recurrenceRule` TEXT,
                        `recurrenceSourceId` TEXT,
                        `completedAt` TEXT,
                        `updatedBy` TEXT,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        `deletedAt` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_taskListId` ON `tasks` (`taskListId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDate` ON `tasks` (`dueDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_assignedTo` ON `tasks` (`assignedTo`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_subtasks` (
                        `id` TEXT NOT NULL,
                        `taskId` TEXT NOT NULL,
                        `householdId` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `isShared` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        `deletedAt` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_subtasks_taskId` ON `task_subtasks` (`taskId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calendar_events` (
                        `id` TEXT NOT NULL,
                        `householdId` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `location` TEXT,
                        `notes` TEXT,
                        `startAt` TEXT NOT NULL,
                        `endAt` TEXT NOT NULL,
                        `isAllDay` INTEGER NOT NULL,
                        `isShared` INTEGER NOT NULL,
                        `assignedMemberIdsCsv` TEXT NOT NULL,
                        `colorHex` TEXT,
                        `recurrenceRule` TEXT,
                        `reminderMinutesBefore` INTEGER,
                        `externalProvider` TEXT,
                        `externalCalendarId` TEXT,
                        `externalEventId` TEXT,
                        `externalEtag` TEXT,
                        `updatedBy` TEXT,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        `deletedAt` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_householdId` ON `calendar_events` (`householdId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_startAt` ON `calendar_events` (`startAt`)")
            }
        }

        fun getInstance(context: Context): CartVrDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CartVrDatabase::class.java,
                    "cartvr.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
