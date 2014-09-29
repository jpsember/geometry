package com.js.geometryapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import static com.js.basic.Tools.*;

public class GeometryStepperActivity extends GeometryActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		doNothing();
		mAlgorithmStepper = AlgorithmStepper.sharedInstance();
		hideTitle();

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onPause() {
		AlgorithmOptions options = AlgorithmOptions.sharedInstance();
		if (options != null && options.isPrepared()) {
			options.persistStepperState(false);
		}
		super.onPause();
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
		AlgorithmOptions mOptions = AlgorithmOptions.construct(this, mainView);
		mainView.addView(mAlgorithmStepper.controllerView(this));

		return mOptions.getView();
	}

	protected final GLSurfaceView buildOpenGLView() {
		GLSurfaceView v = new OurGLSurfaceView(this, buildRenderer(this));
		v.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		return v;
	}

	/**
	 * Subclass can override this method to build their own renderer
	 */
	protected AlgorithmRenderer buildRenderer(Context context) {
		return new AlgorithmRenderer(context);
	}

	private AlgorithmStepper mAlgorithmStepper;
}
