/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */

package com.sonymobile.partyshare.ga;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.PhotoProvider;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.gagtmhelper.GaGtmUtils;

import java.io.File;

/**
 * Convenience class that handles Google analytics sessions and events
 */
public class TrackingUtil {

    // Google Analytics screens
    public static final String SCREEN_MAINSCREEN = "MainScreen";
    public static final String SCREEN_PARTYLIST = "PartyList";
    public static final String SCREEN_MEMBERS = "Members";
    public static final String SCREEN_MUSIC = "Music";
    public static final String SCREEN_PHOTO = "Photo";
    public static final String SCREEN_SETTINGS = "Settings";
    public static final String SCREEN_HELP = "Help";
    public static final String SCREEN_MUSIC_PICKER = "MusicPicker";
    public static final String SCREEN_LEGAL = "Legal";

    // Google Analytics categories
    public static final String CATEGORY_USER = "User";
    public static final String CATEGORY_JOIN = "Join";
    public static final String CATEGORY_TIME = "Time";
    public static final String CATEGORY_SHARE = "Share";

    // Google Analytics actions
    public static final String ACTION_START_ON_XPERIA = "start_on_xperia";
    public static final String ACTION_JOIN_TIME_WFD = "join_time_wfd";
    public static final String ACTION_JOIN_TIME_NFC = "join_time_nfc";
    public static final String ACTION_JOIN_CANCEL_TIME_WFD = "join_cancel_time_wfd";
    public static final String ACTION_JOIN_CANCEL_TIME_NFC = "join_cancel_time_nfc";
    public static final String ACTION_JOIN_ERROR = "join_error";
    public static final String ACTION_HOW_TO_JOIN = "how_to_join";
    public static final String ACTION_NUMBER_OF_MAX_MEMBERS = "number_of_max_members";
    public static final String ACTION_START_END_TIME = "start_end_time";
    public static final String ACTION_JOIN_LEAVE_TIME = "join_leave_time";
    public static final String ACTION_NUMBER_OF_PHOTO = "number_of_photo";
    public static final String ACTION_NUMBER_OF_MUSIC = "number_of_music";

    // Google Analytics labels
    public static final String LABEL_NFC = "NFC";
    public static final String LABEL_WIFI_DIRECT = "WiFi Direct";
    public static final String LABEL_XPERIA = "Xperia";
    public static final String LABEL_OTHER = "Other";

    // Indicator of join time
    public static final String LABEL_JOIN_TIME_0_199 = "00-01.99s";
    public static final String LABEL_JOIN_TIME_2_399 = "02-03.99s";
    public static final String LABEL_JOIN_TIME_4_799 = "04-07.99s";
    public static final String LABEL_JOIN_TIME_8_1599 = "08-15.99s";
    public static final String LABEL_JOIN_TIME_16_3199 = "16-31.99s";
    public static final String LABEL_JOIN_TIME_OVER_32 = "32s-";

    // Indicator of party time
    public static final String LABEL_PARTY_TIME_0_599 = "000-005.99s";
    public static final String LABEL_PARTY_TIME_6_1599 = "006-015.99s";
    public static final String LABEL_PARTY_TIME_16_4799 = "016-047.99s";
    public static final String LABEL_PARTY_TIME_48_13799 = "048-137.99s";
    public static final String LABEL_PARTY_TIME_138_39999 = "138-399.99s";
    public static final String LABEL_PARTY_TIME_OVER_400 = "400s-";

    // Indicator of photo number
    public static final String LABEL_NUMBER_OF_PHOTO_0_2 = "000-002";
    public static final String LABEL_NUMBER_OF_PHOTO_3_7 = "003-007";
    public static final String LABEL_NUMBER_OF_PHOTO_8_23 = "008-023";
    public static final String LABEL_NUMBER_OF_PHOTO_24_68 = "024-068";
    public static final String LABEL_NUMBER_OF_PHOTO_69_199 = "069-199";
    public static final String LABEL_NUMBER_OF_PHOTO_OVER_200 = "069-200";

    // Indicator of music number
    public static final String LABEL_NUMBER_OF_MUSIC_0_2 = "000-002";
    public static final String LABEL_NUMBER_OF_MUSIC_3_7 = "003-007";
    public static final String LABEL_NUMBER_OF_MUSIC_8_23 = "008-023";
    public static final String LABEL_NUMBER_OF_MUSIC_24_68 = "024-068";
    public static final String LABEL_NUMBER_OF_MUSIC_69_200 = "069-200";

    // Log tag
    private static final String GA_LOG_TAG = LogUtil.LOG_TAG + " GA";

    // Google Analytics Enable file path
    private static final String GA_OFF_FILE = "data/data/com.sonymobile.partyshare/files/GA_OFF";

    // Google Analytics constant value
    public static final String VALUE_COUNT = "1";

    // Join start time(UTC)
    private static long sJoinStartTime = 0;

    // Party start time(UTC)
    private static long sPartyStartTime = 0;

    // Max Join Members
    private static int sMaxJoinMembers = 1;

    /**
     * Start a new Tracking session
     */
    public synchronized static void startSession(Context context, Activity activity) {
        if (isGaEnabled()) {
            LogUtil.d(GA_LOG_TAG, "startSession(activity=" +
                activity.getLocalClassName() + ")");
            GoogleAnalytics.getInstance(context).reportActivityStart(activity);
        }
    }

    /**
     * Stop the Tracking session
     */
    public synchronized static void stopSession(Context context, Activity activity) {
        if (isGaEnabled()) {
            LogUtil.d(GA_LOG_TAG, "stopSession(activity=" +
                activity.getLocalClassName() + ")");
            GoogleAnalytics.getInstance(context).reportActivityStop(activity);
        }
    }

    /**
     * Set the specified screen
     */
    public synchronized static void setScreen(Context context, String screen) {
        if (isGaEnabled()) {
            LogUtil.d(GA_LOG_TAG, "setScreen(screen=" + screen + ")");
            GaGtmUtils.pushAppView(context, screen);
        }
    }

    /**
     * Send a new event associated with the specified category, action, label
     * and value.
     */
    public synchronized static void sendEvent(Context context, String category,
            String action, String label, String value) {
        if (isGaEnabled()) {
            LogUtil.d(GA_LOG_TAG, "sendEvent(category=" + category +
                    " action=" + action + " label=" + label + " value=" + value + ")");
            GaGtmUtils.pushEvent(context, category, action, label, value);
        }
    }

    /**
     * Initialize join time
     */
    public synchronized static void iniJoinStartTime() {
        if (isGaEnabled()) {
            LogUtil.d(GA_LOG_TAG, "iniJoinStartTime");
            sJoinStartTime = 0;
        }
    }

    /**
     * Set join time
     */
    public synchronized static void setJoinStartTime(long starttime) {
        if (isGaEnabled()) {
            LogUtil.d(GA_LOG_TAG, "setJoinStartTime(" + Long.toString(starttime) + ")");
            sJoinStartTime = starttime;
        }
    }

    /**
     * Initialize Max join members
     */
    public synchronized static void iniMaxJoinMembers() {
        if (isGaEnabled()) {
            LogUtil.d(GA_LOG_TAG, "iniMaxJoinMembers()");
            sMaxJoinMembers = 1;
        }
    }

    /**
     * Compare max members
     */
    public synchronized static void compareMaxJoinMembers(int members) {
        if (isGaEnabled()) {
            LogUtil.d(GA_LOG_TAG, "compareMaxJoinMembers(" + Long.toString(members) + ")");
            if (sMaxJoinMembers < members) {
                sMaxJoinMembers = members;
            }
        }
    }

    /**
     * Set Party start time
     */
    public synchronized static void setPartyStartTime(long starttime) {
        if (isGaEnabled()) {
            LogUtil.d(GA_LOG_TAG, "setPartyStartTime(" + Long.toString(starttime) + ")");
            sPartyStartTime = starttime;
        }
    }

    /**
     * Send join_time or join_cancel_time
     */
    public synchronized static void sendJoinTime(Context context, String action) {
        if (isGaEnabled()) {
            if (sJoinStartTime == 0) {
                // impossible route
                return;
            }

            float difftime = (float)(System.currentTimeMillis() - sJoinStartTime);
            long time = Math.round(difftime / 1000);

            String label = null;

            if (difftime <= 0) {
                // impossible route
            } else if (difftime < 2000) {
                label = LABEL_JOIN_TIME_0_199;
            } else if (difftime < 4000) {
                label = LABEL_JOIN_TIME_2_399;
            } else if (difftime < 8000) {
                label = LABEL_JOIN_TIME_4_799;
            } else if (difftime < 16000) {
                label = LABEL_JOIN_TIME_8_1599;
            } else if (difftime < 32000) {
                label = LABEL_JOIN_TIME_16_3199;
            } else {
                label = LABEL_JOIN_TIME_OVER_32;
            }

            if (label != null) {
                sendEvent(context, CATEGORY_JOIN, action, label, Long.toString(time));
            }
            sJoinStartTime = 0;
        }
    }

    /**
     * Send number_of_music
     */
    public synchronized static void sendShareMusicCount(Context context) {
        if (isGaEnabled()) {
            int count = ProviderUtil.getListSize(context, MusicProvider.CONTENT_URI);
            String label = null;

            if (count < 0) {
                // impossible route
            } else if (count < 3) {
                label = LABEL_NUMBER_OF_MUSIC_0_2;
            } else if (count < 8) {
                label = LABEL_NUMBER_OF_MUSIC_3_7;
            } else if (count < 24) {
                label = LABEL_NUMBER_OF_MUSIC_8_23;
            } else if (count < 69) {
                label = LABEL_NUMBER_OF_MUSIC_24_68;
            } else {
                label = LABEL_NUMBER_OF_MUSIC_69_200;
            }

            if (label != null) {
                sendEvent(context, CATEGORY_SHARE, ACTION_NUMBER_OF_MUSIC, label,
                        Long.toString(count));
            }
        }
    }

    /**
     * Send number_of_photo
     */
    public synchronized static void sendSharePhotoCount(Context context) {
        if (isGaEnabled()) {
            int count = ProviderUtil.getListSize(context, PhotoProvider.CONTENT_URI);
            String label = null;

            if (count < 0) {
                // impossible route
            } else if (count < 3) {
                label = LABEL_NUMBER_OF_PHOTO_0_2;
            } else if (count < 8) {
                label = LABEL_NUMBER_OF_PHOTO_3_7;
            } else if (count < 24) {
                label = LABEL_NUMBER_OF_PHOTO_8_23;
            } else if (count < 69) {
                label = LABEL_NUMBER_OF_PHOTO_24_68;
            } else if (count < 200) {
                label = LABEL_NUMBER_OF_PHOTO_69_199;
            } else {
                label = LABEL_NUMBER_OF_PHOTO_OVER_200;
            }

            if (label != null) {
                sendEvent(context, CATEGORY_SHARE, ACTION_NUMBER_OF_PHOTO, label,
                        Long.toString(count));
            }
        }
    }

    /**
     * Send number_of_max_members
     */
    public synchronized static void sendMaxJoinMembers(Context context) {
        if (isGaEnabled()) {
            String max = Long.toString(sMaxJoinMembers);
            sendEvent(context, CATEGORY_JOIN, ACTION_NUMBER_OF_MAX_MEMBERS,
                    String.format("%1$02d", sMaxJoinMembers), max);
            iniMaxJoinMembers();
        }
    }

    /**
     * Send start_end_time or join_leave_time
     */
    public synchronized static void sendPartyTime(Context context, String action) {
        if (isGaEnabled()) {
            if (sPartyStartTime == 0) {
                return;
            }

            float difftime = (float)(System.currentTimeMillis() - sPartyStartTime);
            long time = Math.round(difftime) / 1000;
            String label = null;

            if (difftime <= 0) {
                // impossible route
            } else if (difftime < 6000) {
                label = LABEL_PARTY_TIME_0_599;
            } else if (difftime < 16000) {
                label = LABEL_PARTY_TIME_6_1599;
            } else if (difftime < 48000) {
                label = LABEL_PARTY_TIME_16_4799;
            } else if (difftime < 138000) {
                label = LABEL_PARTY_TIME_48_13799;
            } else if (difftime < 400000) {
                label = LABEL_PARTY_TIME_138_39999;
            } else {
                label = LABEL_PARTY_TIME_OVER_400;
            }

            if (label != null) {
                sendEvent(context, CATEGORY_TIME, action, label, Long.toString(time));
            }
            sPartyStartTime = 0;
        }
    }

   /**
     * Check GA enabled
     * GA is invalid GA_OFF When the file is not exist
     */
    public synchronized static boolean isGaEnabled() {
        File file = new File(GA_OFF_FILE);
        if (file.exists()){
            return false;
        } else {
            return true;
        }
    }
}
