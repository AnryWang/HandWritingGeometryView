package com.hand.writing.listener;

import android.graphics.Paint;

import com.hand.writing.DrawType;
import com.hand.writing.view.HandWritingView;


/**
 * Desc:几何图形监听回调接口
 * Copyright: Copyright (c) 2016
 *
 * @author JiLin
 * @version 1.0
 * @since 2017/9/18 0018
 */
public interface IGeometryListener {
    /**
     * 显示可编辑状态的几何图形
     *
     * @param pathInfo      可编辑几何图形信息
     * @param geometryPaint 几何图形画笔
     * @param drawType      笔迹类型
     */
    void onShowEditGeometry(HandWritingView.PathInfo pathInfo, Paint geometryPaint, DrawType drawType);

    /**
     * 可编辑的几何图形是否有效
     *
     * @param drawType 几何图形类型
     * @param minX     左
     * @param minY     上
     * @param maxX     右
     * @param maxY     下
     * @return 无效时返回true;有效时返回false.
     */
    boolean isGeometryInvalid(DrawType drawType, int minX, int minY, int maxX, int maxY);
}