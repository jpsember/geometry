package com.js.geometryapp;

import com.js.android.MyActivity;
import com.js.opengl.OurGLRenderer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
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
		// Hide the title bar, to conserve screen real estate
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);

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

	/**
	 * Subclasses should override this to set the activity's content view. The
	 * default implementation constructs a GLSurfaceView with an OurGLRenderer
	 */
	protected View buildContentView() {
		GLSurfaceView v = new GLSurfaceView(this);
		v.setEGLContextClientVersion(2);
		v.setRenderer(new OurGLRenderer(this));
		mGLView = v;
		return v;
	}

	private GLSurfaceView mGLView;
}
