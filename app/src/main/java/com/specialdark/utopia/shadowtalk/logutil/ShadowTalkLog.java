package com.specialdark.utopia.shadowtalk.logutil;

import android.util.Log;

/**
 * Created by weiwei on 3/30/15.
 */
public class ShadowTalkLog {

    // flag for enable debug log
    private static final boolean DEBUG = true;

    private static final String TAG = "ShadowTalkLog";

    public static void i(String log) {
        if (DEBUG) {
            Log.i(TAG, log);
        }
    }

    public static void w(String log) {
        if (DEBUG) {
            Log.w(TAG, log);
        }
    }

    public static void v(String log) {
        if (DEBUG) {
            Log.v(TAG, log);
        }
    }

    public static void e(String log) {
        if (DEBUG) {
            Log.e(TAG, log);
        }
    }
}
