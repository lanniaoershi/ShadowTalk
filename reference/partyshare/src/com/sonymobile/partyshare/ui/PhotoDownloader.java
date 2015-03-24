/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.httpd.PartyShareEvent;
import com.sonymobile.partyshare.httpd.PartyShareHttpd;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.PhotoUtils;
import com.sonymobile.partyshare.util.Setting;
import com.sonymobile.partyshare.util.Utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoDownloader {

    public static final int RESULT_SUCCESS = 0;

    private static PhotoDownloader sInstance = null;
    private Context mContext;

    private PhotoDownloader(Context context) {
        this.mContext = context;
    }

    public static synchronized PhotoDownloader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PhotoDownloader(context);
        }
        return sInstance;
    }

    public synchronized int download(final long id, final PhotoEventListener listener) {
        LogUtil.d(LogUtil.LOG_TAG, "PhotoDownloader.download()");

        File file = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        int result = RESULT_SUCCESS;
        Cursor cursor = null;
        String filePath = null;
        ContentValues values = new ContentValues();
        Uri targetUri = ContentUris.withAppendedId(PhotoProvider.CONTENT_URI, id);
        int num = 0;
        try {
            // Query file path.
            String[] columns = {
                    PhotoProvider.COLUMN_LOCAL_FILE_PATH,
                    PhotoProvider.COLUMN_MASTER_FILE_PATH,
                    PhotoProvider.COLUMN_OWNER_ADDRESS,
                    };
            cursor = ProviderUtil.query(mContext, targetUri, columns, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                LogUtil.e(LogUtil.LOG_TAG, "Failed query target ");
                result = R.string.party_share_strings_manual_photo_download_err_txt;
                return result;
            }

            String localPath = cursor.getString(
                    cursor.getColumnIndex(PhotoProvider.COLUMN_LOCAL_FILE_PATH));
            LogUtil.d(LogUtil.LOG_TAG,
                    "PhotoDownloader.download localPath : " + localPath);
            if (!TextUtils.isEmpty(localPath) && new File(localPath).exists()) {
                LogUtil.d(LogUtil.LOG_TAG, "File is already downloaded : " + localPath);
                // File is already downloaded.
                filePath = localPath;
                result = RESULT_SUCCESS;
                return result;
            }

            String masterPath = cursor.getString(
                    cursor.getColumnIndex(PhotoProvider.COLUMN_MASTER_FILE_PATH));
            String ownerAddress = cursor.getString(
                    cursor.getColumnIndex(PhotoProvider.COLUMN_OWNER_ADDRESS));
            LogUtil.d(LogUtil.LOG_TAG,
                    "PhotoDownloader.download masterPath : " + masterPath);
            LogUtil.d(LogUtil.LOG_TAG,
                    "PhotoDownloader.download ownerAddress : " + ownerAddress);

            // Update to the downloading state to DB.
            values.clear();
            values.put(PhotoProvider.COLUMN_DL_STATE, PhotoUtils.DOWNLOAD_STATE_DURING);
            num = ProviderUtil.update(mContext, targetUri, values, null, null);
            if (0 >= num) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "Failed update database for Update to the downloading state.");
                result = R.string.party_share_strings_manual_photo_download_err_txt;
                return result;
            }

            // Get file size from server.
            String ipAddress = Utility.getIpAddress(mContext, ownerAddress);
            String serverUri = String.format("http://%s:%s%s",
                    ipAddress, PartyShareHttpd.PORT, masterPath);
            int fileSize = getDownloadFileSizeFromServer(serverUri);
            if (0 > fileSize) {
                LogUtil.e(LogUtil.LOG_TAG, "Failed get download file size.");
                result = R.string.party_share_strings_manual_photo_download_err_txt;
                return result;
            }
            LogUtil.v(LogUtil.LOG_TAG,
                    "PhotoDownloader.download fileSize : " + fileSize);

            // Check whether available storage capacity is enough.
            if (!PhotoUtils.isEnoughStorage(fileSize)) {
                LogUtil.w(LogUtil.LOG_TAG,
                        "Failed download photo, Not enough memory on Internal storage");
                result = R.string.party_share_strings_manual_photo_download_memory_err_txt;
                return result;
            }

            LogUtil.d(LogUtil.LOG_TAG, "Start connect for get download file.");
            // Get file from server.
            URL url = new URL(serverUri);
            HttpURLConnection conn = (HttpURLConnection) (url.openConnection());
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            boolean ret = (conn.getResponseCode() == HttpURLConnection.HTTP_OK);
            String mimetype = conn.getContentType();
            LogUtil.v(LogUtil.LOG_TAG, "PhotoDownloader.download mimetype : " + mimetype);
            if (ret) {
                String ext = PhotoUtils.getExtension(mimetype);
                // Create file path
                final Date date = new Date(System.currentTimeMillis());
                final SimpleDateFormat dataFormat = new SimpleDateFormat(
                        "yyyyMMddHHmmss", Locale.getDefault());
                final String filename = dataFormat.format(date) + ext;
                filePath = PhotoUtils.createUniqueFileName(
                        Setting.getPhotoSaveFolder(mContext), filename);

                LogUtil.d(LogUtil.LOG_TAG, "Start getInputStream for get download file.");
                file = new File(filePath);
                dis = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
                dos = new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(file)));

                byte[] b = new byte[1024];
                int readByte = 0;
                while (-1 != (readByte = dis.read(b))) {
                    dos.write(b, 0, readByte);
                }

                LogUtil.d(LogUtil.LOG_TAG, "Finish write download file.");

                // update file path to photo table.
                values.clear();
                values.put(PhotoProvider.COLUMN_LOCAL_FILE_PATH, filePath);
                values.put(PhotoProvider.COLUMN_DL_STATE, PhotoUtils.DOWNLOAD_STATE_AFTER);
                num = ProviderUtil.update(mContext, targetUri, values, null, null);
                if (0 >= num) {
                    LogUtil.e(LogUtil.LOG_TAG,
                            "Failed update database for update local file path.");
                    result = R.string.party_share_strings_manual_photo_download_err_txt;
                    return result;
                }

                // update file path to local_photo table.
                values.clear();
                values.put(PhotoProvider.COLUMN_FILE_PATH, filePath);
                num = ProviderUtil.update(mContext,
                        PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                        values,
                        PhotoProvider.COLUMN_QUERY_PATH + " = ?",
                        new String[] { String.format("%s:%s", ownerAddress, masterPath) });
                if (0 >= num) {
                    LogUtil.e(LogUtil.LOG_TAG,
                            "Failed update database for update local file path.");
                    result = R.string.party_share_strings_manual_photo_download_err_txt;
                    return result;
                }

                // Scan file.
                PhotoUtils.scanFile(mContext, filePath, null);

                result = RESULT_SUCCESS;
            } else {
                result = R.string.party_share_strings_manual_photo_download_err_txt;
                LogUtil.e(LogUtil.LOG_TAG, "Failed connection to server.");
            }
        } catch (IOException ioe) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed download file : " + ioe);
            result = R.string.party_share_strings_manual_photo_download_err_txt;
        } catch (NullPointerException npe) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed download file : " + npe);
            result = R.string.party_share_strings_manual_photo_download_err_txt;
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
            }

            try {
                if (dos != null) {
                    dos.close();
                }
            } catch (IOException e) {
            }

            if (result == RESULT_SUCCESS) {
                // Notify the complete of the download.
                if (listener != null) {
                    listener.onDownloadCompleted(filePath);
                }
            } else {
                // Clean data.
                if (file != null && file.exists()) {
                    if (!file.delete()) {
                        LogUtil.d(LogUtil.LOG_TAG, "downloadFile - file delete error");
                    }
                }

                // Return to the before state to DB.
                values.clear();
                values.put(PhotoProvider.COLUMN_DL_STATE, PhotoUtils.DOWNLOAD_STATE_BEFORE);
                ProviderUtil.update(mContext, targetUri, values, null, null);

                // Notify the failure of the download.
                if (listener != null) {
                    listener.onDownloadFailed(result);
                }
            }
        }
        return result;
    }

    private int getDownloadFileSizeFromServer(final String path) {
        String serverUrl = path;
        serverUrl += "?" + PartyShareEvent.PARAM_EVENT_CODE
                + "=" + PartyShareEvent.EVENT_GET_FILESIZE;

        int filesize = -1;
        HttpURLConnection http = null;
        DataInputStream dis = null;
        try {
            LogUtil.d(LogUtil.LOG_TAG, "Start connect for get download file size.");
            URL url = new URL(serverUrl);
            http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setConnectTimeout(5000);
            http.setReadTimeout(5000);
            http.connect();
            boolean result = (http.getResponseCode() == HttpURLConnection.HTTP_OK);
            if (!result) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "getDownloadFileSizeFromServer http error code : " +
                        http.getResponseCode());
                return filesize;
            }

            LogUtil.d(LogUtil.LOG_TAG, "Start getInputStream for get download file size.");
            dis = new DataInputStream(http.getInputStream());
            byte[] b = new byte[1024];
            int length = dis.read(b);
            filesize = Integer.parseInt(new String(b, 0, length, "UTF-8"));
            LogUtil.d(LogUtil.LOG_TAG, "Download file size is " + filesize);
        } catch (NumberFormatException nfe) {
            LogUtil.e(LogUtil.LOG_TAG, "Get illegal download file size : " + nfe);
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "Failed get download file size : " + e);
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
            }
            if (http != null) {
                http.disconnect();
                http = null;
            }
        }
        return filesize;
    }
}