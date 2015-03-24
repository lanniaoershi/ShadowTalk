/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.util;

import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;

public class UPhotoUtils extends AndroidTestCase {
    private static final String TAG = "UPhotoUtils";

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext().getApplicationContext();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Unit test for get Extension.
     */
    public void testGetExtension() {
        Log.d(TAG, "testGetExtension");

        assertEquals(".jpg", PhotoUtils.getExtension("image/jpeg"));
        assertEquals(".png", PhotoUtils.getExtension(""));
    }

    /**
     * Unit test for create save folder path.
     */
    public void testCreateSaveFolderPath() {
        Log.d(TAG, "testCreateSaveFolderPath");

        String path = PhotoUtils.createSaveFolderPath(mContext);
        Log.d(TAG, "testCreateSaveFolderPath path : " + path);
        assertNotNull(path);
    }

    /**
     * Unit test for create temp folder path.
     */
    public void testCreateTempFolderPath() {
        Log.d(TAG, "testCreateTempFolderPath");

        String path = PhotoUtils.createTempFolderPath();
        Log.d(TAG, "testCreateTempFolderPath path : " + path);
        assertNotNull(path);
    }

    /**
     * Unit test for create thumbnail folder.
     */
    public void testCreateThumbnailFolder() {
        Log.d(TAG, "testCreateThumbnailFolder");

        String path = PhotoUtils.createThumbnailFolder(mContext);
        Log.d(TAG, "testCreateThumbnailFolder path : " + path);
        assertNotNull(path);

        assertTrue(new File(path).exists());

        // Clean
        PhotoUtils.deleteFile(path);
    }

    /**
     * Unit test for delete thumbnail folder.
     */
    public void testDeleteThumbnailFolder() {
        Log.d(TAG, "testDeleteThumbnailFolder");

        String path = PhotoUtils.createThumbnailFolder(mContext);
        Log.d(TAG, "testCreateThumbnailFolder path : " + path);
        assertNotNull(path);
        assertTrue(new File(path).exists());

        PhotoUtils.deleteThumbnailFolder(mContext);
        assertFalse(new File(path).exists());
    }

    /**
     * Unit test for create my photo folder.
     */
    public void testCreateMyPhotoFolder() {
        Log.d(TAG, "testCreateMyPhotoFolder");

        Setting.setPhotoSaveFolder(mContext, PhotoUtils.createSaveFolderPath(mContext));

        String path = PhotoUtils.createMyPhotoFolder(mContext, "Test");
        Log.d(TAG, "testCreateMyPhotoFolder path : " + path);
        assertNotNull(path);
        assertTrue(new File(path).exists());

        // Clean
        PhotoUtils.deleteFile(path);
    }
}
