/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.service;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.DeviceInfo;
import com.sonymobile.partyshare.ui.PhotoDownloader;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Setting;

import java.util.ConcurrentModificationException;
import java.util.Timer;
import java.util.TimerTask;

public class PhotoDownloadService extends Service {

    public static final String ACTION_START_AUTO_DL =
            "com.sonymobile.partyshare.action.START_AUTO_DL";
    public static final String ACTION_STOP_AUTO_DL =
            "com.sonymobile.partyshare.action.STOP_AUTO_DL";

    // TODO: 1sec.
    private static final long DEFAULT_AUTO_DL_TIME = 1000;

    private static final Handler mHandler = new Handler();
    private static boolean mIsStartedAutoDl = false;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private static class AutoDownloadTimerTask extends TimerTask {
        private PhotoDownloadService mPhotoDownloadService;

        public AutoDownloadTimerTask(PhotoDownloadService service) {
            super();
            mPhotoDownloadService = service;
        }

        @Override
        public void run() {
            LogUtil.v(LogUtil.LOG_TAG, "AutoDownloadTimerTask.run()");

            int mode = Setting.getDownloadMode(mPhotoDownloadService);
            if (mode == Setting.PHOTO_DOWNLOAD_MODE_MANUAL) {
                LogUtil.v(LogUtil.LOG_TAG,
                        "AutoDownloadTimerTask.run stop timer, download mode is manual.");
                // if download mode is manual, stop timer.
                mPhotoDownloadService.cancelAutoDownloadTimer();
                return;
            }

            // Query the photo have not downloaded.
            Cursor cursor = null;
            int id = -1;
            try {
                cursor = ProviderUtil.query(mPhotoDownloadService.getApplicationContext(),
                        PhotoProvider.CONTENT_URI,
                        new String[] { PhotoProvider._ID, PhotoProvider.COLUMN_OWNER_ADDRESS },
                        PhotoProvider.COLUMN_LOCAL_FILE_PATH + " is NULL"
                        + " or " + PhotoProvider.COLUMN_LOCAL_FILE_PATH + " = ''",
                        null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    LogUtil.d(LogUtil.LOG_TAG, "AutoDownloadTimerTask.run : cursor.getcount : "
                            + cursor.getCount());

                    // Check target device exists in the session.
                    boolean isExist = false;
                    do {
                        String targetAddress = cursor.getString(
                                cursor.getColumnIndex(PhotoProvider.COLUMN_OWNER_ADDRESS));
                        isExist = mPhotoDownloadService.checkExistDeviceAddress(targetAddress);
                        if (isExist) {
                            break;
                        }
                    } while (cursor.moveToNext());

                    if (!isExist) {
                        LogUtil.d(LogUtil.LOG_TAG, "Target device not exists in the session.");
                        return;
                    }

                    id = cursor.getInt(cursor.getColumnIndex(PhotoProvider._ID));
                    LogUtil.d(LogUtil.LOG_TAG, "AutoDownloadTimerTask.run id : " + id);
                } else {
                    LogUtil.d(LogUtil.LOG_TAG, "AutoDownloadTimerTask.run : No data");
                    return;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            // Download photo.
            final int result = PhotoDownloader.getInstance(
                    mPhotoDownloadService.getApplicationContext()).download(id, null);
            if (result == R.string.party_share_strings_manual_photo_download_memory_err_txt) {
                LogUtil.v(LogUtil.LOG_TAG,
                        "AutoDownloadTimerTask.run stop timer, Not enough memory.");
                // if Not enough memory on Internal storage, stop timer.
                mPhotoDownloadService.cancelAutoDownloadTimer();
                mHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(mPhotoDownloadService,
                                R.string.party_share_strings_auto_photo_download_memory_err_txt,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    private boolean checkExistDeviceAddress(String targetAddress) {
        boolean isExist = false;
        try {
            ConnectionManager manager = ConnectionManager.getInstance(getApplicationContext());
            for (DeviceInfo info : manager.getGroupList()) {
                String deviceAddress = info.getDeviceAddress();
                if (targetAddress.equalsIgnoreCase(deviceAddress)) {
                    LogUtil.d(LogUtil.LOG_TAG,
                            "The target device exists in the session.");
                    isExist = true;
                    break;
                }
            }
        } catch (ConcurrentModificationException cme){
            LogUtil.e(LogUtil.LOG_TAG,
                    "Occur ConcurrentModificationException when check exist device address : "
                    + cme);
        }
        return isExist;
    }

    private void startAutoDownloadTimer() {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoDownloadService.startAutoDownloadTimer()");
        if (mTimer != null) {
            LogUtil.v(LogUtil.LOG_TAG, "startAutoDownloadTimer() Timer is not null.");
            return;
        }

        mTimerTask = new AutoDownloadTimerTask(this);
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(mTimerTask, getAutoDownloadTime(), getAutoDownloadTime());
        mIsStartedAutoDl = true;
    }

    private long getAutoDownloadTime() {
        long time = DEFAULT_AUTO_DL_TIME * 10;

        // TODO: increase the time by the number of member.
//        int memberNum =
//                ConnectionManager.getInstance(getApplicationContext()).getGroupList().size();
//        time = time + (DEFAULT_AUTO_DL_TIME * memberNum);
        return time;
    }

    private void cancelAutoDownloadTimer() {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoDownloadService.cancelAutoDownloadTimer()");
        if (mTimer == null) {
            LogUtil.v(LogUtil.LOG_TAG, "cancelAutoDownloadTimer() Timer is null.");
            return;
        }
        mTimer.cancel();
        mTimer = null;
        mIsStartedAutoDl = false;
    }

    public static boolean isStartedAutoDownload() {
        LogUtil.v(LogUtil.LOG_TAG, "isStartedAutoDownload() : " + mIsStartedAutoDl);
        return mIsStartedAutoDl;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.v(LogUtil.LOG_TAG, "PhotoDownloadService.onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.v(LogUtil.LOG_TAG, "PhotoDownloadService.onStartCommand()");
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_AUTO_DL.equals(action)) {
                startAutoDownloadTimer();
                return START_REDELIVER_INTENT;
            } else if (ACTION_STOP_AUTO_DL.equals(action)) {
                cancelAutoDownloadTimer();
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.v(LogUtil.LOG_TAG, "PhotoDownloadService.onDestory()");
        cancelAutoDownloadTimer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}