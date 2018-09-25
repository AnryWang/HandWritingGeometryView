package com.handwriting.demo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.hand.writing.DrawType;
import com.hand.writing.HandWritingViewHelper;
import com.hand.writing.view.HandWritingGeometryView;
import com.handwriting.common.base.MyApplication;
import com.handwriting.common.util.CustomToastUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class HandWritingActivity extends AppCompatActivity {

    @Bind(R.id.root_ll)
    LinearLayout rootLl;
    @Bind(R.id.rubber_or_write_sp)
    Spinner mRubberOrWriteSp;
    @Bind(R.id.color_sp)
    Spinner mColorSp;
    @Bind(R.id.draw_type_sp)
    Spinner mDrawTypeSp;
    @Bind(R.id.scale_iv)
    ImageView mScaleIv;
    @Bind(R.id.hand_writing_view)
    HandWritingGeometryView mHandWritingGeometryView;
    @Bind(R.id.save_strokes_btn)
    Button mSaveStrokesBtn;
    @Bind(R.id.restore_btn)
    Button mRestoreBtn;
    @Bind(R.id.delete_btn)
    Button mDeleteBtn;

    public static final String STROKE_PARENT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "math" + File.separator;
    public static final String STROKE_FILE_NAME = "strokes.txt";
    public static final String STROKE_FILE_PATH = STROKE_PARENT_PATH + STROKE_FILE_NAME;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handwriting);
        ButterKnife.bind(this);
        initDataAndSpinner();
    }

    private void initDataAndSpinner() {
        List<String> rubberOrWriteList = new ArrayList<>();
        rubberOrWriteList.add("手写");
        rubberOrWriteList.add("橡皮");

        List<String> colorList = new ArrayList<>();
        colorList.add("蓝色");
        colorList.add("红色");

        List<String> drawTypeList = new ArrayList<>();
        drawTypeList.add("曲线");
        drawTypeList.add("点曲线");
        drawTypeList.add("直线");
        drawTypeList.add("点直线");
        drawTypeList.add("箭头");
        drawTypeList.add("三角形");
        drawTypeList.add("矩形");
        drawTypeList.add("梯形");
        drawTypeList.add("椭圆");
        drawTypeList.add("坐标系");
        drawTypeList.add("数轴");

        ArrayAdapter<String> rubberOrWriteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rubberOrWriteList);
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, colorList);
        ArrayAdapter<String> drawTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, drawTypeList);

        mRubberOrWriteSp.setAdapter(rubberOrWriteAdapter);
        mRubberOrWriteSp.setSelection(0);
        mRubberOrWriteSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mHandWritingGeometryView == null) {
                    return;
                }
                switch (position) {
                    case 0: // 手写
                        mHandWritingGeometryView.setToWriting();
                        break;
                    case 1: // 橡皮
                        mHandWritingGeometryView.setToRubber();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mColorSp.setAdapter(colorAdapter);
        mColorSp.setSelection(0);
        mColorSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mHandWritingGeometryView == null) {
                    return;
                }
                switch (position) {
                    case 0: // 蓝色
                        mHandWritingGeometryView.setPenColor(Color.BLUE);
                        break;
                    case 1: // 红色
                        mHandWritingGeometryView.setPenColor(Color.RED);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mDrawTypeSp.setAdapter(drawTypeAdapter);
        mDrawTypeSp.setSelection(0);
        mDrawTypeSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mHandWritingGeometryView == null) {
                    return;
                }
                switch (position) {
                    case 0: // 曲线
                        mHandWritingGeometryView.setDrawType(DrawType.CURVE);
                        break;
                    case 1: // 点曲线
                        mHandWritingGeometryView.setDrawType(DrawType.DASH);
                        break;
                    case 2: // 直线
                        mHandWritingGeometryView.setDrawType(DrawType.LINE);
                        break;
                    case 3: // 点直线
                        mHandWritingGeometryView.setDrawType(DrawType.DASH_LINE);
                        break;
                    case 4: // 箭头
                        mHandWritingGeometryView.setDrawType(DrawType.ARROW);
                        break;
                    case 5: // 三角形
                        mHandWritingGeometryView.setDrawType(DrawType.TRIANGLE);
                        break;
                    case 6: // 矩形
                        mHandWritingGeometryView.setDrawType(DrawType.RECTANGLE);
                        break;
                    case 7: // 梯形
                        mHandWritingGeometryView.setDrawType(DrawType.TRAPEZIUM);
                        break;
                    case 8: // 椭圆
                        mHandWritingGeometryView.setDrawType(DrawType.OVAL);
                        break;
                    case 9: // 坐标系
                        mHandWritingGeometryView.setDrawType(DrawType.COORDINATE);
                        break;
                    case 10: // 数轴
                        mHandWritingGeometryView.setDrawType(DrawType.NUMBER_AXIS);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @OnClick({R.id.add_btn, R.id.subtract_btn, R.id.save_strokes_btn,
            R.id.restore_btn, R.id.delete_btn, R.id.clear_btn,
            R.id.add_scale_btn, R.id.clear_scale_btn})
    public void onClick(View view) {
        if (mHandWritingGeometryView == null) {
            return;
        }
        switch (view.getId()) {
            case R.id.add_btn: // 增大手写区
                addWriteArea(mHandWritingGeometryView);
                break;
            case R.id.subtract_btn: // 减小手写区
                subtractWriteArea(mHandWritingGeometryView);
                break;
            case R.id.save_strokes_btn: // 保存笔迹
                FileOutputStream fos = null;
                try {
                    File file = new File(STROKE_PARENT_PATH);
                    if (!file.exists()) {
                        boolean mkdirs = file.mkdirs();
                        if (!mkdirs) {
                            return;
                        }
                    }

                    String strokes = mHandWritingGeometryView.getStrokes();
                    if (TextUtils.isEmpty(strokes)) {
                        CustomToastUtil.showInfoToast("笔迹内容为空!没有需要保存的笔迹!");
                        return;
                    }

                    fos = new FileOutputStream(STROKE_FILE_PATH);
                    fos.write(strokes.getBytes()); //写入本地
                    CustomToastUtil.showSuccessToast("笔迹保存成功!");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case R.id.restore_btn: // 还原笔迹
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(STROKE_FILE_PATH);
                    byte[] bytes = new byte[8192];
                    int len;
                    StringBuilder sb = new StringBuilder();
                    while ((len = fis.read(bytes)) != -1) {
                        sb.append(new String(bytes, 0, len));
                    }
                    String strokes = sb.toString();
//                    某些设备获取event.getPressure()时得到一个野值导致笔迹压力出现一个异常大或者异常小的值，出现某部分手写区块变成了色块

                    if (TextUtils.isEmpty(strokes)) {
                        CustomToastUtil.showErrorToast("没有可以还原的笔迹!");
                        return;
                    }

                    HandWritingViewHelper.getHandWriteViewByStroke(mHandWritingGeometryView, strokes, false);
                } catch (IOException e) {
                    CustomToastUtil.showErrorToast("笔迹文件还没有保存!无法复原笔迹!");
                    e.printStackTrace();
                } finally {
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.delete_btn: // 删除笔迹文件
                File file = new File(STROKE_FILE_PATH);
                if (file.exists() && file.isFile()) {
                    if (file.delete()) {
                        CustomToastUtil.showSuccessToast("笔迹文件删除成功!");
                    } else {
                        CustomToastUtil.showErrorToast("笔迹文件删除失败!");
                    }
                } else {
                    CustomToastUtil.showErrorToast("笔迹文件不存在!");
                }
                break;
            case R.id.clear_btn:
                if (mHandWritingGeometryView != null) {
                    mHandWritingGeometryView.clear();
                }
                break;
            case R.id.add_scale_btn: //添加缩放View
                mScaleIv.setImageResource(R.mipmap.wallpaper);
                mHandWritingGeometryView.setScaleView(mScaleIv);
                break;
            case R.id.clear_scale_btn: //移除缩放View
                mScaleIv.setImageDrawable(null);
                mHandWritingGeometryView.setScaleView(null);
                break;
        }
    }

    /**
     * 根据View生成Bitmap
     */
    private Bitmap getViewBitmap(View view) {
        Bitmap bitmap = null;
        if (view.getWidth() > 0 && view.getHeight() > 0) {
            bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
        }
        return bitmap;
    }

    /**
     * 第一等级
     */
    int LEVEL_ONE = 1;

    /**
     * 第二等级
     */
    int LEVEL_TWO = 2;
    /**
     * 第三等级
     */
    int LEVEL_THREE = 3;
    int mDefHeight = MyApplication.sMyApplication.getResources().getDimensionPixelSize(R.dimen.dp_500);

    /**
     * 增加手写区域
     */
    private void addWriteArea(HandWritingGeometryView handWritingGeometryView) {
        int currentHeightLevel = getCurrentHeightLevel(handWritingGeometryView);
        if (currentHeightLevel == LEVEL_THREE) {
            Toast.makeText(this, "已达最大高度!", Toast.LENGTH_SHORT).show();
            return;
        }

        currentHeightLevel++;
        if (currentHeightLevel > LEVEL_THREE) {
            currentHeightLevel = LEVEL_THREE;
        }
        resetHandWritingHeight(currentHeightLevel, handWritingGeometryView);
    }

    /**
     * 减少手写区域
     */
    private void subtractWriteArea(HandWritingGeometryView handWritingGeometryView) {
        int currentHeightLevel = getCurrentHeightLevel(handWritingGeometryView);
        if (currentHeightLevel == LEVEL_ONE) {
            Toast.makeText(this, "已达最小高度!", Toast.LENGTH_SHORT).show();
            return;
        }

        currentHeightLevel--;
        if (currentHeightLevel < LEVEL_ONE) {
            currentHeightLevel = LEVEL_ONE;
        }
        resetHandWritingHeight(currentHeightLevel, handWritingGeometryView);
    }

    /**
     * 重置手写控件高度
     *
     * @param level                   高度等级
     * @param handWritingGeometryView 手写控件
     */
    private void resetHandWritingHeight(int level, HandWritingGeometryView handWritingGeometryView) {
        if (handWritingGeometryView != null) {
            boolean isRubber = handWritingGeometryView.isRubber();
            DrawType drawType = handWritingGeometryView.getDrawType();

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) handWritingGeometryView.getLayoutParams();
            layoutParams.height = mDefHeight * level;
            handWritingGeometryView.setLayoutParams(layoutParams);

            //---------现在增减手写控件高度的时候，手写控件会还原笔迹--------------------------
            //还原笔迹
            String strokes = handWritingGeometryView.getStrokes();
            if (!TextUtils.isEmpty(strokes)) {
                handWritingGeometryView.restoreToImage(strokes);
            }

            //还原橡皮类型，必须放在restoreToImage(stokes)方法之后，因为restoreToImage(stokes)内部会重置成笔迹
            if (isRubber) {
                handWritingGeometryView.setToRubber();
            }
            //还原笔迹类型，必须放在restoreToImage(stokes)方法之后
            if (drawType != DrawType.CURVE) {
                handWritingGeometryView.setDrawType(drawType);
            }
        }
    }

    /**
     * 获取当前区域高度级别
     *
     * @return 高度级别
     */
    private int getCurrentHeightLevel(HandWritingGeometryView handWritingGeometryView) {
        if (null != handWritingGeometryView) {
            if (handWritingGeometryView.getmHeight() <= mDefHeight + 10) {
                return LEVEL_ONE;
            } else if (handWritingGeometryView.getmHeight() <= 2 * mDefHeight + 10) {
                return LEVEL_TWO;
            } else {
                return LEVEL_THREE;
            }
        }
        return LEVEL_ONE;
    }
}
