package com.hand.writing.view;

import android.content.Context;
import android.content.res.TypedArray;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hand.writing.DrawType;
import com.hand.writing.R;
import com.hand.writing.listener.IGeometryListener;

import java.util.ArrayList;
import java.util.List;

import static com.hand.writing.HandWritingViewHelper.DEBUG;
import static com.hand.writing.HandWritingViewHelper.IGNORE_TOOL_TYPE_INPUT;

/**
 * Desc:可以画出可编辑状态的几何图形的手写控件
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
    private int mGeometryRealWidth; //可编辑几何图形的真实宽度
    private int mGeometryRealHeight; //可编辑几何图形的真实高度
    private int mWidth; //手写控件宽度
    private int mHeight; //手写控件高度

    private HandWritingView mHandWritingView; //核心手写view
    private View mEditGeometryView; //当前可编辑区域根布局
    private LayoutParams mEditParams; //可编辑几何图形布局参数
    private RelativeLayout mGeometryRl; //几何图形根布局
    private Path mGeometryPath; //几何图形path路径
    private List<DragInfo> mDragInfoList; //存储当前可拖拽点信息的集合
    private IGeometryViewListener mGeometryViewListener; //可编辑状态几何图形监听接口
    private String mGeometryStrokesOnly;
    private boolean mIsCanScale;
    private View mScaleView;

    //-----------------------------inner interface--------------------start

    /**
     * 可编辑状态几何图形监听接口
     */
    public interface IGeometryViewListener {
        /**
         * 取消可编辑状态几何图形时回调
         *
         * @param handWritingGeometryView 当前取消几何图形的手写控件
         */
        void onCancelGeometry(HandWritingGeometryView handWritingGeometryView);

        /**
         * 保存可编辑状态几何图形时回调
         *
         * @param handWritingGeometryView 当前保存几何图形的手写控件
         */
        void onSaveGeometry(HandWritingGeometryView handWritingGeometryView);
    }

    /**
     * 几何图形可拖拽点信息类(用以标识当前拖拽点可左右移动的距离)
     */
    class DragInfo {
        int index;
        int distanceX;
        int distanceY;
    }
    //-----------------------------inner interface--------------------end

    public HandWritingGeometryView(Context context) {
        this(context, null);
    }

    public HandWritingGeometryView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HandWritingGeometryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * 用代码添加时的构造函数
     *
     * @param width  具体宽
     * @param height 具体高
     */
    public HandWritingGeometryView(Context context, int width, int height) {
        super(context);
        // 宽、高赋值
        mWidth = width == 0 ? 0 : width;
        mHeight = height == 0 ? 0 : height;
        mHandWritingView = new HandWritingView(context, width, height);
        LayoutParams layoutParams = new LayoutParams(mWidth, mHeight);
        addView(mHandWritingView, layoutParams);
        initDimens();
        mHandWritingView.setGeometryListener(this);
    }

    private void init(Context context, AttributeSet attrs) {
        initDimens();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.HandWritingGeometryView);
        mIsCanScale = ta.getBoolean(R.styleable.HandWritingGeometryView_is_can_scale, false);
        ta.recycle();
        mHandWritingView = new HandWritingView(context);
        mHandWritingView.setGeometryListener(this);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mHandWritingView, layoutParams);
    }

    private void initDimens() {
        mDragPointDiameter = getResources().getDimensionPixelSize(R.dimen.hw_drag_point_diameter);
        mImgBtnDiameter = getResources().getDimensionPixelSize(R.dimen.hw_img_btn_diameter);
        mDragPointRadius = mDragPointDiameter / 2;
        mDragPointDiameter2 = mDragPointDiameter + mDragPointDiameter;
        mLimitLeft = mDragPointRadius;
        mLimitTop = mImgBtnDiameter + mDragPointRadius;

        if (DEBUG) {
            Log.i(TAG, "initDimens() >>> mDragPointDiameter:" + mDragPointDiameter +
                    ", mImgBtnDiameter:" + mImgBtnDiameter + ", mLimitRight:" + mLimitRight + ", mLimitBottom:" + mLimitBottom);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        三星GT-N5100平板，API版本16 >>> JELLY_BEAN 在ScrollView中几何图形编辑状态拖动到底部时，
//        changed会变成true,bottom高度会高出1，高版本(5.0以上)的版本没有该问题的产生；需要查看两个版本的底层实现...
//        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN &&
//                mHandWritingCoreView.isGeometryEditable() &&
//                bottom - top > mHeight) {
//            Log.i(TAG, "" + ((FrameLayout) getParent()).isScrollContainer());
//            super.onLayout(changed, left, top, right, mHeight);
//        }
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            mWidth = getWidth();
            mHeight = getHeight();
            initLimitBottomAndRight();
        }

        if (DEBUG) {
            Log.i(TAG, "onLayout() >>> changed:" + changed + ", isGeometryEditable:"
                    + mHandWritingView.isGeometryEditable() + ", width:" + mWidth + ", height:" + mHeight +
                    ", [left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom + "]");
        }
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        LayoutParams layoutParams = (LayoutParams) mHandWritingView.getLayoutParams();
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

    private void initLimitBottomAndRight() {
        mLimitRight = mWidth - mDragPointRadius;
        mLimitBottom = mHeight - mDragPointRadius - 1;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int toolType = ev.getToolType(0);
        //输入设备为手写笔
        if (IGNORE_TOOL_TYPE_INPUT || (mHandWritingView != null && toolType == MotionEvent.TOOL_TYPE_STYLUS)) {
            int x = (int) ev.getX();
            int y = (int) ev.getY();

            if (mHandWritingView.isGeometryType()) {
                // 校验当前几何图形可编辑状态是否有效
                if (!mHandWritingView.isRubber() &&
                        !mHandWritingView.isGeometryEditable() &&
                        (x < mLimitLeft || y < mLimitTop || x > mLimitRight || y > mLimitBottom)) {
                    if (DEBUG) {
                        Log.i(TAG, "onInterceptTouchEvent() >>> invalid position  x : " + x + ", y : " + y);
                    }
                    showInvalidateGeometryToast();
                    return true;
                }
            } else if (mHandWritingView.isInvalid(x, y)) {
                // 基本无效线型直接中断触摸事件
                return true;
            }
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setOnTouchListener(l);
    }

    //------------------------------几何图形监听回调方法------------------------start
    @Override
    public void onShowEditGeometry(HandWritingView.PathInfo pathInfo, Paint geometryPaint, DrawType drawType) {
        if (pathInfo == null || pathInfo.pointsList == null || geometryPaint == null || drawType == null) {
            return;
        }

        // 2018/4/17 0017/15:29 解决手写控件嵌套在ScrollView中，可编辑几何图形书写到大于mHeight高度时，导致ScrollView重新调用onLayout()方法，
        // 最终在调用layoutChildren()方法中的子view调用getVisibility()时报空指针的问题  ------------modify by JiLin-------s
        if (pathInfo.bottom + mDragPointRadius > mHeight - 1) {
            if (mHandWritingView != null) {
                mHandWritingView.onCancelEditView();
            }
            // 重置状态
            reset();
            showInvalidateGeometryToast();
            return;
        }
        //-------------------------------e---------------------------

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
                isInvalid = disX < mDragPointDiameter && disY < mDragPointDiameter;
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

    @Override
    public void handleEditableGeometry() {
        saveGeometryView();
    }

    @Override
    public int getLimitLeft() {
        return mLimitLeft;
    }

    @Override
    public int getLimitTop() {
        return mLimitTop;
    }

    @Override
    public int getLimitRight() {
        return mLimitRight;
    }

    @Override
    public int getLimitBottom() {
        return mLimitBottom;
    }

    @Override
    public int getGeometryRealWidth() {
        return mGeometryRealWidth;
    }

    @Override
    public int getGeometryRealHeight() {
        return mGeometryRealHeight;
    }

    @Override
    public int getDragPointRadius() {
        return mDragPointRadius;
    }

    @Override
    public boolean isCanScale() {
        return mIsCanScale;
    }

    @Override
    public float getHandWritingX() {
        return getX();
    }

    @Override
    public float getHandWritingY() {
        return getY();
    }

    @Override
    public void onSetX(float revisedX) {
        setX(revisedX);
        if (mScaleView != null) {
            mScaleView.setX(revisedX);
        }
    }

    @Override
    public void onSetY(float revisedY) {
        setY(revisedY);
        if (mScaleView != null) {
            mScaleView.setY(revisedY);
        }
    }

    @Override
    public void onScale(float curScale) {
        setScaleX(curScale);
        setScaleY(curScale);

        if (mScaleView != null) {
            mScaleView.setScaleX(curScale);
            mScaleView.setScaleY(curScale);
        }
    }
    //------------------------------几何图形监听回调方法------------------------end

    public void showInvalidateGeometryToast() {
        Toast.makeText(getContext(), "请不要超出书写区域!", Toast.LENGTH_SHORT).show();
    }

    private void initGeometryView() {
        // 只有设置了这个属性,才会调用onDraw()方法
        setWillNotDraw(false);
        // 防止重复添加时会有布局残留
        if (mEditGeometryView != null) {
            if (DEBUG) {
                Log.i(TAG, "mEditGeometryView != null");
            }
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
                    params.setMargins(mGeometryRealWidth / 2, 0, 0, 0);
                    break;
                case 1:
                    params.setMargins(mGeometryRealWidth, mGeometryRealHeight / 2, 0, 0);
                    break;
                case 2:
                    params.setMargins(mGeometryRealWidth / 2, mGeometryRealHeight, 0, 0);
                    break;
                case 3:
                    params.setMargins(0, mGeometryRealHeight / 2, 0, 0);
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
        if (mHandWritingView == null) {
            return;
        }
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

        if (mHandWritingView == null) {
            return;
        }
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
        mGeometryRealWidth = mPathInfo.right - mPathInfo.left;
        mGeometryRealHeight = mPathInfo.bottom - mPathInfo.top;
        int geometryEditWidth = mGeometryRealWidth + mDragPointDiameter;
        int geometryEditHeight = mGeometryRealHeight + mDragPointDiameter;
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

//        当bottom没有设置为0，嵌套在ScrollView中使用处于编辑状态时，整个手写view会被拉伸
        mEditParams.setMargins(left, top, 0, 0);
    }

    //------------------------------事件监听方法-------------------------------start
    @NonNull
    private OnClickListener getCancelClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelGeometryView();
            }
        };
    }

    @NonNull
    private OnClickListener getSaveClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                saveGeometryView();
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
                int toolType = event.getToolType(0);
                if (IGNORE_TOOL_TYPE_INPUT || toolType == MotionEvent.TOOL_TYPE_STYLUS) { //输入设备为手写笔
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
                } else { //不响应非手写笔的触摸事件
                    return false;
                }
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
        tempRight += distanceX;
        tempTop += distanceY;
        tempBottom += distanceY;

        if (distanceX < 0) {
            if (tempLeft < mLimitLeft) {
                tempLeft = mLimitLeft;
                tempRight = tempLeft + mGeometryRealWidth;
                distanceX = tempLeft - mPathInfo.left;
            }
        } else {
            if (tempRight > mLimitRight) {
                tempRight = mLimitRight;
                tempLeft = tempRight - mGeometryRealWidth;
                distanceX = tempRight - mPathInfo.right;
            }
        }

        if (distanceY < 0) {
            if (tempTop < mLimitTop) {
                tempTop = mLimitTop;
                tempBottom = tempTop + mGeometryRealHeight;
                distanceY = tempTop - mPathInfo.top;
            }
        } else {
            if (tempBottom > mLimitBottom) {
                tempBottom = mLimitBottom;
                tempTop = tempBottom - mGeometryRealHeight;
                distanceY = tempBottom - mPathInfo.bottom;
            }
        }

        for (HandWritingView.PointInfo pointInfo : mPathInfo.pointsList) {
            pointInfo.x += distanceX;
            pointInfo.y += distanceY;
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

//        invalidate(tempLeft, tempTop, tempRight, tempBottom);
    }

    @NonNull
    private OnTouchListener getDragOnTouchListener() {
        return new OnTouchListener() {
            private HandWritingView.PointInfo mPointInfo;
            int downX;
            int downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int toolType = event.getToolType(0);
                if (IGNORE_TOOL_TYPE_INPUT || toolType == MotionEvent.TOOL_TYPE_STYLUS) { //输入设备为手写笔
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
                } else {//不响应非手写笔的触摸事件
                    return false;
                }
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

        if (tempX < mLimitLeft) {
            tempX = mLimitLeft;
        }
        if (tempY < mLimitTop) {
            tempY = mLimitTop;
        }
        if (tempX > mLimitRight) {
            tempX = mLimitRight;
        }
        if (tempY > mLimitBottom) {
            tempY = mLimitBottom;
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

    /**
     * 设置手写笔迹宽度阈值,用以控制手写笔迹的粗细.
     *
     * @param paintSizeThreshold 手写笔迹宽度阈值;范围:1.0f <= paintSizeThreshold <= 4.0f
     */
    public void setPaintSizeThreshold(float paintSizeThreshold) {
        if (mHandWritingView != null) {
            mHandWritingView.setPaintSizeThreshold(paintSizeThreshold);
        }
    }

    public void setGeometryPaintColor(@ColorInt int color) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setGeometryPaintColor(color);
    }

    public void setGeometryPaintStyle(@NonNull Paint.Style style) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setGeometryPaintStyle(style);
    }

    public void setAxisUnit(int axisUnit) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setAxisUnit(axisUnit);
    }
    //------------------------------设置几何图形画笔相关属性---------------------end

    /**
     * 取消可编辑状态下的几何图形
     */
    public void cancelGeometryView() {
        boolean isSuccess = false;
        if (mHandWritingView != null) {
            isSuccess = mHandWritingView.onCancelEditView();
        }

        if (DEBUG) {
            Log.i(TAG, "取消几何图形!!! isSuccess : " + isSuccess);
        }

        // 重置状态
        reset();

        if (isSuccess && mGeometryViewListener != null) {
            mGeometryViewListener.onCancelGeometry(HandWritingGeometryView.this);
        }
    }

    /**
     * 保存可编辑状态下的几何图形
     */
    public void saveGeometryView() {
        boolean isSuccess = false;
        if (mHandWritingView != null) {
            isSuccess = mHandWritingView.onSaveEditView(mDrawType);
        }

        if (DEBUG) {
            Log.i(TAG, "保存几何图形!!! isSuccess : " + isSuccess);
        }

        // 单独存储当前几何图形笔迹信息
        if (isSuccess && mPathInfo != null) {
            mGeometryStrokesOnly = mHandWritingView.getGeometryStrokesOnly(mPathInfo.path);
        }

        // 重置状态
        reset();

        if (isSuccess && mGeometryViewListener != null) {
            mGeometryViewListener.onSaveGeometry(HandWritingGeometryView.this);
        }
    }

    public String getGeometryStrokesOnly() {
        return mGeometryStrokesOnly;
    }

    /**
     * 当手写控件支持缩放时,设置跟随手写控件一起缩放的scaleView;如果当前手写控件不支持缩放,设置无效.
     *
     * @param scaleView 跟随手写控件一起缩放的View
     */
    public void setScaleView(View scaleView) {
        if (mIsCanScale) {
            mScaleView = scaleView;
        }
    }

    public IGeometryViewListener getGeometryViewListener() {
        return mGeometryViewListener;
    }

    /**
     * 设置可编辑状态几何图形监听接口回调
     */
    public void setGeometryViewListener(IGeometryViewListener geometryViewListener) {
        mGeometryViewListener = geometryViewListener;
    }

    public boolean isGeometryEditable() {
        return mHandWritingView != null && mHandWritingView.isGeometryEditable();
    }

    public void closeHandWrite() {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.closeHandWrite();
    }

    public void openHandWrite() {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.openHandWrite();
    }

    public void setToWriting() {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setToWriting();
    }

    public void setToRubber() {
        if (mHandWritingView == null) {
            return;
        }
        // 当前为几何图形编辑状态并且切换为"皮擦"状态时,保存当前编辑状态的几何图形
        if (mHandWritingView.isGeometryEditable()) {
            saveGeometryView();
        }

        mHandWritingView.setToRubber();
    }

    public void restoreToImage(String str) {
        if (mHandWritingView == null) {
            if (DEBUG) {
                Log.i(TAG, "mHandWritingCoreView == null");
            }
            return;
        }
        mHandWritingView.restoreToImage(str);
    }

    public void restoreToGeometryOnly(String str) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.restoreToGeometryOnly(str);
    }

    public void clear() {
        if (mHandWritingView != null) {
            if (mHandWritingView.isGeometryEditable()) {
                // 取消当前正在编辑的几何图形
                cancelGeometryView();
            }
            mHandWritingView.clear();
        }
    }

    public void loadBitmap(Bitmap bitmap) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.loadBitmap(bitmap);
    }

    public void loadBitmap(byte[] data) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.loadBitmap(data);
    }

    public String getEncodeBitmap() {
        if (mHandWritingView == null) {
            return "";
        }
        return mHandWritingView.getEncodeBitmap();
    }

    public byte[] getBitmapBytes() {
        if (mHandWritingView == null) {
            return null;
        }
        return mHandWritingView.getBitmapBytes();
    }

    public void recycleBitmap() {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.recycleBitmap();
    }

    public String getStrokes() {
        if (mHandWritingView == null) {
            if (DEBUG) {
                Log.i(TAG, "mHandWritingCoreView == null");
            }
            return "";
        }
        // 当为几何图形编辑状态,调用该方法时，将会把几何图形笔迹保存到笔迹字符串中
        if (mHandWritingView.isGeometryEditable()) {
            saveGeometryView();
        }

        return mHandWritingView.getStrokes();
    }

    public boolean isStrokeChange() {
        return mHandWritingView != null && mHandWritingView.isStrokeChange();
    }

    public void resetStrokeChange() {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.resetStrokeChange();
    }

    public void setPenColor(@ColorInt int color) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setPenColor(color);
    }

    public int getPenColor() {
        if (mHandWritingView == null) {
            return -1;
        }
        return mHandWritingView.getPenColor();
    }

    public int getGeometryPenColor() {
        if (mHandWritingView == null) {
            return -1;
        }
        return mHandWritingView.getGeometryPenColor();
    }

    public boolean setDrawType(DrawType type) {
        return mHandWritingView != null && mHandWritingView.setDrawType(type);
    }

    public void setRubberBitmap(Bitmap rubber) {
        if (rubber == null) {
            return;
        }
        if (mHandWritingView != null) {
            mHandWritingView.setRubberBitmap(rubber);
        }
    }

    public DrawType getDrawType() {
        if (mHandWritingView == null) {
            return null;
        }
        return mHandWritingView.getDrawType();
    }

    public boolean isHWCInitFinished() {
        return mHandWritingView != null && mHandWritingView.isHWCInitFinished();
    }

    public boolean isRubber() {
        return mHandWritingView != null && mHandWritingView.isRubber();
    }

    public boolean getCanDraw() {
        return mHandWritingView != null && mHandWritingView.getCanDraw();
    }

    public void setCanDraw(boolean canDraw) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setCanDraw(canDraw);
    }

    public Bitmap getBitmap() {
        if (mHandWritingView == null) {
            return null;
        }
        return mHandWritingView.getBitmap();
    }

    public void setBitmap(Bitmap mBitmap) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setBitmap(mBitmap);
    }

    public void setBitmap(Bitmap mBitmap, String stroke) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setBitmap(mBitmap, stroke);
    }

    public void setDebug(boolean isDebug) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setDebug(isDebug);
    }

    public View getActionDownView() {
        if (mHandWritingView == null) {
            return null;
        }
        return mHandWritingView.getActionDownView();
    }

    public void setActionDownView(View actionDownView) {
        if (mHandWritingView == null) {
            return;
        }
        mHandWritingView.setActionDownView(actionDownView);
    }

    public int getmWidth() {
        if (mHandWritingView == null) {
            return -1;
        }
        return mHandWritingView.getHWWidth();
    }

    public int getmHeight() {
        if (mHandWritingView == null) {
            return -1;
        }
        return mHandWritingView.getHWHeight();
    }

    public void closeScale() {
        mIsCanScale = false;
    }

    /**
     * 设置当前手写控件支持的最大缩放比例;当手写控件不支持缩放时,设置该值无效.
     *
     * @param maxScale 手写控件最大缩放比;取值范围为: 1.0f < maxScale < 5.0f
     */
    public void setMaxScale(float maxScale) {
        if (mIsCanScale && mHandWritingView != null) {
            mHandWritingView.setMaxScale(maxScale);
        }
    }

    /**
     * 释放手写控件相关资源;该控制权交给应用来控制.
     */
    public void release() {
        mPathInfo = null;
        mGeometryPaint = null;
        mEdgePaint = null;
        mGeometryViewListener = null; //释放几何图形编辑状态监听
        mScaleView = null; //释放跟随手写控件一起缩放的View

        if (mHandWritingView != null) {
            mHandWritingView.release();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
    //------------------------------HandWritingView中同名方法-----------------------end
}