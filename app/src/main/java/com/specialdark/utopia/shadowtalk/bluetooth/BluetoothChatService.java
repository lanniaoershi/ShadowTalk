package com.specialdark.utopia.shadowtalk.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.specialdark.utopia.shadowtalk.constant.ShadowTalkConstant;
import com.specialdark.utopia.shadowtalk.logutil.ShadowTalkLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


/**
 * Created by weiwei on 3/26/15.
 */

public class BluetoothChatService {

    //Name for the SDP record when creating server socket.
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    //Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandlerChat;
    private Handler mHandlerMain;
    private static AcceptThread mSecureAcceptThread;
    private static AcceptThread mInsecureAcceptThread;
    private static ConnectThread mConnectThread;
    private static ConnectedThread mConnectedThread;
    private int mState;


    public BluetoothChatService(Context context, Handler handlerMain, Handler handlerChat) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandlerMain = handlerMain;
        mHandlerChat = handlerChat;
        mState = ShadowTalkConstant.STATE_NONE;
    }


    private synchronized void setState(int state) {
        mState = state;

        //Give the new state to the Handler so the UI activity can update UI
        if (mHandlerChat != null) {
            mHandlerChat.obtainMessage(BluetoothChatActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        }
    }

    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(ShadowTalkConstant.STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
            ShadowTalkLog.i("mSecureAcceptThread start");

        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
            ShadowTalkLog.i("mInsecureAcceptThread start");
        }
    }


    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {

        // Cancel any thread attempting to make a connection
        if (mState == ShadowTalkConstant.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running(holding) a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(ShadowTalkConstant.STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        // keep accept to accept other device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        if (mHandlerChat != null) {
            Message msg = mHandlerChat.obtainMessage(BluetoothChatActivity.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(BluetoothChatActivity.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            mHandlerChat.sendMessage(msg);
        }
        //save sth

        setState(ShadowTalkConstant.STATE_CONNECTED);

    }

    /**
     * Stop all threads, call onDestroy();
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(ShadowTalkConstant.STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != ShadowTalkConstant.STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        if (mHandlerChat != null) {
            // Send a failure message back to the Activity
            Message msg = mHandlerChat.obtainMessage(BluetoothChatActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(BluetoothChatActivity.TOAST, "Unable to connect device");
            msg.setData(bundle);
            mHandlerChat.sendMessage(msg);
        }
        //save sth

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        if (mHandlerChat != null) {
            // Send a failure message back to the Activity
            Message msg = mHandlerChat.obtainMessage(BluetoothChatActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(BluetoothChatActivity.TOAST, "Device connection was lost");
            msg.setData(bundle);
            mHandlerChat.sendMessage(msg);
        }
        //save sth

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }
        @Override
        public void run() {
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;
            // Listen to the server socket if we're not connected
            while (mState != ShadowTalkConstant.STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    if (mmServerSocket != null) {
                        socket = mmServerSocket.accept();
                        ShadowTalkLog.i("run as server, and mmServerSocket.accept(); called");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                // If a connection was accepted

                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case ShadowTalkConstant.STATE_LISTEN:
                            case ShadowTalkConstant.STATE_CONNECTING:
                                ShadowTalkLog.i("mState = " + mState);
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case ShadowTalkConstant.STATE_NONE:
                            case ShadowTalkConstant.STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                }
                                break;
                        }
                    }
                }
            }


        }

        public void cancel() {

            try {
                mmServerSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        public void run() {

            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                ShadowTalkLog.e("IOException happen on ConnectThread mmSocket.connect();");
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    ShadowTalkLog.e("IOException 2 happen on ConnectThread mmSocket.close();");
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                ShadowTalkLog.e("IOException happen on ConnectedThread get input and out put stream");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    if (mHandlerChat != null) {
                        // Send the obtained bytes to the UI Activity
                        mHandlerChat.obtainMessage(BluetoothChatActivity.MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    }
                    //save sth
                } catch (IOException e) {

                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothChatService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                if (mHandlerChat != null) {
                    mHandlerChat.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE, -1, -1, buffer)
                            .sendToTarget();
                }
                //save sth
            } catch (IOException e) {

            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

}
