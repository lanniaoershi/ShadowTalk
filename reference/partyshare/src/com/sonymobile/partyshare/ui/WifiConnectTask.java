/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.LogUtil;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

/*
 * This is AsyncTask for WiFi Connecting.
 */
class WifiConnectTask extends AsyncTask<Object, Void, Integer> {
    public static final int RESULT_SUCCEEDED = 1;
    public static final int RESULT_FAILED = 2;
    public static final int RESULT_CANCELED = 3;
    public static final int RESULT_TIMEOUT = 4;

    private static final int WAIT_CNT_WIFI_CONNECTION = 60;
    private static final int WIFI_CONNECTION_INTERVAL = 2000;
    private static final int WAIT_CNT_WIFI_ENABLE = 10;

    private String mSsid;
    private String mPassPhrase;

    @Override
    protected Integer doInBackground(Object... params) {
        WifiManager wifiManager = (WifiManager) params[0];
        mSsid = (String) params[1];
        mPassPhrase = (String) params[2];

        if (wifiManager == null || mSsid == null || mPassPhrase == null) {
            LogUtil.e(LogUtil.LOG_TAG, "WifiConnectTask param error.");
            return RESULT_FAILED;
        }

        // check whether wifi is already connected
        boolean connected = isConnected(wifiManager, mSsid);
        String ipaddress = getIpAddr();
        if (connected && ipaddress != null) {
            LogUtil.d(LogUtil.LOG_TAG, "SSID [" + mSsid + "] is already connected.");
            LogUtil.d(LogUtil.LOG_TAG, "IP : " + ipaddress);
            return RESULT_SUCCEEDED;
        }

        // if Wifi is not enabled, set to enable.
        if (!wifiManager.isWifiEnabled()) {
            if (!wifiManager.setWifiEnabled(true)) {
                LogUtil.w(LogUtil.LOG_TAG, "WifiManager.setWifiEnabled() is failed.");
                return RESULT_FAILED;
            }
            int count = 0;
            while (!wifiManager.isWifiEnabled()) {
                LogUtil.i(LogUtil.LOG_TAG, "Wait for enabling wifi...");
                if (count >= WAIT_CNT_WIFI_ENABLE) {
                    LogUtil.w(LogUtil.LOG_TAG, "Wifi could not be enabled.");
                    return RESULT_FAILED;
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ie) {
                    // continue
                }
                count++;
            }
        }

        boolean ret = connectWifi(wifiManager, mSsid, mPassPhrase);
        if (!ret) {
            LogUtil.i(LogUtil.LOG_TAG, "connectWifi is failed.");
            return RESULT_FAILED;
        }

        // wait for wifi is configured and ip address is assigned
        int retryCnt;
        for (retryCnt = 0; WAIT_CNT_WIFI_CONNECTION > retryCnt; retryCnt++) {
            LogUtil.i(LogUtil.LOG_TAG, "Wait for connecting Wifi...");

            connected = isConnected(wifiManager, mSsid);
            ipaddress = getIpAddr();
            if (connected && ipaddress != null) {
                LogUtil.d(LogUtil.LOG_TAG, "SSID [" + mSsid + "] is connected.");
                LogUtil.d(LogUtil.LOG_TAG, "IP : " + ipaddress);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo == null) {
                    return RESULT_FAILED;
                }
                ConnectionManager.setCheckAddress(wifiInfo.getMacAddress());
                return RESULT_SUCCEEDED;
            }

            try {
                Thread.sleep(WIFI_CONNECTION_INTERVAL);
            } catch (InterruptedException e) {
                LogUtil.i(LogUtil.LOG_TAG, "Cancelled Wifi connecting.");
                if (connected) {
                    wifiManager.disconnect();
                }
                return RESULT_CANCELED;
            }
        }

        if (retryCnt >= WAIT_CNT_WIFI_CONNECTION) {
            LogUtil.i(LogUtil.LOG_TAG, "connectWifi retry out.");
            return RESULT_TIMEOUT;
        }

        return RESULT_SUCCEEDED;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        LogUtil.v(LogUtil.LOG_TAG, "WifiConnectTask.onCancelled()");
    }

    /**
     * Check whether Wi-Fi SSID is connected or not.
     *
     * @param wifiManager WifiManager object.
     * @param ssid SSID.
     * @return True if WiFi SSID is connected and configured.
     */
    private boolean isConnected(WifiManager wifiManager, String ssid) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            LogUtil.w(LogUtil.LOG_TAG, "wifiManager.getConnectionInfo() == null");
            return false;
        }

        String ssidInConfig = "\"" + ssid + "\"";
        String wifiInfoSsidInConfig = wifiInfo.getSSID();

        List<WifiConfiguration> existConfigs = wifiManager.getConfiguredNetworks();
        if (existConfigs == null) {
            return false;
        }

        boolean ret = false;
        for (WifiConfiguration existConfig: existConfigs) {
            if (existConfig != null && existConfig.SSID != null &&
                existConfig.SSID.equals(ssidInConfig)) {
                if (existConfig.SSID.equals(wifiInfoSsidInConfig)) {
                    ret = true;
                    int foundNetworkId = existConfig.networkId;
                    LogUtil.v(LogUtil.LOG_TAG, "foundNetworkId : " + foundNetworkId);
                    ConnectionManager.setWifiNetworkId(foundNetworkId);
                } else {
                    ret = false;
                }
                break;
            }
        }
        return ret;
    }

    /**
     * Connect with Wi-Fi.
     *
     * @param wifiManager WifiManager object.
     * @param ssid SSID.
     * @param pass PassPhrase.
     * @return Result.
     */
    private boolean connectWifi(WifiManager wifiManager, String ssid, String pass) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.preSharedKey = "\"" + pass + "\"";

        return updateNetwork(wifiManager, config);
    }

    /**
     * Get IP address.
     *
     * @return IP address.
     */
    private String getIpAddr() {
        String ret = null;
        try {
            Enumeration<NetworkInterface> netIfs = NetworkInterface
                    .getNetworkInterfaces();
            if (netIfs == null) {
                return ret;
            }

            while(netIfs.hasMoreElements()) {
                NetworkInterface netIf = netIfs.nextElement();
                Enumeration<InetAddress> addrs = netIf.getInetAddresses();
                while(addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress().toString();
                        if (ip.matches("192.168.[0-9]+\\.[0-9]+")) {
                            ret = ip;
                            break;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Update Network.
     *
     * @param wifiManager WifiManager object.
     * @param config WifiConfiguration object.
     * @return Result.
     */
    private boolean updateNetwork(WifiManager wifiManager, WifiConfiguration config) {
        removeNetworkInExistingConfig(wifiManager, config.SSID);
        int networkId = wifiManager.addNetwork(config);
        LogUtil.v(LogUtil.LOG_TAG, "networkId : " + networkId);

        boolean ret = false;
        if (networkId >= 0) {
            // Try to disable the current network and start a new one.
            if (wifiManager.enableNetwork(networkId, true)) {
                ret = wifiManager.saveConfiguration();
            } else {
                LogUtil.w(LogUtil.LOG_TAG, "Failed to enable network : " + config.SSID);
            }
        } else {
            LogUtil.w(LogUtil.LOG_TAG, "Unable to add network : " + config.SSID);
        }
        return ret;
    }

    /**
     * Remove Network.
     *
     * @param wifiManager WifiManager object.
     * @param ssid SSID.
     */
    private void removeNetworkInExistingConfig(WifiManager wifiManager, String ssid) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs == null) {
            return;
        }

        Integer foundNetworkId = null;
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals(ssid)) {
                foundNetworkId = existingConfig.networkId;
            }
        }
        if (foundNetworkId != null) {
            LogUtil.d(LogUtil.LOG_TAG, "Removing old configuration for network : " + ssid);
            wifiManager.removeNetwork(foundNetworkId);
        }
        return;
    }
}
