package com.lee.patchlib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by jiangli on 16/3/5.
 */
/*package*/ class ReflectUtils {
    private static final String TAG = "ReflectUtils";

    public static Object getField(Object host, String fieldName) {
        try {
            Class klass = host.getClass();
            while (klass != null) {
                try {
                    Field field = klass.getDeclaredField(fieldName);
                    if (null != field) {
                        field.setAccessible(true);
                        return field.get(host);
                    }
                } catch (Exception e) {
                    // no-op
                }
                klass = klass.getSuperclass();
            }
        } catch (Throwable e) {
            Logger.d(TAG, e);
        }
        return null;
    }

    public static boolean setField(Object host, String fieldName, Object value) {
        try {
            Class klass = host.getClass();
            while (klass != null) {
                try {
                    Field field = klass.getDeclaredField(fieldName);
                    if (null != field) {
                        field.setAccessible(true);
                        field.set(host, value);
                        return true;
                    }
                } catch (Exception e) {
                    // no-op
                }
                klass = klass.getSuperclass();
            }
        } catch (Throwable e) {
            Logger.d(TAG, e);
        }
        return false;
    }

    public static Method getMethod(Class klass, String methodName, Class... paramClasses) {
        Method method = null;
        try {
            while (klass != null) {
                try {
                    method = klass.getDeclaredMethod(methodName, paramClasses);
                    if (null != method) {
                        method.setAccessible(true);
                        break;
                    }
                } catch (Exception e) {
                    // no-op
                }
                klass = klass.getSuperclass();
            }
        } catch (Throwable e) {
            Logger.d(TAG, e);
        }
        return method;
    }

    public static Constructor getConstructor(Class klass, Class... paramClasses) {
        try {
            Constructor constructor = klass.getConstructor(paramClasses);
            constructor.setAccessible(true);
            return constructor;
        } catch (Throwable e) {
            Logger.d(TAG, e);
        }
        return null;
    }
}
