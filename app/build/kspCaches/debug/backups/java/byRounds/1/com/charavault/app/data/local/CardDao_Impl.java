package com.charavault.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
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
public final class CardDao_Impl implements CardDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CardEntity> __insertionAdapterOfCardEntity;

  private final EntityDeletionOrUpdateAdapter<CardEntity> __deletionAdapterOfCardEntity;

  private final EntityDeletionOrUpdateAdapter<CardEntity> __updateAdapterOfCardEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSortOrder;

  private final SharedSQLiteStatement __preparedStmtOfSetFavorite;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllCards;

  public CardDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCardEntity = new EntityInsertionAdapter<CardEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cards` (`id`,`name`,`creator`,`description`,`personality`,`scenario`,`firstMes`,`systemPrompt`,`tagsJson`,`alternateGreetingsJson`,`rawJsonData`,`imagePath`,`fileHash`,`semanticHash`,`sortOrder`,`isFavorite`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CardEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCreator());
        statement.bindString(4, entity.getDescription());
        statement.bindString(5, entity.getPersonality());
        statement.bindString(6, entity.getScenario());
        statement.bindString(7, entity.getFirstMes());
        statement.bindString(8, entity.getSystemPrompt());
        statement.bindString(9, entity.getTagsJson());
        statement.bindString(10, entity.getAlternateGreetingsJson());
        statement.bindString(11, entity.getRawJsonData());
        statement.bindString(12, entity.getImagePath());
        statement.bindString(13, entity.getFileHash());
        statement.bindString(14, entity.getSemanticHash());
        statement.bindLong(15, entity.getSortOrder());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(16, _tmp);
        statement.bindLong(17, entity.getCreatedAt());
        statement.bindLong(18, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfCardEntity = new EntityDeletionOrUpdateAdapter<CardEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `cards` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CardEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfCardEntity = new EntityDeletionOrUpdateAdapter<CardEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `cards` SET `id` = ?,`name` = ?,`creator` = ?,`description` = ?,`personality` = ?,`scenario` = ?,`firstMes` = ?,`systemPrompt` = ?,`tagsJson` = ?,`alternateGreetingsJson` = ?,`rawJsonData` = ?,`imagePath` = ?,`fileHash` = ?,`semanticHash` = ?,`sortOrder` = ?,`isFavorite` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CardEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCreator());
        statement.bindString(4, entity.getDescription());
        statement.bindString(5, entity.getPersonality());
        statement.bindString(6, entity.getScenario());
        statement.bindString(7, entity.getFirstMes());
        statement.bindString(8, entity.getSystemPrompt());
        statement.bindString(9, entity.getTagsJson());
        statement.bindString(10, entity.getAlternateGreetingsJson());
        statement.bindString(11, entity.getRawJsonData());
        statement.bindString(12, entity.getImagePath());
        statement.bindString(13, entity.getFileHash());
        statement.bindString(14, entity.getSemanticHash());
        statement.bindLong(15, entity.getSortOrder());
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(16, _tmp);
        statement.bindLong(17, entity.getCreatedAt());
        statement.bindLong(18, entity.getUpdatedAt());
        statement.bindString(19, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateSortOrder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE cards SET sortOrder = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetFavorite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE cards SET isFavorite = ?, updatedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllCards = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cards";
        return _query;
      }
    };
  }

  @Override
  public Object insertCard(final CardEntity card, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCardEntity.insert(card);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCard(final CardEntity card, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCardEntity.handle(card);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCard(final CardEntity card, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCardEntity.handle(card);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSortOrder(final String id, final int sortOrder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSortOrder.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, sortOrder);
        _argIndex = 2;
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
          __preparedStmtOfUpdateSortOrder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setFavorite(final String id, final boolean isFavorite, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetFavorite.acquire();
        int _argIndex = 1;
        final int _tmp = isFavorite ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 3;
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
          __preparedStmtOfSetFavorite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllCards(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllCards.acquire();
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
          __preparedStmtOfDeleteAllCards.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CardEntity>> getAllCardsFlow() {
    final String _sql = "SELECT * FROM cards ORDER BY sortOrder ASC, updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cards"}, new Callable<List<CardEntity>>() {
      @Override
      @NonNull
      public List<CardEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreator = CursorUtil.getColumnIndexOrThrow(_cursor, "creator");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfPersonality = CursorUtil.getColumnIndexOrThrow(_cursor, "personality");
          final int _cursorIndexOfScenario = CursorUtil.getColumnIndexOrThrow(_cursor, "scenario");
          final int _cursorIndexOfFirstMes = CursorUtil.getColumnIndexOrThrow(_cursor, "firstMes");
          final int _cursorIndexOfSystemPrompt = CursorUtil.getColumnIndexOrThrow(_cursor, "systemPrompt");
          final int _cursorIndexOfTagsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "tagsJson");
          final int _cursorIndexOfAlternateGreetingsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "alternateGreetingsJson");
          final int _cursorIndexOfRawJsonData = CursorUtil.getColumnIndexOrThrow(_cursor, "rawJsonData");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfSemanticHash = CursorUtil.getColumnIndexOrThrow(_cursor, "semanticHash");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<CardEntity> _result = new ArrayList<CardEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CardEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCreator;
            _tmpCreator = _cursor.getString(_cursorIndexOfCreator);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpPersonality;
            _tmpPersonality = _cursor.getString(_cursorIndexOfPersonality);
            final String _tmpScenario;
            _tmpScenario = _cursor.getString(_cursorIndexOfScenario);
            final String _tmpFirstMes;
            _tmpFirstMes = _cursor.getString(_cursorIndexOfFirstMes);
            final String _tmpSystemPrompt;
            _tmpSystemPrompt = _cursor.getString(_cursorIndexOfSystemPrompt);
            final String _tmpTagsJson;
            _tmpTagsJson = _cursor.getString(_cursorIndexOfTagsJson);
            final String _tmpAlternateGreetingsJson;
            _tmpAlternateGreetingsJson = _cursor.getString(_cursorIndexOfAlternateGreetingsJson);
            final String _tmpRawJsonData;
            _tmpRawJsonData = _cursor.getString(_cursorIndexOfRawJsonData);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final String _tmpFileHash;
            _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            final String _tmpSemanticHash;
            _tmpSemanticHash = _cursor.getString(_cursorIndexOfSemanticHash);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new CardEntity(_tmpId,_tmpName,_tmpCreator,_tmpDescription,_tmpPersonality,_tmpScenario,_tmpFirstMes,_tmpSystemPrompt,_tmpTagsJson,_tmpAlternateGreetingsJson,_tmpRawJsonData,_tmpImagePath,_tmpFileHash,_tmpSemanticHash,_tmpSortOrder,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getCardById(final String id, final Continuation<? super CardEntity> $completion) {
    final String _sql = "SELECT * FROM cards WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CardEntity>() {
      @Override
      @Nullable
      public CardEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreator = CursorUtil.getColumnIndexOrThrow(_cursor, "creator");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfPersonality = CursorUtil.getColumnIndexOrThrow(_cursor, "personality");
          final int _cursorIndexOfScenario = CursorUtil.getColumnIndexOrThrow(_cursor, "scenario");
          final int _cursorIndexOfFirstMes = CursorUtil.getColumnIndexOrThrow(_cursor, "firstMes");
          final int _cursorIndexOfSystemPrompt = CursorUtil.getColumnIndexOrThrow(_cursor, "systemPrompt");
          final int _cursorIndexOfTagsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "tagsJson");
          final int _cursorIndexOfAlternateGreetingsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "alternateGreetingsJson");
          final int _cursorIndexOfRawJsonData = CursorUtil.getColumnIndexOrThrow(_cursor, "rawJsonData");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfSemanticHash = CursorUtil.getColumnIndexOrThrow(_cursor, "semanticHash");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final CardEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCreator;
            _tmpCreator = _cursor.getString(_cursorIndexOfCreator);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpPersonality;
            _tmpPersonality = _cursor.getString(_cursorIndexOfPersonality);
            final String _tmpScenario;
            _tmpScenario = _cursor.getString(_cursorIndexOfScenario);
            final String _tmpFirstMes;
            _tmpFirstMes = _cursor.getString(_cursorIndexOfFirstMes);
            final String _tmpSystemPrompt;
            _tmpSystemPrompt = _cursor.getString(_cursorIndexOfSystemPrompt);
            final String _tmpTagsJson;
            _tmpTagsJson = _cursor.getString(_cursorIndexOfTagsJson);
            final String _tmpAlternateGreetingsJson;
            _tmpAlternateGreetingsJson = _cursor.getString(_cursorIndexOfAlternateGreetingsJson);
            final String _tmpRawJsonData;
            _tmpRawJsonData = _cursor.getString(_cursorIndexOfRawJsonData);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final String _tmpFileHash;
            _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            final String _tmpSemanticHash;
            _tmpSemanticHash = _cursor.getString(_cursorIndexOfSemanticHash);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new CardEntity(_tmpId,_tmpName,_tmpCreator,_tmpDescription,_tmpPersonality,_tmpScenario,_tmpFirstMes,_tmpSystemPrompt,_tmpTagsJson,_tmpAlternateGreetingsJson,_tmpRawJsonData,_tmpImagePath,_tmpFileHash,_tmpSemanticHash,_tmpSortOrder,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getCardByFileHash(final String fileHash,
      final Continuation<? super CardEntity> $completion) {
    final String _sql = "SELECT * FROM cards WHERE fileHash = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, fileHash);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CardEntity>() {
      @Override
      @Nullable
      public CardEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreator = CursorUtil.getColumnIndexOrThrow(_cursor, "creator");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfPersonality = CursorUtil.getColumnIndexOrThrow(_cursor, "personality");
          final int _cursorIndexOfScenario = CursorUtil.getColumnIndexOrThrow(_cursor, "scenario");
          final int _cursorIndexOfFirstMes = CursorUtil.getColumnIndexOrThrow(_cursor, "firstMes");
          final int _cursorIndexOfSystemPrompt = CursorUtil.getColumnIndexOrThrow(_cursor, "systemPrompt");
          final int _cursorIndexOfTagsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "tagsJson");
          final int _cursorIndexOfAlternateGreetingsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "alternateGreetingsJson");
          final int _cursorIndexOfRawJsonData = CursorUtil.getColumnIndexOrThrow(_cursor, "rawJsonData");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfSemanticHash = CursorUtil.getColumnIndexOrThrow(_cursor, "semanticHash");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final CardEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCreator;
            _tmpCreator = _cursor.getString(_cursorIndexOfCreator);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpPersonality;
            _tmpPersonality = _cursor.getString(_cursorIndexOfPersonality);
            final String _tmpScenario;
            _tmpScenario = _cursor.getString(_cursorIndexOfScenario);
            final String _tmpFirstMes;
            _tmpFirstMes = _cursor.getString(_cursorIndexOfFirstMes);
            final String _tmpSystemPrompt;
            _tmpSystemPrompt = _cursor.getString(_cursorIndexOfSystemPrompt);
            final String _tmpTagsJson;
            _tmpTagsJson = _cursor.getString(_cursorIndexOfTagsJson);
            final String _tmpAlternateGreetingsJson;
            _tmpAlternateGreetingsJson = _cursor.getString(_cursorIndexOfAlternateGreetingsJson);
            final String _tmpRawJsonData;
            _tmpRawJsonData = _cursor.getString(_cursorIndexOfRawJsonData);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final String _tmpFileHash;
            _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            final String _tmpSemanticHash;
            _tmpSemanticHash = _cursor.getString(_cursorIndexOfSemanticHash);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new CardEntity(_tmpId,_tmpName,_tmpCreator,_tmpDescription,_tmpPersonality,_tmpScenario,_tmpFirstMes,_tmpSystemPrompt,_tmpTagsJson,_tmpAlternateGreetingsJson,_tmpRawJsonData,_tmpImagePath,_tmpFileHash,_tmpSemanticHash,_tmpSortOrder,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getCardBySemanticHash(final String semanticHash,
      final Continuation<? super CardEntity> $completion) {
    final String _sql = "SELECT * FROM cards WHERE semanticHash = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, semanticHash);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CardEntity>() {
      @Override
      @Nullable
      public CardEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreator = CursorUtil.getColumnIndexOrThrow(_cursor, "creator");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfPersonality = CursorUtil.getColumnIndexOrThrow(_cursor, "personality");
          final int _cursorIndexOfScenario = CursorUtil.getColumnIndexOrThrow(_cursor, "scenario");
          final int _cursorIndexOfFirstMes = CursorUtil.getColumnIndexOrThrow(_cursor, "firstMes");
          final int _cursorIndexOfSystemPrompt = CursorUtil.getColumnIndexOrThrow(_cursor, "systemPrompt");
          final int _cursorIndexOfTagsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "tagsJson");
          final int _cursorIndexOfAlternateGreetingsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "alternateGreetingsJson");
          final int _cursorIndexOfRawJsonData = CursorUtil.getColumnIndexOrThrow(_cursor, "rawJsonData");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfFileHash = CursorUtil.getColumnIndexOrThrow(_cursor, "fileHash");
          final int _cursorIndexOfSemanticHash = CursorUtil.getColumnIndexOrThrow(_cursor, "semanticHash");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final CardEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCreator;
            _tmpCreator = _cursor.getString(_cursorIndexOfCreator);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpPersonality;
            _tmpPersonality = _cursor.getString(_cursorIndexOfPersonality);
            final String _tmpScenario;
            _tmpScenario = _cursor.getString(_cursorIndexOfScenario);
            final String _tmpFirstMes;
            _tmpFirstMes = _cursor.getString(_cursorIndexOfFirstMes);
            final String _tmpSystemPrompt;
            _tmpSystemPrompt = _cursor.getString(_cursorIndexOfSystemPrompt);
            final String _tmpTagsJson;
            _tmpTagsJson = _cursor.getString(_cursorIndexOfTagsJson);
            final String _tmpAlternateGreetingsJson;
            _tmpAlternateGreetingsJson = _cursor.getString(_cursorIndexOfAlternateGreetingsJson);
            final String _tmpRawJsonData;
            _tmpRawJsonData = _cursor.getString(_cursorIndexOfRawJsonData);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final String _tmpFileHash;
            _tmpFileHash = _cursor.getString(_cursorIndexOfFileHash);
            final String _tmpSemanticHash;
            _tmpSemanticHash = _cursor.getString(_cursorIndexOfSemanticHash);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new CardEntity(_tmpId,_tmpName,_tmpCreator,_tmpDescription,_tmpPersonality,_tmpScenario,_tmpFirstMes,_tmpSystemPrompt,_tmpTagsJson,_tmpAlternateGreetingsJson,_tmpRawJsonData,_tmpImagePath,_tmpFileHash,_tmpSemanticHash,_tmpSortOrder,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
