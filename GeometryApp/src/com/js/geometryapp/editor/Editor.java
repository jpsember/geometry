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
import com.js.android.UITools;
import com.js.basic.JSONTools;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.R;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.ConcreteStepper;
import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.AbstractWidget.Listener;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;
import static com.js.android.Tools.*;

/**
 * Class that encapsulates editing geometric objects. It includes a view, which
 * contains both a content view to display the objects being edited, as well as
 * floating toolbars.
 */
public class Editor implements EditorEventListener {

	private static final boolean TRUNCATE_SAVED_OBJECTS = false && DEBUG_ONLY_FEATURES;
	private static final boolean DONT_RESTORE_OBJECTS = false && DEBUG_ONLY_FEATURES;
	private static final boolean DB_UNDO = false && DEBUG_ONLY_FEATURES;
	private static final boolean DB_RENDER_OBJ_BOUNDS = false && DEBUG_ONLY_FEATURES;
	private static final boolean DB_RENDER_EDITABLE = false && DEBUG_ONLY_FEATURES;
	private static final boolean DB_JSON = false && DEBUG_ONLY_FEATURES;
	private static final int MAX_COMMAND_HISTORY_SIZE = 30;
	private static final String JSON_KEY_OBJECTS = "obj";
	private static final String JSON_KEY_CLIPBOARD = "cb";

	public Editor() {
	}

	public void setDependencies(ConcreteStepper stepper,
			AlgorithmOptions options) {
		mStepper = stepper;
		mOptions = options;
	}

	/**
	 * Constructor
	 * 
	 * @param contentView
	 *            view displaying objects being edited; probably a GLSurfaceView
	 */
	public void prepare(View contentView) {
		mEditorView = contentView;
		mPickRadius = MyActivity.inchesToPixels(.28f);
		prepareObjectTypes();
	}

	/**
	 * Construct the various editor widgets
	 * 
	 * @param algorithmOptions
	 */
	public void prepareOptions() {

		// Place these controls in the aux controls view
		mOptions.pushView(getAuxControlsView());

		// Add a horizontal row of buttons for undo, redo
		{
			mOptions.pushView(mOptions.addView(false));
			// mOptions.addLabel("");
			mOptions.addButton("Undo").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doUndo();
					refresh();
				}
			});
			mOptions.addButton("Redo").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doRedo();
					refresh();
				}
			});
			mOptions.addStaticText("   ");
			mOptions.addButton("Cut").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doCut();
					refresh();
				}
			});
			mOptions.addButton("Copy").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doCopy();
					refresh();
				}
			});
			mOptions.addButton("Paste").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doPaste();
					refresh();
				}
			});
			mOptions.popView();
		}
		prepareAddObjectButtons("Pt", EdPoint.FACTORY, "Seg",
				EdSegment.FACTORY, "Poly", EdPolyline.FACTORY);
		mOptions.popView();

		// put additional controls in the options window

		mOptions.pushView(mOptions.addView(false));
		{
			mOptions.addButton("All").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doSelectAll();
					refresh();
				}
			});
			mOptions.addButton("Unhide").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doUnhide();
					refresh();
				}
			});
		}
		mOptions.popView();
	}

	private void prepareAddObjectButtons(Object... args) {
		mOptions.pushView(mOptions.addView(false));
		int i = 0;
		while (i < args.length) {
			String label = (String) args[i];
			final EdObjectFactory factory = (EdObjectFactory) args[i + 1];
			mOptions.addButton(label).addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					startAddObjectOperation(factory);
				}
			});
			i += 2;
		}
		mOptions.popView();
	}

	public ConcreteStepper getStepper() {
		return mStepper;
	}

	/**
	 * Get the view displaying the editor; construct if necessary. This contains
	 * the contentView
	 */
	public View getView() {
		return mEditorView;
	}

	public boolean isActive() {
		return mOptions.isEditorActive();
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
		if (mTouchLocation != null) {
			mStepper.setColor(Color.BLACK);
			mStepper.plotSprite(R.raw.crosshairicon, mTouchLocation);
		}
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

		if (eventCode == EVENT_UP || eventCode == EVENT_UP_MULTIPLE) {
			mTouchLocation = null;
		} else if (location != null) {
			mTouchLocation = location;
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
		// TODO: we could optimize this by e.g. caching the selected slots or
		// something
		if (!mOptions.isEditorActive())
			return;

		if (QuiescentDelayOperation.replaceExisting(mPendingEnableOperation)) {
			final float ENABLE_DELAY = .2f;
			mPendingEnableOperation = new QuiescentDelayOperation("enable",
					ENABLE_DELAY, new Runnable() {
						public void run() {
							List<Integer> selected = objects()
									.getSelectedSlots();
							mOptions.setEnabled("Undo",
									mCommandHistoryCursor > 0);
							mOptions.setEnabled("Redo",
									mCommandHistoryCursor < mCommandHistory
											.size());
							mOptions.setEnabled("Cut", !selected.isEmpty());
							mOptions.setEnabled("Copy", !selected.isEmpty());
							mOptions.setEnabled("Paste", !mClipboard.isEmpty());
							mOptions.setEnabled("All", objects()
									.getSelectedSlots().size() < objects()
									.size());
							mOptions.setEnabled("Unhide", unhidePossible());
						}
					});
		}
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
			parseObjects(mObjects, map, JSON_KEY_OBJECTS);
			parseObjects(mClipboard, map, JSON_KEY_CLIPBOARD);
		} catch (JSONException e) {
			warning("caught " + e);
		}
	}

	public void begin() {
		updateButtonEnableStates();
	}

	/**
	 * Parse an EdObjectArray from JSON map
	 * 
	 * @param objectsArray
	 *            where to store the objects; cleared beforehand
	 * @param map
	 * @param key
	 *            key objects are stored as
	 * @return object array, or null if no key found
	 * @throws JSONException
	 */
	private void parseObjects(EdObjectArray objectsArray, JSONObject map,
			String key) throws JSONException {
		objectsArray.clear();
		if (map.has(key)) {
			JSONArray array = map.getJSONArray(key);
			for (int i = 0; i < array.length(); i++) {
				if (TRUNCATE_SAVED_OBJECTS) {
					if (key.equals(JSON_KEY_OBJECTS)) {
						if (i < array.length() - 5) {
							warning("omitting all but last n objects");
							continue;
						}
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
				objectsArray.add(edObject);
			}
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
		return mEditorView.getContext();
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
		editorMap.put(JSON_KEY_CLIPBOARD, getEdObjectsArrayJSON(mClipboard));
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
		EdObjectArray originalAll = mObjects.getFrozen();
		List<Integer> originalSelected = originalAll.getSelectedSlots();

		EdObject newObject = objectType.construct(location);
		newObject.setEditor(this);
		newObject.setEditable(true);
		int slot = mObjects.add(newObject);
		List<Integer> slots = SlotList.build(slot);
		mObjects.selectOnly(slots);

		Command c = Command.constructForGeneralChanges(originalAll,
				originalSelected, null, mObjects, slots, null, null);
		c.setPairedWithNext(true);
		pushCommand(c);

		// Start operation for editing this one
		setOperation(newObject.buildEditOperation(slot, location));
	}

	void doUndo() {
		final boolean db = DB_UNDO;
		// Button enabling is delayed, so we can't assume this operation is
		// possible
		if (mCommandHistoryCursor == 0) {
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
	}

	void doRedo() {
		final boolean db = DB_UNDO;
		// Button enabling is delayed, so we can't assume this operation is
		// possible
		if (mCommandHistoryCursor == mCommandHistory.size()) {
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
	}

	void doCut() {
		List<Integer> slots = objects().getSelectedSlots();
		if (slots.isEmpty())
			return;

		EdObjectArray originalObjects = objects().getFrozen();
		EdObjectArray newClipboard = objects().getSubset(slots).freeze();
		EdObjectArray origClipboard = mClipboard;
		setClipboard(newClipboard);
		objects().remove(slots);

		Command command = Command.constructForGeneralChanges(//
				originalObjects,//
				slots, //
				origClipboard, //
				objects(), //
				SlotList.build(),// cutting all selected objects, so none remain
				newClipboard, //
				null);
		pushCommand(command);
	}

	void doCopy() {
		List<Integer> slots = objects().getSelectedSlots();
		if (slots.isEmpty())
			return;
		EdObjectArray originalObjects = objects().getFrozen();
		EdObjectArray newClipboard = objects().getSubset(slots).freeze();
		EdObjectArray origClipboard = mClipboard;
		setClipboard(newClipboard);

		Command command = Command.constructForGeneralChanges(//
				originalObjects, //
				slots,//
				origClipboard, //
				originalObjects, // copying doesn't change the current objects
				null, // nor which are selected
				newClipboard, //
				null);
		pushCommand(command);
	}

	void doPaste() {
		if (mClipboard.isEmpty())
			return;
		List<Integer> originalSlots = objects().getSelectedSlots();
		EdObjectArray originalObjects = objects().getFrozen();
		List<Integer> newSelected = SlotList.build();
		for (EdObject obj : mClipboard) {
			newSelected.add(objects().size());
			EdObject copy = (EdObject) obj.clone();
			copy.moveBy(obj, new Point(20, 20));
			objects().add(copy);
		}
		objects().selectOnly(newSelected);
		unimp("more sophisticated translation method");

		Command command = Command.constructForGeneralChanges(//
				originalObjects, //
				originalSlots,//
				mClipboard, //
				objects(), //
				newSelected, //
				mClipboard, // pasting doesn't change the clipboard contents
				null);
		pushCommand(command);
	}

	private boolean unhidePossible() {
		return !findHiddenObjects(null).isEmpty();
	}

	/**
	 * Find which objects, if any, are essentially offscreen and thus hidden
	 * from the user. Note: an object is deemed to be hidden if all of its
	 * vertices are outside of a (slightly inset) rectangle representing the
	 * editor view. This is inaccurate; consider a very large polygon that
	 * contains the editor view, but none of whose vertices lie within it.
	 * 
	 * @param translation
	 *            if not null, and hidden objects are found, this is set to a
	 *            translation to apply to all objects to bring (at least one of
	 *            them) onscreen
	 * @return slots of hidden objects
	 */
	private List<Integer> findHiddenObjects(Point translation) {
		float minUnhideSquaredDistance = 0;
		boolean translationDefined = false;

		Rect r = mStepper.algorithmRect();
		// Construct a slightly inset version for detecting hidden objects, and
		// a more inset one representing where we'll move vertices to unhide
		// them
		Rect outerRect = new Rect(r);
		outerRect.inset(20, 20);
		Rect innerRect = new Rect(r);
		innerRect.inset(40, 40);

		List<Integer> slots = SlotList.build();
		objLoop: for (int i = 0; i < objects().size(); i++) {
			EdObject obj = objects().get(i);

			// If none of this object's vertices are visible, assume it's hidden
			for (int j = 0; j < obj.nPoints(); j++) {
				Point v = obj.getPoint(j);
				if (outerRect.contains(v))
					continue objLoop;
			}

			// This is a hidden object; add to output list
			slots.add(i);

			if (translation != null) {
				// See if one of its vertices is closest yet to the inner rect
				for (int j = 0; j < obj.nPoints(); j++) {
					Point v = obj.getPoint(j);
					Point v2 = outerRect.nearestPointTo(v);
					float squaredDistance = MyMath
							.squaredDistanceBetween(v, v2);
					if (!translationDefined
							|| squaredDistance < minUnhideSquaredDistance) {
						translationDefined = true;
						translation.setTo(MyMath.subtract(v2, v));
						minUnhideSquaredDistance = squaredDistance;
					}
				}
			}
		}
		return slots;
	}

	private void doSelectAll() {
		for (EdObject obj : objects())
			obj.setSelected(true);
		refresh();
	}

	private void doUnhide() {
		Point translation = new Point();
		List<Integer> slots = findHiddenObjects(translation);
		if (slots.isEmpty())
			return;

		EdObjectArray originalObjects = objects().getFrozen();
		List<Integer> selectedSlots = objects().getSelectedSlots();

		objects().selectOnly(slots);
		objects().replaceWithCopies(slots);

		for (int slot : slots) {
			EdObject obj = objects().get(slot);
			obj.moveBy(null, translation);
		}

		// Create command
		Command cmd = Command.constructForGeneralChanges(originalObjects,
				selectedSlots, null, objects(), slots, null, "unhide");
		pushCommand(cmd);
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

	public LinearLayout getAuxControlsView() {
		if (mAuxView == null) {
			mAuxView = UITools.linearLayout(context(), true);
		}
		return mAuxView;
	}

	public EdObjectArray getClipboard() {
		return mClipboard;
	}

	public void setClipboard(EdObjectArray clipboard) {
		mClipboard = clipboard.freeze();
	}

	public void setObjects(EdObjectArray objects) {
		mObjects = objects;
	}

	public AlgorithmInput constructAlgorithmInput() {
		AlgorithmInput algorithmInput = new AlgorithmInput();
		List<Point> pointList = new ArrayList();
		List<Polygon> polygonList = new ArrayList();

		for (EdObject obj : objects()) {
			if (obj instanceof EdPoint) {
				EdPoint pt = (EdPoint) obj;
				pointList.add(new Point(pt.getPoint(0)));
			} else if (obj instanceof EdPolyline) {
				EdPolyline polyline = (EdPolyline) obj;
				if (!polyline.closed())
					continue;
				Polygon polygon = new Polygon();
				for (int i = 0; i < polyline.nPoints(); i++)
					polygon.add(polyline.getPoint(i));
				polygonList.add(polygon);
			}
		}
		algorithmInput.points = pointList.toArray(new Point[0]);
		algorithmInput.polygons = polygonList.toArray(new Polygon[0]);
		return algorithmInput;
	}

	private Map<String, EdObjectFactory> mObjectTypes;
	private EditorEventListener mCurrentOperation;
	private EdObjectFactory mLastAddObjectOperation;
	// If not null, intercept DOWN events to add object
	private EdObjectFactory mPendingAddObjectOperation;
	private View mEditorView;
	private ConcreteStepper mStepper;
	private EdObjectArray mObjects = new EdObjectArray();
	private QuiescentDelayOperation mPendingFlushOperation;
	private String mLastSavedState;
	private List<Command> mCommandHistory = new ArrayList();
	private int mCommandHistoryCursor;
	private float mPickRadius;
	private Point mTouchLocation;
	private AlgorithmOptions mOptions;
	private LinearLayout mAuxView;
	private EdObjectArray mClipboard = new EdObjectArray();
	private QuiescentDelayOperation mPendingEnableOperation;

}
