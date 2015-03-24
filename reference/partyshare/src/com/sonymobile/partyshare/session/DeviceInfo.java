/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.session;

import com.sonymobile.partyshare.util.LogUtil;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class DeviceInfo implements Externalizable {

    private String mUserName = null;
    private String mDeviceAddress = null;
    private String mIpAddress = null;
    private String mCheckAddress = null;
    private boolean mHost = false;

    public DeviceInfo() {
    }

    public DeviceInfo(String userName, String deviceAddress, String checkAddress, boolean host) {
        super();
        this.mUserName = userName;
        this.mDeviceAddress = deviceAddress;
        this.mIpAddress = "";
        this.mCheckAddress = checkAddress;
        this.mHost = host;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    public void setIpAddress(String ipAddress) {
        LogUtil.v(LogUtil.LOG_TAG, "DeviceInfo.setIpAddress:" + ipAddress);
        this.mIpAddress = ipAddress;
    }

    public String getIpAddress() {
        return mIpAddress;
    }

    public void setCheckAddress(String checkAddress) {
        LogUtil.v(LogUtil.LOG_TAG, "DeviceInfo.setCheckAddress:" + checkAddress);
        this.mCheckAddress = checkAddress;
    }

    public String getCheckAddress() {
        return mCheckAddress;
    }

    public boolean getIsHost() {
        return mHost;
    }

    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
        mUserName = (String)input.readObject();
        mDeviceAddress = (String)input.readObject();
        mIpAddress = (String)input.readObject();
        mCheckAddress = (String)input.readObject();
        mHost = (boolean)input.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        output.writeObject(mUserName);
        output.writeObject(mDeviceAddress);
        output.writeObject(mIpAddress);
        output.writeObject(mCheckAddress);
        output.writeBoolean(mHost);
    }
}
