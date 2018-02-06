package com.handwriting.demo;

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
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.hand.writing.DrawType;
import com.hand.writing.view.HandWritingGeometryView;
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
    @Bind(R.id.hand_writing_view)
    HandWritingGeometryView mHandWritingView;
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
                if (mHandWritingView == null) {
                    return;
                }
                switch (position) {
                    case 0: // 手写
                        mHandWritingView.setToWriting();
                        break;
                    case 1: // 橡皮
                        mHandWritingView.setToRubber();
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
                if (mHandWritingView == null) {
                    return;
                }
                switch (position) {
                    case 0: // 蓝色
                        mHandWritingView.setPenColor(Color.BLUE);
                        break;
                    case 1: // 红色
                        mHandWritingView.setPenColor(Color.RED);
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
                if (mHandWritingView == null) {
                    return;
                }
                switch (position) {
                    case 0: // 曲线
                        mHandWritingView.setDrawType(DrawType.CURVE);
                        break;
                    case 1: // 点曲线
                        mHandWritingView.setDrawType(DrawType.DASH);
                        break;
                    case 2: // 直线
                        mHandWritingView.setDrawType(DrawType.LINE);
                        break;
                    case 3: // 点直线
                        mHandWritingView.setDrawType(DrawType.DASHLINE);
                        break;
                    case 4: // 箭头
                        mHandWritingView.setDrawType(DrawType.ARROW);
                        break;
                    case 5: // 三角形
                        mHandWritingView.setDrawType(DrawType.TRIANGLE);
                        break;
                    case 6: // 矩形
                        mHandWritingView.setDrawType(DrawType.RECTANGLE);
                        break;
                    case 7: // 梯形
                        mHandWritingView.setDrawType(DrawType.TRAPEZIUM);
                        break;
                    case 8: // 椭圆
                        mHandWritingView.setDrawType(DrawType.OVAL);
                        break;
                    case 9: // 坐标系
                        mHandWritingView.setDrawType(DrawType.COORDINATE);
                        break;
                    case 10: // 数轴
                        mHandWritingView.setDrawType(DrawType.NUMBER_AXIS);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @OnClick({R.id.save_strokes_btn, R.id.restore_btn, R.id.delete_btn, R.id.clear_btn})
    public void onClick(View view) {
        if (mHandWritingView == null) {
            return;
        }
        switch (view.getId()) {
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

                    String strokes = mHandWritingView.getStrokes();
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
//                    String strokes = "768,138,767,90&1.0@0#-65536#767,26,0.3993563;767,26,0.46444008#767,26,767,26=0#-65536#767,90,0.4794976;767,90,0.49926066#767,90,767,90";

                    if (TextUtils.isEmpty(strokes)) {
                        CustomToastUtil.showErrorToast("没有可以还原的笔迹!");
                        return;
                    }

                    mHandWritingView.restoreToImage(strokes);
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
                if (mHandWritingView != null) {
                    mHandWritingView.clear();
                }
                break;
        }
    }
}
