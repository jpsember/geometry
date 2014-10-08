package com.js.geometryapp.editor;

import com.js.geometry.Point;
import com.js.geometryapp.ConcreteStepper;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;
import static com.js.android.Tools.*;

/**
 * Class that encapsulates editing geometric objects. It includes a view, which
 * contains both a content view to display the objects being edited, as well as
 * floating toolbars.
 */
public class Editor implements EditEventListener {

	private static final boolean PADDING_BETWEEN_TOOLBAR_AND_CONTAINER = false;
	private static final boolean PADDING_INSIDE_TOOLBAR = true;

	/**
	 * Constructor
	 * 
	 * @param contentView
	 *            view displaying objects being edited; probably a GLSurfaceView
	 * @param stepper
	 *            ConcreteStepper for rendering editor objects
	 */
	public Editor(View contentView, ConcreteStepper stepper) {
		mContentView = contentView;
		mStepper = stepper;
	}

	public View getView() {
		if (mEditorView == null)
			constructView();
		return mEditorView;
	}

	private Context context() {
		return mContentView.getContext();
	}

	private Button buildSampleButton(String label) {
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

			Button segmentButton = buildSampleButton("Seg");
			segmentButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					Editor.this.startAddObjectOperation(EdSegment
							.buildEditorOperation(Editor.this));
				}
			});
			toolbar.addView(segmentButton);
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

	/**
	 * Clear current operation if it matches a particular one
	 */
	public void clearOperation(EditEventListener operation) {
		if (mCurrentOperation == operation) {
			setOperation(null);
		}
	}

	private void startAddObjectOperation(EditEventListener operation) {
		objects().unselectAll();
		setOperation(operation);
		operation.processEvent(EVENT_ADD_NEW, null);
		mLastAddObjectOperation = operation;
		if (false) // figure out a way to determine an appropriate toast message
			toast(context(), "Add segment!");
	}

	private void setOperation(EditEventListener operation) {
		if (mCurrentOperation != null) {
			mCurrentOperation.processEvent(EVENT_STOP, null);
		}
		mCurrentOperation = operation;
	}

	public void render() {
		for (int i = 0; i < mObjects.size(); i++) {
			EdObject obj = mObjects.get(i);
			obj.render(mStepper);
		}
	}

	// EditEventListener interface
	@Override
	public int processEvent(int eventCode, Point location) {
		if (db)
			pr("Editor processEvent " + eventCode + " loc:" + location);

		// If there's a current operation, let it handle it
		if (mCurrentOperation != null) {
			eventCode = mCurrentOperation.processEvent(eventCode, location);
			if (db)
				pr(" handled by current operation...");
		}

		// If it was handled by the current operation, its code will now be
		// EVENT_NONE; if not, try to handle it now
		if (db) {
			if (eventCode != EVENT_NONE)
				pr(" editor handling event " + eventCode);
		}

		switch (eventCode) {

		// A double tap will add another object of the last type added
		case EVENT_DOWN_MULTIPLE:
			if (db)
				pr("DOWN_MULTIPLE, lastAddObject="
						+ nameOf(mLastAddObjectOperation));
			if (mLastAddObjectOperation != null) {
				startAddObjectOperation(mLastAddObjectOperation);
			}
			break;
		}

		// Always request a refresh of the editor view
		mStepper.refresh();

		return eventCode;
	}

	public EdObjectArray objects() {
		return mObjects;
	}

	private EditEventListener mCurrentOperation;
	private EditEventListener mLastAddObjectOperation;
	private View mContentView;
	private View mEditorView;
	private ConcreteStepper mStepper;
	private EdObjectArray mObjects = new EdObjectArray();

}
