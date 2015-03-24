/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.session;

import android.os.Handler;

import com.sonymobile.partyshare.util.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

public class Transfer implements Runnable {

    private static final int BUFFER_SIZE = 512 * 10;

    private Socket mSocket = null;
    private Handler mHandler;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    public Transfer(Socket socket, Handler handler) {
        mSocket = socket;
        mHandler = handler;
        mHandler.obtainMessage(ConnectionManager.MY_HANDLE, this).sendToTarget();
        try {
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "Transfer IOException : " + e);
        }
    }

    @Override
    public void run() {
        int bytes;

        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        try {
            while (true) {
                if (mInputStream == null || mOutputStream == null || mSocket == null) {
                    LogUtil.e(LogUtil.LOG_TAG, "Stream or Socket is null.");
                    return;
                }
                // Read from the InputStream
                byte[] buffer = new byte[BUFFER_SIZE];
                bytes = mInputStream.read(buffer);
                LogUtil.v(LogUtil.LOG_TAG, "bytes:" + bytes);

                if (bytes == -1) {
                    break;
                }

                bais = new ByteArrayInputStream(buffer);
                ois = new ObjectInputStream(bais);

                PartyShareInfo info = (PartyShareInfo)ois.readObject();
                if (info != null) {
                    LogUtil.v(LogUtil.LOG_TAG, "Command:" +
                            PartyShareInfo.getCommand(info.getCommand()) + " from:" +
                            getIpAddress());
                    if (PartyShareInfo.CMD_REQUEST_JOIN == info.getCommand()) {
                        LogUtil.v(LogUtil.LOG_TAG, "set join user ip");
                        List<DeviceInfo> list = info.getGroupList();
                        for (int i = 0; i < list.size(); i++) {
                            DeviceInfo device = list.get(i);
                            device.setIpAddress(getIpAddress());
                            info.setDeviceInfo(device);
                        }
                    }
                    mHandler.obtainMessage(
                            ConnectionManager.MESSAGE_READ, info).sendToTarget();
                } else {
                    mHandler.obtainMessage(ConnectionManager.ERROR, this).sendToTarget();
                }
            }
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "disconnected:" + e);
        } catch (ClassNotFoundException e) {
            LogUtil.e(LogUtil.LOG_TAG, "class not found:" + e);
        } catch (NullPointerException e) {
            LogUtil.e(LogUtil.LOG_TAG, "Object is null:" + e);
        } finally {
            LogUtil.v(LogUtil.LOG_TAG, "finally");
            mHandler.obtainMessage(ConnectionManager.ERROR, this).sendToTarget();
            if (bais != null) {
                try {
                    bais.close();
                } catch (Exception e) {
                    LogUtil.e(LogUtil.LOG_TAG, "ByteArrayInputStream.close() Exception : " + e);
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (Exception e) {
                    LogUtil.e(LogUtil.LOG_TAG, "ObjectInputStream.close() Exception : " + e);
                }
            }
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (Exception e) {
                    LogUtil.e(LogUtil.LOG_TAG, "InputStream.close() Exception : " + e);
                }
            }
            if (mOutputStream != null) {
                try {
                    mOutputStream.close();
                } catch (Exception e) {
                    LogUtil.e(LogUtil.LOG_TAG, "OutputStream.close() Exception : " + e);
                }
            }
            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (Exception e) {
                    LogUtil.e(LogUtil.LOG_TAG, "Socket close Exception : " + e);
                }
            }
        }
    }

    public String getIpAddress() {
        if (mSocket == null) {
            LogUtil.w(LogUtil.LOG_TAG, "Socket is null.");
            return "";
        }
        InetAddress address = mSocket.getInetAddress();
        if (address != null) {
            return address.getHostAddress();
        } else {
            LogUtil.v(LogUtil.LOG_TAG, "this socket is not yet connected.");
            return "";
        }
    }

    public String getLocalIpAddress() {
        if (mSocket == null) {
            LogUtil.w(LogUtil.LOG_TAG, "Socket is null.");
            return "";
        }
        InetAddress address = mSocket.getLocalAddress();
        if (address != null) {
            return address.getHostAddress();
        } else {
            LogUtil.v(LogUtil.LOG_TAG, "this socket is not yet connected.");
            return "";
        }
    }

    public void close() {
        LogUtil.d(LogUtil.LOG_TAG, "Transfer.close() called");
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (Exception e) {
                LogUtil.e(LogUtil.LOG_TAG, "InputStream.close() Exception : " + e);
            }
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (Exception e) {
                LogUtil.e(LogUtil.LOG_TAG, "OutputStream.close() Exception : " + e);
            }
        }
        if (mSocket != null) {
            LogUtil.v(LogUtil.LOG_TAG, "socket close");
            try {
                mSocket.close();
            } catch (Exception e) {
                LogUtil.e(LogUtil.LOG_TAG, "Socket close Exception : " + e);
            }
        }
        mInputStream = null;
        mOutputStream = null;
        mSocket = null;
    }

    public synchronized void write(PartyShareInfo info) {
        if (mOutputStream == null) {
            LogUtil.e(LogUtil.LOG_TAG, "OutputStream is null.");
            mHandler.obtainMessage(ConnectionManager.ERROR, this).sendToTarget();
            return;
        }
        LogUtil.v(LogUtil.LOG_TAG, "write command:" +
                PartyShareInfo.getCommand(info.getCommand()) + " to:" +
                getIpAddress());
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = null;
        byte[] buffer = null;

        try {
            objectOutput = new ObjectOutputStream(byteArray);
            objectOutput.writeObject(info);
            buffer = byteArray.toByteArray();
            LogUtil.v(LogUtil.LOG_TAG, "buffer:" + buffer.length);
            mOutputStream.write(buffer);
            mOutputStream.flush();
            threadSleep(200);
        } catch (Exception e) {
            LogUtil.e(LogUtil.LOG_TAG, "Exception during write:" + e);
            mHandler.obtainMessage(ConnectionManager.ERROR, this).sendToTarget();
        } finally {
            if (objectOutput != null) {
                try {
                    objectOutput.close();
                } catch (Exception e) {
                    LogUtil.e(LogUtil.LOG_TAG, "ObjectOutputStream close failed : " + e);
                }
            }
            if (byteArray != null) {
                try {
                    byteArray.close();
                } catch (Exception e) {
                    LogUtil.e(LogUtil.LOG_TAG, "ByteArrayOutputStream close failed : " + e);
                }
            }
        }
    }

    private void threadSleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
