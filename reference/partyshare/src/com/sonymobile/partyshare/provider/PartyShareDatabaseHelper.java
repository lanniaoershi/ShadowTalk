/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sonymobile.partyshare.util.LogUtil;

/**
 * PartyShareDatabaseHelper
 */
public class PartyShareDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "partyshare.db";

    // playlist table
    static final String TABLE_PLAYLIST = "playlist";
    static final String TABLE_LOCAL_MUSIC = "local_music";

    // photo table
    static final String TABLE_PHOTO = "photo";
    static final String TABLE_LOCAL_PHOTO = "local_photo";

    // column for playlist table
    static final String COLUMN_MUSIC_ID = "music_id";
    static final String COLUMN_TITLE = "title";
    static final String COLUMN_ARTIST = "artist";
    static final String COLUMN_TIME = "time";
    static final String COLUMN_MUSIC_URI = "music_uri";
    static final String COLUMN_OWNER_ADDRESS = "owner_address"; //mac address
    static final String COLUMN_OWNER_NAME = "owner_name";
    static final String COLUMN_STATUS = "status";
    static final String COLUMN_PLAY_NUMBER = "play_number";

    // column for local_music table
    static final String COLUMN_MUSIC_LOCAL_PATH = "music_local_path";
    static final String COLUMN_MUSIC_MIMETYPE = "music_mimetype";

    // column for photo table
    static final String COLUMN_MASTER_THUMBNAIL_PATH = "master_thumbnail_path";
    static final String COLUMN_MASTER_FILE_PATH = "master_file_path";
    static final String COLUMN_MIME_TYPE = "mime_type";
    static final String COLUMN_SHARED_DATE = "shared_date";
    static final String COLUMN_TAKEN_DATE = "taken_date";
    static final String COLUMN_LOCAL_THUMBNAIL_PATH = "local_thumbnail_path";
    static final String COLUMN_LOCAL_FILE_PATH = "local_file_path";
    static final String COLUMN_DL_STATE = "dl_state";

    // column for local_photo table
    static final String COLUMN_QUERY_PATH = "query_path";
    static final String COLUMN_THUMBNAIL_PATH = "thumbnail_path";
    static final String COLUMN_FILE_PATH = "file_path";

    private static PartyShareDatabaseHelper sInstance;

    private static final String TRIGGER_MUSIC_RENUMBERING =
            "CREATE TRIGGER music_renumbering delete on " + TABLE_PLAYLIST + " " +
            "BEGIN " +
                "update " + TABLE_PLAYLIST + " set " + COLUMN_PLAY_NUMBER + " = " +
                COLUMN_PLAY_NUMBER + " -1 " +
                "where " + COLUMN_PLAY_NUMBER + " > old." + COLUMN_PLAY_NUMBER + "; " +
            "END";

    private static final String TRIGGER_MUSIC_STATUS_INITIALIZE =
            "CREATE TRIGGER music_status_initialize before update of " +
                COLUMN_STATUS + " on " + TABLE_PLAYLIST +
                " when new." + COLUMN_STATUS + " = 1 " +
            "BEGIN " +
                "update " + TABLE_PLAYLIST + " set " + COLUMN_STATUS + " = 0 " +
                "where " + COLUMN_STATUS + " = 1; " +
            "END";

    private static final String TRIGGER_MUSIC_ID_SET =
            "CREATE TRIGGER music_id_set after insert on " + TABLE_PLAYLIST + " " +
            "BEGIN " +
                "update " + TABLE_PLAYLIST + " set " + COLUMN_MUSIC_ID + " = _id " +
                "where _id = new._id and " + COLUMN_MUSIC_ID + " IS NULL; " +
            "END";

    private PartyShareDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Return a singleton helper.
     *
     * @param context Context
     * @return PartyShareDatabaseHelper object.
     */
    static synchronized PartyShareDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PartyShareDatabaseHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareDatabaseHelper.onCreate");
        createPlaylistTable(db);
        createLocalMusicTable(db);
        createTriggerForPlaylist(db);
        createPhotoTable(db);
        createLocalPhotoTable(db);

        return;
    }

    private void createPlaylistTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PLAYLIST +
            "(_id INTEGER PRIMARY KEY," +
                COLUMN_TITLE + " TEXT," +
                COLUMN_ARTIST + " TEXT," +
                COLUMN_TIME + " INTEGER," +
                COLUMN_MUSIC_URI + " TEXT," +
                COLUMN_OWNER_ADDRESS + " TEXT," +
                COLUMN_OWNER_NAME + " TEXT," +
                COLUMN_STATUS + " INTEGER," +
                COLUMN_MUSIC_ID + " INTEGER," +
                COLUMN_PLAY_NUMBER + " INTEGER" +
            ");");
    }

    private void createLocalMusicTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_LOCAL_MUSIC +
            "(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_MUSIC_LOCAL_PATH + " TEXT," +
                COLUMN_MUSIC_MIMETYPE + " TEXT" +
            ");");
    }

    private void createPhotoTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PHOTO +
                "(_id INTEGER PRIMARY KEY," +
                    COLUMN_MASTER_THUMBNAIL_PATH + " TEXT," +
                    COLUMN_MASTER_FILE_PATH + " TEXT," +
                    COLUMN_SHARED_DATE + " TEXT," +
                    COLUMN_TAKEN_DATE + " TEXT," +
                    COLUMN_OWNER_ADDRESS + " TEXT," +
                    COLUMN_LOCAL_THUMBNAIL_PATH + " TEXT," +
                    COLUMN_LOCAL_FILE_PATH + " TEXT," +
                    COLUMN_MIME_TYPE + " TEXT," +
                    COLUMN_DL_STATE + " INTEGER" +
                ");");
    }

    private void createLocalPhotoTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_LOCAL_PHOTO +
            "(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_QUERY_PATH + " TEXT," +
                COLUMN_THUMBNAIL_PATH + " TEXT," +
                COLUMN_FILE_PATH + " TEXT" +
            ");");
    }

    private void createTriggerForPlaylist(SQLiteDatabase db) {
        db.execSQL(TRIGGER_MUSIC_RENUMBERING);
        db.execSQL(TRIGGER_MUSIC_STATUS_INITIALIZE);
        db.execSQL(TRIGGER_MUSIC_ID_SET);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtil.v(LogUtil.LOG_TAG, "onUpgrade - oldVersion : " + oldVersion +
                ", newVersion : " + newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
