/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.service;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.net.Uri;

import com.sonymobile.partyshare.util.LogUtil;

public class MusicAsyncQueryHandler extends AsyncQueryHandler{

    public MusicAsyncQueryHandler(ContentResolver cr) {
        super(cr);
    }

    @Override
    public void startQuery(int token, Object cookie, Uri uri,
            String[] projection, String selection, String[] selectionArgs,
            String orderBy) {
        super.startQuery(token, cookie, uri, projection, selection, selectionArgs,
                orderBy);
        throw new RuntimeException("invalid called for startQuery");
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        super.onInsertComplete(token, cookie, uri);
        LogUtil.d("MyAsyncQueryHandler", "called onInsertComplete, uri:" + uri.toString());
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
        super.onUpdateComplete(token, cookie, result);
        LogUtil.d("MyAsyncQueryHandler", "called onUpdateComplete, result:" + result);
    }

    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
        super.onDeleteComplete(token, cookie, result);
        LogUtil.d("MyAsyncQueryHandler", "called onDeleteComplete, result:" + result);
    }
}
