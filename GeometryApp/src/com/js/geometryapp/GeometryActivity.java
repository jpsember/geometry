package com.js.geometryapp;

import static com.js.basic.Tools.*;


import com.js.android.MyActivity;
import com.js.geometry.R;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class GeometryActivity extends MyActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		verifySupportsOpenGL20();

		if (savedInstanceState != null) {
			restorePreviousSavedState(savedInstanceState);
		}

		mGLView = new OurGLSurfaceView(this);

		setContentView(mGLView);

	}

	private void verifySupportsOpenGL20() {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
		boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x2000;
		// Note: above may not work on emulator due to bug with GPU emulation;
		// see 'Open GL ES 2 for Android', p.25
		if (!supportsEs2)
			die("OpenGL ES 2.0 not supported");
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
		mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mGLView.onResume();
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

	private OurGLSurfaceView mGLView;
}
