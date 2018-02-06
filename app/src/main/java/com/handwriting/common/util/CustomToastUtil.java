package com.handwriting.common.util;

import com.handwriting.common.widget.CustomToast;

/**
 * Desc:Toast工具类,用于显示特定样式的Toast提示信息
 *
 * @author JiLin
 */
public class CustomToastUtil {

    /**
     * Toast对话框,显示成功信息,圆角矩形样式,位于屏幕中间
     *
     * @param msg 消息
     */
    public static void showSuccessToast(String msg) {
        CustomToast.show(CustomToast.SUCCESS, msg);
    }

    /**
     * Toast对话框,显示失败信息,圆角矩形样式,位于屏幕中间
     *
     * @param msg 消息
     */
    public static void showErrorToast(String msg) {
        CustomToast.show(CustomToast.ERROR, msg);
    }

    /**
     * Toast对话框,显示提示信息,圆角矩形样式,位于屏幕中间
     *
     * @param msg 消息
     */
    public static void showInfoToast(String msg) {
        CustomToast.show(CustomToast.INFO, msg);
    }

    /**
     * Toast对话框,圆角矩形样式,位于屏幕底部
     *
     * @param msg 消息
     */
    public static void showToast(String msg) {
        CustomToast.show(msg);
    }
}
