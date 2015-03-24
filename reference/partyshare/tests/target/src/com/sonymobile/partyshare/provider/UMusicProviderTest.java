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
 * This test is MusicProvider class
 */
public class UMusicProviderTest extends AndroidTestCase {
    private static final String TEST_BASE_TITLE =  "testTitle";
    private static final String TEST_BASE_ARTIST =  "testArtist";
    private static final String TEST_BASE_MUSIC_URI =  "testUri";
    private static final String TEST_BASE_OWNER_ADDRESS =  "testOwner";
    private static final String TEST_BASE_MUSIC_LOCAL_PATH = "testPath";

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
        db.delete(PartyShareDatabaseHelper.TABLE_PLAYLIST, null, null);
        db.delete(PartyShareDatabaseHelper.TABLE_LOCAL_MUSIC, null, null);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        PartyShareDatabaseHelper openHelper = PartyShareDatabaseHelper.getInstance(getContext());
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.delete(PartyShareDatabaseHelper.TABLE_PLAYLIST, null, null);
        db.delete(PartyShareDatabaseHelper.TABLE_LOCAL_MUSIC, null, null);
    }

    public void testInsert_PlayList_first() {
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_TITLE, "testTitle");
        values.put(MusicProvider.COLUMN_ARTIST, TEST_BASE_ARTIST);
        values.put(MusicProvider.COLUMN_TIME, 100);
        values.put(MusicProvider.COLUMN_MUSIC_URI, TEST_BASE_MUSIC_URI);
        values.put(MusicProvider.COLUMN_OWNER_ADDRESS, TEST_BASE_OWNER_ADDRESS);
        Uri resultUri = mContentResolver.insert(MusicProvider.CONTENT_URI, values);
        assertNotNull("Uri should be get", resultUri);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null, null, null, MusicProvider.COLUMN_PLAY_NUMBER);
            if (cursor != null && cursor.moveToFirst()) {
                if (cursor.getString(cursor.getColumnIndex(MusicProvider.COLUMN_TITLE))
                        .equals("testTitle")) {
                    assertEquals("MusicID should be 1", 1,
                            cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID)));
                    assertEquals("MusicOrder should be 1", 1,
                            cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER)));
                    assertEquals("MusicFocus should be 1", MusicProvider.MUSIC_STATUS_FOCUSED,
                            cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_STATUS)));
                } else {
                    fail("Wrong data is inserted.");
                }
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(MusicProvider.CONTENT_URI));
    }

    public void testInsert_PlayList_second() {
        preTestInsertPlayList(1);

        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_TITLE, "testTitle2");
        values.put(MusicProvider.COLUMN_ARTIST, TEST_BASE_ARTIST);
        values.put(MusicProvider.COLUMN_TIME, 100);
        values.put(MusicProvider.COLUMN_MUSIC_URI, TEST_BASE_MUSIC_URI);
        values.put(MusicProvider.COLUMN_OWNER_ADDRESS, TEST_BASE_OWNER_ADDRESS);
        Uri resultUri = mContentResolver.insert(MusicProvider.CONTENT_URI, values);
        assertNotNull("Uri should be get", resultUri);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null, null, null, MusicProvider.COLUMN_PLAY_NUMBER);
            if (cursor != null && cursor.moveToFirst() && cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndex(MusicProvider.COLUMN_TITLE))
                        .equals("testTitle2")) {
                    assertEquals("MusicID should be 2", 2,
                            cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID)));
                    assertEquals("MusicOrder should be 2", 2,
                            cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER)));
                    assertEquals("MusicFocus should be 0", MusicProvider.MUSIC_STATUS_IDLE,
                            cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_STATUS)));
                } else {
                    fail("Wrong data is inserted.");
                }
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(MusicProvider.CONTENT_URI));
    }

    public void testInsert_LocalMusic() {
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_MUSIC_LOCAL_PATH, "testPath");
        Uri resultUri = mContentResolver.insert(MusicProvider.CONTENT_URI_LOCAL_MUSIC, values);
        assertNotNull("Uri should be get", resultUri);
        int id = getLocalMusicId(1);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                    null,
                    BaseColumns._ID + " = ?",
                    new String[] { String.valueOf(id)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("LocalMusic table should be inserted", "testPath",
                        cursor.getString(cursor.getColumnIndex(
                                MusicProvider.COLUMN_MUSIC_LOCAL_PATH)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(MusicProvider.CONTENT_URI_LOCAL_MUSIC));
    }

    public void testInsert_ErrorUri() {
        ContentValues values = new ContentValues();
        values.put("DummyColumn", "testPath");
        Uri dummyUri = Uri.withAppendedPath(MusicProvider.CONTENT_URI, "dummy");
        Uri resultUri = mContentResolver.insert(dummyUri, values);
        assertNull("Uri should be get", resultUri);

        Cursor cursor = null;
        cursor = mContentResolver.query(dummyUri, null, null, null, null);
        assertNull("Data should be empty", cursor);

        assertFalse("NotifyChanged should not be called",
                mContentResolver.isNotifyChange(dummyUri));
    }

    public void testUpdate_PlayList() {
        preTestInsertPlayList(1);
        int musicId = getCurrentTrack();

        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_TITLE, "testUpdateTitle");
        String selection = MusicProvider.COLUMN_MUSIC_ID + " = ?";
        String[] args = new String[] { String.valueOf(musicId)};
        int count = mContentResolver.update(MusicProvider.CONTENT_URI, values, selection, args);
        assertEquals("Update count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null, selection, args, MusicProvider.COLUMN_PLAY_NUMBER);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("MusicID should be 1", "testUpdateTitle",
                        cursor.getString(cursor.getColumnIndex(MusicProvider.COLUMN_TITLE)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(MusicProvider.CONTENT_URI));
    }

    public void testUpdate_PlayListId() {
        preTestInsertPlayList(1);
        preTestInsertPlayList(2);
        int currentId = getCurrentTrack();
        int nextId = getNextTrack(currentId, true);

        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_STATUS, MusicProvider.MUSIC_STATUS_FOCUSED);
        Uri uri = Uri.withAppendedPath(MusicProvider.CONTENT_URI, String.valueOf(nextId));
        int count =  mContentResolver.update(uri, values, null, null);
        assertEquals("Update count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(nextId)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Status should be changed 0 to 1", MusicProvider.MUSIC_STATUS_FOCUSED,
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_STATUS)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(currentId)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Status should be changed 1 to 0", MusicProvider.MUSIC_STATUS_IDLE,
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_STATUS)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called", mContentResolver.isNotifyChange(uri));
    }

    public void testUpdate_PlayListPlayNext_Forward() {
        preTestInsertPlayList(1);
        preTestInsertPlayList(2);
        preTestInsertPlayList(3);
        preTestInsertPlayList(4);
        int currentId = getCurrentTrack();
        int currentOrder = getOrder(currentId);
        int nextId = getNextTrack(currentId, true);
        int nextOrder = currentOrder + 1;
        int setId = getNextTrack(nextId, true);

        Uri uri = Uri.withAppendedPath(MusicProvider.CONTENT_URI_PLAY_NEXT, String.valueOf(setId));
        int count = mContentResolver.update(uri, null, null, null);
        assertEquals("Update count should be 2", 2, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_PLAY_NUMBER + " = ?",
                    new String[] { String.valueOf(nextOrder)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Next track should be changed to " + setId, setId,
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(nextId)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                assertFalse("Order should be changed from " + nextOrder, nextOrder ==
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called", mContentResolver.isNotifyChange(uri));
    }

    public void testUpdate_PlayListPlayNext_Rewind() {
        preTestInsertPlayList(1);
        preTestInsertPlayList(2);
        preTestInsertPlayList(3);
        preTestInsertPlayList(4);
        int preCurrentId = getCurrentTrack();
        int preNextId1 = getNextTrack(preCurrentId, true);
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_STATUS, MusicProvider.MUSIC_STATUS_FOCUSED);
        Uri uriPlayListId1 = Uri.withAppendedPath(MusicProvider.CONTENT_URI,
                String.valueOf(preNextId1));
        mContentResolver.update(uriPlayListId1, values, null, null);
        int preNextId2 = getNextTrack(preNextId1, true);
        Uri uriPlayListId2 = Uri.withAppendedPath(MusicProvider.CONTENT_URI,
                String.valueOf(preNextId2));
        mContentResolver.update(uriPlayListId2, values, null, null);
        mContentResolver.clearNotifyChangeHistory();

        int currentId = getCurrentTrack();
        int currentOrder = getOrder(currentId);
        int nextId = getNextTrack(currentId, false);
        int setId = getNextTrack(nextId, false);

        Uri uri = Uri.withAppendedPath(MusicProvider.CONTENT_URI_PLAY_NEXT, String.valueOf(setId));
        int count = mContentResolver.update(uri, null, null, null);
        assertEquals("Update count should be 3", 3, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_PLAY_NUMBER + " = ?",
                    new String[] { String.valueOf(currentOrder)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("Next track should be changed to " + setId, setId,
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(currentId)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                assertFalse("Order should be changed from " + currentOrder, currentOrder ==
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(nextId)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                assertFalse("Order should be changed from " + (currentOrder - 1),
                        (currentOrder - 1) ==
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called", mContentResolver.isNotifyChange(uri));
    }

    public void testUpdate_PlayListPlayNext_NoUpdata() {
        preTestInsertPlayList(1);
        preTestInsertPlayList(2);
        int currentId = getCurrentTrack();
        int currentOrder = getOrder(currentId);
        int nextId = getNextTrack(currentId, true);

        Uri uri = Uri.withAppendedPath(MusicProvider.CONTENT_URI_PLAY_NEXT, String.valueOf(nextId));
        int count = mContentResolver.update(uri, null, null, null);
        assertEquals("Update count should be 0", 0, count);
        assertFalse("NotifyChanged should not be called", mContentResolver.isNotifyChange(uri));
    }

    public void testUpdate_LocalMusic() {
        preTestInsertLocalMusic(1);
        preTestInsertLocalMusic(2);
        int id = getLocalMusicId(1);

        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_MUSIC_LOCAL_PATH, "testUpdatePath");
        String selection = BaseColumns._ID + " = ?";
        String[] args = new String[] { String.valueOf(id)};

        int count = mContentResolver.update(MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                values, selection, args);
        assertEquals("Update count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                    null, selection, args, null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("LocalMusic table should be inserted", "testUpdatePath",
                        cursor.getString(cursor.getColumnIndex(
                                MusicProvider.COLUMN_MUSIC_LOCAL_PATH)));
            } else {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(MusicProvider.CONTENT_URI_LOCAL_MUSIC));
    }

    public void testUpdate_LocalMusicId() {
        preTestInsertLocalMusic(1);
        preTestInsertLocalMusic(2);
        int id = getLocalMusicId(1);

        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_MUSIC_LOCAL_PATH, "testUpdatePath");
        Uri uri = Uri.withAppendedPath(MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                String.valueOf(id));
        int count = mContentResolver.update(uri, values, null, null);
        assertEquals("Update count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                    null,
                    BaseColumns._ID + " = ?",
                    new String[] { String.valueOf(id)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("LocalMusic table should be inserted", "testUpdatePath",
                        cursor.getString(cursor.getColumnIndex(
                                MusicProvider.COLUMN_MUSIC_LOCAL_PATH)));
            } else {
                fail("Data is not inserted.");
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
        Uri dummyUri = Uri.withAppendedPath(MusicProvider.CONTENT_URI, "dummy");
        int count = mContentResolver.update(dummyUri, values, null, null);
        assertEquals("Update count should be 0", 0, count);

        Cursor cursor = null;
        cursor = mContentResolver.query(dummyUri, null, null, null, null);
        assertNull("Data should be empty", cursor);
        assertFalse("NotifyChanged should not be called",
                mContentResolver.isNotifyChange(dummyUri));
    }

    public void testDelete_PlayList() {
        preTestInsertPlayList(1);
        int currentId = getCurrentTrack();

        String selection = MusicProvider.COLUMN_MUSIC_ID + " = ?";
        String[] args = new String[] { String.valueOf(currentId)};
        int count = mContentResolver.delete(MusicProvider.CONTENT_URI, selection, args);
        assertEquals("Delete count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null, selection, args, MusicProvider.COLUMN_PLAY_NUMBER);
            if (cursor != null && cursor.moveToFirst()) {
                fail("Data is not deleted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(MusicProvider.CONTENT_URI));
    }

    public void testDelete_PlayList_MoveFocus() {
        preTestInsertPlayList(1);
        preTestInsertPlayList(2);
        int currentId = getCurrentTrack();
        int nextId = getNextTrack(currentId, true);

        String selection = MusicProvider.COLUMN_MUSIC_ID + " = ?";
        String[] args = new String[] { String.valueOf(currentId)};
        int count = mContentResolver.delete(MusicProvider.CONTENT_URI, selection, args);
        assertEquals("Delete count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null, selection, args, MusicProvider.COLUMN_PLAY_NUMBER);
            if (cursor != null && cursor.moveToFirst()) {
                fail("Data is not deleted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    selection,
                    new String[] { String.valueOf(nextId)},
                    MusicProvider.COLUMN_PLAY_NUMBER);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("MusicFocus should be 1", MusicProvider.MUSIC_STATUS_FOCUSED,
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_STATUS)));
                assertEquals("MusicOrder should be 1", 1,
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER)));
            } else {
                fail("Data is not exsisted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(MusicProvider.CONTENT_URI));
    }

    public void testDelete_PlayList_NotMoveFocus() {
        preTestInsertPlayList(1);
        preTestInsertPlayList(2);
        int currentId = getCurrentTrack();
        int nextId = getNextTrack(currentId, true);

        String selection = MusicProvider.COLUMN_MUSIC_ID + " = ?";
        String[] args = new String[] { String.valueOf(nextId)};
        int count = mContentResolver.delete(MusicProvider.CONTENT_URI, selection, args);
        assertEquals("Delete count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null, selection, args, MusicProvider.COLUMN_PLAY_NUMBER);
            if (cursor != null && cursor.moveToFirst()) {
                fail("Data is not deleted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    selection,
                    new String[] { String.valueOf(currentId)},
                    MusicProvider.COLUMN_PLAY_NUMBER);
            if (cursor != null && cursor.moveToFirst()) {
                assertEquals("MusicFocus should be 1", MusicProvider.MUSIC_STATUS_FOCUSED,
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_STATUS)));
                assertEquals("MusicOrder should be 1", 1,
                        cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER)));
            } else {
                fail("Data is not exsisted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(MusicProvider.CONTENT_URI));
    }

    public void testDelete_PlayListId() {
        preTestInsertPlayList(1);
        preTestInsertPlayList(2);
        preTestInsertPlayList(3);
        int currentId = getCurrentTrack();
        int nextId = getNextTrack(currentId, true);
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_STATUS, MusicProvider.MUSIC_STATUS_FOCUSED);
        Uri uriPlayListId1 = Uri.withAppendedPath(MusicProvider.CONTENT_URI,
                String.valueOf(nextId));
        mContentResolver.update(uriPlayListId1, values, null, null);
        mContentResolver.clearNotifyChangeHistory();

        currentId = getCurrentTrack();
        Uri uri = Uri.withAppendedPath(MusicProvider.CONTENT_URI,
                String.valueOf(currentId));
        int count = mContentResolver.delete(uri, null, null);
        assertEquals("Delete count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(currentId)},
                    MusicProvider.COLUMN_PLAY_NUMBER);
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

    public void testDelete_LocalMusic() {
        preTestInsertLocalMusic(1);
        preTestInsertLocalMusic(2);
        int id = getLocalMusicId(1);

        String selection = BaseColumns._ID + " = ?";
        String[] args = new String[] { String.valueOf(id)};
        int count = mContentResolver.delete(MusicProvider.CONTENT_URI_LOCAL_MUSIC, selection, args);
        assertEquals("Delete count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                    null,
                    BaseColumns._ID + " = ?",
                    new String[] { String.valueOf(id)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called",
                mContentResolver.isNotifyChange(MusicProvider.CONTENT_URI_LOCAL_MUSIC));
    }

    public void testDelete_LocalMusicId() {
        preTestInsertLocalMusic(1);
        preTestInsertLocalMusic(2);
        int id = getLocalMusicId(1);

        Uri uri = Uri.withAppendedPath(MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                String.valueOf(id));
        int count = mContentResolver.delete(uri, null, null);
        assertEquals("Delete count should be 1", 1, count);

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                    null,
                    BaseColumns._ID + " = ?",
                    new String[] { String.valueOf(id)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                fail("Data is not inserted.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        assertTrue("NotifyChanged should be called", mContentResolver.isNotifyChange(uri));
    }

    public void testDelete_ErrorUri() {
        Uri dummyUri = Uri.withAppendedPath(MusicProvider.CONTENT_URI, "dummy");
        int count = mContentResolver.delete(dummyUri, null, null);
        assertEquals("Should not be deleted", 0, count);
        assertFalse("NotifyChanged should not be called",
                mContentResolver.isNotifyChange(dummyUri));
    }

    private void preTestInsertPlayList(int id) {
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_TITLE, TEST_BASE_TITLE + id);
        values.put(MusicProvider.COLUMN_ARTIST, TEST_BASE_ARTIST + id);
        values.put(MusicProvider.COLUMN_TIME, 100);
        values.put(MusicProvider.COLUMN_MUSIC_URI, TEST_BASE_MUSIC_URI + id);
        values.put(MusicProvider.COLUMN_OWNER_ADDRESS, TEST_BASE_OWNER_ADDRESS + id);
        mContentResolver.insert(MusicProvider.CONTENT_URI, values);
        mContentResolver.clearNotifyChangeHistory();
    }

    private void preTestInsertLocalMusic(int id) {
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_MUSIC_LOCAL_PATH, TEST_BASE_MUSIC_LOCAL_PATH + id);
        mContentResolver.insert(MusicProvider.CONTENT_URI_LOCAL_MUSIC, values);
        mContentResolver.clearNotifyChangeHistory();
    }

    private int getCurrentTrack() {
        Cursor cursor = null;
        int trackId = 0;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_STATUS + " = " + MusicProvider.MUSIC_STATUS_FOCUSED,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                trackId = cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID));
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return trackId;
    }

    private int getOrder(int id) {
        Cursor cursor = null;
        int currentOrder = 0;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(id)},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                currentOrder = cursor.getInt(cursor.getColumnIndex(
                        MusicProvider.COLUMN_PLAY_NUMBER));
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return currentOrder;
    }

    private int getNextTrack(int currentId, boolean isForward) {
        int nextId = 0;
        int currentOrder = getOrder(currentId);

        String nextWhere;
        String nextSort;
        String[] args = new String[] { String.valueOf(currentOrder)};
        if (isForward) {
            nextWhere = MusicProvider.COLUMN_PLAY_NUMBER + " > ?";
            nextSort = MusicProvider.COLUMN_PLAY_NUMBER;
        } else {
            nextWhere = MusicProvider.COLUMN_PLAY_NUMBER + " < ?";
            nextSort = MusicProvider.COLUMN_PLAY_NUMBER + " DESC";
        }

        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                    null,
                    nextWhere,
                    args,
                    nextSort);
            if (cursor != null && cursor.moveToFirst()) {
                nextId = cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID));
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        // Return first or last music.
        if (nextId == 0) {
            try {
                cursor = mContentResolver.query(MusicProvider.CONTENT_URI,
                        null,
                        null,
                        null,
                        nextSort);
                if (cursor != null && cursor.moveToFirst()) {
                    nextId = cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID));
                }
            } catch (Exception e) {
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return nextId;
    }

    private int getLocalMusicId(int number) {
        Cursor cursor = null;
        int id = 0;
        int postion = 1;
        try {
            cursor = mContentResolver.query(MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    if (postion != number) {
                        postion++;
                    } else {
                        id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }
}
