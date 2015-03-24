/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.PhotoUtils;
import com.sonymobile.partyshare.util.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * PhotoListController class.
 * This class performs processing of the command and event of HttpServer for Photo.
 */
public class PhotoListController {
    /** Object. */
    private static final Object sObject = new Object();
    /** Instance of thread. */
    private static Thread mThread;
    /** Thread start flag. */
    private static boolean mThreadStart = false;
    /** Event reach flag. */
    private static boolean mEventReached = false;
    /** Reach event list. */
    private static List<Map<String, String>> mAddEvents = new ArrayList<Map<String, String>>();
    /** Default retry connect time. */
    private static final long DEFAULT_RETRY_CONNECT_TIME = 1000;
    /** Retry timer. */
    private static Timer mTimer;
    /** Retry timer task. */
    private static TimerTask mTimerTask;
    /** Retry connect flag. */
    private static boolean mIsRetryConnect = false;
    /** Retry connect list. */
    private static List<RetryInfo> mRetryList = new ArrayList<RetryInfo>();
    /** Retry object. */
    private static final Object sRetry = new Object();

    /**
     * When photo is added by client, insert photo info into host.
     * When event reaches simultaneously, it adds in order.
     * @param context Context.
     * @param param Photo parameter.
     */
    public static void addPhotoContent(final Context context, final Map<String, String> param) {
        if (!ConnectionManager.isGroupOwner()) {
            LogUtil.e(LogUtil.LOG_TAG, "PhotoListController.addPhotoContent : Not host");
            return;
        }

        synchronized (sObject) {
            mAddEvents.add(param);
            if (mThreadStart) {
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.addPhotoContent : during processing");
                return;
            }

            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            List<Map<String, String>> tmpAddEvents;
                            synchronized (sObject) {
                                if (mAddEvents.isEmpty()) {
                                    break;
                                }

                                tmpAddEvents = new ArrayList<Map<String,String>>(mAddEvents);
                                mAddEvents.clear();
                            }

                            for (Map<String, String> prm : tmpAddEvents) {
                                if (addPhotoToHost(context, prm)) {
                                    JsonUtil.addContent(context, JsonUtil.CONTENT_TYPE_PHOTO, prm);
                                }
                            }

                            PartyShareEventNotifier.notifyEvent(
                                    context, PartyShareEvent.EVENT_LOAD_PHOTOLIST, null);
                        } catch (Exception e) {
                            LogUtil.e(LogUtil.LOG_TAG,
                                    "PhotoListController.addPhotoContent : " + e.toString());
                        } finally {
                            synchronized (sObject) {
                                if (mAddEvents.isEmpty()) {
                                    LogUtil.d(LogUtil.LOG_TAG,
                                            "PhotoListController.addPhotoContent : No request");
                                    mThreadStart = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            });
            mThreadStart = true;
            mThread.start();
        }
    }

    /**
     * Add photo to host.
     * @param context Context.
     * @param param Photo parameter.
     */
    private static boolean addPhotoToHost(Context context, Map<String, String> param) {
        String thumbPath = param.get(PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH);
        String filePath = param.get(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH);
        String sharedDate = param.get(PartyShareCommand.PARAM_PHOTO_SHARED_DATE);
        String takenDate = param.get(PartyShareCommand.PARAM_PHOTO_TAKEN_DATE);
        String ownerAddress = param.get(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS);
        String mimetype = param.get(PartyShareCommand.PARAM_PHOTO_MIME_TYPE);
        String thumbName = param.get(PartyShareCommand.PARAM_PHOTO_THUMBNAIL_FILENAME);

        Uri uri = addThumbAndPhotoInfo(context, thumbPath, filePath,
                sharedDate, takenDate, ownerAddress, mimetype, thumbName);

        if (uri == null) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "PhotoListController.addPhotoContent : Cannot insert");
            return false;
        }

        Cursor cursor = null;
        try {
            // update local thumbnail path. (Thumbnail file name may be changed.)
            cursor = ProviderUtil.query(
                    context, uri,
                    new String[] { PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH,
                            PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                thumbPath = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH));
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.addPhotoContent thumbPath : " + thumbPath);
                param.put(PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH, thumbPath);

                String newThumbPath = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH));
                thumbName = new File(newThumbPath).getName();
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.addPhotoContent thumbName : " + thumbName);

                param.put(PartyShareCommand.PARAM_PHOTO_THUMBNAIL_FILENAME, thumbName);
            } else {
                return false;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return true;
    }

    /**
     * When photo info is updated on host, load latest photo info in client.
     * When event reaches simultaneously, process 1 time.
     * @param context Context.
     * @param isOwnDataRead Flag of whether to also read the data shared by myself.
     */
    public static void reloadPhotoList(final Context context, final boolean isOwnDataRead) {
        if (ConnectionManager.isGroupOwner()) {
            LogUtil.e(LogUtil.LOG_TAG, "PhotoListController.reloadPhotoList : Not clent");
            return;
        }

        synchronized (sObject) {
            mEventReached = true;
            if (mThreadStart) {
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.reloadPhotoList : during processing");
                return;
            }

            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            synchronized (sObject) {
                                mEventReached = false;
                            }
                            addPhotoToClient(context, isOwnDataRead);
                        } catch (Exception e) {
                            LogUtil.e(LogUtil.LOG_TAG,
                                    "PhotoListController.reloadPhotoList : " + e.toString());
                        } finally {
                            synchronized (sObject) {
                                if (!mEventReached) {
                                    LogUtil.d(LogUtil.LOG_TAG, "PhotoListController"
                                            + ".reloadPhotoList : No new request");
                                    mThreadStart = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            });
            mThreadStart = true;
            mThread.start();
        }
    }

    /**
     * Remove photo.
     * @param context Context.
     * @param param Photo parameter.
     */
    public static void removePhoto(final Context context, final Map<String, String> param) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuffer selection = new StringBuffer();
                Set<Map.Entry<String, String>> entrySet = param.entrySet();
                Iterator<Map.Entry<String, String>> iterator = entrySet.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    if (selection.length() > 0) {
                        selection.append(" and ");
                    }
                    selection.append(entry.getKey() + " = \"" + entry.getValue() + "\"");
                }

                Cursor cursor = null;
                try {
                    cursor = ProviderUtil.query(
                            context, PhotoProvider.CONTENT_URI,
                            new String[] {
                                    PhotoProvider._ID,
                                    PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH,
                                    PhotoProvider.COLUMN_MASTER_FILE_PATH,
                                    PhotoProvider.COLUMN_OWNER_ADDRESS },
                            selection.toString(), null, null);

                    if (cursor == null || cursor.getCount() == 0) {
                        LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.removePhoto : No data");
                        return;
                    }

                    while (cursor.moveToNext()) {
                        String localThumb = cursor.getString(
                                cursor.getColumnIndex(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH));
                        LogUtil.d(LogUtil.LOG_TAG,
                                "PhotoListController.removePhoto localThumb : " + localThumb);

                        if (localThumb != null) {
                            File file = new File(localThumb);
                            if (file.exists()) {
                                if (!file.delete()) {
                                    LogUtil.e(LogUtil.LOG_TAG,
                                            "PhotoListController.removePhoto : delete error.");
                                }
                            }
                        }

                        // delete photo table.
                        int deleteId = cursor.getInt(cursor.getColumnIndex(PhotoProvider._ID));
                        ProviderUtil.delete(
                                context, PhotoProvider.CONTENT_URI,
                                PhotoProvider._ID + " = ?",
                                new String[] { String.valueOf(deleteId) });

                        // delete local_photo table.
                        String masterFilePath = cursor.getString(
                                cursor.getColumnIndex(PhotoProvider.COLUMN_MASTER_FILE_PATH));
                        String ownerAddress = cursor.getString(
                                cursor.getColumnIndex(PhotoProvider.COLUMN_OWNER_ADDRESS));
                        String localQueryPaty = String.format(
                                "%s:%s", ownerAddress, masterFilePath);
                        ProviderUtil.delete(
                                context, PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                                PhotoProvider.COLUMN_QUERY_PATH + " = ?",
                                new String[] { localQueryPaty });
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                if (ConnectionManager.isGroupOwner()) {
                    JsonUtil.removeContent(context, JsonUtil.CONTENT_TYPE_PHOTO, param);
                    PartyShareEventNotifier.notifyEvent(
                            context, PartyShareEvent.EVENT_REMOVE_PHOTO, param);
                }
            }
        }).start();
    }

    /**
     * Remove photo from local_photo table.
     * @param context Context.
     * @param param Photo parameter.
     */
    public static void removeLocalPhoto(Context context, Map<String, String> param) {
        String masterFilePath = param.get(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH);
        String ownerAddress = param.get(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS);

        String localQueryPath = String.format("%s:%s", ownerAddress, masterFilePath);
        ProviderUtil.delete(
                context, PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                PhotoProvider.COLUMN_QUERY_PATH + " = ?",
                new String[] { localQueryPath });
    }

    /**
     * Add photo to client.
     * - Get photo list from host's json file.
     * - Get thumbnail file from server and add photo info into database.
     * @param context Context.
     * @param isOwnDataRead Flag of whether to also read the data shared by myself.
     */
    private static void addPhotoToClient(Context context, boolean isOwnDataRead) {
        ArrayList<JSONObject> object = JsonUtil.getJsonObjectFromHost(
                context, JsonUtil.CONTENT_TYPE_PHOTO);

        if (object == null) {
            LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.addPhotoToClient : No data");
            return;
        }

        for (JSONObject content : object) {
            try {
                String thumbPath = content.getString(
                        PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH);
                String filePath = content.getString(
                        PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH);
                String sharedData = content.getString(
                        PartyShareCommand.PARAM_PHOTO_SHARED_DATE);
                String takenData = content.getString(
                        PartyShareCommand.PARAM_PHOTO_TAKEN_DATE);
                String ownerAddress = content.getString(
                        PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS);
                String mimetype = content.getString(
                        PartyShareCommand.PARAM_PHOTO_MIME_TYPE);
                String thumbName = content.getString(
                        PartyShareCommand.PARAM_PHOTO_THUMBNAIL_FILENAME);

                if (needRegisterPhoto(context, ownerAddress, filePath, isOwnDataRead)) {
                    addThumbAndPhotoInfo(context, thumbPath, filePath,
                            sharedData, takenData, ownerAddress, mimetype, thumbName);
                }
            } catch (JSONException e) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "PhotoListController.addPhotoToClient : " + e.toString());
            }
        }
    }

    /**
     * Check whether registration of photo info is necessity.
     * @param context Context.
     * @param macAddress Mac address of owner.
     * @param filePath Photo file path.
     * @param isOwnDataRead Flag of whether to also read the data shared by myself.
     * @return true when photo info is not registered (need register), otherwise false.
     */
    private static boolean needRegisterPhoto(
            Context context, String macAddress, String filePath, boolean isOwnDataRead) {
        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(
                    context, PhotoProvider.CONTENT_URI,
                    new String[] { PhotoProvider._ID },
                    PhotoProvider.COLUMN_OWNER_ADDRESS + " = ? and "
                            + PhotoProvider.COLUMN_MASTER_FILE_PATH + " = ?",
                    new String[] { macAddress, filePath },
                    null);

            if (cursor != null && cursor.getCount() > 0) {
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.needRegisterPhoto : already registered");
                return false;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return true;
    }

    /**
     * Add thumbnail file and add photo info into database.
     * @param context Context.
     * @param masterThumbPath Thumbnail file path.
     * @param masterFilePath Photo file path.
     * @param sharedDate Shared date of photo.
     * @param takenDate Taken date of photo.
     * @param ownerAddress Mac address of uploader.
     * @param mimetype Mimetype of photo.
     * @param thumbName Thumbnail file name.
     * @return insert uri.
     */
    private static Uri addThumbAndPhotoInfo(Context context, String masterThumbPath,
            String masterFilePath, String sharedDate, String takenDate, String ownerAddress,
            String mimetype, String thumbName) {
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.addThumbAndPhotoInfo masterThumbPath : " + masterThumbPath);
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.addThumbAndPhotoInfo masterFilePath : " + masterFilePath);
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.addThumbAndPhotoInfo sharedDate : " + sharedDate);
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.addThumbAndPhotoInfo takenDate : " + takenDate);
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.addThumbAndPhotoInfo ownerAddress : " + ownerAddress);
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.addThumbAndPhotoInfo mimetype : " + mimetype);
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.addThumbAndPhotoInfo thumbName : " + thumbName);

        File thumbFile = null;
        String localThumbPath = "";
        String localFilePath = "";

        if (!ConnectionManager.isGroupOwner()) {
            Cursor cursor = null;
            try {
                String localQueryPath = String.format("%s:%s", ownerAddress, masterFilePath);
                cursor = ProviderUtil.query(
                        context, PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                        new String[] {
                                PhotoProvider.COLUMN_THUMBNAIL_PATH,
                                PhotoProvider.COLUMN_FILE_PATH },
                        PhotoProvider.COLUMN_QUERY_PATH + " = ?",
                        new String[] { localQueryPath },
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    localThumbPath = cursor.getString(
                            cursor.getColumnIndex(PhotoProvider.COLUMN_THUMBNAIL_PATH));
                    localFilePath = cursor.getString(
                            cursor.getColumnIndex(PhotoProvider.COLUMN_FILE_PATH));

                    if (!localThumbPath.isEmpty()) {
                        File file = new File(localThumbPath);
                        if (file.exists()) {
                            thumbFile = file;
                        } else {
                            localThumbPath = "";
                        }
                    }

                    if (localThumbPath.isEmpty() && localFilePath.isEmpty()) {
                        ProviderUtil.delete(
                                context, PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                                PhotoProvider.COLUMN_QUERY_PATH + " = ?",
                                new String[] { localQueryPath });
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        String serverUrl = null;
        if (masterThumbPath.contains("/host/")) {
            serverUrl = String.format(
                    "http://%s:%s%s", ConnectionManager.getGroupOwnerAddress(),
                    PartyShareHttpd.PORT, masterThumbPath);
        } else {
            String ipAddress = Utility.getIpAddress(context, ownerAddress);
            if (ipAddress == null) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "PhotoListController.addThumbAndPhotoInfo : unknown ip address");
            } else {
                serverUrl = String.format(
                        "http://%s:%s%s", ipAddress, PartyShareHttpd.PORT, masterThumbPath);
            }
        }

        boolean resultThumb = false;
        if (thumbFile == null) {
            String thumbDir = PhotoUtils.createThumbnailFolder(context);
            String thumbPath = thumbDir + "/" + thumbName;
            thumbFile = new File(thumbPath);
            if (thumbFile.exists()) {
                String newThumbPath = convertThumbName(
                        context, ownerAddress, thumbPath, thumbName);
                if (!thumbPath.equals(newThumbPath)) {
                    thumbFile = new File(newThumbPath);
                    resultThumb = getThumbnailFile(context, thumbFile, serverUrl);
                }
            } else {
                resultThumb = getThumbnailFile(context, thumbFile, serverUrl);
            }
        }

        if (localThumbPath.isEmpty()) {
            String localQueryPath = String.format("%s:%s", ownerAddress, masterFilePath);
            ContentValues values = new ContentValues();
            values.put(PhotoProvider.COLUMN_QUERY_PATH, localQueryPath);
            values.put(PhotoProvider.COLUMN_THUMBNAIL_PATH, thumbFile.getAbsolutePath());
            if (localFilePath == null || localFilePath.isEmpty()) {
                ProviderUtil.insert(
                        context, PhotoProvider.CONTENT_URI_LOCAL_PHOTO, values);
            } else {
                ProviderUtil.update(
                        context, PhotoProvider.CONTENT_URI_LOCAL_PHOTO, values,
                        PhotoProvider.COLUMN_QUERY_PATH + " = ?",
                        new String[] { localQueryPath });
            }
        }

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH, masterThumbPath);
        values.put(PhotoProvider.COLUMN_MASTER_FILE_PATH, masterFilePath);
        values.put(PhotoProvider.COLUMN_SHARED_DATE, sharedDate);
        values.put(PhotoProvider.COLUMN_TAKEN_DATE, takenDate);
        values.put(PhotoProvider.COLUMN_OWNER_ADDRESS, ownerAddress);
        values.put(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH, thumbFile.getAbsolutePath());
        values.put(PhotoProvider.COLUMN_LOCAL_FILE_PATH, localFilePath);
        values.put(PhotoProvider.COLUMN_MIME_TYPE, mimetype);
        if (localFilePath == null || localFilePath.isEmpty()) {
            values.put(PhotoProvider.COLUMN_DL_STATE, PhotoUtils.DOWNLOAD_STATE_BEFORE);
        } else {
            values.put(PhotoProvider.COLUMN_DL_STATE, PhotoUtils.DOWNLOAD_STATE_AFTER);
        }

        Uri uri = ProviderUtil.insert(context, PhotoProvider.CONTENT_URI, values);

        if (uri != null) {
            if (ConnectionManager.isGroupOwner() && resultThumb) {
                updateMasterThumbPath(context, uri);
            } else if (!resultThumb) {
                startRetryConnect(context, uri, thumbFile);
            }
        }

        return uri;
    }

    /**
     * Check whether it is the thumbnail which the same member added.
     * When different, return another file name.
     * @param context Context.
     * @param ownerAddress Owner's mac address.
     * @param thumbPath Thumbnail file path.
     * @param thumbName Thumbnail file name before conversion.
     * @return thumbnail file path.
     */
    private static String convertThumbName(
            Context context, String ownerAddress, String thumbPath, String thumbName) {
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.convertThumbName ownerAddress : " + ownerAddress);
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.convertThumbName thumbPath : " + thumbPath);
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.convertThumbName thumbName : " + thumbName);

        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(
                    context, PhotoProvider.CONTENT_URI,
                    null,
                    PhotoProvider.COLUMN_OWNER_ADDRESS + " = ? AND " +
                            PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH + " = ?",
                    new String[] { ownerAddress, thumbPath },
                    null);

            if (cursor != null && cursor.getCount() > 0) {
                return thumbPath;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        String dir = PhotoUtils.createThumbnailFolder(context);
        if (dir != null && !dir.isEmpty()) {
            thumbPath = PhotoUtils.createUniqueFileName(dir, thumbName);
            LogUtil.d(LogUtil.LOG_TAG,
                    "PhotoListController.convertThumbName thumbPath : " + thumbPath);
        }

        return thumbPath;
    }

    /**
     * Get thumbnail file from server.
     * @param context Context.
     * @param file Thumbnail file.
     * @param serverUrl Server url.
     */
    private static boolean getThumbnailFile(Context context, File file, String serverUrl) {
        if (serverUrl == null) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "PhotoListController.getThumbnailFile : server url is null");
            return false;
        }

        if (file.exists()) {
            LogUtil.d(LogUtil.LOG_TAG,
                    "PhotoListController.getThumbnailFile : thumbnail file is exists");
            return true;
        }

        boolean result = connectServer(file, serverUrl);

        LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.getThumbnailFile return " + result);
        return result;
    }

    /**
     * Connect server.
     * @param file Thumbnail file.
     * @param serverUrl Server url.
     * @return true if success, otherwise false.
     */
    private static boolean connectServer(File file, String serverUrl) {
        LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.connectServer file : " + file);
        LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.connectServer url  : " + serverUrl);

        boolean ret = false;

        HttpURLConnection http = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        try {
            URL url = new URL(serverUrl);

            http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setConnectTimeout(5000);
            http.setReadTimeout(5000);
            http.connect();
            ret = (http.getResponseCode() == HttpURLConnection.HTTP_OK);
            if (ret) {
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        LogUtil.w(LogUtil.LOG_TAG, "PhotoListController.connectServer "
                              + "create new file error");
                    }
                }
                dis = new DataInputStream(new BufferedInputStream(http.getInputStream()));
                dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

                byte[] b = new byte[1024];
                int readByte = 0;
                while (-1 != (readByte = dis.read(b))) {
                    dos.write(b, 0, readByte);
                }
            } else {
                if (file.exists()) {
                    if (!file.delete()) {
                        LogUtil.e(LogUtil.LOG_TAG,
                                "PhotoListController.connectServer : delete error");
                    }
                }
            }
        } catch (IOException e) {
            ret = false;
            LogUtil.e(LogUtil.LOG_TAG, "PhotoListController.connectServer : " + e.toString());
            if (file.exists()) {
                if (!file.delete()) {
                    LogUtil.e(LogUtil.LOG_TAG, "PhotoListController.connectServer : delete error");
                }
            }
        } finally {
            if (http != null) {
                http.disconnect();
                http = null;
            }
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
                LogUtil.e(LogUtil.LOG_TAG, "PhotoListController.connectServer : " + e.toString());
            }
            try {
                if (dos != null) {
                    dos.close();
                }
            } catch (IOException e) {
                LogUtil.e(LogUtil.LOG_TAG, "PhotoListController.connectServer : " + e.toString());
            }
        }

        LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.connectServer return " + ret);
        return ret;
    }

    /**
     * Retry info class.
     */
    private static class RetryInfo {
        /** Database uri. */
        public Uri mUri;
        /** Thumbnail file. */
        public File mFile;

        public RetryInfo(Uri uri, File file) {
            mUri = uri;
            mFile = file;
        }
    }

    /**
     * Retry connect timer task class.
     */
    private static class RetryConnectTimerTask extends TimerTask {
        /** State of unnecessary retry. */
        private static final String STATE_UNNECESSARY_RETRY = "unnecessary retry";
        /** Context. */
        private Context mContext;

        public RetryConnectTimerTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Collection<RetryInfo> tmpRetryList;
                    synchronized (sRetry) {
                        if (mRetryList.isEmpty()) {
                            LogUtil.d(LogUtil.LOG_TAG, "mRetryList is empty");
                            return;
                        }
                        tmpRetryList = new ArrayList<RetryInfo>(mRetryList);
                        mRetryList.clear();
                    }

                    Iterator<RetryInfo> iterator = tmpRetryList.iterator();
                    while(iterator.hasNext()) {
                        RetryInfo info = iterator.next();

                        String serverUrl = getServerUrl(mContext, info.mUri);
                        LogUtil.d(LogUtil.LOG_TAG,
                                "RetryConnectTimerTask.run serverUrl : " + serverUrl);
                        if (serverUrl == null) {
                            continue;
                        } else if (serverUrl.equals(STATE_UNNECESSARY_RETRY)) {
                            iterator.remove();
                            continue;
                        }

                        long size = getThumbnailFileSizeFromServer(serverUrl);
                        LogUtil.d(LogUtil.LOG_TAG,
                                "RetryConnectTimerTask.run thumb size : " + size);
                        LogUtil.d(LogUtil.LOG_TAG, "RetryConnectTimerTask.run file size : "
                                + info.mFile.length());

                        if (size <= 0 || size < info.mFile.length()) {
                            continue;
                        }

                        boolean ret = connectServer(info.mFile, serverUrl);
                        LogUtil.d(LogUtil.LOG_TAG,
                                "RetryConnectTimerTask.run retry connect : " + ret);

                        if (ret) {
                            if (ConnectionManager.isGroupOwner()) {
                                updateMasterThumbPath(mContext, info.mUri);
                                updateThumbPathOfJsonFile(mContext, info.mUri);
                            }
                            mContext.getContentResolver().notifyChange(
                                    PhotoProvider.CONTENT_URI, null);
                            iterator.remove();
                        }
                    }

                    synchronized (sRetry) {
                        if (!tmpRetryList.isEmpty()) {
                            mRetryList.addAll(tmpRetryList);
                        }

                        if (mRetryList.isEmpty()) {
                            stopRetryConnect(mContext);
                            return;
                        }
                    }

                    mContext.getContentResolver().notifyChange(
                            PhotoProvider.CONTENT_URI, null);
                }
            }).start();
        }
    }

    /**
     * Start retry connect.
     * @param context Context.
     * @param uri Update database uri.
     * @param file Thumbnail file.
     * @param thumbPath master thumbnail path.
     */
    private static void startRetryConnect(Context context, Uri uri, File file) {
        LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.startRetryConnect");
        LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.startRetryConnect uri  : " + uri);
        LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.startRetryConnect file : " + file);

        RetryInfo info = new RetryInfo(uri, file);
        synchronized (sRetry) {
            mRetryList.add(info);
        }

        if (mIsRetryConnect) {
            LogUtil.d(LogUtil.LOG_TAG,
                    "PhotoListController.startRetryConnect already start retry");
            return;
        }

        mTimerTask = new RetryConnectTimerTask(context);
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(
                mTimerTask, DEFAULT_RETRY_CONNECT_TIME * 10, DEFAULT_RETRY_CONNECT_TIME * 10);
        mIsRetryConnect = true;
    }

    /**
     * Get server url.
     * @param context Context.
     * @param uri Update database uri.
     * @return server url.
     */
    private static String getServerUrl(Context context, Uri uri) {
        String ownerAddress = null;
        String masterFilePath = null;
        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(context, uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                ownerAddress = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_OWNER_ADDRESS));
                masterFilePath = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_MASTER_FILE_PATH));
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.getServerUrl ownerAddress : " + ownerAddress);
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.getServerUrl masterFilePath : " + masterFilePath);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        ArrayList<JSONObject> object = JsonUtil.getJsonObjectFromHost(
                context, JsonUtil.CONTENT_TYPE_PHOTO);

        if (object == null) {
            LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.getServerUrl : No data");
            return null;
        }

        String masterThumbPath = null;
        for (JSONObject content : object) {
            try {
                String owner = content.getString(
                        PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS);
                String filePath = content.getString(
                        PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH);

                if (owner.equals(ownerAddress) && filePath.equals(masterFilePath)) {
                    masterThumbPath = content.getString(
                            PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH);
                    LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.getServerUrl " +
                            "masterThumbPath : " + masterThumbPath);
                    break;
                }
            } catch (JSONException e) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "PhotoListController.getServerUrl : " + e.toString());
            }
        }

        if (masterThumbPath == null) {
            // File is deleted.
            LogUtil.w(LogUtil.LOG_TAG,
                    "PhotoListController.getServerUrl : file is deleted");
            return RetryConnectTimerTask.STATE_UNNECESSARY_RETRY;
        }

        String serverUrl = null;
        if (masterThumbPath.contains("/host/")) {
            serverUrl = String.format(
                    "http://%s:%s%s", ConnectionManager.getGroupOwnerAddress(),
                    PartyShareHttpd.PORT, masterThumbPath);
        } else {
            String ipAddress = Utility.getIpAddress(context, ownerAddress);
            if (ipAddress == null) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "PhotoListController.getServerUrl : unknown ip address");
                return null;
            } else {
                serverUrl = String.format(
                        "http://%s:%s%s", ipAddress, PartyShareHttpd.PORT, masterThumbPath);
            }
        }

        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.getServerUrl serverUrl : " + serverUrl);
        return serverUrl;
    }

    /**
     * Stop retry connect.
     * @param context Context.
     */
    private static void stopRetryConnect(Context context) {
        LogUtil.d(LogUtil.LOG_TAG, "PhotoListController.stopRetryConnect");

        context.getContentResolver().notifyChange(PhotoProvider.CONTENT_URI, null);

        if (!mIsRetryConnect) {
            LogUtil.v(LogUtil.LOG_TAG, "PhotoListController.stopRetryConnect already stop retry");
            return;
        }
        mTimer.cancel();
        mTimer = null;
        mIsRetryConnect = false;
    }

    /**
     * Get thumbnail file size from server.
     * @param path Server url.
     * @return thumbnail file size.
     */
    private static int getThumbnailFileSizeFromServer(final String path) {
        String serverUrl = path;
        serverUrl += "?" + PartyShareEvent.PARAM_EVENT_CODE
                + "=" + PartyShareEvent.EVENT_GET_FILESIZE;

        int filesize = -1;
        HttpURLConnection http = null;
        DataInputStream dis = null;
        try {
            URL url = new URL(serverUrl);
            http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setConnectTimeout(5000);
            http.setReadTimeout(5000);
            http.connect();
            boolean result = (http.getResponseCode() == HttpURLConnection.HTTP_OK);
            if (!result) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "PhotoListController.getThumbnailFileSizeFromServer http error code : " +
                        http.getResponseCode());
                return filesize;
            }

            dis = new DataInputStream(http.getInputStream());
            byte[] b = new byte[1024];
            int length = dis.read(b);
            filesize = Integer.parseInt(new String(b, 0, length, "UTF-8"));
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG, "PhotoListController.getThumbnailFileSizeFromServer " +
                    "Failed get download file size : " + e);
        } catch (NumberFormatException e) {
            LogUtil.e(LogUtil.LOG_TAG, "PhotoListController.getThumbnailFileSizeFromServer " +
                    "Get illegal download file size : " + e);
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "PhotoListController.getThumbnailFileSizeFromServer " + e.toString());
            }
            if (http != null) {
                http.disconnect();
                http = null;
            }
        }

        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.getThumbnailFileSizeFromServer filesize : " + filesize);
        return filesize;
    }

    /**
     * Update master thumbnail path.
     * @param context Context context.
     * @param uri Update uri.
     */
    private static void updateMasterThumbPath(Context context, Uri uri) {
        if (!ConnectionManager.isGroupOwner()) {
            return;
        }

        String id = uri.getLastPathSegment();
        // update master thumbnail path
        String thumbPath = "/host/thumbnail/" + id;
        LogUtil.d(LogUtil.LOG_TAG,
                "PhotoListController.updateMasterThumbPath masterThumbPath : " + thumbPath);

        ContentValues values = new ContentValues();
        values.put(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH, thumbPath);
        ProviderUtil.update(context, uri, values, null, null);
    }

    /**
     * Update master thumbnail path of json file.
     * @param context Context context.
     * @param uri Update uri.
     */
    private static void updateThumbPathOfJsonFile(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(context, uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String owner = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_OWNER_ADDRESS));
                String filePath = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_MASTER_FILE_PATH));
                String thumbPath = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH));
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.updateThumbPathOfJsonFile owner     : " + owner);
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.updateThumbPathOfJsonFile filePath  : " + filePath);
                LogUtil.d(LogUtil.LOG_TAG,
                        "PhotoListController.updateThumbPathOfJsonFile thumbPath : " + thumbPath);

                HashMap<String, String> searchParam = new HashMap<String, String>();
                searchParam.put(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS, owner);
                searchParam.put(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH, filePath);

                HashMap<String, String> updateParam = new HashMap<String, String>();
                updateParam.put(PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH, thumbPath);

                JsonUtil.updateContent(
                        context, JsonUtil.CONTENT_TYPE_PHOTO, searchParam, updateParam);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
