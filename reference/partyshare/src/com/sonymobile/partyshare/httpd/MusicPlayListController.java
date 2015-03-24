/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.MusicProviderUtil;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.service.MusicService;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.DeviceInfo;
import com.sonymobile.partyshare.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MusicPlayListController class.
 * This class performs processing of the command and event of HttpServer for music.
 */
public class MusicPlayListController {
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

    /**
     * When music is added by client, insert music info into host.
     * When event reaches simultaneously, it adds in order.
     * @param context Context.
     * @param param Music parameter.
     */
    public static void addMusicContent(final Context context, final Map<String, String> param) {
        if (!ConnectionManager.isGroupOwner()) {
            LogUtil.e(LogUtil.LOG_TAG, "MusicPlayListController.addMusicContent : Not host");
            return;
        }

        synchronized (sObject) {
            mAddEvents.add(param);
            if (mThreadStart) {
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicContent : during processing");
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
                                addMusicToHost(context, prm);
                            }
                        } catch (Exception e) {
                            LogUtil.e(LogUtil.LOG_TAG,
                                    "MusicPlayListController.addMusicContent : " + e.toString());
                        } finally {
                            synchronized (sObject) {
                                if (mAddEvents.isEmpty()) {
                                    LogUtil.d(LogUtil.LOG_TAG, "MusicPlayListController" +
                                            ".addMusicContent : No new request");
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
     * Add music to host.
     * @param context Context.
     * @param param Music parameter.
     */
    private static void addMusicToHost(Context context, Map<String, String> param) {
        String title = param.get(PartyShareCommand.PARAM_MUSIC_TITLE);
        String artist = param.get(PartyShareCommand.PARAM_MUSIC_ARTIST);
        String time = param.get(PartyShareCommand.PARAM_MUSIC_TIME);
        String uri = param.get(PartyShareCommand.PARAM_MUSIC_URL);
        String address = param.get(PartyShareCommand.PARAM_MUSIC_OWNER_ADDRESS);
        String name = param.get(PartyShareCommand.PARAM_MUSIC_OWNER_NAME);

        LogUtil.d(LogUtil.LOG_TAG, "MusicPlayListController.addMusicToHost title  : " + title);
        LogUtil.d(LogUtil.LOG_TAG, "MusicPlayListController.addMusicToHost artist : " + artist);
        LogUtil.d(LogUtil.LOG_TAG, "MusicPlayListController.addMusicToHost time   : " + time);
        LogUtil.d(LogUtil.LOG_TAG, "MusicPlayListController.addMusicToHost uri    : " + uri);
        LogUtil.d(LogUtil.LOG_TAG, "MusicPlayListController.addMusicToHost address: " + address);
        LogUtil.d(LogUtil.LOG_TAG, "MusicPlayListController.addMusicToHost name   : " + name);

        try {
            addToHost(context, title, artist, Integer.parseInt(time), uri, address, name);
        } catch (NumberFormatException e) {
            LogUtil.e(LogUtil.LOG_TAG,
                    "MusicPlayListController.addMusicToHost name : " + e.toString());
        }
    }

    /**
     * Start MusicService to add music info into host's database.
     * @param context Context.
     * @param title Music title.
     * @param artist Music artist.
     * @param time Music time.
     * @param musicUri Music uri.
     * @param owner Owner's mac address.
     */
    private static void addToHost(Context context, String title, String artist,
            int time, String musicUri, String address, String name) {
        Intent intent = new Intent();
        intent.setClass(context, MusicService.class);
        intent.setAction(MusicService.ACTION_ADD);

        Bundle values = new Bundle();
        values.putString(MusicProvider.COLUMN_TITLE, title);
        values.putString(MusicProvider.COLUMN_ARTIST, artist);
        values.putInt(MusicProvider.COLUMN_TIME, time);
        values.putString(MusicProvider.COLUMN_MUSIC_URI, musicUri);
        values.putString(MusicProvider.COLUMN_OWNER_ADDRESS, address);
        values.putString(MusicProvider.COLUMN_OWNER_NAME, name);
        intent.putExtras(values);

        context.startService(intent);
    }

    /**
     * When play list is updated on host, load latest play list in client.
     * When event reaches simultaneously, process 1 time.
     * @param context Context.
     */
    public static void reloadPlayList(final Context context) {
        if (ConnectionManager.isGroupOwner()) {
            LogUtil.e(LogUtil.LOG_TAG, "MusicPlayListController.reloadPlayList : Not client");
            return;
        }

        synchronized (sObject) {
            mEventReached = true;
            if (mThreadStart) {
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.reloadPlayList : during processing");
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
                            addMusicToClient(context);
                        } catch (Exception e) {
                            LogUtil.e(LogUtil.LOG_TAG,
                                    "MusicPlayListController.reloadPlayList : " + e.toString());
                        } finally {
                            synchronized (sObject) {
                                if (!mEventReached) {
                                    LogUtil.d(LogUtil.LOG_TAG, "MusicPlayListController"
                                            + ".reloadPlayList : no new request");
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
     * Add music to client.
     * - Delete play list from database.
     * - Get play list from host's json file.
     * - Add music info into database.
     * @param context Context.
     */
    private static void addMusicToClient(Context context) {
        ArrayList<JSONObject> objects = JsonUtil.getJsonObjectFromHost(
                context, JsonUtil.CONTENT_TYPE_MUSIC);

        if (objects == null) {
            LogUtil.d(LogUtil.LOG_TAG,
                    "MusicPlayListController.addMusicToClient : No data");
            return;
        }

        ContentValues[] valuesList = new ContentValues[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            JSONObject content = objects.get(i);
            try {
                String title = content.getString(PartyShareCommand.PARAM_MUSIC_TITLE);
                String artist = content.getString(PartyShareCommand.PARAM_MUSIC_ARTIST);
                String time = content.getString(PartyShareCommand.PARAM_MUSIC_TIME);
                String url = content.getString(PartyShareCommand.PARAM_MUSIC_URL);
                String address = content.getString(PartyShareCommand.PARAM_MUSIC_OWNER_ADDRESS);
                String name = content.getString(PartyShareCommand.PARAM_MUSIC_OWNER_NAME);
                String status = content.getString(PartyShareCommand.PARAM_MUSIC_STATUS);
                String musicId = content.getString(PartyShareCommand.PARAM_MUSIC_ID);
                String playNum = content.getString(PartyShareCommand.PARAM_MUSIC_PLAY_NUMBER);

                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient title   : " + title);
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient artist  : " + artist);
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient time    : " + time);
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient url     : " + url);
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient address : " + address);
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient name    : " + name);
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient status  : " + status);
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient musicId : " + musicId);
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient playNum : " + playNum);

                ContentValues values = new ContentValues();
                values.put(MusicProvider.COLUMN_TITLE, title);
                values.put(MusicProvider.COLUMN_ARTIST, artist);
                values.put(MusicProvider.COLUMN_TIME, Integer.parseInt(time));
                values.put(MusicProvider.COLUMN_MUSIC_URI, url);
                values.put(MusicProvider.COLUMN_OWNER_ADDRESS, address);
                values.put(MusicProvider.COLUMN_OWNER_NAME, name);
                values.put(MusicProvider.COLUMN_STATUS, status.equals("1") ? 1 : 0);
                values.put(MusicProvider.COLUMN_MUSIC_ID, Integer.parseInt(musicId));
                values.put(MusicProvider.COLUMN_PLAY_NUMBER, Integer.parseInt(playNum));

                valuesList[i] = values;
            } catch (JSONException e) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient : " + e.toString());
            } catch (NumberFormatException e) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "MusicPlayListController.addMusicToClient : " + e.toString());
            }
        }

        // play list delete all
        ProviderUtil.delete(context, MusicProvider.CONTENT_URI, null, null);
        // insert into play list
        ProviderUtil.bulkInsert(context, MusicProvider.CONTENT_URI, valuesList);
    }

    /**
     * Remove music.
     * Host   : Start MusicService (put in delete id).
     * Client : Reload music info from host.
     * @param context Context.
     * @param param Music parameter.
     */
    public static void removeMusic(Context context, Map<String, String> param) {
        try {
            if (ConnectionManager.isGroupOwner()) {
                String musicId = param.get(PartyShareCommand.PARAM_MUSIC_ID);
                LogUtil.d(LogUtil.LOG_TAG,
                        "MusicPlayListController.removeMusic musicId : " + musicId);

                Intent intent = new Intent(context, MusicService.class);
                intent.setAction(MusicService.ACTION_REMOVE);
                intent.putExtra("delete_id", Integer.parseInt(musicId));
                context.startService(intent);
            } else {
                reloadPlayList(context);
            }
        } catch (NumberFormatException e) {
           LogUtil.e(LogUtil.LOG_TAG, "MusicPlayListController.removeMusic : " + e.toString());
        }
    }

    /**
     * Remove music erased by Host from local_music table of client.
     * @param context Context.
     * @param param Music parameter.
     */
    public static void removeLocalMusic(Context context, Map<String, String> param) {
        String musicUri = param.get(PartyShareCommand.PARAM_MUSIC_LOCAL_PATH);
        LogUtil.d(LogUtil.LOG_TAG,
                "MusicPlayListController.removeLocalMusic musicUri : " + musicUri);

        String localId = Uri.parse(musicUri).getLastPathSegment();
        ProviderUtil.delete(
                context, MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                MusicProvider._ID + " = ?",
                new String[] { localId });
    }

    /**
     * Remove music from local_music table.
     * In the case of host, notify delete event to client if data is not shared by myself.
     * @param context Context.
     * @param musicId Music id (delete key).
     */
    public static void removeLocalMusic(Context context, int musicId) {
        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(
                    context, MusicProvider.CONTENT_URI,
                    new String[] {
                            MusicProvider.COLUMN_OWNER_ADDRESS, MusicProvider.COLUMN_MUSIC_URI },
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(musicId) },
                    null);

            if (cursor == null || cursor.getCount() == 0) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "MusicPlayListController.removeLocalMusic : no data");
                return;
            }

            cursor.moveToFirst();
            String musicUri = cursor.getString(
                    cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_URI));
            LogUtil.d(LogUtil.LOG_TAG,
                    "MusicPlayListController.removeLocalMusic musicUri : " + musicUri);

            if (ConnectionManager.isGroupOwner()) {
                // Get mac address.
                String macAddress = ConnectionManager.getMyAddress();

                // Get owner address which added music.
                String address = cursor.getString(
                        cursor.getColumnIndex(MusicProvider.COLUMN_OWNER_ADDRESS));

                if (!macAddress.equals(address)) {
                    LogUtil.d(LogUtil.LOG_TAG,
                            "MusicPlayListController.removeLocalMusic : music other than myself");

                    // Notify deletion to the client which added the music.
                    HashMap<String, String> param = new HashMap<String, String>();
                    param.put(PartyShareCommand.PARAM_MUSIC_LOCAL_PATH, musicUri);
                    PartyShareEventNotifier.notifyEventSpecifyClient(
                            context, PartyShareEvent.EVENT_REMOVE_LOCAL_MUSIC, address, param);
                    return;
                }
            }

            // Deletes the local music info which added by myself.
            String localId = Uri.parse(musicUri).getLastPathSegment();
            ProviderUtil.delete(
                    context, MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                    MusicProvider._ID + " = ?",
                    new String[] { localId });
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Start MusicService for play next.
     * @param context Context.
     * @param musicId Music id.
     */
    public static void playNextMusic(Context context, int musicId) {
        Intent intent = new Intent(context, MusicService.class);
        intent.putExtra("play_next", musicId);
        intent.setAction(MusicService.ACTION_PLAY_NEXT);
        context.startService(intent);
    }

    /**
     * Change owner name.
     * @param context Context.
     * @param address Owner's address.
     * @param name Owner's new name.
     */
    public static void changeName(Context context, String address, String name) {
        if (name == null) {
            ConnectionManager manager = ConnectionManager.getInstance(context);
            for (DeviceInfo info : manager.getGroupList()) {
                if (info.getDeviceAddress().equals(address)) {
                    name = info.getUserName();
                    break;
                }
            }
        }
        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.changeName address : " + address);
        LogUtil.d(LogUtil.LOG_TAG, "PartyShareHttpd.changeName name    : " + name);

        int count = MusicProviderUtil.updateOwnerName(context, address, name);
        if (count > 0 && ConnectionManager.isGroupOwner()) {
            MusicJsonFile.refreshJsonFile(context);
        }
    }
}
