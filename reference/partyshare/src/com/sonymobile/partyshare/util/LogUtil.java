/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.util;

import android.util.Log;

public class LogUtil {
    public static final String LOG_TAG = "PartyShare";

    public static final boolean IS_LOGGABLE_VERBOSE = Log.isLoggable(LOG_TAG, Log.VERBOSE);

    public static final boolean IS_LOGGABLE_DEBUG = Log.isLoggable(LOG_TAG, Log.DEBUG);

    public static final boolean IS_LOGGABLE_INFO = Log.isLoggable(LOG_TAG, Log.INFO);

    public static final boolean IS_LOGGABLE_WARN = Log.isLoggable(LOG_TAG, Log.WARN);

    public static final boolean IS_LOGGABLE_ERROR = Log.isLoggable(LOG_TAG, Log.ERROR);

    private static final boolean IS_LOGGABLE = false;

    public static int v(String tag, String... msg) {
        int ret = 0;

        if (IS_LOGGABLE || IS_LOGGABLE_VERBOSE) {
            String str = concatenate(msg);
            ret = Log.v(tag, str);
        }
        return ret;
    }

    public static int d(String tag, String... msg) {
        int ret = 0;

        if (IS_LOGGABLE || IS_LOGGABLE_DEBUG) {
            String str = concatenate(msg);
            ret = Log.d(tag, str);
        }
        return ret;
    }

    public static int i(String tag, String... msg) {
        int ret = 0;

        if (IS_LOGGABLE || IS_LOGGABLE_INFO) {
            String str = concatenate(msg);
            ret = Log.i(tag, str);
        }
        return ret;
    }

    public static int w(String tag, String... msg) {
        int ret = 0;

        if (IS_LOGGABLE || IS_LOGGABLE_WARN) {
            String str = concatenate(msg);
            ret = Log.w(tag, str);
        }
        return ret;
    }

    public static int e(String tag, String... msg) {
        int ret = 0;
        if (IS_LOGGABLE || IS_LOGGABLE_ERROR) {
            String str = concatenate(msg);
            ret = Log.e(tag, str);
        }
        return ret;
    }

    public static int e(String tag, Throwable th, String... msg) {
        int ret = 0;
        if (IS_LOGGABLE || IS_LOGGABLE_ERROR) {
            String str = concatenate(msg);
            ret = Log.e(tag, str, th);
        }
        return ret;
    }

    private static String concatenate(String[] str) {
        StringBuffer buf = new StringBuffer();
        if (str != null) {
            for (String s : str) {
                buf.append(s);
            }
        }
        return buf.toString();
    }
}
