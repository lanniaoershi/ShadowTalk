/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.WifiDirectStateMachine;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.NotificationUtil;

public class PartyShareService extends Service {
    public static final String ACTION_START = "action_start";
    public static final String ACTION_START_FOREGROUND = "action_start_foreground";
    public static final String ACTION_STOP_FOREGROUND = "action_stop_foreground";
    public static final String ACTION_NETWORK_CONNECT = "action_network_connect";
    public static final String ACTION_NETWORK_DISCONNECT = "action_network_disconnect";
    public static final String ACTION_NETWORK_WIFI_CONNECT = "action_network_wifi_connect";
    public static final String ACTION_NETWORK_WIFI_DISCONNECT = "action_network_wifi_disconnect";
    public static final String ACTION_NETWORK_WIFI_ENABLE = "action_network_wifi_enable";
    public static final String ACTION_NETWORK_WIFI_DISNABLE = "action_network_wifi_disable";

    private ConnectionManager mConnectionManager = null;

    public class PartyShareServiceBinder extends Binder {

        public PartyShareService getService() {
            return PartyShareService.this;
        }
    }

    private final IBinder mBinder = new PartyShareServiceBinder();
    private Handler mHandler = new Handler();
    private Runnable mDisconnectedRunnable = new DisconnectedRunnable();
    private Runnable mWifiDisconnectedRunnable = new WifiDisconnectedRunnable();
    private Runnable mWifiDissableRunnable = new WifiDisabledRunnable();

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareService.onCreate()");
        mConnectionManager = ConnectionManager.getInstance(this);
        stopForeground(true);
        NotificationUtil.caneclJoinPartyNotification(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareService.onStartCommand()");

        String action = "";
        if (intent != null && intent.getAction() != null) {
            action = intent.getAction();
        }
        if (action.equals(ACTION_START_FOREGROUND)) {
            Notification notification = NotificationUtil.createJoinPartyNotification(
                    this, ConnectionManager.getSessionName());
            startForeground(NotificationUtil.NOTIFICATION_ID_JOIN_PARTY, notification);
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareService.startForeground");
            Intent musicIntent = new Intent(this, MusicService.class);
            startService(musicIntent);
        } else if (action.equals(ACTION_STOP_FOREGROUND)) {
            stopForeground(true);
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareService.stopForeground");
        } else if (action.equals(ACTION_NETWORK_CONNECT)) {
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareService.ACTION_NETWORK_CONNECT");
            mHandler.removeCallbacks(mDisconnectedRunnable);
            mConnectionManager.getStateMachine().sendMessage(
                    WifiDirectStateMachine.CMD_REQUEST_CONNECTION_INFO);
        } else if (action.equals(ACTION_NETWORK_DISCONNECT)) {
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareService.ACTION_NETWORK_DISCONNECT");
            mHandler.removeCallbacks(mDisconnectedRunnable);
            mHandler.postDelayed(mDisconnectedRunnable, 1000);
        } else if (action.equals(ACTION_NETWORK_WIFI_CONNECT)) {
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareService.ACTION_NETWORK_WIFI_CONNECT");
            mHandler.removeCallbacks(mWifiDisconnectedRunnable);
        } else if (action.equals(ACTION_NETWORK_WIFI_DISCONNECT)) {
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareService.ACTION_NETWORK_WIFI_DISCONNECT");
            mHandler.removeCallbacks(mWifiDisconnectedRunnable);
            mHandler.postDelayed(mWifiDisconnectedRunnable, 1000);
        } else if (action.equals(ACTION_NETWORK_WIFI_ENABLE)) {
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareService.ACTION_NETWORK_WIFI_ENABLE");
            mHandler.removeCallbacks(mWifiDissableRunnable);
        } else if (action.equals(ACTION_NETWORK_WIFI_DISNABLE)) {
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareService.ACTION_NETWORK_WIFI_DISNABLE");
            mHandler.removeCallbacks(mWifiDissableRunnable);
            mHandler.postDelayed(mWifiDissableRunnable, 1000);
        } else if (action.equals(ACTION_START)) {
            LogUtil.d(LogUtil.LOG_TAG, "PartyShareService.ACTION_START");
        } else {
            LogUtil.d(LogUtil.LOG_TAG, "Service was restarted by system");
            if (mConnectionManager != null) {
                mConnectionManager.initialize();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareService.onDestory()");
        stopForeground(true);
        mConnectionManager.terminate();
        ConnectionManager.destroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareService.onBind()");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareService.onUnbind()");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareService.onUnbind()");
        return true;
    }

    @Override
    public void onLowMemory() {
        LogUtil.w(LogUtil.LOG_TAG, "PartyShareService.onLowMemory()");
    }

    public ConnectionManager getConnectionManagerInstance() {
        return mConnectionManager;
    }

    private class DisconnectedRunnable implements Runnable {
        public void run() {
            mConnectionManager.getStateMachine().sendMessage(
                    WifiDirectStateMachine.CMD_DISCONNECTED);
        }
    }

    private class WifiDisconnectedRunnable implements Runnable {
        public void run() {
            mConnectionManager.getStateMachine().sendMessage(
                    WifiDirectStateMachine.CMD_WIFI_DISCONNECTED);
        }
    }

    private class WifiDisabledRunnable implements Runnable {
        public void run() {
            mConnectionManager.getStateMachine().sendMessage(
                    WifiDirectStateMachine.CMD_WIFI_DISABLE);
        }
    }
}
