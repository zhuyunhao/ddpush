package org.ddpush.im.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间工具类
 */
public class DateTimeUtil {
	/** 默认时间格式化类型 */
	public static String DEFAULTFORMAT = "yyyy-MM-dd HH:mm:ss";

	/** 按默认格式取得当前时间 */
	public static String getCurDateTime() {
		return getCurDateTime(DEFAULTFORMAT);
	}

	/**
	 * 当前时间
	 * 
	 * @param pattern
	 * @return
	 */
	public static String getCurDateTime(String pattern) {
		return formatCalendar(Calendar.getInstance(), pattern);
	}

	/**
	 * 根据传入日历格式化时间
	 * 
	 * @param calendar
	 * @return
	 */
	public static String formatCalendar(Calendar calendar) {
		return formatCalendar(calendar, DEFAULTFORMAT);
	}

	/**
	 * 根据传入日历和格式取得时间
	 * 
	 * @param calendar
	 * @param pattern
	 * @return
	 */
	public static String formatCalendar(Calendar calendar, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		// sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
		return sdf.format(calendar.getTime());
	}

	/**
	 * 取得年月日
	 * 
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public static Date parseDate(String date) throws ParseException {
		if ("".equals(StringUtil.checkBlankString(date)))
			return null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.parse(date);
	}

	/**
	 * 根据格式取得年月日
	 * 
	 * @param date
	 * @param pattern
	 * @return
	 * @throws ParseException
	 */
	public static Date parseDate(String date, String pattern) throws ParseException {
		if ("".equals(StringUtil.checkBlankString(date)))
			return null;
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.parse(date);
	}

	/**
	 * 根据传入时间和格式取得时间
	 * 
	 * @param date
	 * @param pattern
	 * @return
	 */
	public static String formatDate(Date date, String pattern) {
		if (date == null)
			return "";
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return formatCalendar(calendar, pattern);
	}

	/**
	 * 根据传入时间和默认格式取得时间
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDate(Date date) {
		return formatDate(date, DEFAULTFORMAT);
	}

	/**
	 * 传入字符串和格式取得日历
	 * 
	 * @param dateStr
	 * @param pattern
	 * @return
	 * @throws ParseException
	 */
	public static Calendar parseString(String dateStr, String pattern) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		Date date = sdf.parse(dateStr);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}

	/**
	 * 用新格式，格式化时间
	 * 
	 * @param dateStr
	 * @param pattern
	 * @param newPattern
	 * @return
	 * @throws ParseException
	 */
	public static String convertDateTimeStrFormat(String dateStr, String pattern, String newPattern) throws ParseException {
		return DateTimeUtil.formatCalendar(DateTimeUtil.parseString(dateStr, pattern), newPattern);
	}

	/**
	 * 用默认格式化取得日历
	 * 
	 * @param dateStr
	 * @return
	 * @throws ParseException
	 */
	public static Calendar parseString(String dateStr) throws ParseException {
		return parseString(dateStr, DEFAULTFORMAT);
	}

}
