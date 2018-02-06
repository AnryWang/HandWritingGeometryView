package com.handwriting.common.widget;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.handwriting.common.util.OnClickHelper;
import com.handwriting.demo.R;

/**
 * Desc: 公共头布局
 * Copyright: Copyright (c) 2016
 *
 * @author JiLin
 * @version 1.0
 */
public class HeadWidget {
    private final Activity mActivity;
    /**
     * 布局填充器
     */
    private LayoutInflater mInflater;
    /**
     * 内容根布局
     */
    private FrameLayout mContentView;
    /**
     * 左侧根布局
     */
    private FrameLayout mHeadLeftLayout;
    /**
     * 左侧图片按钮
     */
    private ImageView mHeadLeftImgIv;
    /**
     * 左侧文本,默认为""
     */
    private TextView mHeadLeftTextTv;
    /**
     * 左侧消息按钮根布局(带小红点),默认为隐藏状态
     */
    private RelativeLayout mHeadLeftMsgRl;
    /**
     * 左侧消息按钮图片
     */
    private ImageView mHeadLeftMsgIv;
    /**
     * 左侧消息按钮上方小红点消息数字
     */
    private TextView mHeadLeftMsgRedTv;

    /**
     * 中间标题根布局
     */
    private LinearLayout mHeadCenterLayout;
    /**
     * 标题文本
     */
    private TextView mHeadCenterTitleTv;

    /**
     * 右侧根布局
     */
    private LinearLayout mHeadRightLayout;
    /**
     * 右侧文本前图片
     */
    private ImageView mHeadRightBeforeIv;
    /**
     * 右侧文本
     */
    private TextView mHeadRightTextTv;
    /**
     * 右侧文本后图片
     */
    private ImageView mHeadRightAfterIv;
    /**
     * 底部分割线
     */
    private View mHeadBottomLine;

    public HeadWidget(Activity activity, int layoutResId) {
        this(activity, layoutResId, null);
    }

    public HeadWidget(Activity activity, int layoutResId, ViewGroup parent) {
        mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        mContentView = (FrameLayout) mInflater.inflate(R.layout.layout_main_container, parent, false);
        initUserView(activity, layoutResId);
        initHeadView();
        initDefaultListener();
    }

    /**
     * 初始化用户布局
     *
     * @param activity    activity上下文
     * @param layoutResID 用户布局id
     */
    private void initUserView(Activity activity, int layoutResID) {
        View userView = mInflater.inflate(layoutResID, mContentView, false);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        params.topMargin = (int) activity.getResources().getDimension(R.dimen.head_height);
        mContentView.addView(userView, params);
    }

    /**
     * 初始化头布局控件
     */
    private void initHeadView() {
        // 根布局
        View headView = mInflater.inflate(R.layout.layout_main_head, mContentView);
        // 左侧
        mHeadLeftLayout = (FrameLayout) headView.findViewById(R.id.head_left_layout);
        mHeadLeftImgIv = (ImageView) headView.findViewById(R.id.head_left_img_iv);
        mHeadLeftTextTv = (TextView) headView.findViewById(R.id.head_left_text_tv);
        //头布局左侧消息按钮根布局(带小红点)
        mHeadLeftMsgRl = (RelativeLayout) headView.findViewById(R.id.head_left_msg_rl);
        mHeadLeftMsgIv = (ImageView) headView.findViewById(R.id.head_left_msg_iv);
        mHeadLeftMsgRedTv = (TextView) headView.findViewById(R.id.head_left_msg_red_tv);
        // 中间
        mHeadCenterLayout = (LinearLayout) headView.findViewById(R.id.head_center_layout);
        mHeadCenterTitleTv = (TextView) headView.findViewById(R.id.head_center_title_tv);
        // 右侧
        mHeadRightLayout = (LinearLayout) headView.findViewById(R.id.head_right_layout);
        mHeadRightBeforeIv = (ImageView) headView.findViewById(R.id.head_right_before_iv);
        mHeadRightTextTv = (TextView) headView.findViewById(R.id.head_right_text_tv);
        mHeadRightAfterIv = (ImageView) headView.findViewById(R.id.head_right_after_iv);
        // 底部分割线
        mHeadBottomLine = headView.findViewById(R.id.head_bottom_line);
    }

    /**
     * 初始化默认监听实现(点击头部左侧"返回"按钮,结束当前界面)
     */
    private void initDefaultListener() {
        // 默认实现点击头部左侧"返回"按钮,结束当前界面
        setLeftImgOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivity != null && !mActivity.isFinishing()) {
                    mActivity.finish();
                }
            }
        });
    }

    // ----------------------- left ----------------------------

    /**
     * 显示左侧根布局
     */
    public HeadWidget showLeftLayout() {
        setViewVisibility(mHeadLeftLayout, View.VISIBLE);
        return this;
    }

    /**
     * 隐藏左侧根布局(INVISIBLE)
     */
    public HeadWidget hideLeftLayout() {
        setViewVisibility(mHeadLeftLayout, View.INVISIBLE);
        return this;
    }

    /**
     * 显示左侧按钮图片
     */
    public HeadWidget showLeftImg() {
        setViewVisibility(mHeadLeftImgIv, View.VISIBLE);
        return this;
    }

    /**
     * 隐藏左侧按钮图片(GONE)
     */
    public HeadWidget hideLeftImg() {
        setViewVisibility(mHeadLeftImgIv, View.GONE);
        return this;
    }

    /**
     * 设置左侧按钮图片
     *
     * @param resId 图片资源id
     */
    public HeadWidget setLeftImg(@DrawableRes int resId) {
        if (mHeadLeftImgIv != null) {
            mHeadLeftImgIv.setImageResource(resId);
        }
        return this;
    }

    /**
     * 显示左侧文本内容
     */
    public HeadWidget showLeftText() {
        setViewVisibility(mHeadLeftTextTv, View.VISIBLE);
        return this;
    }

    /**
     * 隐藏左侧文本内容(GONE)
     */
    public HeadWidget hideLeftText() {
        setViewVisibility(mHeadLeftTextTv, View.GONE);
        return this;
    }

    /**
     * 设置左侧文本内容
     *
     * @param resId 字符串资源id
     */
    public HeadWidget setLeftText(@StringRes int resId) {
        return setLeftText(getResString(resId));
    }

    /**
     * 设置左侧文本内容
     *
     * @param text 文本内容
     */
    public HeadWidget setLeftText(String text) {
        if (text != null) {
            mHeadLeftTextTv.setText(text);
        }
        return this;
    }

    /**
     * 设置左侧文本左侧图片
     *
     * @param bmp bitmap图片对象
     */
    public HeadWidget setLeftTextLeftImg(Bitmap bmp) {
        Drawable drawable = null;
        if (bmp != null) {
            drawable = new BitmapDrawable(null, bmp);
        }
        mHeadLeftTextTv.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        return this;
    }

    /**
     * 设置左侧文本左侧图片
     *
     * @param resId 图片资源id
     */
    public HeadWidget setLeftTextLeftImg(@DrawableRes int resId) {
        mHeadLeftTextTv.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0);
        return this;
    }

    /**
     * 显示左侧消息按钮根布局
     */
    public HeadWidget showLeftMsgRl() {
        setViewVisibility(mHeadLeftMsgRl, View.VISIBLE);
        return this;
    }

    /**
     * 隐藏左侧消息按钮根布局(GONE)
     */
    public HeadWidget hideLeftMsgRl() {
        setViewVisibility(mHeadLeftMsgRl, View.GONE);
        return this;
    }

    /**
     * 显示左侧消息按钮图片
     */
    public HeadWidget showLeftMsgIv() {
        setViewVisibility(mHeadLeftMsgIv, View.VISIBLE);
        return this;
    }

    /**
     * 隐藏左侧消息按钮图片(INVISIBLE)
     */
    public HeadWidget hideLeftMsgIv() {
        setViewVisibility(mHeadLeftMsgIv, View.INVISIBLE);
        return this;
    }

    /**
     * 设置左侧消息按钮图片
     *
     * @param resId 图片资源id
     */
    public HeadWidget setLeftMsgIv(@DrawableRes int resId) {
        if (mHeadLeftMsgIv != null) {
            mHeadLeftMsgIv.setImageResource(resId);
        }
        return this;
    }

    /**
     * 显示左侧消息红色小圆点
     */
    public HeadWidget showLeftRedDot() {
        setViewVisibility(mHeadLeftMsgRedTv, View.VISIBLE);
        return this;
    }

    /**
     * 隐藏左侧消息红色小圆点(INVISIBLE)
     */
    public HeadWidget hideLeftRedDot() {
        setViewVisibility(mHeadLeftMsgRedTv, View.INVISIBLE);
        return this;
    }

    /**
     * 设置左侧小红点上的消息数目
     *
     * @param num 小红点上消息数目
     */
    public HeadWidget setLeftMsgRedNum(int num) {
        if (num > 0) {
            mHeadLeftMsgRedTv.setText(String.valueOf(num));
        }
        return this;
    }

    /**
     * 设置左侧根布局点击监听
     *
     * @param onClickListener 点击监听
     */
    public HeadWidget setLeftLayoutOnClickListener(View.OnClickListener onClickListener) {
        OnClickHelper.setOnClickListener(mHeadLeftLayout, onClickListener);
        return this;
    }

    /**
     * 设置左侧图片点击监听
     *
     * @param onClickListener 点击监听
     */
    public HeadWidget setLeftImgOnClickListener(View.OnClickListener onClickListener) {
        OnClickHelper.setOnClickListener(mHeadLeftImgIv, onClickListener);
        return this;
    }

    /**
     * 设置左侧文本点击监听
     *
     * @param onClickListener 点击监听
     */
    public HeadWidget setLeftTextOnClickListener(View.OnClickListener onClickListener) {
        OnClickHelper.setOnClickListener(mHeadLeftTextTv, onClickListener);
        return this;
    }

    /**
     * 设置左侧消息按钮图片点击监听
     *
     * @param onClickListener 点击监听
     */
    public HeadWidget setLeftMsgImgOnClickListener(View.OnClickListener onClickListener) {
        OnClickHelper.setOnClickListener(mHeadLeftMsgIv, onClickListener);
        return this;
    }

    // ----------------------- center ----------------------------

    /**
     * 设置标题文字
     *
     * @param resId 字符串资源id
     */
    public HeadWidget setTitleText(@StringRes int resId) {
        return setTitleText(getResString(resId));
    }

    /**
     * 设置标题文字
     *
     * @param text 标题文本
     */
    public HeadWidget setTitleText(String text) {
        if (text != null) {
            mHeadCenterTitleTv.setText(text);
        }
        return this;
    }

    /**
     * 设置标题文字周围上下左右图片显示
     *
     * @param left   文本左侧图片资源id
     * @param top    文本上方图片资源id
     * @param right  文本右侧图片资源id
     * @param bottom 文本下方图片资源id
     */
    public HeadWidget setTitleCompoundIcon(@DrawableRes int left, @DrawableRes int top, @DrawableRes int right, @DrawableRes int bottom) {
        mHeadCenterTitleTv.setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
        return this;
    }

    /**
     * 设置标题文字右侧图片显示
     *
     * @param resId 文本右侧图片资源id
     */
    public HeadWidget setTitleRightIcon(int resId) {
        return setTitleCompoundIcon(0, 0, resId, 0);
    }

    /**
     * 设置标题文字点击监听
     *
     * @param onClickListener 事件监听
     */
    public HeadWidget setTitleOnClickListener(View.OnClickListener onClickListener) {
        OnClickHelper.setOnClickListener(mHeadCenterTitleTv, onClickListener);
        return this;
    }

    // ----------------------- right ----------------------------

    /**
     * 设置右侧文本显示
     *
     * @param resId 字符串资源id
     */
    public HeadWidget setRightText(@StringRes int resId) {
        return setRightText(getResString(resId));
    }

    /**
     * 设置右侧文本显示
     *
     * @param text 文本内容字符串
     */
    public HeadWidget setRightText(String text) {
        if (text != null) {
            mHeadRightTextTv.setText(text);
        }
        return this;
    }

    /**
     * 设置右侧文字周围上下左右图片显示
     *
     * @param left   文本左侧图片资源id
     * @param top    文本上方图片资源id
     * @param right  文本右侧图片资源id
     * @param bottom 文本下方图片资源id
     */
    public HeadWidget setRightTextCompoundIcon(@DrawableRes int left, @DrawableRes int top, @DrawableRes int right, @DrawableRes int bottom) {
        if (mHeadRightTextTv != null) {
            mHeadRightTextTv.setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
        }
        return this;
    }

    /**
     * 设置右侧文字右侧图片显示
     *
     * @param resId 文本右侧图片资源id
     */
    public HeadWidget setRightTextRightIcon(@DrawableRes int resId) {
        return setRightTextCompoundIcon(0, 0, resId, 0);
    }

    /**
     * 设置右侧文字前图片
     *
     * @param resId 图片资源id
     */
    public HeadWidget setRightBeforeIcon(@DrawableRes int resId) {
        if (resId > 0) {
            mHeadRightBeforeIv.setImageResource(resId);
            setViewVisibility(mHeadRightBeforeIv, View.VISIBLE);
        }
        return this;
    }

    /**
     * 设置右侧文字后图片
     *
     * @param resId 图片资源id
     */
    public HeadWidget setRightAfterIcon(int resId) {
        if (resId > 0) {
            mHeadRightAfterIv.setImageResource(resId);
            setViewVisibility(mHeadRightAfterIv, View.VISIBLE);
        }
        return this;
    }

    /**
     * 右侧根布局点击事件监听
     *
     * @param onClickListener 点击监听
     */
    public HeadWidget setRightLayoutOnClickListener(View.OnClickListener onClickListener) {
        OnClickHelper.setOnClickListener(mHeadRightLayout, onClickListener);
        return this;
    }

    /**
     * 右侧文字点击事件监听
     *
     * @param onClickListener 点击监听
     */
    public HeadWidget setRightTextOnClickListener(View.OnClickListener onClickListener) {
        OnClickHelper.setOnClickListener(mHeadRightTextTv, onClickListener);
        return this;
    }

    /**
     * 右侧文本前图片点击事件监听
     *
     * @param onClickListener 点击监听
     */
    public HeadWidget setRightBeforeImgOnClickListener(View.OnClickListener onClickListener) {
        OnClickHelper.setOnClickListener(mHeadRightBeforeIv, onClickListener);
        return this;
    }

    /**
     * 右侧文本后图片点击事件监听
     *
     * @param onClickListener 点击监听
     */
    public HeadWidget setRightAfterImgOnClickListener(View.OnClickListener onClickListener) {
        OnClickHelper.setOnClickListener(mHeadRightAfterIv, onClickListener);
        return this;
    }

    /**
     * 显示右侧根布局
     */
    public HeadWidget showRightLayout() {
        setViewVisibility(mHeadRightLayout, View.VISIBLE);
        return this;
    }

    /**
     * 隐藏右侧根布局(INVISIBLE)
     */
    public HeadWidget hideRightLayout() {
        setViewVisibility(mHeadRightLayout, View.INVISIBLE);
        return this;
    }

    /**
     * 显示头布局下方分割线
     */
    public HeadWidget showHeadBottomLine() {
        mHeadBottomLine.setVisibility(View.VISIBLE);
        return this;
    }

    /**
     * 隐藏头布局下方分割线(GONE)
     */
    public HeadWidget hideHeadBottomLine() {
        mHeadBottomLine.setVisibility(View.GONE);
        return this;
    }

    /**
     * 设置view的显示状态
     *
     * @param view       需要被设置的view对象
     * @param visibility (VISIBLE, INVISIBLE, GONE)
     */
    private void setViewVisibility(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    /**
     * 根据字符串资源id获取字符串
     *
     * @param resId 字符串资源id
     */
    private String getResString(@StringRes int resId) {
        if (resId > 0) {
            return mActivity.getString(resId);
        }
        return "";
    }

    //---------------getter-----------------------
    public FrameLayout getContentView() {
        return mContentView;
    }

    public FrameLayout getHeadLeftLayout() {
        return mHeadLeftLayout;
    }

    public ImageView getHeadLeftImgIv() {
        return mHeadLeftImgIv;
    }

    public TextView getHeadLeftTextTv() {
        return mHeadLeftTextTv;
    }

    public RelativeLayout getHeadLeftMsgRl() {
        return mHeadLeftMsgRl;
    }

    public ImageView getHeadLeftMsgIv() {
        return mHeadLeftMsgIv;
    }

    public TextView getHeadLeftMsgRedTv() {
        return mHeadLeftMsgRedTv;
    }

    public LinearLayout getHeadCenterLayout() {
        return mHeadCenterLayout;
    }

    public TextView getHeadCenterTitleTv() {
        return mHeadCenterTitleTv;
    }

    public LinearLayout getHeadRightLayout() {
        return mHeadRightLayout;
    }

    public ImageView getHeadRightBeforeIv() {
        return mHeadRightBeforeIv;
    }

    public TextView getHeadRightTextTv() {
        return mHeadRightTextTv;
    }

    public ImageView getHeadRightAfterIv() {
        return mHeadRightAfterIv;
    }

    public View getHeadBottomLine() {
        return mHeadBottomLine;
    }
}
