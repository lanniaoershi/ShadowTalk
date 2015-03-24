/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

import android.provider.BaseColumns;

import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.PhotoProvider;

/**
 * PartyShareCommand class.
 * These commands are sent to host from client.
 */
public class PartyShareCommand {
    /** Parameter name of command. */
    public static final String PARAM_CMD = "party_cmd";

    /** Command of add music. */
    public static final int CMD_ADD_MUSIC_CONTENT = 0;
    /** Command of remove music. */
    public static final int CMD_REMOVE_MUSIC_CONTENT = 1;
    /** Command of play next of music. */
    public static final int CMD_PLAY_NEXT = 2;
    /** Command of add photo. */
    public static final int CMD_ADD_PHOTO_CONTENT = 3;
    /** Command of remove photo. */
    public static final int CMD_REMOVE_PHOTO_CONTENT = 4;
    /** Command of change name. */
    public static final int CMD_CHANGE_NAME = 5;
    /** Command of delete client data. */
    public static final int CMD_DELETE_CLIENT_DATA = 6;
    /** Command of event reception. */
    public static final int CMD_CLIENT_RECV_EVENT = 7;

    /** Name of the command parameter for HTTP request parameter. */
    /** id. */
    public static final String PARAM_ID = BaseColumns._ID;
    /** owner address. */
    public static final String PARAM_OWNER_ADDRESS = "owner_address";

    /** title. */
    public static final String PARAM_MUSIC_TITLE = MusicProvider.COLUMN_TITLE;
    /** artist. */
    public static final String PARAM_MUSIC_ARTIST = MusicProvider.COLUMN_ARTIST;
    /** time. */
    public static final String PARAM_MUSIC_TIME = MusicProvider.COLUMN_TIME;
    /** uri. */
    public static final String PARAM_MUSIC_URL = MusicProvider.COLUMN_MUSIC_URI;
    /** owner address. */
    public static final String PARAM_MUSIC_OWNER_ADDRESS = MusicProvider.COLUMN_OWNER_ADDRESS;
    /** owner name. */
    public static final String PARAM_MUSIC_OWNER_NAME = MusicProvider.COLUMN_OWNER_NAME;
    /** status. */
    public static final String PARAM_MUSIC_STATUS = MusicProvider.COLUMN_STATUS;
    /** music id. */
    public static final String PARAM_MUSIC_ID = MusicProvider.COLUMN_MUSIC_ID;
    /** play number. */
    public static final String PARAM_MUSIC_PLAY_NUMBER = MusicProvider.COLUMN_PLAY_NUMBER;
    /** local file path. */
    public static final String PARAM_MUSIC_LOCAL_PATH = MusicProvider.COLUMN_MUSIC_LOCAL_PATH;

    /** master thumbnail path. */
    public static final String PARAM_PHOTO_MASTER_THUMBNAIL_PATH =
            PhotoProvider.COLUMN_MASTER_THUMBNAIL_PATH;
    /** master file path. */
    public static final String PARAM_PHOTO_MASTER_FILE_PATH =
            PhotoProvider.COLUMN_MASTER_FILE_PATH;
    /** shared date. */
    public static final String PARAM_PHOTO_SHARED_DATE = PhotoProvider.COLUMN_SHARED_DATE;
    /** taken date. */
    public static final String PARAM_PHOTO_TAKEN_DATE = PhotoProvider.COLUMN_TAKEN_DATE;
    /** owner address. */
    public static final String PARAM_PHOTO_OWNER_ADDRESS = PhotoProvider.COLUMN_OWNER_ADDRESS;
    /** local thumbnail path, */
    public static final String PARAM_PHOTO_LOCAL_THUMBNAIL_PATH =
            PhotoProvider.COLUMN_LOCAL_THUMBNAIL_PATH;
    /** local file path. */
    public static final String PARAM_PHOTO_LOCAL_FILE_PATH = PhotoProvider.COLUMN_LOCAL_FILE_PATH;
    /** mime type. */
    public static final String PARAM_PHOTO_MIME_TYPE = PhotoProvider.COLUMN_MIME_TYPE;
    /** thumbnail path for local_photo. */
    public static final String PARAM_PHOTO_THUMBNAIL_PATH = PhotoProvider.COLUMN_THUMBNAIL_PATH;
    /** file path.for local_path */
    public static final String PARAM_PHOTO_FILE_PATH = PhotoProvider.COLUMN_FILE_PATH;
    /** thubnail file name. */
    public static final String PARAM_PHOTO_THUMBNAIL_FILENAME = "thumbnail_filename";
}
