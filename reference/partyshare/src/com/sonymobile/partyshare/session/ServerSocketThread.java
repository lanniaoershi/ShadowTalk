/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.session;

import android.os.Handler;

import com.sonymobile.partyshare.util.LogUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerSocketThread extends Thread {

    ServerSocket mSocket = null;
    private final static int THREAD_COUNT = 10;
    private final static int MULTI_CONNECTION = 10;
    private Handler mHandler;
    private Transfer mTransfer;
    private ArrayList<Socket> mSocketList = new ArrayList<Socket>();

    public ServerSocketThread(Handler handler) throws IOException {
        try {
            mSocket = new ServerSocket(ConnectionManager.SERVER_PORT, MULTI_CONNECTION);
            mHandler = handler;
            LogUtil.d(LogUtil.LOG_TAG, "ServerSocketThread Started");
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "io exception");
            pool.shutdownNow();
            throw e;
        }
    }

    /**
     * A ThreadPool for client sockets.
     */
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = mSocket.accept();
                mSocketList.add(socket);
                mTransfer = new Transfer(socket, mHandler);
                pool.execute(mTransfer);
                LogUtil.d(LogUtil.LOG_TAG, "Server Launching the I/O handler");

            } catch (SocketException e) {
                pool.shutdownNow();
                break;
            } catch (IOException e) {
                LogUtil.e(LogUtil.LOG_TAG, "ServerSocketThread IOException");
                try {
                    if (!mSocket.isClosed())
                        mSocket.close();
                } catch (IOException ioe) {

                }
                e.printStackTrace();
                pool.shutdownNow();
                break;
            }
        }
    }

    public void finish() {
        try {
            if (mSocket != null && !mSocket.isClosed()) {
                LogUtil.v(LogUtil.LOG_TAG, "finish() Socket.close()");
                mSocket.close();
            }

            for (Socket socket : mSocketList) {
                if (!socket.isClosed()) {
                    socket.close();
                }
            }
        } catch (IOException ioe) {
        }
    }
}
