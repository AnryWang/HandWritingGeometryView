package com.handwriting.common.util;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Desc:点击事件帮助类(当短时间内连续点击时(两次点击间隔小于等于400毫秒时),不做处理)
 * Copyright: Copyright (c) 2016
 *
 * @author JiLin
 * @version 1.0
 */
public class OnClickHelper {

    private static long clickTime = 0;

    public static void setSingleDoListener(SingleDoListener singleDoListener, Object... objects) {
        if (isCanClick()) {
            singleDoListener.doIt(objects);
            updateClickTime();
        }
    }

    public static void setOnClickListener(View view, final View.OnClickListener onClickListener) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCanClick()) {
                    v.setEnabled(false);
                    if (onClickListener != null) {
                        onClickListener.onClick(v);
                    }
                    updateClickTime();
                    v.setEnabled(true);
                }
            }
        });
    }

    public static void setOnItemClickListener(ListView listView, final AdapterView.OnItemClickListener onItemClickListener) {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isCanClick()) {
                    view.setEnabled(false);
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(parent, view, position, id);
                    }
                    updateClickTime();
                    view.setEnabled(true);
                }
            }
        });
    }

    private static boolean isCanClick() {
        return System.currentTimeMillis() - clickTime > 400L;
    }

    private static void updateClickTime() {
        clickTime = System.currentTimeMillis();
    }

    public interface SingleDoListener {
        void doIt(Object... objects);
    }
}
