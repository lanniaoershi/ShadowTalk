/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.testutils;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

/**
 * MockContext.
 */
public class MockContext extends ContextWrapper {
    private ContentResolver mContentResolver = null;
    private Intent mServiceIntent;

    public MockContext(Context context) {
        super(context);
    }

    @Override
    public ContentResolver getContentResolver() {
        if (mContentResolver == null) {
            return super.getContentResolver();
        } else {
            return mContentResolver;
        }
    }

    public void setContentResolver(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    @Override
    public ComponentName startService(Intent service) {
        mServiceIntent = service;
        return service.getComponent();
    }

    public Intent getServiceIntent() {
        return mServiceIntent;
    }
}
