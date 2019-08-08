package com.hlq.touchserver;

import android.util.Log;

public class LogUtil {
    private static final String TAG = "TouchServer";

    public static void d(String tag, String msg) {
        System.out.println(TAG + "_" + tag + " : " + msg);
        Log.w(TAG + "_" + tag, msg);
    }

    public static void d(String msg) {
        System.out.println(TAG + " : " + msg);
        Log.w(TAG, msg);
    }
}
