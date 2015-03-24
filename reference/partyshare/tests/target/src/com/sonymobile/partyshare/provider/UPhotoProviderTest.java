/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.test.AndroidTestCase;

import com.sonymobile.partyshare.testutils.MockContext;
import com.sonymobile.partyshare.testutils.MockContentResolverProviderTest;

/**
 * This test is PhotoProvider class
 */
public class UPhotoProviderTest extends AndroidTestCase {
    private static final String TEST_BASE_MASTER_THUMBNAIL_PATH = "testMasterThumbnailPath";
    private static final String TEST_BASE_MASTER_FILE_PATH = "testMasterFilePath";
    private static final String TEST_BASE_SHARED_DATE = "testSharedDate";
    private static final String TEST_BASE_TAKEN_DATE = "testTakenDate";
    private static final String TEST_BASE_OWNER_ADDRESS = "testOwnerAddress";
    private static final String TEST_BASE_LOCAL_THUMBNAIL_PATH = "testLocalThumbnailPath";
    private static final String TEST_BASE_LOCAL_FILE_PATH = "testLocalFilePath";
    private static final String TEST_BASE_MIME_TYPE = "testMimeType";

    private static final String TEST_BASE_QUERY_PATH = "testQueryPath";
    private static final String TEST_BASE_THUMBNAIL_PATH = "testThumbnailPath";
    private static final String TEST_BASE_FILE_PATH = "testFilePath";

    private MockContext mContext;
    private MockContentResolverProviderTest mContentResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = new MockContext(getContext());
        mContentResolver = new MockContentResolverProviderTest(mContext);
        mContext.setContentResolver(mContentResolver);
        setContext(mContext);

        PartyShareDatabaseHelper openHelper = PartyShareDatabaseHelper.getInstance(getContext());
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.delete(PartyShareDatabaseHelper.TABLE_PHOTO, null, null);
        db.delete(PartyShareDatabaseHelper.TABLE_LOCAL_PHOTO, null, null);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        PartyShareDatabaseHelper openHelper = PartyShareDatabaseHelper.getInstance(getContext());
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.delete(PartyShareDatabaseHelper.TABLE_PHOTO, null, null);
        db.delete(PartyShareDatabaseHelper.TABLE_LOCAL_PHOTO, null, null);
    }

    public void testInsert_Photo() {
        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH, TEST_BASE_MASTER_THUMBNAIL_PATH);
        values.put(PhotoProvider.COLUMN_MASTER_FILE_PATH, TEST_BASE_MASTER_FILE_PATH);
        values.put(PhotoProvider.COLUMN_SHARED_DATE, TEST_BASE_SHARED_DATE);
        values.put(PhotoProvider.COLUMN_TAKEN_DATE, TEST_BASE_TAKEN_DATE);
        values.put(PhotoProvider.COLUMN_OWNER_ADDRESS, TEST_BASE_OWNER_ADDRESS);
        values.put(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH, TEST_BASE_LOCAL_THUMBNAIL_PATH);
        values.put(PhotoProvider.COLUMN_LOCAL_FILE_PATH, TEST_BASE_LOCAL_FILE_PATH);
        values.put(PhotoProvider.COLUMN_MIME_TYPE, TEST_BASE_MIME_TYPE);
        values.put(PhotoProvider.COLUMN_DL_STATE, 0);

        Uri resultUri = mContentResolver.insert(PhotoProvider.CONTENT_URI, values);
        assertNotNull("Uri should be get", resultUri);

        
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(resultUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Wrong data is inserted.", TEST_BASE_MASTER_THUMBNAIL_PATH,
                        cursor.getString(cursor.getColumnIndex(
                                PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(PhotoProvider.CONTENT_URI));
    }

    public void testInsert_LocalPhoto() {
        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_QUERY_PATH, TEST_BASE_QUERY_PATH);
        values.put(PhotoProvider.COLUMN_THUMBNAIL_PATH, TEST_BASE_THUMBNAIL_PATH);
        values.put(PhotoProvider.COLUMN_FILE_PATH, TEST_BASE_FILE_PATH);

        Uri resultUri = mContentResolver.insert(PhotoProvider.CONTENT_URI_LOCAL_PHOTO, values);
        assertNotNull("Uri should be get", resultUri);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(resultUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Wrong data is inserted.", TEST_BASE_QUERY_PATH,
                        cursor.getString(cursor.getColumnIndex(PhotoProvider.COLUMN_QUERY_PATH)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(PhotoProvider.CONTENT_URI_LOCAL_PHOTO));
    }

    public void testInsert_ErrorUri() {
        ContentValues values = new ContentValues();
        values.put("DummyColumn", "testPath");
        Uri dummyUri = Uri.withAppendedPath(PhotoProvider.CONTENT_URI, "dummy");
        Uri resultUri = mContentResolver.insert(dummyUri, values);
        assertNull("Uri should be get", resultUri);

        Cursor cursor = null;
        cursor = mContentResolver.query(dummyUri, null, null, null, null);
        assertNull("Data should be empty", cursor);

        assertFalse("NotifyChanged should not be called",
                mContentResolver.isNotifyChange(dummyUri));
    }

    public void testUpdate_Photo() {
        String targetId = preTestInsertPhoto(1);
        preTestInsertPhoto(2);

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH, "testUpdatePath");
        String selection = BaseColumns._ID + " = ?";
        String[] args = new String[] { targetId };
        int count = mContentResolver.update(PhotoProvider.CONTENT_URI, values, selection, args);
        assertEquals("Update count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(PhotoProvider.CONTENT_URI, null, selection, args, null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Wrong data is updated.",  "testUpdatePath",
                        cursor.getString(cursor.getColumnIndex(
                                PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH)));
            } else {
                fail("Data is not found.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(PhotoProvider.CONTENT_URI));
    }

    public void testUpdate_PhotoId() {
        String targetId = preTestInsertPhoto(1);
        preTestInsertPhoto(2);

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH, "testUpdatePath");
        Uri uri = Uri.withAppendedPath(PhotoProvider.CONTENT_URI, targetId);
        int count = mContentResolver.update(uri, values, null, null);
        assertEquals("Update count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Wrong data is updated.",  "testUpdatePath",
                        cursor.getString(cursor.getColumnIndex(
                                PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH)));
            } else {
                fail("Data is not found.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called", mContentResolver.isNotifyChange(uri));
    }

    public void testUpdate_LocalPhoto() {
        String targetId = preTestInsertLocalPhoto(1);
        preTestInsertLocalPhoto(2);

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_QUERY_PATH, "testUpdatePath");
        String selection = BaseColumns._ID + " = ?";
        String[] args = new String[] { targetId };
        int count = mContentResolver.update(PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                values, selection, args);
        assertEquals("Update count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                    null, selection, args, null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Wrong data is updated.",  "testUpdatePath",
                        cursor.getString(cursor.getColumnIndex(PhotoProvider.COLUMN_QUERY_PATH)));
            } else {
                fail("Data is not found.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(PhotoProvider.CONTENT_URI_LOCAL_PHOTO));
    }

    public void testUpdate_LocalPhotoId() {
        String targetId = preTestInsertLocalPhoto(1);
        preTestInsertLocalPhoto(2);

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_QUERY_PATH, "testUpdatePath");
        Uri uri = Uri.withAppendedPath(PhotoProvider.CONTENT_URI_LOCAL_PHOTO, targetId);
        int count = mContentResolver.update(uri, values, null, null);
        assertEquals("Update count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Wrong data is updated.",  "testUpdatePath",
                        cursor.getString(cursor.getColumnIndex(PhotoProvider.COLUMN_QUERY_PATH)));
            } else {
                fail("Data is not found.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called", mContentResolver.isNotifyChange(uri));
    }

    public void testUpdate_ErrorUri() {
        ContentValues values = new ContentValues();
        values.put("DummyColumn", "testPath");
        Uri dummyUri = Uri.withAppendedPath(PhotoProvider.CONTENT_URI_LOCAL_PHOTO, "dummy");
        int count = mContentResolver.update(dummyUri, values, null, null);
        assertEquals("Update count should be 0", 0, count);

        Cursor cursor = null;
        cursor = mContentResolver.query(dummyUri, null, null, null, null);
        assertNull("Data should be empty", cursor);
        assertFalse("NotifyChanged should not be called",
                mContentResolver.isNotifyChange(dummyUri));
    }

    public void testDelete_Photo() {
        String targetId = preTestInsertPhoto(1);
        preTestInsertPhoto(2);

        String selection = BaseColumns._ID + " = ?";
        String[] args = new String[] { targetId };
        int count = mContentResolver.delete(PhotoProvider.CONTENT_URI, selection, args);
        assertEquals("Delete count should be 1", 1, count);


        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(PhotoProvider.CONTENT_URI,
                    null, selection, args, null);
            if (cursor != null && cursor.moveToFirst()) {
                fail("Data is not deleted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(PhotoProvider.CONTENT_URI));
    }

    public void testDelete_PhotoId() {
        String targetId = preTestInsertPhoto(1);
        preTestInsertPhoto(2);

        Uri uri = Uri.withAppendedPath(PhotoProvider.CONTENT_URI, targetId);
        int count = mContentResolver.delete(uri, null, null);
        assertEquals("Delete count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                fail("Data is not deleted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called", mContentResolver.isNotifyChange(uri));
    }

    public void testDelete_LocalPhoto() {
        String targetId = preTestInsertLocalPhoto(1);
        preTestInsertLocalPhoto(2);

        String selection = BaseColumns._ID + " = ?";
        String[] args = new String[] { targetId };
        int count = mContentResolver.delete(PhotoProvider.CONTENT_URI_LOCAL_PHOTO, selection, args);
        assertEquals("Delete count should be 1", 1, count);


        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                    null, selection, args, null);
            if (cursor != null && cursor.moveToFirst()) {
                fail("Data is not deleted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(PhotoProvider.CONTENT_URI_LOCAL_PHOTO));
    }

    public void testDelete_LocalPhotoId() {
        String targetId = preTestInsertLocalPhoto(1);
        preTestInsertLocalPhoto(2);

        Uri uri = Uri.withAppendedPath(PhotoProvider.CONTENT_URI_LOCAL_PHOTO, targetId);
        int count = mContentResolver.delete(uri, null, null);
        assertEquals("Delete count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                fail("Data is not deleted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called", mContentResolver.isNotifyChange(uri));
    }

    public void testDelete_ErrorUri() {
        Uri dummyUri = Uri.withAppendedPath(PhotoProvider.CONTENT_URI, "dummy");
        int count = mContentResolver.delete(dummyUri, null, null);
        assertEquals("Update count should be 0", 0, count);

        Cursor cursor = null;
        cursor = mContentResolver.query(dummyUri, null, null, null, null);
        assertNull("Data should be empty", cursor);
        assertFalse("NotifyChanged should not be called",
                mContentResolver.isNotifyChange(dummyUri));
    }

    private String preTestInsertPhoto(int id) {
        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH,
                TEST_BASE_MASTER_THUMBNAIL_PATH + id);
        values.put(PhotoProvider.COLUMN_MASTER_FILE_PATH, TEST_BASE_MASTER_FILE_PATH + id);
        values.put(PhotoProvider.COLUMN_SHARED_DATE, TEST_BASE_SHARED_DATE + id);
        values.put(PhotoProvider.COLUMN_TAKEN_DATE, TEST_BASE_TAKEN_DATE + id);
        values.put(PhotoProvider.COLUMN_OWNER_ADDRESS, TEST_BASE_OWNER_ADDRESS + id);
        values.put(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH, TEST_BASE_LOCAL_THUMBNAIL_PATH + id);
        values.put(PhotoProvider.COLUMN_LOCAL_FILE_PATH, TEST_BASE_LOCAL_FILE_PATH + id);
        values.put(PhotoProvider.COLUMN_MIME_TYPE, TEST_BASE_MIME_TYPE + id);
        values.put(PhotoProvider.COLUMN_DL_STATE, 0);
        Uri uri = mContentResolver.insert(PhotoProvider.CONTENT_URI, values);
        mContentResolver.clearNotifyChangeHistory();
        return uri.getLastPathSegment();
    }

    private String preTestInsertLocalPhoto(int id) {
        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_QUERY_PATH, TEST_BASE_QUERY_PATH + id);
        values.put(PhotoProvider.COLUMN_THUMBNAIL_PATH, TEST_BASE_THUMBNAIL_PATH + id);
        values.put(PhotoProvider.COLUMN_FILE_PATH, TEST_BASE_FILE_PATH + id);
        Uri uri = mContentResolver.insert(PhotoProvider.CONTENT_URI_LOCAL_PHOTO, values);
        mContentResolver.clearNotifyChangeHistory();
        return uri.getLastPathSegment();
    }
}
