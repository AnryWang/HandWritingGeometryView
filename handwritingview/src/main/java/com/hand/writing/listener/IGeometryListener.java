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

    /**
     * 处理当前处于几何图形编辑状态时的切换逻辑
     */
    void handleEditableGeometry();

    /**
     * 获取当前可编辑区域最小左边距
     */
    int getLimitLeft();

    /**
     * 获取当前可编辑区域最小上边距
     */
    int getLimitTop();

    /**
     * 获取当前可编辑区域最大右边距
     */
    int getLimitRight();

    /**
     * 获取当前可编辑区域最大下边距
     */
    int getLimitBottom();

    /**
     * 获取可编辑几何图形的真实宽度
     */
    int getGeometryRealWidth();

    /**
     * 获取可编辑几何图形的真实高度
     */
    int getGeometryRealHeight();

    /**
     * 获取拖拽控制点半径大小
     */
    int getDragPointRadius();
}