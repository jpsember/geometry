package com.js.geometryapp;

import com.js.android.UITools;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

public abstract class GeometryStepperActivity extends GeometryActivity {

	public GeometryStepperActivity() {
		mStepper = new ConcreteStepper();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addAlgorithms(mStepper);
		mStepper.begin();
	}

	public abstract void addAlgorithms(AlgorithmStepper s);

	@Override
	protected void onPause() {
		mOptions.persistStepperState(false);
		super.onPause();
	}

	@Override
	protected View buildContentView() {
		// Have superclass construct the OpenGL view
		View glView = super.buildContentView();
		mStepper.setGLSurfaceView(getGLSurfaceView());

		// Wrap it in a containing view
		LinearLayout mainView = new LinearLayout(this);
		{
			mainView.setOrientation(LinearLayout.VERTICAL);
			LinearLayout.LayoutParams p = UITools.layoutParams(false);
			p.weight = 1;
			mainView.addView(glView, p);
		}
		mOptions = mStepper.constructOptions(this);

		// Add the stepper control panel to this container
		mainView.addView(mStepper.controllerView());
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
		return new AlgorithmRenderer(context, mStepper);
	}

	public AlgorithmStepper getStepper() {
		return mStepper;
	}

	static {
		doNothing();
	}

	private ConcreteStepper mStepper;
	private AlgorithmOptions mOptions;
}
