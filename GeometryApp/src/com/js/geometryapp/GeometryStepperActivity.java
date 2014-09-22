package com.js.geometryapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import static com.js.basic.Tools.*;

public abstract class GeometryStepperActivity extends GeometryActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		doNothing();
		mAlgorithmStepper = AlgorithmStepper.sharedInstance();
		hideTitle();

		super.onCreate(savedInstanceState);
		mAlgorithmStepper.restoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		mAlgorithmStepper.saveInstanceState(outState);
		super.onSaveInstanceState(outState);
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
		prepareOptions();
		return mOptions.getView();
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
