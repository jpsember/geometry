package com.js.android;

import com.js.basic.Files;

import android.content.Context;
import android.widget.Toast;
import static com.js.basic.Tools.*;

public final class Tools {

	/**
	 * A do-nothing method that can be called to avoid 'unused import' warnings
	 * related to this class
	 */
	public static final void doNothingAndroid() {
	}

	/**
	 * Display a toast message of short duration
	 * 
	 * @param context
	 * @param message
	 */
	public static void toast(Context context, String message) {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}

	public static String readTextFileResource(Context context, int resourceId) {
		String str = null;
		try {
			str = Files.readTextFile(context.getResources().openRawResource(
					resourceId));
		} catch (Throwable e) {
			die("problem reading resource #" + resourceId, e);
		}
		return str;
	}

	public static String getStringResource(Context context, String stringName) {
		String packageName = context.getPackageName();
		int resId = context.getResources().getIdentifier(stringName, "string",
				packageName);
		String str = null;
		if (resId != 0)
			str = context.getString(resId);
		if (str == null)
			throw new IllegalArgumentException("string name " + stringName
					+ "  has resource id " + resId + ", no string found");
		return str;
	}

	public static String applyStringSubstitution(Context context, String s) {
		if (s.startsWith("@")) {
			s = getStringResource(context, s.substring(1));
		}
		return s;
	}

}
