package com.lee.patchlib;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author jiangli
 */
/*package*/ class Logger {

    private static final int DEBUG = 0;
    private static final int INFO = 1;
    private static final int ERROR = 2;
    private static final int level = DEBUG;

    private static final String divideLine = " ---------------------------- ";

    public static void d(String TAG, String msg) {
        if (level <= DEBUG) {
            Log.d(TAG, Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")\t" + msg);
        }
    }

    public static void d(String TAG, Throwable e) {
        d(TAG, wrapThroable(e));
    }

    public static void i(String TAG, String msg) {
        if (level <= INFO) {
            Log.i(TAG, Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")\t" + msg);
        }
    }

    public static void i(String TAG, Throwable e) {
        i(TAG, wrapThroable(e));
    }

    public static void e(String TAG, String msg) {
        if (level <= ERROR) {
            Log.e(TAG, Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")\t" + msg);
        }
    }

    public static void e(String TAG, Throwable e) {
        e(TAG, wrapThroable(e));
    }

    public static void dd(String TAG, String msg) {
        d(TAG, divideMessage(msg));
    }

    public static void di(String TAG, String msg) {
        i(TAG, divideMessage(msg));
    }

    public static void de(String TAG, String msg) {
        e(TAG, divideMessage(msg));
    }

    private static String divideMessage(String msg) {
        return divideLine + msg + divideLine;
    }

    private static String wrapThroable(Throwable e) {
        StringWriter writer = new StringWriter();
        try {
            e.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        } finally {
            try {
                writer.close();
            } catch (IOException e1) {
                // no-op
            }
        }
    }
}
