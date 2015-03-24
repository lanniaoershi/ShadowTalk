/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.test.AndroidTestCase;
import android.util.Log;

import com.sonymobile.partyshare.service.PhotoDownloadService;
import com.sonymobile.partyshare.util.Setting;

public class UDownloadModeDialog extends AndroidTestCase {
    private static final String TAG = "UDownloadModeDialog";

    private MockContextWrapper mContext;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = new MockContextWrapper(getContext());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Unit test for create dialog.
     */
    public void testCreateDialog() {
        Log.d(TAG, "testCreateDialog");

        DownloadModeDialog downloadModeDialog = new DownloadModeDialog(mContext);
        Dialog dialog = downloadModeDialog.createDialog();
        assertNotNull(dialog);
    }

    /**
     * Unit test for the click auto item.
     */
    public void testClick_Auto() {
        Log.d(TAG, "testClick_Auto");

        DownloadModeDialog downloadModeDialog = new DownloadModeDialog(mContext);
        Dialog dialog = downloadModeDialog.createDialog();
        assertNotNull(dialog);

        downloadModeDialog.mItemListener.onClick(dialog, Setting.PHOTO_DOWNLOAD_MODE_AUTO);
        String action = mContext.mIntent.getAction();
        assertEquals(PhotoDownloadService.ACTION_START_AUTO_DL, action);
        assertEquals(Setting.PHOTO_DOWNLOAD_MODE_AUTO, Setting.getDownloadMode(mContext));
    }

    /**
     * Unit test for the click manual item.
     */
    public void testClick_Manual() {
        Log.d(TAG, "testClick_Manual");

        DownloadModeDialog downloadModeDialog = new DownloadModeDialog(mContext);
        Dialog dialog = downloadModeDialog.createDialog();
        assertNotNull(dialog);

        downloadModeDialog.mItemListener.onClick(dialog, Setting.PHOTO_DOWNLOAD_MODE_MANUAL);
        String action = mContext.mIntent.getAction();
        assertEquals(PhotoDownloadService.ACTION_STOP_AUTO_DL, action);
        assertEquals(Setting.PHOTO_DOWNLOAD_MODE_MANUAL, Setting.getDownloadMode(mContext));
    }

    private static class MockContextWrapper extends ContextWrapper {
        Intent mIntent;
        public MockContextWrapper(Context context) {
            super(context);
        }

        @Override
        public ComponentName startService(Intent service) {
            mIntent = service;
            return null;
        }
    }
}
