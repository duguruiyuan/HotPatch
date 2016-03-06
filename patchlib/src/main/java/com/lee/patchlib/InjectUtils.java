package com.lee.patchlib;

import java.io.File;

import static com.lee.patchlib.ReflectUtils.*;
import static com.lee.patchlib.ArrayUtils.*;

import dalvik.system.DexClassLoader;

/*package*/ final class InjectUtils {
    private static final String TAG = "InjectUtils";

    public static boolean inject(ClassLoader loader, File dexFile, boolean isHotFix) {
        if (dexFile != null && dexFile.exists()) {
            try {
                if (isAiLiYunOS()) {
                    return AiLiYunOS.inject(loader, dexFile, isHotFix);
                } else if (hasBaseDexClassLoader()) {
                    return ABOVE_V14.inject(loader, dexFile, isHotFix);
                } else {
                    return BELOW_OR_EQUAL_V14.inject(loader, dexFile, isHotFix);
                }
            } catch (Throwable e) {
                Logger.i(TAG, e);
            }
        }
        return false;
    }

    private static boolean isAiLiYunOS() {
        try {
            Class.forName("dalvik.system.LexClassLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasBaseDexClassLoader() {
        try {
            Class.forName("dalvik.system.BaseDexClassLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Warning : Need to Test
     */
    private final static class AiLiYunOS {
        private static final String[] fieldNames = new String[]{
                "mFiles",
                "mZips",
        };

        private final static boolean inject(ClassLoader loader, File dexFile, boolean isHotFix) {
            try {
                String replaceName = dexFile.getName().replaceAll("\\.[a-zA-Z0-9]+", ".lex");
                Object dexLoader = getConstructor(Class.forName("dalvik.system.LexClassLoader"), new Class[]{
                        String.class,
                        String.class,
                        String.class,
                        ClassLoader.class,
                }).newInstance(new Object[]{
                        dexFile.getParent() + File.separator + replaceName,
                        dexFile.getParent(),
                        dexFile.getAbsolutePath(),
                        loader,
                });
                Object injects = null;
                Object originals = null;
                injects = new String[]{
                        (String) getField(dexLoader, "mRawDexPath")
                };
                originals = getField(loader, "mPaths");
                setField(loader, "mPaths",
                        isHotFix ? combineArray(injects, originals) : combineArray(originals, injects));
                for (String fieldName : fieldNames) {
                    injects = getField(dexLoader, fieldName);
                    originals = getField(loader, fieldName);
                    setField(loader, fieldName,
                            isHotFix ? combineArray(injects, originals) : combineArray(originals, injects));
                }
                injects = getField(dexLoader, "mDexs");
                originals = getField(loader, "mLexs");
                setField(loader, "mLexs",
                        isHotFix ? combineArray(injects, originals) : combineArray(originals, injects));
                return true;
            } catch (Exception e) {
                Logger.i(TAG, e);
            }
            return false;
        }
    }

    private final static class BELOW_OR_EQUAL_V14 {
        private static final String[] fieldNames = new String[]{
                "mFiles",
                "mZips",
                "mDexs",
        };

        private final static boolean inject(ClassLoader loader, File dexFile, boolean isHotFix) {
            try {
                DexClassLoader dexLoader = new DexClassLoader(dexFile.getAbsolutePath(), dexFile.getParent(), dexFile.getAbsolutePath(), loader);
                Object injects = null;
                Object originals = null;
                injects = new String[]{
                        (String) getField(dexLoader, "mRawDexPath")
                };
                originals = getField(loader, "mPaths");
                setField(loader, "mPaths",
                        isHotFix ? combineArray(injects, originals) : combineArray(originals, injects));
                for (String fieldName : fieldNames) {
                    injects = getField(dexLoader, fieldName);
                    originals = getField(loader, fieldName);
                    setField(loader, fieldName,
                            isHotFix ? combineArray(injects, originals) : combineArray(originals, injects));
                }
                return true;
            } catch (Exception e) {
                Logger.i(TAG, e);
            }
            return false;
        }
    }

    private final static class ABOVE_V14 {
        private final static boolean inject(ClassLoader loader, File dexFile, boolean isHotFix) {
            try {
                DexClassLoader dexLoader = new DexClassLoader(dexFile.getAbsolutePath(), dexFile.getParent(), dexFile.getAbsolutePath(), loader);
                Object injects = getField(getField(dexLoader, "pathList"), "dexElements");
                Object pathList = getField(loader, "pathList");
                Object originals = getField(pathList, "dexElements");
                setField(pathList, "dexElements",
                        isHotFix ? combineArray(injects, originals) : combineArray(originals, injects));
                return true;
            } catch (Exception e) {
                Logger.i(TAG, e);
            }
            return false;
        }
    }
}