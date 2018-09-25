package com.hand.writing.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

import static com.hand.writing.HandWritingViewHelper.DEBUG;

/**
 * 实现"笔写手滑"的自定义ScrollView
 */
public class HandScrollView extends ScrollView {
    private static final String TAG = "HandScrollView";

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
     * 手滑动时不拦截;笔滑动时拦截
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int toolType = ev.getToolType(0);
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS) { //输入设备为手写笔
            if (DEBUG) {
                Log.i(TAG, "onInterceptTouchEvent() >>> toolType == TOOL_TYPE_STYLUS");
            }

            return false;//是笔，直接传下去
        } else { //非手写笔
            return super.onInterceptTouchEvent(ev);
        }
    }
}
