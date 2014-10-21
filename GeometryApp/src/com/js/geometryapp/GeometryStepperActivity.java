package com.js.geometryapp;

import com.js.android.AppPreferences;
import com.js.android.UITools;
import com.js.geometryapp.editor.Editor;
import com.js.geometryapp.editor.EditorGLSurfaceView;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public abstract class GeometryStepperActivity extends GeometryActivity {

	public static final String PERSIST_KEY_OPTIONS = "_widget_values";
	public static final String PERSIST_KEY_EDITOR = "_editor";

	public GeometryStepperActivity() {
		// First, we construct the various components; this is analogous to
		// constructing the vertices of the object graph
		mEditor = new Editor();
		mStepper = new ConcreteStepper();
		mOptions = new AlgorithmOptions(this);
		mRenderer = new AlgorithmRenderer(this);

		// Second, we initialize the dependencies; this is analogous to
		// constructing the edges of the object graph
		mEditor.setDependencies(mStepper, mOptions);
		mStepper.setDependencies(mOptions);
		mOptions.setDependencies(mEditor, mStepper);
		mRenderer.setDependencies(mEditor, mStepper);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addAlgorithms(mStepper);
		mStepper.begin();
	}

	public abstract void addAlgorithms(AlgorithmStepper s);

	@Override
	protected void onResume() {
		super.onResume();
		if (mGLView != null)
			mGLView.onResume();
	}

	@Override
	protected void onPause() {
		mOptions.persistStepperState(false);
		mEditor.persistEditorState(false);
		super.onPause();
		if (mGLView != null)
			mGLView.onPause();
	}

	@Override
	protected View buildContentView() {
		EditorGLSurfaceView surfaceView = new EditorGLSurfaceView(this);
		mGLView = surfaceView;
		surfaceView.setRenderer(mRenderer);
		surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mStepper.setGLSurfaceView(surfaceView);

		// Build a view that will contain the GLSurfaceView and a stepper
		// control panel
		LinearLayout mainView = new LinearLayout(this);
		{
			mainView.setOrientation(LinearLayout.VERTICAL);
			LinearLayout.LayoutParams p = UITools.layoutParams(false);
			p.weight = 1;

			// Wrap the GLSurfaceView within another container, so we can
			// overlay it with an editing toolbar
			mEditor.prepare(surfaceView);
			surfaceView.setEditor(mEditor, mStepper);
			mainView.addView(mEditor.getView(), p);

			// Restore previous items
			String script = AppPreferences.getString(
					GeometryStepperActivity.PERSIST_KEY_EDITOR, null);
			mEditor.restoreFromJSON(script);
		}

		buildAuxilliaryView();
		mainView.addView(mAuxView);

		// Add the stepper control panel to this container
		mainView.addView(mStepper.buildControllerView());

		// Make the container the main view of a TwinViewContainer
		TwinViewContainer twinViews = new TwinViewContainer(this, mainView);
		mOptions.prepareViews(twinViews.getAuxilliaryView());
		return twinViews.getContainer();
	}

	private void buildAuxilliaryView() {
		mAuxView = new LinearLayout(this);
	}

	private ConcreteStepper mStepper;
	private AlgorithmOptions mOptions;
	private AlgorithmRenderer mRenderer;
	private Editor mEditor;
	private EditorGLSurfaceView mGLView;
	private LinearLayout mAuxView;
}
