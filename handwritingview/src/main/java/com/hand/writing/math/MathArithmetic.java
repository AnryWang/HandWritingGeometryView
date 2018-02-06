package com.hand.writing.math;

import android.graphics.Point;

import java.text.DecimalFormat;

/**
 * Desc: 算法工具类
 * Copyright: Copyright (c) 2016
 *
 * @author JiLin
 * @version 1.0
 * @since 2017/10/24 0024
 */

public class MathArithmetic {
    // 保留小数点后两位
    public static DecimalFormat sDecimalFormat = new DecimalFormat("#.00");

    /**
     * return a+f*(b-a)根据f系数返回a-b之间的值
     */
    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    /**
     * 矢量旋转函数
     *
     * @param px      x分量
     * @param py      y分量
     * @param ang     旋转角
     * @param isChLen 是否改变长度
     * @param newLen  新长度
     * @return
     */
    public static double[] rotateVec(float px, float py, double ang,
                                     boolean isChLen, double newLen) {
        double param[] = new double[2];
        double vx = px * Math.cos(ang) - py * Math.sin(ang);
        double vy = px * Math.sin(ang) + py * Math.cos(ang);
        if (isChLen) {
            double d = Math.sqrt(vx * vx + vy * vy);
            vx = vx / d * newLen;
            vy = vy / d * newLen;
            param[0] = vx;
            param[1] = vy;
        }
        return param;
    }

    /**
     * 计算两点之间的距离
     */
    public static double lineSpace(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    /**
     * 计算点到线段所在直线的距离
     */
    public static double pointToLine(int x1, int y1, int x2, int y2, int x0, int y0) {
        double space;
        double a, b, c;
        a = lineSpace(x1, y1, x2, y2);// 线段的长度
        b = lineSpace(x1, y1, x0, y0);// (x1,y1)到点的距离
        c = lineSpace(x2, y2, x0, y0);// (x2,y2)到点的距离
        if (c <= 0.000001 || b <= 0.000001) {
            space = 0;
            return space;
        }
        if (a <= 0.000001) {
            space = b;
            return space;
        }
        if (c * c >= a * a + b * b) {
            space = b;
            return space;
        }
        if (b * b >= a * a + c * c) {
            space = c;
            return space;
        }
        double p = (a + b + c) / 2;// 半周长
        double s = Math.sqrt(p * (p - a) * (p - b) * (p - c));// 海伦公式求面积
        space = 2 * s / a;// 返回点到线的距离（利用三角形面积公式求高）
        return space;
    }

    /* 判断两条线段是否相交begin */
    public static int direction(Point p0, Point p1, Point p2) {
        return ((p2.x - p0.x) * (p1.y - p0.y) - (p1.x - p0.x) * (p2.y - p0.y));
    }

    /**
     * 叉积
     */
    public static double mult(Point a, Point b, Point c) {
        return (a.x - c.x) * (b.y - c.y) - (b.x - c.x) * (a.y - c.y);
    }

    /**
     * aa, bb为一条线段两端点 cc, dd为另一条线段的两端点 相交返回true, 不相交返回false
     */
    public static boolean intersect1(Point aa, Point bb, Point cc, Point dd) {
        return Math.max(aa.x, bb.x) >= Math.min(cc.x, dd.x) &&
                Math.max(aa.y, bb.y) >= Math.min(cc.y, dd.y) &&
                Math.max(cc.x, dd.x) >= Math.min(aa.x, bb.x) &&
                Math.max(cc.y, dd.y) >= Math.min(aa.y, bb.y) &&
                mult(cc, bb, aa) * mult(bb, dd, aa) >= 0 &&
                mult(aa, dd, cc) * mult(dd, bb, cc) >= 0;
    }
    /* 判断两条线段是否相交end */
}
