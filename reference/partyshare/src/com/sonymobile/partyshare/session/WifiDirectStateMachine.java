/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.session;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Message;

import com.sonymobile.partyshare.util.IState;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.State;
import com.sonymobile.partyshare.util.StateMachine;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class WifiDirectStateMachine extends StateMachine {

    public static final int CMD_ENABLE                          = 1;
    public static final int CMD_DISABLE                         = 2;
    public static final int CMD_START_DISCOVERY                 = 3;
    public static final int CMD_START_SESSION                   = 4;
    public static final int CMD_CONNECT                         = 5;
    public static final int CMD_DISCONNECT                      = 6;
    public static final int CMD_DISCONNECTED                    = 7;
    public static final int CMD_CANCEL                          = 8;
    public static final int CMD_ON_CONNECTION_INFO_AVAILABLE    = 9;
    public static final int CMD_ON_DNS_SB_TXT_RECORD_AVAILABLE  = 10;
    public static final int CMD_REQUEST_CONNECTION_INFO         = 11;
    public static final int CMD_INITIALIZE                      = 12;
    public static final int CMD_DISCOVER                        = 13;
    public static final int CMD_CHANGE_NAME                     = 14;
    public static final int CMD_THIS_DEVICE_CHANGED             = 15;
    public static final int CMD_NOTIFY_DISCONNECT               = 16;
    public static final int CMD_NOTIFY_ADD_MEMBER               = 17;
    public static final int CMD_NOTIFY_REMOVE_MEMBER            = 18;
    public static final int CMD_CREATE_GROUP_SUCCESS            = 19;
    public static final int CMD_ON_CHANNEL_DISCONNECTED         = 20;
    public static final int CMD_WIFI_CONNECT                    = 21;
    public static final int CMD_WIFI_CONNECT_SUCCESS            = 22;
    public static final int CMD_WIFI_DISCONNECTED               = 23;
    public static final int CMD_CREATE_GROUP_FAILED             = 24;
    public static final int CMD_WIFI_ENABLE                     = 25;
    public static final int CMD_WIFI_DISABLE                    = 26;

    private static volatile IState mConnectModeState;
    private Action mAction;

    private void stateMachineLog(String msg) {
        LogUtil.v(LogUtil.LOG_TAG, "WifiDirectStateMachine." + msg);
    }

    private String getCmdName(int cmd) {
        String name = "Unknown";
        switch (cmd) {
            case CMD_ENABLE:
                name = "CMD_ENABLE";
                break;
            case CMD_DISABLE:
                name = "CMD_DISABLE";
                break;
            case CMD_START_DISCOVERY:
                name = "CMD_START_DISCOVER";
                break;
            case CMD_START_SESSION:
                name = "CMD_START_SESSION";
                break;
            case CMD_CONNECT:
                name = "CMD_CONNECT";
                break;
            case CMD_DISCONNECT:
                name = "CMD_DISCONNECT";
                break;
            case CMD_DISCONNECTED:
                name = "CMD_DISCONNECTED";
                break;
            case CMD_CANCEL:
                name = "CMD_CANCEL";
                break;
            case CMD_ON_CONNECTION_INFO_AVAILABLE:
                name = "CMD_ON_CONNECTION_INFO_AVAILABLE";
                break;
            case CMD_ON_DNS_SB_TXT_RECORD_AVAILABLE:
                name = "CMD_ON_DNS_SB_TXT_RECORD_AVAILABLE";
                break;
            case CMD_REQUEST_CONNECTION_INFO:
                name = "CMD_REQUEST_CONNECTION_INFO";
                break;
            case CMD_INITIALIZE:
                name = "CMD_INITIALIZE";
                break;
            case CMD_DISCOVER:
                name = "CMD_DISCOVER";
                break;
            case CMD_CHANGE_NAME:
                name = "CMD_CHANGE_NAME";
                break;
            case CMD_THIS_DEVICE_CHANGED:
                name = "CMD_THIS_DEVICE_CHANGED";
                break;
            case CMD_NOTIFY_DISCONNECT:
                name = "CMD_NOTIFY_DISCONNECT";
                break;
            case CMD_NOTIFY_ADD_MEMBER:
                name = "CMD_NOTIFY_ADD_MEMBER";
                break;
            case CMD_NOTIFY_REMOVE_MEMBER:
                name = "CMD_NOTIFY_REMOVE_MEMBER";
                break;
            case CMD_CREATE_GROUP_SUCCESS:
                name = "CMD_CREATE_GROUP_SUCCESS";
                break;
            case CMD_ON_CHANNEL_DISCONNECTED:
                name = "CMD_ON_CHANNEL_DISCONNECTED";
                break;
            case CMD_WIFI_CONNECT:
                name = "CMD_WIFI_CONNECT";
                break;
            case CMD_WIFI_CONNECT_SUCCESS:
                name = "CMD_WIFI_CONNECT_SUCCESS";
                break;
            case CMD_WIFI_DISCONNECTED:
                name = "CMD_WIFI_DISCONNECTED";
                break;
            case CMD_CREATE_GROUP_FAILED:
                name = "CMD_CREATE_GROUP_FAILED";
                break;
            case CMD_WIFI_ENABLE:
                name = "CMD_WIFI_ENABLE";
                break;
            case CMD_WIFI_DISABLE:
                name = "CMD_WIFI_DISABLE";
                break;
            default:
                break;
        }
        return name;
    }

    public void reset() {
        setConnectModeState(mNullState);
    }

    public static void setConnectModeState(IState state) {
        mConnectModeState = state;
    }

    public static IState getConnectModeState() {
        return mConnectModeState;
    }

    public static boolean isJoiningSession() {
        boolean ret = false;
        if (mConnectModeState != null && (
                mConnectModeState.getName().equals(ConnectingState.class.getSimpleName()) ||
                mConnectModeState.getName().equals(ConnectedState.class.getSimpleName()) ||
                mConnectModeState.getName().equals(OnGoingSessionState.class.getSimpleName()) ||
                mConnectModeState.getName().equals(DisconnectingState.class.getSimpleName()) ||
                mConnectModeState.getName().equals(WifiConnectingState.class.getSimpleName()) ||
                mConnectModeState.getName().equals(WifiConnectedState.class.getSimpleName()))) {
            ret = true;
        }
        return ret;
    }

    public static boolean isOnGoingSession() {
        boolean ret = false;
        if (mConnectModeState != null && (
                mConnectModeState.getName().equals(OnGoingSessionState.class.getSimpleName()))) {
            ret = true;
        }
        return ret;
    }

    public static boolean isConnectedState() {
        boolean ret = false;
        if (mConnectModeState != null && (
                mConnectModeState.getName().equals(ConnectedState.class.getSimpleName()) ||
                mConnectModeState.getName().equals(WifiConnectedState.class.getSimpleName()))) {
            ret = true;
        }
        return ret;
    }

    public static boolean isConnectingState() {
        boolean ret = false;
        if (mConnectModeState != null && (
                mConnectModeState.getName().equals(ConnectingState.class.getSimpleName()))) {
            ret = true;
        }
        return ret;
    }

    // Actions
    public interface Action {
        void initialize();
        void startDiscovery();
        void stopDiscovery();
        void stopDiscoveryForWifi();
        void startSession();
        void connect(WifiP2pSessionInfo sessionInfo);
        void cancelConnect();
        void requestConnectionInfo();
        void requestJoin();
        void disconnected();
        void connectError();
        void createGroup();
        void removeGroup();
        void updateSession(WifiP2pSessionInfo sessionInfo);
        void changeName(String userName);
        void discoverServices();
        void openServerSocket(WifiP2pInfo p2pInfo);
        void openClientSocket(InetAddress address);
        void updateDevice(WifiP2pDevice device);
        void retryConnect();
        void retryConnect(WifiP2pInfo p2pInfo);
        void reset();
        void notifyAddMember(DeviceInfo device);
        void notifyRemoveMember(DeviceInfo device, String reason);
        void notifyDisconnect();
        void wifiDisconnect();
        void keepAlive();
        void createGroupResult(boolean result);
    }

    public static WifiDirectStateMachine makeWifiDirectStateMachine(
            Context context, Action action) {
        WifiDirectStateMachine sm = new WifiDirectStateMachine("WifiDirectStateMachine", action);
        sm.start();
        return sm;
    }

    WifiDirectStateMachine(String name, Action action) {
        super(name);
        stateMachineLog("ctor E");
        mAction = action;
        addState(mNullState);

        addState(mDisabledState, mNullState);
        addState(mEnabledState, mNullState);

        addState(mInitializedState, mEnabledState);
        addState(mDiscoverSessionState, mEnabledState);
        addState(mConnectingState, mEnabledState);
        addState(mConnectedState, mEnabledState);
        addState(mOnGoingSessionState, mEnabledState);
        addState(mDisconnectingState, mEnabledState);
        addState(mWifiConnectingState, mEnabledState);
        addState(mWifiConnectedState, mEnabledState);

        // Set the initial state
        setInitialState(mNullState);
        stateMachineLog("ctor X");
    }

    /**
     * State of Null
     */
    class NullState extends State {
        @Override
        public void enter() {
            stateMachineLog("NullState.enter");
        }

        @Override
        public boolean processMessage(Message message) {
            Object obj = message.obj;
            boolean retVal = NOT_HANDLED;
            stateMachineLog("NullState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_ENABLE:
                case CMD_WIFI_ENABLE:
                    transitionTo(mEnabledState);
                    retVal = HANDLED;
                    break;
                case CMD_DISABLE:
                case CMD_WIFI_DISABLE:
                case CMD_WIFI_DISCONNECTED:
                    mAction.disconnected();
                    transitionTo(mDisabledState);
                    retVal = HANDLED;
                    break;
                case CMD_THIS_DEVICE_CHANGED:
                    if (WifiP2pDevice.class.isInstance(obj)) {
                        WifiP2pDevice device = (WifiP2pDevice)obj;
                        mAction.updateDevice(device);
                        retVal = HANDLED;
                    }
                    break;
                case CMD_INITIALIZE:
                    deferMessage(message);
                    transitionTo(mEnabledState);
                    retVal = HANDLED;
                    break;
                case CMD_ON_CONNECTION_INFO_AVAILABLE:
                    deferMessage(message);
                    transitionTo(mEnabledState);
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog(
                            "NullState Error! unhandled message " + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("NullState.exit");
        }
    }

    /**
     * State of Disabled
     */
    class DisabledState extends State {
        @Override
        public void enter() {
            stateMachineLog("DisabledState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retVal;
            stateMachineLog("DisabledState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_INITIALIZE:
                case CMD_DISABLE:
                case CMD_WIFI_DISABLE:
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog(
                            "DisabledState Error! unhandled message " + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("DisabledState.exit");
        }
    }

    /**
     * State of Enabled
     */
    class EnabledState extends State {
        @Override
        public void enter() {
            stateMachineLog("EnabledState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retVal;
            stateMachineLog("EnabledState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_INITIALIZE:
                    mAction.initialize();
                    transitionTo(mInitializedState);
                    retVal = HANDLED;
                    break;
                case CMD_DISCONNECTED:
                    mAction.disconnected();
                    transitionTo(mDiscoverSessionState);
                    retVal = HANDLED;
                    break;
                case CMD_ON_CONNECTION_INFO_AVAILABLE:
                    deferMessage(message);
                    transitionTo(mDiscoverSessionState);
                    retVal = HANDLED;
                    break;
                case CMD_ON_CHANNEL_DISCONNECTED:
                    mAction.initialize();
                    retVal = HANDLED;
                    break;
                case CMD_ENABLE:
                case CMD_WIFI_ENABLE:
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog(
                            "EnabledState Error! unhandled message " + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("EnabledState.exit");
        }
    }

    /**
     * State of Initialized
     */
    class InitializedState extends State {
        @Override
        public void enter() {
            stateMachineLog("InitializedState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retVal;
            stateMachineLog("InitializedState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_START_SESSION:
                    mAction.createGroup();
                    retVal = HANDLED;
                    break;
                case CMD_CREATE_GROUP_SUCCESS:
                    mAction.startSession();
                    transitionTo(mOnGoingSessionState);
                    retVal = HANDLED;
                    break;
                case CMD_START_DISCOVERY:
                    mAction.startDiscovery();
                    transitionTo(mDiscoverSessionState);
                    retVal = HANDLED;
                    break;
                case CMD_ON_CONNECTION_INFO_AVAILABLE:
                    deferMessage(message);
                    transitionTo(mDiscoverSessionState);
                    retVal = HANDLED;
                    break;
                case CMD_WIFI_CONNECT:
                    mAction.stopDiscoveryForWifi();
                    transitionTo(mWifiConnectingState);
                    retVal = HANDLED;
                    break;
                case CMD_INITIALIZE:
                case CMD_DISCONNECTED:
                case CMD_WIFI_DISCONNECTED:
                case CMD_WIFI_ENABLE:
                case CMD_WIFI_DISABLE:
                    retVal = HANDLED;
                    break;

                default:
                    stateMachineLog("InitializedState Error! unhandled message "
                            + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("InitializedState.exit");
        }
    }

    /**
     * State of DiscoverSession
     */
    class DiscoverSessionState extends State {

        @Override
        public void enter() {
            stateMachineLog("DiscoverSessionState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            Object obj = message.obj;
            boolean retVal = NOT_HANDLED;
            stateMachineLog(
                    "DiscoverSessionState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_DISCOVER:
                    mAction.discoverServices();
                    retVal = HANDLED;
                    break;
                case CMD_START_SESSION:
                    mAction.createGroup();
                    retVal = HANDLED;
                    break;
                case CMD_START_DISCOVERY:
                    mAction.startDiscovery();
                    retVal = HANDLED;
                    break;
                case CMD_CREATE_GROUP_SUCCESS:
                    mAction.createGroupResult(true);
                    mAction.startSession();
                    transitionTo(mOnGoingSessionState);
                    retVal = HANDLED;
                    break;
                case CMD_CREATE_GROUP_FAILED:
                    mAction.createGroupResult(false);
                    retVal = HANDLED;
                    break;
                case CMD_ON_CONNECTION_INFO_AVAILABLE:
                    if (WifiP2pInfo.class.isInstance(obj)) {
                        WifiP2pInfo p2pInfo = (WifiP2pInfo)obj;
                        mAction.retryConnect(p2pInfo);
                        transitionTo(mConnectedState);
                        retVal = HANDLED;
                    }
                    break;
                case CMD_CONNECT:
                    if (WifiP2pSessionInfo.class.isInstance(obj)) {
                        WifiP2pSessionInfo config = (WifiP2pSessionInfo)obj;
                        mAction.connect(config);
                        transitionTo(mConnectingState);
                        retVal = HANDLED;
                    }
                    break;
                case CMD_REQUEST_CONNECTION_INFO:
                    mAction.requestConnectionInfo();
                    transitionTo(mConnectingState);
                    retVal = HANDLED;
                    break;
                case CMD_ON_DNS_SB_TXT_RECORD_AVAILABLE:
                    if (WifiP2pSessionInfo.class.isInstance(obj)) {
                        mAction.updateSession((WifiP2pSessionInfo)obj);
                        retVal = HANDLED;
                    }
                    break;
                case CMD_WIFI_CONNECT:
                    mAction.stopDiscoveryForWifi();
                    transitionTo(mWifiConnectingState);
                    retVal = HANDLED;
                    break;
                case CMD_INITIALIZE:
                case CMD_DISCONNECTED:
                case CMD_WIFI_DISCONNECTED:
                case CMD_WIFI_ENABLE:
                case CMD_WIFI_DISABLE:
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog("DiscoverSessionState Error! unhandled message "
                            + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("DiscoverSessionState.exit");
        }
    }

    /**
     * State of OnGoingSession
     */
    class OnGoingSessionState extends State {
        @Override
        public void enter() {
            stateMachineLog("OnGoingSessionState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            Object obj = message.obj;
            boolean retVal = NOT_HANDLED;
            stateMachineLog("OnGoingSessionState.processMessage what=" +
                        getCmdName(message.what));
            switch (message.what) {
                case CMD_CHANGE_NAME:
                    String userName = obj.toString();
                    mAction.changeName(userName);
                    retVal = HANDLED;
                    break;
                case CMD_DISCOVER:
                    mAction.discoverServices();
                    retVal = HANDLED;
                    break;
                case CMD_REQUEST_CONNECTION_INFO:
                    mAction.requestConnectionInfo();
                    retVal = HANDLED;
                    break;
                case CMD_ON_CONNECTION_INFO_AVAILABLE:
                    if (WifiP2pInfo.class.isInstance(obj)) {
                        WifiP2pInfo p2pInfo = (WifiP2pInfo)obj;
                        mAction.openServerSocket(p2pInfo);
                        retVal = HANDLED;
                    }
                    break;
                case CMD_DISCONNECT:
                    if (DeviceInfo.class.isInstance(obj)) {
                        mAction.notifyRemoveMember((DeviceInfo)obj,
                                PartyShareInfo.DISCONNECT_REASON_LEAVE_PARTY);
                        retVal = HANDLED;
                    }
                    break;
                case CMD_NOTIFY_DISCONNECT:
                    mAction.notifyDisconnect();
                    transitionTo(mDisconnectingState);
                    retVal = HANDLED;
                    break;
                case CMD_NOTIFY_ADD_MEMBER:
                    if (DeviceInfo.class.isInstance(obj)) {
                        mAction.notifyAddMember((DeviceInfo)obj);
                        retVal = HANDLED;
                    }
                    break;
                case CMD_NOTIFY_REMOVE_MEMBER:
                    if (DeviceInfo.class.isInstance(obj)) {
                        mAction.notifyRemoveMember((DeviceInfo)obj,
                                PartyShareInfo.DISCONNECT_REASON_BY_HOST);
                        retVal = HANDLED;
                    }
                    break;
                case CMD_WIFI_DISCONNECTED:
                    mAction.keepAlive();
                    retVal = HANDLED;
                    break;
                case CMD_INITIALIZE:
                case CMD_WIFI_ENABLE:
                case CMD_WIFI_DISABLE:
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog("OnGoingSessionState Error! unhandled message "
                            + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("OnGoingSessionState.exit");
        }
    }

    /**
     * State of Disconnecting
     */
    class DisconnectingState extends State {
        @Override
        public void enter() {
            stateMachineLog("DisconnectingState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retVal;
            stateMachineLog("DisconnectingState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_INITIALIZE:
                case CMD_THIS_DEVICE_CHANGED:
                case CMD_WIFI_ENABLE:
                case CMD_WIFI_DISABLE:
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog("DisconnectingState Error! unhandled message "
                            + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("DisconnectingState.exit");
        }
    }

    /**
     * State of Connecting
     */
    class ConnectingState extends State {
        @Override
        public void enter() {
            stateMachineLog("ConnectingState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            Object obj = message.obj;
            boolean retVal = NOT_HANDLED;
            stateMachineLog("ConnectingState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_CANCEL:
                    mAction.cancelConnect();
                    mAction.startDiscovery();
                    transitionTo(mDiscoverSessionState);
                    retVal = HANDLED;
                    break;
                case CMD_CHANGE_NAME:
                    String userName = obj.toString();
                    mAction.changeName(userName);
                    retVal = HANDLED;
                    break;
                case CMD_REQUEST_CONNECTION_INFO:
                    mAction.requestConnectionInfo();
                    retVal = HANDLED;
                    break;
                case CMD_ON_CONNECTION_INFO_AVAILABLE:
                    if (WifiP2pInfo.class.isInstance(obj)) {
                        WifiP2pInfo p2pInfo = (WifiP2pInfo)obj;
                        mAction.openClientSocket(p2pInfo.groupOwnerAddress);
                        mAction.requestJoin();
                        transitionTo(mConnectedState);
                        retVal = HANDLED;
                    }
                    break;
                case CMD_DISCONNECTED:
                    mAction.cancelConnect();
                    mAction.retryConnect();
                    retVal = HANDLED;
                    break;
                case CMD_DISCONNECT:
                    mAction.connectError();
                    retVal = HANDLED;
                    break;
                case CMD_INITIALIZE:
                case CMD_WIFI_DISCONNECTED:
                case CMD_WIFI_ENABLE:
                case CMD_WIFI_DISABLE:
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog("ConnectingState Error! unhandled message "
                            + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("ConnectingState.exit");
        }
    }

    /**
     * State of Connected
     */
    class ConnectedState extends State {
        @Override
        public void enter() {
            stateMachineLog("ConnectedState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retVal;
            stateMachineLog("ConnectedState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_DISCONNECT:
                    mAction.removeGroup();
                    transitionTo(mDisconnectingState);
                    retVal = HANDLED;
                    break;
                case CMD_CHANGE_NAME:
                    String userName = message.obj.toString();
                    mAction.changeName(userName);
                    retVal = HANDLED;
                    break;
                case CMD_NOTIFY_DISCONNECT:
                    mAction.notifyDisconnect();
                    retVal = HANDLED;
                    break;
                case CMD_INITIALIZE:
                case CMD_WIFI_DISCONNECTED:
                case CMD_WIFI_ENABLE:
                case CMD_WIFI_DISABLE:
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog("ConnectedState Error! unhandled message "
                            + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("ConnectedState.exit");
        }
    }

    /**
     * State of WifiConnecting
     */
    class WifiConnectingState extends State {
        @Override
        public void enter() {
            stateMachineLog("WifiConnectingState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retVal;
            stateMachineLog("WifiConnectingState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_CANCEL:
                    mAction.startDiscovery();
                    transitionTo(mDiscoverSessionState);
                    retVal = HANDLED;
                    break;
                case CMD_CHANGE_NAME:
                    String userName = message.obj.toString();
                    mAction.changeName(userName);
                    retVal = HANDLED;
                    break;
                case CMD_WIFI_CONNECT_SUCCESS:
                    String address = message.obj.toString();
                    try {
                        mAction.openClientSocket(InetAddress.getByName(address));
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    mAction.requestJoin();
                    transitionTo(mWifiConnectedState);
                    retVal = HANDLED;
                    break;
                case CMD_INITIALIZE:
                case CMD_DISCONNECTED:
                case CMD_WIFI_DISCONNECTED:
                case CMD_ENABLE:
                case CMD_DISABLE:
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog("WifiConnectingState Error! unhandled message "
                            + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("WifiConnectingState.exit");
        }
    }

    /**
     * State of WifiConnecting
     */
    class WifiConnectedState extends State {
        @Override
        public void enter() {
            stateMachineLog("WifiConnectedState.enter");
            setConnectModeState(this);
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retVal;
            stateMachineLog("WifiConnectedState.processMessage what=" + getCmdName(message.what));
            switch (message.what) {
                case CMD_DISCONNECT:
                    mAction.wifiDisconnect();
                    transitionTo(mDisconnectingState);
                    retVal = HANDLED;
                    break;
                case CMD_CHANGE_NAME:
                    String userName = message.obj.toString();
                    mAction.changeName(userName);
                    retVal = HANDLED;
                    break;
                case CMD_NOTIFY_DISCONNECT:
                    mAction.notifyDisconnect();
                    retVal = HANDLED;
                    break;
                case CMD_INITIALIZE:
                case CMD_DISCONNECTED:
                case CMD_ENABLE:
                case CMD_DISABLE:
                    retVal = HANDLED;
                    break;
                default:
                    stateMachineLog("WifiConnectedState Error! unhandled message "
                            + getCmdName(message.what));
                    retVal = NOT_HANDLED;
                    break;
            }
            return retVal;
        }

        @Override
        public void exit() {
            stateMachineLog("WifiConnectedState.exit");
        }
    }

    NullState mNullState = new NullState();
    EnabledState mEnabledState = new EnabledState();
    DisabledState mDisabledState = new DisabledState();
    InitializedState mInitializedState = new InitializedState();
    DiscoverSessionState mDiscoverSessionState = new DiscoverSessionState();
    ConnectingState mConnectingState = new ConnectingState();
    ConnectedState mConnectedState = new ConnectedState();
    OnGoingSessionState mOnGoingSessionState = new OnGoingSessionState();
    DisconnectingState mDisconnectingState = new DisconnectingState();
    WifiConnectingState mWifiConnectingState = new WifiConnectingState();
    WifiConnectedState mWifiConnectedState = new WifiConnectedState();

    @Override
    protected synchronized void onHalting() {
        stateMachineLog("halting");
        this.notifyAll();
    }
}
