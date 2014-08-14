package com.js.geometryapp;

import static com.js.android.Tools.*;

import com.js.android.MyActivity;
import com.js.geometry.R;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class GeometryActivity extends MyActivity
{
	private static boolean DEBUG_ORIENTATION = true;

	private static int sOrientation = -1;

	private void initOrientation() {
		if (sOrientation >= 0)
			return;
		sOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
		// TODO: make this a preferences flag
		setRequestedOrientation(sOrientation);
	}

	private void toggleOrientation() {
		sOrientation ^= (ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ^ ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setRequestedOrientation(sOrientation);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (db)
			pr(hey());
		super.onCreate(savedInstanceState);

		if (DEBUG_ORIENTATION)
			initOrientation();

		unimp("set content view");
		// setContentView(getFragmentOrganizer().getContainer(), new
		// LayoutParams(
		// LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		if (savedInstanceState != null) {
			restorePreviousSavedState(savedInstanceState);
		}
	}

	private void restorePreviousSavedState(Bundle savedInstanceState) {
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			unimp("settings");
			return true;
		case R.id.action_testonly_exit:
			android.os.Process.killProcess(android.os.Process.myPid());
			return true;
		case R.id.action_testonly_rotate:
			if (DEBUG_ORIENTATION)
				toggleOrientation();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	public static void setMenuLabel(Menu menu, int menuItemId, String label) {
		MenuItem item = menu.findItem(menuItemId);
		if (item != null) {
			item.setTitle(label);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {

		if (!DEBUG_ORIENTATION)
			menu.removeItem(R.id.action_testonly_rotate);

		// There's a bug in the Android API which causes some items to
		// disappear; specifically, the 'exit' item (the last one) is not
		// appearing. Workaround seems to be to delay this code until after the
		// onPrepareCall() completes...

		runOnUiThread(new Runnable() {
			public void run() {
			}
		});
		return super.onPrepareOptionsMenu(menu);
	}



}
