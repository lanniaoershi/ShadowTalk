/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.session;

import android.os.Handler;

import com.sonymobile.partyshare.util.LogUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketThread extends Thread {
    private static final int CONNECT_TIMEOUT = 5000;
    private Handler mHandler;
    private Transfer mTransfer;
    private InetAddress mAddress;

    public ClientSocketThread(Handler handler, InetAddress groupOwnerAddress) {
        this.mHandler = handler;
        this.mAddress = groupOwnerAddress;
    }

    @Override
    public void run() {
        if (mAddress != null) {
            Socket socket = new Socket();
            try {
                String address = mAddress.getHostAddress();
                socket.bind(null);
                socket.connect(new InetSocketAddress(
                        address, ConnectionManager.SERVER_PORT), CONNECT_TIMEOUT);
                LogUtil.d(LogUtil.LOG_TAG, "Client Launching the I/O handler");
                mTransfer = new Transfer(socket, mHandler);
                new Thread(mTransfer).start();
            } catch (IOException ioe) {
                LogUtil.e(LogUtil.LOG_TAG, "ClientSocketThread IOException : " + ioe);
                try {
                    socket.close();
                } catch (Exception e) {
                    LogUtil.e(LogUtil.LOG_TAG, "ClientSocketThread socket close failed : " + e);
                }
                mHandler.obtainMessage(ConnectionManager.ERROR, mTransfer).sendToTarget();
            }
        }
        return;
    }

    public Transfer getTransfer() {
        return mTransfer;
    }
}
