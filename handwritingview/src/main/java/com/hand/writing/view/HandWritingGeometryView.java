package com.hand.writing.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hand.writing.DrawType;
import com.hand.writing.IHandWritingViewCache;
import com.hand.writing.R;
import com.hand.writing.listener.IGeometryListener;


import java.util.ArrayList;
import java.util.List;

import static com.hand.writing.HandWritingViewHelper.DEBUG;


/**
 * Desc:可以画出可编辑状态的几何图形的手写控件
 * Copyright: Copyright (c) 2016
 *
 * @author JiLin
 * @version 1.0
 * @since 2017/9/18 0018
 */
public class HandWritingGeometryView extends FrameLayout implements IGeometryListener {
    private static final String TAG = "HandWritingGeometryView";
    /**
     * X轴缩放类型
     */
    private static final int SCALE_X = 0;
    /**
     * Y轴缩放类型
     */
    private static final int SCALE_Y = 1;
    /**
     * XY轴缩放类型
     */
    private static final int SCALE_XY = 2;
    /**
     * 当前几何图形缩放类型
     */
    private int mScaleType = -1;
    private HandWritingView.PathInfo mPathInfo; //当前可编辑几何图形路径信息
    private Paint mGeometryPaint; //几何图形画笔
    private Paint mEdgePaint; //边界画笔
    private DrawType mDrawType; //当前正在画的几何类型

    private int mDragPointRadius; //拖拽控制点半径
    private int mDragPointDiameter; //拖拽控制点直径
    private int mImgBtnDiameter; //取消和保存按钮直径
    private int mDragPointDiameter2; //2倍拖拽控制点直径
    private int mLimitLeft; //当前可编辑区域最小左边距
    private int mLimitTop; //当前可编辑区域最小上边距
    private int mLimitRight; //当前可编辑区域最大右边距
    private int mLimitBottom; //当前可编辑区域最大下边距
    private int mDrawViewMaxLeftMargin; //可编辑几何图形的左边距
    private int mDrawViewMaxTopMargin; //可编辑几何图形的上边距
    private int mWidth; //手写控件宽度
    private int mHeight; //手写控件高度

    private HandWritingView mHandWritingView; //核心手写view
    private View mEditGeometryView; //当前可编辑区域根布局
    private LayoutParams mEditParams; //可编辑几何图形布局参数
    private RelativeLayout mGeometryRl; //几何图形根布局
    private Path mGeometryPath; //几何图形path路径
    private List<DragInfo> mDragInfoList; //存储当前可拖拽点信息的集合

    /**
     * 几何图形可拖拽点信息类(用以标识当前拖拽点可左右移动的距离)
     */
    class DragInfo {
        int index;
        int distanceX;
        int distanceY;
    }

    public HandWritingGeometryView(Context context) {
        this(context, null);
    }

    public HandWritingGeometryView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HandWritingGeometryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mHandWritingView = new HandWritingView(context);
        mHandWritingView.setGeometryListener(this);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mHandWritingView, layoutParams);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mWidth = getWidth();
                mHeight = getHeight();
                if (mWidth > 0 && mHeight > 0 && getViewTreeObserver().isAlive()) {
                    initDimens();
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    private void initDimens() {
        mDragPointDiameter = getResources().getDimensionPixelSize(R.dimen.hw_drag_point_diameter);
        mImgBtnDiameter = getResources().getDimensionPixelSize(R.dimen.hw_img_btn_diameter);
        mDragPointRadius = mDragPointDiameter / 2;
        mDragPointDiameter2 = mDragPointDiameter + mDragPointDiameter;
        mLimitLeft = mDragPointRadius;
        mLimitTop = mImgBtnDiameter + mDragPointRadius;
        mLimitRight = mWidth - mDragPointRadius;
        mLimitBottom = mHeight - mDragPointRadius;

        if (DEBUG) {
            Log.i(TAG, "initDimens() >>> mDragPointDiameter:" + mDragPointDiameter +
                    ", mImgBtnDiameter:" + mImgBtnDiameter + ", mLimitRight:" + mLimitRight + ", mLimitBottom:" + mLimitBottom);
        }
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        ViewGroup.LayoutParams layoutParams = mHandWritingView.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new LayoutParams(params.width, params.height);
        } else {
            layoutParams.width = params.width;
            layoutParams.height = params.height;
        }

        if (DEBUG) {
            Log.i(TAG, "setLayoutParams >>> width:" + params.width + ", height:" + params.height);
        }

        mHandWritingView.setLayoutParams(layoutParams);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mHandWritingView.isGeometryType()) { //几何图形
            int x = (int) ev.getX();
            int y = (int) ev.getY();

            if (!mHandWritingView.isRubber() && !mHandWritingView.isGeometryEditable() && (x < mLimitLeft || y < mLimitTop
                    || x > mLimitRight || y > mLimitBottom)) { //非几何图形编辑状态
                if (DEBUG) {
                    Log.i(TAG, "onInterceptTouchEvent() >>> invalid position  x : " + x + ", y : " + y);
                }
                Toast.makeText(getContext(), "无效的几何图形起始点!!!", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    //------------------------------几何图形监听回调方法------------------------start
    @Override
    public void onShowEditGeometry(HandWritingView.PathInfo pathInfo, Paint geometryPaint, DrawType drawType) {
        if (pathInfo == null || pathInfo.pointsList == null || geometryPaint == null || drawType == null) {
            return;
        }

        // 设置虚线边框画笔相关属性
        if (mEdgePaint == null) {
            mEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            mEdgePaint.setStyle(Paint.Style.STROKE);
            mEdgePaint.setColor(Color.parseColor("#32b48c"));
            PathEffect effect = new DashPathEffect(new float[]{5, 15, 5, 15}, 1);
            mEdgePaint.setPathEffect(effect);
        }

        mPathInfo = pathInfo;
        mGeometryPaint = geometryPaint;
        mDrawType = drawType;

        switch (drawType) {
            case TRIANGLE: //三角形
            case TRAPEZIUM: //梯形
                initEditTriangleOrTrapeziumPath();
                break;
            case RECTANGLE: //矩形
                initEditRectanglePath();
                break;
            case OVAL: //椭圆
                initEditOvalPath();
                break;
            case COORDINATE: //坐标系
                initEditCoordinatePath();
                break;
            case NUMBER_AXIS: //数轴
                initEditNumberAxisPath();
                break;
            default:
                return;
        }

        initGeometryView();
    }

    @Override
    public boolean isGeometryInvalid(DrawType drawType, int minX, int minY, int maxX, int maxY) {
        int disX = Math.abs(maxX - minX);
        int disY = Math.abs(maxY - minY);
        boolean isInvalid;
        switch (drawType) {
            case TRIANGLE: //三角形
            case RECTANGLE: //矩形
                isInvalid = disX < mDragPointDiameter || disY < mDragPointDiameter;
                break;
            case TRAPEZIUM: //梯形
                int offsetX = (maxX - minX) / 4;
                isInvalid = Math.abs(maxX - offsetX - (minX + offsetX)) < mDragPointDiameter || disY < mDragPointDiameter;
                break;
            case OVAL: //椭圆
                isInvalid = disX < mDragPointDiameter2 || disY < mDragPointDiameter2;
                break;
            case COORDINATE: //坐标系
                isInvalid = disX < mDragPointDiameter || disY < mDragPointDiameter;
                break;
            case NUMBER_AXIS: //数轴
                isInvalid = disX < mDragPointDiameter;
                break;
            default:
                isInvalid = true;
                break;
        }
        return isInvalid;
    }
    //------------------------------几何图形监听回调方法------------------------end

    private void initGeometryView() {
        // 只有设置了这个属性,才会调用onDraw()方法
        setWillNotDraw(false);
        // 防止重复添加时会有布局残留
        if (mEditGeometryView != null) {
            removeView(mEditGeometryView);
        }

        mEditGeometryView = View.inflate(getContext(), R.layout.layout_floating_shape_view, null);
        ImageView cancelIv = (ImageView) mEditGeometryView.findViewById(R.id.cancel_iv);
        ImageView saveIv = (ImageView) mEditGeometryView.findViewById(R.id.save_iv);
        mGeometryRl = (RelativeLayout) mEditGeometryView.findViewById(R.id.geometry_rl);

        reviseParams();
        addView(mEditGeometryView, mEditParams);

        // 初始化添加几何图形可拖拽控制点
        initGeometryDragPointView();
        // 取消几何图形点击事件
        cancelIv.setOnClickListener(getCancelClickListener());
        // 保存几何图形点击事件
        saveIv.setOnClickListener(getSaveClickListener());
        // 几何图形view触摸事件
        mEditGeometryView.setOnTouchListener(getGeometryTouchListener());
    }

    /**
     * 初始化添加几何图形可拖拽控制点
     */
    private void initGeometryDragPointView() {
        mDragInfoList = new ArrayList<>();
        for (HandWritingView.PointInfo pointInfo : mPathInfo.pointsList) {
            if (pointInfo.index < 0) {
                continue;
            }

            DragInfo dragInfo = new DragInfo();
            dragInfo.index = pointInfo.index;
            mDragInfoList.add(dragInfo);

            ImageView dragPointView = (ImageView) View.inflate(getContext(), R.layout.item_drag_point_view, null);
            dragPointView.setTag(pointInfo.index);
            dragPointView.setOnTouchListener(getDragOnTouchListener());
            mGeometryRl.addView(dragPointView, initOrUpdateDragViewParams(null, pointInfo));
        }
    }

    private RelativeLayout.LayoutParams initOrUpdateDragViewParams(RelativeLayout.LayoutParams params,
                                                                   @NonNull HandWritingView.PointInfo pointInfo) {
        if (params == null) {
            params = new RelativeLayout.LayoutParams(mDragPointDiameter, mDragPointDiameter);
        }

        if (mDrawType == DrawType.OVAL) {
            switch (pointInfo.index) {
                case 0:
                    params.setMargins(mDrawViewMaxLeftMargin / 2, 0, 0, 0);
                    break;
                case 1:
                    params.setMargins(mDrawViewMaxLeftMargin, mDrawViewMaxTopMargin / 2, 0, 0);
                    break;
                case 2:
                    params.setMargins(mDrawViewMaxLeftMargin / 2, mDrawViewMaxTopMargin, 0, 0);
                    break;
                case 3:
                    params.setMargins(0, mDrawViewMaxTopMargin / 2, 0, 0);
                    break;
                default:
                    break;
            }
        } else {
            params.setMargins(pointInfo.x - mPathInfo.left, pointInfo.y - mPathInfo.top, 0, 0);
        }

        return params;
    }

    /**
     * 初始化可编辑状态的三角形或梯形path信息
     */
    private void initEditTriangleOrTrapeziumPath() {
        initGeometryPath();
        List<HandWritingView.PointInfo> pointsList = mPathInfo.pointsList;
        for (int i = 0, pointsListSize = pointsList.size(); i < pointsListSize; i++) {
            HandWritingView.PointInfo pointInfo = pointsList.get(i);
            if (i == 0) {
                mGeometryPath.moveTo(pointInfo.x, pointInfo.y);
            } else {
                mGeometryPath.lineTo(pointInfo.x, pointInfo.y);
            }
        }
        mGeometryPath.close();
    }

    /**
     * 初始化可编辑状态的矩形path信息
     */
    private void initEditRectanglePath() {
        initGeometryPath();
        RectF rectF = new RectF(mPathInfo.left, mPathInfo.top, mPathInfo.right, mPathInfo.bottom);
        mGeometryPath.addRect(rectF, Path.Direction.CW); //使用path画出矩形
    }

    /**
     * 初始化可编辑状态的椭圆path信息
     */
    private void initEditOvalPath() {
        initGeometryPath();
        RectF rectF = new RectF(mPathInfo.left, mPathInfo.top, mPathInfo.right, mPathInfo.bottom);
        mGeometryPath.addOval(rectF, Path.Direction.CW); //使用path画出椭圆
    }

    /**
     * 初始化可编辑状态的坐标系path信息
     */
    private void initEditCoordinatePath() {
        initGeometryPath();

        List<HandWritingView.PointInfo> pointsList = mPathInfo.pointsList;
        if (pointsList.size() != 5) {
            return;
        }

        HandWritingView.PointInfo originInfo = pointsList.get(4);
        mHandWritingView.obtainCoordinatePath(mGeometryPath, mPathInfo.left, mPathInfo.top,
                mPathInfo.right, mPathInfo.bottom, originInfo.x, originInfo.y,
                HandWritingView.AXIS_ARROW_HEIGHT, HandWritingView.AXIS_OTHER_LENGTH);
    }

    /**
     * 初始化可编辑状态的数轴path信息
     */
    private void initEditNumberAxisPath() {
        initGeometryPath();

        List<HandWritingView.PointInfo> pointsList = mPathInfo.pointsList;
        if (pointsList.size() != 3) {
            return;
        }

        HandWritingView.PointInfo maxInfo = pointsList.get(0);
        HandWritingView.PointInfo minInfo = pointsList.get(1);
        HandWritingView.PointInfo originInfo = pointsList.get(2);

        mHandWritingView.obtainNumberAxisPath(mGeometryPath, minInfo.x, maxInfo.x, originInfo.x, originInfo.y,
                HandWritingView.AXIS_ARROW_HEIGHT, HandWritingView.AXIS_OTHER_LENGTH);
    }

    /**
     * 初始化几何图形path
     */
    private void initGeometryPath() {
        if (mGeometryPath == null) {
            mGeometryPath = new Path();
        } else {
            mGeometryPath.reset();
        }
    }

    /**
     * 修正布局参数
     */
    private void reviseParams() {
        mDrawViewMaxLeftMargin = mPathInfo.right - mPathInfo.left;
        mDrawViewMaxTopMargin = mPathInfo.bottom - mPathInfo.top;
        int geometryEditWidth = mDrawViewMaxLeftMargin + mDragPointDiameter;
        int geometryEditHeight = mDrawViewMaxTopMargin + mDragPointDiameter;
        int height = geometryEditHeight + mImgBtnDiameter;
        int left = mPathInfo.left - mDragPointRadius;
        int top = mPathInfo.top - mDragPointRadius - mImgBtnDiameter;
        int right = mPathInfo.right + mDragPointRadius;
        int bottom = mPathInfo.bottom + mDragPointRadius;

        if (DEBUG) {
            Log.i(TAG, "reviseParams() >>> width:" + geometryEditWidth + ", height:" + height +
                    ", left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom);
        }

        if (mEditParams == null) {
            mEditParams = new LayoutParams(geometryEditWidth, height);
        } else {
            mEditParams.width = geometryEditWidth;
            mEditParams.height = height;
        }
        mEditParams.setMargins(left, top, right, bottom);
    }

    //------------------------------事件监听方法-------------------------------start
    @NonNull
    private OnClickListener getCancelClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) {
                    Log.i(TAG, "取消几何图形!!!");
                }

                if (mHandWritingView != null) {
                    mHandWritingView.onCancelEditView();
                }
                // 重置状态
                reset();
            }
        };
    }

    @NonNull
    private OnClickListener getSaveClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) {
                    Log.i(TAG, "保存几何图形!!!");
                }

                if (mHandWritingView != null) {
                    mHandWritingView.onSaveEditView(mDrawType);
                }
                // 重置状态
                reset();
            }
        };
    }

    private void reset() {
        // 移除可编辑view
        if (mEditGeometryView != null) {
            removeView(mEditGeometryView);
            mEditGeometryView = null;
        }
        // 取消onDraw()方法中画出的几何图形
        setWillNotDraw(true);
        mScaleType = -1;
        mDrawType = null;
        mPathInfo = null;
        mGeometryPath = null;
        mDragInfoList = null;
    }

    @NonNull
    private OnTouchListener getGeometryTouchListener() {
        return new OnTouchListener() {
            // 按下时坐标
            int downX;
            int downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: // 按下
                        downX = (int) event.getX();
                        downY = (int) event.getY();
                        if (DEBUG) {
                            Log.i(TAG, "按下时坐标x:" + downX + ",y:" + downY);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE: // 移动
                        int moveX = (int) event.getX();
                        int moveY = (int) event.getY();
                        int distanceX = moveX - downX;
                        int distanceY = moveY - downY;
                        updateGeometryViewParams(distanceX, distanceY);
                        break;
                    case MotionEvent.ACTION_UP: // 抬起
                        break;
                }
                return true;
            }
        };
    }

    /**
     * 更新编辑状态下的几何图形位置
     *
     * @param distanceX 水平方向位移距离
     * @param distanceY 垂直方向位移距离
     */
    private void updateGeometryViewParams(int distanceX, int distanceY) {
        if (DEBUG) {
            Log.i(TAG, "updateGeometryViewParams() >>> distanceX_" + distanceX + ", distanceY_" + distanceY);
        }

        if (distanceX == 0 && distanceY == 0) {
            return;
        }

        int tempLeft = mPathInfo.left;
        int tempTop = mPathInfo.top;
        int tempRight = mPathInfo.right;
        int tempBottom = mPathInfo.bottom;
        tempLeft += distanceX;
        if (tempLeft < mLimitLeft) {
            return;
        }

        tempTop += distanceY;
        if (tempTop < mLimitTop) {
            return;
        }

        tempRight += distanceX;
        if (tempRight > mLimitRight) {
            return;
        }

        tempBottom += distanceY;
        if (tempBottom > mLimitBottom) {
            return;
        }

        for (HandWritingView.PointInfo pointInfo : mPathInfo.pointsList) {
            int tempX = pointInfo.x;
            int tempY = pointInfo.y;
            tempX += distanceX;
            if (tempX < mLimitLeft || tempX > mLimitRight) {
                break;
            }
            tempY += distanceY;
            if (tempY < mLimitTop || tempY > mLimitBottom) {
                break;
            }
            pointInfo.x = tempX;
            pointInfo.y = tempY;
        }

        mPathInfo.left = tempLeft;
        mPathInfo.top = tempTop;
        mPathInfo.right = tempRight;
        mPathInfo.bottom = tempBottom;

        switch (mDrawType) {
            case TRIANGLE: //三角形
            case TRAPEZIUM: //梯形
                initEditTriangleOrTrapeziumPath(); //更新三角形path信息
                break;
            case RECTANGLE: //矩形
                initEditRectanglePath(); //更新矩形path信息
                break;
            case OVAL: //椭圆
                initEditOvalPath(); //更新椭圆path信息
                break;
            case COORDINATE: //坐标系
                initEditCoordinatePath(); //更新坐标系path信息
                break;
            case NUMBER_AXIS: //数轴
                initEditNumberAxisPath(); //更新数轴path信息
                break;
            default:
                break;
        }

//        mEditGeometryView.setX(tempLeft - mLimitLeft);
//        mEditGeometryView.setY(tempTop - mLimitTop);

        // 改为用如下方式修改布局参数;如果采用setX()、setY()这种方式,会导致当拖动几何图形时的坐标系统值混乱
        reviseParams();
        mEditGeometryView.setLayoutParams(mEditParams);
        // 更新可编辑几何图形所在矩形范围
        if (mHandWritingView != null) {
            mHandWritingView.updateRange(tempLeft, tempTop, tempRight, tempBottom);
        }

        invalidate(tempLeft, tempTop, tempRight, tempBottom);
    }

    @NonNull
    private OnTouchListener getDragOnTouchListener() {
        return new OnTouchListener() {
            private HandWritingView.PointInfo mPointInfo;
            int downX;
            int downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        int index = (int) v.getTag();
                        if (DEBUG) {
                            Log.i(TAG, "getDragOnTouchListener() index >>> " + index);
                        }
                        downX = (int) event.getX();
                        downY = (int) event.getY();
                        mPointInfo = obtainDragPointInfo(index); //得到需要更新坐标位置的信息点
                        if (mPointInfo == null) {
                            return false;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int moveX = (int) event.getX();
                        int moveY = (int) event.getY();
                        int distanceX = moveX - downX;
                        int distanceY = moveY - downY;
                        updateDragView(mPointInfo, distanceX, distanceY);
                        break;
                    case MotionEvent.ACTION_UP:
                        downX = 0;
                        downY = 0;
                        mPointInfo = null;
                        break;
                }
                return true;
            }
        };
    }

    /**
     * 更新拖拽点View信息
     */
    private void updateDragView(HandWritingView.PointInfo pointInfo, int distanceX, int distanceY) {
        if (DEBUG) {
            Log.i(TAG, "updateDragView() >>> distanceX >> " + distanceX + "; distanceY >> " + distanceY);
        }
        if (pointInfo == null || (distanceX == 0 && distanceY == 0)) {
            return;
        }
        if (distanceX != 0 && distanceY == 0) {
            mScaleType = SCALE_X;
        }
        if (distanceY != 0 && distanceX == 0) {
            mScaleType = SCALE_Y;
        }
        if (distanceX != 0 && distanceY != 0) {
            mScaleType = SCALE_XY;
        }

        switch (mDrawType) {
            case TRIANGLE: //三角形
                updateTriangleDragView(pointInfo, distanceX, distanceY);
                break;
            case RECTANGLE: //矩形
                updateRectangleDragView(pointInfo, distanceX, distanceY);
                break;
            case TRAPEZIUM: //梯形
                updateTrapeziumDragView(pointInfo, distanceX, distanceY);
                break;
            case OVAL: //椭圆
                updateOvalDragView(pointInfo, distanceX, distanceY);
                break;
            case COORDINATE: //坐标系
                updateCoordinateDragView(pointInfo, distanceX, distanceY);
                break;
            case NUMBER_AXIS: //数轴
                updateNumberAxisDragView(pointInfo, distanceX);
                break;
        }

        invalidateRec();
    }

    /**
     * 三角形拖拽规则:可在手写区域任意拖动
     */
    private void updateTriangleDragView(HandWritingView.PointInfo pointInfo,
                                        int distanceX, int distanceY) {
        // 更新三角形拖拽点坐标信息
        if (updateDragPointInfo(pointInfo, distanceX, distanceY)) {
            // 更新三角形所在的矩形范围信息
            updatePathRangeInfo();
            // 更新几何图形布局参数信息
            reviseParams();
            mEditGeometryView.setLayoutParams(mEditParams);

            // 更新三角形path信息
            initEditTriangleOrTrapeziumPath();
            //更新拖拽控制点布局参数信息
            updateDragViewParams();
        }
    }

    /**
     * 矩形拖拽规则:矩形拖拽时只有对角线的点不更新
     */
    private void updateRectangleDragView(HandWritingView.PointInfo pointInfo,
                                         int distanceX, int distanceY) {
        boolean canNotMove = isDragViewCanNotMove(pointInfo, distanceX, distanceY, mDragPointDiameter);
        if (canNotMove) {
            return;
        }

        // 根据拖拽类型获取需要更新信息的坐标点索引集合
        List<DragInfo> dragInfoList = obtainRecDragInfoList(mScaleType, pointInfo.index, distanceX, distanceY);
        if (updateDragPointInfo(pointInfo, distanceX, distanceY)) {
            // 更新其它需要更新的坐标点信息
            for (DragInfo dragInfo : dragInfoList) {
                if (dragInfo.index == pointInfo.index) {
                    continue;
                }
                HandWritingView.PointInfo info = mPathInfo.pointsList.get(dragInfo.index);
                updateDragPointInfo(info, dragInfo.distanceX, dragInfo.distanceY);
            }

            // 更新矩形范围信息
            updatePathRangeInfo();
            // 更新几何图形布局参数信息
            reviseParams();
            mEditGeometryView.setLayoutParams(mEditParams);
            //更新矩形path信息
            initEditRectanglePath();

            //更新拖拽控制点布局参数信息
            updateDragViewParams();
        }
    }

    /**
     * 梯形拖拽规则:同一时间内只允许改变拖拽点和与拖拽点y坐标相同的临近点;拖拽点不允许从一侧拖拽到另一侧
     */
    private void updateTrapeziumDragView(HandWritingView.PointInfo pointInfo,
                                         int distanceX, int distanceY) {
        boolean canNotMove = isDragViewCanNotMove(pointInfo, distanceX, distanceY, mDragPointDiameter);
        if (canNotMove) {
            return;
        }

        List<DragInfo> dragInfoList = obtainTrapDragInfoList(mScaleType, pointInfo.index, distanceX, distanceY);
        if (updateDragPointInfo(pointInfo, distanceX, distanceY)) {
            // 更新其它需要更新的坐标点信息
            for (DragInfo dragInfo : dragInfoList) {
                if (dragInfo.index == pointInfo.index) {
                    continue;
                }
                HandWritingView.PointInfo info = mPathInfo.pointsList.get(dragInfo.index);
                updateDragPointInfo(info, dragInfo.distanceX, dragInfo.distanceY);
            }

            // 更新梯形矩形范围信息
            updatePathRangeInfo();
            // 更新几何图形布局参数信息
            reviseParams();
            mEditGeometryView.setLayoutParams(mEditParams);
            //更新梯形path信息
            initEditTriangleOrTrapeziumPath();

            //更新拖拽控制点布局参数信息
            updateDragViewParams();
        }
    }

    /**
     * 椭圆拖拽规则:同一时间内,只允许改变x轴或者y轴,不允许同时改变;拖拽点不允许从一侧拖拽到另一侧
     */
    private void updateOvalDragView(HandWritingView.PointInfo pointInfo,
                                    int distanceX, int distanceY) {
        int index = pointInfo.index;
        if (index == 0 || index == 2) {
            distanceX = 0;
            if (distanceY == 0) {
                return;
            }
        }
        if (index == 1 || index == 3) {
            distanceY = 0;
            if (distanceX == 0) {
                return;
            }
        }

        boolean canNotMove = isDragViewCanNotMove(pointInfo, distanceX, distanceY, mDragPointDiameter2);
        // 更新椭圆拖拽点坐标信息(椭圆比较特殊,拖拽点跟实际坐标点不在一起)
        if (!canNotMove && updateDragPointInfo(pointInfo, distanceX, distanceY)) {
            // 得到另一个需要更新坐标点信息的索引
            int tempIndex = index + 1;
            if (index == mPathInfo.pointsList.size() - 1) {
                tempIndex = 0;
            }
            HandWritingView.PointInfo otherPoint = mPathInfo.pointsList.get(tempIndex);
            updateDragPointInfo(otherPoint, distanceX, distanceY);

            // 更新椭圆矩形范围信息
            updatePathRangeInfo();
            // 更新几何图形布局参数信息
            reviseParams();
            mEditGeometryView.setLayoutParams(mEditParams);

            //更新椭圆path信息
            initEditOvalPath();

            //更新拖拽控制点布局参数信息
            updateDragViewParams();
        }
    }

    /**
     * 坐标系拖拽规则:同一时间内,只允许改变x轴或者y轴的其中一端,并且原点坐标不跟随改变
     */
    private void updateCoordinateDragView(HandWritingView.PointInfo pointInfo,
                                          int distanceX, int distanceY) {
        switch (pointInfo.index) {
            case 0:
            case 2:
                distanceX = 0;
                break;
            case 1:
            case 3:
                distanceY = 0;
                break;
        }
        boolean canNotMove = isDragViewCanNotMove(pointInfo, distanceX, distanceY, mDragPointRadius);
        if (!canNotMove && updateDragPointInfo(pointInfo, distanceX, distanceY)) {
            switch (pointInfo.index) {
                case 0:
                    mPathInfo.top = pointInfo.y;
                    break;
                case 1:
                    mPathInfo.right = pointInfo.x;
                    break;
                case 2:
                    mPathInfo.bottom = pointInfo.y;
                    break;
                case 3:
                    mPathInfo.left = pointInfo.x;
                    break;
            }

            // 更新可编辑几何图形所在矩形范围
            if (mHandWritingView != null) {
                mHandWritingView.updateRange(mPathInfo.left, mPathInfo.top,
                        mPathInfo.right, mPathInfo.bottom);
            }

            // 更新几何图形布局参数信息
            reviseParams();
            mEditGeometryView.setLayoutParams(mEditParams);

            //更新坐标系path信息
            initEditCoordinatePath();

            //更新拖拽控制点布局参数信息
            updateDragViewParams();
        }
    }

    /**
     * 数轴拖拽规则:同一时间内,只允许改变x轴的其中一端,并且原点坐标不跟随改变
     */
    private void updateNumberAxisDragView(HandWritingView.PointInfo pointInfo, int distanceX) {
        boolean canNotMove = isDragViewCanNotMove(pointInfo, distanceX, 0, mDragPointRadius);
        if (!canNotMove && updateDragPointInfo(pointInfo, distanceX, 0)) {
            // 数轴只用更新所在矩形范围的左右信息
            if (pointInfo.index == 1) {
                mPathInfo.right = pointInfo.x;
            } else if (pointInfo.index == 3) {
                mPathInfo.left = pointInfo.x;
            }

            // 更新可编辑几何图形所在矩形范围
            if (mHandWritingView != null) {
                mHandWritingView.updateRange(mPathInfo.left, mPathInfo.top,
                        mPathInfo.right, mPathInfo.bottom);
            }

            // 更新几何图形布局参数信息
            reviseParams();
            mEditGeometryView.setLayoutParams(mEditParams);

            //更新数轴path信息
            initEditNumberAxisPath();

            //更新拖拽控制点布局参数信息
            updateDragViewParams();
        }
    }

    /**
     * 更新拖拽点布局参数信息
     */
    private void updateDragViewParams() {
        for (HandWritingView.PointInfo info : mPathInfo.pointsList) {
            View viewWithTag = mGeometryRl.findViewWithTag(info.index);
            if (viewWithTag == null) {
                continue;
            }
            RelativeLayout.LayoutParams params = initOrUpdateDragViewParams((RelativeLayout.LayoutParams)
                    viewWithTag.getLayoutParams(), info);
            viewWithTag.setLayoutParams(params);
        }
    }

    /**
     * 更新pathInfo左上右下矩形范围
     */
    private void updatePathRangeInfo() {
        if (mPathInfo == null || mPathInfo.pointsList == null) {
            return;
        }

        int minX = -1;
        int minY = -1;
        int maxX = -1;
        int maxY = -1;
        for (HandWritingView.PointInfo pointInfo : mPathInfo.pointsList) {
            if (minX != -1) {
                minX = Math.min(minX, pointInfo.x);
            } else {
                minX = pointInfo.x;
            }
            if (minY != -1) {
                minY = Math.min(minY, pointInfo.y);
            } else {
                minY = pointInfo.y;
            }
            if (maxX != -1) {
                maxX = Math.max(maxX, pointInfo.x);
            } else {
                maxX = pointInfo.x;
            }
            if (maxY != -1) {
                maxY = Math.max(maxY, pointInfo.y);
            } else {
                maxY = pointInfo.y;
            }
        }

        mPathInfo.left = minX;
        mPathInfo.top = minY;
        mPathInfo.right = maxX;
        mPathInfo.bottom = maxY;

        if (DEBUG) {
            Log.i(TAG, "updatePathRangeInfo() >>> left:" + minX + ", top:" + minY +
                    ", right:" + maxX + ", bottom:" + maxY);
        }

        // 更新可编辑几何图形所在矩形范围
        if (mHandWritingView != null) {
            mHandWritingView.updateRange(minX, minY, maxX, maxY);
        }
    }

    /**
     * 更新拖拽点参数信息.更新失败返回false;更新成功返回true.
     */
    private boolean updateDragPointInfo(HandWritingView.PointInfo pointInfo,
                                        int distanceX, int distanceY) {
        if ((distanceX == 0 && distanceY == 0) || pointInfo == null) {
            return false;
        }


        int tempX = pointInfo.x;
        tempX += distanceX;
        int tempY = pointInfo.y;
        tempY += distanceY;

        if (DEBUG) {
            Log.i(TAG, "updateDragPointInfo() >>> tempX : " + tempX + ",tempY : " + tempY);
        }

        if (tempX < mLimitLeft || tempY < mLimitTop ||
                tempX > mLimitRight || tempY > mLimitBottom) {
            return false;
        }

        pointInfo.x = tempX;
        pointInfo.y = tempY;

        return true;
    }
    //------------------------------事件监听方法-----------------------------end

    /**
     * 根据拖拽类型获取矩形需要更新信息的坐标点信息集合
     */
    private List<DragInfo> obtainRecDragInfoList(int scaleType, int index, int distanceX, int distanceY) {
        DragInfo dragInfo0 = mDragInfoList.get(0);
        DragInfo dragInfo1 = mDragInfoList.get(1);
        DragInfo dragInfo2 = mDragInfoList.get(2);
        DragInfo dragInfo3 = mDragInfoList.get(3);
        switch (scaleType) {
            case SCALE_X:
                dragInfo0.distanceY = dragInfo1.distanceY =
                        dragInfo2.distanceY = dragInfo3.distanceY = 0;
                if (index == 0 || index == 3) {
                    dragInfo0.distanceX = dragInfo3.distanceX = distanceX;
                    dragInfo1.distanceX = dragInfo2.distanceX = 0;
                } else {
                    dragInfo1.distanceX = dragInfo2.distanceX = distanceX;
                    dragInfo0.distanceX = dragInfo3.distanceX = 0;
                }
                break;
            case SCALE_Y:
                dragInfo0.distanceX = dragInfo1.distanceX =
                        dragInfo2.distanceX = dragInfo3.distanceX = 0;
                if (index == 0 || index == 1) {
                    dragInfo0.distanceY = dragInfo1.distanceY = distanceY;
                    dragInfo2.distanceY = dragInfo3.distanceY = 0;
                } else {
                    dragInfo2.distanceY = dragInfo3.distanceY = distanceY;
                    dragInfo0.distanceY = dragInfo1.distanceY = 0;
                }
                break;
            case SCALE_XY:
                switch (index) {
                    case 0:
                        updateDragInfo(dragInfo0, distanceX, distanceY);
                        updateDragInfo(dragInfo1, 0, distanceY);
                        updateDragInfo(dragInfo3, distanceX, 0);
                        updateDragInfo(dragInfo2, 0, 0);
                        break;
                    case 1:
                        updateDragInfo(dragInfo1, distanceX, distanceY);
                        updateDragInfo(dragInfo0, 0, distanceY);
                        updateDragInfo(dragInfo2, distanceX, 0);
                        updateDragInfo(dragInfo3, 0, 0);
                        break;
                    case 2:
                        updateDragInfo(dragInfo2, distanceX, distanceY);
                        updateDragInfo(dragInfo3, 0, distanceY);
                        updateDragInfo(dragInfo1, distanceX, 0);
                        updateDragInfo(dragInfo0, 0, 0);
                        break;
                    case 3:
                        updateDragInfo(dragInfo3, distanceX, distanceY);
                        updateDragInfo(dragInfo2, 0, distanceY);
                        updateDragInfo(dragInfo0, distanceX, 0);
                        updateDragInfo(dragInfo1, 0, 0);
                        break;
                }
                break;
            default:
                break;
        }

        return mDragInfoList;
    }

    /**
     * 根据拖拽类型获取梯形需要更新信息的坐标点信息集合
     */
    private List<DragInfo> obtainTrapDragInfoList(int scaleType, int index, int distanceX, int distanceY) {
        DragInfo dragInfo0 = mDragInfoList.get(0);
        DragInfo dragInfo1 = mDragInfoList.get(1);
        DragInfo dragInfo2 = mDragInfoList.get(2);
        DragInfo dragInfo3 = mDragInfoList.get(3);

        switch (scaleType) {
            case SCALE_X:
                dragInfo0.distanceY = dragInfo1.distanceY =
                        dragInfo2.distanceY = dragInfo3.distanceY = 0;
                switch (index) {
                    case 0:
                        dragInfo0.distanceX = distanceX;
                        dragInfo1.distanceX = dragInfo2.distanceX = dragInfo3.distanceX = 0;
                        break;
                    case 1:
                        dragInfo1.distanceX = distanceX;
                        dragInfo0.distanceX = dragInfo2.distanceX = dragInfo3.distanceX = 0;
                        break;
                    case 2:
                        dragInfo2.distanceX = distanceX;
                        dragInfo0.distanceX = dragInfo1.distanceX = dragInfo3.distanceX = 0;
                        break;
                    case 3:
                        dragInfo3.distanceX = distanceX;
                        dragInfo0.distanceX = dragInfo1.distanceX = dragInfo2.distanceX = 0;
                        break;
                }
                break;
            case SCALE_Y:
                dragInfo0.distanceX = dragInfo1.distanceX =
                        dragInfo2.distanceX = dragInfo3.distanceX = 0;
                if (index == 0 || index == 1) {
                    dragInfo0.distanceY = dragInfo1.distanceY = distanceY;
                    dragInfo2.distanceY = dragInfo3.distanceY = 0;
                } else {
                    dragInfo2.distanceY = dragInfo3.distanceY = distanceY;
                    dragInfo0.distanceY = dragInfo1.distanceY = 0;
                }
                break;
            case SCALE_XY:
                switch (index) {
                    case 0:
                        updateDragInfo(dragInfo0, distanceX, distanceY);
                        updateDragInfo(dragInfo1, 0, distanceY);
                        updateDragInfo(dragInfo3, 0, 0);
                        updateDragInfo(dragInfo2, 0, 0);
                        break;
                    case 1:
                        updateDragInfo(dragInfo1, distanceX, distanceY);
                        updateDragInfo(dragInfo0, 0, distanceY);
                        updateDragInfo(dragInfo2, 0, 0);
                        updateDragInfo(dragInfo3, 0, 0);
                        break;
                    case 2:
                        updateDragInfo(dragInfo2, distanceX, distanceY);
                        updateDragInfo(dragInfo3, 0, distanceY);
                        updateDragInfo(dragInfo1, 0, 0);
                        updateDragInfo(dragInfo0, 0, 0);
                        break;
                    case 3:
                        updateDragInfo(dragInfo3, distanceX, distanceY);
                        updateDragInfo(dragInfo2, 0, distanceY);
                        updateDragInfo(dragInfo0, 0, 0);
                        updateDragInfo(dragInfo1, 0, 0);
                        break;
                }
                break;
            default:
                break;
        }

        return mDragInfoList;
    }

    private void updateDragInfo(@NonNull DragInfo dragInfo, int distanceX, int distanceY) {
        dragInfo.distanceX = distanceX;
        dragInfo.distanceY = distanceY;
    }

    /**
     * 获取需要更新坐标位置的信息点
     *
     * @param index 拖拽点的索引位置
     */
    private HandWritingView.PointInfo obtainDragPointInfo(int index) {
        if (mPathInfo == null || mPathInfo.pointsList == null || mPathInfo.pointsList.size() == 0) {
            return null;
        }

        for (HandWritingView.PointInfo pointInfo : mPathInfo.pointsList) {
            if (pointInfo.index == index) {
                return pointInfo;
            }
        }

        return null;
    }

    /**
     * 如果当前拖拽点不可移动到新的位置时返回true;可移动时返回false.
     *
     * @param pointInfo 当前拖拽点信息
     * @param distanceX X轴移动距离
     * @param distanceY Y轴移动距离
     * @param tempLimit 拖拽点间最小距离限制
     */
    private boolean isDragViewCanNotMove(@NonNull HandWritingView.PointInfo pointInfo,
                                         int distanceX, int distanceY, int tempLimit) {
        int index = pointInfo.index;
        int size = mPathInfo.pointsList.size();
        int nonMoveIndex;

        // 数轴和坐标系需要特殊判断
        boolean isAxis = mDrawType == DrawType.NUMBER_AXIS || mDrawType == DrawType.COORDINATE;
        if (isAxis) {
            // 得到数轴和坐标系中不需要移动的坐标点索引
            nonMoveIndex = size - 1;
        } else {
            nonMoveIndex = index + 2;
            if (nonMoveIndex >= size) {
                nonMoveIndex -= size;
            }
        }

        int tempX = pointInfo.x + distanceX;
        int tempY = pointInfo.y + distanceY;

        HandWritingView.PointInfo nonMovePoint = mPathInfo.pointsList.get(nonMoveIndex);
        int nonMoveX;
        // 如果是梯形,nonMoveX需要取其相邻的拖拽点x坐标信息
        if (mDrawType == DrawType.TRAPEZIUM) {
            switch (index) {
                case 0:
                    nonMoveX = mPathInfo.pointsList.get(1).x;
                    break;
                case 1:
                    nonMoveX = mPathInfo.pointsList.get(0).x;
                    break;
                case 2:
                    nonMoveX = mPathInfo.pointsList.get(3).x;
                    break;
                case 3:
                    nonMoveX = mPathInfo.pointsList.get(2).x;
                    break;
                default:
                    nonMoveX = nonMovePoint.x;
                    break;
            }
        } else {
            nonMoveX = nonMovePoint.x;
        }
        int nonMoveY = nonMovePoint.y;

        boolean canNotMove;
        if (isAxis) {
            if (distanceX == 0) {
                canNotMove = Math.abs(nonMoveY - tempY) < tempLimit;
            } else {
                canNotMove = distanceY != 0 || Math.abs(nonMoveX - tempX) < tempLimit;
            }
        } else {
            canNotMove = Math.abs(nonMoveX - tempX) < tempLimit ||
                    Math.abs(nonMoveY - tempY) < tempLimit;
        }

        if (!canNotMove) {
            switch (index) {
                case 0:
                    if (isAxis) {
                        canNotMove = tempY > nonMoveY - tempLimit;
                    } else {
                        canNotMove = tempY > nonMoveY - tempLimit ||
                                tempX > nonMoveX - tempLimit;
                    }
                    break;
                case 1:
                    if (isAxis) {
                        canNotMove = tempX < nonMoveX + tempLimit;
                    } else {
                        canNotMove = tempX < nonMoveX + tempLimit ||
                                tempY > nonMoveY - tempLimit;
                    }
                    break;
                case 2:
                    if (isAxis) {
                        canNotMove = tempY < nonMoveY + tempLimit;
                    } else {
                        canNotMove = tempY < nonMoveY + tempLimit ||
                                tempX < nonMoveX + tempLimit;
                    }
                    break;
                case 3:
                    if (isAxis) {
                        canNotMove = tempX > nonMoveX - tempLimit;
                    } else {
                        canNotMove = tempX > nonMoveX - tempLimit ||
                                tempY < nonMoveY + tempLimit;
                    }
                    break;
                default:
                    break;
            }
        }
        return canNotMove;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (DEBUG) {
            Log.i(TAG, "onDraw()");
        }

        // 画出几何图形
        if (mGeometryPath != null && mGeometryPaint != null) {
            canvas.drawPath(mGeometryPath, mGeometryPaint);
        }

        // 画出虚线边框
        if (mPathInfo != null && mEdgePaint != null) {
            canvas.drawRect(mPathInfo.left - mDragPointRadius, mPathInfo.top - mDragPointRadius,
                    mPathInfo.right + mDragPointRadius, mPathInfo.bottom + mDragPointRadius, mEdgePaint);
        }
    }

    /**
     * 刷新局部矩形范围
     */
    private void invalidateRec() {
        if (mPathInfo == null) {
            return;
        }

        invalidate(mPathInfo.left, mPathInfo.top, mPathInfo.right, mPathInfo.bottom);
    }

    //------------------------------HandWritingView中同名方法-----------------------start

    //------------------------------设置几何图形画笔相关属性---------------------start

    public void setGeometryPaintColor(@ColorInt int color) {
        mHandWritingView.setGeometryPaintColor(color);
    }

    public void setGeometryPaintStyle(@NonNull Paint.Style style) {
        mHandWritingView.setGeometryPaintStyle(style);
    }

    public void setAxisUnit(int axisUnit) {
        mHandWritingView.setAxisUnit(axisUnit);
    }
    //------------------------------设置几何图形画笔相关属性---------------------end

    public void closeHandWrite() {
        mHandWritingView.closeHandWrite();
    }

    public void openHandWrite() {
        mHandWritingView.openHandWrite();
    }

    public void setToWriting() {
        mHandWritingView.setToWriting();
    }

    public void setToRubber() {
        mHandWritingView.setToRubber();
    }

    public void restoreToImage(String str) {
        mHandWritingView.restoreToImage(str);
    }

    public void clear() {
        setWillNotDraw(true);// 取消onDraw()方法中画出的几何图形

        if (mEditGeometryView != null) { // 移除可编辑view
            removeView(mEditGeometryView);
        }

        if (mHandWritingView != null) {
            mHandWritingView.onCancelEditView();
            mHandWritingView.clear();
        }
    }

    public void loadBitmap(Bitmap bitmap) {
        mHandWritingView.loadBitmap(bitmap);
    }

    public void loadBitmap(byte[] data) {
        mHandWritingView.loadBitmap(data);
    }

    public String getEncodeBitmap() {
        return mHandWritingView.getEncodeBitmap();
    }

    public byte[] getBitmapBytes() {
        return mHandWritingView.getBitmapBytes();
    }

    public void recycleBitmap() {
        mHandWritingView.recycleBitmap();
    }

    public String getStrokes() {
        return mHandWritingView.getStrokes();
    }

    public boolean isStrokeChange() {
        return mHandWritingView.isStrokeChange();
    }

    public void resetStrokeChange() {
        mHandWritingView.resetStrokeChange();
    }

    public void setPenColor(@ColorInt int color) {
        mHandWritingView.setPenColor(color);
    }

    public int getPenColor() {
        return mHandWritingView.getPenColor();
    }

    public void setDrawType(DrawType type) {
        mHandWritingView.setDrawType(type);
    }

    public DrawType getDrawType() {
        return mHandWritingView.getDrawType();
    }

    public boolean isRubber() {
        return mHandWritingView.isRubber();
    }

    public boolean getCanDraw() {
        return mHandWritingView.getCanDraw();
    }

    public void setCanDraw(boolean canDraw) {
        mHandWritingView.setCanDraw(canDraw);
    }

    public Bitmap getBitmap() {
        return mHandWritingView.getBitmap();
    }

    public void setBitmap(Bitmap mBitmap) {
        mHandWritingView.setBitmap(mBitmap);
    }

    public void setBitmap(Bitmap mBitmap, String stroke) {
        mHandWritingView.setBitmap(mBitmap, stroke);
    }

    public void setDebug(boolean isDebug) {
        mHandWritingView.setDebug(isDebug);
    }

    public IHandWritingViewCache getHandWritingViewCache() {
        return mHandWritingView.getHandWritingViewCache();
    }

    public View getActionDownView() {
        return mHandWritingView.getActionDownView();
    }

    public void setActionDownView(View actionDownView) {
        mHandWritingView.setActionDownView(actionDownView);
    }

    public int getmWidth() {
        return mHandWritingView.getmWidth();
    }

    public int getmHeight() {
        return mHandWritingView.getmHeight();
    }

    public void setHandWritingViewCache(IHandWritingViewCache handWritingViewCache) {
        mHandWritingView.setHandWritingViewCache(handWritingViewCache);
    }

    public void setRecycleListener(HandWritingView.RecycleListener recycleListener) {
        mHandWritingView.setRecycleListener(recycleListener);
    }
    //------------------------------HandWritingView中同名方法-----------------------end
}