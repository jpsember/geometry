package com.js.geometryapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

public class GeometryStepperActivity extends GeometryActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Construct a new singleton for the algorithm stepper, replacing any
		// old one
		AlgorithmStepper.constructSingleton();
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onPause() {
		AlgorithmOptions options = AlgorithmOptions.sharedInstance();
		options.persistStepperState(false);
		super.onPause();
	}

	private AlgorithmStepper stepper() {
		return AlgorithmStepper.sharedInstance();
	}

	@Override
	protected View buildContentView() {
		AlgorithmOptions options = AlgorithmOptions.construct(this);

		// Have superclass construct the OpenGL view
		View glView = super.buildContentView();
		stepper().setGLSurfaceView(getGLSurfaceView());

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
		mainView.addView(stepper().controllerView(this));
		// Make the container the main view of a TwinViewContainer
		TwinViewContainer twinViews = new TwinViewContainer(this, mainView);
		options.prepareViews(twinViews.getAuxilliaryView());
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
		return new AlgorithmRenderer(context);
	}

	static {
		doNothing();
	}

}
