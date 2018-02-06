package com.hand.writing;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.ViewGroup;


import com.hand.writing.view.HandWritingView;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 帮助类
 */
public class HandWritingViewHelper {
    public static boolean DEBUG = false;// 控制日志打印的标志位
    public static final String HEIGHT = "height";
    public static final String WIDTH = "width";
    public static final String STROKE_HEIGHT = "stroke_height";
    public static final String STROKE_WIDTH = "stroke_width";
    static int sDefMinHeight = 275;//默认手写控件最小高度

    private HandWritingViewHelper() {
    }

    /**
     * 根据手写笔迹得到手写控件,若无则创建默认手写区域
     */
    public static HandWritingView getHandWriteViewByStrokeOrDefault(
            Context context, String stroke, int width, int height) {
        HandWritingView handWritingView;
        if (stroke == null || !stroke.contains("@")) {
            handWritingView = new HandWritingView(context, width, height);
            handWritingView.setToWriting();
        } else {
            handWritingView = getHandWriteViewByStroke(context, stroke);
        }
        return handWritingView;
    }

    /**
     * 根据手写笔迹得到手写控件
     */
    public static HandWritingView getHandWriteViewByStroke(Context context, String stroke) {
        Map<String, Integer> widthHeight = getWidthAndHeight(stroke);
        if (widthHeight == null) {
            return null;
        }
        HandWritingView handWritingView = new HandWritingView(context,
                widthHeight.get(WIDTH), widthHeight.get(HEIGHT));
        handWritingView.restoreToImage(stroke);
        handWritingView.setToWriting();
        return handWritingView;
    }

    /**
     * 根据手写笔迹得到手写控件(可根据笔记高度截断调整view高度)
     */
    public static HandWritingView getHandWriteViewByStroke(Context context, String stroke,
                                                           boolean isCutStrokeHeight) {
        return getHandWriteViewByStroke(context, stroke, sDefMinHeight, isCutStrokeHeight);
    }

    public static HandWritingView getHandWriteViewByStroke(Context context, String stroke,
                                                           int minHeight, boolean isCutStrokeHeight) {
        Map<String, Integer> widthHeight = getWidthAndHeight(stroke);
        if (widthHeight == null) {
            return null;
        }
        HandWritingView handWritingView;
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

        handWritingView = new HandWritingView(context, widthHeight.get(WIDTH), height);

        handWritingView.restoreToImage(stroke);
        handWritingView.setToWriting();
        return handWritingView;
    }

    /**
     * 根据手写笔迹得到手写控件,(可根据笔记高度截断调整view高度)
     */
    public static void getHandWriteViewByStroke(@NonNull HandWritingView handWritingView,
                                                String stroke, boolean isCutStrokeHeight) {
        getHandWriteViewByStroke(handWritingView, stroke, sDefMinHeight, isCutStrokeHeight);
    }

    public static void getHandWriteViewByStroke(@NonNull HandWritingView handWritingView, String stroke,
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

        ViewGroup.LayoutParams layoutParams = handWritingView.getLayoutParams();
        layoutParams.height = height;
        handWritingView.setLayoutParams(layoutParams);

        handWritingView.restoreToImage(stroke);
        handWritingView.setToWriting();
    }

    /**
     * 合并多个笔记
     */
    public static String mergeStrokes(String[] strokes) {
        int height = 0;
        int width = 0;
        int strokeHeight = 0;
        int strokeWidth = 0;
        String mergePaths = null;
        if (strokes == null) {
            return null;
        }

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
                    mergePaths = paths;
                } else {
                    mergePaths += "=" + paths;
                }
            }
        }

        if (mergePaths == null) {
            return null;
        }

        return width + "," + height + "," + strokeWidth + "," + strokeHeight
                + "&" + HandWritingView.STROKE_VERSION + "@" + mergePaths;
    }

    /**
     * 书写笔迹存储格式用&将宽、高数据与其他数据分隔开
     */
    public static int[] getImageSize(String s) {
        int[] ss = new int[2];
        String mstrImage;
        try {
            mstrImage = new String(BASE64.decode(s));
            String[] mstrImageArr = mstrImage.split("&");
            int mwidth, mheight;
            if (mstrImageArr.length > 0) {
                String[] strArr = mstrImageArr[0].split(",");
                mwidth = HandWritingView.toInt(strArr[0]);
                mheight = HandWritingView.toInt(strArr[1]);
                ss[0] = mwidth;
                ss[1] = mheight;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return ss;
    }

    /**
     * 解码好的字符串，直接解析得到宽高
     */
    public static Map<String, Integer> getWidthAndHeight(String drawPath) {
        if (TextUtils.isEmpty(drawPath)) {
            return null;
        }
        String str = drawPath.split("&")[0];
        String[] size = str.split(",");
        int height = 0;
        int width = 0;
        int strokeHeight = 0;
        int strokeWidth = 0;
        if (size.length == 2 || size.length == 4) {
            width = HandWritingView.toInt(size[0]);
            height = HandWritingView.toInt(size[1]);
            if (size.length == 4) {
                strokeWidth = HandWritingView.toInt(size[2]);
                strokeHeight = HandWritingView.toInt(size[3]);
            }
        }
        Map<String, Integer> map = new HashMap<>();
        map.put(HEIGHT, height);
        map.put(WIDTH, width);
        map.put(STROKE_WIDTH, strokeWidth);
        map.put(STROKE_HEIGHT, strokeHeight);
        return map;
    }

    public static String getPaths(String drawPath) {
        if (TextUtils.isEmpty(drawPath)) {
            return null;
        }
        String[] str = drawPath.split("@");
        if (str.length == 2) {
            return str[1];
        }
        return null;
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
