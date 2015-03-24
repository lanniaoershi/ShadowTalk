/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

public interface PhotoEventListener {
    public void onRefreshList();
    public void onDownloadCompleted(String path);
    public void onDownloadFailed(int msgId);
}
