package com.lee.hotpatch;

import android.os.Environment;
import android.util.Log;

import com.lee.patchlib.PatchUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by jiangli on 16/3/5.
 */
public class Utils {
    private static final String TAG = "Utils";

    public static void tryCopyPatch() {
        File f = new File(Environment.getExternalStorageDirectory(), "patch.apk");
        if (f.exists()) {
            InputStream in = null;
            FileOutputStream out = null;
            try {
                in = new FileInputStream(f);
                out = new FileOutputStream(PatchUtils.getInstance().getPatchName());
                byte[] buf = new byte[1024];
                int len = -1;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                if (f.exists()) {
                    f.delete();
                }
            } catch (Exception e) {
                Log.i(TAG, "Failed to copy patch.apk from sdcard.");
            } finally {
                try {
                    in.close();
                } catch (Exception e) {
                    // no-op
                }
                try {
                    out.close();
                } catch (Exception e) {
                    // no-op
                }
            }
        }
    }
}
