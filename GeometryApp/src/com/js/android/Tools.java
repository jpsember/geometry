package com.js.android;

import java.io.InputStream;

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
	 * Display a toast message
	 */
	public static void toast(Context context, String message, int duration) {
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}

	/**
	 * Display a toast message of short duration
	 */
	public static void toast(Context context, String message) {
		toast(context, message, Toast.LENGTH_SHORT);
	}

	/**
	 * Display toast message describing an exception
	 * 
	 * @param context
	 * @param throwable
	 *            exception received
	 * @param optional
	 *            message message to display within toast (may be shown with
	 *            exception message as well)
	 */
	public static void showException(Context context, Throwable exception,
			String message) {
		warning("caught: " + exception);
		if (message == null)
			message = "Caught";
		toast(context, message + ": " + exception, Toast.LENGTH_LONG);
	}

	public static String readTextFileResource(Context context, int resourceId) {
		String str = null;
		try {
			InputStream stream = context.getResources().openRawResource(
					resourceId);
			str = Files.readString(stream);
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
