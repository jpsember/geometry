package com.js.geometryapp;

import com.js.android.AppPreferences;
import com.js.android.UITools;
import com.js.geometryapp.editor.Editor;
import com.js.geometryapp.editor.EditorGLSurfaceView;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

public abstract class GeometryStepperActivity extends GeometryActivity {

	public static final String PERSIST_KEY_OPTIONS = "_widget_values";
	public static final String PERSIST_KEY_EDITOR = "_editor";

	public GeometryStepperActivity() {
		mEditor = new Editor();
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
		mEditor.persistEditorState(false);
		super.onPause();
	}

	@Override
	protected View buildContentView() {
		// Have superclass construct the OpenGL view
		GLSurfaceView glView = (GLSurfaceView) super.buildContentView();
		mStepper.setGLSurfaceView(glView);

		// Build a view that will contain the GLSurfaceView and a stepper
		// control panel
		LinearLayout mainView = new LinearLayout(this);
		{
			mainView.setOrientation(LinearLayout.VERTICAL);
			LinearLayout.LayoutParams p = UITools.layoutParams(false);
			p.weight = 1;

			// Wrap the GLSurfaceView within another container, so we can
			// overlay it with an editing toolbar
			mEditor.prepare(glView, mStepper);
			mRenderer.setEditor(mEditor);
			mSurfaceView.setEditor(mEditor);
			mainView.addView(mEditor.getView(), p);

			// Restore previous items
			String script = AppPreferences.getString(
					GeometryStepperActivity.PERSIST_KEY_EDITOR, null);
			mEditor.restoreFromJSON(script);
		}
		mOptions = mStepper.constructOptions(this);
		mOptions.setEditor(mEditor);

		// Add the stepper control panel to this container
		mainView.addView(mStepper.controllerView());
		// Make the container the main view of a TwinViewContainer
		TwinViewContainer twinViews = new TwinViewContainer(this, mainView);
		mOptions.prepareViews(twinViews.getAuxilliaryView());
		return twinViews.getContainer();
	}

	@Override
	protected final GLSurfaceView buildOpenGLView() {
		mSurfaceView = new EditorGLSurfaceView(this);
		mRenderer = buildRenderer(this);
		mSurfaceView.setRenderer(mRenderer);
		mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		return mSurfaceView;
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
	private AlgorithmRenderer mRenderer;
	private EditorGLSurfaceView mSurfaceView;
	private Editor mEditor;
}
