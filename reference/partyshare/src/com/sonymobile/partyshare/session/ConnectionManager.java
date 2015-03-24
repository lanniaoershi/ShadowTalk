/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.session;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.httpd.JsonUtil;
import com.sonymobile.partyshare.httpd.MusicPlayListController;
import com.sonymobile.partyshare.httpd.PartyShareHttpd;
import com.sonymobile.partyshare.httpd.PostContent;
import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.service.MusicAsyncQueryHandler;
import com.sonymobile.partyshare.service.MusicService;
import com.sonymobile.partyshare.service.PhotoAsyncQueryHandler;
import com.sonymobile.partyshare.service.PhotoDownloadService;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.NotificationUtil;
import com.sonymobile.partyshare.util.PhotoUtils;
import com.sonymobile.partyshare.util.Setting;
import com.sonymobile.partyshare.util.Utility;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

/**
 * This is connection management class. Control WiFi P2p process and WiFi process.
 */
public class ConnectionManager implements ChannelListener,
        ConnectionInfoListener, DnsSdTxtRecordListener, Handler.Callback {

    public static final String TXTRECORD_KEY_SESSION = "SessionName";
    public static final String TXTRECORD_KEY_USERNAME = "UserName";
    public static final String TXTRECORD_KEY_TIME = "Time";
    public static final String TXTRECORD_KEY_VERSION = "Version";

    public static final int SERVER_PORT = 8080;
    public static final int MAX = 10;
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    public static final int ERROR = 0x400 + 3;

    public static final int UPDATE_OK_DATAONLY = 0;
    public static final int UPDATE_OK_NOTIFY = 1;
    public static final int UPDATE_NOT_REQUIRED = 2;

    public static final int NONE = 0;
    public static final int MYSELF = 1;
    public static final int REJECT_MAX = 2;
    public static final int REJECT_CLOSED = 3;
    public static final int SOCKET_ERROR = 4;
    public static final int CONNECTING = 5;

    private static final int UPDATE_TIME = 30000;
    private static final int KEEP_ALIVE = 30000;
    private static final int POST_DELAY = 1000;

    private static final int RETRY_COUNT = 5;

    public static final int UNKNOWN_NETWORK_ID = -1;

    // For HOST
    public static final String SERVICE_INSTANCE_MAIN = "partyshare";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    private static ConnectionManager sInstance = null;

    private Context mContext;

    private WifiP2pManager mWifiP2pManager;
    private Channel mChannel = null;
    private int mNetworkId = UNKNOWN_NETWORK_ID;

    private final IntentFilter mIntentFilter = new IntentFilter();
    private WifiDirectBroadcastReceiver mWifiDirectReceiver = null;

    private WifiDirectStateMachine mStateMachine = null;

    private WifiLock mWifiLock = null;

    private static String sSsid = "";
    private static String sPassPhrase = "";
    private static String sGroupOwnerAddress = "";
    private static String sMyLocalIpAddress = "";
    private static int sWifiNetworkId = UNKNOWN_NETWORK_ID;
    private static boolean sNfcJoinPermission = false;

    // DnsSdService for HOST
    private WifiP2pDnsSdServiceInfo mMainService;
    private WifiP2pDnsSdServiceRequest mServiceRequest;

    /* package */
    private static String sSessionName = "";
    private static String sStartSessionTime = "";
    private static String sCheckAddress = "";
    private static String sMyAddress = "";
    private static volatile boolean sIsGroupOwner = false;
    private String mUserName = "";
    private String mAppVersion = "";
    private int mErrorReason = NONE;

    private String mTargetDeviceAddress;
    private int mRetryCount;
    private boolean mStartDiscovery;

    private List<DeviceInfo> mGroupList =
            Collections.synchronizedList(new ArrayList<DeviceInfo>());
    private List<WifiP2pSessionInfo> mSessionList =
            Collections.synchronizedList(new ArrayList<WifiP2pSessionInfo>());

    private Handler mHandler = new Handler();
    private Handler mCallbackHandler = new Handler(this);
    private Runnable mRetryDiscoveryRunnable = new RetryDiscoveryRunnable();
    private Runnable mKeepAliveRunnable = new KeepAliveRunnable();
    private Runnable mRetryConnectRunnable = new RetryConnectRunnable();

    private WifiP2pDevice mMyDevice;
    private static List<ConnectionManagerListener> mListener =
            new ArrayList<ConnectionManagerListener>();

    private Vector<Transfer> mTransfers = new Vector<Transfer>();
    private Thread mServerSocketThread = null;

    private PartyShareHttpd mHttpd;

    public static void setIsGroupOwner(boolean b) {
        LogUtil.v(LogUtil.LOG_TAG, "setIsGroupOwner:" + b);
        sIsGroupOwner = b;
    }

    public static boolean isGroupOwner() {
        return sIsGroupOwner;
    }

    public Handler getCallbackHandler() {
        return mCallbackHandler;
    }

    public static void setNfcJoinPermission(boolean permission) {
        LogUtil.v(LogUtil.LOG_TAG, "setNfcJoinPermission:" + permission);
        sNfcJoinPermission = permission;
    }

    public static boolean getNfcJoinPermission() {
        return sNfcJoinPermission;
    }

    public static void setSsid(String ssid) {
        LogUtil.v(LogUtil.LOG_TAG, "setSsid:" + ssid);
        sSsid = ssid;
    }

    public static String getSsid() {
        return sSsid;
    }

    public static void setWifiNetworkId(int wifiNetworkId) {
        LogUtil.v(LogUtil.LOG_TAG, "setWifiNetworkId:" + wifiNetworkId);
        sWifiNetworkId = wifiNetworkId;
    }

    public static int getWifiNetworkId() {
        return sWifiNetworkId;
    }

    public static void setPassPhrase(String passPhrase) {
        LogUtil.v(LogUtil.LOG_TAG, "setPassPhrase:" + passPhrase);
        sPassPhrase = passPhrase;
    }

    public static String getPassPhrase() {
        return sPassPhrase;
    }

    public static void setGroupOwnerAddress(String address) {
        sGroupOwnerAddress = address;
    }

    public static String getGroupOwnerAddress() {
        return sGroupOwnerAddress;
    }

    public static void setLocalAddress(String address) {
        sMyLocalIpAddress = address;
    }

    public static String getLocalAddress() {
        return sMyLocalIpAddress;
    }

    private void connectionManagerLog(String msg) {
        LogUtil.v(LogUtil.LOG_TAG, "ConnectionManager." + msg);
    }

    private WifiDirectStateMachine.Action mAction = new WifiDirectStateMachine.Action() {
        private void actionLog(String msg) {
            connectionManagerLog("Action." + msg);
        }

        @Override
        public void initialize() {
            actionLog("initialize");
            mChannel = mWifiP2pManager.initialize(
                    mContext, mHandler.getLooper(), ConnectionManager.this);
        }

        @Override
        public void createGroup() {
            actionLog("createGroup");
            setIsGroupOwner(true);
            mWifiP2pManager.createGroup(mChannel, new ActionListener() {
                @Override
                public void onSuccess() {
                    LogUtil.v(LogUtil.LOG_TAG, "Create success");
                    setIsGroupOwner(true);
                    mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CREATE_GROUP_SUCCESS);
                    requestGroupInfo();

                    // Start new party, so delete old local data.
                    deleteLocalData();
                    // set session info
                    Setting.setJoinedSessionInfo(
                            mContext, getSessionName(), getStartSessionTime(), getMyAddress());
                }

                @Override
                public void onFailure(int reasonCode) {
                    LogUtil.e(LogUtil.LOG_TAG, "Create failed. Reason :" +
                            Utility.getActionListenerStatus(reasonCode));
                    setIsGroupOwner(false);
                    setSessionName("");
                    mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CREATE_GROUP_FAILED);
                }
            });
        }

        @Override
        public void removeGroup() {
            actionLog("removeGroup");
            mWifiP2pManager.removeGroup(mChannel, new ActionListener() {
                @Override
                public void onSuccess() {
                    // End party by host, so delete local data from host.
                    if (isGroupOwner()) {
                        deleteLocalData();
                    }
                    setIsGroupOwner(false);
                }

                @Override
                public void onFailure(int reasonCode) {
                    LogUtil.e(LogUtil.LOG_TAG, "Disconnect failed. Reason :" +
                            Utility.getActionListenerStatus(reasonCode));
                }
            });
        }

        @Override
        public void createGroupResult(final boolean result) {
            actionLog("createGroupResult:" + result);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < mListener.size(); i++) {
                        mListener.get(i).startSession(result);
                    }
                    if (!result) {
                        String msg = mContext.getResources().getString(
                                R.string.party_share_strings_wifi_failure_leave_txt);
                        Utility.showToast(mContext, msg, Toast.LENGTH_LONG);
                    }
                }
            });
        }

        @Override
        public void connect(final WifiP2pSessionInfo sessionInfo) {
            actionLog("connect");
            mErrorReason = CONNECTING;
            mRetryCount = 0;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    joinParty(sessionInfo.device.deviceAddress,
                            mContext.getResources().getString(
                                    R.string.party_share_strings_join_connecting_txt));
                }
            });
        }

        @Override
        public void cancelConnect() {
            actionLog("cancelConnect deviceStatus=" +
                    Utility.getWifiP2pDeviceStatus(getMyDevice().status));
            mHandler.removeCallbacks(mRetryConnectRunnable);
            if (getMyDevice().status == WifiP2pDevice.CONNECTED) {
                removeGroup();
            } else if (getMyDevice().status == WifiP2pDevice.AVAILABLE ||
                    getMyDevice().status == WifiP2pDevice.INVITED) {
                mWifiP2pManager.cancelConnect(mChannel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        LogUtil.e(LogUtil.LOG_TAG, "Connect abort request failed. Reason :" +
                                Utility.getActionListenerStatus(reasonCode));
                        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT);
                    }
                });
            }

        }

        @Override
        public void updateDevice(WifiP2pDevice device) {
            actionLog("updateDevice");
            mMyDevice = device;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateThisDevice(isGroupOwner());
                }
            });
        }

        @Override
        public void requestConnectionInfo() {
            actionLog("requestConnectionInfo");
            mWifiP2pManager.requestConnectionInfo(mChannel, ConnectionManager.this);
        }

        @Override
        public void retryConnect() {
            actionLog("retryConnect");
            mErrorReason = CONNECTING;
            mHandler.removeCallbacks(mRetryConnectRunnable);
            mHandler.postDelayed(mRetryConnectRunnable, 5000);
            mRetryCount++;
        }

        @Override
        public void retryConnect(WifiP2pInfo p2pInfo) {
            actionLog("retryConnect isGroupOwwner:" + p2pInfo.isGroupOwner);
            if (p2pInfo.isGroupOwner) {
                removeGroup();
                reset();
            } else {
                openClientSocket(p2pInfo.groupOwnerAddress);
                requestJoin();
            }
        }

        @Override
        public void startDiscovery() {
            actionLog("startDiscovery");
            setIsGroupOwner(false);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    discoverService();
                }
            });
        }

        @Override
        public void stopDiscovery() {
            actionLog("stopDiscovery");
            setIsGroupOwner(false);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopDiscoverService();
                }
            });
        }

        @Override
        public void stopDiscoveryForWifi() {
            actionLog("stopDiscovery");
            stopDiscoverService();
            mWifiP2pManager.stopPeerDiscovery(mChannel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFailure(int arg0) {
                    }
            });
        }

        @Override
        public void startSession() {
            actionLog("startSession");
            stopDiscoverService();
            startP2pService();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    NotificationUtil.setJoinPartyNotification(
                            mContext, getSessionName(), isGroupOwner());
                }
            });
        }

        @Override
        public void requestJoin() {
            actionLog("requestJoin");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DeviceInfo device = new DeviceInfo(
                            mUserName, getMyAddress(), getCheckAddress(), false);
                    List<DeviceInfo> groupList = new ArrayList<DeviceInfo>();
                    groupList.add(device);
                    PartyShareInfo info = new PartyShareInfo(mAppVersion,
                            PartyShareInfo.CMD_REQUEST_JOIN, groupList, getSessionName());
                    write(info);
                }
            });
        }

        @Override
        public void connectError() {
            actionLog("connectError");
            if (mRetryCount < RETRY_COUNT) {
                retryConnect();
            } else {
                mRetryCount = 0;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < mListener.size(); i++) {
                            mListener.get(i).connectError();
                        }
                    }
                });
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CANCEL);
            }
        }

        @Override
        public void disconnected() {
            actionLog("disconnected:" + mErrorReason);
            final boolean isJoiningSession = WifiDirectStateMachine.isJoiningSession();
            if (mServerSocketThread != null) {
                actionLog("ServerSocketThread end");
                ((ServerSocketThread) mServerSocketThread).finish();
                mServerSocketThread = null;
            }
            if (isGroupOwner()) {
                TrackingUtil.sendShareMusicCount(mContext);
                TrackingUtil.sendSharePhotoCount(mContext);
                TrackingUtil.sendMaxJoinMembers(mContext);
                TrackingUtil.sendPartyTime(mContext,
                        TrackingUtil.ACTION_START_END_TIME);
            } else {
                TrackingUtil.sendPartyTime(mContext,
                        TrackingUtil.ACTION_JOIN_LEAVE_TIME);
            }
            NotificationUtil.removeJoinPartyNotification(mContext);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    String reason = mContext.getResources().getString(
                            R.string.party_share_strings_join_err_wifi_txt);
                    if (mErrorReason == REJECT_MAX) {
                        reason = mContext.getResources().getString(
                                R.string.party_share_strings_member_full_txt);
                    } else if (mErrorReason == NONE) {
                        reason = null;
                        if (isGroupOwner()) {
                            reason = mContext.getResources().getString(
                                    R.string.party_share_strings_wifi_failure_end_txt);
                        } else {
                            if (isJoiningSession) {
                                reason = mContext.getResources().getString(
                                    R.string.party_share_strings_wifi_failure_leave_txt);
                            }
                        }
                        if (reason != null && !reason.isEmpty()) {
                            Utility.showToast(mContext, reason, Toast.LENGTH_LONG);
                        }
                    } else if (mErrorReason == SOCKET_ERROR) {
                        reason = null;
                        if (isGroupOwner()) {
                            reason = mContext.getResources().getString(
                                    R.string.party_share_strings_wifi_failure_end_txt);
                        } else {
                            reason = mContext.getResources().getString(
                                    R.string.party_share_strings_wifi_failure_leave_txt);
                        }
                        if (reason != null && !reason.isEmpty()) {
                            Utility.showToast(mContext, reason, Toast.LENGTH_LONG);
                        }
                    } else if (mErrorReason == REJECT_CLOSED) {
                        reason = mContext.getResources().getString(
                                R.string.party_share_strings_join_err_wifi_txt);
                        if (reason != null && !reason.isEmpty()) {
                            Utility.showToast(mContext, reason, Toast.LENGTH_LONG);
                        }
                        reason = null;
                    }
                    forget();
                    deletePersistentGroup();
                    stopSessionService();
                    stopDiscoverService();
                    reset();
                    wifiLockRelease();
                    for (int i = 0; i < mListener.size(); i++) {
                        mListener.get(i).disconnected(reason);
                    }
                }
            });
        }

        @Override
        public void updateSession(final WifiP2pSessionInfo sessionInfo) {
            actionLog("updateSession");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ConnectionManager.this.updateSession(sessionInfo);
                }
            });
        }

        @Override
        public void changeName(final String userName) {
            actionLog("changeName:" + userName);
            setUserName(userName);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DeviceInfo device = new DeviceInfo(
                            userName, getMyAddress(), getCheckAddress(), isGroupOwner());
                    updateSessionMemberList(device);
                    PartyShareInfo info = new PartyShareInfo(mAppVersion,
                            PartyShareInfo.CMD_CHANGE_NAME, device, getMyAddress());

                    write(info);
                }
            });
        }

        @Override
        public void discoverServices() {
            actionLog("discoverServices");
            mWifiP2pManager.discoverServices(mChannel, new ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int errorCode) {
                    LogUtil.e(LogUtil.LOG_TAG, "Service discovery failed. error="
                            + Utility.getActionListenerStatus(errorCode));
                }
            });
        }

        @Override
        public void openServerSocket(WifiP2pInfo p2pInfo) {
            actionLog("openServerSocket");
            String address = p2pInfo.groupOwnerAddress.getHostAddress();
            for (int i = 0; i < mGroupList.size(); i++) {
                DeviceInfo device = mGroupList.get(i);
                if (device.getDeviceAddress().equals(getMyAddress())) {
                    setLocalAddress(address);
                    device.setIpAddress(address);
                    mGroupList.set(i, device);
                }
            }
            if (mServerSocketThread == null) {
                LogUtil.v(LogUtil.LOG_TAG, "Connected as group owner");
                try {
                    mServerSocketThread = new ServerSocketThread(getCallbackHandler());
                    mServerSocketThread.start();
                    startHttpServer(address);
                } catch (IOException e) {
                    LogUtil.v(LogUtil.LOG_TAG, "Failed to create a server thread - "
                            + e.getMessage());
                }
            }
        }

        @Override
        public void openClientSocket(InetAddress address) {
            actionLog("openClientSocket:" + address.getHostAddress());
            stopDiscoverService();
            Thread handler = new ClientSocketThread(getCallbackHandler(), address);
            handler.start();
            try {
                handler.join();
                actionLog("join");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void reset() {
            actionLog("reset");
            ConnectionManager.this.initialize();
        }

        @Override
        public void notifyAddMember(final DeviceInfo device) {
            actionLog("notifyAddMember:" + device.getUserName() + ", ip:"+ device.getIpAddress());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    write(new PartyShareInfo(mAppVersion, PartyShareInfo.CMD_ADD_MEMBER,
                            getGroupList(), device.getDeviceAddress()),
                            device.getIpAddress(), true);
                    write(new PartyShareInfo(mAppVersion, PartyShareInfo.CMD_ACCEPT_JOIN,
                            getGroupList(), getPassPhrase(), String.valueOf(getNfcJoinPermission())),
                            device.getIpAddress(), false);
                }
            });
            MusicPlayListController.changeName(
                    mContext, device.getDeviceAddress(), device.getUserName());
        }

        @Override
        public void notifyRemoveMember(final DeviceInfo device, final String reason) {
            actionLog("notifyRemoveMember");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    write(new PartyShareInfo(mAppVersion, PartyShareInfo.CMD_REMOVE_MEMBER,
                            getGroupList(), device.getDeviceAddress(), reason),
                            device.getIpAddress(), false);
                }
            });
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    deleteSessionMemberList(device);
                    write(new PartyShareInfo(mAppVersion, PartyShareInfo.CMD_REMOVE_MEMBER,
                            getGroupList(), device.getDeviceAddress(), reason),
                            device.getIpAddress(), true);
                }
            }, 1000);
        }

        @Override
        public void notifyDisconnect() {
            actionLog("notifyDisconnect");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DeviceInfo device = new DeviceInfo(mUserName, getMyAddress(), getCheckAddress(), isGroupOwner());
                    List<DeviceInfo> groupList = new ArrayList<DeviceInfo>();
                    groupList.add(device);
                    PartyShareInfo info = new PartyShareInfo(
                            mAppVersion, PartyShareInfo.CMD_DISCONNECT, groupList);
                    write(info);
                }
            });
        }

        @Override
        public void wifiDisconnect() {
            actionLog("wifiDisconnect");
            WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.disconnect()) {
                LogUtil.v(LogUtil.LOG_TAG, "wifiDisconnect success");
            }
        }

        @Override
        public void keepAlive() {
            actionLog("keepAlive");
            mHandler.removeCallbacks(mKeepAliveRunnable);
            mHandler.postDelayed(mKeepAliveRunnable, POST_DELAY);
        }
    };

    public static synchronized ConnectionManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ConnectionManager(context);
        }

        return sInstance;
    }

    public static synchronized void destroy() {
        sInstance = null;
    }

    private ConnectionManager(Context context) {
        mContext = context;
        mStateMachine = WifiDirectStateMachine.makeWifiDirectStateMachine(mContext, mAction);

        mWifiP2pManager = (WifiP2pManager)mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), null);

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        mWifiDirectReceiver = new WifiDirectBroadcastReceiver(mStateMachine);
    }

    public void initialize() {
        connectionManagerLog("initialize");
        setUserName(Setting.getUserName(mContext));
        mGroupList.clear();
        mSessionList.clear();
        mAppVersion = Utility.getApplicationVersion(mContext);
        mErrorReason = NONE;
        mRetryCount = 0;
        mNetworkId = 0;

        initStaticField();
        closeSocket();
        deleteData();
    }

    private static void initStaticField() {
        setStartSessionTime("");
        setSessionName("");
        setCheckAddress("");
        setMyAddress("");
        setSsid("");
        setPassPhrase("");
        setLocalAddress("");
        setNfcJoinPermission(false);
    }

    public void start() {
        mContext.registerReceiver(mWifiDirectReceiver, mIntentFilter);
    }

    public WifiDirectStateMachine getStateMachine() {
        return mStateMachine;
    }

    public static void setConnectionManagerListener(ConnectionManagerListener listener) {
        for (int i = 0; i < mListener.size(); i++) {
            if (mListener.get(i) == listener) {
                LogUtil.d(LogUtil.LOG_TAG, "already set Listener :" + listener);
                return;
            }
        }
        LogUtil.d(LogUtil.LOG_TAG, "setListener :" + listener);
        mListener.add(listener);
    }

    public static synchronized void removeConnectionManagerListener(
            ConnectionManagerListener listener) {
        if (listener != null) {
            for (int i = 0; i < mListener.size(); i++) {
                if (mListener.get(i) == listener) {
                    LogUtil.d(LogUtil.LOG_TAG, "removeListener :"+ listener);
                    mListener.remove(i);
                }
            }
        }
    }

    public void wifiLock() {
        connectionManagerLog("wifiLock");
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, LogUtil.LOG_TAG);
        mWifiLock.acquire();
    }

    public void wifiLockRelease() {
        connectionManagerLog("wifiLockRelease");
        if (mWifiLock != null) {
            mWifiLock.release();
            mWifiLock = null;
        }
    }

    public void deletePersistentGroup() {
        connectionManagerLog("deletePersistentGroup");
        final ActionListener al = new ActionListener() {
                @Override
                public void onSuccess() {
                    LogUtil.d(LogUtil.LOG_TAG, "deletePersistentGroup onSuccess");
                }

                @Override
                public void onFailure(int errorCode) {
                    LogUtil.e(LogUtil.LOG_TAG, "deletePersistentGroup onFailure : " + errorCode);
                }
        };
        try {
            Method method = WifiP2pManager.class.getMethod("deletePersistentGroup",
                            Channel.class, int.class, ActionListener.class);
            LogUtil.d(LogUtil.LOG_TAG, "mChannel : " + mChannel);
            LogUtil.d(LogUtil.LOG_TAG, "mNetworkId : " + mNetworkId);
            method.invoke(mWifiP2pManager, mChannel, mNetworkId, al);
        } catch (Exception e) {
            LogUtil.w(LogUtil.LOG_TAG, "Exception occurs : " + e.toString());
        }
    }

    public void forget() {
        connectionManagerLog("forget");
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        int networkId = UNKNOWN_NETWORK_ID;
        if (existingConfigs == null) {
            networkId = getWifiNetworkId();
        } else {
            String ssid = getSsid();
            if (ssid != null && !ssid.isEmpty()) {
                for (WifiConfiguration existingConfig : existingConfigs) {
                    if (existingConfig.SSID.equals(ssid)) {
                        networkId = existingConfig.networkId;
                    }
                }
            }
        }
        if (networkId == UNKNOWN_NETWORK_ID) {
            return;
        }
        try {
            Method method = WifiManager.class.getMethod("forget",
                    int.class, Class.forName("android.net.wifi.WifiManager$ActionListener"));
            method.invoke(wifiManager, networkId, null);
            LogUtil.v(LogUtil.LOG_TAG, "id : " + networkId);
        } catch (Exception e) {
            LogUtil.w(LogUtil.LOG_TAG, "Exception occurs : " + e.toString());
        }
    }
    public void deleteData() {
        connectionManagerLog("deleteData");

        if (mHttpd != null) {
            mHttpd.stop();
            mHttpd = null;
        }

        new MusicAsyncQueryHandler(mContext.getContentResolver()).startDelete(
                0, null, MusicProvider.CONTENT_URI, null, null);
        mContext.stopService(new Intent(mContext, MusicService.class));

        // Stop the auto download of photo.
        Intent intent = new Intent(PhotoDownloadService.ACTION_STOP_AUTO_DL);
        mContext.startService(intent);

        // Delete photo table from db.
        new PhotoAsyncQueryHandler(mContext.getContentResolver()).startDelete(
                0, null, PhotoProvider.CONTENT_URI, null, null);
    }

    public void terminate() {
        connectionManagerLog("terminate");
        try {
            mContext.unregisterReceiver(mWifiDirectReceiver);
        } catch (IllegalArgumentException e) {
            LogUtil.w(LogUtil.LOG_TAG, "WifiDirectReceiver was already unregistered.");
        }

        if (mWifiP2pManager != null && mChannel != null) {
            mWifiP2pManager.clearLocalServices(mChannel, new ActionListener () {
                @Override
                public void onSuccess() {
                    LogUtil.v(LogUtil.LOG_TAG, "clear local service");
                }

                @Override
                public void onFailure(int reason) {
                    LogUtil.e(LogUtil.LOG_TAG, "Failed to clear local service");
                }
            });
        }

        mListener.clear();
        stopDiscoverService();
        stopSessionService();
        initialize();
    }

    private void stopDiscoverService() {
        connectionManagerLog("stopDiscoverService");
        if (mServiceRequest != null) {
            if (!mStartDiscovery) {
                return;
            }
            mStartDiscovery = false;
            mWifiP2pManager.removeServiceRequest(mChannel, mServiceRequest, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        LogUtil.v(LogUtil.LOG_TAG, "RemoveServiceRequest success");
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        LogUtil.e(LogUtil.LOG_TAG, "Failed to remove a service error="
                                + Utility.getActionListenerStatus(errorCode));
                    }
                });
            mServiceRequest = null;
        } else {
            LogUtil.v(LogUtil.LOG_TAG, "already stopped");
        }
        mHandler.removeCallbacks(mRetryDiscoveryRunnable);
    }

    private void stopSessionService() {
        connectionManagerLog("stopSessionService");
        if (mMainService != null) {
            mWifiP2pManager.removeLocalService(mChannel, mMainService, new ActionListener() {
                @Override
                public void onSuccess() {
                    LogUtil.v(LogUtil.LOG_TAG, "removed service : " + SERVICE_INSTANCE_MAIN);
                }

                @Override
                public void onFailure(int errorCode) {
                    LogUtil.e(LogUtil.LOG_TAG,
                            "Failed to remove a service : " + SERVICE_INSTANCE_MAIN);
                }
            });
            mMainService = null;
        }
    }

    private void discoverService() {
        connectionManagerLog("discoverService");
        if (mStartDiscovery) {
            return;
        }
        mStartDiscovery = true;
        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
        mWifiP2pManager.setDnsSdResponseListeners(mChannel,
                new DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                            String registrationType, WifiP2pDevice srcDevice) {
                    }
                }, this);

        // After attaching listeners, create a service request and initiate
        // discovery.
        mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance(
                SERVICE_INSTANCE_MAIN, SERVICE_REG_TYPE);
        mWifiP2pManager.addServiceRequest(mChannel, mServiceRequest, new ActionListener() {
            @Override
            public void onSuccess() {
                LogUtil.v(LogUtil.LOG_TAG, "Added service discovery request");
            }

            @Override
            public void onFailure(int arg0) {
                LogUtil.e(LogUtil.LOG_TAG, "Failed adding service discovery request");
            }
        });
        mHandler.removeCallbacks(mRetryDiscoveryRunnable);
        mHandler.postDelayed(mRetryDiscoveryRunnable, UPDATE_TIME / 6);
    }

    private void startP2pService() {
        connectionManagerLog("startP2pService");
        setUserName(Setting.getUserName(mContext));

        stopSessionService();

        String time = getUtc();
        setStartSessionTime(time);
        Setting.setPhotoSaveFolder(mContext, PhotoUtils.createSaveFolderPath(mContext));

        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_KEY_SESSION, getSessionName());
        record.put(TXTRECORD_KEY_USERNAME, mUserName);
        record.put(TXTRECORD_KEY_TIME, time);
        record.put(TXTRECORD_KEY_VERSION, mAppVersion);

        LogUtil.v(LogUtil.LOG_TAG, "user:" + mUserName +
                ", session:" + getSessionName() +
                ", time:" + time +
                ", version:" + mAppVersion);

        mMainService = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE_MAIN, SERVICE_REG_TYPE, record);

        mWifiP2pManager.addLocalService(mChannel, mMainService, new ActionListener() {
            @Override
            public void onSuccess() {
                LogUtil.v(LogUtil.LOG_TAG,
                        "Added Local Service : " + SERVICE_INSTANCE_MAIN);
            }

            @Override
            public void onFailure(int errorCode) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "Failed to add a service : " + SERVICE_INSTANCE_MAIN);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private String getUtc() {
        Date date = new Date();
        String dateTime = String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", date);
        LogUtil.v(LogUtil.LOG_TAG, "dateTime:"+dateTime);
        return dateTime;
    }

    private int timeComparison(String comparisonSource, String comparedTime) {
        if (comparisonSource != null && comparedTime != null) {
            if (comparisonSource.length() < 13 || comparedTime.length() < 13) {
                return UPDATE_OK_DATAONLY;
            }
            int year = Integer.parseInt(comparisonSource.substring(0, 4));
            int month = Integer.parseInt(comparisonSource.substring(4, 6));
            int day = Integer.parseInt(comparisonSource.substring(6, 8));
            int hour = Integer.parseInt(comparisonSource.substring(8, 10));
            int minute = Integer.parseInt(comparisonSource.substring(10, 12));
            int second = Integer.parseInt(comparisonSource.substring(12, 14));
            Calendar calBase = new GregorianCalendar(year, month - 1, day, hour, minute, second);

            year = Integer.parseInt(comparedTime.substring(0, 4));
            month = Integer.parseInt(comparedTime.substring(4, 6));
            day = Integer.parseInt(comparedTime.substring(6, 8));
            hour = Integer.parseInt(comparedTime.substring(8, 10));
            minute = Integer.parseInt(comparedTime.substring(10, 12));
            second = Integer.parseInt(comparedTime.substring(12, 14));
            Calendar calCompared = new GregorianCalendar(year, month - 1, day, hour, minute, second);

            int diff = calBase.compareTo(calCompared);
            if (diff == 0){
                return UPDATE_OK_DATAONLY;
            } else if (diff > 0) {
                return UPDATE_NOT_REQUIRED;
            } else {
                return UPDATE_OK_NOTIFY;
            }
        } else {
            return UPDATE_OK_DATAONLY;
        }
    }

    private class RetryDiscoveryRunnable implements Runnable {
        public void run() {
            LogUtil.v(LogUtil.LOG_TAG, "RetryDiscoveryRunnable");
            boolean forcus = false;
            for (int i = 0; i < mListener.size(); i++) {
                forcus = mListener.get(i).isForeground();
                if (forcus) {
                    break;
                }
            }
            if (forcus) {
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCOVER);
            }

            long baseTime = System.currentTimeMillis();
            for (int i = 0; i < mSessionList.size(); i++) {
                WifiP2pSessionInfo session = mSessionList.get(i);
                if ((baseTime - session.keepAlive) > KEEP_ALIVE) {
                    mSessionList.remove(i);
                }
            }
            for (int i = 0; i < mListener.size(); i++) {
                mListener.get(i).updateSession(getSessionList());
            }

            mHandler.postDelayed(mRetryDiscoveryRunnable, UPDATE_TIME);
        }
    }

    private class RetryConnectRunnable implements Runnable {
        public void run() {
            LogUtil.v(LogUtil.LOG_TAG, "RetryConnectRunnable");
            joinParty(mTargetDeviceAddress,
                    mContext.getResources().getString(
                            R.string.party_share_strings_join_retry_txt));
        }
    }

    public void requestGroupInfo() {
        connectionManagerLog("requestGroupInfo");
        mWifiP2pManager.requestGroupInfo(mChannel, new GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                LogUtil.v(LogUtil.LOG_TAG, "onGroupInfoAvailable");
                if (group == null) {
                    LogUtil.v(LogUtil.LOG_TAG, "group is NULL.");
                    return;
                }

                try {
                    Method method = WifiP2pGroup.class.getMethod("getNetworkId");
                    mNetworkId = (Integer) method.invoke(group);
                    LogUtil.v(LogUtil.LOG_TAG, "networkId : " + mNetworkId);
                } catch (Exception e) {
                    LogUtil.w(LogUtil.LOG_TAG, "Exception occurs : " + e.toString());
                }

                setSsid(group.getNetworkName());
                setPassPhrase(group.getPassphrase());
                List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
                peers.addAll(group.getClientList());
                for (int i = 0; i < peers.size(); i++) {
                    LogUtil.v(LogUtil.LOG_TAG, "device:" + peers.get(i).deviceAddress);
                }
                checkSessionMemberList(peers);
            }
        });
    }

    public static void setSessionName(String sessionName) {
        LogUtil.v(LogUtil.LOG_TAG, "ConnectionManager.setSessionName:" + sessionName);
        sSessionName = (sessionName != null) ? sessionName : "";
    }
    public static String getSessionName() {
        return sSessionName;
    }

    public static void setStartSessionTime(String time) {
        sStartSessionTime = time;
    }
    public static String getStartSessionTime() {
        return sStartSessionTime;
    }

    public static void setCheckAddress(String address) {
        LogUtil.v(LogUtil.LOG_TAG, "ConnectionManager.setCheckAddress:" + address);
        sCheckAddress = address;
    }

    public static String getCheckAddress() {
        return sCheckAddress;
    }

    public static void setMyAddress(String address) {
        LogUtil.v(LogUtil.LOG_TAG, "ConnectionManager.setMyAddress:" + address);
        sMyAddress = address;
    }

    public static String getMyAddress() {
        return sMyAddress;
    }

    public void setUserName(String userName) {
        connectionManagerLog("setUserName :" + userName);
        mUserName = userName;
    }

    public void updateSession(WifiP2pSessionInfo sessionInfo) {
        connectionManagerLog("updateSession");
        sessionInfo.keepAlive = System.currentTimeMillis();
        for (int i = 0; i < mSessionList.size(); i++) {
            WifiP2pSessionInfo session = mSessionList.get(i);
            if (sessionInfo.device.deviceAddress.equals(session.device.deviceAddress)) {
                int updateCheck = timeComparison(session.time, sessionInfo.time);
                if (updateCheck == UPDATE_OK_DATAONLY) {
                    mSessionList.set(i, sessionInfo);
                    return;
                } else if (updateCheck == UPDATE_NOT_REQUIRED) {
                    return;
                } else {
                    mSessionList.set(i, sessionInfo);
                    for (int j = 0; j < mListener.size(); j++) {
                        mListener.get(j).updateSession(getSessionList());
                    }
                    return;
                }
            }
        }
        mSessionList.add(sessionInfo);
        for (int i = 0; i < mListener.size(); i++) {
            mListener.get(i).updateSession(getSessionList());
        }
    }

    private class KeepAliveRunnable implements Runnable {
        public void run() {
            DeviceInfo device = new DeviceInfo(
                    mUserName, getMyAddress(), getCheckAddress(), isGroupOwner());
            List<DeviceInfo> groupList = new ArrayList<DeviceInfo>();
            groupList.add(device);
            PartyShareInfo info = new PartyShareInfo(
                    mAppVersion, PartyShareInfo.CMD_KEEP_ALIVE, groupList);
            write(info);
        }
    }

    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */
    public void updateThisDevice(boolean host) {
        setMyAddress(mMyDevice.deviceAddress);
        setCheckAddress(mMyDevice.deviceAddress);
        updateSessionMemberList(mUserName, getMyAddress(), host);
    }

    public WifiP2pDevice getMyDevice() {
        return mMyDevice;
    }

    public List<DeviceInfo> getGroupList() {
        return mGroupList;
    }

    public List<WifiP2pSessionInfo> getSessionList() {
        return mSessionList;
    }

    public void updateSessionMemberList(String userName, String deviceAddress) {
        updateSessionMemberList(userName, deviceAddress, isGroupOwner());
    }

    public void updateSessionMemberList(String userName, String deviceAddress, boolean host) {
        DeviceInfo device = new DeviceInfo(userName, deviceAddress, deviceAddress, host);
        updateSessionMemberList(device);
    }

    private void updateSessionMemberList(DeviceInfo device) {
        connectionManagerLog("updateSessionMemberList update user=" +
                device.getUserName() + ", address=" +
                device.getDeviceAddress() + ", ip=" +
                device.getIpAddress() + ", checkAddress=" +
                device.getCheckAddress() + ", host=" +
                device.getIsHost());
        for (int i = 0; i < mGroupList.size(); i++) {
            if (device.getDeviceAddress().equals(mGroupList.get(i).getDeviceAddress())) {
                if (device.getIpAddress().isEmpty() &&
                        !mGroupList.get(i).getIpAddress().isEmpty()) {
                    connectionManagerLog("updateSessionMemberList update IpAddress");
                    device.setIpAddress(mGroupList.get(i).getIpAddress());
                }
                mGroupList.set(i, device);
                for (int j = 0; j < mListener.size(); j++) {
                    mListener.get(j).updateMemberList(getGroupList());
                }
                return;
            }
        }
        mGroupList.add(device);
        for (int i = 0; i < mListener.size(); i++) {
            mListener.get(i).updateMemberList(getGroupList());
        }
    }

    public void deleteSessionMemberList(DeviceInfo deviceInfo) {
        String deviceAddress = deviceInfo.getDeviceAddress();
        for (int i = 0; i < mGroupList.size(); i++) {
            if (deviceAddress.equals(mGroupList.get(i).getDeviceAddress())) {
                connectionManagerLog("deleteSessionMemberList delete user=" +
                        mGroupList.get(i).getUserName());
                mGroupList.remove(i);
                closeSocket(deviceInfo.getIpAddress());

                if (isGroupOwner()) {
                    // Stop music if current track is this member.
                    Intent intent = new Intent(mContext, MusicService.class);
                    intent.putExtra("delete_member", deviceAddress);
                    intent.setAction(MusicService.ACTION_DELETE_MEMBER);
                    mContext.startService(intent);
                }

                for (int j = 0; j < mListener.size(); j++) {
                    mListener.get(j).updateMemberList(getGroupList());
                }
                mContext.getContentResolver().notifyChange(
                        MusicProvider.CONTENT_URI_REFRESH, null);
                return;
            }
        }
    }

    public void checkSessionMemberList(List<WifiP2pDevice> peers) {
        connectionManagerLog("checkSessionMemberList");
        int listSize = peers.size();
        if (0 < listSize) {
            for (int i = 0; i < mGroupList.size(); i++) {
                String deviceAddress = mGroupList.get(i).getCheckAddress();
                if (deviceAddress.equals(getCheckAddress())) {
                    continue;
                }
                boolean check = false;
                for (int j = 0; j < listSize; j++) {
                    if (deviceAddress.equals(peers.get(j).deviceAddress)) {
                        check = true;
                        break;
                    }
                }
                if (!check) {
                    connectionManagerLog("remove member:" + mGroupList.get(i).getUserName());
                    mStateMachine.sendMessage(
                            WifiDirectStateMachine.CMD_NOTIFY_REMOVE_MEMBER, mGroupList.get(i));
                }
            }
        } else {
            for (int i = 0; i < mGroupList.size(); i++) {
                String deviceAddress = mGroupList.get(i).getCheckAddress();
                if (deviceAddress.equals(getCheckAddress())) {
                    continue;
                }
                deleteSessionMemberList(mGroupList.get(i));
            }
        }
        mAction.keepAlive();
    }

    public void endParty() {
        connectionManagerLog("endParty");
        mErrorReason = MYSELF;
        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_NOTIFY_DISCONNECT);
        if (isGroupOwner()) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopSessionService();
                    mAction.removeGroup();
                }
            }, 1000);
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT);
                }
            }, 1000);
        }
    }

    public void joinParty(final String deviceAddress, final String msg) {
        connectionManagerLog("joinParty");
        stopDiscoverService();

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent =  0;
        mWifiP2pManager.connect(mChannel, config, new ActionListener() {
                @Override
                public void onSuccess() {
                    LogUtil.v(LogUtil.LOG_TAG, "Connecting to host:" + deviceAddress);
                    for (int i = 0; i < mListener.size(); i++) {
                        mListener.get(i).connect(msg);
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    LogUtil.e(LogUtil.LOG_TAG,
                            "Failed connecting to service:" +
                                    Utility.getActionListenerStatus(errorCode));
                    mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT);
                }
            });
        mTargetDeviceAddress = deviceAddress;
    }

    private void write(final PartyShareInfo info) {
        for (int i = 0; i < mTransfers.size(); i++) {
            final Transfer transfer = mTransfers.get(i);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    transfer.write(info);
                }
            });
        }
    }

    private void write(final PartyShareInfo info, final String ipAddress, final boolean exclude) {
        for (int i = 0; i < mTransfers.size(); i++) {
            final Transfer transfer = mTransfers.get(i);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (exclude && !transfer.getIpAddress().equalsIgnoreCase(ipAddress)) {
                        transfer.write(info);
                    } else if (!exclude && transfer.getIpAddress().equalsIgnoreCase(ipAddress)) {
                        transfer.write(info);
                    }
                }
            });
        }
    }

    private void closeSocket(String ipAddress) {
        connectionManagerLog("closeSocket targetIP:" + ipAddress);

        for (int i = 0; i < mTransfers.size(); i++) {
            Transfer transfer = mTransfers.get(i);
            if (transfer.getIpAddress().equals(ipAddress)) {
                connectionManagerLog("closeSocket success");
                transfer.close();
                mTransfers.remove(i);
            }
        }
    }

    private void closeSocket() {
        connectionManagerLog("closeSocket all");
        for (int i = 0; i < mTransfers.size(); i++) {
            Transfer transfer = mTransfers.get(i);
            transfer.close();
        }
        mTransfers.clear();
    }

    private String getIpAddress(String deviceAddress) {
        for (int i = 0; i < mGroupList.size(); i++) {
            DeviceInfo device = mGroupList.get(i);
            if (deviceAddress.equals(device.getDeviceAddress())) {
                return device.getIpAddress();
            }
        }
        return "";
    }

    private void receivesMessage(PartyShareInfo info) {
        int command = info.getCommand();

        if (PartyShareInfo.CMD_REQUEST_JOIN == command) {
            List<DeviceInfo> list = info.getGroupList();
            String sessionName = info.getAttribute();
            for (int i = 0; i < list.size(); i++) {
                final DeviceInfo device = list.get(i);
                LogUtil.v(LogUtil.LOG_TAG, "join user=" + device.getUserName());

                String reason = "";
                if (!getSessionName().equals(sessionName)) {
                    reason = PartyShareInfo.DISCONNECT_REASON_CLOSED;
                }
                if (MAX <= mGroupList.size()) {
                    reason = PartyShareInfo.DISCONNECT_REASON_MAX;
                }
                if (!reason.isEmpty()) {
                    write(new PartyShareInfo(mAppVersion,
                            PartyShareInfo.CMD_REJECT_JOIN, reason),
                            device.getIpAddress(),
                            false);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            closeSocket(device.getIpAddress());
                        }
                    }, 500);
                    return;
                }
                updateSessionMemberList(device);
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_NOTIFY_ADD_MEMBER, device);
            }
        } else if (PartyShareInfo.CMD_ACCEPT_JOIN == command) {
            startHttpServer(getIpAddress(getMyAddress()));
            if (!Utility.isSavedSession(
                    mContext, getSessionName(), getStartSessionTime(), getMyAddress())) {
                // Not the last Party, so delete old local data.
                deleteLocalData();
                // set session info
                Setting.setJoinedSessionInfo(
                        mContext, getSessionName(), getStartSessionTime(), getMyAddress());

                new PostContent(mContext).deleteDataFromHost(getMyAddress());
            }
            mErrorReason = NONE;
            String passPhrase = info.getAttribute();
            setPassPhrase(passPhrase);
            boolean premission = Boolean.valueOf(info.getAttribute2()).booleanValue();
            setNfcJoinPermission(premission);

            Setting.setPhotoSaveFolder(mContext, PhotoUtils.createSaveFolderPath(mContext));

            List<DeviceInfo> list = info.getGroupList();
            for (int i = 0; i < list.size(); i++) {
                DeviceInfo device = list.get(i);
                LogUtil.v(LogUtil.LOG_TAG, "user name:" + device.getUserName());
                if (device.getDeviceAddress().equals(getMyAddress())) {
                    device.setIpAddress(getLocalAddress());
                }
            }
            mGroupList.clear();
            mGroupList.addAll(list);
            for (int i = 0; i < mListener.size(); i++) {
                mListener.get(i).acceptJoin();
            }
            NotificationUtil.setJoinPartyNotification(
                    mContext, getSessionName(), isGroupOwner());

            // Send event to all the guests.
            new PostContent(mContext).join(null);
        } else if (PartyShareInfo.CMD_DISCONNECT == command) {
            List<DeviceInfo> list = info.getGroupList();
            for (int i = 0; i < list.size(); i++) {
                DeviceInfo device = list.get(i);
                String deviceAddress = device.getDeviceAddress();
                device.setIpAddress(getIpAddress(device.getDeviceAddress()));

                if (isGroupOwner()) {
                    mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT, device);
                } else {
                    if (deviceAddress.equals(getMyAddress())) {
                        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT);
                    } else if (device.getIsHost()) {
                        mErrorReason = MYSELF;
                        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT);
                        String msg = mContext.getResources().getString(
                                        R.string.party_share_strings_end_party_toast_txt);
                        Utility.showToast(mContext, msg, Toast.LENGTH_LONG);
                        NotificationUtil.setEndPartyNotification(mContext, getSessionName());

                        // End party, so delete local data from client.
                        deleteLocalData();
                    }
                }
            }
        } else if (PartyShareInfo.CMD_CHANGE_NAME == command) {
            List<DeviceInfo> list = info.getGroupList();
            for (int i = 0; i < list.size(); i++) {
                DeviceInfo device = list.get(i);
                updateSessionMemberList(device);
                if (isGroupOwner()) {
                    write(new PartyShareInfo(mAppVersion, PartyShareInfo.CMD_CHANGE_NAME, list));
                }
                MusicPlayListController.changeName(
                        mContext, device.getDeviceAddress(), device.getUserName());
            }
        } else if (PartyShareInfo.CMD_ADD_MEMBER == command) {
            List<DeviceInfo> list = info.getGroupList();
            mGroupList.clear();
            mGroupList.addAll(list);
            for (int i = 0; i < mListener.size(); i++) {
                mListener.get(i).updateMemberList(getGroupList());
            }
            String address = info.getAttribute();
            MusicPlayListController.changeName(mContext, address, null);
            // Send event to the new guest.
            new PostContent(mContext).join(address);
        } else if (PartyShareInfo.CMD_REMOVE_MEMBER == command) {
            String address = info.getAttribute();
            String reason = info.getAttribute2();
            List<DeviceInfo> list = info.getGroupList();
            if (address.equals(getMyAddress())) {
                if (reason.equals(PartyShareInfo.DISCONNECT_REASON_BY_HOST)) {
                    mErrorReason = MYSELF;
                    String msg = String.format(
                            mContext.getResources().getString(
                                    R.string.party_share_strings_remove_toast_txt),
                                    getSessionName());
                    Utility.showToast(mContext, msg, Toast.LENGTH_LONG);
                }
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT);
            } else {
                mGroupList.clear();
                mGroupList.addAll(list);
                for (int i = 0; i < mListener.size(); i++) {
                    mListener.get(i).updateMemberList(getGroupList());
                }
            }
        } else if (PartyShareInfo.CMD_REJECT_JOIN == command) {
            String reason = info.getAttribute();
            if (reason.equals(PartyShareInfo.DISCONNECT_REASON_MAX)) {
                mErrorReason = REJECT_MAX;
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT);
            } else if (reason.equals(PartyShareInfo.DISCONNECT_REASON_CLOSED)) {
                mErrorReason = REJECT_CLOSED;
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT);
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        Object obj = msg.obj;
        Transfer transfer = null;
        String ipAddress;
        switch (msg.what) {
        case MESSAGE_READ:
            connectionManagerLog("handleMessage.MESSAGE_READ");
            if (PartyShareInfo.class.isInstance(obj)) {
                PartyShareInfo info = (PartyShareInfo) obj;
                receivesMessage(info);
            }
            break;

        case MY_HANDLE:
            connectionManagerLog("handleMessage.MY_HANDLE");
            if (Transfer.class.isInstance(obj)) {
                transfer = (Transfer)obj;
            }
            if (transfer != null) {
                if (!isGroupOwner()) {
                    setLocalAddress(transfer.getLocalIpAddress());

                    DeviceInfo device = new DeviceInfo(
                            mUserName, getMyAddress(), getCheckAddress(), false);
                    device.setIpAddress(getLocalAddress());
                    updateSessionMemberList(device);
                }

                ipAddress = transfer.getIpAddress();
                for (int i = 0; i < mTransfers.size(); i++) {
                    if (mTransfers.get(i).getIpAddress().equals(ipAddress)) {
                        mTransfers.set(i, transfer);
                        return true;
                    }
                }
                mTransfers.add(transfer);
            }
            break;

        case ERROR:
            connectionManagerLog("handleMessage.ERROR");
            if (Transfer.class.isInstance(obj)) {
                transfer = (Transfer)obj;
            }
            if (transfer != null) {
                ipAddress = transfer.getIpAddress();
                LogUtil.v(LogUtil.LOG_TAG, "Error ipAddress:" + ipAddress);

                if (isGroupOwner()) {
                    for (int i = 0; i < mGroupList.size(); i++) {
                        DeviceInfo device = mGroupList.get(i);
                        if (device.getIpAddress().equals(ipAddress)) {
                            deleteSessionMemberList(device);
                            mStateMachine.sendMessage(
                                    WifiDirectStateMachine.CMD_NOTIFY_REMOVE_MEMBER, device);
                        }
                    }
                } else {
                    if (mErrorReason != MYSELF) {
                        mErrorReason = SOCKET_ERROR;
                    }

                    closeSocket(ipAddress);
                    mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT,
                            new DeviceInfo(
                                    mUserName, getMyAddress(), getCheckAddress(), isGroupOwner()));
                }
            } else {
                if (mErrorReason != MYSELF) {
                    mErrorReason = SOCKET_ERROR;
                }
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECT,
                        new DeviceInfo(
                                mUserName, getMyAddress(), getCheckAddress(), isGroupOwner()));
            }
            break;

        default:
            break;
        }
        return true;
    }

    /*************** Listener ******************/

    @Override
    public void onChannelDisconnected() {
        connectionManagerLog("onChannelDisconnected");
        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_DISCONNECTED);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        connectionManagerLog("onConnectionInfoAvailable");
        if (getSessionName().isEmpty()) {
            mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CANCEL);
        } else {
            mStateMachine.sendMessage(
                    WifiDirectStateMachine.CMD_ON_CONNECTION_INFO_AVAILABLE, info);
            setGroupOwnerAddress(info.groupOwnerAddress.getHostAddress());
            requestGroupInfo();
        }
    }

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String,
            String> record, WifiP2pDevice device) {
        LogUtil.v(LogUtil.LOG_TAG, "fullDomainName : " + fullDomainName);

        String instanceName = null;
        if (fullDomainName != null) {
            instanceName = fullDomainName.substring(0, fullDomainName.indexOf("."));
        }
        if (instanceName == null) {
            return;
        }

        if (instanceName.startsWith(SERVICE_INSTANCE_MAIN.toLowerCase(Locale.getDefault())) &&
                !isGroupOwner()) {
            String username = record.get(TXTRECORD_KEY_USERNAME);
            String sessionName = record.get(TXTRECORD_KEY_SESSION);
            String time = record.get(TXTRECORD_KEY_TIME);
            String appVersion = record.get(TXTRECORD_KEY_VERSION);
            LogUtil.v(LogUtil.LOG_TAG, "user:" + username +
                    ", session:" + sessionName +
                    ", time:" + time);

            WifiP2pSessionInfo sessionInfo = new WifiP2pSessionInfo();
            sessionInfo.device = device;
            sessionInfo.userName = username;
            sessionInfo.sessionName = sessionName;
            sessionInfo.time = time;
            sessionInfo.applicationVersion = appVersion;

            mStateMachine.sendMessage(
                    WifiDirectStateMachine.CMD_ON_DNS_SB_TXT_RECORD_AVAILABLE, sessionInfo);

        }
    }

    private void startHttpServer(String ipAddress) {
        try {
            LogUtil.d(LogUtil.LOG_TAG,
                    "ConnectionManager.startHttpServer ipAddress : " + ipAddress);
            if (mHttpd == null) {
                mHttpd = new PartyShareHttpd(mContext, PartyShareHttpd.PORT, ipAddress);
                mHttpd.start();
            }
        } catch (IOException e) {
            LogUtil.v(LogUtil.LOG_TAG, "IOException!!!");
            e.printStackTrace();
        }
    }

    /**
     * Delete local data.
     * - End party.
     * - Join to new party.
     * - Create new party.
     */
    private void deleteLocalData() {
        if (isGroupOwner()) {
            // Delete json file.
            JsonUtil.deleteJsonFile(mContext, JsonUtil.CONTENT_TYPE_MUSIC);
            JsonUtil.deleteJsonFile(mContext, JsonUtil.CONTENT_TYPE_PHOTO);
        }
        // Delete local_music table.
        new MusicAsyncQueryHandler(mContext.getContentResolver()).startDelete(
                0, null, MusicProvider.CONTENT_URI_LOCAL_MUSIC, null, null);
        // Delete local_photo table.
        new PhotoAsyncQueryHandler(mContext.getContentResolver()).startDelete(
                0, null, PhotoProvider.CONTENT_URI_LOCAL_PHOTO, null, null);
        // Delete thumbnail folder.
        PhotoUtils.deleteThumbnailFolder(mContext.getApplicationContext());
    }

    public interface ConnectionManagerListener {
        public void updateSession(List<WifiP2pSessionInfo> sessionList);
        public void updateMemberList(List<DeviceInfo> groupList);
        public void disconnected(String reason);
        public void connectError();
        public void acceptJoin();
        public void connect(String msg);
        public void startSession(boolean result);
        public boolean isForeground();
    }
}
