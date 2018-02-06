package com.handwriting.common.widget;

import android.graphics.Color;
import android.support.annotation.DrawableRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.handwriting.common.base.MyApplication;
import com.handwriting.demo.R;

/**
 * Desc: 自定义Toast
 * Copyright: Copyright (c) 2016
 *
 * @author JiLin
 */
public class CustomToast {

    //成功
    public static final int SUCCESS = 0;
    //错误
    public static final int ERROR = 1;
    //信息
    public static final int INFO = 2;
    private static Toast sCustomToast;
    private static Toast sToast;


    /**
     * 显示特定样式的Toast
     *
     * @param type 需要显示的Toast类型(SUCCESS,ERROR,INFO)
     * @param text 需要显示的文本信息
     */
    public static void show(int type, String text) {
        switch (type) {
            case SUCCESS:
                show(text, R.mipmap.ic_toast_success);
                break;
            case ERROR:
                show(text, R.mipmap.ic_toast_error);
                break;
            case INFO:
                show(text, R.mipmap.ic_toast_info);
                break;
            default:
                show(text, R.mipmap.ic_toast_info);
                break;
        }
    }

    /**
     * 显示指定图片样式的Toast
     *
     * @param text  要显示的文本信息
     * @param resId 指定显示的图片id
     */
    public static void show(String text, @DrawableRes int resId) {
        LayoutInflater inflater = LayoutInflater.from(MyApplication.sMyApplication);
        View layout = inflater.inflate(R.layout.toast_custom, null);
        ImageView image = (ImageView) layout.findViewById(R.id.toast_image);
        image.setImageResource(resId);
        TextView textV = (TextView) layout.findViewById(R.id.toast_text);
        textV.setText(text);

        if (sCustomToast == null) {
            sCustomToast = new Toast(MyApplication.sMyApplication);
            sCustomToast.setDuration(Toast.LENGTH_SHORT);
        }
        sCustomToast.setView(layout);
        sCustomToast.setGravity(Gravity.CENTER, 0, 0);
        sCustomToast.show();
    }

    /**
     * 显示默认圆角矩形样式,位于底部的Toast
     *
     * @param msg 需要显示的文本信息
     */
    public static void show(String msg) {
        View layout = LayoutInflater.from(MyApplication.sMyApplication).inflate(R.layout.toast_default, null, false);
        TextView text = (TextView) layout.findViewById(R.id.toastMessage);
        text.setText(msg);
        text.setBackgroundResource(R.drawable.default_toast);
        text.setTextColor(Color.parseColor("#FFFFFF"));
        if (sToast == null) {
            sToast = new Toast(MyApplication.sMyApplication);
            sToast.setDuration(Toast.LENGTH_SHORT);
        }

        sToast.setView(layout);
        sToast.show();
    }
}
