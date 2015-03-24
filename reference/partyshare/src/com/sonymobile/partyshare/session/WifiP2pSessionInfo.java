/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.session;

import android.net.wifi.p2p.WifiP2pDevice;

public class WifiP2pSessionInfo {
    public WifiP2pDevice device;
    public String userName = "";
    public String sessionName = "";
    public String time = "";
    public String requestNumber = "";
    public String applicationVersion = "";
    public long keepAlive = 0;
}
