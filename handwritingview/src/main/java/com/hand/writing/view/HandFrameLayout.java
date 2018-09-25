package com.hand.writing.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.hand.writing.HandWritingViewHelper;

import static com.hand.writing.HandWritingViewHelper.DEBUG;

/**
 * 控制手写事件分发的FrameLayout.(没有手写笔的设备实现单指书写,多指滑动)
 */
public class HandFrameLayout extends FrameLayout {
    private static final String TAG = "HandFrameLayout";
    // -------------------------fields
    //是否分发子视图touch事件
    private boolean mIsDispatch = true;
    //是否是带有手写笔的设备
    private boolean mIsStylusDevice;
    //外部scrollView是否可被滑动
    private boolean mEnableScroll = true;

    /**
     * 动作分发视图
     */
    private View actionDispatchView;

    // -------------------------constructors
    public HandFrameLayout(Context context) {
        super(context);
    }

    public HandFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HandFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 布局完成调用的事件
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        actionDispatchView = getChildAt(0);
    }

    /**
     * 拦截touch事件
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mIsStylusDevice = !HandWritingViewHelper.isSpecialDevice();

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (DEBUG) {
            Log.i(TAG, "pressure >>> " + String.valueOf(event.getPressure()));
        }
        if (mIsStylusDevice) {
            if (actionDispatchView != null) {//分发子视图
                if (DEBUG) {
                    Log.i(TAG, "手写设备 >>> dispatchTouchEvent");
                }
                return actionDispatchView.dispatchTouchEvent(event);
            }
            if (!mEnableScroll) {
                disableScrollImpl();
            }
            return super.onTouchEvent(event);
        }


        // Handle touch events here...
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (DEBUG) {
                    Log.i(TAG, "ACTION_DOWN");
                }
                mIsDispatch = true;
                disableScrollImpl();//单点，拦截touch事件,使父控件不接受
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (DEBUG) {
                    Log.i(TAG, "ACTION_POINTER_DOWN");
                }
                mIsDispatch = false;
                enableScrollImpl();//多点，不拦截touch事件,使父控件可接受
                break;
            case MotionEvent.ACTION_CANCEL:
                if (DEBUG) {
                    Log.i(TAG, "ACTION_CANCEL");
                }
                disableScrollImpl();//单点，拦截touch事件,使父控件不接受
                mIsDispatch = true;
                break;
            case MotionEvent.ACTION_UP:
                if (DEBUG) {
                    Log.i(TAG, "ACTION_UP");
                }
                enableScrollImpl();
                mIsDispatch = true;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
        }

        if (mIsDispatch && actionDispatchView != null) {//分发子视图
            if (DEBUG) {
                Log.i(TAG, "非手写设备 >>> dispatchTouchEvent");
            }
            return actionDispatchView.dispatchTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    public void disableScroll() {
        mEnableScroll = false;
    }

    public void enableScroll() {
        mEnableScroll = true;
    }

    /**
     * disables intercept touchevents
     */
    private void disableScrollImpl() {
        requestDisallowInterceptTouchEvent(true);
    }

    /**
     * enables intercept touchevents
     */
    private void enableScrollImpl() {
        requestDisallowInterceptTouchEvent(false);
    }

    // -------------------------getter & setter
    public View getActionDispatchView() {
        return actionDispatchView;
    }

    public void setActionDispatchView(View actionDispatchView) {
        this.actionDispatchView = actionDispatchView;
    }
}
