package com.js.geometryapp;

import com.js.android.AppPreferences;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import static com.js.basic.Tools.*;

public abstract class GeometryStepperActivity extends GeometryActivity {

	private static final String PERSIST_KEY_WIDGET_VALUES = "_widget_values";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		doNothing();
		mAlgorithmStepper = AlgorithmStepper.sharedInstance();
		hideTitle();

		super.onCreate(savedInstanceState);

		// Now that views have been built, restore option values
		mAlgorithmStepper.restoreState();
		prepareOptionsAux();
	}

	@Override
	protected void onPause() {
		mAlgorithmStepper.saveState();

		AlgorithmOptions options = AlgorithmOptions.sharedInstance();
		if (options != null && options.isPrepared()) {
			AppPreferences.putString(PERSIST_KEY_WIDGET_VALUES,
					options.saveValues());
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mAlgorithmStepper.destroy();
		super.onDestroy();
	}

	/**
	 * Hide the title bar, to conserve screen real estate
	 */
	private void hideTitle() {
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
	}

	public void setAlgorithmDelegate(AlgorithmStepper.Delegate delegate) {
		mAlgorithmStepper.setDelegate(delegate);
	}

	protected ViewGroup buildContentView() {
		ViewGroup mainView = super.buildContentView();
		mainView.addView(mAlgorithmStepper.controllerView(this));

		AlgorithmOptions mOptions = AlgorithmOptions.construct(this, mainView);

		return mOptions.getView();
	}

	private void prepareOptionsAux() {
		AlgorithmOptions mOptions = AlgorithmOptions.sharedInstance();
		prepareOptions();
		String mSavedWidgetValues = AppPreferences.getString(
				PERSIST_KEY_WIDGET_VALUES, null);
		if (mSavedWidgetValues != null)
			mOptions.restoreValues(mSavedWidgetValues);
		mOptions.setPrepared(true);
	}

	/**
	 * Override this method to populate the options view with algorithm-specific
	 * controls
	 */
	protected void prepareOptions() {
	}

	protected final GLSurfaceView buildOpenGLView() {
		GLSurfaceView v = new OurGLSurfaceView(this, buildRenderer(this));
		v.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		return v;
	}

	protected abstract AlgorithmRenderer buildRenderer(Context context);

	private AlgorithmStepper mAlgorithmStepper;
}
