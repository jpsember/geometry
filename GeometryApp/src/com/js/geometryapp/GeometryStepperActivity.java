package com.js.geometryapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

public abstract class GeometryStepperActivity extends GeometryActivity {

	public GeometryStepperActivity() {
		mStepper = new AlgorithmStepper();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addAlgorithms(getStepper());
		getStepper().begin();
	}

	public abstract void addAlgorithms(AlgorithmStepper s);

	@Override
	protected void onPause() {
		mOptions.persistStepperState(false);
		super.onPause();
	}

	@Override
	protected View buildContentView() {
		mOptions = new AlgorithmOptions(this, getStepper());
		getStepper().setOptions(mOptions);

		// Have superclass construct the OpenGL view
		View glView = super.buildContentView();
		getStepper().setGLSurfaceView(getGLSurfaceView());

		// Wrap it in a containing view
		LinearLayout mainView = new LinearLayout(this);
		{
			mainView.setOrientation(LinearLayout.VERTICAL);
			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			p.weight = 1;
			mainView.addView(glView, p);
		}
		// Add the stepper control panel to this container
		mainView.addView(getStepper().controllerView());
		// Make the container the main view of a TwinViewContainer
		TwinViewContainer twinViews = new TwinViewContainer(this, mainView);
		mOptions.prepareViews(twinViews.getAuxilliaryView());
		return twinViews.getContainer();
	}

	@Override
	protected final GLSurfaceView buildOpenGLView() {
		GLSurfaceView v = new OurGLSurfaceView(this, buildRenderer(this));
		v.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		return v;
	}

	/**
	 * Subclass can override this method to build their own renderer
	 */
	protected AlgorithmRenderer buildRenderer(Context context) {
		return new AlgorithmRenderer(context, getStepper());
	}

	public AlgorithmStepper getStepper() {
		return mStepper;
	}

	static {
		doNothing();
	}

	private AlgorithmStepper mStepper;
	private AlgorithmOptions mOptions;
}
