/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.sonymobile.partyshare.R;

/**
 * Settings.
 */
public class Setting {
    /** Download mode setting : auto. */
    public static final int PHOTO_DOWNLOAD_MODE_AUTO = 0;
    /** Download mode setting : manual. */
    public static final int PHOTO_DOWNLOAD_MODE_MANUAL = 1;

    /** Sort order setting : taken date in ascending. */
    public static final int PHOTO_SORT_TAKEN_DATE_ASC = 0;
    /** Sort order setting : taken date in descending. */
    public static final int PHOTO_SORT_TAKEN_DATE_DESC = 1;
    /** Sort order setting : shared date in ascending. */
    public static final int PHOTO_SORT_SHARED_DATE_ASC = 2;
    /** Sort order setting : shared date in descending. */
    public static final int PHOTO_SORT_SHARED_DATE_DESC = 3;

    /** Settings file name. */
    private static final String SETTINGS_FILE = "settings";

    /** Key : disclamer accepted version. */
    private static final String KEY_DISCLAMER_ACCEPTED_VERSION = "disclamer_accepted_version";
    /** Key : user name. */
    private static final String KEY_USER_NAME = "user_name";
    /** Key : display security alert. */
    private static final String KEY_DISPLAY_SECURITY_ALERT = "display_security_alert";
    /** Key : download mode. */
    private static final String KEY_DOWNLOAD_MODE = "download_mode";
    /** Key : sort order for photos. */
    private static final String KEY_SORT_PHOTO = "sort_photo";
    /** Key : photo save folder path. */
    private static final String KEY_PHOTO_SAVE_FOLDER = "photo_save_folder";
    /** Key : joined session info. */
    private static final String KEY_JOINED_SESSION_INFO = "joined_session_info";

    /**
     * Get disclamer accepted.
     * @param context Context
     * @return Disclamer accepted.
     */
    public static boolean getDisclamerAccepted(Context context) {
        boolean ret = false;
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        String currentVersion = Utility.getApplicationVersion(context);
        if (currentVersion.equals(prefs.getString(KEY_DISCLAMER_ACCEPTED_VERSION, ""))) {
            ret = true;
        }
        return ret;
    }

    /**
     * Set disclamer accepted.
     * @param context Context
     */
    public static void setDisclamerAccepted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(KEY_DISCLAMER_ACCEPTED_VERSION, Utility.getApplicationVersion(context));
        editor.commit();
    }

    /**
     * Get user name.
     * @param context Context
     * @return User name.
     */
    public static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);

        String userName = prefs.getString(KEY_USER_NAME, null);
        if (userName == null) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(
                        ContactsContract.Profile.CONTENT_URI, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME);
                    userName = cursor.getString(nameIndex);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (userName == null || userName.isEmpty()) {
                userName = context.getResources().getString(
                        R.string.party_share_strings_setup_not_set_txt);
            }
        }
        return userName;
    }

    /**
     * Set user name.
     * @param context Context
     * @param user User name
     */
    public static void setUserName(Context context, String user) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(KEY_USER_NAME, user);
        editor.commit();
    }

    /**
     * Get display security alert flag.
     * @param context Context
     * @return True if a party is hosted for the first time.
     */
    public static boolean getDisplaySecurityAlert(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DISPLAY_SECURITY_ALERT, true);
    }

    /**
     * Set display security alert flag.
     * @param context Context
     */
    public static void setDisplaySecurityAlert(Context context, boolean check) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putBoolean(KEY_DISPLAY_SECURITY_ALERT, check);
        editor.commit();
    }

    /**
     * Get download mode.
     * @param context Context
     * @return Download mode.
     */
    public static int getDownloadMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_DOWNLOAD_MODE, PHOTO_DOWNLOAD_MODE_AUTO);
    }

    /**
     * Set download mode.
     * @param context Context
     * @param value Download mode
     */
    public static void setDownloadMode(Context context, int value) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(KEY_DOWNLOAD_MODE, value);
        editor.commit();
    }

    /**
     * Get sort order setting for photos.
     * @param context Context
     * @return Sort order setting.
     */
    public static int getSortOrderPhoto(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SORT_PHOTO, PHOTO_SORT_TAKEN_DATE_ASC);
    }

    /**
     * Set sort order setting for photos.
     * @param context Context
     * @param value Sort order setting
     */
    public static void setSortOrderPhoto(Context context, int value) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(KEY_SORT_PHOTO, value);
        editor.commit();
    }

    /**
     * Get photo save folder path.
     * @param context Context
     * @return Photo save folder path.
     */
    public static String getPhotoSaveFolder(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PHOTO_SAVE_FOLDER, "");
    }

    /**
     * Set photo save folder path.
     * @param context Context
     * @param path Photo save folder path.
     */
    public static void setPhotoSaveFolder(Context context, String path) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(KEY_PHOTO_SAVE_FOLDER, path);
        editor.commit();
    }

    /**
     * Get joined session info.
     * @param context Context
     * @return Joined Session info (name, time and host address is concatenate).
     */
    public static String getJoinedSessionInfo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        return prefs.getString(KEY_JOINED_SESSION_INFO, null);
    }

    /**
     * Set joined session info.
     * @param context Context
     * @param name Joined session name
     * @param time The time which Joined session is created
     * @param hostAddr Host's mac address
     */
    public static void setJoinedSessionInfo(
            Context context, String name, String time, String hostAddr) {
        SharedPreferences prefs = context.getSharedPreferences(
                SETTINGS_FILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(KEY_JOINED_SESSION_INFO, name + time + hostAddr);
        editor.commit();
    }

}
