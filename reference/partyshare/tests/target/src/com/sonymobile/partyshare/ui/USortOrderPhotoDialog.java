/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.Dialog;
import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

import com.sonymobile.partyshare.util.Setting;

public class USortOrderPhotoDialog extends AndroidTestCase {
    private static final String TAG = "USortOrderPhotoDialog";

    private Context mContext;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Unit test for create dialog.
     */
    public void testCreateDialog() {
        Log.d(TAG, "testCreateDialog");

        SortOrderPhotoDialog sortOrderPhotoDialog = new SortOrderPhotoDialog(mContext);
        Dialog dialog = sortOrderPhotoDialog.createDialog();
        assertNotNull(dialog);
    }

    /**
     * Unit test for the click taken date asc item.
     */
    public void testClick_TakenDateAsc() {
        Log.d(TAG, "testClick_TakenDateAsc");

        SortOrderPhotoDialog sortOrderPhotoDialog = new SortOrderPhotoDialog(mContext);
        Dialog dialog = sortOrderPhotoDialog.createDialog();
        assertNotNull(dialog);

        sortOrderPhotoDialog.mItemListener.onClick(dialog, Setting.PHOTO_SORT_TAKEN_DATE_ASC);
        assertEquals(Setting.PHOTO_SORT_TAKEN_DATE_ASC, Setting.getSortOrderPhoto(mContext));
    }

    /**
     * Unit test for the click taken date desc item.
     */
    public void testClick_TakenDateDesc() {
        Log.d(TAG, "testClick_TakenDateDesc");

        SortOrderPhotoDialog sortOrderPhotoDialog = new SortOrderPhotoDialog(mContext);
        Dialog dialog = sortOrderPhotoDialog.createDialog();
        assertNotNull(dialog);

        sortOrderPhotoDialog.mItemListener.onClick(dialog, Setting.PHOTO_SORT_TAKEN_DATE_DESC);
        assertEquals(Setting.PHOTO_SORT_TAKEN_DATE_DESC, Setting.getSortOrderPhoto(mContext));
    }

    /**
     * Unit test for the click shared date asc item.
     */
    public void testClick_SharedDateAsc() {
        Log.d(TAG, "testClick_SharedDateAsc");

        SortOrderPhotoDialog sortOrderPhotoDialog = new SortOrderPhotoDialog(mContext);
        Dialog dialog = sortOrderPhotoDialog.createDialog();
        assertNotNull(dialog);

        sortOrderPhotoDialog.mItemListener.onClick(dialog, Setting.PHOTO_SORT_SHARED_DATE_ASC);
        assertEquals(Setting.PHOTO_SORT_SHARED_DATE_ASC, Setting.getSortOrderPhoto(mContext));
    }

    /**
     * Unit test for the click shared date desc item.
     */
    public void testClick_SharedDateDesc() {
        Log.d(TAG, "testClick_SharedDateDesc");

        SortOrderPhotoDialog sortOrderPhotoDialog = new SortOrderPhotoDialog(mContext);
        Dialog dialog = sortOrderPhotoDialog.createDialog();
        assertNotNull(dialog);

        sortOrderPhotoDialog.mItemListener.onClick(dialog, Setting.PHOTO_SORT_SHARED_DATE_DESC);
        assertEquals(Setting.PHOTO_SORT_SHARED_DATE_DESC, Setting.getSortOrderPhoto(mContext));
    }
}
