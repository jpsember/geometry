package com.js.android;

import com.js.basic.Files;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import static com.js.basic.Tools.*;

public final class Tools {

	/**
	 * A do-nothing method that can be called to avoid 'unused import' warnings
	 * related to this class
	 */
	public static final void doNothingAndroid() {
	}

	public static void assertUIThread() {
		if (DEBUG_ONLY_FEATURES) {
			if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
				die("not running within UI thread");
			}
		}
	}

	public static void assertNotUIThread() {
		if (DEBUG_ONLY_FEATURES) {
			if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
				die("unexpectedly running within UI thread");
			}
		}
	}

	/**
	 * Display a yes/no dialog box to confirm an operation. For test purposes
	 * only (uses fixed English yes/no strings)
	 * 
	 * @param activity
	 * @param warningMessage
	 *            message to display
	 * @param operation
	 *            operation to perform if user selects 'yes'
	 */
	public static void confirmOperation(Context context, String warningMessage,
			final Runnable operation) {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					operation.run();
					break;
				}
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(warningMessage)
				.setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener).show();
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

	/**
	 * Debug utility to set the background color for a view. Each call
	 * increments a counter to produce a unique color (from a finite set)
	 */
	public static void debugChangeBgndColor(View view) {
		assertUIThread();
		int i = mDebugBgndColorIndex % debugColorCycle.length;
		view.setBackgroundColor(Color.parseColor(debugColorCycle[i]));
		mDebugBgndColorIndex++;
	}

	private static String debugColorCycle[] = { "#402020", "#204020",
			"#202040", "#602020", "#206020", "#202060", "#802020", "#208020",
			"#202080", };

	private static int mDebugBgndColorIndex;
}
