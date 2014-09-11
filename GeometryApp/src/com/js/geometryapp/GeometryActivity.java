package com.js.geometryapp;

import static com.js.basic.Tools.*;

import com.js.android.MyActivity;
import com.js.geometry.*;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class GeometryActivity extends MyActivity {

	private void testPolygonStuff() {
		GeometryContext c = new GeometryContext(42);
		Polygon p = Polygon.testPolygon(c, Polygon.TESTPOLY_DRAGON_X + 7);
		pr("polygon vertices=" + p.numVertices());
		pr("area=" + p.area());
		pr("boundary length=" + p.boundaryLength());
		pr("bounds=" + p.bounds());

		PolygonTriangulator pt = PolygonTriangulator.triangulator(c, p);
		pt.triangulate();
		mSampleContext = c;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (false)
			testPolygonStuff();

		if (savedInstanceState != null) {
			restorePreviousSavedState(savedInstanceState);
		}

		if (supportsOpenGL20()) {
			setContentView(buildContentView());
		} else {
			Toast.makeText(this, "This device does not support OpenGL ES 2.0",
					Toast.LENGTH_LONG).show();
		}
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

	private void restorePreviousSavedState(Bundle savedInstanceState) {
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

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

	private View buildContentView() {
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);

		buildOpenGLView();
		// layout.addView(buildTestView());
		layout.addView(mGLView);

		mStepperView = buildControlsView();
		layout.addView(mStepperView.view());
		// layout.addView(buildTestView());
		return layout;
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

	private void buildOpenGLView() {
		mGLView = new OurGLSurfaceView(this);
		mGLView.setSampleContext(mSampleContext);
		if (true)
			mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mGLView.setLayoutParams(layoutParams(false, true));
	}

	private AlgorithmStepperView buildControlsView() {
		AlgorithmStepperView v = new AlgorithmStepperView(this);
		return v;
	}

	/* private */View buildTestView() {
		View v;

		// Using 'View' here creates views that try to be very large for
		// some reason; so use LinearLayout as the basic test view
		// so treat LinearLayout as the fundamental
		v = new LinearLayout(this);
		v.setLayoutParams(layoutParams(false, false));
		v.setMinimumHeight(50);
		v.setBackgroundColor(sTestViewColors[mTestViewCount
				% sTestViewColors.length]);

		mTestViewCount++;
		return v;
	}

	private static int sTestViewColors[] = { Color.DKGRAY, Color.GREEN,
			Color.BLUE, Color.MAGENTA };

	private int mTestViewCount;
	private OurGLSurfaceView mGLView;
	private GeometryContext mSampleContext;
	private AlgorithmStepperView mStepperView;
}
