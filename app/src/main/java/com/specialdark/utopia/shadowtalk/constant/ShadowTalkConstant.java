package com.specialdark.utopia.shadowtalk.constant;

/**
 * Created by wei on 2015/3/30/0030.
 */
public class ShadowTalkConstant {

    //Intent action
    public static final String ACTION_BLUETOOTH_CHAT_ACTIVITY = "com.specialdark.utopia.shadowtalk.BLUETOOTH_CHAT";
    public static final String ACTION_BLUETOORH_DEVICE_LIST_ACTIVITY = "com.specialdark.utopia.shadowtalk.BLUETOOTH_DEVICE_LIST";


    //Request code
    public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    public static final int REQUEST_ENABLE_BT = 3;

    //Message type sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

}
