package com.hand.writing;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.view.ViewGroup;

import com.hand.writing.view.HandWritingGeometryView;

import java.util.HashMap;
import java.util.Map;

import static com.hand.writing.utils.HandWritingCacheUtils.toInt;

/**
 * 帮助类
 */
public class HandWritingViewHelper {
    public static boolean DEBUG = false;// 控制日志打印的标志位
    public static boolean IGNORE_TOOL_TYPE_INPUT = true; //是否忽略输入设备类型

    public static final String HEIGHT = "height";
    public static final String WIDTH = "width";
    public static final String STROKE_HEIGHT = "stroke_height";
    public static final String STROKE_WIDTH = "stroke_width";

    private static final String STROKE_VERSION = "1.0";
    private static int sDefMinHeight = 275;//默认手写控件最小高度

    /**
     * Creates a new instance of HandWritingViewHelper.
     */
    private HandWritingViewHelper() {
    }

    /**
     * 根据手写笔迹得到手写控件,若无则创建默认手写区域
     */
    @Deprecated
    public static HandWritingGeometryView getHandWriteViewByStrokeOrDefault(
            Context context, String stroke, int width, int height) {
        HandWritingGeometryView handWritingGeometryView;
        if (stroke == null || !stroke.contains("@")) {
            handWritingGeometryView = new HandWritingGeometryView(context, width, height);
            handWritingGeometryView.setToWriting();
        } else {
            handWritingGeometryView = getHandWriteViewByStroke(context, stroke);
        }
        return handWritingGeometryView;
    }

    /**
     * 根据手写笔迹得到手写控件
     */
    @Nullable
    public static HandWritingGeometryView getHandWriteViewByStroke(Context context, String stroke) {
        Map<String, Integer> widthHeight = getWidthAndHeight(stroke);
        if (widthHeight == null) {
            return null;
        }
        HandWritingGeometryView handWritingGeometryView = new HandWritingGeometryView(context,
                widthHeight.get(WIDTH), widthHeight.get(HEIGHT));
        handWritingGeometryView.restoreToImage(stroke);
        handWritingGeometryView.setToWriting();
        return handWritingGeometryView;
    }

    /**
     * 根据手写笔迹得到手写控件(可根据笔记高度截断调整view高度)
     */
    @Nullable
    public static HandWritingGeometryView getHandWriteViewByStroke(Context context, String stroke,
                                                                   boolean isCutStrokeHeight) {
        return getHandWriteViewByStroke(context, stroke, sDefMinHeight, isCutStrokeHeight);
    }

    @Nullable
    public static HandWritingGeometryView getHandWriteViewByStroke(Context context, String stroke,
                                                                   int minHeight, boolean isCutStrokeHeight) {
        Map<String, Integer> widthHeight = getWidthAndHeight(stroke);
        if (widthHeight == null) {
            return null;
        }
        HandWritingGeometryView handWritingGeometryView;
        int height;
        if (isCutStrokeHeight) {
            if (widthHeight.get(STROKE_HEIGHT) == 0) {
                height = widthHeight.get(HEIGHT);
            } else {
                height = Math.min(widthHeight.get(STROKE_HEIGHT) + 2, widthHeight.get(HEIGHT));
                height = Math.max(height, minHeight);
            }
        } else {
            height = widthHeight.get(HandWritingViewHelper.HEIGHT);
        }

        handWritingGeometryView = new HandWritingGeometryView(context, widthHeight.get(WIDTH), height);

        handWritingGeometryView.restoreToImage(stroke);
        handWritingGeometryView.setToWriting();
        return handWritingGeometryView;
    }

    /**
     * 根据手写笔迹得到手写控件,(可根据笔记高度截断调整view高度)
     */
    public static void getHandWriteViewByStroke(@NonNull HandWritingGeometryView handWritingGeometryView,
                                                String stroke, boolean isCutStrokeHeight) {
        getHandWriteViewByStroke(handWritingGeometryView, stroke, sDefMinHeight, isCutStrokeHeight);
    }

    public static void getHandWriteViewByStroke(@NonNull HandWritingGeometryView handWritingGeometryView, String stroke,
                                                int minHeight, boolean isCutStrokeHeight) {
        Map<String, Integer> widthHeight = getWidthAndHeight(stroke);
        if (widthHeight == null) {
            return;
        }
        int height;
        if (isCutStrokeHeight) {
            if (widthHeight.get(STROKE_HEIGHT) == 0) {
                height = widthHeight.get(HEIGHT);
            } else {
                height = Math.min(widthHeight.get(STROKE_HEIGHT) + 2, widthHeight.get(HEIGHT));
                height = Math.max(height, minHeight);
            }
        } else {
            height = widthHeight.get(HEIGHT);
        }

        ViewGroup.LayoutParams layoutParams = handWritingGeometryView.getLayoutParams();
        layoutParams.height = height;
        handWritingGeometryView.setLayoutParams(layoutParams);

        handWritingGeometryView.restoreToImage(stroke);
    }

    /**
     * 合并多个笔记
     */
    @Nullable
    public static String mergeStrokes(String[] strokes) {
        if (strokes == null) {
            return null;
        }

        int height = 0;
        int width = 0;
        int strokeHeight = 0;
        int strokeWidth = 0;
        StringBuilder mergePaths = null;

        for (String stroke : strokes) {
            Map<String, Integer> widthHeight = getWidthAndHeight(stroke);
            if (widthHeight == null) {
                continue;
            }
            height = Math.max(height, widthHeight.get(HEIGHT));
            width = Math.max(width, widthHeight.get(WIDTH));
            strokeHeight = Math.max(strokeHeight, widthHeight.get(STROKE_HEIGHT));
            strokeWidth = Math.max(strokeWidth, widthHeight.get(STROKE_WIDTH));

            String paths = getPaths(stroke);
            if (paths != null) {
                if (mergePaths == null) {
                    mergePaths = new StringBuilder(paths);
                } else {
                    mergePaths.append("=").append(paths);
                }
            }
        }

        if (mergePaths == null) {
            return null;
        }

        return width + "," + height + "," + strokeWidth + "," + strokeHeight
                + "&" + STROKE_VERSION + "@" + mergePaths.toString();
    }

    /**
     * 书写笔迹存储格式用&将宽、高数据与其他数据分隔开
     */
    @Deprecated
    public static int[] getImageSize(String s) {
        int[] ss = new int[2];
        String mStrImage;
        try {
            mStrImage = new String(Base64.decode(s, Base64.DEFAULT));
            String[] mImageArr = mStrImage.split("&");
            int width, height;
            if (mImageArr.length > 0) {
                String[] strArr = mImageArr[0].split(",");
                width = toInt(strArr[0]);
                height = toInt(strArr[1]);
                ss[0] = width;
                ss[1] = height;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ss;
    }

    /**
     * 解码好的字符串，直接解析得到宽高
     */
    @Nullable
    public static Map<String, Integer> getWidthAndHeight(String strokes) {
        if (TextUtils.isEmpty(strokes)) {
            return null;
        }

        Map<String, Integer> map = null;

        int index = strokes.indexOf("&");
        if (index != -1) {
            String str = strokes.substring(0, index);
            String[] sizeArr = str.split(",");
            if (sizeArr.length == 2 || sizeArr.length == 4) {
                map = new HashMap<>();
                map.put(WIDTH, toInt(sizeArr[0]));
                map.put(HEIGHT, toInt(sizeArr[1]));

                if (sizeArr.length == 4) {
                    map.put(STROKE_WIDTH, toInt(sizeArr[2]));
                    map.put(STROKE_HEIGHT, toInt(sizeArr[3]));
                }
            }
        }

        return map;
    }

    @Nullable
    public static String getPaths(String strokes) {
        if (TextUtils.isEmpty(strokes)) {
            return null;
        }

        int lastIndex = strokes.lastIndexOf("@");
        if (lastIndex != -1) {
            return strokes.substring(lastIndex + 1);
        }
        return null;
    }

    public static String getStrokeVersion() {
        return STROKE_VERSION;
    }

    // 三星手写设备
    private static String[] allSpanDevice = {"GT-N5100", "GT-N5110", "GT-N5120",
            "GT-N8000", "GT-N8010", "SM-P350", "SM-P355C", "SM-P600"};

    public static boolean isSpanDevice() {
        String device_model = Build.MODEL;
        for (String dname : allSpanDevice) {
            if (dname.equals(device_model)) {
                return true;
            }
        }
        return false;
    }

    // 需要特殊适配的没有手写笔的平板型号(单手指书写;多手指滑动)
    private static String[] allSpecialDevices = {"SP1099V"};

    public static boolean isSpecialDevice() {
        String model = Build.MODEL;
        for (String device : allSpecialDevices) {
            if (device.equalsIgnoreCase(model)) {
                return true;
            }
        }

        return false;
    }
}
