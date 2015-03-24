/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.service;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sonymobile.partyshare.util.LogUtil;

public class PhotoAsyncQueryHandler extends AsyncQueryHandler {

    public PhotoAsyncQueryHandler(ContentResolver cr) {
        super(cr);
    }

    private class AsyncWorkerHandler extends AsyncQueryHandler.WorkerHandler {
        public AsyncWorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
            } catch (Exception e) {
                LogUtil.w(LogUtil.LOG_TAG,
                        "PhotoAsyncQueryHandler occur exception " + e.getMessage());
            }
        }
    }

    @Override
    protected Handler createHandler(Looper looper) {
        LogUtil.d(LogUtil.LOG_TAG, "PhotoAsyncQueryHandler createHandler");
        return new AsyncWorkerHandler(looper);
    }

    @Override
    public void startQuery(int token, Object cookie, Uri uri,
            String[] projection, String selection, String[] selectionArgs,
            String orderBy) {
        super.startQuery(token, cookie, uri, projection, selection,
                selectionArgs, orderBy);
        throw new RuntimeException("invalid called for startQuery");
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        super.onInsertComplete(token, cookie, uri);
        LogUtil.d(LogUtil.LOG_TAG,
                "called onInsertComplete, uri:" + uri.toString());
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
        super.onUpdateComplete(token, cookie, result);
        LogUtil.d(LogUtil.LOG_TAG, "called onUpdateComplete, result:" + result);
    }

    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
        super.onDeleteComplete(token, cookie, result);
        LogUtil.d(LogUtil.LOG_TAG, "called onDeleteComplete, result:" + result);
    }
}