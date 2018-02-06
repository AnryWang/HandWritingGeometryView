package com.hand.writing;

/**
 * 线条类型
 */
public enum DrawType {
    //绘画线条类型---
    RUBBER(-1),
    CURVE(0),//曲线
    LINE(1), //直线
    DASH(2), //点曲线
    ARROW(3), //箭头
    DASHLINE(4), //点直线
    TRIANGLE(5), //三角形
    RECTANGLE(6), //矩形
    TRAPEZIUM(7), //梯形
    OVAL(8), //椭圆
    COORDINATE(9), //坐标系
    NUMBER_AXIS(10); //数轴

    private int code = 0;

    DrawType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * @return code对于的DrawType枚举
     */
    public static DrawType toDrawType(int code) {
        for (DrawType drawType : DrawType.values()) {
            if (drawType.getCode() == code) {
                return drawType;
            }
        }
        return CURVE;
    }
}
