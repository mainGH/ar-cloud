package org.ar.pay.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {
    public static final String yyyyMMdd = "yyyy/MM/dd";
    public static final String yyyy_MM_dd = "yyyy-MM-dd";
    public static final String HH_mm_ss = "HH:mm:ss";
    public static final String yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";
    public static final String yyyyMMddHHmmss = "yyyyMMddHHmmss";
    public static final String yyyyMMddHHmmssfff = "yyyyMMddHHmmssSSS";
    public static final String yyyy_MM_dd_HH_mm_ss_fff = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String yyyy_MM_dd_HH_mm_ss_fffffff = "yyyy-MM-dd HH:mm:ss.SSSSSSS";
    private static long nd = 1000 * 24 * 60 * 60;//每天毫秒数
    private static long nh = 1000 * 60 * 60;//每小时毫秒数
    private static long nm = 1000 * 60;//每分钟毫秒数

    public static final Date DEFAULT_DATE = parseDate("1970-01-01");

    private final static TimeZone timeZone = TimeZone.getTimeZone("GMT+08:00");


    /**
     * 获取日期的格式化字符串
     *
     * @param date    待格式化日期
     * @param pattern 格式模式，可选DateUtil的公开静态定义
     * @return
     */
    public static String formatDate(Date date, String pattern) {
        try {
            return getDateFormat(pattern).format(date);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 获取日期的格式化字符串(默认格式:yyyy-MM-dd)
     *
     * @param date 待格式化日期
     * @return
     */
    public static String formatDate(Date date) {
        return formatDate(date, yyyy_MM_dd);
    }

    /**
     * 将日期字符串转换成日期对象
     *
     * @param dateStr 待转换字符串
     * @param pattern 格式模式，可选DateUtil的公开静态定义
     * @return
     * @throws ParseException
     */
    public static Date parseDate(String dateStr,  String pattern) {
        try {
            return getDateFormat(pattern).parse(dateStr);
        } catch (ParseException ex) {
            return null;
        }
    }

    /**
     * 将日期字符串转换成日期对象(默认格式:yyyy-MM-dd)
     *
     * @param dateStr 待转换字符串
     * @return
     * @throws ParseException
     */
    public static Date parseDate(String dateStr) {
        return parseDate(dateStr, yyyy_MM_dd);
    }

    /**
     * 根据生日判断是否为成年人
     *
     * @param birthday
     * @return
     */
    public static boolean isAdult(Date birthday) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.add(Calendar.YEAR, -18);
        return calendar.getTime().after(birthday);
    }

    private static DateFormat getDateFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    /**
     * 指定日期 进行 指定类型 加减
     *
     * @param date         指定日期,可选，默认当前时间
     * @param calendarType Calendar指定类型 如： 分钟用 Calendar.MINUTE
     * @param no           加减数
     * @return Date 计算后的日期
     */
    public static Date dateCalculation(Date date, int calendarType, int no) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);
        calendar.add(calendarType, no);
        return calendar.getTime();
    }

    /**
     * 通过时间秒毫秒数判断两个时间间隔天数（满24小时进1天）
     *
     * @param date1 被减时间
     * @param date2
     * @return
     */
    public static int differentDaysByMillisecond(Date date1, Date date2) {
        return (int) ((date2.getTime() - date1.getTime()) / (1000 * 3600 * 24));
    }

    /**
     * 获取指定时间的小时数
     *
     * @param date
     * @return
     */
    public static int getHours(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取指定时间的分钟
     *
     * @param date
     * @return
     */
    public static int getMinutes(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MINUTE);
    }
}
