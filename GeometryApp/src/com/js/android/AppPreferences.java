package com.js.android;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Simpler interface to application preferences
 * 
 */
public class AppPreferences {

	/**
	 * No objects are to be constructed
	 */
	private AppPreferences() {
	}

	/**
	 * This should be called once before any other methods
	 * 
	 * @param context
	 *            application or activity context (treated differently in test
	 *            mode)
	 */
	public synchronized static void prepare(Context context) {
		if (preferences != null)
			return;

		if (context instanceof Activity) {
			preferences = ((Activity) context)
					.getPreferences(Context.MODE_PRIVATE);
		} else {
			preferences = context.getSharedPreferences("__RBuddyApp_test_",
					Context.MODE_PRIVATE);
		}
	}

	public static int getUniqueIdentifier(String key) {
		synchronized (preferences) {
			int value = preferences.getInt(key, 1000);
			preferences.edit().putInt(key, 1 + value).commit();
			return value;
		}
	}

	/**
	 * Read string from app preferences
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static String getString(String key, String defaultValue) {
		return preferences.getString(key, defaultValue);
	}

	/**
	 * Store string
	 * 
	 * @param key
	 * @param value
	 */
	public static void putString(String key, String value) {
		preferences.edit().putString(key, value).commit();
	}

	/**
	 * Read boolean from app preferences
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBoolean(String key, boolean defaultValue) {
		return preferences.getBoolean(key, defaultValue);
	}

	/**
	 * Store boolean
	 * 
	 * @param key
	 * @param value
	 */
	public static void putBoolean(String key, boolean value) {
		preferences.edit().putBoolean(key, value).commit();
	}

	/**
	 * Store a series of string values (an optimization to allow a single commit
	 * at the end)
	 * 
	 * @param map
	 *            map with string values
	 */
	public static void putStrings(Map<String, String> map) {
		SharedPreferences.Editor editor = preferences.edit();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			editor.putString(entry.getKey(), entry.getValue());
		}
		editor.commit();
	}

	public static void removeKey(String preferencesKey) {
		preferences.edit().remove(preferencesKey).commit();
	}

	public static boolean toggle(String preferencesKey) {
		boolean val = !getBoolean(preferencesKey, false);
		putBoolean(preferencesKey, val);
		return val;
	}

	private static SharedPreferences preferences;

}
