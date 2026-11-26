package com.cartvr.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.cartvr.app.data.local.entity.CategoryEntity;
import com.cartvr.app.data.local.entity.RegularItemEntity;
import com.cartvr.app.data.local.entity.ShoppingItemEntity;
import com.cartvr.app.data.local.entity.StoreEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CartVrDao_Impl implements CartVrDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StoreEntity> __insertionAdapterOfStoreEntity;

  private final EntityInsertionAdapter<CategoryEntity> __insertionAdapterOfCategoryEntity;

  private final EntityInsertionAdapter<ShoppingItemEntity> __insertionAdapterOfShoppingItemEntity;

  private final EntityInsertionAdapter<RegularItemEntity> __insertionAdapterOfRegularItemEntity;

  private final SharedSQLiteStatement __preparedStmtOfHardDeleteShoppingItem;

  private final SharedSQLiteStatement __preparedStmtOfHardDeleteRegularItem;

  private final SharedSQLiteStatement __preparedStmtOfClearStores;

  private final SharedSQLiteStatement __preparedStmtOfClearCategories;

  private final SharedSQLiteStatement __preparedStmtOfClearShoppingItems;

  private final SharedSQLiteStatement __preparedStmtOfClearRegularItems;

  public CartVrDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStoreEntity = new EntityInsertionAdapter<StoreEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `stores` (`id`,`householdId`,`name`,`sortOrder`,`updatedAt`,`deletedAt`,`syncStatus`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StoreEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getHouseholdId());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getSortOrder());
        statement.bindString(5, entity.getUpdatedAt());
        if (entity.getDeletedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDeletedAt());
        }
        statement.bindString(7, entity.getSyncStatus());
      }
    };
    this.__insertionAdapterOfCategoryEntity = new EntityInsertionAdapter<CategoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `categories` (`id`,`householdId`,`name`,`sortOrder`,`updatedAt`,`deletedAt`,`syncStatus`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CategoryEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getHouseholdId());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getSortOrder());
        statement.bindString(5, entity.getUpdatedAt());
        if (entity.getDeletedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDeletedAt());
        }
        statement.bindString(7, entity.getSyncStatus());
      }
    };
    this.__insertionAdapterOfShoppingItemEntity = new EntityInsertionAdapter<ShoppingItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `shopping_items` (`id`,`householdId`,`storeId`,`categoryId`,`name`,`quantity`,`note`,`isChecked`,`estimatedPrice`,`addedBy`,`createdAt`,`updatedAt`,`deletedAt`,`syncStatus`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShoppingItemEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getHouseholdId());
        statement.bindString(3, entity.getStoreId());
        if (entity.getCategoryId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCategoryId());
        }
        statement.bindString(5, entity.getName());
        if (entity.getQuantity() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getQuantity());
        }
        if (entity.getNote() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getNote());
        }
        final int _tmp = entity.isChecked() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getEstimatedPrice() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getEstimatedPrice());
        }
        if (entity.getAddedBy() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getAddedBy());
        }
        statement.bindString(11, entity.getCreatedAt());
        statement.bindString(12, entity.getUpdatedAt());
        if (entity.getDeletedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getDeletedAt());
        }
        statement.bindString(14, entity.getSyncStatus());
      }
    };
    this.__insertionAdapterOfRegularItemEntity = new EntityInsertionAdapter<RegularItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `regular_items` (`id`,`householdId`,`defaultStoreId`,`defaultCategoryId`,`name`,`defaultQuantity`,`updatedAt`,`deletedAt`,`syncStatus`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RegularItemEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getHouseholdId());
        if (entity.getDefaultStoreId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDefaultStoreId());
        }
        if (entity.getDefaultCategoryId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDefaultCategoryId());
        }
        statement.bindString(5, entity.getName());
        if (entity.getDefaultQuantity() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDefaultQuantity());
        }
        statement.bindString(7, entity.getUpdatedAt());
        if (entity.getDeletedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getDeletedAt());
        }
        statement.bindString(9, entity.getSyncStatus());
      }
    };
    this.__preparedStmtOfHardDeleteShoppingItem = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM shopping_items WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfHardDeleteRegularItem = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM regular_items WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearStores = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM stores";
        return _query;
      }
    };
    this.__preparedStmtOfClearCategories = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM categories";
        return _query;
      }
    };
    this.__preparedStmtOfClearShoppingItems = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM shopping_items";
        return _query;
      }
    };
    this.__preparedStmtOfClearRegularItems = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM regular_items";
        return _query;
      }
    };
  }

  @Override
  public Object upsertStores(final List<StoreEntity> stores,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStoreEntity.insert(stores);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertStore(final StoreEntity store, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStoreEntity.insert(store);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertCategories(final List<CategoryEntity> categories,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCategoryEntity.insert(categories);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertCategory(final CategoryEntity category,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCategoryEntity.insert(category);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertShoppingItems(final List<ShoppingItemEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfShoppingItemEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertShoppingItem(final ShoppingItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfShoppingItemEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertRegularItems(final List<RegularItemEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRegularItemEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertRegularItem(final RegularItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRegularItemEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object hardDeleteShoppingItem(final String id,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfHardDeleteShoppingItem.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfHardDeleteShoppingItem.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object hardDeleteRegularItem(final String id,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfHardDeleteRegularItem.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfHardDeleteRegularItem.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearStores(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearStores.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearStores.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearCategories(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearCategories.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearCategories.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearShoppingItems(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearShoppingItems.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearShoppingItems.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearRegularItems(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearRegularItems.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearRegularItems.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<StoreEntity>> observeActiveStores() {
    final String _sql = "SELECT * FROM stores WHERE deletedAt IS NULL ORDER BY sortOrder ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stores"}, new Callable<List<StoreEntity>>() {
      @Override
      @NonNull
      public List<StoreEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<StoreEntity> _result = new ArrayList<StoreEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StoreEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new StoreEntity(_tmpId,_tmpHouseholdId,_tmpName,_tmpSortOrder,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getStoreById(final String id, final Continuation<? super StoreEntity> $completion) {
    final String _sql = "SELECT * FROM stores WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<StoreEntity>() {
      @Override
      @Nullable
      public StoreEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final StoreEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _result = new StoreEntity(_tmpId,_tmpHouseholdId,_tmpName,_tmpSortOrder,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingStores(final Continuation<? super List<StoreEntity>> $completion) {
    final String _sql = "SELECT * FROM stores WHERE syncStatus != 'SYNCED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<StoreEntity>>() {
      @Override
      @NonNull
      public List<StoreEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<StoreEntity> _result = new ArrayList<StoreEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StoreEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new StoreEntity(_tmpId,_tmpHouseholdId,_tmpName,_tmpSortOrder,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CategoryEntity>> observeActiveCategories() {
    final String _sql = "SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY sortOrder ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"categories"}, new Callable<List<CategoryEntity>>() {
      @Override
      @NonNull
      public List<CategoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<CategoryEntity> _result = new ArrayList<CategoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategoryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new CategoryEntity(_tmpId,_tmpHouseholdId,_tmpName,_tmpSortOrder,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getCategoryById(final String id,
      final Continuation<? super CategoryEntity> $completion) {
    final String _sql = "SELECT * FROM categories WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CategoryEntity>() {
      @Override
      @Nullable
      public CategoryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final CategoryEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _result = new CategoryEntity(_tmpId,_tmpHouseholdId,_tmpName,_tmpSortOrder,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingCategories(final Continuation<? super List<CategoryEntity>> $completion) {
    final String _sql = "SELECT * FROM categories WHERE syncStatus != 'SYNCED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CategoryEntity>>() {
      @Override
      @NonNull
      public List<CategoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<CategoryEntity> _result = new ArrayList<CategoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategoryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new CategoryEntity(_tmpId,_tmpHouseholdId,_tmpName,_tmpSortOrder,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ShoppingItemEntity>> observeAllActiveShoppingItems() {
    final String _sql = "SELECT * FROM shopping_items WHERE deletedAt IS NULL ORDER BY isChecked ASC, createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shopping_items"}, new Callable<List<ShoppingItemEntity>>() {
      @Override
      @NonNull
      public List<ShoppingItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfStoreId = CursorUtil.getColumnIndexOrThrow(_cursor, "storeId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfIsChecked = CursorUtil.getColumnIndexOrThrow(_cursor, "isChecked");
          final int _cursorIndexOfEstimatedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "estimatedPrice");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<ShoppingItemEntity> _result = new ArrayList<ShoppingItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShoppingItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpStoreId;
            _tmpStoreId = _cursor.getString(_cursorIndexOfStoreId);
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpQuantity;
            if (_cursor.isNull(_cursorIndexOfQuantity)) {
              _tmpQuantity = null;
            } else {
              _tmpQuantity = _cursor.getString(_cursorIndexOfQuantity);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final boolean _tmpIsChecked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsChecked);
            _tmpIsChecked = _tmp != 0;
            final Double _tmpEstimatedPrice;
            if (_cursor.isNull(_cursorIndexOfEstimatedPrice)) {
              _tmpEstimatedPrice = null;
            } else {
              _tmpEstimatedPrice = _cursor.getDouble(_cursorIndexOfEstimatedPrice);
            }
            final String _tmpAddedBy;
            if (_cursor.isNull(_cursorIndexOfAddedBy)) {
              _tmpAddedBy = null;
            } else {
              _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new ShoppingItemEntity(_tmpId,_tmpHouseholdId,_tmpStoreId,_tmpCategoryId,_tmpName,_tmpQuantity,_tmpNote,_tmpIsChecked,_tmpEstimatedPrice,_tmpAddedBy,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ShoppingItemEntity>> observeStoreShoppingItems(final String storeId) {
    final String _sql = "SELECT * FROM shopping_items WHERE storeId = ? AND deletedAt IS NULL ORDER BY isChecked ASC, createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, storeId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shopping_items"}, new Callable<List<ShoppingItemEntity>>() {
      @Override
      @NonNull
      public List<ShoppingItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfStoreId = CursorUtil.getColumnIndexOrThrow(_cursor, "storeId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfIsChecked = CursorUtil.getColumnIndexOrThrow(_cursor, "isChecked");
          final int _cursorIndexOfEstimatedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "estimatedPrice");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<ShoppingItemEntity> _result = new ArrayList<ShoppingItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShoppingItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpStoreId;
            _tmpStoreId = _cursor.getString(_cursorIndexOfStoreId);
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpQuantity;
            if (_cursor.isNull(_cursorIndexOfQuantity)) {
              _tmpQuantity = null;
            } else {
              _tmpQuantity = _cursor.getString(_cursorIndexOfQuantity);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final boolean _tmpIsChecked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsChecked);
            _tmpIsChecked = _tmp != 0;
            final Double _tmpEstimatedPrice;
            if (_cursor.isNull(_cursorIndexOfEstimatedPrice)) {
              _tmpEstimatedPrice = null;
            } else {
              _tmpEstimatedPrice = _cursor.getDouble(_cursorIndexOfEstimatedPrice);
            }
            final String _tmpAddedBy;
            if (_cursor.isNull(_cursorIndexOfAddedBy)) {
              _tmpAddedBy = null;
            } else {
              _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new ShoppingItemEntity(_tmpId,_tmpHouseholdId,_tmpStoreId,_tmpCategoryId,_tmpName,_tmpQuantity,_tmpNote,_tmpIsChecked,_tmpEstimatedPrice,_tmpAddedBy,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getShoppingItemById(final String id,
      final Continuation<? super ShoppingItemEntity> $completion) {
    final String _sql = "SELECT * FROM shopping_items WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ShoppingItemEntity>() {
      @Override
      @Nullable
      public ShoppingItemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfStoreId = CursorUtil.getColumnIndexOrThrow(_cursor, "storeId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfIsChecked = CursorUtil.getColumnIndexOrThrow(_cursor, "isChecked");
          final int _cursorIndexOfEstimatedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "estimatedPrice");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final ShoppingItemEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpStoreId;
            _tmpStoreId = _cursor.getString(_cursorIndexOfStoreId);
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpQuantity;
            if (_cursor.isNull(_cursorIndexOfQuantity)) {
              _tmpQuantity = null;
            } else {
              _tmpQuantity = _cursor.getString(_cursorIndexOfQuantity);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final boolean _tmpIsChecked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsChecked);
            _tmpIsChecked = _tmp != 0;
            final Double _tmpEstimatedPrice;
            if (_cursor.isNull(_cursorIndexOfEstimatedPrice)) {
              _tmpEstimatedPrice = null;
            } else {
              _tmpEstimatedPrice = _cursor.getDouble(_cursorIndexOfEstimatedPrice);
            }
            final String _tmpAddedBy;
            if (_cursor.isNull(_cursorIndexOfAddedBy)) {
              _tmpAddedBy = null;
            } else {
              _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _result = new ShoppingItemEntity(_tmpId,_tmpHouseholdId,_tmpStoreId,_tmpCategoryId,_tmpName,_tmpQuantity,_tmpNote,_tmpIsChecked,_tmpEstimatedPrice,_tmpAddedBy,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingShoppingItems(
      final Continuation<? super List<ShoppingItemEntity>> $completion) {
    final String _sql = "SELECT * FROM shopping_items WHERE syncStatus != 'SYNCED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ShoppingItemEntity>>() {
      @Override
      @NonNull
      public List<ShoppingItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfStoreId = CursorUtil.getColumnIndexOrThrow(_cursor, "storeId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfIsChecked = CursorUtil.getColumnIndexOrThrow(_cursor, "isChecked");
          final int _cursorIndexOfEstimatedPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "estimatedPrice");
          final int _cursorIndexOfAddedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "addedBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<ShoppingItemEntity> _result = new ArrayList<ShoppingItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShoppingItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpStoreId;
            _tmpStoreId = _cursor.getString(_cursorIndexOfStoreId);
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpQuantity;
            if (_cursor.isNull(_cursorIndexOfQuantity)) {
              _tmpQuantity = null;
            } else {
              _tmpQuantity = _cursor.getString(_cursorIndexOfQuantity);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final boolean _tmpIsChecked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsChecked);
            _tmpIsChecked = _tmp != 0;
            final Double _tmpEstimatedPrice;
            if (_cursor.isNull(_cursorIndexOfEstimatedPrice)) {
              _tmpEstimatedPrice = null;
            } else {
              _tmpEstimatedPrice = _cursor.getDouble(_cursorIndexOfEstimatedPrice);
            }
            final String _tmpAddedBy;
            if (_cursor.isNull(_cursorIndexOfAddedBy)) {
              _tmpAddedBy = null;
            } else {
              _tmpAddedBy = _cursor.getString(_cursorIndexOfAddedBy);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new ShoppingItemEntity(_tmpId,_tmpHouseholdId,_tmpStoreId,_tmpCategoryId,_tmpName,_tmpQuantity,_tmpNote,_tmpIsChecked,_tmpEstimatedPrice,_tmpAddedBy,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RegularItemEntity>> observeActiveRegularItems() {
    final String _sql = "SELECT * FROM regular_items WHERE deletedAt IS NULL ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"regular_items"}, new Callable<List<RegularItemEntity>>() {
      @Override
      @NonNull
      public List<RegularItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfDefaultStoreId = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultStoreId");
          final int _cursorIndexOfDefaultCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultCategoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDefaultQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultQuantity");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<RegularItemEntity> _result = new ArrayList<RegularItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RegularItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpDefaultStoreId;
            if (_cursor.isNull(_cursorIndexOfDefaultStoreId)) {
              _tmpDefaultStoreId = null;
            } else {
              _tmpDefaultStoreId = _cursor.getString(_cursorIndexOfDefaultStoreId);
            }
            final String _tmpDefaultCategoryId;
            if (_cursor.isNull(_cursorIndexOfDefaultCategoryId)) {
              _tmpDefaultCategoryId = null;
            } else {
              _tmpDefaultCategoryId = _cursor.getString(_cursorIndexOfDefaultCategoryId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDefaultQuantity;
            if (_cursor.isNull(_cursorIndexOfDefaultQuantity)) {
              _tmpDefaultQuantity = null;
            } else {
              _tmpDefaultQuantity = _cursor.getString(_cursorIndexOfDefaultQuantity);
            }
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new RegularItemEntity(_tmpId,_tmpHouseholdId,_tmpDefaultStoreId,_tmpDefaultCategoryId,_tmpName,_tmpDefaultQuantity,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRegularItemById(final String id,
      final Continuation<? super RegularItemEntity> $completion) {
    final String _sql = "SELECT * FROM regular_items WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RegularItemEntity>() {
      @Override
      @Nullable
      public RegularItemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfDefaultStoreId = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultStoreId");
          final int _cursorIndexOfDefaultCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultCategoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDefaultQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultQuantity");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final RegularItemEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpDefaultStoreId;
            if (_cursor.isNull(_cursorIndexOfDefaultStoreId)) {
              _tmpDefaultStoreId = null;
            } else {
              _tmpDefaultStoreId = _cursor.getString(_cursorIndexOfDefaultStoreId);
            }
            final String _tmpDefaultCategoryId;
            if (_cursor.isNull(_cursorIndexOfDefaultCategoryId)) {
              _tmpDefaultCategoryId = null;
            } else {
              _tmpDefaultCategoryId = _cursor.getString(_cursorIndexOfDefaultCategoryId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDefaultQuantity;
            if (_cursor.isNull(_cursorIndexOfDefaultQuantity)) {
              _tmpDefaultQuantity = null;
            } else {
              _tmpDefaultQuantity = _cursor.getString(_cursorIndexOfDefaultQuantity);
            }
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _result = new RegularItemEntity(_tmpId,_tmpHouseholdId,_tmpDefaultStoreId,_tmpDefaultCategoryId,_tmpName,_tmpDefaultQuantity,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingRegularItems(
      final Continuation<? super List<RegularItemEntity>> $completion) {
    final String _sql = "SELECT * FROM regular_items WHERE syncStatus != 'SYNCED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RegularItemEntity>>() {
      @Override
      @NonNull
      public List<RegularItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHouseholdId = CursorUtil.getColumnIndexOrThrow(_cursor, "householdId");
          final int _cursorIndexOfDefaultStoreId = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultStoreId");
          final int _cursorIndexOfDefaultCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultCategoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDefaultQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultQuantity");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<RegularItemEntity> _result = new ArrayList<RegularItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RegularItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHouseholdId;
            _tmpHouseholdId = _cursor.getString(_cursorIndexOfHouseholdId);
            final String _tmpDefaultStoreId;
            if (_cursor.isNull(_cursorIndexOfDefaultStoreId)) {
              _tmpDefaultStoreId = null;
            } else {
              _tmpDefaultStoreId = _cursor.getString(_cursorIndexOfDefaultStoreId);
            }
            final String _tmpDefaultCategoryId;
            if (_cursor.isNull(_cursorIndexOfDefaultCategoryId)) {
              _tmpDefaultCategoryId = null;
            } else {
              _tmpDefaultCategoryId = _cursor.getString(_cursorIndexOfDefaultCategoryId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDefaultQuantity;
            if (_cursor.isNull(_cursorIndexOfDefaultQuantity)) {
              _tmpDefaultQuantity = null;
            } else {
              _tmpDefaultQuantity = _cursor.getString(_cursorIndexOfDefaultQuantity);
            }
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new RegularItemEntity(_tmpId,_tmpHouseholdId,_tmpDefaultStoreId,_tmpDefaultCategoryId,_tmpName,_tmpDefaultQuantity,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
