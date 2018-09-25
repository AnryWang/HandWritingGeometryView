package com.hand.writing.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.ScrollView;
import android.widget.Toast;

import com.hand.writing.CustomGestureDetector;
import com.hand.writing.DrawType;
import com.hand.writing.HandWritingCanvas;
import com.hand.writing.HandWritingViewHelper;
import com.hand.writing.listener.IGeometryListener;
import com.hand.writing.listener.OnGestureListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hand.writing.HandWritingViewHelper.DEBUG;
import static com.hand.writing.HandWritingViewHelper.IGNORE_TOOL_TYPE_INPUT;
import static com.hand.writing.HandWritingViewHelper.getStrokeVersion;
import static com.hand.writing.math.MathArithmetic.intersect1;
import static com.hand.writing.math.MathArithmetic.lerp;
import static com.hand.writing.math.MathArithmetic.pointToLine;
import static com.hand.writing.math.MathArithmetic.rotateVec;
import static com.hand.writing.utils.HandWritingCacheUtils.cachedToFloat;
import static com.hand.writing.utils.HandWritingCacheUtils.cachedToInt;
import static com.hand.writing.utils.HandWritingCacheUtils.toFloat;
import static com.hand.writing.utils.HandWritingCacheUtils.toInt;

/**
 * Desc:手写控件核心类
 *
 * @author JiLin
 * @version 1.0
 * @since 2017/9/18 0018
 */
public class HandWritingView extends View {
    private static String TAG = "HandWritingView";
    // ------------------------------------------------------------------静态常量
    private static final float MAX_PEN_SIZE = 2.5f;// 最大笔迹宽度
    private static final int MIN_PEN_SIZE = 1;// 最小笔迹宽度
    private static final int DEFAULT_PAINT_SIZE = 2;// 默认笔迹宽度
    private static float mPaintSizeThreshold = MAX_PEN_SIZE - MIN_PEN_SIZE;
    public static final int AXIS_ARROW_HEIGHT = 10; //坐标轴箭头高度
    public static final int AXIS_OTHER_LENGTH = 4; //坐标轴其它点线段长度
    // ------------------------------------------------------------------全局变量
    // -------------------------core
    private HandWritingCanvas mHandWritingCanvas; // 核心画布
    private Path mTilePath = new Path();
    private PathMeasure mPathMeasure = new PathMeasure();
    private Canvas mWritingViewCanvas;// 手写view bitmap canvas
    private Bitmap mWritingViewBitmap;// 手写view的bitmap
    private Path mPath;// 核心笔画路径
    private Paint mPaint;// 核心画笔
    private Paint mRubberPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);// 橡皮画笔
    private Paint mBitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);// 绘画背景图片画笔

    // -------------------------for touch
    // 记录触击事件的开始点与结束点
    private int endX, endY; // 用来描述线段以及带箭头线段的终点
    private int oldX = -1, oldY = -1;// 用来判断曲线的开始与结束
    private int minX = 0, minY = 0, maxX = 0, maxY = 0;// 这四个变量用来记录划线过程中，当前线条的矩形范围，用来提高擦除时的碰撞检测性能
    private List<PathInfo> pathsList = new ArrayList<>();// 用来存储整个笔记各线条的数据结构，主要擦除时用
    private View mActionDownView; // 传递点击事件
    // -------------------------宽 高
    private int mWidth;// view的宽
    private int mHeight;// view的高
    private int mStrokeWidth;// 笔迹的宽
    private int mStrokeHeight;// 笔迹的高
    private DrawType mDrawType = DrawType.CURVE;// 笔迹类型,默认为曲线类型
    private boolean mIsRubber;// 默认值为false表示书写状态;当isRubber = true时,表示为擦除状态.
    private boolean canDraw;// 是否可被绘画，在onDraw方法 首先监测
    private int validAbs = 2;// 若太小笔记会不好看，若太大，笔记出现的慢
    private boolean hasRange;// 笔迹线条是否具有(左上右下)范围信息,新的笔迹都具有范围信息;旧的笔迹可能没有该信息
    private String[] ranges = new String[5];
    private int eraserSize = 20; // 默认皮擦大小
    private int eraserHalf = eraserSize / 2;
    private Bitmap mRubber = Bitmap.createBitmap(eraserSize, eraserSize, Bitmap.Config.ARGB_4444); //橡皮擦Bitmap
    float lastPressure;
    float lastLength;
    Path linePath = new Path(); // 用以绘制直线的path信息
    private boolean isNeedRedraw = true;// 用于记录是否需要重绘整个画面,因为android的同一条线重绘多次后会变粗，所以会有这种操作

    /* 下面用于圈定擦除时应该重绘的局部矩形范围 */
    private int rectMinX;
    private int rectMinY;
    private int rectMaxX;
    private int rectMaxY;
    private boolean mIsStrokesChanged;// 笔迹是否改变的标志位
    private StringBuilder mStrokes;// 用来存储整个笔记各线条的字符串，笔记格式如下:
    /*@后面是多条线的信息，线用‘=’分开，每条线信息包括：线型、颜色、多个点信息以及这条线的范围
    view宽,view高,笔记宽,笔记高&版本@线型#颜色#x1,y1,压力值;x2,y2,压力值;...#left,top,right,bottom=线型#颜色#x3,y3,压力值;x4,y4,压力值;
    ...#left,top,right,bottom
    800,1200,420,119&1.0@0#-16776961#288,97,0.23560464;295,96,0.4120525;299,98,0.46805257;303,100,0.51835746;307,106,0.58026785;
    308,113,0.63732594;308,119,0.68476045;318,115,0.71865356;323,108,0.5514474;
    325,106,0.31040287#288,96,325,119=0#-16776961#370,92,0.41343236;376,89,0.5705198;380,91,0.62366414;381,95,0.66852975;
    382,101,0.715369;383,106,0.7622229;391,105,0.8033477;401,98,0.81574965;414,85,0.6300096;
    420,78,0.4726293;420,78,0.3545941#370,78,420,106*/
    private String mOldStrokes = "";

    //--------------add by JiLin--------------
    private boolean mIsDispatch; //特殊设备是否响应双指触摸滑动事件标记
    private boolean mIsTempChangeRubber; //用以标记当前是否临时修改为擦除状态标记,默认值为false;为true时表示有临时更改
    private Paint mGeometryPaint; //几何图形画笔
    private Path mGeometryPath;
    private PathInfo mGeometryTempInfo;
    private boolean mIsGeometryEditable; //是否是几何图形编辑状态标记;当为true时,表示当前有未保存的几何图形;
    private IGeometryListener mGeometryListener; //几何图形监听回调接口
    private int mAxisUnit = 50; //坐标轴单位长度
    private AtomicInteger mAtomicInteger = new AtomicInteger();
    private volatile boolean mIsHWCInitFinished;
    private Runnable mRestoreToImageRunnable;
    private int mStrokesThreshold;
    private OnGestureListener mOnGestureListener;
    private CustomGestureDetector mCustomGestureDetector;
    private float mMinScale = 1.0f; //最小缩放比
    private float mMaxScale = 5.0f; //最大缩放比
    private RectF dirtyRect = new RectF();

    //-----------------------------inner interface--------------------start
    interface SplitCall {
        void splitCall(int index, String subString);
    }
    //-----------------------------inner interface--------------------end

    //-----------------------------inner class--------------------start
    /* 存储单个点的数据结构 */
    public class PointInfo {
        int x;
        int y;
        float pressure;
        int index = -1;// 几何图形中需要用到的控制点索引
    }

    /* 存储单条线的数据结构 */
    public class PathInfo {
        String path;// 字符串，包括线型、颜色、所有点，矩形范围信息

        int drawType;
        int color;
        List<PointInfo> pointsList;

        /* 线所在的矩形范围 */
        int left;
        int top;
        int right;
        int bottom;
    }

    // 求圆心与另一个点形成的直线与圆的交点
    public class CGPoint {
        double x;
        double y;

        CGPoint() {
        }

        @Override
        public String toString() {
            return "x=" + this.x + ", y=" + this.y;
        }
    }
    //-----------------------------inner class--------------------end

    //-----------------------------constructor--------------------start
    public HandWritingView(Context context) {
        this(context, null);
    }

    public HandWritingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HandWritingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (DEBUG) {
            Log.i(TAG, "constructor >>> context = [" + context + "], attrs = ["
                    + attrs + "], defStyleAttr = [" + defStyleAttr + "]");
        }
        initArgc();
        initCoreWriting();
        initRubberBitmap();
    }

    /**
     * 用代码添加时的构造函数
     *
     * @param width  具体宽
     * @param height 具体高
     */
    public HandWritingView(Context context, int width, int height) {
        super(context);
        // 宽、高赋值
        mWidth = width == 0 ? 0 : width;
        mHeight = height == 0 ? 0 : height;
        if (DEBUG) {
            Log.i(TAG, "constructor >>> context = [" + context + "], width = ["
                    + width + "], height = [" + height + "]");
        }
        // 初始化view宽高
        LayoutParams layoutParams = new LayoutParams(mWidth, mHeight);
        super.setLayoutParams(layoutParams);
        initArgc();
        initCoreWriting();
        initRubberBitmap();
        initHandWritingCanvas();
    }

    /**
     * 参数
     */
    private void initArgc() {
        mStrokes = new StringBuilder();
        canDraw = true;
        mDrawType = DrawType.CURVE;
        mIsStrokesChanged = false;
    }

    /**
     * 初始化手写控件的核心成员
     */
    private void initCoreWriting() {
        // -------------------------初始化path
        mPath = new Path();
        // -------------------------核心Paint的初始化
        mPaint = new Paint();
        mPaint.setAntiAlias(true); //打开抗锯齿
        /*
            设置图像抖动（注意，它就叫抖动，不是防抖动，也不是去抖动，有些人在翻译的时候自作主张地加了一个「防」
            字或者「去」字，这是不对的），是指把图像从较高色彩深度（即可用的颜色数）向较低色彩深度的区域绘制时，
            在图像中有意地插入噪点，通过有规律地扰乱图像来让图像对于肉眼更加真实的做法。
            关于图像抖动的详细解释详见 : https://hencoder.com/ui-1-2/
        */
        mPaint.setDither(true);
        /*
            设置使用双线性过滤来绘制Bitmap;图像在放大绘制的时候，默认使用的是最近邻插值过滤，这种算法简单，
            但会出现马赛克现象；而如果开启了双线性过滤，就可以让结果图像显得更加平滑。
        */
        mPaint.setFilterBitmap(true);
        mPaint.setColor(Color.BLUE); //设置画笔为蓝色
        mPaint.setStyle(Paint.Style.STROKE); //stroke笔画
        mPaint.setStrokeWidth(DEFAULT_PAINT_SIZE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        // -------------------------初始化几何图形画笔
        mGeometryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGeometryPaint.setColor(Color.BLACK);
        mGeometryPaint.setStrokeWidth(DEFAULT_PAINT_SIZE);
        mGeometryPaint.setStyle(Paint.Style.STROKE);
        mGeometryPath = new Path();
    }

    /**
     * 初始化HandWritingCanvas
     */
    private void initHandWritingCanvas() {
        if (mWidth <= 0 || mHeight <= 0) {
            if (DEBUG) {
                Log.e(TAG, "mWidth or mHeight is 0!!!");
            }
            return;
        }

        if (mHandWritingCanvas != null && mHandWritingCanvas.width == mWidth
                && mHandWritingCanvas.height == mHeight) {
            return;
        }

        mHandWritingCanvas = new HandWritingCanvas(mWidth, mHeight);
        mIsHWCInitFinished = true;
    }

    /**
     * 初始化默认皮擦
     */
    private void initRubberBitmap() {
        mRubberPaint.setColor(Color.parseColor("#59d1a3"));
        Canvas canvasTmp = new Canvas(mRubber);
        canvasTmp.drawCircle(eraserHalf, eraserHalf, eraserHalf, mRubberPaint);
    }
    //-----------------------------constructor--------------------end

    // -------------------------writing & rubber------------------start

    /**
     * 禁用手写
     */
    public void closeHandWrite() {
        if (DEBUG) {
            Log.i(TAG, "closeHandWrite");
        }
        setCanDraw(false);
    }

    /**
     * 打开手写
     */
    public void openHandWrite() {
        if (DEBUG) {
            Log.i(TAG, "openHandWrite");
        }
        setCanDraw(true);
    }

    /**
     * 将擦除状态切换为手写状态
     */
    public void setToWriting() {
        if (DEBUG) {
            Log.i(TAG, "setToWriting");
        }
        if (!mIsRubber) {// 书写状态，跳出
            return;
        }

        setToWritingInside();

        // 手写时笔记是直接存储到字符串里的，擦除时则用数据结构来进行碰撞检测，所以在书写和擦除切换时要进行字符串与数据结构的转换
        listToStrokes(true);
    }

    private void setToWritingInside() {
        if (!mIsRubber) {// 书写状态，跳出
            return;
        }
        if (mPaint != null) {
            mIsRubber = false;
            mPaint.setStrokeWidth(DEFAULT_PAINT_SIZE);
            // 从擦除过来 ，需要加这个，要不就笔画很大
            mPaint.setXfermode(null);
            setDrawType(mDrawType);
        }
    }

    /**
     * 将手写状态切换为擦除状态
     */
    public void setToRubber() {
        if (DEBUG) {
            Log.i(TAG, "setToRubber");
        }
        if (mIsRubber) {// 擦除状态，跳出
            return;
        }

        onSaveEditView(mDrawType);

        setToRubberInside();

        if (DEBUG) {
            Log.i(TAG, "this.mStrokes.toString()=" + this.mStrokes.toString());
        }

        // 手写时笔记是直接存储到字符串里的，擦除时则用数据结构来进行碰撞检测，所以在书写和擦除切换时要进行字符串与数据结构的转换
        pathsList = strokesToList(mStrokes.toString());
    }

    private void setToRubberInside() {
        if (mIsRubber) {// 擦除状态，跳出
            return;
        }

        mIsRubber = true;
    }
    // -------------------------writing & rubber------------------end

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (DEBUG) {
            Log.i(TAG, "onMeasure >>> getWidth:" + this.getWidth() + ",getHeight:" + this.getHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // 此时有宽 、 高
        if (DEBUG) {
            Log.i(TAG, "onLayout() >>> changed:" + changed + ", width:" + this.getWidth()
                    + ", height:" + this.getHeight());
        }

        if (changed) {
            mWidth = getWidth();
            mHeight = getHeight();
            mStrokesThreshold = mWidth * mHeight / 3;
            initHandWritingCanvas();
            String strokesString = getStrokesString();
            if (!TextUtils.isEmpty(strokesString)) {
                restoreToImage(strokesString);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /*适配所有带手写笔的平板*//* 主要思想是手指滑动，笔书写 */
        int toolType = event.getToolType(0);
        if (IGNORE_TOOL_TYPE_INPUT || toolType == MotionEvent.TOOL_TYPE_STYLUS) { //输入设备为手写笔
            if (DEBUG) {
                Log.i(TAG, "input toolType == TOOL_TYPE_STYLUS");
            }
        } else if (HandWritingViewHelper.isSpecialDevice()) { //特殊设备允许使用单手指书写
            if (DEBUG) {
                Log.i(TAG, "isSpecialDevice() == true!!!");
            }
        } else {//非手写笔、非特殊设备
            return handleGestureEvent(event);
        }

        if (mActionDownView != null) {
            mActionDownView.dispatchTouchEvent(event);
        }

        //这里通过标志位来控制是否禁止手写
        return canDraw && handleTouchEvent(event);
    }

    private boolean handleGestureEvent(MotionEvent event) {
        if (mGeometryListener == null || !mGeometryListener.isCanScale()) {
            if (DEBUG) {
                Log.e(TAG, "当前手写控件不允许缩放.mGeometryListener.isCanScale() == false !!!");
            }
            return false;
        }

        if (mOnGestureListener == null) {
            mOnGestureListener = new OnGestureListener() {
                private float mLimitTop; //上边距限制值
                private float mLimitLeft; //左边距限制值
                private float mPreScale = mMinScale; //上一次缩放比
                private float mScaleWidth; //缩放比例宽度
                private float mScaleHeight; //缩放比例高度
                private ViewParent mParentScrollView;
                private ViewPager mParentViewPager;

                @Override
                public void onDrag(float dx, float dy) {
                    if (mGeometryListener == null || mPreScale <= mMinScale ||
                            mCustomGestureDetector != null && mCustomGestureDetector.isScaling()) {
                        return; // Do not drag if we are already scaling
                    }
                    // 优化拖拽体验,过滤掉迷之抖动
                    if (Math.abs(dx) < 2 && Math.abs(dy) < 2) {
                        return;
                    }

                    float revisedX = getRevisedX(mGeometryListener.getHandWritingX() + dx);
                    float revisedY = getRevisedY(mGeometryListener.getHandWritingY() + dy);

                    if (DEBUG) {
                        Log.i(TAG, "mLimitLeft = [" + mLimitLeft + "], mLimitTop = [" + mLimitTop +
                                "], " + "dx = [" + dx + "], dy = [" + dy + "], revisedX = [" +
                                revisedX + "], revisedY = [" + revisedY + "]");
                    }

                    if ((revisedX == mLimitLeft && dx > 0) ||
                            (revisedX == -mLimitLeft && dx < 0)) {
                        //此时处于手写控件的边缘位置,请求父类中断事件
                        switchViewPagerInterceptTouchEvent(false);
                    } else {
                        switchViewPagerInterceptTouchEvent(true);
                    }

                    if ((revisedY == mLimitTop && dx > 0) ||
                            (revisedY == -mLimitTop && dx < 0)) {
                        //此时处于手写控件的边缘位置,请求父类中断事件
                        switchScrollViewInterceptTouchEvent(false);
                    } else {
                        switchScrollViewInterceptTouchEvent(true);
                    }

                    mGeometryListener.onSetX(revisedX);
                    mGeometryListener.onSetY(revisedY);
                }

                private float getRevisedX(float x) {
                    mLimitLeft = (mScaleWidth - mWidth) / 2;
                    if (x < -mLimitLeft) {
                        x = -mLimitLeft;
                    }
                    if (x > mLimitLeft) {
                        x = mLimitLeft;
                    }
                    return x;
                }

                private float getRevisedY(float y) {
                    mLimitTop = (mScaleHeight - mHeight) / 2;
                    if (y < -mLimitTop) {
                        y = -mLimitTop;
                    }
                    if (y > mLimitTop) {
                        y = mLimitTop;
                    }
                    return y;
                }

                @Override
                public void onFling(float startX, float startY, float velocityX, float velocityY) {
                    if (mPreScale <= mMinScale || mCustomGestureDetector != null && mCustomGestureDetector.isScaling()) {
                        return; // Do not drag if we are already scaling
                    }
                    if (DEBUG) {
                        Log.i(TAG, "startX = [" + startX + "], startY = [" +
                                startY + "], velocityX = [" + velocityX + "], velocityY = [" + velocityY + "]");
                    }
                }

                @Override
                public boolean onScaleBegin(ScaleGestureDetector detector) {
                    if (DEBUG) {
                        Log.i(TAG, "onScaleBegin()");
                    }
                    return true;
                }

                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    if (mCustomGestureDetector != null && mCustomGestureDetector.isDragging()) {
                        return true;
                    }

                    // 获取当前手写控件的缩放比例
                    float curScale = Math.round(mPreScale * detector.getScaleFactor() * 100) / 100f;
                    if (DEBUG) {
                        Log.i(TAG, "mPreScale = [" + mPreScale + "], curScale = [" + curScale + "]");
                    }

                    // 限制缩放比例
                    if (curScale < mMinScale || curScale > mMaxScale) {
                        return true;
                    }

                    // 计算缩放比例宽高信息
                    mScaleWidth = mWidth * curScale;
                    mScaleHeight = mHeight * curScale;

                    if (DEBUG) {
                        Log.i(TAG, "mScaleWidth = [" + mScaleWidth + "], mScaleHeight = [" + mScaleHeight + "]");
                    }

                    switchScrollViewInterceptTouchEvent(true);
//                    switchViewPagerInterceptTouchEvent(true); //当频繁在ViewPager中的条目缩放时,会出现崩溃

                    // 优化当前视图在非中心区域的缩放体验
                    if (mGeometryListener != null) {
                        // 计算当前缩放比和最小缩放比的差值
                        float minOffset = curScale - mMinScale;

                        if (DEBUG) {
                            Log.i(TAG, "minOffset = [" + minOffset + "]");
                        }

                        if (minOffset < 0.01f) {
                            // 当差值过小时可以近似认为当前不需要缩放,设置x和y坐标为0
                            mGeometryListener.onSetX(0);
                            mGeometryListener.onSetY(0);
                        } else {
                            float x = mGeometryListener.getHandWritingX();
                            float y = mGeometryListener.getHandWritingY();
                            // 得到当前缩放比和前一次缩放比差值的绝对值
                            float scaleAbs = Math.abs(mPreScale - curScale);
                            // 需特别注意除数为0的情况;分别求出x轴和y轴坐标偏移量
                            // 注意:当除数为一个非常小的非0数时,求出的值可能不是你想要的
                            float xOffset = scaleAbs * Math.abs(x) / minOffset;
                            float yOffset = scaleAbs * Math.abs(y) / minOffset;


                            if (curScale < mPreScale) { //缩小手写区
                                if (x > 0) {
                                    // 当前视图在左边区域缩小
                                    mGeometryListener.onSetX(getRevisedX(x - xOffset));
                                } else if (x < 0) {
                                    // 当前视图在右边区域缩小
                                    mGeometryListener.onSetX(getRevisedX(x + xOffset));
                                }

                                if (y > 0) {
                                    // 当前视图在上边区域缩小
                                    mGeometryListener.onSetY(getRevisedY(y - yOffset));
                                } else if (y < 0) {
                                    // 当前视图在下边区域缩小
                                    mGeometryListener.onSetY(getRevisedY(y + yOffset));
                                }
                            } else if (mPreScale < curScale) { //放大手写区
                                if (x > 0) {
                                    // 当前视图在左边区域放大
                                    mGeometryListener.onSetX(getRevisedX(x + xOffset));
                                } else if (x < 0) {
                                    // 当前视图在右边区域放大
                                    mGeometryListener.onSetX(getRevisedX(x - xOffset));
                                }

                                if (y > 0) {
                                    // 当前视图在上边区域放大
                                    mGeometryListener.onSetY(getRevisedY(y + yOffset));
                                } else if (y < 0) {
                                    // 当前视图在下边区域放大
                                    mGeometryListener.onSetY(getRevisedY(y - yOffset));
                                }
                            }
                        }

                        mGeometryListener.onScale(curScale);
                    }

                    mPreScale = curScale;
                    return false;
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector) {
                    if (DEBUG) {
                        if (mGeometryListener != null) {
                            Log.i(TAG, "getHandWritingX() = [" + mGeometryListener.getHandWritingX() +
                                    "], getHandWritingY() = [" + mGeometryListener.getHandWritingY() + "]");
                        }
                    }
                }

                @Override
                public void release() {
                    mParentScrollView = null;
                    mParentViewPager = null;
                }

                private void switchViewPagerInterceptTouchEvent(boolean disallowIntercept) {
                    if (mParentViewPager != null) {
                        mParentViewPager.requestDisallowInterceptTouchEvent(disallowIntercept);
                    } else {
                        disallowViewPagerInterceptTouchEvent(HandWritingView.this.getParent(), disallowIntercept);
                    }
                }

                private void disallowViewPagerInterceptTouchEvent(ViewParent parent, boolean isIntercept) {
                    if (parent != null) {
                        if (parent instanceof ViewPager) {
                            mParentViewPager = (ViewPager) parent;
                            parent.requestDisallowInterceptTouchEvent(isIntercept);
                        } else {
                            disallowViewPagerInterceptTouchEvent(parent.getParent(), isIntercept);
                        }
                    }
                }

                private void switchScrollViewInterceptTouchEvent(boolean disallowIntercept) {
                    if (mParentScrollView != null) {
                        mParentScrollView.requestDisallowInterceptTouchEvent(disallowIntercept);
                    } else {
                        disallowScrollViewInterceptTouchEvent(HandWritingView.this.getParent(), disallowIntercept);
                    }
                }

                private void disallowScrollViewInterceptTouchEvent(ViewParent parent, boolean isIntercept) {
                    if (parent != null) {
                        if (parent instanceof ScrollView) {
                            mParentScrollView = parent;
                            parent.requestDisallowInterceptTouchEvent(isIntercept);
                        } else {
                            disallowScrollViewInterceptTouchEvent(parent.getParent(), isIntercept);
                        }
                    }
                }
            };
        }

        if (mCustomGestureDetector == null) {
            mCustomGestureDetector = new CustomGestureDetector(getContext(), mOnGestureListener);
        }

        return mCustomGestureDetector.onTouchEvent(event);
    }

    /**
     * 处理手写控件触摸事件
     */
    private boolean handleTouchEvent(MotionEvent event) {
        float pressure = obtainPressure(event);
        // -------------------------开始工作
        int x = (int) event.getX();// 优化点，浮点转为整形
        int y = (int) event.getY();
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (DEBUG) {
                    Log.i(TAG, "mIsGeometryEditable = [" + mIsGeometryEditable + "]");
                }
                // 2017/9/19 0019/11:02 添加几何图形编辑状态判断  ------------modify by JiLin start
                if (mIsGeometryEditable) {
                    if (x < minX || x > maxX || y < minY || y > maxY) {
                        Toast.makeText(getContext(), "编辑状态下的几何图形尚未保存,请先保存!", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                // 2017/9/19 0019/11:02 添加几何图形编辑状态判断  ------------modify by JiLin end
                mIsDispatch = true;
                if (handleActionDown(pressure, x, y)) {
                    disableScrollImpl();
                    this.invalidate();
                } else {
                    return false;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mIsDispatch = false;
                enableScrollImpl();
                return false;
            case MotionEvent.ACTION_MOVE:
                if (mIsDispatch) {
                    handleActionMove(pressure, x, y);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                mIsDispatch = true;
                handleActionUp(pressure, x, y);
                enableScrollImpl();
                this.invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsDispatch = true;
                handleActionCancel(x, y);
                disableScrollImpl();
                this.invalidate();
                break;
            case 211:
                handleAction211(x, y);
                this.invalidate();
                break;
            case 213:
                handleAction213(x, y);
                this.invalidate();
                break;
            case 212:
                handleAction212(x, y);
                this.invalidate();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 算出压力，根据压力值来调节笔记宽度
     */
    private float obtainPressure(MotionEvent event) {
        float pressure = Math.round(lerp(lastPressure, event.getPressure(), 0.15f) * 100) / 100.0f;
        if (DEBUG) {
            Log.i(TAG, "lastPressure_" + lastPressure + ", pressure_" + pressure);
        }

        // 解决某些设备获取event.getPressure()时得到一个野值导致的异常状况
        if (pressure > 0.9f) {
            pressure = 0.9f;
        }

        lastPressure = pressure;
        return pressure;
    }

    private void disableScrollImpl() {
        ViewParent parent = getParent();
        if (parent != null) {
            //请求父类不要中断事件
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private void enableScrollImpl() {
        ViewParent parent = getParent();
        if (parent != null) {
            //请求父类中断事件
            parent.requestDisallowInterceptTouchEvent(false);
        }
    }

    private boolean handleActionDown(float pressure, int x, int y) {
        if (DEBUG) {
            Log.i(TAG, "handleActionDown:" + x + "_" + y);
        }

        if (!mIsRubber && mStrokesThreshold > 0 && getStrokes().length() >= mStrokesThreshold) {
            Toast.makeText(getContext(), "笔迹已达上限!!!", Toast.LENGTH_SHORT).show();
            return false;
        }

        recordStart(x, y, pressure);
        touchStart(x, y, pressure);
        mIsStrokesChanged = true;

        return true;
    }

    private void handleActionMove(float pressure, int x, int y) {
        if (DEBUG) {
            Log.i(TAG, "handleActionMove:" + x + "_" + y + "; old:" + oldX + "_" + oldY);
        }

        //---------------------校验非法的坐标值---------------------s
        if (isGeometryType()) {
            // 处理当前无效的几何图形编辑逻辑;
            // 2018/5/11 0011/16:47 操作逻辑优化：超出手写区域时不取消几何图形  s------------modify by JiLin
            x = reviseGeometryX(x);
            y = reviseGeometryY(y);
            // 2018/5/11 0011/16:47 操作逻辑优化：超出手写区域时不取消几何图形  e------------modify by JiLin
        } else {
            // 处理基本线型超出手写view的范围时逻辑处理
            if (y > mHeight) {
                if (mAtomicInteger.get() == 0) {
                    //
                    recordUp(x, mHeight, pressure);
                    touchUp(x, mHeight, pressure);
                    mAtomicInteger.getAndIncrement();
                }
                return;
            }
        }
        // 处理基本线型从view外围重新进入view时的逻辑处理
        if (mAtomicInteger.get() > 0) {
            recordStart(x, mHeight, pressure);
            touchStart(x, mHeight, pressure);
            mAtomicInteger.set(0);
        }
        //---------------------校验非法的坐标值---------------------e

        if (oldX == -1 || oldY == -1) {
            // 处理down事件丢失时的逻辑处理
            recordStart(x, y, pressure);
            touchStart(x, y, pressure);
            mIsStrokesChanged = true;
        } else {
            int dx = Math.abs(x - oldX);
            int dy = Math.abs(y - oldY);
            // 优化，增加微距移动点的判断
            if (dx > validAbs || dy > validAbs) {
                recordMove(x, y, pressure);
                touchMove(x, y, pressure);
                mIsStrokesChanged = true;
            }
        }
    }

    private void handleActionUp(float pressure, int x, int y) {
        if (DEBUG) {
            Log.i(TAG, "handleActionUp:" + x + "_" + y);
        }

        //---------------------校验非法的坐标值---------------------s
        if (isGeometryType()) {
            // 处理当前无效的几何图形编辑逻辑;
            // 2018/5/11 0011/16:47 操作逻辑优化：超出手写区域时不取消几何图形  s------------modify by JiLin
            x = reviseGeometryX(x);
            y = reviseGeometryY(y);
            // 2018/5/11 0011/16:47 操作逻辑优化：超出手写区域时不取消几何图形  e------------modify by JiLin
        } else {
            // 当在view手写区域的外部抬起时,重置状态
            if (mAtomicInteger.get() > 0) {
                mAtomicInteger.set(0);
                oldX = oldY = -1;
                return;
            }
        }
        //---------------------校验非法的坐标值---------------------e

        recordUp(x, y, pressure);
        touchUp(x, y, pressure);
    }

    /**
     * 修正几何图形x轴无效坐标
     */
    private int reviseGeometryX(int x) {
        if (mGeometryListener != null) {
            int dragPointRadius = mGeometryListener.getDragPointRadius();
            if (x < dragPointRadius) {
                return dragPointRadius;
            }

            int tempX = mWidth - dragPointRadius;
            if (x > tempX) {
                return tempX;
            }
        }
        return x;
    }

    /**
     * 修正几何图形y轴无效坐标
     */
    private int reviseGeometryY(int y) {
        if (mGeometryListener != null) {
            int limitTop = mGeometryListener.getLimitTop();
            if (y < limitTop) {
                return limitTop;
            }

            int tempY = mHeight - mGeometryListener.getDragPointRadius();
            if (y > tempY) {
                return tempY;
            }
        }
        return y;
    }

    private void handleActionCancel(int x, int y) {
        if (DEBUG) {
            Log.i(TAG, "handleActionCancel:" + x + "_" + y + "; old:" + oldX + "_" + oldY);
        }

        oldX = oldY = -1;// 重置状态

        // 当擦除状态标记被临时修改时,重置回原来的手写状态.解决手写笔上按钮按下的擦除事件在书写区域外抬起时,手写状态没有重置的问题
        if (mIsTempChangeRubber) {
            setToWriting();
        }
    }

    /**
     * 手写笔上按钮按下时down事件,只会调用一次
     */
    private void handleAction211(int x, int y) {
        if (DEBUG) {
            Log.i(TAG, "handleAction211:" + x + "_" + y);
        }

        // 回调事件给父View处理相关逻辑
        if (mGeometryListener != null) {
            mGeometryListener.handleEditableGeometry();
        }

        if (!mIsRubber) { // 当前为非擦除状态时,临时设置为擦除状态
            setToRubber();
            mIsTempChangeRubber = true;
        } else {
            mIsTempChangeRubber = false;
        }
        isNeedRedraw = false;
        deleteRecord(x, y);
        oldX = endX = x;
        oldY = endY = y;
    }

    /**
     * 手写笔上按钮按下时move事件,当笔在屏幕上按下和移动时会重复不断地调用
     */
    private void handleAction213(int x, int y) {
        if (DEBUG) {
            Log.i(TAG, "handleAction213:" + x + "_" + y);
        }
        int distanceX = Math.abs(x - oldX);
        int distanceY = Math.abs(y - oldY);
        // 优化，增加微距移动点的判断
        if (distanceX > validAbs || distanceY > validAbs) {
            deleteRecord(x, y);
            oldX = x;
            oldY = y;
            invalidate();
        }
    }

    /**
     * 手写笔上按钮按下时up事件,当笔抬离屏幕时调用一次
     */
    private void handleAction212(int x, int y) {
        if (DEBUG) {
            Log.i(TAG, "handleAction212:" + x + "_" + y);
        }

        endX = x;
        endY = y;
        oldX = oldY = -1;
        if (mIsTempChangeRubber) { // 当擦除状态标记被临时修改时,重置回原来的手写状态
            setToWriting();
        }
    }

    /**
     * 当触摸坐标超过手写view范围时，返回true；表示为无效的触摸坐标
     */
    public boolean isInvalid(int x, int y) {
        return x <= 0 || y <= 0 || x >= mWidth || y >= mHeight;
    }

    // -------------------------for touch event start-----------------------------------

    private void recordStart(int x, int y, float pressure) {
        if (mIsRubber) {
            isNeedRedraw = false;
            deleteRecord(x, y);
        } else {
            minX = x;
            minY = y;
            maxX = x;
            maxY = y;
            mStrokeWidth = Math.max(mStrokeWidth, x);
            mStrokeHeight = Math.max(mStrokeHeight, y);
            if (isGeometryType()) { //几何图形线型
                recordGeometryDown();
            } else { //基本线型
                if (TextUtils.isEmpty(mStrokes.toString())) {// 第一个
                    mStrokes.append(mDrawType.getCode()).append("#").append(mPaint.getColor())
                            .append("#").append(x).append(",").append(y).append(",").append(pressure);
                } else {
                    mStrokes.append("=").append(mDrawType.getCode()).append("#").append(mPaint.getColor())
                            .append("#").append(x).append(",").append(y).append(",").append(pressure);
                }
            }
        }
    }

    private void touchStart(int x, int y, float pressure) {
        lastPressure = pressure;
        oldX = endX = x;
        oldY = endY = y;

        switch (mDrawType) {
            case CURVE:// 曲线
            case DASH:// 点曲线
                mPath.reset();
                mPath.moveTo(x, y);
                break;
        }
    }

    private void recordMove(int x, int y, float pressure) {
        if (mIsRubber) { //橡皮状态
            deleteRecord(x, y);
        } else {
            switch (mDrawType) {
                case CURVE: //曲线
                case DASH: //点曲线
                    updateRange(x, y);
                    mStrokes.append(";").append(x).append(",").append(y).append(",").append(pressure);
                    break;
            }
        }
    }

    private void touchMove(int x, int y, float pressure) {
        if (mIsRubber) {
            oldX = x;
            oldY = y;
            postInvalidate();
        } else {
            switch (mDrawType) {
                case CURVE:// 曲线
                case DASH:// 点曲线
                    drawMovePath(pressure);
                    mPath.quadTo(oldX, oldY, (x + oldX) / 2, (y + oldY) / 2);
                    oldX = x;
                    oldY = y;
                    break;
                default:
                    endX = x;
                    endY = y;
                    postInvalidate();
                    break;
            }
        }
    }

    private void recordUp(int x, int y, float pressure) {
        if (mIsRubber) {
            deleteRecord(x, y);
            // 如果有线条被删除，在抬起画笔的时候重绘整个笔记，更新strokes的值
            if (isNeedRedraw) {
                if (mHandWritingCanvas != null) {
                    mHandWritingCanvas.clearCanvas();
                }
                restorePoints();
                isNeedRedraw = false;
            }
        } else {
            updateRange(x, y);
            if (isGeometryType()) { //几何图形线型
                recordGeometryUp();
            } else { //基本线型
                mStrokes.append(";").append(x).append(",").append(y).append(",").append(pressure);
                mStrokes.append("#").append(minX).append(",").append(minY).append(",").append(maxX).append(",").append(maxY);
            }
        }
    }

    private void touchUp(int x, int y, float pressure) {
        endX = x;
        endY = y;

        if (!mIsRubber) { //非皮擦状态
            if (isGeometryType()) { // 几何图形
                if (mIsGeometryEditable && mGeometryListener != null) { // 几何图形编辑状态
                    mGeometryListener.onShowEditGeometry(mGeometryTempInfo, mGeometryPaint, mDrawType);
                } else { // 非编辑状态的几何图形直接画出来
                    drawGeometryBitmap(mDrawType);
                }
            } else { // 非几何图形
                switch (mDrawType) {
                    case CURVE:// 曲线
                    case DASH:// 点曲线
                        mPath.quadTo(oldX, oldY, (x + oldX) / 2, (y + oldY) / 2);
                        mPath.quadTo((x + oldX) / 2, (y + oldY) / 2, x, y);
                        drawMovePath(pressure);
                        lastLength = 0;
                        mPath.reset();
                        break;
                    case LINE:// 直线
                    case DASH_LINE:// 点直线
                        drawLine(null, oldX, oldY, x, y);
                        break;
                    case ARROW:// 箭头
                        drawAL(null, oldX, oldY, x, y);
                        break;
                }
            }
        }

        oldX = oldY = -1;// 重置状态
    }

    // -------------------------for touch event end-----------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        if (DEBUG) {
            Log.i(TAG, "onDraw()");
        }
        // 设置画布背景为透明,画图片
        mPaint.setXfermode(null);
        if (mHandWritingCanvas != null) {
            mHandWritingCanvas.drawCanvas(canvas, mBitmapPaint);
        }
        if (oldX != -1) {// 我们借用oldX是否等于－1来判定是否画线过程中，另外：old在画直线中，代表起始位置，在曲线中及擦除中代表刚刚走过的位置
            // 画 书写过程
            if (!mIsRubber) {// 书写时
                switch (mDrawType) {
                    case CURVE:// 曲线
                    case DASH:// 点曲线
                        //empty
                        break;
                    case LINE:// 直线
                    case DASH_LINE:// 点直线
                        drawLine(canvas, oldX, oldY, endX, endY);
                        break;
                    case ARROW:// 箭头
                        drawAL(canvas, oldX, oldY, endX, endY);
                        break;
                    case TRIANGLE:// 三角形
                        drawTriangle(canvas, oldX, oldY, endX, endY);
                        break;
                    case RECTANGLE: //矩形
                        drawRectangle(canvas, oldX, oldY, endX, endY);
                        break;
                    case TRAPEZIUM: //梯形
                        drawTrapezium(canvas, oldX, oldY, endX, endY);
                        break;
                    case OVAL: //椭圆
                        drawOval(canvas, oldX, oldY, endX, endY);
                        break;
                    case COORDINATE: //坐标系
                        drawCoordinate(canvas, oldX, oldY, endX, endY);
                        break;
                    case NUMBER_AXIS: //数轴
                        drawNumberAxis(canvas, oldX, oldY, endX, endY);
                        break;
                }
            } else {
                // 让小橡皮随笔移动而出现
                canvas.drawBitmap(mRubber, oldX - mRubber.getWidth() / 2,
                        oldY - mRubber.getHeight() / 2, mRubberPaint);
            }
        }
    }

    /**
     * 绘制直线
     */
    private void drawLine(Canvas canvas, float fromX, float fromY, float toX, float toY) {
        linePath.reset();
        linePath.moveTo(fromX, fromY);
        linePath.lineTo(toX, toY);
        linePath.close();
        if (canvas == null) {
            if (mHandWritingCanvas != null) {
                mHandWritingCanvas.drawPath(linePath, mPaint, DEFAULT_PAINT_SIZE);
            }
            return;
        }
        canvas.drawPath(linePath, mPaint);
    }

    /**
     * 画箭头
     */
    private void drawAL(Canvas canvas, float sx, float sy, float ex, float ey) {
        Path path = obtainALPath(null, sx, sy, ex, ey, 16f, 7f);
        if (canvas == null) {
            if (mHandWritingCanvas != null) {
                mHandWritingCanvas.drawPath(path, mPaint, DEFAULT_PAINT_SIZE);
            }
        } else {
            canvas.drawPath(path, mPaint);
        }
    }

    //------------------------------画几何图形 start-----------------------------------

    /**
     * 把几何图形真实地画在bitmap中
     */
    private void drawGeometryBitmap(DrawType drawType) {
        switch (drawType) {
            case TRIANGLE:// 三角形
            case TRAPEZIUM: //梯形
                if (mGeometryTempInfo != null) {
                    drawTriangleOrTrapezium(mGeometryTempInfo.pointsList);
                }
                break;
            case RECTANGLE: //矩形
                drawRectangle(mGeometryTempInfo);
                break;
            case OVAL: //椭圆
                drawOval(mGeometryTempInfo);
                break;
            case COORDINATE: //坐标系
                drawCoordinate(mGeometryTempInfo);
                break;
            case NUMBER_AXIS: //数轴
                drawNumberAxis(mGeometryTempInfo);
                break;
            default:
                break;
        }
    }

    /**
     * 首次画三角形时在move事件中不断绘制
     */
    private void drawTriangle(@NonNull Canvas canvas, float fromX, float fromY, float toX, float toY) {
        initGeometryPath();

        mGeometryPath.moveTo(fromX, fromY);
        mGeometryPath.lineTo(fromX, toY);
        mGeometryPath.lineTo(toX, toY);
        mGeometryPath.close();
        canvas.drawPath(mGeometryPath, mGeometryPaint);
    }

    /**
     * 把三角形或者梯形真实地画在bitmap中
     */
    private void drawTriangleOrTrapezium(List<PointInfo> pointInfoList) {
        if (mHandWritingCanvas == null || pointInfoList == null || pointInfoList.size() == 0) {
            return;
        }

        initGeometryPath();

        for (int i = 0, pointsListSize = pointInfoList.size(); i < pointsListSize; i++) {
            PointInfo pointInfo = pointInfoList.get(i);
            if (i == 0) {
                mGeometryPath.moveTo(pointInfo.x, pointInfo.y);
            } else {
                mGeometryPath.lineTo(pointInfo.x, pointInfo.y);
            }
        }
        mGeometryPath.close();

        mHandWritingCanvas.drawPath(mGeometryPath, mGeometryPaint, mGeometryPaint.getStrokeWidth());
    }

    /**
     * 首次画矩形时在move事件中不断绘制
     */
    private void drawRectangle(@NonNull Canvas canvas, int oldX, int oldY, int endX, int endY) {
        initGeometryPath();
        //Path.Direction.CW顺时针方向 Path.Direction.CCW逆时针方向
        RectF rectF = new RectF(Math.min(oldX, endX), Math.min(oldY, endY),
                Math.max(oldX, endX), Math.max(oldY, endY));
        mGeometryPath.addRect(rectF, Path.Direction.CW);
        canvas.drawPath(mGeometryPath, mGeometryPaint);
    }

    /**
     * 把矩形真实地画在bitmap中
     */
    private void drawRectangle(PathInfo pathInfo) {
        if (mHandWritingCanvas == null || pathInfo == null) {
            return;
        }

        initGeometryPath();
        //Path.Direction.CW顺时针方向 Path.Direction.CCW逆时针方向
        RectF rectF = new RectF(pathInfo.left, pathInfo.top, pathInfo.right, pathInfo.bottom);
        mGeometryPath.addRect(rectF, Path.Direction.CW);
        mHandWritingCanvas.drawPath(mGeometryPath, mGeometryPaint, mGeometryPaint.getStrokeWidth());
    }

    /**
     * 首次画梯形时在move事件中不断绘制
     */
    private void drawTrapezium(@NonNull Canvas canvas, int oldX, int oldY, int endX, int endY) {
        int minX = Math.min(oldX, endX);
        int minY = Math.min(oldY, endY);
        int maxX = Math.max(oldX, endX);
        int maxY = Math.max(oldY, endY);
        int offsetX = (maxX - minX) / 4;
        initGeometryPath();

        mGeometryPath.moveTo(minX + offsetX, minY);
        mGeometryPath.lineTo(maxX - offsetX, minY);
        mGeometryPath.lineTo(maxX, maxY);
        mGeometryPath.lineTo(minX, maxY);
        mGeometryPath.close();

        canvas.drawPath(mGeometryPath, mGeometryPaint);
    }

    /**
     * 首次画椭圆时在move事件中不断绘制
     */
    private void drawOval(@NonNull Canvas canvas, int oldX, int oldY, int endX, int endY) {
        initGeometryPath();
        //Path.Direction.CW顺时针方向 Path.Direction.CCW逆时针方向
        RectF rectF = new RectF(oldX, oldY, endX, endY);
        mGeometryPath.addOval(rectF, Path.Direction.CW); //使用path画出椭圆
        canvas.drawPath(mGeometryPath, mGeometryPaint);
    }

    /**
     * 把椭圆真实地画在bitmap中
     */
    private void drawOval(PathInfo pathInfo) {
        if (mHandWritingCanvas == null || pathInfo == null) {
            return;
        }

        initGeometryPath();

        //Path.Direction.CW顺时针方向 Path.Direction.CCW逆时针方向
        RectF rectF = new RectF(pathInfo.left, pathInfo.top, pathInfo.right, pathInfo.bottom);
        mGeometryPath.addOval(rectF, Path.Direction.CW); //使用path画出椭圆

        mHandWritingCanvas.drawPath(mGeometryPath, mGeometryPaint, mGeometryPaint.getStrokeWidth());
    }

    /**
     * 首次画坐标系时在move事件中不断绘制
     */
    private void drawCoordinate(@NonNull Canvas canvas, int oldX, int oldY, int endX, int endY) {
        int minX = Math.min(oldX, endX);
        int maxX = Math.max(oldX, endX);
        int minY = Math.min(oldY, endY);
        int maxY = Math.max(oldY, endY);
        obtainCoordinatePath(mGeometryPath, minX, minY, maxX, maxY, (minX + maxX) / 2, (minY + maxY) / 2,
                AXIS_ARROW_HEIGHT, AXIS_OTHER_LENGTH);

        canvas.drawPath(mGeometryPath, mGeometryPaint);
    }

    /**
     * 把坐标系真实地画在bitmap中
     */
    private void drawCoordinate(PathInfo pathInfo) {
        if (mHandWritingCanvas == null || pathInfo == null) {
            return;
        }
        List<PointInfo> pointsList = pathInfo.pointsList;
        if (pointsList == null || pointsList.size() != 5) {
            return;
        }

        // 得到坐标系中心点坐标信息
        PointInfo pointInfo = pointsList.get(4);
        obtainCoordinatePath(mGeometryPath, pathInfo.left, pathInfo.top, pathInfo.right, pathInfo.bottom,
                pointInfo.x, pointInfo.y, AXIS_ARROW_HEIGHT, AXIS_OTHER_LENGTH);

        Paint.Style tempStyle = mGeometryPaint.getStyle();
        if (tempStyle != Paint.Style.FILL_AND_STROKE) {
            mGeometryPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        // 画坐标系
        mHandWritingCanvas.drawPath(mGeometryPath, mGeometryPaint, mGeometryPaint.getStrokeWidth());
        // 还原笔迹样式
        mGeometryPaint.setStyle(tempStyle);
    }

    /**
     * 首次画数轴时在move事件中不断绘制
     */
    private void drawNumberAxis(@NonNull Canvas canvas, int oldX, int oldY, int endX, int endY) {
        int minX = Math.min(oldX, endX);
        int maxX = Math.max(oldX, endX);
        int minY = Math.min(oldY, endY);
        int maxY = Math.max(oldY, endY);
        obtainNumberAxisPath(mGeometryPath, minX, maxX, (minX + maxX) / 2, (minY + maxY) / 2,
                AXIS_ARROW_HEIGHT, AXIS_OTHER_LENGTH);

        canvas.drawPath(mGeometryPath, mGeometryPaint);
    }

    /**
     * 把数轴真实地画在bitmap中
     */
    private void drawNumberAxis(PathInfo pathInfo) {
        if (mHandWritingCanvas == null || pathInfo == null) {
            return;
        }

        if (pathInfo.pointsList == null || pathInfo.pointsList.size() != 3) {
            return;
        }

        // 得到原点坐标信息
        PointInfo originInfo = pathInfo.pointsList.get(2);
        obtainNumberAxisPath(mGeometryPath, pathInfo.left, pathInfo.right,
                originInfo.x, originInfo.y, AXIS_ARROW_HEIGHT, AXIS_OTHER_LENGTH);

        Paint.Style tempStyle = mGeometryPaint.getStyle();
        if (tempStyle != Paint.Style.FILL_AND_STROKE) {
            mGeometryPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        // 画数轴
        mHandWritingCanvas.drawPath(mGeometryPath, mGeometryPaint, mGeometryPaint.getStrokeWidth());
        // 还原笔迹样式
        mGeometryPaint.setStyle(tempStyle);
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
    //------------------------------画几何图形 end-----------------------------------

    private void drawMovePath(float pressure) {
        mTilePath.reset();
        float penSize;

        if (pressure <= 0) {
            penSize = DEFAULT_PAINT_SIZE;
        } else {
            penSize = MIN_PEN_SIZE + pressure * mPaintSizeThreshold;
        }

        drawTilePath(penSize);
        lastPressure = pressure;
    }

    /**
     * 分段绘制path，实现了按压感变化笔记宽度
     */
    private float drawTilePath(float penSize) {
        mPathMeasure.setPath(mPath, false);
        float length = mPathMeasure.getLength();
        mPathMeasure.getSegment(lastLength, length, mTilePath, true);
        if (mHandWritingCanvas != null) {
            mHandWritingCanvas.drawPath(mTilePath, mPaint, penSize);
        }
        lastLength = length;
        return length;
    }

    // 更新正在画的线条的范围
    private void updateRange(int x, int y) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);

        mStrokeWidth = Math.max(mStrokeWidth, x);
        mStrokeHeight = Math.max(mStrokeHeight, y);
    }

    // -------------------------for record

    public void updateRange(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;

        mStrokeWidth = Math.max(mStrokeWidth, maxX);
        mStrokeHeight = Math.max(mStrokeHeight, maxY);
    }

    /**
     * 获取坐标系path信息
     *
     * @param path        path信息
     * @param minX        开始x轴坐标
     * @param maxX        结束x轴坐标
     * @param originX     原点x轴坐标
     * @param originY     原点y轴坐标
     * @param arrowHeight 箭头高度
     * @param arrowHalf   箭头底边的一半
     */
    public Path obtainCoordinatePath(@NonNull Path path, int minX, int minY, int maxX, int maxY,
                                     int originX, int originY, float arrowHeight, float arrowHalf) {
        int unit = mAxisUnit;

        path.reset();
        // 获取x轴方向数轴path信息
        path = obtainALPath(path, minX, originY, maxX, originY, arrowHeight, arrowHalf);
        // 获取y轴方向数轴path信息
        obtainALPath(path, originX, maxY, originX, minY, arrowHeight, arrowHalf);

        // 封装坐标系原点
        path.addCircle(originX, originY, arrowHalf, Path.Direction.CCW);

        // 封装x轴正方向
        int halfAxisX = maxX - originX;
        int halfCount = halfAxisX / unit;
        for (int i = 0; i < halfCount; i++) {
            int offset = i + 1;
            int positiveX = originX + offset * unit;
            path.moveTo(positiveX, originY - arrowHalf);
            path.lineTo(positiveX, originY);
        }

        // 封装x轴负方向
        halfAxisX = originX - minX;
        halfCount = halfAxisX / unit;
        for (int i = 0; i < halfCount; i++) {
            int offset = i + 1;
            int minusX = originX - offset * unit;
            path.moveTo(minusX, originY - arrowHalf);
            path.lineTo(minusX, originY);
        }

        // 封装y轴正方向
        int halfAxisY = originY - minY;
        halfCount = halfAxisY / unit;
        for (int i = 0; i < halfCount; i++) {
            int offset = i + 1;
            int positiveY = originY - offset * unit;
            path.moveTo(originX + arrowHalf, positiveY);
            path.lineTo(originX, positiveY);
        }

        // 封装y轴负方向
        halfAxisY = maxY - originY;
        halfCount = halfAxisY / unit;
        for (int i = 0; i < halfCount; i++) {
            int offset = i + 1;
            int minusY = originY + offset * unit;
            path.moveTo(originX + arrowHalf, minusY);
            path.lineTo(originX, minusY);
        }

        return path;
    }

    /**
     * 获取数轴path信息
     *
     * @param path        path信息
     * @param minX        开始x轴坐标
     * @param maxX        结束x轴坐标
     * @param originX     原点x轴坐标
     * @param originY     原点y轴坐标
     * @param arrowHeight 箭头高度
     * @param arrowHalf   箭头底边的一半
     */
    public Path obtainNumberAxisPath(@NonNull Path path, int minX, int maxX, int originX, int originY,
                                     float arrowHeight, float arrowHalf) {
        int unit = mAxisUnit;

        path.reset();
        // 获取x轴方向数轴path信息
        obtainALPath(path, minX, originY, maxX, originY, arrowHeight, arrowHalf);

        // 封装数轴原点
        path.addCircle(originX, originY, arrowHalf, Path.Direction.CCW);

        // 封装x轴正方向
        int halfAxisX = maxX - originX;
        int halfCount = halfAxisX / unit;
        for (int i = 0; i < halfCount; i++) {
            int offset = i + 1;
            int positiveX = originX + offset * unit;
            path.moveTo(positiveX, originY - arrowHalf);
            path.lineTo(positiveX, originY);
        }

        // 封装x轴负方向
        halfAxisX = originX - minX;
        halfCount = halfAxisX / unit;
        for (int i = 0; i < halfCount; i++) {
            int offset = i + 1;
            int minusX = originX - offset * unit;
            path.moveTo(minusX, originY - arrowHalf);
            path.lineTo(minusX, originY);
        }

        return path;
    }

    /**
     * 获取箭头path信息
     *
     * @param sx          开始x轴坐标
     * @param sy          开始y轴坐标
     * @param ex          结束x轴坐标
     * @param ey          结束y轴坐标
     * @param arrowHeight 箭头高度
     * @param arrowHalf   箭头底边的一半
     */
    private Path obtainALPath(Path path, float sx, float sy, float ex, float ey,
                              float arrowHeight, float arrowHalf) {
        // 箭头角度
        double arrowAngle = Math.atan(arrowHalf / arrowHeight);
        // 箭头的长度
        double arrowLen = Math.sqrt(arrowHalf * arrowHalf + arrowHeight * arrowHeight);

        double[] arrXY1 = rotateVec(ex - sx, ey - sy, arrowAngle, true, arrowLen);
        double[] arrXY2 = rotateVec(ex - sx, ey - sy, -arrowAngle, true, arrowLen);
        // (x3,y3)是第一端点;(x4,y4)是第二端点
        int x3 = (int) (ex - arrXY1[0]);
        int y3 = (int) (ey - arrXY1[1]);
        int x4 = (int) (ex - arrXY2[0]);
        int y4 = (int) (ey - arrXY2[1]);

        if (path == null) {
            path = new Path();
        }

        // 判断 防止有从0点开始
        if (x3 == 0 && y3 == 0 && x4 == 0 && y4 == 0) {
            return path;
        }

        // 封装箭头path信息
        path.moveTo(ex, ey);
        path.lineTo(x3, y3);
        path.lineTo(x4, y4);
        path.close();
        // 封装线段path信息
        path.moveTo(sx, sy);
        path.lineTo(ex, ey);

        return path;
    }

    //---------------------------几何图形编辑状态回调方法 start------------------------------------------

    /**
     * 当取消保存当前可编辑的几何图形时回调该方法
     */
    public boolean onCancelEditView() {
        if (mIsGeometryEditable) {
            mIsGeometryEditable = false;
            return true;
        }
        mGeometryTempInfo = null;
        return false;
    }

    /**
     * 当保存当前可编辑的几何图形时回调该方法
     *
     * @param drawType 当前编辑状态下的几何图形类型
     */
    public boolean onSaveEditView(DrawType drawType) {
        if (mIsGeometryEditable) {
            mIsGeometryEditable = false;
            updateGeometryPathInfo(); //更新几何图形笔迹信息
            appendGeometryStrokes(); //拼接几何图形笔迹
            drawGeometryBitmap(drawType); //真实地画出几何图形
            mGeometryTempInfo = null;
            mIsStrokesChanged = true;
            return true;
        }

        return false;
    }
    //---------------------------几何图形编辑状态回调方法 end--------------------------------------------

    //---------------------------for geometry record start------------------------------------------

    /**
     * 记录画几何图形时down事件信息
     */
    private void recordGeometryDown() {
        mGeometryTempInfo = new PathInfo();
    }

    /**
     * 记录画几何图形时up事件信息
     */
    private void recordGeometryUp() {
        // 判断几何图形编辑状态是否有效(矩形中的左上右下点距离如果小于拖拽点的直径则为无效的几何图形)
        if (mGeometryListener != null && mGeometryListener.isGeometryInvalid(mDrawType, minX, minY, maxX, maxY)) {
            if (DEBUG) {
                Log.i(TAG, "invalid Geometry!!!");
            }
            mIsGeometryEditable = false;
            mGeometryTempInfo = null;
            return;
        }

        mGeometryTempInfo.color = mGeometryPaint.getColor();
        mGeometryTempInfo.drawType = mDrawType.getCode();

        switch (mDrawType) {
            case TRIANGLE: //三角形
                mGeometryTempInfo.pointsList = recordGeometryTriangle();
                break;
            case OVAL: //椭圆
            case RECTANGLE: //矩形
                mGeometryTempInfo.pointsList = recordGeometryOvalOrRec();
                break;
            case TRAPEZIUM: //梯形
                mGeometryTempInfo.pointsList = recordGeometryTrapezium();
                break;
            case COORDINATE: //坐标系
                mGeometryTempInfo.pointsList = recordGeometryCoordinate();
                break;
            case NUMBER_AXIS: //数轴
                mGeometryTempInfo.pointsList = recordGeometryNumberAxis();
                break;
            default:
                break;
        }

        mGeometryTempInfo.left = minX;
        mGeometryTempInfo.top = minY;
        mGeometryTempInfo.right = maxX;
        mGeometryTempInfo.bottom = maxY;

        updateGeometryPathInfo();

        if (mGeometryListener != null) {
            //置为几何图形编辑状态
            mIsGeometryEditable = true;
        } else {
            //重置状态;如果几何图形监听接口为null时,此时画出的几何图形没有编辑状态,直接添加
            mIsGeometryEditable = false;
            appendGeometryStrokes();
        }
    }

    /**
     * 拼接几何图形笔迹字符串
     */
    private void appendGeometryStrokes() {
        if (mGeometryTempInfo == null) {
            return;
        }
        if (!TextUtils.isEmpty(mStrokes.toString())) { //笔迹不为空时拼接连接符
            mStrokes.append("=");
        }
        mStrokes.append(mGeometryTempInfo.path); //拼接几何图形笔迹字符串
    }

    /**
     * 记录三角形中坐标点信息
     */
    private List<PointInfo> recordGeometryTriangle() {
        List<PointInfo> pointsList = new ArrayList<>(3);
        pointsList.add(obtainGeometryPoint(oldX, oldY));// 三角形第一次按下时的坐标点
        pointsList.add(obtainGeometryPoint(oldX, endY));// 三角形第二个计算出的坐标点
        pointsList.add(obtainGeometryPoint(endX, endY));// 三角形抬起时的坐标点
        return pointsList;
    }

    /**
     * 记录椭圆或矩形中坐标点信息
     */
    private List<PointInfo> recordGeometryOvalOrRec() {
        List<PointInfo> pointsList = new ArrayList<>(4);
        pointsList.add(obtainGeometryPoint(minX, minY, 0));
        pointsList.add(obtainGeometryPoint(maxX, minY, 1));
        pointsList.add(obtainGeometryPoint(maxX, maxY, 2));
        pointsList.add(obtainGeometryPoint(minX, maxY, 3));
        return pointsList;
    }

    /**
     * 记录梯形中坐标点信息
     */
    private List<PointInfo> recordGeometryTrapezium() {
        List<PointInfo> pointsList = new ArrayList<>(4);
        int offsetX = (maxX - minX) / 4;
        pointsList.add(obtainGeometryPoint(minX + offsetX, minY, 0));
        pointsList.add(obtainGeometryPoint(maxX - offsetX, minY, 1));
        pointsList.add(obtainGeometryPoint(maxX, maxY, 2));
        pointsList.add(obtainGeometryPoint(minX, maxY, 3));
        return pointsList;
    }

    /**
     * 记录坐标系中坐标点信息
     */
    private List<PointInfo> recordGeometryCoordinate() {
        List<PointInfo> pointsList = new ArrayList<>(5);

        // 2018/5/11 0011/14:34 优化坐标系操作逻辑：当有任意一条轴的起始跟结束点距离小于拖拽点直径时，按照最小标准展示  ------------modify by JiLin
        if (mGeometryListener != null) {
            int dragPointDiameter = mGeometryListener.getDragPointRadius() * 2;
            int disX;
            if ((disX = maxX - minX) < dragPointDiameter) {
                int offsetX = Math.round((dragPointDiameter - disX) * 1.0f / 2);
                minX -= offsetX;
                maxX += offsetX;
            }
            int disY;
            if ((disY = maxY - minY) < dragPointDiameter) {
                int offsetY = Math.round((dragPointDiameter - disY) * 1.0f / 2);
                minY -= offsetY;
                maxY += offsetY;
            }
        }
        // 2018/5/11 0011/14:34 优化坐标系操作逻辑：当有任意一条轴的起始跟结束点距离小于拖拽点直径时，按照最小标准展示  ------------modify by JiLin

        // 得到中心点坐标
        int midX = (minX + maxX) / 2;
        int midY = (minY + maxY) / 2;
        // 坐标系中的坐标点索引和椭圆保持一致(左上角从0开始顺时针计算)
        pointsList.add(obtainGeometryPoint(midX, minY, 0));
        pointsList.add(obtainGeometryPoint(maxX, midY, 1));
        pointsList.add(obtainGeometryPoint(midX, maxY, 2));
        pointsList.add(obtainGeometryPoint(minX, midY, 3));
        // 坐标系中心点坐标索引为-1,不需要拖拽控制
        pointsList.add(obtainGeometryPoint(midX, midY, -1));

        return pointsList;
    }

    /**
     * 记录数轴中坐标信息点
     */
    private List<PointInfo> recordGeometryNumberAxis() {
        List<PointInfo> pointsList = new ArrayList<>(3);
        // 得到中心点坐标
        int midX = (minX + maxX) / 2;
        int midY = (minY + maxY) / 2;
        // 坐标点索引和椭圆保持一致(左上角从0开始顺时针计算)
        pointsList.add(obtainGeometryPoint(maxX, midY, 1));
        pointsList.add(obtainGeometryPoint(minX, midY, 3));
        // 数轴中心点坐标索引为-1,不需要拖拽控制
        pointsList.add(obtainGeometryPoint(midX, midY, -1));

        // 特别重要: 因为数轴只允许改变X方法的距离,所以需要更新数轴所在矩形范围的上下Y轴信息,避免在擦除的时候影响其他线型
        minY = midY - AXIS_OTHER_LENGTH;
        maxY = midY + AXIS_OTHER_LENGTH;

        return pointsList;
    }

    private void updateGeometryPathInfo() {
        if (mGeometryTempInfo == null || mGeometryTempInfo.pointsList == null || mGeometryTempInfo.pointsList.size() == 0) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(mGeometryTempInfo.drawType).append("#").append(mGeometryTempInfo.color).append("#");
        List<PointInfo> pointsList = mGeometryTempInfo.pointsList;
        for (int i = 0, pointsListSize = pointsList.size(); i < pointsListSize; i++) {
            PointInfo pointInfo = pointsList.get(i);
            sb.append(pointInfo.x).append(",").append(pointInfo.y);
            if (i != pointsListSize - 1) { // 最后一个坐标点不拼接";"
                sb.append(";");
            }
        }
        sb.append("#").append(mGeometryTempInfo.left).append(",").append(mGeometryTempInfo.top)
                .append(",").append(mGeometryTempInfo.right).append(",").append(mGeometryTempInfo.bottom);
        mGeometryTempInfo.path = sb.toString();
    }

    /**
     * 获取三角图形中坐标点对象,不带压力值
     */
    private PointInfo obtainGeometryPoint(int x, int y) {
        int index = -1;
        if (x == minX && y == minY) {
            index = 0;
        }
        if (x == maxX && y == minY) {
            index = 1;
        }
        if (x == maxX && y == maxY) {
            index = 2;
        }
        if (x == minX && y == maxY) {
            index = 3;
        }

        return obtainGeometryPoint(x, y, index);
    }

    private PointInfo obtainGeometryPoint(int x, int y, int index) {
        PointInfo pointInfo = new PointInfo();
        pointInfo.x = x;
        pointInfo.y = y;
        pointInfo.index = index;
        return pointInfo;
    }
    //---------------------------for geometry record end------------------------------------------

    /**
     * 得到笔迹字符串，可用于保存、恢复笔记
     */
    public String getStrokes() {
        if (DEBUG) {
            Log.i(TAG, "getStrokes() >>> mIsHWCInitFinished_" + mIsHWCInitFinished + " mIsStrokesChanged_" + mIsStrokesChanged);
        }

        if (!mIsStrokesChanged) {
            if (DEBUG) {
                Log.i(TAG, "getStrokes() >>> mOldStrokes_" + mOldStrokes);
            }
            return mOldStrokes;
        }

        return getStrokesString();
    }

    @Nullable
    private String getStrokesString() {
        if (mIsRubber) {
            listToStrokes(true);
        }

        if (mStrokes == null || mStrokes.length() <= 1) {
            if (DEBUG) {
                Log.i(TAG, "getStrokesString() >>> mStrokes == null || mStrokes.length() <= 1");
            }
            return "";
        }

        String temp = mStrokes.toString();
        // 2018/8/3 0003/16:51 新增笔迹字符串中笔迹宽度信息  ------------modify by JiLin
        temp = mWidth + "," + mHeight + "," + mStrokeWidth + ","
                + mStrokeHeight + "&" + getStrokeVersion() + "@" + mPaintSizeThreshold + "@" + temp;

        if (DEBUG) {
            Log.i(TAG, "getStrokesString() >>> " + temp);
        }
        return temp;
    }

    public String getGeometryStrokesOnly(@NonNull String geometryStroke) {
        return mWidth + "," + mHeight + "," + mStrokeWidth + ","
                + mStrokeHeight + "&" + getStrokeVersion() + "@" + geometryStroke;
    }

    // -------------------------for bitmap

    /**
     * 导入图片
     *
     * @param bitmap 图片
     */
    public void loadBitmap(Bitmap bitmap) {
        if (DEBUG) {
            Log.i(TAG, "loadBitmap");
        }
        if (mHandWritingCanvas != null) {
            mHandWritingCanvas.drawBitmap(bitmap, mPaint);
        }
    }

    /**
     * 导入图片，数据 -> 图片
     *
     * @param data 图片数据
     */
    public void loadBitmap(byte[] data) {
        if (DEBUG) {
            Log.i(TAG, "loadBitmap");
        }
        Bitmap image;
        try {
            image = BitmapFactory.decodeByteArray(data, 0, data.length);
            loadBitmap(image);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取手写笔迹图片的base64编码字符串
     */
    public String getEncodeBitmap() {
        byte[] data = getBitmapBytes();
        if (data == null) {
            return null;
        }
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    /**
     * 获取手写笔迹图片的byte[]
     */
    public byte[] getBitmapBytes() {
        Bitmap bitmap = getBitmap();
        if (bitmap != null && !bitmap.isRecycled()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            byte[] data = bos.toByteArray();
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        } else {
            return null;
        }
    }

    /**
     * 获取手写笔迹Bitmap
     */
    public Bitmap getBitmap() {
        if (mWritingViewBitmap == null) {
            if (mWidth == 0 || mHeight == 0) {
                Log.e(TAG, "mWidth == 0 || mHeight == 0 !!!");
                return null;
            } else {
                mWritingViewBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_4444);
            }

            mWritingViewCanvas = new Canvas(mWritingViewBitmap);
        }

        mWritingViewBitmap.eraseColor(Color.TRANSPARENT);
        draw(mWritingViewCanvas);
        return mWritingViewBitmap;
    }

    /**
     * 回收图片
     */
    public void recycleBitmap() {
        if (DEBUG) {
            Log.i(TAG, "recycleBitmap");
        }
        if (mWritingViewBitmap != null && !mWritingViewBitmap.isRecycled()) {
            mWritingViewBitmap.recycle();
            mWritingViewBitmap = null;
        }
    }

    //------------------------各种小算法: begin by tianpengbin--------------------------
    // 检查点有没有在橡皮范围内
    private boolean checkDelete(int x, int y, int touchX, int touchY) {
        return Math.pow(touchY - y, 2) + Math.pow(touchX - x, 2) <= Math.pow(eraserHalf, 2);
    }

    /* 检查圆与椭圆是否相交，可用于检测橡皮与椭圆的碰撞检测*/

    /**
     * @param ovalX   椭圆中心点x轴坐标
     * @param ovalY   椭圆中心点y轴坐标
     * @param a       椭圆x轴半径
     * @param b       椭圆y轴半径
     * @param circleX 橡皮擦中心点x轴坐标
     * @param circleY 橡皮擦中心点y轴坐标
     * @param r       橡皮擦的半径
     */
    public boolean checkOvalDelete(int ovalX, int ovalY, int a, int b, int circleX, int circleY, int r) {
        /* 求两圆心形成的直线与圆的两个交点 */
        CGPoint[] points = getPoint(circleX, circleY, r, ovalX, ovalY, circleX, circleY);

        if (DEBUG) {
            Log.i(TAG, "x1=" + points[0].x + "y1=" + points[0].y);
            Log.i(TAG, "x2=" + points[1].x + "y2=" + points[1].y);
        }

		/* 判断两交点是否在椭圆内 */
        return getValue(ovalX, ovalY, a, b, (int) points[0].x, (int) points[0].y)
                * getValue(ovalX, ovalY, a, b, (int) points[1].x, (int) points[1].y) <= 0;
    }

    CGPoint[] getPoint(int cx, int cy, int r, int stx, int sty, int edx, int edy) {
        // (x - cx )^2 + (y - cy)^2 = r^2
        // y = kx +b

        // 求得直线方程
        double k = ((double) (edy - sty)) / (edx - stx);
        double b = edy - k * edx;

        // 列方程
        /*
         * (1 + k^2)*x^2 - x*(2*cx -2*k*(b -cy) ) + cx*cx + ( b - cy)*(b - cy) -
		 * r*r = 0
		 */
        double x1, y1, x2, y2;
        double c = cx * cx + (b - cy) * (b - cy) - r * r;
        double a = (1 + k * k);
        double b1 = (2 * cx - 2 * k * (b - cy));
        // 得到下面的简化方程
        // a*x^2 - b1*x + c = 0;

        double tmp = Math.sqrt(b1 * b1 - 4 * a * c);
        x1 = (b1 + tmp) / (2 * a);
        y1 = k * x1 + b;
        x2 = (b1 - tmp) / (2 * a);
        y2 = k * x2 + b;

        // 判断求出的点是否在圆上
        CGPoint[] p = new CGPoint[2];
        p[0] = new CGPoint();
        p[0].x = x1;
        p[0].y = y1;
        p[1] = new CGPoint();
        p[1].x = x2;
        p[1].y = y2;
        return p;
    }

    /* 计算x2/a2+y2/b2-1的值 */
    public float getValue(int ovalX, int ovalY, int a, int b, int x, int y) {
        return (float) Math.pow(x - ovalX, 2) / (float) Math.pow(a, 2)
                + (float) Math.pow(y - ovalY, 2) / (float) Math.pow(b, 2) - (float) 1;
    }
    //------------------------各种小算法: end by tianpengbin--------------------------

    // 检查线段是不是在橡皮范围内或与橡皮轨迹相交
    private boolean checkDelete(PointInfo startPoint, PointInfo endPoint, int touchX, int touchY) {
        int startX = startPoint.x;
        int startY = startPoint.y;
        int endX = endPoint.x;
        int endY = endPoint.y;
        int minX = Math.min(startX, endX);
        int minY = Math.min(startY, endY);
        int maxX = Math.max(startX, endX);
        int maxY = Math.max(startY, endY);

        // 检查两点在不在橡皮范围内
        if (checkDelete(endX, endY, touchX, touchY)) {
            return true;
        }

		/* 检查两点形成的线段会不会与橡皮轨迹相交 ,这一步检查不能放在下一步之后，会遗露 */
        if (oldX != -1) {
            if (intersect1(startX, startY, endX, endY, oldX, oldY, touchX, touchY)) {
                return true;
            }
        }

        // 检查两点形成的线段会不会与橡皮范围相交
        if (touchX + eraserHalf < minX || touchX - eraserHalf > maxX
                || touchY + eraserHalf < minY || touchY - eraserHalf > maxY) {
            return false;
        }
        double distance = pointToLine(startX, startY, endX, endY, touchX, touchY);

        return distance <= eraserHalf;
    }

    private void updateRect(PathInfo pathInfo, boolean isFirst) {
        if (isFirst) {
            rectMinX = pathInfo.left;
            rectMinY = pathInfo.top;
            rectMaxX = pathInfo.right;
            rectMaxY = pathInfo.bottom;
        } else {
            rectMinX = Math.min(rectMinX, pathInfo.left);
            rectMinY = Math.min(rectMinY, pathInfo.top);
            rectMaxX = Math.max(rectMaxX, pathInfo.right);
            rectMaxY = Math.max(rectMaxY, pathInfo.bottom);
        }
    }

    /* 检测并擦除橡皮碰到的线 */
    private void deleteRecord(int touchX, int touchY) {
        boolean isDel = false; //用于记录是否有线被删除了
        if (DEBUG) {
            Log.i(TAG, "pathsList.size()=" + pathsList.size());
        }

        long s = System.currentTimeMillis();
        for (int i = 0; i < pathsList.size(); i++) {
            PathInfo pathInfo = pathsList.get(i);
            List<PointInfo> pointsList = pathInfo.pointsList;
            int pointSize = pointsList.size();
            if (pointSize == 0) {
                continue;
            }

            // 加入范围检查，来提高碰撞算法性能
            if (touchX + eraserHalf < pathInfo.left
                    || touchY + eraserHalf < pathInfo.top
                    || touchX - eraserHalf > pathInfo.right
                    || touchY - eraserHalf > pathInfo.bottom) {
                if (oldX != -1) {
                    if (!intersect1(pathInfo.left, pathInfo.top, pathInfo.right, pathInfo.bottom,
                            oldX, oldY, touchX, touchY) &&
                            !intersect1(pathInfo.left, pathInfo.bottom, pathInfo.right, pathInfo.top,
                                    oldX, oldY, touchX, touchY)) {
                        continue;
                    }
                }
            }

            // 先检查第一个点有没有在橡皮范围内
            PointInfo pointInfo = pointsList.get(0);
            if (checkDelete(pointInfo.x, pointInfo.y, touchX, touchY)) {
                updateRect(pathInfo, !isDel);
                pathsList.remove(i);
                i -= 1;
                isDel = true;
                continue;
            }

            // 2017/11/8 0008/14:17  检测椭圆笔迹删除操作 start------------modify by JiLin
            // 椭圆只用该方法检测就可以了;如果继续往下执行就会在边界碰撞检测时出问题
            if (isDefDrawType(pathInfo.drawType, DrawType.OVAL)) {
                int midX = (pathInfo.right - pathInfo.left) / 2;
                int midY = (pathInfo.bottom - pathInfo.top) / 2;
                if (checkOvalDelete(pathInfo.left + midX, pathInfo.top + midY, midX, midY,
                        touchX, touchY, eraserHalf)) {
                    updateRect(pathInfo, !isDel);
                    pathsList.remove(i);
                    i -= 1;
                    isDel = true;
                }
                continue;
            }
            // 2017/11/8 0008/14:17  检测椭圆笔迹删除操作 end------------modify by JiLin

            // 2017/11/27 0027/9:27 检测坐标系笔迹删除操作 start  ------------modify by JiLin
            if (isDefDrawType(pathInfo.drawType, DrawType.COORDINATE) && pointSize == 5) {
                PointInfo pointInfo0 = pointsList.get(0);
                PointInfo pointInfo1 = pointsList.get(1);
                PointInfo pointInfo2 = pointsList.get(2);
                PointInfo pointInfo3 = pointsList.get(3);
                if (checkDelete(pointInfo0, pointInfo2, touchX, touchY) ||
                        checkDelete(pointInfo1, pointInfo3, touchX, touchY)) {
                    updateRect(pathInfo, !isDel);
                    pathsList.remove(i);
                    i -= 1;
                    isDel = true;
                }
                continue;
            }
            // 2017/11/27 0027/9:27 检测坐标系笔迹删除操作 end  ------------modify by JiLin

            // 再检查其它点及与前一个点形成的线是否与橡皮范围相交
            for (int j = 1; j < pointSize; j++) {
                PointInfo prePointInfo = pointsList.get(j - 1);
                pointInfo = pointsList.get(j);
                if (checkDelete(prePointInfo, pointInfo, touchX, touchY)) {
                    updateRect(pathInfo, !isDel);
                    pathsList.remove(i);
                    i -= 1;
                    isDel = true;
                    break;
                }

                // 2017/11/3 0003/13:47 新增几何图形中自动生成的最后一条边的碰撞删除检测  ------------modify by JiLin start
                if ((isDefDrawType(pathInfo.drawType, DrawType.TRIANGLE) ||
                        isDefDrawType(pathInfo.drawType, DrawType.RECTANGLE) ||
                        isDefDrawType(pathInfo.drawType, DrawType.TRAPEZIUM)) &&
                        j == pointSize - 1) {
                    if (checkDelete(pointsList.get(0), pointInfo, touchX, touchY)) {
                        updateRect(pathInfo, !isDel);
                        pathsList.remove(i);
                        i -= 1;
                        isDel = true;
                        break;
                    }
                }
                // 2017/11/3 0003/13:47 新增几何图形中自动生成的最后一条边的碰撞删除检测  ------------modify by JiLin end
            }
        }
        long e = System.currentTimeMillis();

        if (DEBUG) {
            Log.i(TAG, "擦除遍历集合耗时 : [" + (e - s) + "ms]");
        }

        int addedValue = 18;// 两个点之间形成的曲线可能超过点形成的矩形范围，多加一点范围来容错

        if (isDel) {
            rectMinX -= addedValue;
            rectMinY -= addedValue;
            rectMaxX += addedValue;
            rectMaxY += addedValue;

            if (DEBUG) {
                Log.i(TAG, "rectMinX = " + rectMinX + ", rectMinY = " + rectMinY +
                        ", rectMaxX = " + rectMaxX + ", rectMaxY = " + rectMaxY);
            }

            Rect mSrcRect = new Rect(rectMinX, rectMinY, rectMaxX, rectMaxY);

            mPaint.setStrokeWidth(DEFAULT_PAINT_SIZE);
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setPathEffect(null);

            if (mHandWritingCanvas != null) {
                mHandWritingCanvas.drawRect(mSrcRect, mPaint);
            }
            // 判断哪条线可能过了矩形范围，重新画线
            if (pathsList.size() > 0) {
                s = System.currentTimeMillis();
                int pathSize = pathsList.size();
                int count = 0;
                for (int i = 0; i < pathSize; i++) {
                    PathInfo pathInfo = pathsList.get(i);
                    if (mSrcRect.intersects(pathInfo.left - addedValue,
                            pathInfo.top - addedValue, pathInfo.right
                                    + addedValue, pathInfo.bottom + addedValue)) {
                        count++;
                        isNeedRedraw = true;// 其实这里可以再进一步，若被重绘的线也被删了，isNeedRedraw其实不应该是true的
                        drawOrRecordOnePath(pathInfo, true);
                    }
                }
                e = System.currentTimeMillis();

                if (DEBUG) {
                    Log.i(TAG, "pathsList.size() = " + pathSize + ", 重绘次数 : " + count + ", 耗时 : [" + (e - s) + "ms]");
                }
            }

            mIsStrokesChanged = true;
        }

        if (DEBUG) {
            Log.d(TAG, "out");
        }
    }

    //----------------------------恢复笔迹 start----------------------------

    /**
     * 将保存的字符串笔记恢复到手写view里
     *
     * @param str 字符串笔记
     */
    public void restoreToImage(final String str) {
        if (DEBUG) {
            Log.i(TAG, "restoreToImage() >>> str:" + str);
        }

        clear();
        if (TextUtils.isEmpty(str)) {
            return;
        }

        mOldStrokes = str;
        if (mHandWritingCanvas == null) {
            // 如果代码执行到这里,说明是当前手写控件宽高信息还没有初始化完成时,调用了还原笔迹的方法;
            // 此时,创建一个Runnable任务,在onAttachedToWindow()方法把该任务发送到消息队列去执行;
            mRestoreToImageRunnable = new Runnable() {
                @Override
                public void run() {
                    restoreToImage(str);
                }
            };
        } else {
            mIsStrokesChanged = false;
            long s = System.currentTimeMillis();
            restoreCanvas(str, false);
            long e = System.currentTimeMillis();
            if (DEBUG) {
                Log.i(TAG, "restoreToImage() 耗时 : [" + (e - s) + "ms]");
            }
            postInvalidate();
        }
    }

    public void restoreToGeometryOnly(String str) {
        if (DEBUG) {
            Log.i(TAG, "restoreToGeometryOnly() >>> str:" + str);
        }

        if (TextUtils.isEmpty(str)) {
            return;
        }

        mIsStrokesChanged = true;
        restoreCanvas(str, true);
        invalidate();
    }

    /**
     * 清除所有笔迹
     */
    public void clear() {
        if (DEBUG) {
            Log.i(TAG, "clear()");
        }

        if (mHandWritingCanvas != null) {
            mIsStrokesChanged = true;
            mStrokeWidth = 0;
            mStrokeHeight = 0;
            mOldStrokes = "";
            mStrokes = new StringBuilder();
            pathsList.clear();
            mHandWritingCanvas.clearCanvas();
            this.postInvalidate();
        } else if (DEBUG) {
            Log.e(TAG, "clear() >>> mHandWritingCanvas == null!!!!");
        }
    }

    /**
     * @param str      笔迹字符串
     * @param isAppend 是否追加到原有笔迹中
     */
    private void restoreCanvas(String str, boolean isAppend) {
        int index = str.indexOf("&");
        if (index != -1) {
            // 解析笔迹字符串中笔迹宽高信息
            restoreStrokesRange(str.substring(0, index));
            // 剔除控件宽高、笔迹宽高信息;
            str = str.substring(index + 1);
        } else {
            if (DEBUG) {
                Log.e(TAG, "invalid Stroke >>> str.indexOf(&) == -1");
            }
            return;
        }

        // 校验笔迹版本信息
        int versionIndex = str.indexOf("@");
        String strokeVersion;
        if (versionIndex != -1) {
            // 得到笔迹版本号
            strokeVersion = str.substring(0, versionIndex);
        } else {
            if (DEBUG) {
                Log.e(TAG, "invalid Stroke >>> str.indexOf(@) == -1");
            }
            return;
        }

        // 开始解析笔迹内容字符串
        if (getStrokeVersion().equals(strokeVersion)) {
            str = str.substring(versionIndex + 1);
            // 2018/8/3 0003/16:45 新增解析笔迹字符串中笔迹宽度信息  s------------modify by JiLin
            int paintSizeThresholdIndex = str.indexOf("@");
            float tempPaintSizeThreshold = mPaintSizeThreshold; //记录还原笔迹之前的笔迹宽度阈值
            if (paintSizeThresholdIndex != -1) {
                float paintSizeThreshold = toFloat(str.substring(0, paintSizeThresholdIndex));
                if (paintSizeThreshold < MIN_PEN_SIZE) {
                    if (DEBUG) {
                        Log.e(TAG, "invalid paintSizeThreshold! paintSizeThreshold == " + paintSizeThreshold);
                    }
                } else {
                    mPaintSizeThreshold = paintSizeThreshold;
                }
                str = str.substring(paintSizeThresholdIndex + 1);
            }
            // 2018/8/3 0003/16:45 新增解析笔迹字符串中笔迹宽度信息  e------------modify by JiLin

            // 追加笔迹数据集合到原有集合中
            if (isAppend && pathsList != null && pathsList.size() > 0) {
                pathsList.addAll(strokesToList(str));
            } else {
                pathsList = strokesToList(str);
            }

            if (hasRange) {
                // 追加笔迹字符串到原有笔迹字符串中
                if (isAppend && !TextUtils.isEmpty(mStrokes)) {
                    mStrokes.append("=").append(str);
                } else {
                    mStrokes = new StringBuilder(str);
                }
            }

            // 开始绘制笔迹数据
            restorePoints();
            mPaintSizeThreshold = tempPaintSizeThreshold; //恢复笔迹宽度阈值
        } else {
            if (DEBUG) {
                Log.e(TAG, "invalid Stroke! STROKE_VERSION == " + strokeVersion);
            }
        }
    }

    /**
     * 从笔迹字符串范围信息中解析出笔迹宽高
     */
    private void restoreStrokesRange(String strokesRange) {
        int strokesHeightIndex = strokesRange.lastIndexOf(",");
        int strokesWidthIndex = -1;
        if (strokesHeightIndex != -1) {
            mStrokeHeight = Math.max(mStrokeHeight, toInt(strokesRange.substring(strokesHeightIndex + 1)));
            strokesRange = strokesRange.substring(0, strokesHeightIndex);
            strokesWidthIndex = strokesRange.lastIndexOf(",");

            if (DEBUG) {
                Log.i(TAG, "mStrokeHeight = [" + mStrokeHeight + "]");
            }
        }

        if (strokesWidthIndex != -1) {
            mStrokeWidth = Math.max(mStrokeWidth, toInt(strokesRange.substring(strokesWidthIndex + 1)));

            if (DEBUG) {
                Log.i(TAG, "mStrokeWidth = [" + mStrokeWidth + "]");
            }
        }
    }

    /* 将笔记字符串转为数据结构 */
    private List<PathInfo> strokesToList(String str) {
        final ArrayList<PathInfo> paths = new ArrayList<>();
        long s = System.currentTimeMillis();
        splitString(str, "=", new SplitCall() {
            @Override
            public void splitCall(int index, String subString) {
                PathInfo pathInfo = new PathInfo();
                pathInfo.path = subString;

				/* 即使字符串为空也会回调一次，将这一次过滤掉 */
                if (subString.length() <= 1) {
                    return;
                }

                pathInfo.pointsList = new ArrayList<>();
                eventStrokes(subString, pathInfo);// 状态#x,y;x,y
                paths.add(pathInfo);
            }
        });
        long e = System.currentTimeMillis();

        if (DEBUG) {
            Log.i(TAG, "strokesToList()方法总耗时 : [" + (e - s) + "ms]");
        }

        return paths;
    }

    /**
     * 动作string转换为PathInfo
     */
    private void eventStrokes(final String subStokers, final PathInfo pathInfo) {
        splitString(subStokers, "#", new SplitCall() {
            @Override
            public void splitCall(int index, String subString) {
                switch (index) {
                    case 0:
                        pathInfo.drawType = cachedToInt(subString);
                        break;
                    case 1:
                        pathInfo.color = cachedToInt(subString);
                        break;
                    case 2:
                        splitString(subString, ";", new SplitCall() {
                            @Override
                            public void splitCall(int index, String subString) {
                                obtainPointInfo(pathInfo, subString);
                            }
                        });
                        break;
                    case 3:
                        hasRange = true;
                        splitString(subString, ",", new SplitCall() {
                            @Override
                            public void splitCall(int index, String subString) {
                                if (index < ranges.length) {
                                    ranges[index] = subString;
                                } else {
                                    if (DEBUG) {
                                        Log.e(TAG, "笔迹损坏，大概是丢失了down事件吧");
                                    }
                                }
                            }
                        });
                        pathInfo.left = toInt(ranges[0]);
                        pathInfo.top = toInt(ranges[1]);
                        pathInfo.right = toInt(ranges[2]);
                        pathInfo.bottom = toInt(ranges[3]);
                        break;
                }
            }
        });
    }

    private void obtainPointInfo(PathInfo pathInfo, String coordinatesToken) {
        final PointInfo pointInfo = new PointInfo();
        splitString(coordinatesToken, ",", new SplitCall() {
            @Override
            public void splitCall(int index, String subString) {
                switch (index) {
                    case 0:
                        pointInfo.x = toInt(subString);// 位置- x,y
                        break;
                    case 1:
                        pointInfo.y = toInt(subString);
                        break;
                    case 2:
                        pointInfo.pressure = cachedToFloat(subString);
                        break;
                }
            }
        });

        pathInfo.pointsList.add(pointInfo);
    }

    /* 将数据结构转为笔记字符串 */
    private void listToStrokes(boolean hasRange) {
        mStrokes = new StringBuilder();
        int pathSize = pathsList.size();
        boolean first = true;
        mStrokeWidth = 0;
        mStrokeHeight = 0;
        for (int i = 0; i < pathSize; i++) {
            PathInfo pathInfo = pathsList.get(i);
            if (hasRange) {
                if (first) {
                    mStrokes.append(pathInfo.path);
                    first = false;
                } else {
                    mStrokes.append("=").append(pathInfo.path);
                }

                mStrokeWidth = Math.max(mStrokeWidth, pathInfo.right);
                mStrokeHeight = Math.max(mStrokeHeight, pathInfo.bottom);
            } else {
                drawOrRecordOnePath(pathInfo, false);
            }
        }
    }

    /* 恢复笔记时用到的子函数 */
    private void restorePoints() {
        if (mHandWritingCanvas == null) {
            if (DEBUG) {
                Log.e(TAG, "restorePoints() >>> mHandWritingCanvas == null");
            }
            return;
        }

        long s = System.currentTimeMillis();
        for (int i = 0; i < pathsList.size(); i++) {
            drawOrRecordOnePath(pathsList.get(i), true);
        }
        long e = System.currentTimeMillis();

        if (DEBUG) {
            Log.i(TAG, "restorePoints()方法耗时 : [" + (e - s) + "ms]");
        }

        if (!hasRange) {// 若是旧的的没有范围和压力值的笔记格式，会在这里将它整合成新的笔记格式
            listToStrokes(false);
            mIsStrokesChanged = true;
            pathsList = strokesToList(mStrokes.toString());
            hasRange = true;
        }
    }

    /* 恢复笔记中的用到的函数，用来划一条线 */
    private void drawOrRecordOnePath(PathInfo pathInfo, boolean isDraw) {
        /* 记录划线之前的状态，划完后要恢复到原来状态 */
        boolean isRubberSrc = mIsRubber;
        DrawType drawTypeSrc = mDrawType;
        int penColorSrc = getPenColor(); //得到画笔原始颜色
        int geometryPenColorSrc = getGeometryPenColor(); //得到几何画笔原始颜色

		/* 划线 */
        setToWritingInside();

        DrawType tempDrawType = DrawType.toDrawType(pathInfo.drawType);
        setDrawType(tempDrawType);
        if (isGeometryType()) {
            mGeometryPaint.setColor(pathInfo.color);
        } else {
            mPaint.setColor(pathInfo.color);
        }

        List<PointInfo> pointsList = pathInfo.pointsList;
        switch (mDrawType) {
            case TRIANGLE: //三角形
            case TRAPEZIUM: //梯形
                drawTriangleOrTrapezium(pointsList);
                break;
            case RECTANGLE: //矩形
                drawRectangle(pathInfo);
                break;
            case OVAL: //椭圆
                drawOval(pathInfo);
                break;
            case COORDINATE: //坐标系
                drawCoordinate(pathInfo);
                break;
            case NUMBER_AXIS: //数轴
                drawNumberAxis(pathInfo);
                break;
            case CURVE://曲线
            case LINE: //直线
            case DASH: //点曲线
            case ARROW: //箭头
            case DASH_LINE: //点直线
            default:
                //恢复基础线型
                int pointSize = pointsList.size();
                for (int j = 0; j < pointSize; j++) {
                    PointInfo pointInfo = pointsList.get(j);
                    int x = pointInfo.x;
                    int y = pointInfo.y;
                    float pressure = pointInfo.pressure;
                    if (j == 0) {
                        if (isDraw) {
                            touchStart(x, y, pressure);
                        } else {
                            recordStart(x, y, pressure);
                        }

                    } else if (j == pointSize - 1) {
                        // 终点
                        if (isDraw) {
                            touchUp(x, y, pressure);
                        } else {
                            recordUp(x, y, pressure);
                        }
                    } else {
                        // 拖动
                        if (isDraw) {
                            touchMove(x, y, pressure);
                        } else {
                            recordMove(x, y, pressure);
                        }
                    }
                }
        }

        if (isGeometryType()) {
            // 重置几何图形画笔颜色
            if (geometryPenColorSrc != getGeometryPenColor()) {
                setGeometryPaintColor(geometryPenColorSrc);
            }
        } else {
            // 重置画笔颜色
            if (penColorSrc != getPenColor()) {
                setPenColor(penColorSrc);
            }
        }

        /* 恢复到原来状态 */
        if (drawTypeSrc != mDrawType) {
            setDrawType(drawTypeSrc);
        }

        if (isRubberSrc) {
            setToRubberInside();
        }
    }

    private void deleteOnePath(PathInfo deletePathInfo) {
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setPathEffect(null);

        List<PointInfo> pointsList = deletePathInfo.pointsList;
        int pointSize = pointsList.size();
        Path path = new Path();
        for (int j = 0; j < pointSize; j++) {
            PointInfo pointInfo = pointsList.get(j);
            int x = pointInfo.x;
            int y = pointInfo.y;
            if (j == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        Path destPath = new Path();
        if (mHandWritingCanvas != null) {
            mPaint.setTextSize(10);
            mPaint.getFillPath(path, destPath);
            mHandWritingCanvas.drawPath(destPath, mPaint);
        }

        if (pathsList.size() > 0) {
            long s = System.currentTimeMillis();
            int pathSize = pathsList.size();
            int count = 0;
            for (int i = 0; i < pathSize; i++) {
                PathInfo pathInfo = pathsList.get(i);
                destPath.computeBounds(dirtyRect, false);
                if (dirtyRect.intersects(pathInfo.left, pathInfo.top, pathInfo.right, pathInfo.bottom)) {
                    count++;
                    drawOrRecordOnePath(pathInfo, true);
                }
            }
            long e = System.currentTimeMillis();

            if (DEBUG) {
                Log.i(TAG, "pathsList.size() = " + pathSize + ", 重绘次数 : " + count + ", 耗时 : [" + (e - s) + "ms]");
            }
        }
    }

    /**
     * 截取字符串
     */
    private void splitString(String content, String separator, SplitCall splitCall) {
        int startIndex = 0;
        int endIndex;
        int index = 0;
        int separatorLength = separator.length();
        int contentLength = content.length();
        while (true) {
            endIndex = content.indexOf(separator, startIndex);
            if (endIndex == -1) {
                endIndex = contentLength;
            }
            String subString = content.substring(startIndex, endIndex);
            splitCall.splitCall(index, subString);
            startIndex = endIndex + separatorLength;
            index++;
            if (endIndex == contentLength) {
                break;
            }
        }
    }

    //----------------------------恢复笔迹 end----------------------------

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.i(TAG, "onAttachedToWindow()");
        }

        if (mRestoreToImageRunnable != null) {
            post(mRestoreToImageRunnable);
            mRestoreToImageRunnable = null;

            if (DEBUG) {
                Log.i(TAG, "mRestoreToImageRunnable");
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // 存储未保存的几何图形笔迹字符串
        if (mIsGeometryEditable) {
            updateGeometryPathInfo(); //更新几何图形笔迹信息
            appendGeometryStrokes(); //拼接几何图形笔迹
        }

        if (DEBUG) {
            Log.i(TAG, "mIsGeometryEditable : " + mIsGeometryEditable);
        }
    }

    /**
     * 判断当前线型是否为几何图形
     */
    public boolean isGeometryType() {
        return isGeometryType(mDrawType);
    }

    private boolean isGeometryType(DrawType drawType) {
        switch (drawType) {
            case TRIANGLE: //三角形
            case RECTANGLE: //矩形
            case TRAPEZIUM: //梯形
            case OVAL: //椭圆
            case COORDINATE: //坐标系
            case NUMBER_AXIS: //数轴
                return true;
            case CURVE://曲线
            case LINE: //直线
            case DASH: //点曲线
            case ARROW: //箭头
            case DASH_LINE: //点直线
            default:
                return false;
        }
    }

    /**
     * 判断给定线类型是否是指定线类型
     */
    private boolean isDefDrawType(int drawTypeCode, DrawType defDrawType) {
        return defDrawType != null && defDrawType.getCode() == drawTypeCode;
    }

    //------------------------------设置几何图形画笔相关属性---------------------start

    public void setPaintSizeThreshold(float paintSizeThreshold) {
        if (paintSizeThreshold < MIN_PEN_SIZE || paintSizeThreshold > 4.0f) {
            if (DEBUG) {
                Log.e(TAG, "ERROR paintSizeThreshold : " + paintSizeThreshold + " < 1.0f or > 4.0f");
            }
            return;
        }
        mPaintSizeThreshold = paintSizeThreshold;
    }

    /**
     * 设置几何图形画笔颜色
     *
     * @param color 画笔颜色值
     */
    public void setGeometryPaintColor(@ColorInt int color) {
        // 处理当前处于几何图形编辑状态时的切换逻辑
        if (mGeometryListener != null) {
            mGeometryListener.handleEditableGeometry();
        }

        if (!mIsGeometryEditable && mGeometryPaint != null) {
            mGeometryPaint.setColor(color);
        }
    }

    /**
     * 设置几何图形画笔样式
     *
     * @param style 画笔样式
     */
    public void setGeometryPaintStyle(@NonNull Paint.Style style) {
        // 处理当前处于几何图形编辑状态时的切换逻辑
        if (mGeometryListener != null) {
            mGeometryListener.handleEditableGeometry();
        }

        if (!mIsGeometryEditable && mGeometryPaint != null) {
            mGeometryPaint.setStyle(style);
        }
    }

    /**
     * 设置数轴或者坐标系单位长度
     *
     * @param axisUnit 单位长度
     */
    public void setAxisUnit(int axisUnit) {
        if (axisUnit <= 0) {
            return;
        }
        mAxisUnit = axisUnit;
    }

    //------------------------------设置几何图形画笔相关属性---------------------end

    //------------------------------getter and setter-----------------------start

    /**
     * 判断笔记是否有变化
     */
    public boolean isStrokeChange() {
        return mIsStrokesChanged;
    }

    public void resetStrokeChange() {
        if (DEBUG) {
            Log.i(TAG, "resetStrokeChange()");
        }
        mIsStrokesChanged = false;
    }

    /**
     * 设置当前笔记的颜色，应该传入Color.RED等int值，而不是id
     */
    public void setPenColor(@ColorInt int color) {
        if (DEBUG) {
            Log.d(TAG, "setPenColor >>> color = " + color);
        }

        // 处理当前处于几何图形编辑状态时的切换逻辑
        if (mGeometryListener != null) {
            mGeometryListener.handleEditableGeometry();
        }

        if (mPaint != null) {
            mPaint.setColor(color);
        }
    }

    public int getPenColor() {
        if (mPaint != null) {
            return mPaint.getColor();
        }
        return 0;
    }

    public int getGeometryPenColor() {
        if (mGeometryPaint != null) {
            return mGeometryPaint.getColor();
        }
        return 0;
    }

    /**
     * 设置线型;当在"皮擦"状态时，不允许切换线型，返回false。
     */
    public boolean setDrawType(DrawType type) {
        if (DEBUG) {
            Log.i(TAG, "setDrawType >>> type = " + type);
        }

        if (mIsRubber) {
            if (DEBUG) {
                Log.e(TAG, "mRubber status not allow to set mDrawType!!!");
            }
            return false;
        }

        if (mPaint == null) {
            return false;
        }

        // 处理当前处于几何图形编辑状态时的切换逻辑
        if (mGeometryListener != null) {
            mGeometryListener.handleEditableGeometry();
        }

        mDrawType = type;
        // 重置画笔状态
        switch (mDrawType) {
            case DASH: //点曲线
            case DASH_LINE: //点直线
                mPaint.setStyle(Paint.Style.STROKE);
                // DashPathEffect 画虚线，{5,15,5,15}  5实线，15虚线，5实线，15虚线
                // 虚线绘制的时候会不断的循环这个数组，1表示偏移量
                /*  它的构造方法 DashPathEffect(float[] intervals, float phase) 中，第一个参数intervals是
                    一个数组，它指定了虚线的格式：数组中元素必须为偶数（最少是2个），按照「画线长度、空白长度、画线长度、空白长度」
                    ……的顺序排列，例如下面代码中的5,15,5,15就表示虚线是按照「画5像素、空15像素、画5像素、空15像素」的模式来绘制；
                    第二个参数 phase 是虚线的偏移量。
                */
                PathEffect effect = new DashPathEffect(new float[]{5, 15, 5, 15}, 1);
                mPaint.setPathEffect(effect);
                break;
            case CURVE: //曲线
            case LINE: //直线
                mPaint.setStyle(Paint.Style.STROKE);
                if (mPaint.getPathEffect() != null) {// 非点线 清除点线效果
                    mPaint.setPathEffect(null);
                }
                break;
            case ARROW: //箭头
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                if (mPaint.getPathEffect() != null) {// 非点线 清除点线效果
                    mPaint.setPathEffect(null);
                }
                break;
            case TRIANGLE: //三角形
            case RECTANGLE: //矩形
            case TRAPEZIUM: //梯形
            case OVAL: //椭圆
                if (mGeometryPaint != null) {
                    mGeometryPaint.setStyle(Paint.Style.STROKE);
                }
                break;
            case COORDINATE: //坐标系
            case NUMBER_AXIS: //数轴
                if (mGeometryPaint != null) {
                    mGeometryPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                }
                break;
        }
        return true;
    }

    public DrawType getDrawType() {
        return mDrawType;
    }

    public void setRubberBitmap(Bitmap rubber) {
        if (rubber == null) {
            return;
        }
        this.mRubber = rubber;
        eraserHalf = rubber.getWidth() / 2;
    }

    public boolean isRubber() {
        return mIsRubber;
    }

    public boolean getCanDraw() {
        return canDraw;
    }

    /**
     * 是否打开手写，传入true则可以写，传入false则不能书写笔记
     */
    public void setCanDraw(boolean canDraw) {
        if (DEBUG) {
            Log.i(TAG, "canDraw = [" + canDraw + "]");
        }
        this.canDraw = canDraw;
    }

    public void setBitmap(Bitmap mBitmap) {
        if (DEBUG) {
            Log.i(TAG, "setBitmap");
        }
        if (mHandWritingCanvas != null) {
            mHandWritingCanvas.drawBitmap(mBitmap, mBitmapPaint);
        }
    }

    public void setBitmap(Bitmap mBitmap, String stroke) {
        if (DEBUG) {
            Log.i(TAG, "setBitmap");
        }
        // 去掉宽和高和版本
        if (stroke != null) {
            int index = stroke.lastIndexOf("@");
            if (index > 0) {
                this.mStrokes = new StringBuilder(stroke.substring(index + 1));
            } else {
                this.mStrokes = new StringBuilder();
            }
        }

        if (mHandWritingCanvas != null) {
            mHandWritingCanvas.drawBitmap(mBitmap, mBitmapPaint);
        }
        invalidate();
    }

    public boolean isHWCInitFinished() {
        return mIsHWCInitFinished;
    }

    public void setDebug(boolean isDebug) {
        DEBUG = isDebug;
    }

    public View getActionDownView() {
        if (DEBUG) {
            Log.i(TAG, "getActionDownView()");
        }
        return mActionDownView;
    }

    public void setActionDownView(View actionDownView) {
        if (DEBUG) {
            Log.i(TAG, "setActionDownView()");
        }
        this.mActionDownView = actionDownView;
    }

    public int getHWWidth() {
        return mWidth;
    }

    public int getHWHeight() {
        return mHeight;
    }

    public void setGeometryListener(IGeometryListener geometryListener) {
        mGeometryListener = geometryListener;
    }

    public boolean isGeometryEditable() {
        return mIsGeometryEditable;
    }

    public void setMaxScale(float maxScale) {
        if (maxScale > mMinScale && maxScale < mMaxScale) {
            mMaxScale = maxScale;
        } else {
            Log.e(TAG, "invalid maxScale : " + maxScale);
        }
    }

    public void release() {
        if (mOnGestureListener != null) {
            mOnGestureListener.release();
        }

        mWritingViewBitmap = null;
        mOnGestureListener = null;
        mCustomGestureDetector = null;
    }
    //------------------------------getter and setter-----------------------end
}