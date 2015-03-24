/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Utility;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * PostContent class.
 * This class performs the notice to Host from Client
 * and the notice to Client from Host on AsyncTask.
 */
public class PostContent {
    /** Context. */
    public Context mContext;

    /**
     * Response class.
     */
    /* package */ static class Response {
        /** Status code of success. */
        public static final int STATUS_CODE_SUCCESS = 0;
        /** Status code of nothing. */
        public static final int STATUS_CODE_NOTHING = 1;
        /** Status code of playlist maximum. */
        public static final int STATUS_CODE_PLAYLIST_MAXIMUM = 2;
        /** Status code of post error. */
        public static final int STATUS_CODE_POST_ERROR = 3;
        /** Status code of response.*/
        int mStatusCode;
        /** Message of response. */
        String mMessage;

        public Response(int status, String msg) {
            mStatusCode = status;
            mMessage = msg;
        }
    }

    public PostContent(Context context) {
        mContext = context;
    }

    /**
     * Post processing of content.
     * @param param Content parameter.
     */
    public void post(HashMap<String, String> param) {
        new PostContentTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, PartyShareEvent.EVENT_CONTENT_ADDED, param);
    }

    /**
     * Remove processing of content.
     * @param param Content parameter.
     */
    public void remove(HashMap<String, String> param) {
        new PostContentTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, PartyShareEvent.EVENT_CONTENT_REMOVED, param);
    }

    /**
     * Play next processing.
     * @param musicId Music id of play next.
     */
    public void playNext(int musicId) {
        if (ConnectionManager.isGroupOwner()) {
            return;
        }
        new PostContentTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, PartyShareEvent.EVENT_PLAY_NEXT, musicId);
    }

    /**
     * Communicate with joined guest.
     * @param address Mac address of joined guest.
     */
    public void join(String address) {
        new PostContentTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, PartyShareEvent.EVENT_JOIN, address);
    }

    /**
     * New member does not have data.
     * So delete new member's data from host.
     * @param address Mac address of client.
     */
    public void deleteDataFromHost(String address) {
        if (ConnectionManager.isGroupOwner()) {
            return;
        }
        new PostContentTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, PartyShareEvent.EVENT_DELETE_CLIENT_DATA, address);
    }

    /**
     * AsyncTask class for post content.
     */
    private class PostContentTask extends AsyncTask<Object, Integer, Response> {
        @Override
        @SuppressWarnings("unchecked")
        protected Response doInBackground(Object... params) {
            Response response = null;
            if ((params != null) && (params.length > 0)) {
                switch ((Integer)params[0]) {
                    case PartyShareEvent.EVENT_CONTENT_ADDED: {
                        HashMap<String, String> map = (HashMap<String, String>)params[1];
                        response = addContent(map);
                        break;
                    }
                    case PartyShareEvent.EVENT_CONTENT_REMOVED: {
                        HashMap<String, String> map = (HashMap<String, String>)params[1];
                        response = removeContent(map);
                        break;
                    }
                    case PartyShareEvent.EVENT_PLAY_NEXT:
                        String musicId = params[1].toString();
                        response = playNextContent(musicId);
                        break;
                    case PartyShareEvent.EVENT_DELETE_CLIENT_DATA: {
                        String address = params[1].toString();
                        response = deleteClientData(address);
                        break;
                    }
                    case PartyShareEvent.EVENT_JOIN: {
                        String address = null;
                        if (params[1] != null) {
                            address = params[1].toString();
                        }
                        response = sendJoinEvent(address);
                        break;
                    }
                    default :
                        break;
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);
            if (response != null) {
                LogUtil.d(LogUtil.LOG_TAG,
                        "PostContent.onPostExecute mStatusCode : " + response.mStatusCode);
                LogUtil.d(LogUtil.LOG_TAG,
                        "PostContent.onPostExecute mMessage : " + response.mMessage);

                switch (response.mStatusCode) {
                    case Response.STATUS_CODE_PLAYLIST_MAXIMUM:
                    case Response.STATUS_CODE_POST_ERROR:
                        Utility.showToast(mContext, response.mMessage, Toast.LENGTH_SHORT);
                        break;
                    default:
                        break;
                }
            } else {
                LogUtil.d(LogUtil.LOG_TAG, "PostContent.onPostExecute not post message");
            }
        }
    }

    /**
     * Post message to host.
     * @param param Content parameter.
     * @return Response from http server.
     */
    /* package */ Response postMessageToHost(List<NameValuePair> param) {
        String ownerAddress = ConnectionManager.getGroupOwnerAddress();
        if (ownerAddress == null || ownerAddress.isEmpty()) {
            return new Response(HttpStatus.SC_NOT_FOUND, "Group owner is not found");
        }

        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            String httpUrl = String.format(
                    "http://%s:%s", ownerAddress, PartyShareHttpd.PORT);

            LogUtil.d(LogUtil.LOG_TAG, "PostContent.postMessageToHost httpUrl : " + httpUrl);

            HttpPost post = new HttpPost(httpUrl);
            post.setEntity(new UrlEncodedFormEntity(param, "utf-8"));
            HttpResponse httpResponse = httpClient.execute(post);

            int statudCode = httpResponse.getStatusLine().getStatusCode();
            String message = EntityUtils.toString(httpResponse.getEntity());
            LogUtil.d(LogUtil.LOG_TAG,
                    "PostContent.postMessageToHost http status code : " + statudCode);
            LogUtil.d(LogUtil.LOG_TAG,
                    "PostContent.postMessageToHost response message : " + message);

            return new Response(statudCode, message);
        } catch (IOException e) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "PostContent.postMessageToHost IOException : " + e.toString());
        }
        return null;
    }

    /**
     * Post processing of content.
     * @param param Content parameter.
     * @return Response from http server.
     */
    /* package */ Response addContent(HashMap<String, String> param) {
        return null;
    }

    /**
     * Remove processing of content.
     * @param param Content parameter.
     * @return Response from http server.
     */
    /* package */ Response removeContent(HashMap<String, String> param) {
        return null;
    }

    /**
     * Play next processing.
     * @param musicId Music id of play next.
     * @return Response from http server.
     */
    private Response playNextContent(String musicId) {
        LogUtil.d(LogUtil.LOG_TAG, "PostContent.playNextContent musicId : " + musicId);

        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(
                PartyShareCommand.PARAM_CMD, String.valueOf(PartyShareCommand.CMD_PLAY_NEXT)));
        params.add(new BasicNameValuePair(PartyShareCommand.PARAM_MUSIC_ID, musicId));

        return postMessageToHost(params);
    }

    /**
     * New member does not have data.
     * So delete new member's data from host.
     * @param address Mac address of client.
     * @return Response from http server.
     */
    private Response deleteClientData(String address) {
        LogUtil.d(LogUtil.LOG_TAG, "PostContent.deleteClientData address : " + address);

        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(
                PartyShareCommand.PARAM_CMD,
                String.valueOf(PartyShareCommand.CMD_DELETE_CLIENT_DATA)));
        params.add(new BasicNameValuePair(PartyShareCommand.PARAM_OWNER_ADDRESS, address));

        return postMessageToHost(params);
    }

    /**
     * Send event to all the guests at the time of joining the party.
     * And send event to the new guest who joined the party.
     * @param address Mac address of joined guest.
     * @return Response from http server.
     */
    private Response sendJoinEvent(String address) {
        if (address == null) {
            PartyShareEventNotifier.notifyEvent(mContext, PartyShareEvent.EVENT_JOIN, null);
        } else {
            PartyShareEventNotifier.notifyEventSpecifyClient(
                    mContext, PartyShareEvent.EVENT_JOIN, address, null);
        }
        return new Response(Response.STATUS_CODE_SUCCESS, "Notify event of join");
    }
}
