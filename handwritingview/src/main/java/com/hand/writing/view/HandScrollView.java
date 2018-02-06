package com.hand.writing.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

import static com.hand.writing.HandWritingViewHelper.DEBUG;

/**
 * 手滑动的滑动容器
 */
public class HandScrollView extends ScrollView {
    private static final String TAG = "HandScrollView";
    public static final int TAG_TOUCH = 0;
    public static final int TAG_PEN = 1;

    // ------------------------------------------------------------------getter & setter
    private HandScrollViewCallback callback;

    public HandScrollViewCallback getCallback() {
        return callback;
    }

    public void setCallback(HandScrollViewCallback callback) {
        this.callback = callback;
    }

    // ------------------------------------------------------------------constructor
    public HandScrollView(Context context) {
        super(context);
    }

    public HandScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HandScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @Comment : 父容器的拦截触击事件
     * 手滑动时不拦截
     * 笔滑动时拦截
     */
    @SuppressLint("NewApi")
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int toolType = ev.getToolType(0);
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS) { //输入设备为手写笔
            if (DEBUG) {
                Log.i(TAG, "onInterceptTouchEvent() >>> toolType == TOOL_TYPE_STYLUS");
            }
            if (callback != null) {
                callback.back(TAG_PEN, null);
            }
            return false;//是笔，直接传下去
        } else { //非手写笔
            if (callback != null) {
                callback.back(TAG_TOUCH, null);
            }
        }

        return super.onInterceptTouchEvent(ev);
    }

    // ------------------------------------------------------------------inner class

    /**
     * 手滑动回调
     */
    public interface HandScrollViewCallback {
        void back(int tag, Object obj);
    }
}
