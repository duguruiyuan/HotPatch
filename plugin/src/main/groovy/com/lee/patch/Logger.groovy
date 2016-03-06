package com.lee.patch

import static com.lee.patch.Utils.closeSafely

class Logger {
    static int DEBUG = 0;
    static int INFO = 1;
    static int ERROR = 2;

    int level = 0;
    String tag = 'unknown'

    def d(String msg) {
        if (level <= DEBUG) {
            println "${tag} [DEBUG] ${new Date(System.currentTimeMillis())} : ${msg}"
        }
    }

    def i(String msg) {
        if (level <= INFO) {
            println "${tag} [INFO] ${new Date(System.currentTimeMillis())} : ${msg}"
        }
    }

    def e(String msg) {
        if (level <= ERROR) {
            println "${tag} [ERROR] ${new Date(System.currentTimeMillis())} : ${msg}"
        }
    }

    def d(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer))
        d("${writer.toString()}")
        closeSafely(writer)
    }

    def i(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer))
        i("${writer.toString()}")
        closeSafely(writer)
    }

    def e(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer))
        e("${writer.toString()}")
        closeSafely(writer)
    }
}