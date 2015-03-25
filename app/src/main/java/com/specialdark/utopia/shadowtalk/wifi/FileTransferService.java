package com.specialdark.utopia.shadowtalk.wifi;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by weiwei on 3/25/15.
 */
public class FileTransferService extends IntentService{

    private static final int SOCKET_TIMEOUT = 5000; //ms
    public static final String ACTION_SEND_FILE = "com.specialdark.utopia.shadowtalk.wifi.SEND_FILE";
    public static final String EXTRA_FILE_PTAH = "file _url";
    public static final String EXTRA_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRA_GROUP_OWNER_PORT = "go_port";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRA_FILE_PTAH);
            String host = intent.getExtras().getString(EXTRA_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRA_GROUP_OWNER_PORT);

            try {
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                OutputStream stream = socket.getOutputStream();
                ContentResolver contentResolver = context.getContentResolver();
                InputStream inputStream = null;
                try {
                    inputStream = contentResolver.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                DeviceDetailFragment.copyFile(inputStream, stream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
