package com.lee.patchlib;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by jiangli on 16/3/5.
 */
public class PatchUtils {
    private static final String TAG = "PatchUtils";

    public static final String PATCH_VERSION = "1.0";

    public static final String HACK_PATH = "hack";
    public static final String HACK_NAME = "hack.apk";
    public static final String PATCH_PATH = "patch";
    public static final String PATCH_NAME = "patch.apk";

    private static PatchUtils sInstance = null;

    private Context context;

    private boolean isDebug = true;

    private boolean isPatched = false;

    public static PatchUtils getInstance() {
        if (null == sInstance) {
            synchronized (PatchUtils.class) {
                if (null == sInstance) {
                    sInstance = new PatchUtils();
                }
            }
        }

        return sInstance;
    }

    private PatchUtils() {

    }

    public void init(Context context) {
        this.context = context;
        File dir = new File(context.getFilesDir(), HACK_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dir = new File(context.getFilesDir(), PATCH_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void hack() {
        try {
            File f = new File(context.getFilesDir(), HACK_PATH + File.separator + HACK_NAME);
            if (!f.exists()) {
                InputStream in = null;
                FileOutputStream out = null;
                try {
                    in = context.getAssets().open(HACK_NAME);
                    out = new FileOutputStream(f);
                    byte[] buf = new byte[1024];
                    int len = -1;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }

                } catch (Exception e) {
                    Logger.i(TAG, "Failed to copy hack.apk from assets.");
                } finally {
                    closeSafely(in);
                    closeSafely(out);
                }
            }
            if (f.exists()) {
                if (isDebug || SignUtils.checkSign(context, f.getAbsolutePath())) {
                    synchronized (this) {
                        if (InjectUtils.inject(context.getClassLoader(), f, false)) {
                            Logger.i(TAG, "Hack Successfully.");
                        } else {
                            Logger.i(TAG, "Hack Failed.");
                        }
                    }
                } else {
                    // should not reach here
                    clearHack();
                    Logger.e(TAG, "signature of hack.apk is not match");
                }
            }
        } catch (Exception e) {
            Logger.d(TAG, e);

        }
    }

    public void setDebug(boolean isDebugOn) {
        isDebug = isDebugOn;
    }

    public void patch() {
        if (isPatched) return;
        File f = new File(getPatchName());
        if (f.exists()) {
            if ((isDebug || SignUtils.checkSign(context, getPatchName()))) {
                synchronized (this) {
                    if (isPatched) return;
                    if (InjectUtils.inject(context.getClassLoader(), f, true)) {
                        isPatched = true;
                        Logger.i(TAG, "Patch Successfully.");
                    } else {
                        Logger.i(TAG, "Patch Failed.");
                    }
                }
            } else {
                clearPatch();
                Logger.e(TAG, "signature of patch.apk is not match");
            }
        } else {
            Logger.i(TAG, "No Patch File.");
        }
    }

    public void clearHack() {
        File dir = new File(context.getFilesDir().getAbsolutePath() + File.separator + HACK_PATH);
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
    }

    public void clearPatch() {
        File dir = new File(context.getFilesDir().getAbsolutePath() + File.separator + PATCH_PATH);
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
    }

    public String getPatchName() {
        return context.getFilesDir().getAbsolutePath() + File.separator + PATCH_PATH + File.separator + PATCH_NAME;
    }

    private void closeSafely(InputStream in) {
        try {
            in.close();
        } catch (Exception e) {
            // no-op
        }
    }

    private void closeSafely(OutputStream out) {
        try {
            out.close();
        } catch (Exception e) {
            // no-op
        }
    }
}
