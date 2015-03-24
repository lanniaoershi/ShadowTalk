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
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/**
 * PhotoProvider
 */
public class PhotoProvider extends ContentProvider implements BaseColumns {
    private static final String AUTHORITIES = "com.sonymobile.partyshare.photo";
    private static final String TABLE_PHOTO = PartyShareDatabaseHelper.TABLE_PHOTO;
    private static final String TABLE_LOCAL_PHOTO = PartyShareDatabaseHelper.TABLE_LOCAL_PHOTO;

    public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITIES + "/" + TABLE_PHOTO);
    public static final Uri CONTENT_URI_LOCAL_PHOTO =
            Uri.parse("content://" + AUTHORITIES + "/" + TABLE_LOCAL_PHOTO);

    // column for photo table
    public static final String COLUMN_MASTER_THUMBNAIL_PATH =
            PartyShareDatabaseHelper.COLUMN_MASTER_THUMBNAIL_PATH;
    public static final String COLUMN_MASTER_FILE_PATH =
            PartyShareDatabaseHelper.COLUMN_MASTER_FILE_PATH;
    public static final String COLUMN_SHARED_DATE = PartyShareDatabaseHelper.COLUMN_SHARED_DATE;
    public static final String COLUMN_TAKEN_DATE = PartyShareDatabaseHelper.COLUMN_TAKEN_DATE;
    public static final String COLUMN_OWNER_ADDRESS =
            PartyShareDatabaseHelper.COLUMN_OWNER_ADDRESS;
    public static final String COLUMN_LOCAL_THUMBNAIL_PATH =
            PartyShareDatabaseHelper.COLUMN_LOCAL_THUMBNAIL_PATH;
    public static final String COLUMN_LOCAL_FILE_PATH =
            PartyShareDatabaseHelper.COLUMN_LOCAL_FILE_PATH;
    public static final String COLUMN_MIME_TYPE = PartyShareDatabaseHelper.COLUMN_MIME_TYPE;
    public static final String COLUMN_DL_STATE = PartyShareDatabaseHelper.COLUMN_DL_STATE;

    // column for local_photo table
    public static final String COLUMN_QUERY_PATH = PartyShareDatabaseHelper.COLUMN_QUERY_PATH;
    public static final String COLUMN_THUMBNAIL_PATH =
            PartyShareDatabaseHelper.COLUMN_THUMBNAIL_PATH;
    public static final String COLUMN_FILE_PATH = PartyShareDatabaseHelper.COLUMN_FILE_PATH;

    private static final int URI_PHOTO = 1;
    private static final int URI_PHOTO_ID = 2;
    private static final int URI_LOCAL_PHOTO = 3;
    private static final int URI_LOCAL_PHOTO_ID = 4;
    private static final UriMatcher s_urlMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private PartyShareDatabaseHelper mOpenHelper;

    static {
        s_urlMatcher.addURI(AUTHORITIES, TABLE_PHOTO, URI_PHOTO);
        s_urlMatcher.addURI(AUTHORITIES, TABLE_PHOTO + "/#", URI_PHOTO_ID);
        s_urlMatcher.addURI(AUTHORITIES, TABLE_LOCAL_PHOTO, URI_LOCAL_PHOTO);
        s_urlMatcher.addURI(AUTHORITIES, TABLE_LOCAL_PHOTO + "/#", URI_LOCAL_PHOTO_ID);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = PartyShareDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String id = null;
        int match = s_urlMatcher.match(uri);
        switch (match) {
            case URI_PHOTO:
                qb.setTables(TABLE_PHOTO);
                break;
            case URI_PHOTO_ID:
                qb.setTables(TABLE_PHOTO);
                id = uri.getLastPathSegment();
                selection = BaseColumns._ID + "=" + id
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
                break;
            case URI_LOCAL_PHOTO:
                qb.setTables(TABLE_LOCAL_PHOTO);
                break;
            case URI_LOCAL_PHOTO_ID:
                qb.setTables(TABLE_LOCAL_PHOTO);
                id = uri.getLastPathSegment();
                selection = BaseColumns._ID + "=" + id
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
                break;
            default:
                return null;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri resultUri = null;
        long rowId = -1;

        int match = s_urlMatcher.match(uri);
        switch (match) {
            case URI_PHOTO:
                rowId = db.insert(TABLE_PHOTO, null, values);
                resultUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                break;
            case URI_LOCAL_PHOTO:
                rowId = db.insert(TABLE_LOCAL_PHOTO, null, values);
                resultUri = ContentUris.withAppendedId(CONTENT_URI_LOCAL_PHOTO, rowId);
                break;
            default:
                break;
        }
        if (rowId != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
        } else {
            resultUri = null;
        }

        return resultUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        String id;

        int match = s_urlMatcher.match(uri);
        switch (match) {
            case URI_PHOTO:
                count = db.update(TABLE_PHOTO, values, selection, selectionArgs);
                break;
            case URI_PHOTO_ID:
                id = uri.getLastPathSegment();
                count = db.update(TABLE_PHOTO, values, BaseColumns._ID
                        + "="
                        + id
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                                + ")" : ""), selectionArgs);
                break;
            case URI_LOCAL_PHOTO:
                count = db.update(TABLE_LOCAL_PHOTO, values, selection, selectionArgs);
                break;
            case URI_LOCAL_PHOTO_ID:
                id = uri.getLastPathSegment();
                count = db.update(TABLE_LOCAL_PHOTO, values, BaseColumns._ID
                        + "="
                        + id
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                                + ")" : ""), selectionArgs);
                break;
            default:
                break;
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        String id;

        int match = s_urlMatcher.match(uri);
        switch (match) {
            case URI_PHOTO:
                count = db.delete(TABLE_PHOTO, selection, selectionArgs);
                break;
            case URI_PHOTO_ID:
                id = uri.getLastPathSegment();
                count = db.delete(TABLE_PHOTO, BaseColumns._ID
                        + "="
                        + id
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                                + ")" : ""), selectionArgs);
                break;
            case URI_LOCAL_PHOTO:
                count = db.delete(TABLE_LOCAL_PHOTO, selection, selectionArgs);
                break;
            case URI_LOCAL_PHOTO_ID:
                id = uri.getLastPathSegment();
                count = db.delete(TABLE_LOCAL_PHOTO, BaseColumns._ID
                        + "="
                        + id
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                                + ")" : ""), selectionArgs);
                break;
            default:
                break;
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}
