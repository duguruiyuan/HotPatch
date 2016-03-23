package com.lee.patchlib;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by jiangli on 16/3/21.
 */
public final class SignUtils {

    private SignUtils() {

    }

    public static boolean checkSign(Context ctx, String apkPath) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            if (null != pi) {
                byte[] myCertBytes = CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(pi.signatures[0].toByteArray()))
                        .getEncoded();

                JarFile jarFile = new JarFile(apkPath);
                try {
                    final List<JarEntry> toVerify = new ArrayList<>();
                    Enumeration<JarEntry> i = jarFile.entries();
                    while (i.hasMoreElements()) {
                        final JarEntry entry = i.nextElement();
                        if (entry.isDirectory()) continue;
                        if (entry.getName().startsWith("META-INF/")) continue;
                        toVerify.add(entry);
                    }

                    Class verifyClass = Class.forName("java.util.jar.JarVerifier");
                    Method getCertificateChains = ReflectUtils.getMethod(verifyClass, "getCertificateChains", String.class);
                    Object verifier = ReflectUtils.getField(jarFile, "verifier");
                    for (JarEntry entry : toVerify) {
                        final Certificate[][] entryCerts = loadCertificates(jarFile, entry, getCertificateChains, verifier);
                        Certificate cert = entryCerts[0][0];
                        if (null != cert) {
                            if (!Arrays.equals(myCertBytes, cert.getEncoded())) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                    return true;
                } finally {
                    if (null != jarFile) {
                        try {
                            jarFile.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static Certificate[][] loadCertificates(JarFile jarFile, JarEntry entry, Method getCertificateChains, Object verifier)
            throws Exception {
        InputStream is = null;
        try {
            is = jarFile.getInputStream(entry);
            readFullyIgnoringContents(is);
            return (Certificate[][]) getCertificateChains.invoke(verifier, entry.getName());
        } finally {
            try {
                if (null != is)
                    is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static AtomicReference<byte[]> sBuffer = new AtomicReference<byte[]>();

    private static long readFullyIgnoringContents(InputStream in) throws IOException {
        byte[] buffer = sBuffer.getAndSet(null);
        if (buffer == null) {
            buffer = new byte[4096];
        }

        int n = 0;
        int count = 0;
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {
            count += n;
        }

        sBuffer.set(buffer);
        return count;
    }
}
