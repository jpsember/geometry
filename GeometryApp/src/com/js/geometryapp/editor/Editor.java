package com.js.geometryapp.editor;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

/**
 * Class that encapsulates editing geometric objects. It includes a view, which
 * contains both a content view to display the objects being edited, as well as
 * floating toolbars.
 */
public class Editor {

	private static final boolean PADDING_BETWEEN_TOOLBAR_AND_CONTAINER = false;
	private static final boolean PADDING_INSIDE_TOOLBAR = true;

	/**
	 * Constructor
	 * 
	 * @param contentView
	 *            view displaying objects being edited; probably a GLSurfaceView
	 */
	public Editor(View contentView) {
		mContentView = contentView;
	}

	public View getView() {
		if (mEditorView == null)
			constructView();
		return mEditorView;
	}

	private Context context() {
		return mContentView.getContext();
	}

	private View buildSampleButton(String label) {
		Button b = new Button(context());
		b.setText(label);
		b.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		return b;
	}

	private void constructView() {
		FrameLayout frameLayout = new FrameLayout(mContentView.getContext());

		// Add the content child view
		FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		frameLayout.addView(mContentView, p);

		// Add toolbar child view
		{
			LinearLayout toolbar = new LinearLayout(frameLayout.getContext());
			if (PADDING_INSIDE_TOOLBAR) {
				// Add a bit of padding between buttons and the toolbar frame
				toolbar.setPadding(10, 10, 10, 10);
			}

			// Give the toolview a transparent gray background
			toolbar.setBackgroundColor(Color.argb(0x40, 0x80, 0x80, 0x80));

			toolbar.addView(buildSampleButton("New"));
			toolbar.addView(buildSampleButton("Save"));
			toolbar.addView(buildSampleButton("Undo"));

			p = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);

			if (PADDING_BETWEEN_TOOLBAR_AND_CONTAINER) {
				// Add space between the toolbar and the content view's boundary
				p.setMargins(20, 20, 20, 20);
			}

			// Place the toolbar in the top right of the content view
			p.gravity = Gravity.RIGHT | Gravity.TOP;
			frameLayout.addView(toolbar, p);
		}
		mEditorView = frameLayout;
	}

	public void render() {
		unimp("rendering " + mObjects);
	}

	private View mContentView;
	private View mEditorView;
	private EdObjectArray mObjects = new EdObjectArray();
}
