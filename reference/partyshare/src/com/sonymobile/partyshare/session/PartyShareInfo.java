/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.session;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

public class PartyShareInfo implements Externalizable {

    private static final long serialVersionUID = -6337815127797603301L;

    /**
     * Connection notification
     */
    public static final int CMD_REQUEST_JOIN = 0;

    /**
     * Allow connection of guest
     */
    public static final int CMD_ACCEPT_JOIN = 1;

    /**
     * Eliminate guest
     */
    public static final int CMD_DISCONNECT = 2;

    /**
     * User name change notification
     */
    public static final int CMD_CHANGE_NAME = 3;

    /**
     * Add member
     */
    public static final int CMD_ADD_MEMBER = 4;

    /**
     * Guest is removed from the session
     */
    public static final int CMD_REMOVE_MEMBER = 5;

    /**
     * Reject connection of guest
     */
    public static final int CMD_REJECT_JOIN = 6;

    /**
     * Check of alive
     */
    public static final int CMD_KEEP_ALIVE = 7;

    public static final String DISCONNECT_REASON_BY_HOST = "BY_HOST";
    public static final String DISCONNECT_REASON_LEAVE_PARTY = "LEAVE_PARTY";
    public static final String DISCONNECT_REASON_MAX = "MAX";
    public static final String DISCONNECT_REASON_CLOSED = "CLOSED";

    private String mAppVersion = "";

    private int mCommand = -1;
    private String mAttribute = "";
    private String mAttribute2 = "";
    private List<DeviceInfo> mGroupList = null;

    public PartyShareInfo() {
    }

    public PartyShareInfo(String version, int command, String attr) {
        super();
        this.mAppVersion = version;
        this.mCommand = command;
        this.mAttribute = attr;
    }

    public PartyShareInfo(String version, int command, List<DeviceInfo> groupList) {
        super();
        this.mAppVersion = version;
        this.mCommand = command;
        this.mGroupList = groupList;
    }

    public PartyShareInfo(String version, int command, DeviceInfo device, String attr) {
        super();
        this.mAppVersion = version;
        this.mCommand = command;
        List<DeviceInfo> groupList = new ArrayList<DeviceInfo>();
        groupList.add(device);
        this.mGroupList = groupList;
        this.mAttribute = attr;
    }

    public PartyShareInfo(String version, int command, List<DeviceInfo> groupList, String attr) {
        super();
        this.mAppVersion = version;
        this.mCommand = command;
        this.mGroupList = groupList;
        this.mAttribute = attr;
    }

    public PartyShareInfo(
            String version, int command, List<DeviceInfo> groupList, String attr, String attr2) {
        super();
        this.mAppVersion = version;
        this.mCommand = command;
        this.mGroupList = groupList;
        this.mAttribute = attr;
        this.mAttribute2 = attr2;
    }

    public String getAppVersion() {
        return mAppVersion;
    }

    public int getCommand() {
        return mCommand;
    }

    public String getAttribute() {
        return mAttribute;
    }

    public String getAttribute2() {
        return mAttribute2;
    }

    public List<DeviceInfo> getGroupList() {
        return mGroupList;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        mGroupList.set(0, deviceInfo);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
        mAppVersion = (String)input.readObject();
        mCommand = input.readInt();
        mAttribute = (String)input.readObject();
        mAttribute2 = (String)input.readObject();
        mGroupList = (List<DeviceInfo>)input.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        output.writeObject(mAppVersion);
        output.writeInt(mCommand);
        output.writeObject(mAttribute);
        output.writeObject(mAttribute2);
        output.writeObject(mGroupList);
    }

    public static String getCommand(int command) {
        switch(command) {
        case CMD_REQUEST_JOIN:
            return "REQUEST_JOIN";
        case CMD_ACCEPT_JOIN:
            return "ACCEPT_JOIN";
        case CMD_DISCONNECT:
            return "DISCONNECT";
        case CMD_CHANGE_NAME:
            return "CHANGE_NAME";
        case CMD_ADD_MEMBER:
            return "ADD_MEMBER";
        case CMD_REMOVE_MEMBER:
            return "REMOVE_MEMBER";
        case CMD_REJECT_JOIN:
            return "REJECT_JOIN";
        case CMD_KEEP_ALIVE:
            return "KEEP_ALIVE";
        default:
            return "Unknown";
        }
    }
}
