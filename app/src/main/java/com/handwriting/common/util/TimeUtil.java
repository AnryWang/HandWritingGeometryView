package com.handwriting.common.util;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Desc: 时间工具类
 * Copyright: Copyright (c) 2016
 *
 * @author JiLin
 */
public class TimeUtil {

    public static final String YEAR_2_DAY = "yyyy-MM-dd";
    public static final String YEAR_2_SECOND = "yyyy-MM-dd HH:mm:ss";
    public static final String YEAR_2_SECOND2 = "yyyy-MM-dd-HH-mm-ss";
    public static final String YEAR_2_MINUTE = "yyyy-MM-dd HH:mm";
    public static final String YEAR_2_MINUTE2 = "yyyy-MM-dd-HH-mm";
    public static final String MONTH_2_DAY = "MM-dd";
    public static final String HOUR_2_MINU = "HH:mm";
    public static final String HOUR_2_MINU2 = "H:mm";
    public static final String YEAR_2_DAY_CHN = "yyyy年MM月dd日";
    public static final String MONTH_2_MINUTE = "MM-dd HH:mm";

    private static HashMap<String, SimpleDateFormat> formats = new HashMap();

    static {
        formats.put(YEAR_2_DAY, new SimpleDateFormat(YEAR_2_DAY, Locale.getDefault()));
        formats.put(YEAR_2_SECOND, new SimpleDateFormat(YEAR_2_SECOND, Locale.getDefault()));
        formats.put(YEAR_2_SECOND2, new SimpleDateFormat(YEAR_2_SECOND2, Locale.getDefault()));
        formats.put(YEAR_2_MINUTE, new SimpleDateFormat(YEAR_2_MINUTE, Locale.getDefault()));
        formats.put(YEAR_2_MINUTE2, new SimpleDateFormat(YEAR_2_MINUTE2, Locale.getDefault()));
        formats.put(MONTH_2_DAY, new SimpleDateFormat(MONTH_2_DAY, Locale.getDefault()));
        formats.put(HOUR_2_MINU, new SimpleDateFormat(HOUR_2_MINU, Locale.getDefault()));
        formats.put(HOUR_2_MINU2, new SimpleDateFormat(HOUR_2_MINU2, Locale.getDefault()));
        formats.put(YEAR_2_DAY_CHN, new SimpleDateFormat(YEAR_2_DAY_CHN, Locale.getDefault()));
        formats.put(MONTH_2_MINUTE, new SimpleDateFormat(MONTH_2_MINUTE, Locale.getDefault()));
    }

    /**
     * 获取当前日期字符串
     *
     * @return 返回当前日期字符串表示, 格式为"yyyy-MM-dd"
     */
    public static String getCurrentDayStr() {
        Calendar calendar = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%02d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * 根据给定的时间毫秒值获取时间字符串,格式为"yyyy:MM:dd"
     *
     * @param millis 给定的时间毫秒值
     * @return 返回时间字符串
     */
    public static String millsToTimeStr(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * 是否是今天
     *
     * @param date 待判断的日期
     * @return 今天返回true, 否则返回false
     */
    public static boolean isToday(Date date) {
        String curDay = getCurrentDayStr();
        String otherDay = getYea2DayFromDate(date);
        if (curDay.equals(otherDay)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 把对应的日期字符串转换为Date
     *
     * @param dateStr 日期字符串,格式必须为"yyyy-MM-dd"
     * @return 字符串转换为Date类型数据
     */
    public static Date getDayDateFromStr(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) {
            return new Date();
        }
        Date date = null;
        SimpleDateFormat simpleDateFormat = formats.get(YEAR_2_DAY);
        try {
            date = simpleDateFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 把对应的日期字符串转换为Date
     *
     * @param dateStr 日期字符串,格式必须为"yyyy-MM-dd HH:mm:ss"
     * @return 对应Date
     */
    public static Date getDaySecondFromStr(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) {
            return new Date();
        }
        Date date = null;
        SimpleDateFormat simpleDateFormat = formats.get(YEAR_2_SECOND);
        try {
            date = simpleDateFormat.parse(dateStr);
        } catch (ParseException var5) {
            var5.printStackTrace();
        }
        return date;
    }

    /**
     * 把对应的日期字符串转换为Date
     *
     * @param dateStr 日期字符串,格式必须为"yyyy-MM-dd HH:mm"
     * @return 对应Date
     */
    public static Date getDayMinFromStr(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) {
            return new Date();
        }
        Date date = null;
        SimpleDateFormat simpleDateFormat = formats.get(YEAR_2_MINUTE);
        try {
            date = simpleDateFormat.parse(dateStr);
        } catch (ParseException var5) {
            var5.printStackTrace();
        }
        return date;
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 格式为"yyyy-MM-dd"
     */
    public static String getYea2DayFromDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(YEAR_2_DAY);
        return simpleDateFormat.format(date);
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 格式为"yyyy年MM月dd日"
     */
    public static String getYea2DayCHNFromDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(YEAR_2_DAY_CHN);
        return simpleDateFormat.format(date);
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 格式为"yyyy-MM-dd HH:mm:ss"
     */
    public static String getYear2SecondFromDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(YEAR_2_SECOND);
        return simpleDateFormat.format(date);
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 格式为"yyyy-MM-dd-HH-mm-ss"
     */
    public static String getYear2Second2FromDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(YEAR_2_SECOND2);
        return simpleDateFormat.format(date);
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 格式为"yyyy-MM-dd HH:mm"
     */
    public static String getYear2MinuteFromDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(YEAR_2_MINUTE);
        return simpleDateFormat.format(date);
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 格式为"yyyy-MM-dd-HH-mm"
     */
    public static String getYear2Minute2FromDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(YEAR_2_MINUTE2);
        return simpleDateFormat.format(date);
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 格式为"MM-dd"
     */
    public static String getMonth2DayFromDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(MONTH_2_DAY);
        return simpleDateFormat.format(date);
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 格式为"HH:mm"
     */
    public static String getHour2MinuFromDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(HOUR_2_MINU);
        return simpleDateFormat.format(date);
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 没有前面的0 比如09:11 => 9:11,格式为"H:mm"
     */
    public static String getHour2MinuFromDate2(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(HOUR_2_MINU2);
        return simpleDateFormat.format(date);
    }

    /**
     * @param date 待显示的date类型
     * @return 返回日期字符串, 格式为"MM-dd HH:mm"
     */
    public static String getMonth2MinuteFromDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat simpleDateFormat = formats.get(MONTH_2_MINUTE);
        return simpleDateFormat.format(date);
    }

    /**
     * 比较当前时间和指定时间的差距,单位为"分钟"
     *
     * @param startTimeStr 待比较的日期
     * @return 返回当前时间和指定时间的差, 单位为"分钟"
     */
    public static int compareTimeMinute(Date startTimeStr) {
        long currentTime = System.currentTimeMillis();
        long startTime = startTimeStr.getTime();
        long date = currentTime - startTime;
        int minutes = (int) date / (1000 * 60);
        return minutes;
    }

    /**
     * 格式化语音时长为指定字符串形式
     *
     * @param delta 待格式化的语音持续时长,单位为"秒"
     * @return 返回格式化后的时长字符串;如果超过一个小时,格式为02:40'45",否则格式为04'45"
     */
    public static String getFormatTimeLength(long delta) {
        if (delta < 0) {
            return "已结束";
        } else {
            long days = delta / (60 * 60 * 24);
            long hours = (delta - days * (60 * 60 * 24)) / (60 * 60);
            long minutes = (delta - days * (60 * 60 * 24) - hours * (60 * 60)) / (60);
            long second = (delta - days * (60 * 60 * 24) - hours * (60 * 60) - minutes * (60));
            String s;
            if (hours > 0) {
                s = String.format(Locale.getDefault(), "%02d:%02d'%02d\"", hours, minutes, second);
            } else {
                s = String.format(Locale.getDefault(), "%02d'%02d\"", minutes, second);
            }
            return s;
        }
    }

    /**
     * 比较给定的时间是否在今天之前
     *
     * @param date 待比较的时间
     * @return 给定的时间在今天之前返回true, 否则返回false
     */
    public static boolean isBeforeToday(Date date) {
        Calendar currentCalendar = Calendar.getInstance();
        int curYear = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);
        int day = currentCalendar.get(Calendar.DATE);

        Calendar other = Calendar.getInstance();
        other.setTime(date);
        int otherYear = other.get(Calendar.YEAR);
        int otherMonth = other.get(Calendar.MONTH);
        int otherDay = other.get(Calendar.DATE);

        if (otherYear < curYear) {//1. 比较年
            return true;
        } else if (otherYear == curYear) {
            if (otherMonth < month) {//2. 年相同 比较月
                return true;
            } else if (otherMonth == month) {//3. 年、月相同，比较日
                if (otherDay < day) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 比较给定的时间是否在今天之后
     *
     * @param sr_time 待比较的时间
     * @return 给定的时间在今天之后返回true, 否则返回false
     */
    public static boolean isAfterToday(Date sr_time) {
        Calendar currentCalendar = Calendar.getInstance();
        int curYear = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);
        int day = currentCalendar.get(Calendar.DATE);

        Calendar other = Calendar.getInstance();
        other.setTime(sr_time);
        int otherYear = other.get(Calendar.YEAR);
        int otherMonth = other.get(Calendar.MONTH);
        int otherDay = other.get(Calendar.DATE);

        if (otherYear > curYear) {//1. 比较年
            return true;
        } else if (otherYear == curYear) {
            if (otherMonth > month) {//2. 年相同 比较月
                return true;
            } else if (otherMonth == month) {//3. 年、月相同，比较日
                if (otherDay > day) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param firstDate  第一个时间
     * @param secondDate 第二个时间
     * @return 第一个时间大于第二个时间返回true, 否则返回false
     */
    public static boolean firstLargeThanSecond(@NonNull Date firstDate, @NonNull Date secondDate) {
        return firstDate.getTime() > secondDate.getTime();
    }
}
