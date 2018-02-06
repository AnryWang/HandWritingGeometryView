package com.hand.writing;


import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * 手写view缓存类
 */
public class HandWritingViewCache implements IHandWritingViewCache {
    static String TAG = "HandWritingViewCache";
    static HashMap<String, Float> stringFloatHashMap = new HashMap<>();
    static WeakHashMap<String, Bitmap> stringBitmapHashMap = new WeakHashMap<>();

    //12.21
    public static float parseFloat(String floatStr) {
        Float value = stringFloatHashMap.get(floatStr);
        if (value == null) {
            value = Float.valueOf(floatStr);
            stringFloatHashMap.put(floatStr, value);
        }
        return value;
    }

    static HashMap<String, Integer> integerHashMap = new HashMap<>();

    public static int parseInt(String intStr) {
        Integer value = integerHashMap.get(intStr);
        if (value == null) {
            value = Integer.parseInt(intStr);
            integerHashMap.put(intStr, value);
        }
        return value;
    }

    @Override
    public Bitmap getCacheBitmap(String key) {
        Bitmap bitmap = stringBitmapHashMap.get(key);
        return bitmap;
    }

    @Override
    public void putCacheBitmap(String key, Bitmap bitmap) {
        stringBitmapHashMap.put(key, bitmap);
    }
}
