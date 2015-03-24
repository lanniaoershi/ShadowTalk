/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */

package com.sonymobile.partyshare.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.sonymobile.partyshare.httpd.PartyShareHttpd;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Music;
import com.sonymobile.partyshare.util.Utility;

/**
 * Music provider utility.
 */
public class MusicProviderUtil {
    /** Registration maximum to playlist. */
    public static final int PLAYLIST_MAX = 200;

    /**
     * Add music to MusicProvider
     * Host only
     * @return add result
     */
    public static String addMusic(Context context, Music music) {
        String result = "";

        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_TITLE, music.getMusicTitle());
        values.put(MusicProvider.COLUMN_ARTIST, music.getArtistName());
        values.put(MusicProvider.COLUMN_TIME, music.getTime());
        values.put(MusicProvider.COLUMN_MUSIC_URI, music.getURI());
        values.put(MusicProvider.COLUMN_OWNER_ADDRESS, music.getOwner());
        values.put(MusicProvider.COLUMN_OWNER_NAME, music.getName());

        int listId = music.getId();
        if (listId != -1) {
            values.put(MusicProvider.COLUMN_MUSIC_ID, listId);
        }

        int order = music.getOrder();
        if (order != 0) {
            values.put(MusicProvider.COLUMN_PLAY_NUMBER, order);
        }

        music.setOrder(order);

        Uri addedUri = ProviderUtil.insert(context, MusicProvider.CONTENT_URI, values);

        // Get list id when music has no list_id.
        if (addedUri == null || addedUri == Uri.EMPTY) {
            LogUtil.w(LogUtil.LOG_TAG, "addUri is EMPTY !!!");
            result = "Cannot add to database";
            return result;
        } else {
            LogUtil.w(LogUtil.LOG_TAG,
                    "[MusicRetriever] addUri = " + addedUri.toString());
        }
        return result;
    }

    public static void playNext(Context context, int reqId) {
        Uri uri = Uri.withAppendedPath(MusicProvider.CONTENT_URI_PLAY_NEXT, String.valueOf(reqId));
        ContentValues values = new ContentValues();
        ProviderUtil.update(context, uri, values, null, null);
    }

    public static void setPlayingStatus(Context context, int id, boolean isPlaying) {
        // Set desired playing status
        ContentValues values = new ContentValues();
        Uri playing = Uri.withAppendedPath(MusicProvider.CONTENT_URI,
                String.valueOf(id));
        values.clear();
        values.put(MusicProvider.COLUMN_STATUS, isPlaying ? 1 : 0);
        ProviderUtil.update(context, playing, values, null, null);
    }

    public static void removeMusic(Context context, int position) {
        ProviderUtil.delete(context, MusicProvider.CONTENT_URI,
                MusicProvider.COLUMN_MUSIC_ID + "=" + position, null);
    }

    public static Music getListItemById(Context context, int musicId) {
        Cursor cursor = null;
        Music music = null;
        try {
            cursor = ProviderUtil.query(context, MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(musicId) },
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                music = getMusicDataFromCursor(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return music;
    }

    public static int getListSize(Context context) {
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = ProviderUtil.query(
                    context,
                    MusicProvider.CONTENT_URI,
                    new String[] { BaseColumns._ID },
                    null, null, null);
            if (cursor != null) {
                count = cursor.getCount();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    public static int getCurrentTrack(Context context) {
        Cursor cursor = null;
        int trackId = 0;
        try {
            cursor = ProviderUtil.query(
                    context,
                    MusicProvider.CONTENT_URI,
                    null,
                    MusicProvider.COLUMN_STATUS + " = " + MusicProvider.MUSIC_STATUS_FOCUSED,
                    null, null);
            if (cursor != null && cursor.moveToFirst()) {
                trackId = cursor.getInt(cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return trackId;
    }

    public static int getNextTrack(Context context, int currentId, boolean isForward) {
        return getNextTrack(context, currentId, isForward, null);
    }

    public static int getNextTrack(
            Context context, int currentId, boolean isForward, String owner) {
        Music currentMusic = getListItemById(context, currentId);
        Music nextMusic = null;
        int nextId = 0;

        if (currentMusic != null) {
            int currentOrder = currentMusic.getOrder();

            String nextWhere;
            String nextSort;
            String[] args;
            if (isForward) {
                nextWhere = MusicProvider.COLUMN_PLAY_NUMBER + " > ?";
                nextSort = MusicProvider.COLUMN_PLAY_NUMBER;
            } else {
                nextWhere = MusicProvider.COLUMN_PLAY_NUMBER + " < ?";
                nextSort = MusicProvider.COLUMN_PLAY_NUMBER + " DESC";
            }

            if (owner != null && !owner.isEmpty()) {
                nextWhere += " AND " + MusicProvider.COLUMN_OWNER_ADDRESS + " != ?";
                args = new String[] { String.valueOf(currentOrder), owner};
            } else {
                args = new String[] { String.valueOf(currentOrder)};
            }

            Cursor cursor = null;
            try {
                cursor = ProviderUtil.query(
                        context,
                        MusicProvider.CONTENT_URI,
                        null,
                        nextWhere,
                        args,
                        nextSort);
                if (cursor != null && cursor.moveToFirst()) {
                    nextMusic = getMusicDataFromCursor(cursor);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            // Return first or last music.
            if (nextMusic == null) {
                cursor = null;
                try {
                    cursor = ProviderUtil.query(
                            context,
                            MusicProvider.CONTENT_URI,
                            null,
                            null,
                            null,
                            nextSort);
                    if (cursor != null && cursor.moveToFirst()) {
                        nextMusic = getMusicDataFromCursor(cursor);
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
        if (nextMusic != null) {
            nextId = nextMusic.getId();
        }
        return nextId;
    }

    public static Uri getMusicUri(Context context, Music music) {
        Uri uri = null;
        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(
                    context,
                    MusicProvider.CONTENT_URI,
                    new String[] { MusicProvider.COLUMN_OWNER_ADDRESS },
                    MusicProvider.COLUMN_MUSIC_ID + " = ?",
                    new String[] { String.valueOf(music.getId()) },
                    null);

            if (cursor != null) {
                cursor.moveToFirst();
                String macAddress = cursor.getString(
                        cursor.getColumnIndex(MusicProvider.COLUMN_OWNER_ADDRESS));
                String ipAddress = Utility.getIpAddress(context, macAddress);

                uri = Uri.parse(String.format("http://%s:%s%s",
                        ipAddress, PartyShareHttpd.PORT, music.getURI()));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        LogUtil.d(LogUtil.LOG_TAG, "MusicProviderUtil.getMusicUri uri " + uri);
        return uri;
    }

    /**
     * Check whether the number of playlists is the maximum.
     * @return true when playlist is reached maximum, otherwise false.
     */
    public static boolean isPlayListMaximum(Context context) {
        boolean result = false;
        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(
                    context, MusicProvider.CONTENT_URI,
                    new String[] { MusicProvider._ID },
                    null, null, null);

            if (cursor != null && cursor.getCount() >= PLAYLIST_MAX) {
                result = true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        LogUtil.d(LogUtil.LOG_TAG, "MusicProviderUtil.isPlayListMaximum return " + result);
        return result;
    }

    public static boolean hasLocalData(Context context) {
        boolean result = false;
        Cursor cursor = null;
        try {
            cursor = ProviderUtil.query(
                    context, MusicProvider.CONTENT_URI_LOCAL_MUSIC,
                    new String[] { MusicProvider._ID },
                    null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                result = true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        LogUtil.d(LogUtil.LOG_TAG, "MusicProviderUtil.hasLocalData return " + result);
        return result;
    }

    /**
     * Update owner name of playlist database.
     * @param context Context.
     * @param address Owner's mac address.
     * @param name Owner's name.
     * @return update count.
     */
    public static int updateOwnerName(Context context, String address, String name) {
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_OWNER_NAME, name);
        int count = ProviderUtil.update(
                context, MusicProvider.CONTENT_URI,
                values,
                MusicProvider.COLUMN_OWNER_ADDRESS + " = ?",
                new String[] { address });

        return count;
    }

    private static Music getMusicDataFromCursor(Cursor cursor) {
        // retrieve the indices of the columns where the ID, title, etc. of the song area.
        int titleColumn = cursor.getColumnIndex(MusicProvider.COLUMN_TITLE);
        int artistColumn = cursor.getColumnIndex(MusicProvider.COLUMN_ARTIST);
        int ownerColumn = cursor.getColumnIndex(MusicProvider.COLUMN_OWNER_ADDRESS);
        int nameColumn = cursor.getColumnIndex(MusicProvider.COLUMN_OWNER_NAME);
        int timeColumn = cursor.getColumnIndex(MusicProvider.COLUMN_TIME);
        int uriColumn = cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_URI);
        int idColumn = cursor.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID);
        int orderColumn = cursor.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER);

        Music music = new Music(
                cursor.getString(titleColumn),
                cursor.getString(artistColumn),
                cursor.getString(ownerColumn),
                cursor.getString(nameColumn),
                cursor.getInt(timeColumn),
                cursor.getString(uriColumn),
                cursor.getInt(idColumn),
                cursor.getInt(orderColumn));
        return music;
    }
}
