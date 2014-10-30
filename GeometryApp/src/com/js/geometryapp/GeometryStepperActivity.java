package com.js.geometryapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.js.android.AppPreferences;
import com.js.android.UITools;
import com.js.basic.Files;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.R;
import com.js.geometryapp.editor.Editor;
import com.js.geometryapp.editor.EditorGLSurfaceView;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;
import static com.js.android.Tools.*;

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
		// Set the theme to appply to the entire application.
		// AppTheme is defined in res/values/styles.xml:
		setTheme(R.style.AppTheme);
		super.onCreate(savedInstanceState);
		addAlgorithms(mStepper);
		mStepper.begin();
		processIntent();
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

			View editorView = mEditor.getView();
			// Place editor view within a container with a black background
			// to emphasize boundary between the editor and the neighbors
			{
				LinearLayout borderView = UITools.linearLayout(this, true);
				borderView.setPadding(2, 2, 2, 2);
				borderView.setBackgroundColor(Color.rgb(128, 128, 128));
				borderView.addView(editorView);
				editorView = borderView;
			}
			mainView.addView(editorView, p);

			// Restore previous items
			String script = AppPreferences.getString(
					GeometryStepperActivity.PERSIST_KEY_EDITOR, null);
			if (script != null)
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

	/**
	 * Process intent; read editor objects from its contents if possible
	 */
	private void processIntent() {
		final boolean db = false && DEBUG_ONLY_FEATURES;
		Intent intent = getIntent();
		if (intent == null)
			return;
		if (db)
			pr("processIntent:\n" + intent);

		String action = intent.getAction();
		String type = intent.getType();
		if (db)
			pr(" action=" + action + "\n type=" + type);
		if (type == null)
			return;
		if (!("application/json".equals(type) || "text/plain".equals(type))) {
			if (db)
				pr(" unexpected type");
			return;
		}

		String jsonContent = null;

		if (Intent.ACTION_SEND.equals(action)) {
			jsonContent = intent.getStringExtra(Intent.EXTRA_TEXT);
		} else if (Intent.ACTION_VIEW.equals(action)) {
			try {
				Uri u = intent.getData();
				String scheme = u.getScheme();
				if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
					// handle as content uri
					InputStream stream = getContentResolver()
							.openInputStream(u);
					jsonContent = Files.readTextFile(stream);
				} else {
					File f = new File(u.getPath());
					jsonContent = Files.readTextFile(f);
				}
			} catch (IOException e) {
				toast(this, "Problem reading file");
				pr(e);
			}
		}
		if (jsonContent == null)
			return;
		mEditor.restoreFromJSON(jsonContent);
	}

	private ConcreteStepper mStepper;
	private AlgorithmOptions mOptions;
	private AlgorithmRenderer mRenderer;
	private Editor mEditor;
	private EditorGLSurfaceView mGLView;
	private LinearLayout mAuxView;
}
