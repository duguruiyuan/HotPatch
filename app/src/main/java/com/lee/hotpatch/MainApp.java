package com.lee.hotpatch;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import com.lee.patchlib.PatchUtils;


/**
 * Created by jiangli on 16/2/29.
 */
public class MainApp extends Application {
    private static final String TAG = "MainApp";

    @Override
    public void onCreate() {
        super.onCreate();
        PatchUtils.getInstance().init(getApplicationContext());
        PatchUtils.getInstance().hack();
        if (Build.VERSION.SDK_INT < 23 || PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Utils.tryCopyPatch();
            PatchUtils.getInstance().patch();
        }
    }
}
