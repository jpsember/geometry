package com.js.geometryapp;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.ViewGroup;

public class GeometryStepperActivity extends GeometryActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mAlgorithmStepper = AlgorithmStepper.sharedInstance();
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

	public void setAlgorithmDelegate(AlgorithmStepper.Delegate delegate) {
		mAlgorithmStepper.setDelegate(delegate);
	}

	protected ViewGroup buildContentView() {
		ViewGroup layout = super.buildContentView();
		layout.addView(mAlgorithmStepper.controllerView(this));
		return layout;
	}

	protected GLSurfaceView buildOpenGLView() {
		GLSurfaceView v = new OurGLSurfaceView(this,
				new AlgorithmRenderer(this));
		v.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		return v;
	}

	private AlgorithmStepper mAlgorithmStepper;
}
