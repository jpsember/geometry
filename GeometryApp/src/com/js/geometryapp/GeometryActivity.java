package com.js.geometryapp;

import static com.js.basic.Tools.*;

import com.js.android.MyActivity;
import com.js.geometry.*;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

public class GeometryActivity extends MyActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!supportsOpenGL20()) {
			Toast.makeText(this, "This device does not support OpenGL ES 2.0",
					Toast.LENGTH_LONG).show();
			return;
		}

		setContentView(buildContentView());
	}

	/**
	 * Note: may not work on emulator due to bug with GPU emulation; see 'Open
	 * GL ES 2 for Android', p.25
	 */
	private boolean supportsOpenGL20() {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo configurationInfo = activityManager
				.getDeviceConfigurationInfo();
		boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x2000;
		return supportsEs2;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mGLView != null)
			mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mGLView != null)
			mGLView.onResume();
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

	protected ViewGroup buildContentView() {
		LinearLayout mainView = new LinearLayout(this);
		{
			mainView.setOrientation(LinearLayout.VERTICAL);
			mGLView = buildOpenGLView();
			mainView.addView(mGLView, layoutParams(false, true));
		}
		return mainView;
	}

	public static LinearLayout.LayoutParams layoutParams(boolean horizontal,
			boolean fillRemaining) {
		boolean withMargins = false;
		LinearLayout.LayoutParams p;
		if (horizontal) {
			p = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.MATCH_PARENT);
			if (withMargins)
				p.setMargins(20, 4, 20, 4);
		} else {
			p = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			if (withMargins)
				p.setMargins(4, 20, 4, 20);
		}
		p.weight = fillRemaining ? 1 : 0;
		return p;
	}

	protected GLSurfaceView buildOpenGLView() {
		GLSurfaceView v = new OurGLSurfaceView(this, new OurGLRenderer(this));
		return v;
	}

	protected GLSurfaceView getGLSurfaceView() {
		return mGLView;
	}

	private GLSurfaceView mGLView;
}
