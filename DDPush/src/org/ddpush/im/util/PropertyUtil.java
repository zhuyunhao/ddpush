package org.ddpush.im.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * 配置文件工具类
 */
public class PropertyUtil {

	/** 配置文件默认名 */
	public static final String DEFAULTSET = "ddpush";

	/** 配置缓存map key为配置文件名 */
	protected static HashMap<String, Properties> propertiesSets = new HashMap<String, Properties>();

	private PropertyUtil() {

	}

	/**
	 * 读取默认配置文件
	 */
	protected static void init() {
		init(DEFAULTSET);
	}

	/**
	 * 根据文件名读取配置
	 * 
	 * @param setName
	 */
	protected static void init(String setName) {

		ResourceBundle rb = ResourceBundle.getBundle(setName);
		Properties properties = new Properties();
		// 如果使用Iterator可以使用remove来删除元素
		Enumeration<String> eu = rb.getKeys();
		while (eu.hasMoreElements()) {
			String key = eu.nextElement().trim();
			String value = rb.getString(key).trim();
			try {
				// ISO8859-1英文字符编码
				value = new String(value.getBytes("ISO8859-1"), "UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			}
			properties.put(key.toUpperCase(), value);
		}

		propertiesSets.put(setName, properties);

	}

	/**
	 * 根据key取得配置，文件内的key都为大写
	 * 
	 * @param key
	 * @return
	 */
	public static String getProperty(String key) {
		if (propertiesSets.get(DEFAULTSET) == null) {
			init();
		}
		return propertiesSets.get(DEFAULTSET).getProperty(key.toUpperCase());
	}

	/**
	 * 根据key取得整数型值
	 * 
	 * @param key
	 * @return
	 */
	public static Integer getPropertyInt(String key) {
		int value = 0;
		try {
			value = Integer.parseInt(getProperty(key));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return value;
	}

	/**
	 * 根据key取得浮点型
	 * 
	 * @param key
	 * @return
	 */
	public static Float getPropertyFloat(String key) {
		float value = 0;
		try {
			value = Float.parseFloat(getProperty(key));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return value;
	}

	/**
	 * 根据配置文件名和key取得
	 * 
	 * @param setName
	 * @param key
	 * @return
	 */
	public static String getProperty(String setName, String key) {
		if (propertiesSets.get(setName) == null) {
			init(setName);
		}
		String value = propertiesSets.get(setName).getProperty(key.toUpperCase());
		if (value == null) {
			return "";
		}
		return value;
	}

}
