/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */

package com.sonymobile.partyshare.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.sonymobile.partyshare.util.LogUtil;

/**
 * Provider utility.
 * Catch the Exception that occurs in ContentProvider.
 */
public class ProviderUtil {

    public static Cursor query(Context context, Uri uri, String[] projectionIn,
            String selection, String[] selectionArgs, String sort) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projectionIn,
                    selection, selectionArgs, sort);
        } catch (Exception e) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed query, occur Exception : " + e);
        }
        return cursor;
    }

    public static Uri insert(Context context, Uri uri, ContentValues values) {
        Uri resultUri = null;
        try {
            resultUri = context.getContentResolver().insert(uri, values);
        } catch (Exception e) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed insert, occur Exception : " + e);
        }
        return resultUri;
    }

    public static int bulkInsert(Context context, Uri uri, ContentValues[] values) {
        int count = 0;
        try {
            count = context.getContentResolver().bulkInsert(uri, values);
        } catch (Exception e) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed bulkInsert, occur Exception : " + e);
        }
        return count;
    }

    public static int update(Context context, Uri uri, ContentValues values,
            String selection, String[] selectionArgs) {
        int count = 0;
        try {
            if (selection == null) {
                count = context.getContentResolver().update(uri, values, "", null);
            } else {
                count = context.getContentResolver().update(uri, values, selection, selectionArgs);
            }
        } catch (Exception e) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed update, occur Exception : " + e);
        }
        return count;
    }

    public static int delete(Context context, Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        try {
            if (selection == null) {
                count = context.getContentResolver().delete(uri, "", null);
            } else {
                count = context.getContentResolver().delete(uri, selection, selectionArgs);
            }
        } catch (Exception e) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed delete, occur Exception : " + e);
        }
        return count;
    }

    public static int getListSize(Context context, Uri uri) {
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = context.getContentResolver().query(
                    uri, new String[] { BaseColumns._ID },
                    null, null, null);
            if (cursor != null) {
                count = cursor.getCount();
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }
}
