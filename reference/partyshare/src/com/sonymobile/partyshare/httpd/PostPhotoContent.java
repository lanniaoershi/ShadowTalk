/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.Context;
import android.database.Cursor;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.LogUtil;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * PostPhotoContent class.
 * This class is PostContent for Photo.
 */
public class PostPhotoContent extends PostContent {
    public PostPhotoContent(Context context) {
        super(context);
    }

    @Override
    /* package */ Response addContent(HashMap<String, String> map) {
        if (ConnectionManager.isGroupOwner()) {
            JsonUtil.addContent(mContext, JsonUtil.CONTENT_TYPE_PHOTO, map);
            PartyShareEventNotifier.notifyEvent(
                    mContext, PartyShareEvent.EVENT_LOAD_PHOTOLIST, null);
            return new Response(Response.STATUS_CODE_SUCCESS,
                    "Notify event of load photolist to client from host.");
        } else {
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_CMD,
                    String.valueOf(PartyShareCommand.CMD_ADD_PHOTO_CONTENT)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH,
                    map.get(PartyShareCommand.PARAM_PHOTO_MASTER_THUMBNAIL_PATH)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH,
                    map.get(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_PHOTO_SHARED_DATE,
                    map.get(PartyShareCommand.PARAM_PHOTO_SHARED_DATE)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_PHOTO_TAKEN_DATE,
                    map.get(PartyShareCommand.PARAM_PHOTO_TAKEN_DATE)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS,
                    map.get(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_PHOTO_MIME_TYPE,
                    map.get(PartyShareCommand.PARAM_PHOTO_MIME_TYPE)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_PHOTO_THUMBNAIL_FILENAME,
                    map.get(PartyShareCommand.PARAM_PHOTO_THUMBNAIL_FILENAME)));

            Response response = postMessageToHost(params);
            if (response == null || response.mStatusCode != HttpStatus.SC_OK) {
                int status = Response.STATUS_CODE_POST_ERROR;
                String msg = mContext.getResources().getString(
                        R.string.party_share_strings_photo_upload_err_txt);

                if (response == null) {
                    response = new Response(status, msg);
                } else {
                    response.mStatusCode = status;
                    response.mMessage = msg;
                }

                deletePhoto(map);
            }
            return response;
        }
    }

    @Override
    /* package */ Response removeContent(HashMap<String, String> map) {
        String masterPath = map.get(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH);
        String address = map.get(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS);
        LogUtil.d(LogUtil.LOG_TAG, "PostPhotoContent.removeContent masterPath : " + masterPath);
        LogUtil.d(LogUtil.LOG_TAG, "PostPhotoContent.removeContent address    : " + address);

        PhotoListController.removeLocalPhoto(mContext, map);

        if (ConnectionManager.isGroupOwner()) {
            JsonUtil.removeContent(mContext, JsonUtil.CONTENT_TYPE_PHOTO, map);
            PartyShareEventNotifier.notifyEvent(
                    mContext, PartyShareEvent.EVENT_REMOVE_PHOTO, map);
            return new Response(Response.STATUS_CODE_SUCCESS,
                    "Notify event of remove photo to client from host.");
        } else {
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_CMD,
                    String.valueOf(PartyShareCommand.CMD_REMOVE_PHOTO_CONTENT)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH, masterPath));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS, address));
            return postMessageToHost(params);
        }
    }

    /**
     * Since post error occurs, photo is deleted.
     * @param map Content parameter.
     */
    private void deletePhoto(HashMap<String, String> map) {
        String address = map.get(PartyShareCommand.PARAM_PHOTO_OWNER_ADDRESS);
        String masterPath = map.get(PartyShareCommand.PARAM_PHOTO_MASTER_FILE_PATH);
        LogUtil.d(LogUtil.LOG_TAG, "PostPhotoContent.deletePhoto address    : " + address);
        LogUtil.d(LogUtil.LOG_TAG, "PostPhotoContent.deletePhoto masterPath : " + masterPath);

        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(
                    mContext, PhotoProvider.CONTENT_URI,
                    new String[] { PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH },
                    PhotoProvider.COLUMN_OWNER_ADDRESS + " = ? AND " +
                            PhotoProvider.COLUMN_MASTER_FILE_PATH + " = ?",
                    new String[] { address, masterPath },
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                String path = cursor.getString(
                        cursor.getColumnIndex(PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH));
                File file = new File(path);
                if (file.exists()) {
                    if (!file.delete()) {
                        LogUtil.e(LogUtil.LOG_TAG,
                                "PostPhotoContent.deletePhoto : file delete error");
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        ProviderUtil.delete(
                mContext, PhotoProvider.CONTENT_URI,
                PhotoProvider.COLUMN_OWNER_ADDRESS + " = ? AND " +
                        PhotoProvider.COLUMN_MASTER_FILE_PATH + " = ?",
                new String[] { address, masterPath });

        ProviderUtil.delete(
                mContext, PhotoProvider.CONTENT_URI_LOCAL_PHOTO,
                PhotoProvider.COLUMN_QUERY_PATH + " = ?",
                new String[] { address + masterPath });
    }
}
