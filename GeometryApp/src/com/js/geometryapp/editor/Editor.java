package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.android.AppPreferences;
import com.js.android.MyActivity;
import com.js.android.QuiescentDelayOperation;
import com.js.basic.JSONTools;
import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmStepper;
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
	private static final boolean TRUNCATE_SAVED_OBJECTS = true && DEBUG_ONLY_FEATURES;
	private static final boolean DONT_RESTORE_OBJECTS = false && DEBUG_ONLY_FEATURES;
	private static final boolean DB_UNDO = false && DEBUG_ONLY_FEATURES;
	private static final boolean DB_RENDER_OBJ_BOUNDS = false && DEBUG_ONLY_FEATURES;
	private static final boolean DB_RENDER_EDITABLE = false && DEBUG_ONLY_FEATURES;
	private static final boolean DB_JSON = false && DEBUG_ONLY_FEATURES;
	private static final int MAX_COMMAND_HISTORY_SIZE = 30;
	private static final String JSON_KEY_OBJECTS = "obj";

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
		mPickRadius = MyActivity.inchesToPixels(.28f);
		prepareObjectTypes();
	}

	/**
	 * Get the view displaying the editor; construct if necessary. This contains
	 * the contentView
	 */
	public View getView() {
		if (mEditorView == null)
			constructView();
		return mEditorView;
	}

	/**
	 * Render editor-related elements to the contentView; this includes all the
	 * EdObjects, and any highlighting related to an active operation (e.g. a
	 * selection rectangle)
	 */
	public void render() {
		for (int i = 0; i < mObjects.size(); i++) {
			EdObject obj = mObjects.get(i);
			if (DB_RENDER_OBJ_BOUNDS
					|| (DB_RENDER_EDITABLE && obj.isEditable())) {
				mStepper.setColor(Color.GRAY);
				EditorTools.plotRect(mStepper, obj.getBounds(this));
			}
			obj.render(mStepper);
		}
		if (mCurrentOperation != null)
			mCurrentOperation.render(mStepper);
	}

	/**
	 * EditEventListener interface
	 */
	@Override
	public int processEvent(int eventCode, Point location) {

		if (mPendingAddObjectOperation != null) {
			switch (eventCode) {
			case EVENT_DOWN:
				addNewObject(mPendingAddObjectOperation, location);
				// Have the now activated object-specific handler process the
				// DOWN event
				mPendingAddObjectOperation = null;
				break;
			case EVENT_DOWN_MULTIPLE:
				mPendingAddObjectOperation = null;
				break;
			}
		}

		// If there's no current operation, and we have a DOWN event, start a
		// default event listener
		if (mCurrentOperation == null) {
			if (eventCode == EVENT_DOWN || eventCode == EVENT_DOWN_MULTIPLE) {
				setOperation(new DefaultEventListener(this));
			}
		}

		if (mCurrentOperation != null) {
			eventCode = mCurrentOperation.processEvent(eventCode, location);
			if (eventCode == EVENT_STOP) {
				clearOperation();
			}
		}

		// Request a refresh of the editor view after any event
		refresh();

		return eventCode;
	}

	@Override
	public void render(AlgorithmStepper s) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Make an object editable if it is the only selected object. We perform
	 * this operation with each refresh, since this is simpler than trying to
	 * maintain the editable state while the editor objects undergo various
	 * editing operations
	 */
	private void updateEditableObjectStatus() {
		List<Integer> list = mObjects.getSelectedSlots();
		for (int slot : list) {
			EdObject obj = mObjects.get(slot);
			obj.setEditable(list.size() == 1);
		}
	}

	private void refresh() {
		updateEditableObjectStatus();

		mStepper.refresh();
		// Set delay to save changes
		persistEditorState(true);
		updateButtonEnableStates();
	}

	private void updateButtonEnableStates() {
		mUndoButton.setEnabled(mCommandHistoryCursor > 0);
		mRedoButton.setEnabled(mCommandHistoryCursor < mCommandHistory.size());
	}

	/**
	 * Restore the editor state, including the EdObjects, from a JSON string
	 */
	public void restoreFromJSON(String script) {
		if (DONT_RESTORE_OBJECTS)
			script = null;
		if (script == null)
			return;
		if (DB_JSON) {
			pr("\n\nRestoring JSON:\n" + script);
		}
		try {
			JSONObject map = JSONTools.parseMap(script);
			JSONArray array = map.getJSONArray(JSON_KEY_OBJECTS);
			mObjects.clear();
			for (int i = 0; i < array.length(); i++) {
				if (TRUNCATE_SAVED_OBJECTS) {
					if (i < array.length() - 5) {
						warning("omitting all but last n objects");
						continue;
					}
				}
				JSONObject objMap = array.getJSONObject(i);
				String tag = objMap.getString(EdObjectFactory.JSON_KEY_TYPE);
				EdObjectFactory factory = mObjectTypes.get(tag);
				if (factory == null) {
					warning("no factory found for: " + tag);
					continue;
				}
				EdObject edObject = factory.parse(objMap);
				edObject.setEditor(this);
				if (!edObject.valid()) {
					warning("Unable to parse: " + objMap);
					continue;
				}
				mObjects.add(edObject);
			}
		} catch (JSONException e) {
			warning("caught " + e);
		}
	}

	/**
	 * Save the editor state (including EdObjects) to a JSON string within the
	 * user preferences
	 * 
	 * @param withDelay
	 *            if true, save operation is delayed by several seconds using
	 *            the QuiescentDelayOperation
	 */
	public void persistEditorState(boolean withDelay) {
		if (!withDelay) {
			persistEditorStateAux();
			return;
		}
		// Make a delayed call to persist the values
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

	/**
	 * Invoke a repeat of the last 'add object' operation
	 */
	void startAddAnotherOperation() {
		if (mLastAddObjectOperation != null) {
			startAddObjectOperation(mLastAddObjectOperation);
		}
	}

	EditorEventListener currentOperation() {
		return mCurrentOperation;
	}

	/**
	 * Get the EdObjectArray being edited
	 */
	EdObjectArray objects() {
		return mObjects;
	}

	/**
	 * Clear current operation
	 */
	private void clearOperation() {
		setOperation(null);
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

			toolbar.addView(addObjectTypeButton("Pt", EdPoint.FACTORY));
			toolbar.addView(addObjectTypeButton("Seg", EdSegment.FACTORY));
			toolbar.addView(addObjectTypeButton("Poly", EdPolyline.FACTORY));
			{
				Button button = buildSampleButton("Undo");
				button.setOnClickListener(new Button.OnClickListener() {
					@Override
					public void onClick(View v) {
						Editor editor = Editor.this;
						editor.doUndo();
					}
				});
				toolbar.addView(button);
				mUndoButton = button;
			}
			{
				Button button = buildSampleButton("Redo");
				button.setOnClickListener(new Button.OnClickListener() {
					@Override
					public void onClick(View v) {
						Editor editor = Editor.this;
						editor.doRedo();
					}
				});
				toolbar.addView(button);
				mRedoButton = button;
			}

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
		updateButtonEnableStates();
	}

	private void startAddObjectOperation(EdObjectFactory objectType) {
		objects().unselectAll();
		clearOperation();
		mPendingAddObjectOperation = objectType;
		mLastAddObjectOperation = objectType;
		if (false) // figure out a way to determine an appropriate toast message
			toast(context(), "Add segment!");
	}

	void setOperation(EditorEventListener operation) {
		mPendingAddObjectOperation = null;
		if (mCurrentOperation != null) {
			mCurrentOperation.processEvent(EVENT_STOP, null);
		}
		mCurrentOperation = operation;
	}

	private void persistEditorStateAux() {
		try {
			String jsonState = compileObjectsToJSON();
			if (!jsonState.equals(mLastSavedState)) {
				AppPreferences.putString(
						GeometryStepperActivity.PERSIST_KEY_EDITOR, jsonState);
				mLastSavedState = jsonState;
				if (DB_JSON) {
					pr("\n\nJSON:\n" + mLastSavedState);
				}
			}
		} catch (JSONException e) {
			warning("caught: " + e);
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
		editorMap.put(JSON_KEY_OBJECTS, getEdObjectsArrayJSON(objects()));
		return editorMap.toString();
	}

	private void addObjectType(EdObjectFactory factory) {
		mObjectTypes.put(factory.getTag(), factory);
	}

	private void prepareObjectTypes() {
		mObjectTypes = new HashMap();
		addObjectType(EdPoint.FACTORY);
		addObjectType(EdSegment.FACTORY);
		addObjectType(EdPolyline.FACTORY);
	}

	private void addNewObject(EdObjectFactory objectType, Point location) {
		EdObject newObject = objectType.construct();
		newObject.setEditor(this);
		newObject.setDefaultLocation(location);
		newObject.setEditable(true);
		int slot = mObjects.add(newObject);
		List<Integer> slots = SlotList.build(slot);
		mObjects.selectOnly(slots);

		Command c = Command.constructForAddedObjects(mObjects, slots);
		c.setPairedWithNext(true);
		pushCommand(c);

		// Start operation for editing this one
		setOperation(newObject.buildEditOperation(slot, location));
	}

	void doUndo() {
		final boolean db = DB_UNDO;
		if (mCommandHistoryCursor == 0) {
			warning("attempt to undo, nothing available" + getHistory());
			return;
		}
		mCommandHistoryCursor--;
		Command command = mCommandHistory.get(mCommandHistoryCursor);
		if (db)
			pr("Undoing " + command + getHistory());
		command.getReverse().perform(this);
		// While previous command exists, and is paired with the one we just
		// undid, repeat undo
		while (true) {
			if (mCommandHistoryCursor == 0)
				break;
			command = mCommandHistory.get(mCommandHistoryCursor - 1);
			if (!command.isPairedWithNext())
				break;
			mCommandHistoryCursor--;
			if (db)
				pr(" undoing paired previous: " + command);
			command.getReverse().perform(this);
		}
		refresh();
	}

	void doRedo() {
		final boolean db = DB_UNDO;
		if (mCommandHistoryCursor == mCommandHistory.size()) {
			warning("attempt to redo, nothing left" + getHistory());
			return;
		}
		Command command = mCommandHistory.get(mCommandHistoryCursor);
		if (db)
			pr("Redoing " + command);
		command.perform(this);
		mCommandHistoryCursor++;

		// While next command exists, and is paired with the one we just
		// did, repeat redo
		while (command.isPairedWithNext()) {
			if (mCommandHistoryCursor == mCommandHistory.size()) {
				warning("attempt to redo paired follower, none found");
				break;
			}
			command = mCommandHistory.get(mCommandHistoryCursor);
			mCommandHistoryCursor++;
			if (db)
				pr(" redoing paired follower: " + command);
			command.perform(this);
		}

		refresh();
	}

	private String getHistory() {
		if (DEBUG_ONLY_FEATURES) {
			return " (History size:" + mCommandHistory.size() + " cursor:"
					+ mCommandHistoryCursor + ")";
		}
		return "";
	}

	/**
	 * Add a command that has already been performed to the undo stack
	 */
	public void pushCommand(Command command) {
		final boolean db = DB_UNDO;
		if (db)
			pr("pushCommand to undo stack: " + command + getHistory());

		if (!command.valid()) {
			warning("attempt to perform invalid command: " + command);
			return;
		}
		// Throw out any older 'redoable' commands that will now be stale
		while (mCommandHistory.size() > mCommandHistoryCursor) {
			Command popped = pop(mCommandHistory);
			if (db)
				pr(" popped command " + popped + getHistory());
		}

		// Merge this command with its predecessor if possible
		while (true) {
			if (mCommandHistoryCursor == 0)
				break;
			Command prev = mCommandHistory.get(mCommandHistoryCursor - 1);
			Command merged = prev.attemptMergeWith(command);
			if (merged == null)
				break;
			if (db)
				pr("Merging: " + prev + "\n with: " + command + "\n yields: "
						+ merged);
			pop(mCommandHistory);
			mCommandHistoryCursor--;
			command = merged;
		}

		mCommandHistory.add(command);
		mCommandHistoryCursor++;
		if (db)
			pr(" added command" + getHistory());

		// If this command is not reversible, throw out all commands, including
		// this one
		if (command.getReverse() == null) {
			mCommandHistory.clear();
			mCommandHistoryCursor = 0;
			if (db)
				pr(" command isn't reversible; throwing out all commands"
						+ getHistory());
		}

		if (mCommandHistoryCursor > MAX_COMMAND_HISTORY_SIZE) {
			int del = mCommandHistoryCursor - MAX_COMMAND_HISTORY_SIZE;
			if (db)
				pr(" history full, deleting first " + del + " items");
			// If last command to be deleted is paired with following, include
			// following as well
			while (true) {
				if (del == mCommandHistoryCursor)
					break;
				Command last = mCommandHistory.get(del - 1);
				if (!last.isPairedWithNext())
					break;
				del++;
				if (db)
					pr("  last element is paired, deleting extra one");
			}
			mCommandHistoryCursor -= del;
			mCommandHistory.subList(0, del).clear();
			if (db)
				pr("  deleted first " + del + " items" + getHistory());
		}

	}

	private static String sEditorEventNames[] = { "NONE", "DOWN", "DRAG", "UP",
			"DOWN_M", "DRAG_M", "UP_M", "STOP", };

	public static String editorEventName(int eventCode) {
		if (!DEBUG_ONLY_FEATURES)
			return null;
		if (eventCode < 0 || eventCode >= sEditorEventNames.length)
			return "??#" + eventCode + "??";
		return sEditorEventNames[eventCode];
	}

	public float pickRadius() {
		return mPickRadius;
	}

	private Map<String, EdObjectFactory> mObjectTypes;
	private EditorEventListener mCurrentOperation;
	private EdObjectFactory mLastAddObjectOperation;
	// If not null, intercept DOWN events to add object
	private EdObjectFactory mPendingAddObjectOperation;
	private View mContentView;
	private View mEditorView;
	private ConcreteStepper mStepper;
	private EdObjectArray mObjects = new EdObjectArray();
	private QuiescentDelayOperation mPendingFlushOperation;
	private String mLastSavedState;
	private List<Command> mCommandHistory = new ArrayList();
	private int mCommandHistoryCursor;
	private Button mUndoButton, mRedoButton;
	private float mPickRadius;
}
