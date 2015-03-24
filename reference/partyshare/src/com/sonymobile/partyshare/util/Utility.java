/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;
import android.widget.Toast;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.DeviceInfo;

import java.nio.charset.Charset;

/**
 * Utility class.
 */
public class Utility {
    private static final String PKG_NAME = "com.sonymobile.partyshare";
    private static final String XPERIA_LIB = "com.sony.device";
    private static final String MIME_APPLICATION = "application/" + PKG_NAME;
    private static final int DEVICE_NOT_CHECKED = 0;
    private static final int DEVICE_HOST_AVAILABLE = 1;
    private static final int DEVICE_HOST_UNAVAILABLE = 2;

    private static int sDeviceInfo = DEVICE_NOT_CHECKED;
    private static Toast sToast = null;

    /**
     * Get WifiP2p device status string.
     * @param deviceStatus WifiP2p device status value.
     * @return WifiP2p device status string.
     */
    public static String getWifiP2pDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    /**
     * Get WifiP2p manager status string.
     * @param status WifiP2p manager status value.
     * @return WifiP2p manager status string.
     */
    public static String getWifiP2pManagerStatus(int status) {
        switch(status) {
            case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                return "DISABLED";
            case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                return "ENABLED";
            default:
                return "Unknown";
        }
    }

    /**
     * Get Wifi manager status string.
     * @param status Wifi manager status value.
     * @return Wifi manager status string.
     */
    public static String getWifiManagerStatus(int status) {
        switch(status) {
            case WifiManager.WIFI_STATE_DISABLING:
                return "DISABLING";
            case WifiManager.WIFI_STATE_DISABLED:
                return "DISABLED";
            case WifiManager.WIFI_STATE_ENABLING:
                return "ENABLING";
            case WifiManager.WIFI_STATE_ENABLED:
                return "ENABLED";
            default:
                return "Unknown";
        }
    }

    /**
     * Get action listener status string.
     * @param reasonCode WifiP2pManager fail reason code.
     * @return Action listener status string.
     */
    public static String getActionListenerStatus(int reasonCode) {
        switch(reasonCode) {
            case WifiP2pManager.ERROR:
                return "Error";
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "P2p Unsupported";
            case WifiP2pManager.BUSY:
                return "Busy";
            case WifiP2pManager.NO_SERVICE_REQUESTS:
                return "No Service Requests";
            default:
                return "Unknown";
        }
    }

    /**
     * Get application version.
     * @param context Context
     * @return App version.
     */
    public static String getApplicationVersion(Context context) {
        if (context == null) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.getApplicationVersion param error.");
            return null;
        }
        String ret = "";
        PackageInfo packageInfo = null;
        PackageManager pm = context.getPackageManager();
        try {
            packageInfo = pm.getPackageInfo(context.getApplicationContext().getPackageName(),
                    PackageManager.GET_META_DATA);
            ret = String.valueOf(packageInfo.versionCode);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Devices check whether Exclude
     * @param context Context.
     * @return true is exclude.
     */
    public static boolean excludeCheck(Context context) {
        if (context == null) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.excludeCheck param error.");
            return true;
        }

        TypedArray models = context.getResources().obtainTypedArray(R.array.excludeList);
        boolean exclude = false;
        for (int i = 0; i < models.length(); i++) {
            String model = models.getString(i);
            if (model.equalsIgnoreCase(Build.MODEL)) {
                exclude = true;
                break;
            }
        }
        models.recycle();

        if (hasHostCapability(context)
                && Build.VERSION.RELEASE.equalsIgnoreCase("4.4.2") && exclude) {
            return true;
        }
        return false;
    }

    /**
     * Create NdefMessage.
     * @param ssid SSID
     * @param pp PassPhrase
     * @param hostAddr Host IP address
     * @param startTime Session created time
     * @return NdefMessage.
     */
    public static NdefMessage createNdefMsg(
            String ssid, String pp, String hostAddr, String sessionName, String startTime,
            boolean invitedByHost, boolean permission) {
        String mimeType = MIME_APPLICATION;
        NdefMessage msg;
        if (ssid == null || ssid.isEmpty() || pp == null || pp.isEmpty() ||
            hostAddr == null || hostAddr.isEmpty() || startTime == null || startTime.isEmpty()) {
            msg = new NdefMessage(new NdefRecord[] {
                NdefRecord.createApplicationRecord(PKG_NAME)
            });
        } else {
            msg = new NdefMessage(new NdefRecord[] {
                NdefRecord.createMime(mimeType, ssid.getBytes(Charset.forName("UTF-8"))),
                NdefRecord.createMime(mimeType, pp.getBytes(Charset.forName("UTF-8"))),
                NdefRecord.createMime(mimeType, hostAddr.getBytes(Charset.forName("UTF-8"))),
                NdefRecord.createMime(mimeType, sessionName.getBytes(Charset.forName("UTF-8"))),
                NdefRecord.createMime(mimeType, startTime.getBytes(Charset.forName("UTF-8"))),
                NdefRecord.createMime(mimeType,
                        String.valueOf(invitedByHost).getBytes(Charset.forName("UTF-8"))),
                NdefRecord.createMime(mimeType,
                        String.valueOf(permission).getBytes(Charset.forName("UTF-8"))),
                NdefRecord.createApplicationRecord(PKG_NAME)
            });
        }
        return msg;
    }

    /**
     * Get file path from uri.
     * @param context Context
     * @param uri Uri
     * @param dataType Data type for where clause
     * @return NdefMessage.
     */
    public static String getFilePathByUri(Context context, Uri uri, String[] dataType) {
        if (context == null || uri == null || dataType == null) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.getFilePathByUri param error.");
            return null;
        }
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, dataType, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String path = cursor.getString(0);
            cursor.close();
            return path;
        }
        return null;
    }

    /**
     * Check whether device has host capability.
     * @return True if device has host capability, otherwise false.
     */
    public static boolean hasHostCapability(Context context) {
        if (context == null) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.hasHostCapability param error.");
            return false;
        }
        if (sDeviceInfo == DEVICE_NOT_CHECKED) {
            PackageManager pm = context.getPackageManager();
            String[] libs = pm.getSystemSharedLibraryNames();
            sDeviceInfo = DEVICE_HOST_UNAVAILABLE;
            for (String lib : libs) {
                if (XPERIA_LIB.equalsIgnoreCase(lib)) {
                    sDeviceInfo = DEVICE_HOST_AVAILABLE;
                    break;
                }
            }
        }
        boolean ret = false;
        if (sDeviceInfo == DEVICE_HOST_AVAILABLE) {
            ret = true;
        }
        return ret;
    }

    /**
     * Show toast message. If toast has been shown currently, it is canceled and next one is shown.
     * @param context Context
     * @param msg Text message
     * @param duration Toast.LENGTH_SHORT or Toast.LENGTH_LONG
     */
    public static void showToast(Context context, String msg, int duration) {
        if (context == null || msg == null || msg.isEmpty()) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.showToast param error.");
            return;
        }
        if (sToast == null) {
            sToast = Toast.makeText(context.getApplicationContext(), msg, duration);
        } else {
            sToast.setText(msg);
        }
        sToast.show();
    }

    /**
     * Check whether session info is saved.
     * @param context Context
     * @param name Joined session name
     * @param time The time which Joined session is created
     * @param hostAddr Host's mac address
     * @return True if session ifo is saved.
     */
    public static boolean isSavedSession(
            Context context, String name, String time, String hostAddr) {
        if (context == null || name == null || name.isEmpty() || time == null || time.isEmpty() ||
                hostAddr == null || hostAddr.isEmpty()) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.isSavedSession param error.");
            return false;
        }
        String sessinoInfo = name + time + hostAddr;
        String savedSession = Setting.getJoinedSessionInfo(context);
        boolean ret = false;
        if (savedSession != null && savedSession.equals(sessinoInfo)) {
            ret = true;
        }
        return ret;
    }

    /**
     * Get ip address from mac address.
     * @param context Context.
     * @param macAddress Client mac address.
     * @return client ip address.
     */
    public static String getIpAddress(Context context, String macAddress) {
        if (context == null || macAddress == null || macAddress.isEmpty()) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.getIpAddress param error.");
            return null;
        }
        ConnectionManager manager = ConnectionManager.getInstance(context.getApplicationContext());
        for (DeviceInfo info : manager.getGroupList()) {
            if (info.getDeviceAddress().equals(macAddress)) {
                return info.getIpAddress();
            }
        }
        return null;
    }

    /**
     * Get mac address from ip address.
     * @param context Context.
     * @param ipAddress Client ip address.
     * @return client mac address.
     */
    public static String getMacAddress(Context context, String ipAddress) {
        if (context == null || ipAddress == null || ipAddress.isEmpty()) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.getMacAddress param error.");
            return null;
        }
        ConnectionManager manager = ConnectionManager.getInstance(context.getApplicationContext());
        for (DeviceInfo info : manager.getGroupList()) {
            if (info.getIpAddress().equals(ipAddress)) {
                return info.getDeviceAddress();
            }
        }
        return null;
    }

    /**
     * Get resource ID of session color.
     * @param context Context.
     * @param sessionName Session name.
     * @param hostName Host user name.
     * @return Resourc ID of session color.
     */
    public static int getSessionListResId(Context context, String sessionName, String hostName) {
        if (context == null || sessionName == null || sessionName.isEmpty() ||
                hostName == null || hostName.isEmpty()) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.getSessionListResId param error.");
            return R.drawable.session_item_01;
        }

        String str = sessionName + hostName;
        int seed = Math.abs(str.hashCode() + 1);
        TypedArray ids = context.getResources().obtainTypedArray(R.array.sessionListResourceIds);
        int selected = seed % ids.length();
        int ret = ids.getResourceId(selected, 0);
        ids.recycle();
        return ret;
    }

    /**
     * Get resource ID of album art.
     * @param context Context.
     * @param title Music title.
     * @return Resourc ID of album art.
     */
    public static int getAlbumArtResId(Context context, String title) {
        if (context == null || title == null || title.isEmpty()) {
            LogUtil.e(LogUtil.LOG_TAG, "Utility.getAlbumArtResId param error.");
            return R.drawable.party_share_music_list_jacket_0001_icn;
        }

        int seed = Math.abs(title.hashCode() + 1);
        TypedArray icons = context.getResources().obtainTypedArray(R.array.albumArtResourceIds);
        int selected = seed % icons.length();
        int ret = icons.getResourceId(selected, 0);
        icons.recycle();
        return ret;
    }
}
