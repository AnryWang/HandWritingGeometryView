package com.hand.writing.utils;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;

import static com.hand.writing.HandWritingViewHelper.DEBUG;


/**
 * Desc: 手写控件缓存工具类
 *
 * @author JiLin
 * @version 1.0
 * @since 2018/7/25 0025
 */
public class HandWritingCacheUtils {
    private static String TAG = "HandWritingCacheUtils";
    /**
     * 压力值现在只保留小数点后两位;也就是说最极端的情况下,会有"0.00 - 0.99"100个压力值;
     * 在笔迹字符串非常多的时候,缓存压力值带来的效率提升是显而易见的.
     */
    private static HashMap<String, Float> sPressureMaps = new HashMap<>(64);
    private static HashMap<String, Integer> sCachedIntMaps = new HashMap<>();

    /**
     * 文本转换成int类型，增加转换失败的异常捕获
     */
    public static int toInt(@NonNull String text) {
        return toInt(text, 0);
    }

    public static int cachedToInt(@NonNull String text) {
        Integer cachedInt = sCachedIntMaps.get(text);
        if (cachedInt != null) {
            return cachedInt;
        }

        int result = toInt(text, 0);
        sCachedIntMaps.put(text, result);
        return result;
    }

    /*int类型的数据存在的数据比较多，如果直接缓存，反而变成了负优化*/
    public static int toInt(@NonNull String text, int defValue) {
        int result = defValue;
        try {
            result = Integer.parseInt(text);
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, "toInt fail, text = " + text);
            }
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 文本转换成float类型，增加转换失败的异常捕获
     */
    public static float toFloat(@NonNull String text) {
        return toFloat(text, 0.0f);
    }

    public static float cachedToFloat(@NonNull String text) {
        // 缓存如果有则直接返回,不需要再次解析
        Float pressure = sPressureMaps.get(text);
        if (pressure != null) {
            return pressure;
        }

        float result = toFloat(text, 0.0f);
        sPressureMaps.put(text, result);
        return result;
    }

    public static float toFloat(@NonNull String text, float value) {
        float result = value;
        try {
            result = Float.parseFloat(text);
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, "toFloat fail, text = " + text);
            }
            e.printStackTrace();
        }

        return result;
    }
}
