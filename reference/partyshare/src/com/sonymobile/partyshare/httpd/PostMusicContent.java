/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.Context;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.LogUtil;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * PostMusicContent class.
 * This class is PostContent for music.
 */
public class PostMusicContent extends PostContent {
    public PostMusicContent(Context context) {
        super(context);
    }

    @Override
    /* package */ Response addContent(HashMap<String, String> map) {
        if (!ConnectionManager.isGroupOwner()) {
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_CMD,
                    String.valueOf(PartyShareCommand.CMD_ADD_MUSIC_CONTENT)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_MUSIC_TITLE,
                    map.get(PartyShareCommand.PARAM_MUSIC_TITLE)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_MUSIC_ARTIST,
                    map.get(PartyShareCommand.PARAM_MUSIC_ARTIST)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_MUSIC_TIME,
                    map.get(PartyShareCommand.PARAM_MUSIC_TIME)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_MUSIC_URL,
                    map.get(PartyShareCommand.PARAM_MUSIC_URL)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_MUSIC_OWNER_ADDRESS,
                    map.get(PartyShareCommand.PARAM_MUSIC_OWNER_ADDRESS)));
            params.add(new BasicNameValuePair(
                    PartyShareCommand.PARAM_MUSIC_OWNER_NAME,
                    map.get(PartyShareCommand.PARAM_MUSIC_OWNER_NAME)));

            Response response = postMessageToHost(params);
            if (response != null) {
                if (response.mStatusCode == PartyShareHttpd.STATUS_CODE_PLAYLIST_MAXIMUM) {
                    int status = Response.STATUS_CODE_PLAYLIST_MAXIMUM;
                    String msg = mContext.getResources().getString(
                            R.string.party_share_strings_music_list_full_txt);

                    response.mStatusCode = status;
                    response.mMessage = msg;
                }
            }
            return response;
        }

        return new Response(Response.STATUS_CODE_NOTHING, "Not post message to host");
    }

    @Override
    /* package */ Response removeContent(HashMap<String, String> map) {
        String musicId = map.get(PartyShareCommand.PARAM_MUSIC_ID);
        LogUtil.d(LogUtil.LOG_TAG, "PostMusicContent.removeContent musicId : " + musicId);

        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(
                PartyShareCommand.PARAM_CMD,
                String.valueOf(PartyShareCommand.CMD_REMOVE_MUSIC_CONTENT)));
        params.add(new BasicNameValuePair(PartyShareCommand.PARAM_MUSIC_ID, musicId));

        return postMessageToHost(params);
    }
}
