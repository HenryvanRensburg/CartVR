package com.cartvr.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.cartvr.app.data.local.dao.CartVrDao;
import com.cartvr.app.data.local.dao.CartVrDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CartVrDatabase_Impl extends CartVrDatabase {
  private volatile CartVrDao _cartVrDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `stores` (`id` TEXT NOT NULL, `householdId` TEXT NOT NULL, `name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `updatedAt` TEXT NOT NULL, `deletedAt` TEXT, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `householdId` TEXT NOT NULL, `name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `updatedAt` TEXT NOT NULL, `deletedAt` TEXT, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `shopping_items` (`id` TEXT NOT NULL, `householdId` TEXT NOT NULL, `storeId` TEXT NOT NULL, `categoryId` TEXT, `name` TEXT NOT NULL, `quantity` TEXT, `note` TEXT, `isChecked` INTEGER NOT NULL, `estimatedPrice` REAL, `addedBy` TEXT, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `deletedAt` TEXT, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_storeId` ON `shopping_items` (`storeId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_categoryId` ON `shopping_items` (`categoryId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_householdId` ON `shopping_items` (`householdId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `regular_items` (`id` TEXT NOT NULL, `householdId` TEXT NOT NULL, `defaultStoreId` TEXT, `defaultCategoryId` TEXT, `name` TEXT NOT NULL, `defaultQuantity` TEXT, `updatedAt` TEXT NOT NULL, `deletedAt` TEXT, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_regular_items_householdId` ON `regular_items` (`householdId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '26216ad075559be5b57a1480a2faf3ce')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `stores`");
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `shopping_items`");
        db.execSQL("DROP TABLE IF EXISTS `regular_items`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsStores = new HashMap<String, TableInfo.Column>(7);
        _columnsStores.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStores.put("householdId", new TableInfo.Column("householdId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStores.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStores.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStores.put("updatedAt", new TableInfo.Column("updatedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStores.put("deletedAt", new TableInfo.Column("deletedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStores.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStores = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStores = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStores = new TableInfo("stores", _columnsStores, _foreignKeysStores, _indicesStores);
        final TableInfo _existingStores = TableInfo.read(db, "stores");
        if (!_infoStores.equals(_existingStores)) {
          return new RoomOpenHelper.ValidationResult(false, "stores(com.cartvr.app.data.local.entity.StoreEntity).\n"
                  + " Expected:\n" + _infoStores + "\n"
                  + " Found:\n" + _existingStores);
        }
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(7);
        _columnsCategories.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("householdId", new TableInfo.Column("householdId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("updatedAt", new TableInfo.Column("updatedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("deletedAt", new TableInfo.Column("deletedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(com.cartvr.app.data.local.entity.CategoryEntity).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsShoppingItems = new HashMap<String, TableInfo.Column>(14);
        _columnsShoppingItems.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("householdId", new TableInfo.Column("householdId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("storeId", new TableInfo.Column("storeId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("categoryId", new TableInfo.Column("categoryId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("quantity", new TableInfo.Column("quantity", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("isChecked", new TableInfo.Column("isChecked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("estimatedPrice", new TableInfo.Column("estimatedPrice", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("addedBy", new TableInfo.Column("addedBy", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("updatedAt", new TableInfo.Column("updatedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("deletedAt", new TableInfo.Column("deletedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShoppingItems.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysShoppingItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesShoppingItems = new HashSet<TableInfo.Index>(3);
        _indicesShoppingItems.add(new TableInfo.Index("index_shopping_items_storeId", false, Arrays.asList("storeId"), Arrays.asList("ASC")));
        _indicesShoppingItems.add(new TableInfo.Index("index_shopping_items_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        _indicesShoppingItems.add(new TableInfo.Index("index_shopping_items_householdId", false, Arrays.asList("householdId"), Arrays.asList("ASC")));
        final TableInfo _infoShoppingItems = new TableInfo("shopping_items", _columnsShoppingItems, _foreignKeysShoppingItems, _indicesShoppingItems);
        final TableInfo _existingShoppingItems = TableInfo.read(db, "shopping_items");
        if (!_infoShoppingItems.equals(_existingShoppingItems)) {
          return new RoomOpenHelper.ValidationResult(false, "shopping_items(com.cartvr.app.data.local.entity.ShoppingItemEntity).\n"
                  + " Expected:\n" + _infoShoppingItems + "\n"
                  + " Found:\n" + _existingShoppingItems);
        }
        final HashMap<String, TableInfo.Column> _columnsRegularItems = new HashMap<String, TableInfo.Column>(9);
        _columnsRegularItems.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRegularItems.put("householdId", new TableInfo.Column("householdId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRegularItems.put("defaultStoreId", new TableInfo.Column("defaultStoreId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRegularItems.put("defaultCategoryId", new TableInfo.Column("defaultCategoryId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRegularItems.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRegularItems.put("defaultQuantity", new TableInfo.Column("defaultQuantity", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRegularItems.put("updatedAt", new TableInfo.Column("updatedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRegularItems.put("deletedAt", new TableInfo.Column("deletedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRegularItems.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRegularItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRegularItems = new HashSet<TableInfo.Index>(1);
        _indicesRegularItems.add(new TableInfo.Index("index_regular_items_householdId", false, Arrays.asList("householdId"), Arrays.asList("ASC")));
        final TableInfo _infoRegularItems = new TableInfo("regular_items", _columnsRegularItems, _foreignKeysRegularItems, _indicesRegularItems);
        final TableInfo _existingRegularItems = TableInfo.read(db, "regular_items");
        if (!_infoRegularItems.equals(_existingRegularItems)) {
          return new RoomOpenHelper.ValidationResult(false, "regular_items(com.cartvr.app.data.local.entity.RegularItemEntity).\n"
                  + " Expected:\n" + _infoRegularItems + "\n"
                  + " Found:\n" + _existingRegularItems);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "26216ad075559be5b57a1480a2faf3ce", "f8fb0a9b2a495c3e874e6f23ac34c53c");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "stores","categories","shopping_items","regular_items");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `stores`");
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `shopping_items`");
      _db.execSQL("DELETE FROM `regular_items`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(CartVrDao.class, CartVrDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public CartVrDao cartVrDao() {
    if (_cartVrDao != null) {
      return _cartVrDao;
    } else {
      synchronized(this) {
        if(_cartVrDao == null) {
          _cartVrDao = new CartVrDao_Impl(this);
        }
        return _cartVrDao;
      }
    }
  }
}
