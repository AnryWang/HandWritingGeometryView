package com.hand.writing;


import android.graphics.Bitmap;

/**
 * 手写view缓存类
 */
public interface IHandWritingViewCache {
    /**
     * 缓存笔记 bitmap
     *
     * @param key    笔记
     * @param bitmap bitmap
     */
    void putCacheBitmap(String key, Bitmap bitmap);

    /**
     * 根据笔记获取bitmap
     */
    Bitmap getCacheBitmap(String key);
}
