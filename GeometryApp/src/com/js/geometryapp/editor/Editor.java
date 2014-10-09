package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.android.AppPreferences;
import com.js.android.QuiescentDelayOperation;
import com.js.basic.JSONTools;
import com.js.geometry.Point;
import com.js.geometryapp.ConcreteStepper;
import com.js.geometryapp.GeometryStepperActivity;

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
public class Editor implements EditorEventListener {

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
		mDefaultListener = new DefaultEventListener(this);
		prepareObjectTypes();
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

	private Button addObjectTypeButton(String label,
			final EdObjectFactory factory) {
		Button button = buildSampleButton(label);
		button.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				Editor editor = Editor.this;
				editor.startAddObjectOperation(factory);
			}
		});
		return button;
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

			toolbar.addView(addObjectTypeButton("Seg", EdSegment.FACTORY));
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
	public void clearOperation(EditorEventListener operation) {
		if (mCurrentOperation == operation) {
			setOperation(null);
		}
	}

	private void startAddObjectOperation(EdObjectFactory objectType) {
		objects().unselectAll();
		setOperation(null);
		mLastAddObjectOperation = objectType;
		mDefaultListener.setAddObjectOper(objectType);
		if (false) // figure out a way to determine an appropriate toast message
			toast(context(), "Add segment!");
	}

	private void setOperation(EditorEventListener operation) {
		mDefaultListener.setAddObjectOper(null);
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
		mDefaultListener.render(mStepper);
	}

	// EditEventListener interface
	@Override
	public int processEvent(int eventCode, Point location) {

		// If there's a current operation, let it handle it
		if (mCurrentOperation != null) {
			eventCode = mCurrentOperation.processEvent(eventCode, location);
		}

		eventCode = mDefaultListener.processEvent(eventCode, location);

		// Always request a refresh of the editor view
		mStepper.refresh();
		// Set delay to save changes
		persistEditorState(true);

		return eventCode;
	}

	void startAddAnotherOperation() {
		if (mLastAddObjectOperation != null) {
			startAddObjectOperation(mLastAddObjectOperation);
		}
	}

	public EdObjectArray objects() {
		return mObjects;
	}

	private void persistEditorStateAux() {
		try {
			String jsonState = compileObjectsToJSON();
			if (!jsonState.equals(mLastSavedState)) {
				AppPreferences.putString(
						GeometryStepperActivity.PERSIST_KEY_EDITOR, jsonState);
				mLastSavedState = jsonState;
			}
		} catch (JSONException e) {
			warning("caught: " + e);
		}
	}

	public void restoreFromJSON(String script) {
		if (script == null)
			script = "{}";

		try {
			JSONObject map = JSONTools.parseMap(script);
			JSONArray array = map.getJSONArray("objects");
			mObjects.clear();
			for (int i = 0; i < array.length(); i++) {
				if (true) {
					if (i < array.length() - 5) {
						warning("omitting all but last n objects");
						continue;
					}
				}
				JSONObject objMap = array.getJSONObject(i);
				String tag = objMap.getString("type");
				EdObjectFactory factory = mObjectTypes.get(tag);
				if (factory == null) {
					warning("no factory found for: " + tag);
					continue;
				}
				EdObject edObject = factory.parse(objMap);
				mObjects.add(edObject);
			}
		} catch (JSONException e) {
			warning("caught " + e);
		}

	}

	public void persistEditorState(boolean withDelay) {
		if (!withDelay) {
			persistEditorStateAux();
			return;
		}

		// Make a delayed call to persist the values (on the UI thread)
		if (QuiescentDelayOperation.replaceExisting(mPendingFlushOperation)) {
			final float FLUSH_DELAY = 5.0f;
			mPendingFlushOperation = new QuiescentDelayOperation(
					"flush editor", FLUSH_DELAY, new Runnable() {
						public void run() {
							persistEditorStateAux();
						}
					});
		}
	}

	private JSONArray getEdObjectsArrayJSON(EdObjectArray objects) {
		ArrayList values = new ArrayList();
		for (EdObject obj : objects) {
			Map m = obj.getFactory().write(obj);
			values.add(m);
		}
		return new JSONArray(values);
	}

	private String compileObjectsToJSON() throws JSONException {
		JSONObject editorMap = new JSONObject();
		editorMap.put("objects", getEdObjectsArrayJSON(objects()));
		return editorMap.toString();
	}

	private void addObjectType(EdObjectFactory factory) {
		mObjectTypes.put(factory.getTag(), factory);
	}

	private void prepareObjectTypes() {
		mObjectTypes = new HashMap();
		addObjectType(EdSegment.FACTORY);
	}

	public EditorEventListener addNewObject(EdObjectFactory objectType) {
		mObjects.unselectAll();
		EdObject object = objectType.construct();
		object.setSelected(true);
		int slot = mObjects.add(object);

		// Start operation for editing this one
		setOperation(objectType.buildEditorOperation(this, slot));
		return mCurrentOperation;
	}

	private Map<String, EdObjectFactory> mObjectTypes;
	private DefaultEventListener mDefaultListener;
	private EditorEventListener mCurrentOperation;
	private EdObjectFactory mLastAddObjectOperation;
	private View mContentView;
	private View mEditorView;
	private ConcreteStepper mStepper;
	private EdObjectArray mObjects = new EdObjectArray();
	private QuiescentDelayOperation mPendingFlushOperation;
	private String mLastSavedState;

}
