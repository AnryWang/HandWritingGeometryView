package com.handwriting.common.util;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.handwriting.demo.R;

import butterknife.ButterKnife;


/**
 * Desc:dialog弹窗工具类
 *
 * @author JiLin
 */
public class DialogUtil {
    /**
     * 点击确定POSITIVE
     */
    public static final int CLICK_POSITIVE = 1;
    /**
     * 点击取消NEGATIVE
     */
    public static final int CLICK_NEGATIVE = 2;
    /**
     * 点击中立按钮
     */
    public static final int CLICK_NEUTRAL = 3;
    /**
     * dialog默认占屏幕宽度百分比
     */
    private static float DEFAULT_WIDTH_PERCENT = 0.6f;
    /**
     * dialog默认占屏幕高度百分比
     */
    private static float DEFAULT_HEIGHT_PERCENT = 0.2f;

    /**
     * 有回调,带取消和确定按钮的dialog
     *
     * @param activity activity上下文对象
     * @param message  需要显示的提示内容信息
     * @param callback 点击操作回调接口
     */
    public static void showCancelDialog(Activity activity, String message, final CallBack callback) {
        showCustomBtnNameDialog(activity, message, "取消", "确定", callback);
    }

    /**
     * 有回调,可以自定义2个按钮文字的dialog
     *
     * @param activity   activity上下文对象
     * @param message    需要显示的提示内容信息
     * @param cancelName "取消"按钮自定义名称
     * @param sureName   "确定"按钮自定义名称
     * @param callback   点击操作回调接口
     */
    public static void showCustomBtnNameDialog(Activity activity, String message, String cancelName, String sureName, final CallBack callback) {
        showCustomBtnNameDialog(activity, message, cancelName, sureName, DEFAULT_WIDTH_PERCENT, DEFAULT_HEIGHT_PERCENT, callback);
    }

    /**
     * 有回调,可以自定义2个按钮文字的dialog
     *
     * @param activity      activity上下文对象
     * @param message       需要显示的提示内容信息
     * @param cancelName    "取消"按钮自定义名称
     * @param sureName      "确定"按钮自定义名称
     * @param widthPercent  dialog宽度占屏幕的百分比(范围必须为>0,<=1)
     * @param heightPercent dialog高度占屏幕的百分比(范围必须为>0,<=1)
     * @param callback      点击操作回调接口
     */
    public static void showCustomBtnNameDialog(Activity activity, String message, String cancelName, String sureName, float widthPercent, float heightPercent, final CallBack callback) {
        if (activity == null) {
            return;
        }
        View dialogView = View.inflate(activity, R.layout.dialog_sure_cancle, null);
        final Dialog dialog = new Dialog(activity, R.style.TransparentDialog);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);
        TextView messageTxt = ButterKnife.findById(dialogView, R.id.dialog_message);
        Button cancelBtn = ButterKnife.findById(dialogView, R.id.dialog_cancel_btn);
        Button sureBtn = ButterKnife.findById(dialogView, R.id.dialog_sure_btn);
        if (!TextUtils.isEmpty(message)) {
            messageTxt.setText(message);
        }
        if (!TextUtils.isEmpty(cancelName)) {
            cancelBtn.setText(cancelName);
        }
        if (!TextUtils.isEmpty(sureName)) {
            sureBtn.setText(sureName);
        }

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (callback != null) {
                    callback.back(CLICK_NEGATIVE, null);
                }
            }
        });
        sureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (callback != null) {
                    callback.back(CLICK_POSITIVE, null);
                }
            }
        });
        dialog.onWindowAttributesChanged(setDialogParams(activity, dialog, widthPercent, heightPercent));
        dialog.show();
    }

    /**
     * 无回调，只有确定按钮的dialog
     *
     * @param activity activity上下文对象
     * @param message  需要显示的提示内容信息
     */
    public static void showNoCallBackDialog(Activity activity, String message) {
        showCallBackDialog(activity, message, "确定", null);
    }

    /**
     * 有回调，只有确定按钮的dialog
     *
     * @param activity activity上下文对象
     * @param message  需要显示的提示内容信息
     * @param callback 点击操作回调接口
     * @param sureName "确定"按钮自定义名称
     */
    public static void showCallBackDialog(Activity activity, String message, String sureName, final CallBack callback) {
        showCallBackDialog(activity, message, sureName, DEFAULT_WIDTH_PERCENT, DEFAULT_HEIGHT_PERCENT, callback);
    }

    /**
     * 有回调，只有确定按钮的dialog
     *
     * @param activity      activity上下文对象
     * @param message       需要显示的提示内容信息
     * @param sureName      "确定"按钮自定义名称
     * @param widthPercent  dialog宽度占屏幕的百分比(范围必须为>0,<=1)
     * @param heightPercent dialog高度占屏幕的百分比(范围必须为>0,<=1)
     * @param callback      点击操作回调接口
     */
    public static void showCallBackDialog(Activity activity, String message, String sureName, float widthPercent, float heightPercent, final CallBack callback) {
        if (activity == null) {
            return;
        }
        View dialogView = View.inflate(activity, R.layout.dialog_sure, null);
        final Dialog dialog = new Dialog(activity, R.style.TransparentDialog);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);
        TextView messageTxt = ButterKnife.findById(dialogView, R.id.dialog_message);
        if (!TextUtils.isEmpty(message)) {
            messageTxt.setText(message);
        }
        Button sureBtn = ButterKnife.findById(dialogView, R.id.dialog_sure_btn);
        if (!TextUtils.isEmpty(sureName)) {
            sureBtn.setText(sureName);
        }
        sureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (null != callback) {
                    callback.back(CLICK_POSITIVE, null);
                }
            }
        });

        dialog.onWindowAttributesChanged(setDialogParams(activity, dialog, widthPercent, heightPercent));
        dialog.show();
    }

    /**
     * 显示没有按钮的dialog
     *
     * @param activity activity上下文对象
     * @param message  提示信息
     */
    public static Dialog showNoBtnDialog(Activity activity, String message) {
        return showNoBtnDialog(activity, message, DEFAULT_WIDTH_PERCENT);
    }

    /**
     * 显示没有按钮的dialog
     *
     * @param activity     activity上下文对象
     * @param message      提示信息
     * @param widthPercent dialog宽度占屏幕的百分比(范围必须为>0,<=1)
     */
    public static Dialog showNoBtnDialog(Activity activity, String message, float widthPercent) {
        View view = View.inflate(activity, R.layout.dialog_no_btn, null);
        Dialog dialog = new Dialog(activity, R.style.TransparentDialog);
        dialog.setContentView(view);
        dialog.setCancelable(true); // 设置可以点击"返回"按钮取消当前对话框
        dialog.setCanceledOnTouchOutside(true); // 设置可以点击对话框外边取消当前对话框
        TextView messageTv = ButterKnife.findById(view, R.id.dialog_message);
        if (!TextUtils.isEmpty(message)) {
            messageTv.setText(message);
        }
        //将对话框的大小按屏幕大小的百分比设置
        dialog.onWindowAttributesChanged(setDialogParams(activity, dialog, widthPercent, 0f));
        dialog.show();
        return dialog;
    }

    /**
     * 设置dialog参数
     *
     * @param activity      activity上下文对象
     * @param dialog        dialog对话框对象
     * @param widthPercent  宽度占屏幕的百分比(范围必须为>0,<=1)
     * @param heightPercent 高度占屏幕的百分比(范围必须为>0,<=1)
     * @return 返回dialog参数
     */
    private static WindowManager.LayoutParams setDialogParams(Activity activity, Dialog dialog, float widthPercent, float heightPercent) {
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.CENTER);
        WindowManager m = activity.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        if (widthPercent > 0 && widthPercent <= 1) {
            p.width = (int) (d.getWidth() * widthPercent);
        }
        if (heightPercent > 0 && heightPercent <= 1) {
            p.height = (int) (d.getHeight() * heightPercent);
        }
        return p;
    }

    /**
     * 显示默认圆形转圈进度条
     *
     * @param activity activity上下文对象
     * @param msg      进度条提示信息
     * @return 返回圆形转圈进度条对象
     */
    public static ProgressDialog showSpinnerPd(Activity activity, String msg) {
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(msg);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);// 点击dialog外不消失
        progressDialog.setIndeterminate(false);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
        return progressDialog;
    }

    // ------------------------ ProgressDialog----------------------------------

    /**
     * 显示条形进度条
     *
     * @param activity activity上下文对象
     * @param msg      进度条提示信息
     * @param max      进度条最大值
     * @return 返回条形进度条对象
     */
    public static ProgressDialog showHorizontalPd(Activity activity, String msg, int max) {
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progressDialog.setTitle("标题"); // 不显示标题
        progressDialog.setMessage(msg);
        progressDialog.setMax(max);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);// 点击dialog外不消失
        progressDialog.setIndeterminate(false);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
        return progressDialog;
    }

    /**
     * 关闭进度条
     *
     * @param progressDialog 进度条对象
     */
    public static void closeProgressDialog(ProgressDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * dialog弹窗点击操作回调接口
     */
    public interface CallBack {
        /**
         * @param type CLICK_POSITIVE(确定),CLICK_NEGATIVE(取消),CLICK_NEUTRAL(中立)
         * @param obj
         */
        void back(int type, Object obj);
    }

}
