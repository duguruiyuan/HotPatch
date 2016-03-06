package com.lee.patchlib;

import java.lang.reflect.Array;

/**
 * Created by jiangli on 16/3/5.
 */
/*package*/ class ArrayUtils {

    public static Object[] combineArray(Object first, Object second) {
        if (null == first || null == second)
            throw new IllegalArgumentException("combine array can not be null");
        if (!first.getClass().isArray() || !second.getClass().isArray())
            throw new IllegalArgumentException("combine array must be Array");
        Class type = first.getClass().getComponentType();
        if (!type.equals(second.getClass().getComponentType()))
            throw new IllegalArgumentException("combine array must have the same ComponentType");
        Object[] firstArr = (Object[]) first;
        Object[] secondArr = (Object[]) second;
        Object[] arr = (Object[]) Array.newInstance(type, firstArr.length + secondArr.length);
        System.arraycopy(firstArr, 0, arr, 0, firstArr.length);
        System.arraycopy(secondArr, 0, arr, firstArr.length, secondArr.length);
        return arr;
    }

}
