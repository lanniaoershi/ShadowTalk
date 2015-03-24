/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.test.InstrumentationTestCase;

import com.sonymobile.partyshare.httpd.JsonUtil;
import com.sonymobile.partyshare.httpd.MusicJsonFile;
import com.sonymobile.partyshare.httpd.NanoHTTPD.Response;
import com.sonymobile.partyshare.httpd.NanoHTTPD.Response.Status;
import com.sonymobile.partyshare.httpd.PartyShareCommand;
import com.sonymobile.partyshare.httpd.PartyShareHttpd;
import com.sonymobile.partyshare.httpd.NanoHTTPD.Method;
import com.sonymobile.partyshare.httpd.PhotoJsonFile;
import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.service.MusicService;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.DeviceInfo;
import com.sonymobile.partyshare.testutils.MockContext;
import com.sonymobile.partyshare.testutils.MockHttpSession;
import com.sonymobile.partyshare.util.PhotoUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This unit test is for Setup class.
 */
public class UPartyShareHttpd extends InstrumentationTestCase {
    private static final String TEST_DATA_PATH = "/data/data/com.sonymobile.partyshare/tests";
    private static final String PHOTO_TEST_DATA = TEST_DATA_PATH + "/imageThumb.jpg";
    private static final String MUSIC_TEST_DATA = TEST_DATA_PATH + "/music.mp3";

    private static final String TEST_OWNER = "aa:bb:cc:dd:ee:ff";

    private static final String TEST_MUSIC_TITLE = "test_title";
    private static final String TEST_MUSIC_ARTIST = "test_artist";
    private static final String TEST_MUSIC_TIME = "12345";
    private static final String TEST_MUSIC_URL = "/music/1";
    private static final String TEST_MUSIC_OWNER_ADDRESS = TEST_OWNER;
    private static final String TEST_MUSIC_OWNER_NAME = "Test User";
    private static final String TEST_MUSIC_ID = "1";
    private static final String TEST_MUSIC_STATUS = "1";
    private static final String TEST_MUSIC_PLAY_NUMBER = "1";

    private static final String TEST_PHOTO_MASTER_THUMBNAILE = "/host/thumbnail/1";
    private static final String TEST_PHOTO_MASTER_FILE = "/file/1";
    private static final String TEST_PHOTO_SHARED_DATE = "22222";
    private static final String TEST_PHOTO_TAKEN_DATE = "11111";
    private static final String TEST_PHOTO_OWNER = TEST_OWNER;
    private static final String TEST_PHOTO_LOCAL_THUMBNAIL =
            "/data/data/com.sonymobile.partyshare/files/thumbnail/imageThumb.jpg";
    private static final String TEST_PHOTO_LOCAL_FILE =
            "/data/data/com.sonymobile.partyshare/files/image.jpg";
    private static final String TEST_PHOTO_MIMETYPE = "image/jpeg";
    private static final int TEST_PHOTO_DL_STATE = PhotoUtils.DOWNLOAD_STATE_BEFORE;
    private static final String TEST_PHOTO_THUMBNAIL_FILENAME = "imageThumb.jpg";

    private static final String TEST_LOCAL_MUSIC_PATH = TEST_DATA_PATH + MUSIC_TEST_DATA;
    private static final String TEST_LOCAL_MUSIC_MIMETYPE = "audio/mpeg";

    private static final String TEST_LOCAL_PHOTO_QUERY_PATH = TEST_OWNER + ":/file/1";
    private static final String TEST_LOCAL_PHOTO_THUMBNAIL = TEST_PHOTO_LOCAL_THUMBNAIL;
    private static final String TEST_LOCAL_PHOTO_FILE = TEST_PHOTO_LOCAL_FILE;

    private static final String PKG_MUSIC_SERVICE = "com.sonymobile.partyshare";
    private static final String CLS_MUSIC_SERVICE = PKG_MUSIC_SERVICE + ".service.MusicService";

    /** Context. */
    private Context mContext;
    private Context mTargetContext;
    private MockContext mMockContext;

    private boolean mOriginalOwner;
    private String mOriginalOwnerAddress;

    private PartyShareHttpd mHttpd;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        mTargetContext = getInstrumentation().getTargetContext();
        mMockContext = new MockContext(mTargetContext);
        mOriginalOwner = ConnectionManager.isGroupOwner();
        mOriginalOwnerAddress = ConnectionManager.getGroupOwnerAddress();

        ConnectionManager.setGroupOwnerAddress("localhost");
        mHttpd = new PartyShareHttpd(mMockContext, 51000, "localhost");
        mHttpd.start();

        backupFile();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ConnectionManager.setIsGroupOwner(mOriginalOwner);
        ConnectionManager.setGroupOwnerAddress(mOriginalOwnerAddress);

        if (mHttpd != null) {
            mHttpd.stop();
        }

        restoreFile();
    }

    /**
     * Test for add music to host.
     */
    public void testAddMusic() {
        ConnectionManager.setIsGroupOwner(true);

        HashMap<String, String> param = new HashMap<String, String>();
        param.put(PartyShareCommand.PARAM_CMD,
                String.valueOf(PartyShareCommand.CMD_ADD_MUSIC_CONTENT));
        param.put(PartyShareCommand.PARAM_MUSIC_TITLE, TEST_MUSIC_TITLE);
        param.put(PartyShareCommand.PARAM_MUSIC_ARTIST, TEST_MUSIC_ARTIST);
        param.put(PartyShareCommand.PARAM_MUSIC_TIME, TEST_MUSIC_TIME);
        param.put(PartyShareCommand.PARAM_MUSIC_URL, TEST_MUSIC_URL);
        param.put(PartyShareCommand.PARAM_MUSIC_OWNER_ADDRESS, TEST_MUSIC_OWNER_ADDRESS);
        param.put(PartyShareCommand.PARAM_MUSIC_OWNER_NAME, TEST_MUSIC_OWNER_NAME);

        MockHttpSession session = new MockHttpSession();
        session.setMethod(Method.POST);
        session.setUri("/");
        session.setParams(param);
        Response response = mHttpd.serve(session);

        try {
            Field field = MusicPlayListController.class.getDeclaredField("mThread");
            field.setAccessible(true);
            Thread thread = (Thread)field.get(MusicPlayListController.class);
            field = MusicPlayListController.class.getDeclaredField("mThreadStart");
            field.setAccessible(true);
            boolean threadStart = (Boolean)field.get(MusicPlayListController.class);
            if (threadStart) {
                // Wait until thread is finished.
                thread.join(10000);
            }
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(Status.OK, response.getStatus());
        assertEquals(PartyShareHttpd.MIME_HTML, response.getMimeType());

        Intent intent = mMockContext.getServiceIntent();
        assertNotNull(intent);
        assertEquals(PKG_MUSIC_SERVICE, intent.getComponent().getPackageName());
        assertEquals(CLS_MUSIC_SERVICE, intent.getComponent().getClassName());
        assertEquals(MusicService.ACTION_ADD, intent.getAction());

        assertEquals(TEST_MUSIC_TITLE,
                intent.getStringExtra(MusicProvider.COLUMN_TITLE));
        assertEquals(TEST_MUSIC_ARTIST,
                intent.getStringExtra(MusicProvider.COLUMN_ARTIST));
        assertEquals(TEST_MUSIC_TIME,
                String.valueOf(intent.getIntExtra(MusicProvider.COLUMN_TIME, -1)));
        assertEquals(TEST_MUSIC_URL,
                intent.getStringExtra(MusicProvider.COLUMN_MUSIC_URI));
        assertEquals(TEST_MUSIC_OWNER_ADDRESS,
                intent.getStringExtra(MusicProvider.COLUMN_OWNER_ADDRESS));
        assertEquals(TEST_MUSIC_OWNER_NAME,
                intent.getStringExtra(MusicProvider.COLUMN_OWNER_NAME));
    }

    public void testRemoveMusic() {
        ConnectionManager.setIsGroupOwner(true);
        Uri uri = insertMusicTestData();

        try {
            String musicId = uri.getLastPathSegment();
            HashMap<String, String> param = new HashMap<String, String>();
            param.put(PartyShareCommand.PARAM_CMD,
                    String.valueOf(PartyShareCommand.CMD_REMOVE_MUSIC_CONTENT));
            param.put(PartyShareCommand.PARAM_MUSIC_ID, musicId);

            MockHttpSession session = new MockHttpSession();
            session.setMethod(Method.POST);
            session.setUri("/");
            session.setParams(param);
            Response response = mHttpd.serve(session);

            assertEquals(Status.OK, response.getStatus());
            assertEquals(PartyShareHttpd.MIME_HTML, response.getMimeType());

            Intent intent = mMockContext.getServiceIntent();
            assertNotNull(intent);
            assertEquals(PKG_MUSIC_SERVICE, intent.getComponent().getPackageName());
            assertEquals(CLS_MUSIC_SERVICE, intent.getComponent().getClassName());
            assertEquals(MusicService.ACTION_REMOVE, intent.getAction());
            assertEquals(musicId, String.valueOf(intent.getIntExtra("delete_id", -1)));
        } catch (Exception e) {
            fail(e.toString());
        } finally {
            deleteTestData(uri);
        }
    }

    /**
     * Test for play next.
     */
    public void testPlayNext() {
        ConnectionManager.setIsGroupOwner(true);
        Uri uri = insertMusicTestData();

        try {
            String musicId = uri.getLastPathSegment();
            HashMap<String, String> param = new HashMap<String, String>();
            param.put(PartyShareCommand.PARAM_CMD,
                    String.valueOf(PartyShareCommand.CMD_PLAY_NEXT));
            param.put(PartyShareCommand.PARAM_MUSIC_ID, musicId);

            MockHttpSession session = new MockHttpSession();
            session.setMethod(Method.POST);
            session.setUri("/");
            session.setParams(param);
            Response response = mHttpd.serve(session);

            assertEquals(Status.OK, response.getStatus());
            assertEquals(PartyShareHttpd.MIME_HTML, response.getMimeType());

            Intent intent = mMockContext.getServiceIntent();
            assertNotNull(intent);
            assertEquals(PKG_MUSIC_SERVICE, intent.getComponent().getPackageName());
            assertEquals(CLS_MUSIC_SERVICE, intent.getComponent().getClassName());
            assertEquals(MusicService.ACTION_PLAY_NEXT, intent.getAction());
            assertEquals(musicId, String.valueOf(intent.getIntExtra("play_next", -1)));
        } catch (Exception e) {
            fail(e.toString());
        } finally {
            deleteTestData(uri);
        }
    }

    /**
     * Test for add photo to host.
     */
    public void testAddPhoto() {
        ConnectionManager.setIsGroupOwner(true);
        File photo = createTestFile(PHOTO_TEST_DATA, "image.jpg");

        DeviceInfo info = new DeviceInfo(
                "Test User", "aa:bb:cc:dd:ee:ff", "ff:ff:ff:ff:ff:ff", true);
        info.setIpAddress("localhost");
        List<DeviceInfo> list = new ArrayList<DeviceInfo>();
        list.add(info);

        try {
            ConnectionManager manager = ConnectionManager.getInstance(mTargetContext);
            Field field = ConnectionManager.class.getDeclaredField("mGroupList");
            field.setAccessible(true);
            field.set(manager, list);
        } catch (Exception e) {
            fail(e.toString());
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(PartyShareCommand.PARAM_CMD,
                String.valueOf(PartyShareCommand.CMD_ADD_PHOTO_CONTENT));
        map.put(PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH, TEST_PHOTO_MASTER_THUMBNAILE);
        map.put(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH, TEST_PHOTO_MASTER_FILE);
        map.put(PartyShareCommand.PARAM_PHOTO_SHARED_DATE, TEST_PHOTO_SHARED_DATE);
        map.put(PartyShareCommand.PARAM_PHOTO_TAKEN_DATE, TEST_PHOTO_TAKEN_DATE);
        map.put(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS, TEST_PHOTO_OWNER);
        map.put(PartyShareCommand.PARAM_PHOTO_MIME_TYPE, TEST_PHOTO_MIMETYPE);
        map.put(PartyShareCommand.PARAM_PHOTO_THUMBNAIL_FILENAME, TEST_PHOTO_THUMBNAIL_FILENAME);

        MockHttpSession session = new MockHttpSession();
        session.setMethod(Method.POST);
        session.setUri("/");
        session.setParams(map);
        Response response = mHttpd.serve(session);

        try {
            Field field = PhotoListController.class.getDeclaredField("mThread");
            field.setAccessible(true);
            Thread thread = (Thread)field.get(PhotoListController.class);
            field = PhotoListController.class.getDeclaredField("mThreadStart");
            field.setAccessible(true);
            boolean threadStart = (Boolean)field.get(PhotoListController.class);
            if (threadStart) {
                thread.join(10000);
            }
        } catch (Exception e) {
            fail(e.toString());
        }

        assertEquals(Status.OK, response.getStatus());
        assertEquals(PartyShareHttpd.MIME_HTML, response.getMimeType());

        Cursor c1 = mTargetContext.getContentResolver().query(
                PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                null,
                PhotoProvider.COLUMN_QUERY_PATH + " = ?",
                new String[] { TEST_LOCAL_PHOTO_QUERY_PATH },
                null);
        assertNotNull(c1);
        assertTrue(c1.moveToFirst());

        assertEquals(TEST_LOCAL_PHOTO_THUMBNAIL,
                c1.getString(c1.getColumnIndex(PhotoProvider.COLUMN_THUMBNAIL_PATH)));

        Cursor c2 = mTargetContext.getContentResolver().query(
                PhotoProvider.CONTENT_URI,
                null,
                PhotoProvider.COLUMN_OWNER_ADDRESS + " = ? AND " +
                PhotoProvider.COLUMN_MASTER_FILE_PATH + " = ?",
                new String[] { TEST_PHOTO_OWNER, TEST_PHOTO_MASTER_FILE },
                null);
        assertNotNull(c2);
        assertTrue(c2.moveToFirst());

        String masterThumbnailPath =
                "/host/thumbnail/" + c2.getString(c2.getColumnIndex(PhotoProvider._ID));
        assertEquals(masterThumbnailPath,
                c2.getString(c2.getColumnIndex(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH)));
        assertEquals(TEST_PHOTO_MASTER_FILE,
                c2.getString(c2.getColumnIndex(PhotoProvider.COLUMN_MASTER_FILE_PATH)));
        assertEquals(TEST_PHOTO_SHARED_DATE,
                c2.getString(c2.getColumnIndex(PhotoProvider.COLUMN_SHARED_DATE)));
        assertEquals(TEST_PHOTO_TAKEN_DATE,
                c2.getString(c2.getColumnIndex(PhotoProvider.COLUMN_TAKEN_DATE)));
        assertEquals(TEST_PHOTO_OWNER,
                c2.getString(c2.getColumnIndex(PhotoProvider.COLUMN_OWNER_ADDRESS)));
        assertEquals(TEST_PHOTO_LOCAL_THUMBNAIL,
                c2.getString(c2.getColumnIndex(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH)));
        assertEquals("",
                c2.getString(c2.getColumnIndex(PhotoProvider.COLUMN_LOCAL_FILE_PATH)));
        assertEquals(TEST_PHOTO_MIMETYPE,
                c2.getString(c2.getColumnIndex(PhotoProvider.COLUMN_MIME_TYPE)));
        assertEquals(TEST_PHOTO_DL_STATE,
                c2.getInt(c2.getColumnIndex(PhotoProvider.COLUMN_DL_STATE)));

        File jsonFile = new File(mTargetContext.getFilesDir(), PhotoJsonFile.FILENAME);
        JSONObject object = JsonUtil.loadJsonFile(jsonFile);
        try {
            JSONArray array = object.getJSONArray(PhotoJsonFile.KEY_JSON_ARRAY);
            object = array.getJSONObject(0);
            assertEquals(masterThumbnailPath,
                    object.getString(PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH));
            assertEquals(TEST_PHOTO_MASTER_FILE,
                    object.getString(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH));
            assertEquals(TEST_PHOTO_SHARED_DATE,
                    object.getString(PartyShareCommand.PARAM_PHOTO_SHARED_DATE));
            assertEquals(TEST_PHOTO_TAKEN_DATE,
                    object.getString(PartyShareCommand.PARAM_PHOTO_TAKEN_DATE));
            assertEquals(TEST_PHOTO_OWNER,
                    object.getString(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS));
            assertEquals(TEST_PHOTO_MIMETYPE,
                    object.getString(PartyShareCommand.PARAM_PHOTO_MIME_TYPE));
            assertEquals(TEST_PHOTO_THUMBNAIL_FILENAME,
                    object.getString(PartyShareCommand.PARAM_PHOTO_THUMBNAIL_FILENAME));
        } catch (JSONException e) {
            fail(e.toString());
        }

        Uri uri1 = Uri.withAppendedPath(PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                c1.getString(c1.getColumnIndex(PhotoProvider._ID)));
        deleteTestData(uri1);

        Uri uri2 = Uri.withAppendedPath(PhotoProvider.CONTENT_URI,
                c2.getString(c2.getColumnIndex(PhotoProvider._ID)));
        deleteTestData(uri2);

        c1.close();
        c2.close();

        deleteTestFile(photo);
    }

    /**
     * Test remove photo from host.
     */
    public void testRemovePhoto() {
        ConnectionManager.setIsGroupOwner(true);
        Uri photo = insertPhotoTestData();
        Uri localPhoto = insertLocalPhotoTestData();

        DeviceInfo info = new DeviceInfo(
                "Test User", "aa:bb:cc:dd:ee:ff", "ff:ff:ff:ff:ff:ff", true);
        info.setIpAddress("localhost");
        List<DeviceInfo> list = new ArrayList<DeviceInfo>();
        list.add(info);
        try {
            ConnectionManager manager = ConnectionManager.getInstance(mTargetContext);
            Field field = ConnectionManager.class.getDeclaredField("mGroupList");
            field.setAccessible(true);
            field.set(manager, list);
        } catch (Exception e) {
            fail(e.toString());
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(PartyShareCommand.PARAM_CMD,
                String.valueOf(PartyShareCommand.CMD_REMOVE_PHOTO_CONTENT));
        map.put(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH, TEST_PHOTO_MASTER_FILE);
        map.put(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS, TEST_PHOTO_OWNER);

        MockHttpSession session = new MockHttpSession();
        session.setMethod(Method.POST);
        session.setUri("/");
        session.setParams(map);
        Response response = mHttpd.serve(session);

        boolean existJsonFile = true;
        long endTime = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() <= endTime) {
            existJsonFile = checkJsonContent(JsonUtil.CONTENT_TYPE_PHOTO,
                    TEST_PHOTO_OWNER, TEST_PHOTO_MASTER_FILE);
            if (!existJsonFile) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                fail(e.toString());
            }
        }

        assertFalse(existJsonFile);

        assertEquals(Status.OK, response.getStatus());
        assertEquals(PartyShareHttpd.MIME_HTML, response.getMimeType());

        File thumbFile = new File(TEST_PHOTO_LOCAL_THUMBNAIL);
        assertFalse(thumbFile.exists());

        Cursor c1 = mTargetContext.getContentResolver().query(photo, null, null, null, null);
        assertNotNull(c1);
        assertFalse(c1.moveToFirst());
        assertEquals(0, c1.getCount());

        Cursor c2 = mTargetContext.getContentResolver().query(localPhoto, null, null, null, null);
        assertNotNull(c2);
        assertFalse(c2.moveToFirst());
        assertEquals(0, c2.getCount());

        c1.close();
        c2.close();
    }

    /**
     * Test remove client data.
     */
    public void testDeleteClientData() {
        ConnectionManager.setIsGroupOwner(true);
        Uri music = insertMusicTestData();
        Uri photo = insertPhotoTestData();
        createJsonFile(MusicJsonFile.FILENAME);
        createJsonFile(PhotoJsonFile.FILENAME);

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(PartyShareCommand.PARAM_CMD,
                String.valueOf(PartyShareCommand.CMD_DELETE_CLIENT_DATA));
        map.put(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS, TEST_OWNER);

        MockHttpSession session = new MockHttpSession();
        session.setMethod(Method.POST);
        session.setUri("/");
        session.setParams(map);
        Response response = mHttpd.serve(session);

        assertEquals(Status.OK, response.getStatus());
        assertEquals(PartyShareHttpd.MIME_HTML, response.getMimeType());

        try {
            Field field = MusicJsonFile.class.getDeclaredField("mThread");
            field.setAccessible(true);
            Thread thread = (Thread)field.get(MusicJsonFile.class);
            field = MusicJsonFile.class.getDeclaredField("mThreadStart");
            field.setAccessible(true);
            boolean threadStart = (Boolean)field.get(MusicJsonFile.class);
            if (threadStart) {
                // Wait until thread is finished.
                thread.join(10000);
            }
        } catch (Exception e) {
            fail(e.toString());
        }

        int musicCnt = mTargetContext.getContentResolver().delete(
                MusicProvider.CONTENT_URI,
                MusicProvider.COLUMN_OWNER_ADDRESS + " = ?",
                new String[] { TEST_MUSIC_OWNER_ADDRESS });
        assertEquals(0, musicCnt);

        int photoCnt = mTargetContext.getContentResolver().delete(
                PhotoProvider.CONTENT_URI,
                PhotoProvider.COLUMN_OWNER_ADDRESS + " = ?",
                new String[] { TEST_PHOTO_OWNER });
        assertEquals(0, photoCnt);

        try {
            File musicJsonFile = new File(mTargetContext.getFilesDir(), MusicJsonFile.FILENAME);
            JSONObject musicObject = JsonUtil.loadJsonFile(musicJsonFile);
            JSONArray musicArray = musicObject.getJSONArray(MusicJsonFile.KEY_JSON_ARRAY);
            assertEquals(0, musicArray.length());

            File photoJsonFile = new File(mTargetContext.getFilesDir(), PhotoJsonFile.FILENAME);
            JSONObject photoObject = JsonUtil.loadJsonFile(photoJsonFile);
            JSONArray photoArray = photoObject.getJSONArray(PhotoJsonFile.KEY_JSON_ARRAY);
            assertEquals(0, photoArray.length());
        } catch (JSONException e) {
            fail(e.toString());
        }

        deleteTestData(music);
        deleteTestData(photo);
        deleteJsonFile(MusicJsonFile.FILENAME);
        deleteJsonFile(PhotoJsonFile.FILENAME);
    }

    /**
     * Test for getting json file of playlist.
     */
    public void testGetJson_playlist() {
        ConnectionManager.setIsGroupOwner(true);
        createJsonFile(MusicJsonFile.FILENAME);

        MockHttpSession session = new MockHttpSession();
        session.setMethod(Method.GET);
        session.setUri(mTargetContext.getFilesDir() + "/" + MusicJsonFile.FILENAME);

        Response response = mHttpd.serve(session);

        assertEquals(Status.OK, response.getStatus());
        assertEquals(PartyShareHttpd.MIME_PLAINTEXT, response.getMimeType());

        InputStream is = response.getData();
        assertNotNull(is);

        try {
            StringBuffer jsonData = new StringBuffer();
            byte[] buf = new byte[1024];
            int len = 0;
            while((len = is.read(buf)) > 0) {
                jsonData.append(new String(buf, 0, len, Charset.forName("UTF-8")));
            }
            is.close();

            JSONObject json = new JSONObject(jsonData.toString());
            JSONArray array = json.getJSONArray(MusicJsonFile.KEY_JSON_ARRAY);
            json = array.getJSONObject(0);
            assertEquals(TEST_MUSIC_TITLE,
                    json.getString(PartyShareCommand.PARAM_MUSIC_TITLE));
            assertEquals(TEST_MUSIC_ARTIST,
                    json.getString(PartyShareCommand.PARAM_MUSIC_ARTIST));
            assertEquals(TEST_MUSIC_TIME,
                    json.getString(PartyShareCommand.PARAM_MUSIC_TIME));
            assertEquals(TEST_MUSIC_URL,
                    json.getString(PartyShareCommand.PARAM_MUSIC_URL));
            assertEquals(TEST_MUSIC_OWNER_ADDRESS,
                    json.getString(PartyShareCommand.PARAM_MUSIC_OWNER_ADDRESS));
            assertEquals(TEST_MUSIC_OWNER_NAME,
                    json.getString(PartyShareCommand.PARAM_MUSIC_OWNER_NAME));
            assertEquals(TEST_MUSIC_ID,
                    json.getString(PartyShareCommand.PARAM_MUSIC_ID));
            assertEquals(TEST_MUSIC_STATUS,
                    json.getString(PartyShareCommand.PARAM_MUSIC_STATUS));
            assertEquals(TEST_MUSIC_PLAY_NUMBER,
                    json.getString(PartyShareCommand.PARAM_MUSIC_PLAY_NUMBER));
        } catch (Exception e) {
            fail(e.toString());
        }

        deleteJsonFile(MusicJsonFile.FILENAME);
    }

    /**
     * Test for getting json file of photolist.
     */
    public void testGetJson_photolist() {
        ConnectionManager.setIsGroupOwner(true);
        createJsonFile(PhotoJsonFile.FILENAME);

        MockHttpSession session = new MockHttpSession();
        session.setMethod(Method.GET);
        session.setUri(mTargetContext.getFilesDir() + "/" + PhotoJsonFile.FILENAME);

        Response response = mHttpd.serve(session);

        assertEquals(Status.OK, response.getStatus());
        assertEquals(PartyShareHttpd.MIME_PLAINTEXT, response.getMimeType());

        InputStream is = response.getData();
        assertNotNull(is);

        try {
            StringBuffer jsonData = new StringBuffer();
            byte[] buf = new byte[1024];
            int len = 0;
            while((len = is.read(buf)) > 0) {
                jsonData.append(new String(buf, 0, len, Charset.forName("UTF-8")));
            }
            is.close();

            JSONObject json = new JSONObject(jsonData.toString());
            JSONArray array = json.getJSONArray(PhotoJsonFile.KEY_JSON_ARRAY);
            json = array.getJSONObject(0);
            assertEquals(TEST_PHOTO_LOCAL_FILE,
                    json.getString(PartyShareCommand.PARAM_PHOTO_LOCAL_FILE_PATH));
            assertEquals(TEST_PHOTO_LOCAL_THUMBNAIL,
                    json.getString(PartyShareCommand.PARAM_PHOTO_LOCAL_THUMBNAIL_PATH));
            assertEquals(TEST_PHOTO_OWNER,
                    json.getString(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS));
            assertEquals(TEST_PHOTO_MIMETYPE,
                    json.getString(PartyShareCommand.PARAM_PHOTO_MIME_TYPE));
            assertEquals(TEST_PHOTO_THUMBNAIL_FILENAME,
                    json.getString(PartyShareCommand.PARAM_PHOTO_THUMBNAIL_FILENAME));
            assertEquals(TEST_PHOTO_MASTER_THUMBNAILE,
                    json.getString(PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH));
            assertEquals(TEST_PHOTO_MASTER_FILE,
                    json.getString(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH));
            assertEquals(TEST_PHOTO_TAKEN_DATE,
                    json.getString(PartyShareCommand.PARAM_PHOTO_TAKEN_DATE));
            assertEquals(TEST_PHOTO_SHARED_DATE,
                    json.getString(PartyShareCommand.PARAM_PHOTO_SHARED_DATE));
        } catch (Exception e) {
            fail(e.toString());
        }

        deleteJsonFile(PhotoJsonFile.FILENAME);
    }

    /**
     * Test for getting music file.
     */
    public void testGetMusic() {
        ConnectionManager.setIsGroupOwner(true);
        File testFile = createTestFile(MUSIC_TEST_DATA, "music.mp3");

        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_MUSIC_LOCAL_PATH, MUSIC_TEST_DATA);
        Uri uri = mTargetContext.getContentResolver().insert(
                MusicProvider.CONTENT_URI_LOCAL_MUSIC, values);

        MockHttpSession session = new MockHttpSession();
        Map<String, String> header = new HashMap<String, String>();
        header.put("Header", "Test");
        session.setHeaders(header);
        session.setMethod(Method.GET);
        session.setUri("/music/" + uri.getLastPathSegment());

        Response response = mHttpd.serve(session);

        assertEquals(Status.OK, response.getStatus());

        File respFile = new File(TEST_DATA_PATH, "music2.mp3");

        try {
            BufferedInputStream bis = new BufferedInputStream(response.getData());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(respFile));
            byte[] b = new byte[1024];
            int readByte = 0;
            while (-1 != (readByte = bis.read(b))) {
                bos.write(b, 0, readByte);
            }
            bis.close();
            bos.close();

            BufferedInputStream expectImg = new BufferedInputStream(new FileInputStream(testFile));
            BufferedInputStream actualImg = new BufferedInputStream(new FileInputStream(respFile));
            assertTrue(compareInputStream(expectImg, actualImg));
        } catch (Exception e) {
            fail(e.toString());
        }

        respFile.delete();
        testFile.delete();
        deleteTestData(uri);
    }

    /**
     * Test for getting music file with range.
     */
    public void testGetMusicWithRange() {
        ConnectionManager.setIsGroupOwner(true);
        File testFile = createTestFile(MUSIC_TEST_DATA, "music.mp3");
        String etag = Integer.toHexString((testFile.getAbsolutePath() + testFile.lastModified()
                + "" + testFile.length()).hashCode());

        try {
            Field field = PartyShareHttpd.class.getDeclaredField("mEtag");
            field.setAccessible(true);
            field.set(mHttpd, etag);
        } catch (Exception e) {
            fail(e.toString());
        }

        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_MUSIC_LOCAL_PATH, MUSIC_TEST_DATA);
        Uri uri = mTargetContext.getContentResolver().insert(
                MusicProvider.CONTENT_URI_LOCAL_MUSIC, values);

        MockHttpSession session = new MockHttpSession();
        Map<String, String> header = new HashMap<String, String>();
        header.put("Header", "Test");
        header.put("range", "bytes=0-1000");
        session.setHeaders(header);
        session.setMethod(Method.GET);
        session.setUri("/music/" + uri.getLastPathSegment());

        Response response = mHttpd.serve(session);
        assertEquals(Status.PARTIAL_CONTENT, response.getStatus());
        assertEquals("1001", response.getHeader("Content-Length"));
        assertEquals("bytes 0-1000/" + testFile.length(), response.getHeader("Content-Range"));
        assertEquals(etag, response.getHeader("ETag"));

        testFile.delete();
        deleteTestData(uri);
    }

    /**
     * Test for getting thumbnail file.
     */
    public void testGetThumbnail() {
        ConnectionManager.setIsGroupOwner(true);
        File testFile = createTestFile(PHOTO_TEST_DATA, "image.jpg");

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH, PHOTO_TEST_DATA);
        Uri uri = mTargetContext.getContentResolver().insert(
                PhotoProvider.CONTENT_URI, values);

        MockHttpSession session = new MockHttpSession();
        Map<String, String> header = new HashMap<String, String>();
        header.put("Header", "Test");
        session.setHeaders(header);
        session.setMethod(Method.GET);
        session.setUri("/thumbnail/" + uri.getLastPathSegment());

        Response response = mHttpd.serve(session);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("image/jpeg", response.getMimeType());

        File respFile = new File(mTargetContext.getFilesDir() + "/image2.jpg");

        try {
            BufferedInputStream bis = new BufferedInputStream(response.getData());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(respFile));
            byte[] b = new byte[1024];
            int readByte = 0;
            while (-1 != (readByte = bis.read(b))) {
                bos.write(b, 0, readByte);
            }
            bis.close();
            bos.close();

            BufferedInputStream expectImg = new BufferedInputStream(new FileInputStream(testFile));
            BufferedInputStream actualImg = new BufferedInputStream(new FileInputStream(respFile));
            assertTrue(compareInputStream(expectImg, actualImg));
        } catch (Exception e) {
            fail(e.toString());
        }

        respFile.delete();
        testFile.delete();
        deleteTestData(uri);
    }

    /**
     * Test for getting photo file.
     */
    public void testGetPhotoFile() {
        ConnectionManager.setIsGroupOwner(true);
        File testFile = createTestFile(PHOTO_TEST_DATA, "image.jpg");

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_FILE_PATH, PHOTO_TEST_DATA);
        Uri uri = mTargetContext.getContentResolver().insert(
                PhotoProvider.CONTENT_URI_LOCAL_PHOTO, values);

        MockHttpSession session = new MockHttpSession();
        Map<String, String> header = new HashMap<String, String>();
        header.put("Header", "Test");
        session.setHeaders(header);
        session.setMethod(Method.GET);
        session.setUri("/file/" + uri.getLastPathSegment());

        Response response = mHttpd.serve(session);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("image/jpeg", response.getMimeType());

        File respFile = new File(mTargetContext.getFilesDir() + "/image2.jpg");

        try {
            BufferedInputStream bis = new BufferedInputStream(response.getData());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(respFile));
            byte[] b = new byte[1024];
            int readByte = 0;
            while (-1 != (readByte = bis.read(b))) {
                bos.write(b, 0, readByte);
            }
            bis.close();
            bos.close();

            InputStream expectImg = new FileInputStream(testFile);
            InputStream actualImg = new FileInputStream(respFile);
            assertTrue(compareInputStream(expectImg, actualImg));
        } catch (Exception e) {
            fail(e.toString());
        }

        respFile.delete();
        testFile.delete();
        deleteTestData(uri);
    }

    /**
     * Test for getting photo file which does not exist.
     */
    public void testGetPhotoFile_NotExist() {
        ConnectionManager.setIsGroupOwner(true);
        ConnectionManager.setLocalAddress("localhost");

        DeviceInfo info = new DeviceInfo(
                "Test User", "aa:bb:cc:dd:ee:ff", "ff:ff:ff:ff:ff:ff", true);
        info.setIpAddress("localhost");
        List<DeviceInfo> list = new ArrayList<DeviceInfo>();
        list.add(info);

        try {
            ConnectionManager manager = ConnectionManager.getInstance(mTargetContext);
            Field field = ConnectionManager.class.getDeclaredField("mGroupList");
            field.setAccessible(true);
            field.set(manager, list);
        } catch (Exception e) {
            fail(e.toString());
        }

        File testFile = new File(TEST_LOCAL_PHOTO_FILE);
        if (testFile.exists()) {
            testFile.delete();
        }

        Uri uri = insertPhotoTestData();
        Uri localUri = insertLocalPhotoTestData();
        String localId = localUri.getLastPathSegment();

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_MASTER_FILE_PATH, "/file/" + localId);
        mTargetContext.getContentResolver().update(uri, values, null, null);

        MockHttpSession session = new MockHttpSession();
        Map<String, String> header = new HashMap<String, String>();
        header.put("Header", "Test");
        session.setHeaders(header);
        session.setMethod(Method.GET);
        session.setUri("/file/" + localId);

        Response response = mHttpd.serve(session);

        assertEquals(Status.NOT_FOUND, response.getStatus());
        assertEquals("text/plain", response.getMimeType());

        deleteTestData(uri);
        deleteTestData(localUri);
    }

    /**
     * Test for getting photo size.
     */
    public void testGetPhotoSize() {
        ConnectionManager.setIsGroupOwner(true);
        File testFile = createTestFile(PHOTO_TEST_DATA, "image.jpg");

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_FILE_PATH, PHOTO_TEST_DATA);
        Uri uri = mTargetContext.getContentResolver().insert(
                PhotoProvider.CONTENT_URI_LOCAL_PHOTO, values);

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(PartyShareEvent.PARAM_EVENT_CODE,
                String.valueOf(PartyShareEvent.EVENT_GET_FILESIZE));

        MockHttpSession session = new MockHttpSession();
        Map<String, String> header = new HashMap<String, String>();
        header.put("Header", "Test");
        session.setHeaders(header);
        session.setMethod(Method.GET);
        session.setParams(map);
        session.setUri("/file/" + uri.getLastPathSegment());

        Response response = mHttpd.serve(session);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("text/plain", response.getMimeType());

        try {
            BufferedInputStream bis = new BufferedInputStream(response.getData());
            byte[] b = new byte[1024];
            int length = bis.read(b);
            long filesize = Long.parseLong(new String(b, 0, length, "UTF-8"));

            assertEquals(testFile.length(), filesize);

            bis.close();
        } catch (Exception e) {
            fail(e.toString());
        }

        testFile.delete();
        deleteTestData(uri);
    }

    /**
     * Test remove local music.
     */
    public void testRemoveLocalMusic() {
        ConnectionManager.setIsGroupOwner(false);
        Uri uri = insertLocalMusicTestData();
        String musicId = uri.getLastPathSegment();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(PartyShareCommand.PARAM_CMD,
                String.valueOf(PartyShareCommand.CMD_CLIENT_RECV_EVENT));
        map.put(PartyShareEvent.PARAM_EVENT_CODE,
                String.valueOf(PartyShareEvent.EVENT_REMOVE_LOCAL_MUSIC));
        map.put(PartyShareCommand.PARAM_MUSIC_LOCAL_PATH, "/music/" + musicId);

        MockHttpSession session = new MockHttpSession();
        session.setMethod(Method.GET);
        session.setParams(map);
        session.setUri("/");

        Response response = mHttpd.serve(session);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("text/html", response.getMimeType());

        Cursor c = mTargetContext.getContentResolver().query(
                MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                null,
                MusicProvider._ID + " = ?",
                new String[] { musicId },
                null);
        assertNotNull(c);
        assertFalse(c.moveToFirst());
        assertEquals(0, c.getCount());

        c.close();
    }

    /**
     * Test remove local photo.
     */
    public void testRemoveLocalPhoto() {
        ConnectionManager.setIsGroupOwner(false);
        Uri uri = insertLocalPhotoTestData();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(PartyShareCommand.PARAM_CMD,
                String.valueOf(PartyShareCommand.CMD_CLIENT_RECV_EVENT));
        map.put(PartyShareEvent.PARAM_EVENT_CODE,
                String.valueOf(PartyShareEvent.EVENT_REMOVE_LOCAL_PHOTO));
        map.put(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH, TEST_PHOTO_MASTER_FILE);
        map.put(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS, TEST_PHOTO_OWNER);

        MockHttpSession session = new MockHttpSession();
        session.setMethod(Method.GET);
        session.setParams(map);
        session.setUri("/");

        Response response = mHttpd.serve(session);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("text/html", response.getMimeType());

        Cursor c = mTargetContext.getContentResolver().query(uri, null, null, null, null);
        assertNotNull(c);
        assertFalse(c.moveToFirst());
        assertEquals(0, c.getCount());

        c.close();
    }

    /**
     * Insert test data into playlist of databases.
     */
    private Uri insertMusicTestData() {
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_TITLE, TEST_MUSIC_TITLE);
        values.put(MusicProvider.COLUMN_ARTIST, TEST_MUSIC_ARTIST);
        values.put(MusicProvider.COLUMN_TIME, TEST_MUSIC_TIME);
        values.put(MusicProvider.COLUMN_MUSIC_URI, TEST_MUSIC_URL);
        values.put(MusicProvider.COLUMN_OWNER_ADDRESS, TEST_MUSIC_OWNER_ADDRESS);
        values.put(MusicProvider.COLUMN_OWNER_NAME, TEST_MUSIC_OWNER_NAME);
        try {
            return mTargetContext.getContentResolver().insert(MusicProvider.CONTENT_URI, values);
        } catch (Exception e) {
            fail(e.toString());
            return null;
        }
    }

    /**
     * Insert test data into local music of databases.
     */
    private Uri insertLocalMusicTestData() {
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_MUSIC_LOCAL_PATH, TEST_LOCAL_MUSIC_PATH);
        values.put(MusicProvider.COLUMN_MUSIC_MIMETYPE, TEST_LOCAL_MUSIC_MIMETYPE);
        try {
            return mTargetContext.getContentResolver().insert(
                    MusicProvider.CONTENT_URI_LOCAL_MUSIC, values);
        } catch (Exception e) {
            fail(e.toString());
            return null;
        }
    }

    /**
     * Insert test data into photolist of databases.
     */
    private Uri insertPhotoTestData() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH, TEST_PHOTO_MASTER_THUMBNAILE);
        map.put(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH, TEST_PHOTO_MASTER_FILE);
        map.put(PartyShareCommand.PARAM_PHOTO_SHARED_DATE, TEST_PHOTO_SHARED_DATE);
        map.put(PartyShareCommand.PARAM_PHOTO_TAKEN_DATE, TEST_PHOTO_TAKEN_DATE);
        map.put(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS, TEST_PHOTO_OWNER);
        map.put(PartyShareCommand.PARAM_PHOTO_LOCAL_THUMBNAIL_PATH, TEST_PHOTO_LOCAL_THUMBNAIL);
        map.put(PartyShareCommand.PARAM_PHOTO_LOCAL_FILE_PATH, TEST_PHOTO_LOCAL_FILE);
        map.put(PartyShareCommand.PARAM_PHOTO_MIME_TYPE, TEST_PHOTO_MIMETYPE);
        JsonUtil.addContent(mTargetContext, JsonUtil.CONTENT_TYPE_PHOTO, map);

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH, TEST_PHOTO_MASTER_THUMBNAILE);
        values.put(PhotoProvider.COLUMN_MASTER_FILE_PATH, TEST_PHOTO_MASTER_FILE);
        values.put(PhotoProvider.COLUMN_SHARED_DATE, TEST_PHOTO_SHARED_DATE);
        values.put(PhotoProvider.COLUMN_TAKEN_DATE, TEST_PHOTO_TAKEN_DATE);
        values.put(PhotoProvider.COLUMN_OWNER_ADDRESS, TEST_PHOTO_OWNER);
        values.put(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH, TEST_PHOTO_LOCAL_THUMBNAIL);
        values.put(PhotoProvider.COLUMN_LOCAL_FILE_PATH, TEST_PHOTO_LOCAL_FILE);
        values.put(PhotoProvider.COLUMN_MIME_TYPE, TEST_PHOTO_MIMETYPE);
        values.put(PhotoProvider.COLUMN_DL_STATE, TEST_PHOTO_DL_STATE);
        try {
            return mTargetContext.getContentResolver().insert(PhotoProvider.CONTENT_URI, values);
        } catch (Exception e) {
            fail(e.toString());
            return null;
        }
    }

    /**
     * Insert test data into local photo of databases.
     */
    private Uri insertLocalPhotoTestData() {
        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_QUERY_PATH, TEST_LOCAL_PHOTO_QUERY_PATH);
        values.put(PhotoProvider.COLUMN_THUMBNAIL_PATH, TEST_LOCAL_PHOTO_THUMBNAIL);
        values.put(PhotoProvider.COLUMN_FILE_PATH, TEST_LOCAL_PHOTO_FILE);
        try {
            return mTargetContext.getContentResolver().insert(
                    PhotoProvider.CONTENT_URI_LOCAL_PHOTO, values);
        } catch (Exception e) {
            fail(e.toString());
            return null;
        }
    }

    /**
     * Delete test data from database.
     */
    private void deleteTestData(Uri uri) {
        try {
            mTargetContext.getContentResolver().delete(uri, null, null);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * Create test file.
     */
    private File createTestFile(String filePath, String fileName) {
        File dir = new File(TEST_DATA_PATH);
        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                fail(e.toString());
            }
        }

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(mContext.getAssets().open(fileName));
            bos = new BufferedOutputStream(new FileOutputStream(file));

            byte[] b = new byte[1024];
            int readByte = 0;
            while (-1 != (readByte = bis.read(b))) {
                 bos.write(b, 0, readByte);
            }
        } catch (IOException e) {
            fail(e.toString());
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    fail(e.toString());
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    fail(e.toString());
                }
            }
        }

        return file;
    }

    /**
     * Delete test file.
     */
    private void deleteTestFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Compare InputStream.
     */
    private boolean compareInputStream(InputStream is1, InputStream is2) {
        byte[] buffer1 = new byte[1024];
        byte[] buffer2 = new byte[1024];
        try {
            while (is1.read(buffer1) > 0) {
                is2.read(buffer2);
                if (!Arrays.equals(buffer1, buffer2)) {
                    return false;
                }
            }
            if (is2.read(buffer2) > 0) {
                return false;
            }
        } catch (IOException e) {
            fail(e.toString());
            return false;
        } finally {
            if (is1 != null) {
                try {
                    is1.close();
                } catch (IOException e) {
                    fail(e.toString());
                }
            }
            if (is2 != null) {
                try {
                    is2.close();
                } catch (IOException e) {
                    fail(e.toString());
                }
            }
        }
        return true;
    }

    /**
     * Restore original file.
     */
    private void restoreFile() {
        restoreJsonFile(JsonUtil.CONTENT_TYPE_MUSIC);
        restoreJsonFile(JsonUtil.CONTENT_TYPE_PHOTO);
    }

    /**
     * Restore json file.
     */
    private void restoreJsonFile(int type) {
        File dir = mTargetContext.getFilesDir();

        String filename = "";
        switch (type) {
            case JsonUtil.CONTENT_TYPE_MUSIC:
                filename = MusicJsonFile.FILENAME;
                break;
            case JsonUtil.CONTENT_TYPE_PHOTO:
                filename = PhotoJsonFile.FILENAME;
                break;
            default:
                break;
        }

        File file = new File(dir, filename);
        File bkFile = new File(dir, filename + "_bk");
        if (file.exists()) {
            file.delete();
        }
        bkFile.renameTo(file);
    }

    /**
     * Backup original file.
     */
    private void backupFile() {
        backupJsonFile(JsonUtil.CONTENT_TYPE_MUSIC);
        backupJsonFile(JsonUtil.CONTENT_TYPE_PHOTO);
    }

    /**
     * Backup json file.
     */
    private void backupJsonFile(int type) {
        String filename = "";
        switch (type) {
            case JsonUtil.CONTENT_TYPE_MUSIC:
                filename = MusicJsonFile.FILENAME;
                break;
            case JsonUtil.CONTENT_TYPE_PHOTO:
                filename = PhotoJsonFile.FILENAME;
                break;
            default:
                break;
        }

        File dir = mTargetContext.getFilesDir();
        File jsonFile = new File(dir, filename);
        File bkFile = new File(dir, filename + "_bk");
        if (jsonFile.exists()) {
            jsonFile.renameTo(bkFile);
            jsonFile.delete();
        }
    }

    /**
     * Create json file.
     */
    private void createJsonFile(String filename) {
        File dir = mTargetContext.getFilesDir();
        File jsonFile = new File(dir, filename);

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(mContext.getAssets().open(filename));
            bos = new BufferedOutputStream(new FileOutputStream(jsonFile));
            byte[] b = new byte[1024];
            int readByte = 0;
            while (-1 != (readByte = bis.read(b))) {
                bos.write(b, 0, readByte);
            }
        } catch (IOException e) {
            fail(e.toString());
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                fail(e.toString());
            }
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                fail(e.toString());
            }
        }
    }

    /**
     * Delete json file.
     */
    private void deleteJsonFile(String filename) {
        File dir = mTargetContext.getFilesDir();
        File jsonFile = new File(dir, filename);
        if (jsonFile.exists()) {
            jsonFile.delete();
        }
    }

    /**
     * Confirm whether contents exist in the json file.
     */
    private boolean checkJsonContent(int type, String owner, String filePath) {
        String fileName = null;
        String arrayName = null;
        switch (type) {
            case JsonUtil.CONTENT_TYPE_PHOTO:
                fileName = PhotoJsonFile.FILENAME;
                arrayName = PhotoJsonFile.KEY_JSON_ARRAY;
                break;
            case JsonUtil.CONTENT_TYPE_MUSIC:
                fileName = MusicJsonFile.FILENAME;
                arrayName = MusicJsonFile.KEY_JSON_ARRAY;
                break;
            default:
                break;
        }

        File jsonFile = new File(mTargetContext.getFilesDir(), fileName);
        JSONObject object = JsonUtil.loadJsonFile(jsonFile);
        try {
            JSONArray array = object.getJSONArray(arrayName);
            for (int cnt = 0; cnt < array.length(); cnt++) {
                JSONObject obj = array.getJSONObject(cnt);
                String masterFilePath =
                        obj.getString(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH);
                String ownerAddress =
                        obj.getString(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS);

                if (masterFilePath.equals(filePath) && ownerAddress.equals(owner)) {
                    return true;
                }
            }
        } catch (Exception e) {
            fail(e.toString());
        }

        return false;
    }
}
