/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.Context;
import android.database.Cursor;

import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.util.LogUtil;

import java.util.HashMap;

/**
 * MusicJsonFile class.
 * This class processes the json file for music.
 */
public class MusicJsonFile {
    /** Json file name. */
    public static final String FILENAME = "playlist.json";
    /** Json array name. */
    public static final String KEY_JSON_ARRAY = "music";
    /** Object. */
    private static final Object sObject = new Object();
    /** Instance of thread. */
    private static Thread mThread;
    /** Thread start flag. */
    private static boolean mThreadStart = false;
    /** Event reach flag. */
    private static boolean mEventReached = false;

    /**
     * Refresh music json file and notify event of LOAD_PLAYLIST to client.
     * When event reaches simultaneously, process 1 time.
     * @param context Context.
     */
    public static void refreshJsonFile(final Context context) {
        synchronized (sObject) {
            mEventReached = true;
            if (mThreadStart) {
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile : during processing");
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

                            createJsonFileForMusic(context);
                            PartyShareEventNotifier.notifyEvent(
                                    context, PartyShareEvent.EVENT_LOAD_PLAYLIST, null);
                        } catch (Exception e) {
                            LogUtil.e(LogUtil.LOG_TAG,
                                    "MusicJsonFile.refreshJsonFile : " + e.toString());
                        } finally {
                            synchronized (sObject) {
                                if (!mEventReached) {
                                    LogUtil.d(LogUtil.LOG_TAG,
                                            "MusicJsonFile.refreshJsonFile : No new request");
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
     * Recreate music json file.
     * @param context Context
     */
    private static void createJsonFileForMusic(Context context) {
        JsonUtil.clearJsonFile(context, JsonUtil.CONTENT_TYPE_MUSIC);

        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(
                    context, MusicProvider.CONTENT_URI, null, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile : no date");
                return;
            }

            HashMap<String, String> jsonData = new HashMap<String, String>();
            while (cursor.moveToNext()) {
                String title = cursor.getString(
                        cursor.getColumnIndex(MusicProvider.COLUMN_TITLE));
                String artist = cursor.getString(
                        cursor.getColumnIndex(MusicProvider.COLUMN_ARTIST));
                int time = cursor.getInt(
                        cursor.getColumnIndex(MusicProvider.COLUMN_TIME));
                String url = cursor.getString(
                        cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_URI));
                String address = cursor.getString(
                        cursor.getColumnIndex(MusicProvider.COLUMN_OWNER_ADDRESS));
                String name = cursor.getString(
                        cursor.getColumnIndex(MusicProvider.COLUMN_OWNER_NAME));
                int status = cursor.getInt(
                        cursor.getColumnIndex(MusicProvider.COLUMN_STATUS));
                int musicId = cursor.getInt(
                        cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID));
                int playNum = cursor.getInt(
                        cursor.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER));

                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile title   : " + title);
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile artist  : " + artist);
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile time    : " + time);
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile url     : " + url);
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile address : " + address);
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile name    : " + name);
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile status  : " + status);
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile musicId : " + musicId);
                LogUtil.d(LogUtil.LOG_TAG, "MusicJsonFile.refreshJsonFile playNum : " + playNum);

                jsonData.put(PartyShareCommand.PARAM_MUSIC_TITLE, title);
                jsonData.put(PartyShareCommand.PARAM_MUSIC_ARTIST, artist);
                jsonData.put(PartyShareCommand.PARAM_MUSIC_TIME, Integer.toString(time));
                jsonData.put(PartyShareCommand.PARAM_MUSIC_URL, url);
                jsonData.put(PartyShareCommand.PARAM_MUSIC_OWNER_ADDRESS, address);
                jsonData.put(PartyShareCommand.PARAM_MUSIC_OWNER_NAME, name);
                jsonData.put(PartyShareCommand.PARAM_MUSIC_STATUS, Integer.toString(status));
                jsonData.put(PartyShareCommand.PARAM_MUSIC_ID, Integer.toString(musicId));
                jsonData.put(
                        PartyShareCommand.PARAM_MUSIC_PLAY_NUMBER, Integer.toString(playNum));

                JsonUtil.addContent(context, JsonUtil.CONTENT_TYPE_MUSIC, jsonData);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
