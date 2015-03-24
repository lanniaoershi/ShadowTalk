/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.Context;

import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.DeviceInfo;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Utility;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PartyShareEventNotifier class.
 * This class notifies the event from host to clients.
 */
public class PartyShareEventNotifier {
    /** Connection retry count. */
    private static final int RETRY_COUNT = 5;

    /**
     * Send event to all clients.
     * @param context Context.
     * @param event Event to notify.
     * @param param Content parameter.
     */
    public static synchronized void notifyEvent(
            Context context, int event, Map<String, String> param) {
        ArrayList<String> addrList = getClientAddressList(context);
        for (String address : addrList) {
            String client = String.format("http://%s:%s", address, PartyShareHttpd.PORT);
            LogUtil.d(LogUtil.LOG_TAG,
                    "PartyShareEventNotifier.notifyEvent client : " + client);

            sendEvent(client, event, param);
        }
    }

    /**
     * Send event to specified client.
     * @param context Context.
     * @param event Event to notify.
     * @param macAddress Mac address of client.
     * @param param Content parameter.
     */
    public static synchronized void notifyEventSpecifyClient(
            Context context, int event, String macAddress, Map<String, String> param) {
        String ipAddress = Utility.getIpAddress(context, macAddress);
        String client = String.format("http://%s:%s", ipAddress, PartyShareHttpd.PORT);
        if (client != null) {
            LogUtil.d(LogUtil.LOG_TAG,
                    "PartyShareEventNotifier.notifyEventSpecifyClient client : " + client);

            sendEvent(client, event, param);
        }
    }

    /**
     * Send event to client.
     * @param address Client address.
     * @param event Event to notify.
     * @param param Content parameter.
     */
    private static void sendEvent(String address, int event, Map<String, String> param) {
        final String serverUrl = createServerUrl(address, event, param);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < RETRY_COUNT; i++) {
                    int result = connectServer(serverUrl);
                    if (result == HttpURLConnection.HTTP_OK) {
                        LogUtil.d(LogUtil.LOG_TAG,
                                "PartyShareEventNotifier.sendEvent result OK");
                        break;
                    } else {
                        LogUtil.e(LogUtil.LOG_TAG,
                                "PartyShareEventNotifier.sendEvent http error code : " + result);
                    }
                }
            }
        }).start();
    }

    /**
     * Connect to http server.
     * @param serverUrl Http server url.
     * @return http response code.
     */
    private static int connectServer(String serverUrl) {
        HttpURLConnection http = null;
        try {
            URL url = new URL(serverUrl);
            http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setConnectTimeout(5000);
            http.setReadTimeout(5000);
            http.connect();
            return http.getResponseCode();
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "PartyShareEventNotifier.sendEvent : " + e.toString());
            return -1;
        } finally {
            if (http != null) {
                http.disconnect();
                http = null;
            }
        }
    }

    /**
     * Create server url.
     * @param address Client address.
     * @param event Event to notify.
     * @param param Content parameter.
     * @return server url.
     */
    private static String createServerUrl(String address, int event, Map<String, String> param) {
        List<String> key = new ArrayList<String>();
        List<String> value = new ArrayList<String>();

        if (param != null) {
            Set<Map.Entry<String, String>> entrySet = param.entrySet();
            Iterator<Map.Entry<String, String>> iterator = entrySet.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                key.add(entry.getKey());
                value.add(entry.getValue());
            }
        }

        StringBuffer url = new StringBuffer();
        url.append(address + "?");

        for (int cnt = 0; cnt < key.size(); cnt++) {
            url.append(key.get(cnt) + "=" + value.get(cnt));
            url.append("&");
        }

        url.append(PartyShareCommand.PARAM_CMD + "=" + PartyShareCommand.CMD_CLIENT_RECV_EVENT);
        url.append("&");
        url.append(PartyShareEvent.PARAM_EVENT_CODE + "=" + event);

        LogUtil.d(LogUtil.LOG_TAG, "PartyShareEventNotifier.sendEvent httpUrl : " + url.toString());

        return url.toString();
    }

    /**
     * Get client address list.
     * @param context Context.
     * @return client address list.
     */
    private static ArrayList<String> getClientAddressList(Context context) {
        ArrayList<String> addressList = new ArrayList<String>();

        String hostAddress = ConnectionManager.getGroupOwnerAddress();
        LogUtil.d(LogUtil.LOG_TAG,
                "PartyShareEventNotifier.getClientAddressList hostAddress : " + hostAddress);

        ConnectionManager manager = ConnectionManager.getInstance(context.getApplicationContext());
        for (DeviceInfo info : manager.getGroupList()) {
            String address = info.getIpAddress();
            LogUtil.d(LogUtil.LOG_TAG,
                    "PartyShareEventNotifier.getClientAddressList address : " + address);

            if (!address.equals(hostAddress)) {
                addressList.add(address);
            }
        }
        return addressList;
    }
}
