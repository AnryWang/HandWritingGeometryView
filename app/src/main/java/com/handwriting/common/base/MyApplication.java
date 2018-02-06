package com.handwriting.common.base;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.hand.writing.HandWritingViewHelper;

public class MyApplication extends Application {
    public static Handler mMainHandler;
    public static Application sMyApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        mMainHandler = new Handler(Looper.getMainLooper());
        sMyApplication = this;
        HandWritingViewHelper.DEBUG = true; // 日志标记值
    }

}
