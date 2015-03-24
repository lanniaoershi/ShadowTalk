/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */

package com.sonymobile.partyshare.session;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import com.sonymobile.partyshare.service.PartyShareService;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Utility;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiDirectStateMachine mStateMachine;

    /*
     * @param stateMachine
     */
    public WifiDirectBroadcastReceiver(WifiDirectStateMachine stateMachine) {
        super();
        mStateMachine = stateMachine;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            LogUtil.d(LogUtil.LOG_TAG,
                    "WIFI_P2P_STATE_CHANGED_ACTION:" + Utility.getWifiP2pManagerStatus(state));
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_ENABLE);
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_INITIALIZE);
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_START_DISCOVERY);
            } else {
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISABLE);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            LogUtil.d(LogUtil.LOG_TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
            NetworkInfo networkInfo =
                    (NetworkInfo)intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            WifiP2pInfo wifiP2pInfo =
                    (WifiP2pInfo)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);

            LogUtil.d(LogUtil.LOG_TAG, "wifiP2pInfo.isGroupOwner = " + wifiP2pInfo.isGroupOwner);

            Intent i = new Intent(context, PartyShareService.class);
            if (networkInfo.isConnected()) {
                LogUtil.i(LogUtil.LOG_TAG, "networkInfo : connected");
                // we are connected with the other device, request
                // connection
                // info to find group owner IP
                i.setAction(PartyShareService.ACTION_NETWORK_CONNECT);
            } else {
                // It's a disconnect
                // activity.resetData();
                i.setAction(PartyShareService.ACTION_NETWORK_DISCONNECT);
            }
            context.startService(i);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            LogUtil.d(LogUtil.LOG_TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            mStateMachine.sendMessage(WifiDirectStateMachine.CMD_THIS_DEVICE_CHANGED,
                (WifiP2pDevice)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            LogUtil.d(LogUtil.LOG_TAG, "NETWORK_STATE_CHANGED_ACTION");
            NetworkInfo networkInfo =
                    (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            Intent i = new Intent(context, PartyShareService.class);
            if (networkInfo.isConnected()) {
                i.setAction(PartyShareService.ACTION_NETWORK_WIFI_CONNECT);
            } else {
                i.setAction(PartyShareService.ACTION_NETWORK_WIFI_DISCONNECT);
            }
            context.startService(i);
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int wifiState = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE ,WifiManager.WIFI_STATE_UNKNOWN);
            LogUtil.d(LogUtil.LOG_TAG, "WIFI_STATE_CHANGED_ACTION:" + wifiState);
            Intent i = new Intent(context, PartyShareService.class);
            if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                i.setAction(PartyShareService.ACTION_NETWORK_WIFI_ENABLE);
            } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                i.setAction(PartyShareService.ACTION_NETWORK_WIFI_DISNABLE);
            }
            context.startService(i);
        }
    }
}
