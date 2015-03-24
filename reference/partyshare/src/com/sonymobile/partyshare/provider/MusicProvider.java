/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.sonymobile.partyshare.util.LogUtil;

/**
 * MusicProvider
 */
public class MusicProvider extends ContentProvider implements BaseColumns {
    private static final String AUTHORITIES = "com.sonymobile.partyshare.music";
    private static final String TABLE_PLAYLIST = PartyShareDatabaseHelper.TABLE_PLAYLIST;
    private static final String TABLE_LOCAL_MUSIC = PartyShareDatabaseHelper.TABLE_LOCAL_MUSIC;

    // column for playlist table
    public static final String COLUMN_TITLE = PartyShareDatabaseHelper.COLUMN_TITLE;
    public static final String COLUMN_ARTIST = PartyShareDatabaseHelper.COLUMN_ARTIST;
    public static final String COLUMN_TIME = PartyShareDatabaseHelper.COLUMN_TIME;
    public static final String COLUMN_MUSIC_URI = PartyShareDatabaseHelper.COLUMN_MUSIC_URI;
    public static final String COLUMN_OWNER_ADDRESS =
            PartyShareDatabaseHelper.COLUMN_OWNER_ADDRESS;
    public static final String COLUMN_OWNER_NAME = PartyShareDatabaseHelper.COLUMN_OWNER_NAME;
    public static final String COLUMN_STATUS= PartyShareDatabaseHelper.COLUMN_STATUS;
    public static final String COLUMN_MUSIC_ID = PartyShareDatabaseHelper.COLUMN_MUSIC_ID;
    public static final String COLUMN_PLAY_NUMBER = PartyShareDatabaseHelper.COLUMN_PLAY_NUMBER;

    // column for local_music table
    public static final String COLUMN_MUSIC_LOCAL_PATH =
            PartyShareDatabaseHelper.COLUMN_MUSIC_LOCAL_PATH;
    public static final String COLUMN_MUSIC_MIMETYPE =
            PartyShareDatabaseHelper.COLUMN_MUSIC_MIMETYPE;

    // Content Uri
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITIES + "/" + TABLE_PLAYLIST);
    public static final Uri CONTENT_URI_PLAY_NEXT =
            Uri.parse("content://" + AUTHORITIES + "/" + TABLE_PLAYLIST + "/" + COLUMN_PLAY_NUMBER);
    public static final Uri CONTENT_URI_REFRESH =
            Uri.parse("content://" + AUTHORITIES + "/" + TABLE_PLAYLIST + "/refresh");
    public static final Uri CONTENT_URI_LOCAL_MUSIC =
            Uri.parse("content://" + AUTHORITIES + "/" + TABLE_LOCAL_MUSIC);

    // Status
    public static final int MUSIC_STATUS_IDLE = 0;
    public static final int MUSIC_STATUS_FOCUSED = 1;

    // Matching code
    private static final int URI_PLAYLIST = 1;
    private static final int URI_PLAYLIST_ID = 2;
    private static final int URI_PLAYLIST_PLAY_NEXT = 3;
    private static final int URI_LOCAL_MUSIC = 4;
    private static final int URI_LOCAL_MUSIC_ID = 5;

    // SQL query get max play number
    private static final String SQL_QUERY_MAX_PLAY_NUMBER =
        "select max(" + COLUMN_PLAY_NUMBER + ") from " + TABLE_PLAYLIST;

    // SQL update play number lower case for play next
    private static final String SQL_UPDATE_PLAY_NUMBER_LOWER =
        "update " + TABLE_PLAYLIST + " set " + COLUMN_PLAY_NUMBER +
        " = (" + COLUMN_PLAY_NUMBER + " -1) where " + COLUMN_PLAY_NUMBER +
        " > %1$s and " + COLUMN_PLAY_NUMBER + " <= %2$s";

    // SQL update play number upper case for play next
    private static final String SQL_UPDATE_PLAY_NUMBER_UPPER =
        "update " + TABLE_PLAYLIST + " set " + COLUMN_PLAY_NUMBER +
        " = (" + COLUMN_PLAY_NUMBER + " +1) where " + COLUMN_PLAY_NUMBER +
        " > %1$s and " + COLUMN_PLAY_NUMBER + " < %2$s";

    // SQL bulk insert for playlist table
    private static final String SQL_BULK_INSERT_PLAY_LIST =
        "insert into " + TABLE_PLAYLIST + "(" +
        COLUMN_TITLE + ", " + COLUMN_ARTIST + ", " + COLUMN_TIME + ", " + COLUMN_MUSIC_URI +", " +
        COLUMN_OWNER_ADDRESS + ", " + COLUMN_OWNER_NAME + ", " + COLUMN_STATUS + ", " +
        COLUMN_MUSIC_ID + ", " + COLUMN_PLAY_NUMBER + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";

    private PartyShareDatabaseHelper mOpenHelper;

    private static final UriMatcher s_urlMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        s_urlMatcher.addURI(AUTHORITIES, TABLE_PLAYLIST, URI_PLAYLIST);
        s_urlMatcher.addURI(AUTHORITIES, TABLE_PLAYLIST + "/#", URI_PLAYLIST_ID);
        s_urlMatcher.addURI(AUTHORITIES,
                TABLE_PLAYLIST+ "/" + COLUMN_PLAY_NUMBER + "/#", URI_PLAYLIST_PLAY_NEXT);
        s_urlMatcher.addURI(AUTHORITIES, TABLE_LOCAL_MUSIC, URI_LOCAL_MUSIC);
        s_urlMatcher.addURI(AUTHORITIES, TABLE_LOCAL_MUSIC + "/#", URI_LOCAL_MUSIC_ID);
    }

    /**
     * onCreate.
     * @return True if the provider was successfully loaded, false otherwise.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = PartyShareDatabaseHelper.getInstance(getContext());
        return true;
    }

    /**
     * query.
     * @param uri The URI to query.
     * @param projection The list of columns to put into the cursor.
     * @param selection A selection criteria to apply when filtering rows.
     * @param selection A selection criteria to apply when filtering rows.
     * @param selectionArgs If selection include "?s", it will be replaced
     *        by the values from selectionArgs.
     * @param sortOrder How the rows in the cursor should be sorted.
     * @return Cursor or null.
     */
    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int match = s_urlMatcher.match(url);
        switch (match) {
            case URI_PLAYLIST:
                qb.setTables(TABLE_PLAYLIST);
                break;
            case URI_LOCAL_MUSIC:
                qb.setTables(TABLE_LOCAL_MUSIC);
                break;
            default:
                LogUtil.e(LogUtil.LOG_TAG, "MusicProvider.query : uri does not match");
                return null;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), url);
        }
        return cursor;
    }

    /**
     * insert.
     * @param uri The URI to insert.
     * @param values A set of column_name/value pairs to add to the database.
     * @return The URI for the newly inserted item.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LogUtil.d(LogUtil.LOG_TAG, "MusicProvider.insert - uri : " + uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri resultUri = null;
        long rowId = -1;

        int match = s_urlMatcher.match(uri);
        switch (match) {
            case URI_PLAYLIST:
                adjustContentValuesIfNeeded(values);
                rowId = db.insert(TABLE_PLAYLIST, null, values);
                resultUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                break;
            case URI_LOCAL_MUSIC:
                rowId = db.insert(TABLE_LOCAL_MUSIC, null, values);
                resultUri = ContentUris.withAppendedId(CONTENT_URI_LOCAL_MUSIC, rowId);
                break;
            default:
                LogUtil.e(LogUtil.LOG_TAG, "MusicProvider.insert : uri does not match");
                break;
        }
        if (rowId != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return resultUri;
    }

    /**
     * bulk insert.
     * @param uri The URI to insert.
     * @param values An array of sets of column_name/value pairs to add to the database.
     * @return The number of values that were inserted.
     */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        LogUtil.d(LogUtil.LOG_TAG, "MusicProvider.bulkInsert - uri : " + uri);
        int count = 0;

        int match = s_urlMatcher.match(uri);
        switch (match) {
            case URI_PLAYLIST:
                count = bulkInsertForPlaylist(values);
                break;
            default:
                LogUtil.e(LogUtil.LOG_TAG, "MusicProvider.bulkInsert : uri does not match");
                break;
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    /**
     * update.
     * @param uri The URI to update.
     * @param values A set of column_name/value pairs to add to the database.
     * @param selection An optional filter to match rows to update.
     * @param selectionArgs If selection include "?s", it will be replaced
     *        by the values from selectionArgs.
     * @return The number of rows affected.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        LogUtil.d(LogUtil.LOG_TAG, "MusicProvider.update - uri : " + uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;

        int match = s_urlMatcher.match(uri);
        switch (match) {
            case URI_PLAYLIST:
                count = db.update(TABLE_PLAYLIST, values, selection, selectionArgs);
                break;
            case URI_PLAYLIST_ID:
                String id = uri.getLastPathSegment();
                count = db.update(TABLE_PLAYLIST, values,
                        COLUMN_MUSIC_ID + "=" + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;
            case URI_PLAYLIST_PLAY_NEXT:
                String nextId = uri.getLastPathSegment();
                count = updatePlayNumberToNext(nextId);
                break;
            case URI_LOCAL_MUSIC:
                count = db.update(TABLE_LOCAL_MUSIC, values, selection, selectionArgs);
                break;
            case URI_LOCAL_MUSIC_ID:
                count = db.update(TABLE_LOCAL_MUSIC, values,
                        BaseColumns._ID + "=" + uri.getLastPathSegment() +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;
            default:
                LogUtil.e(LogUtil.LOG_TAG, "MusicProvider.update : uri does not match");
                break;
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    /**
     * delete.
     * @param uri The URI to delete.
     * @param selection An optional restriction to apply to rows when deleting.
     * @param selectionArgs If selection include "?s", it will be replaced
     *        by the values from selectionArgs.
     * @return The number of rows affected.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        LogUtil.d(LogUtil.LOG_TAG, "MusicProvider.delete - uri : " + uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        int musicId = -1;
        int match = s_urlMatcher.match(uri);
        switch (match) {
            case URI_PLAYLIST:
                musicId = getNextFocusedMusicId(selection, selectionArgs);
                count = db.delete(TABLE_PLAYLIST, selection, selectionArgs);
                break;
            case URI_PLAYLIST_ID:
                String id = uri.getLastPathSegment();
                String where = COLUMN_MUSIC_ID + "=" + id +
                    (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
                musicId = getNextFocusedMusicId(where, selectionArgs);
                count = db.delete(TABLE_PLAYLIST, where, selectionArgs);
                break;
            case URI_LOCAL_MUSIC:
                count = db.delete(TABLE_LOCAL_MUSIC, selection, selectionArgs);
                break;
            case URI_LOCAL_MUSIC_ID:
                String playlistId = uri.getLastPathSegment();
                String localwhere = BaseColumns._ID + "=" + playlistId +
                    (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
                count = db.delete(TABLE_LOCAL_MUSIC, localwhere, selectionArgs);
                break;
            default:
                LogUtil.e(LogUtil.LOG_TAG, "MusicProvider.delete : uri does not match");
                break;
        }
        if (count > 0) {
            // If focused music does not exist by delete operation, update music status.
            if (!isExistFocusedMusic() && musicId != -1) {
                updateMusicStatus(musicId, MUSIC_STATUS_FOCUSED);
            }
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    /**
     * getType.
     * @param uri The URI to get type.
     * @return null.
     */
    @Override
    public String getType(Uri uri) {
        return null;
    }

    /**
     * Update play number to next play position.
     * @param id Music ID.
     * @return The number of rows affected.
     */
    private int updatePlayNumberToNext(String id) {
        // get specified music's play number
        String where = COLUMN_MUSIC_ID + " = " + id;
        int currentNum = getPlayNumber(where);
        LogUtil.d(LogUtil.LOG_TAG, "requested play number : " + currentNum);

        // get focused music's play number
        where = COLUMN_STATUS + " = " + MUSIC_STATUS_FOCUSED;
        int playingNum = getPlayNumber(where);
        LogUtil.d(LogUtil.LOG_TAG, "Playing play number : " + playingNum);

        ContentValues contentValues = new ContentValues();
        String sql;
        int count = 0;
        where = COLUMN_MUSIC_ID + " = " + id;
        if (currentNum == playingNum || (currentNum -1) == playingNum) {
            return 0;
        } else if (currentNum < playingNum) {
            sql = String.format(SQL_UPDATE_PLAY_NUMBER_LOWER, currentNum, playingNum);
            contentValues.put(COLUMN_PLAY_NUMBER, playingNum);
            count = playingNum - currentNum + 1;
        } else {
            sql = String.format(SQL_UPDATE_PLAY_NUMBER_UPPER, playingNum, currentNum);
            contentValues.put(COLUMN_PLAY_NUMBER, playingNum + 1);
            count = currentNum - playingNum;
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.execSQL(sql);
        db.update(TABLE_PLAYLIST, contentValues, where, null);
        return count;
    }

    /**
     * Get play number with specified condition.
     * @param where WHERE clause.
     * @return Play number of first row.
     */
    private int getPlayNumber(String where) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_PLAYLIST);

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] projectionIn = new String[]{COLUMN_PLAY_NUMBER};
        int num = -1;
        try {
            cursor = qb.query(db, projectionIn, where, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                num = cursor.getInt(cursor.getColumnIndex(COLUMN_PLAY_NUMBER));
            } else {
                LogUtil.d(LogUtil.LOG_TAG, "getPlayNumber where : " + where);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        LogUtil.d(LogUtil.LOG_TAG, "getPlayNumber : " + num);
        return num;
    }

    /**
     * If play number or status is not specified, set ContentValues to appropriate value.
     * @param values ContentValues.
     */
    private void adjustContentValuesIfNeeded(ContentValues values) {
        if (values.containsKey(COLUMN_PLAY_NUMBER) && values.containsKey(COLUMN_STATUS)) {
            LogUtil.d(LogUtil.LOG_TAG, "It is unnecessary to adjust ContentValues");
            return;
        }
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = null;

        int num = 0;
        try {
            cursor = db.rawQuery(SQL_QUERY_MAX_PLAY_NUMBER, null);
            if (cursor != null && cursor.moveToFirst()) {
                num = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        LogUtil.d(LogUtil.LOG_TAG, "max play num : " + num);

        // set play number to max play number + 1 if not specified play_number.
        if (!values.containsKey(COLUMN_PLAY_NUMBER)) {
            LogUtil.d(LogUtil.LOG_TAG, "adjustContentValuesIfNeeded set play_number : " + num + 1);
            values.put(COLUMN_PLAY_NUMBER, num + 1);
        }

        // set status if not specified status.
        if (!values.containsKey(COLUMN_STATUS)) {
            // set focused if first row, otherwise set idle status.
            if (num < 1) {
                LogUtil.d(LogUtil.LOG_TAG,
                    "adjustContentValuesIfNeeded set status : " + MUSIC_STATUS_FOCUSED);
                values.put(COLUMN_STATUS, MUSIC_STATUS_FOCUSED);
            } else {
                LogUtil.d(LogUtil.LOG_TAG,
                    "adjustContentValuesIfNeeded set status : " + MUSIC_STATUS_IDLE);
                values.put(COLUMN_STATUS, MUSIC_STATUS_IDLE);
            }
        }
    }

    /**
     * Get next focused music ID after delete.
     * @param whereClause WHERE clause for delete.
     * @param whereArgs If whereClause include "?s", it will be replaced
     *        by the values from whereArgs.
     * @return Next focused music ID.
     *         -1 is returned if music which has focus exists after delete or music is empty.
     *         If play num exists only larger than current play num, the music ID which has the
     *         largest play num is returned.
     *         If play num exists only smaller than current play num, the music ID which has the
     *         smallest play num is returned.
     */
    private int getNextFocusedMusicId(String whereClause, String[] whereArgs) {
        // Next focused music is same as current, if condition is nothing.
        if (whereClause == null || whereClause.isEmpty()) {
            LogUtil.d(LogUtil.LOG_TAG, "getNextFocusedMusicId - condition is nothing");
            return -1;
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_PLAYLIST);

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] projectionIn = new String[]{COLUMN_MUSIC_ID};
        String where = COLUMN_STATUS + " = " + MUSIC_STATUS_FOCUSED +
            (!TextUtils.isEmpty(whereClause) ? " AND NOT (" + whereClause + ")" : "");
        int musicId = -1;
        try {
            cursor = qb.query(db, projectionIn, where, whereArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                musicId = cursor.getInt(cursor.getColumnIndex(COLUMN_MUSIC_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (musicId != -1) {
            LogUtil.d(LogUtil.LOG_TAG, "getNextFocusedMusicId - current focus keep");
            return -1;
        }

        // get current focused music's play number
        where = COLUMN_STATUS + " = " + MUSIC_STATUS_FOCUSED;
        int currentNum = getPlayNumber(where);
        LogUtil.d(LogUtil.LOG_TAG, "current focused play number : " + currentNum);

        projectionIn = new String[]{COLUMN_MUSIC_ID, COLUMN_PLAY_NUMBER};
        where = COLUMN_STATUS + " != " + MUSIC_STATUS_FOCUSED +
            (!TextUtils.isEmpty(whereClause) ? " AND NOT (" + whereClause + ")" : "");
        String orderBy = COLUMN_PLAY_NUMBER + " ASC";
        try {
            cursor = qb.query(db, projectionIn, where, whereArgs, null, null, orderBy);
            if (cursor != null && cursor.moveToFirst()) {
                // Focus is assigned to next play num of current.
                // If play num larger than current is not exist, focus is assigned to smallest num.
                musicId = cursor.getInt(cursor.getColumnIndex(COLUMN_MUSIC_ID));
                do {
                    int playNum = cursor.getInt(cursor.getColumnIndex(COLUMN_PLAY_NUMBER));
                    if (playNum > currentNum) {
                        musicId = cursor.getInt(cursor.getColumnIndex(COLUMN_MUSIC_ID));
                        break;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        LogUtil.d(LogUtil.LOG_TAG, "getNextFocusedMusicId musicId : " + musicId);
        return musicId;
    }

    /**
     * Check whether focused music exists.
     * @return True focused music exists, otherwise false.
     */
    private boolean isExistFocusedMusic() {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_PLAYLIST);

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] projectionIn = new String[]{COLUMN_MUSIC_ID};
        String where = COLUMN_STATUS + " = " + MUSIC_STATUS_FOCUSED;
        int musicId = -1;
        try {
            cursor = qb.query(db, projectionIn, where, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                musicId = cursor.getInt(cursor.getColumnIndex(COLUMN_MUSIC_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        boolean ret = false;
        if (musicId != -1) {
            ret = true;
        }
        return ret;
    }

    /**
     * Update music status.
     * @param musicId Target music ID.
     * @param status Music status.
     */
    private void updateMusicStatus(int musicId, int status) {
        ContentValues contentValues = new ContentValues();
        int count = 0;
        String where = COLUMN_MUSIC_ID + " = " + musicId;
        contentValues.put(COLUMN_STATUS, status);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        count = db.update(TABLE_PLAYLIST, contentValues, where, null);
        LogUtil.d(LogUtil.LOG_TAG, "updateMusicStatus count : " + count);
        LogUtil.d(LogUtil.LOG_TAG,
            "updateMusicStatus musicId = " + musicId + " : status = " + status);
        return;
    }

    /**
     * bulk insert for playlist.
     * @param values An array of sets of column_name/value pairs to add to the database.
     * @return The number of values that were inserted.
     */
    private int bulkInsertForPlaylist(ContentValues[] values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int length = values.length;

        SQLiteStatement statement = db.compileStatement(SQL_BULK_INSERT_PLAY_LIST);

        LogUtil.v(LogUtil.LOG_TAG, "bulkInsertForPlaylist beginTransaction");
        db.beginTransaction();
        try {
            for (int cnt = 0; length > cnt; cnt++) {
                statement.bindString(1, values[cnt].getAsString(COLUMN_TITLE));
                statement.bindString(2, values[cnt].getAsString(COLUMN_ARTIST));
                statement.bindLong(3, values[cnt].getAsLong(COLUMN_TIME));
                statement.bindString(4, values[cnt].getAsString(COLUMN_MUSIC_URI));
                statement.bindString(5, values[cnt].getAsString(COLUMN_OWNER_ADDRESS));
                statement.bindString(6, values[cnt].getAsString(COLUMN_OWNER_NAME));
                statement.bindLong(7, values[cnt].getAsLong(COLUMN_STATUS));
                statement.bindLong(8, values[cnt].getAsLong(COLUMN_MUSIC_ID));
                statement.bindLong(9, values[cnt].getAsLong(COLUMN_PLAY_NUMBER));
                statement.executeInsert();
            }
            db.setTransactionSuccessful();
            LogUtil.v(LogUtil.LOG_TAG, "bulkInsertForPlaylist setTransactionSuccessful");
        } finally {
            LogUtil.v(LogUtil.LOG_TAG, "bulkInsertForPlaylist endTransaction");
            db.endTransaction();
        }
        return length;
    }
}
