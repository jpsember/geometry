package com.js.android;

import com.js.basic.Files;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Tools extends com.js.basic.Tools {

  /**
   * If true, views can be wrapped with diagnostic labels and colors
   */
	public static final boolean DEBUG_VIEWS = false;

	public static final int DEVICESIZE_UNKNOWN = 0;
	public static final int DEVICESIZE_SMALL = 1;
	public static final int DEVICESIZE_NORMAL = 2;
	public static final int DEVICESIZE_LARGE = 3;
	public static final int DEVICESIZE_XLARGE = 4;

	public static void assertUIThread() {
		if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
			die("not running within UI thread");
		}
	}

	public static void assertNotUIThread() {
		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			die("unexpectedly running within UI thread");
		}
	}

	/**
	 * Construct an Intent for starting an activity
	 * 
	 * @param context
	 *            current activity's context
	 * @param theClass
	 *            the activity's class
	 * @return intent
	 */
	public static Intent startIntentFor(Context context, Class theClass) {
		return new Intent(context, theClass);
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
	 * Debug purposes only; get description of an activity's intent
	 * 
	 * @param activity
	 * @return
	 */
	public static String dumpIntent(Activity activity) {
		StringBuilder sb = new StringBuilder(nameOf(activity) + " Intent:");

		Intent intent = activity.getIntent();

		Bundle bundle = intent.getExtras();
		for (String key : bundle.keySet()) {
			Object value = bundle.get(key);
			sb.append("  " + key + " : " + describe(value));
		}
		return sb.toString();
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

	public static int getDeviceSize(Context context) {
		int screenLayout = context.getResources().getConfiguration().screenLayout;
		screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;
		switch (screenLayout) {
		case Configuration.SCREENLAYOUT_SIZE_SMALL:
			return DEVICESIZE_SMALL;
		case Configuration.SCREENLAYOUT_SIZE_NORMAL:
			return DEVICESIZE_NORMAL;
		case Configuration.SCREENLAYOUT_SIZE_LARGE:
			return DEVICESIZE_LARGE;
		case 4: // Configuration.SCREENLAYOUT_SIZE_XLARGE is API >= 9
			return DEVICESIZE_XLARGE;
		default:
			return DEVICESIZE_UNKNOWN;
		}
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

	public static View wrapView(View view) {
		return wrapView(view, null);
	}

	public static View wrapView(View view, String title) {
		if (!DEBUG_VIEWS)
			return view;

		// If view already has a parent, something odd is happening
		if (view.getParent() != null) {
			pr("wrapView, already has parent: " + describe(view.getParent()));
			ViewGroup p = (ViewGroup) view.getParent();
			p.removeView(view);
		}

		LinearLayout f = new LinearLayout(view.getContext());
		f.setOrientation(LinearLayout.VERTICAL);
		final int PADDING = 14;
		f.setPadding(PADDING, PADDING, PADDING, PADDING);
		if (title != null) {
			TextView tv = new TextView(view.getContext());
			tv.setText(title);
			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			p.gravity = Gravity.CENTER_HORIZONTAL;
			f.addView(tv, p);
		}

		f.addView(view);
		debugChangeBgndColor(f);
		return f;
	}

	private static String debugColorCycle[] = { "#402020", "#204020",
			"#202040", "#602020", "#206020", "#202060", "#802020", "#208020",
			"#202080", };

	private static int mDebugBgndColorIndex;
}
