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
import com.js.geometryapp.AlgorithmDisplayElement;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.ConcreteStepper;
import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.AbstractWidget.Listener;
import com.js.geometryapp.widget.CheckBoxWidget;

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
public class Editor {

	private static final boolean ZAP_SUPPORTED = (false && DEBUG_ONLY_FEATURES);

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
			mOptions.addStaticText("   ");
			mOptions.addButton("Dup").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doDup();
					refresh();
				}
			});
			if (ZAP_SUPPORTED) {
				mOptions.addButton("Zap").addListener(new Listener() {
					public void valueChanged(AbstractWidget widget) {
						doZap();
						refresh();
					}
				});
			}
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
			mOptions.addButton("Scale").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doScale();
					refresh();
				}
			});
			mOptions.addButton("Rotate").addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doRotate();
					refresh();
				}
			});
		}
		mOptions.popView();
		mRenderAlways = mOptions.addCheckBox("_render_always_", "label",
				"Always plot editor");
	}

	private void prepareAddObjectButtons(Object... args) {
		mOptions.pushView(mOptions.addView(false));
		int i = 0;
		while (i < args.length) {
			String label = (String) args[i];
			final EdObjectFactory factory = (EdObjectFactory) args[i + 1];
			mOptions.addButton(label).addListener(new Listener() {
				public void valueChanged(AbstractWidget widget) {
					doStartAddObjectOperation(factory);
					refresh();
				}
			});
			i += 2;
		}
		mOptions.popView();
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
		if (!isActive() && !mRenderAlways.getBooleanValue())
			return;
		AlgorithmDisplayElement.setRendering(true);

		for (EdObject obj : mObjects) {
			if (DB_RENDER_OBJ_BOUNDS
					|| (DB_RENDER_EDITABLE && obj.isEditable())) {
				mStepper.setColor(Color.GRAY);
				EditorTools.plotRect(mStepper, obj.getBounds());
			}
			obj.render(mStepper);
		}
		if (mCurrentOperation != null)
			mCurrentOperation.render(mStepper);
		if (mPlotTouchLocation != null) {
			mStepper.setColor(Color.BLACK);
			mStepper.plotSprite(R.raw.crosshairicon, mPlotTouchLocation);
		}
		AlgorithmDisplayElement.setRendering(false);
	}

	private void updatePlotTouchLocation(EditorEvent event) {
		if (event.isUpVariant()) {
			mPlotTouchLocation = null;
		} else if (event.hasLocation()) {
			mPlotTouchLocation = event.getLocation();
		}
	}

	/**
	 * Process EditorEvent generated by GLSurfaceView
	 */
	void processEvent(EditorEvent event) {
		final boolean db = false && DEBUG_ONLY_FEATURES;
		if (db)
			event.printProcessingMessage("Editor");

		updatePlotTouchLocation(event);

		if (event.isDownVariant()) {
			// If multiple DOWN, start adding another object of the most recent
			// type
			if (event.isMultipleTouch()) {
				warning("maybe instead of last added object, include last editable object as well");
				if (mLastAddObjectOperation != null) {
					doStartAddObjectOperation(mLastAddObjectOperation);
					return;
				}
			} else {
				// If user wants to add a new object, do so
				EdObjectFactory factory = mPendingAddObjectOperation;
				if (factory != null) {
					mPendingAddObjectOperation = null;
					addNewObject(factory, event.getLocation());
					// fall through to let the new operation handle the touch
					// event
				} else {
					// If current operation is the default, check if this is a
					// press that starts editing an existing object
					if (mCurrentOperation == mDefaultOperation)
						startEditableObjectOperation(event);
				}
			}
		}

		// If there's no current operation, and we have a DOWN event, start a
		// default event listener
		if (mCurrentOperation == null) {
			if (event.isDownVariant()) {
				setOperation(new DefaultEventListener(this));
			}
		}

		// If there's a current operation, handle the event
		if (mCurrentOperation != null) {
			event = mCurrentOperation.processEvent(event);
			if (event.getCode() == EditorEvent.CODE_STOP) {
				clearOperation();
			}
		}

		// Request a refresh of the editor view after any event
		refresh();
	}

	/**
	 * Make an object editable if it is the only selected object. We perform
	 * this operation with each refresh, since this is simpler than trying to
	 * maintain the editable state while the editor objects undergo various
	 * editing operations
	 */
	private void updateEditableObjectStatus() {
		int currentEditable = -1;
		int newEditable = -1;
		List<Integer> list = mObjects.getSelectedSlots();
		for (int slot : list) {
			EdObject obj = mObjects.get(slot);
			if (obj.isEditable())
				currentEditable = slot;
		}
		if (list.size() == 1)
			newEditable = list.get(0);
		if (currentEditable != newEditable) {
			objects().setEditableSlot(newEditable);
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
		if (!mOptions.isEditorActive())
			return;

		if (QuiescentDelayOperation.replaceExisting(mPendingEnableOperation)) {
			final float ENABLE_DELAY = .1f;
			mPendingEnableOperation = new QuiescentDelayOperation("enable",
					ENABLE_DELAY, new Runnable() {
						public void run() {
							updateButtonEnableStatesAux();
						}
					});
		}
	}

	private void updateButtonEnableStatesAux() {
		List<Integer> selected = objects().getSelectedSlots();
		mOptions.setEnabled("Undo", mCommandHistoryCursor > 0);
		mOptions.setEnabled("Redo",
				mCommandHistoryCursor < mCommandHistory.size());
		mOptions.setEnabled("Cut", !selected.isEmpty());
		mOptions.setEnabled("Copy", !selected.isEmpty());
		mOptions.setEnabled("Paste", !mClipboard.isEmpty());
		mOptions.setEnabled("Dup", !selected.isEmpty());
		mOptions.setEnabled("All", selected.size() < objects().size());
		mOptions.setEnabled("Unhide", unhidePossible());
		mOptions.setEnabled("Scale", !selected.isEmpty());
		mOptions.setEnabled("Rotate", !selected.isEmpty());
	}

	/**
	 * Restore the editor state, including the EdObjects, from a JSON string
	 */
	public void restoreFromJSON(String script) {
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
		clearOperation();
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

	/**
	 * Set pending operation to add a new object of a particular type
	 */
	private void doStartAddObjectOperation(EdObjectFactory objectType) {
		objects().unselectAll();
		clearOperation();
		mPendingAddObjectOperation = objectType;
		mLastAddObjectOperation = objectType;
		if (false) // figure out a way to determine an appropriate toast message
			toast(context(), "Add segment!");
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
		EditorState originalState = new EditorState(this);
		EdObject newObject = objectType.construct(location);
		newObject.setEditor(this);
		int slot = mObjects.add(newObject);
		mObjects.setEditableSlot(slot);

		Command c = Command.constructForGeneralChanges(originalState,
				new EditorState(this), objectType.getTag());
		pushCommand(c);

		// Start operation for editing this one
		setOperation(newObject.buildEditOperation(slot, location));
	}

	private void doUndo() {
		// Button enabling is delayed, so we can't assume this operation is
		// possible
		if (mCommandHistoryCursor == 0) {
			return;
		}
		clearOperation();
		mCommandHistoryCursor--;
		Command command = mCommandHistory.get(mCommandHistoryCursor);
		command.getReverse().perform(this);
	}

	private void doRedo() {
		// Button enabling is delayed, so we can't assume this operation is
		// possible
		if (mCommandHistoryCursor == mCommandHistory.size()) {
			return;
		}
		clearOperation();
		Command command = mCommandHistory.get(mCommandHistoryCursor);
		command.perform(this);
		mCommandHistoryCursor++;
	}

	private void doCut() {
		EditorState originalState = new EditorState(this);
		if (originalState.getSelectedSlots().isEmpty())
			return;
		clearOperation();

		EdObjectArray newClipboard = objects().getSelectedObjects().freeze();
		setClipboard(newClipboard);
		objects().removeSelected();

		Command command = Command.constructForGeneralChanges(originalState,
				new EditorState(this), null);
		pushCommand(command);
		resetDuplicationOffset();
	}

	private void doCopy() {
		EditorState originalState = new EditorState(this);
		if (originalState.getSelectedSlots().isEmpty())
			return;
		clearOperation();
		EdObjectArray newClipboard = objects().getSelectedObjects().freeze();
		setClipboard(newClipboard);

		Command command = Command.constructForGeneralChanges(originalState,
				new EditorState(this), null);
		pushCommand(command);
		resetDuplicationOffset();
	}

	private void doPaste() {
		if (mClipboard.isEmpty())
			return;
		clearOperation();
		EditorState originalState = new EditorState(this);
		List<Integer> newSelected = SlotList.build();

		Point offset = getDupAccumulator().getOffsetForPaste();

		for (EdObject obj : mClipboard) {
			newSelected.add(objects().size());
			EdObject copy = obj.getCopy();
			copy.moveBy(obj, offset);
			objects().add(copy);
		}
		objects().setSelected(newSelected);

		Command command = Command.constructForGeneralChanges(originalState,
				new EditorState(this), null);
		pushCommand(command);
	}

	private void doZap() {
		if (ZAP_SUPPORTED) {
			clearOperation();
			objects().clear();
			mClipboard = new EdObjectArray();
			mCommandHistory.clear();
			mCommandHistoryCursor = 0;
		}
	}

	private void doDup() {
		EditorState originalState = new EditorState(this);
		if (originalState.getSelectedSlots().isEmpty())
			return;
		clearOperation();
		List<Integer> newSelected = SlotList.build();

		Point offset = getDupAccumulator().getOffsetForDup();

		for (int slot : originalState.getSelectedSlots()) {
			EdObject obj = objects().get(slot);
			EdObject copy = obj.getCopy();
			copy.moveBy(obj, offset);
			newSelected.add(objects().add(copy));
		}
		objects().setSelected(newSelected);

		Command command = Command.constructForGeneralChanges(originalState,
				new EditorState(this), null);
		pushCommand(command);
	}

	DupAccumulator getDupAccumulator() {
		if (mDupAccumulator == null)
			resetDuplicationOffset();
		return mDupAccumulator;
	}

	void resetDuplicationOffset() {
		mDupAccumulator = new DupAccumulator(pickRadius());
	}

	/**
	 * Determine which slot, if any, holds the (at most one) editable object
	 * 
	 * @return slot if found, or -1
	 */
	private int getEditableSlot() {
		if (objects().getSelectedSlots().size() != 1)
			return -1;
		int slot = objects().getSelectedSlots().get(0);
		EdObject src = objects().get(slot);
		if (src.isEditable()) {
			return slot;
		}
		return -1;
	}

	private void doScale() {
		if (objects().getSelectedSlots().isEmpty())
			return;
		setOperation(new ScaleOperation(this));
	}

	private void doRotate() {
		if (objects().getSelectedSlots().isEmpty())
			return;
		setOperation(new RotateOperation(this));
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
		objects().selectAll();
	}

	private void doUnhide() {
		Point translation = new Point();
		List<Integer> slots = findHiddenObjects(translation);
		if (slots.isEmpty())
			return;

		EditorState originalState = new EditorState(this);

		objects().setSelected(slots);
		objects().replaceSelectedObjectsWithCopies();

		for (int slot : slots) {
			EdObject obj = objects().get(slot);
			obj.moveBy(null, translation);
		}

		Command cmd = Command.constructForGeneralChanges(originalState,
				new EditorState(this), "unhide");
		pushCommand(cmd);
	}

	/**
	 * Add a command that has already been performed to the undo stack
	 */
	public void pushCommand(Command command) {
		// Throw out any older 'redoable' commands that will now be stale
		while (mCommandHistory.size() > mCommandHistoryCursor) {
			pop(mCommandHistory);
		}

		// Merge this command with its predecessor if possible
		while (true) {
			if (mCommandHistoryCursor == 0)
				break;
			Command prev = mCommandHistory.get(mCommandHistoryCursor - 1);
			Command merged = prev.attemptMergeWith(command);
			if (merged == null)
				break;
			pop(mCommandHistory);
			mCommandHistoryCursor--;
			command = merged;
		}

		mCommandHistory.add(command);
		mCommandHistoryCursor++;

		// If this command is not reversible, throw out all commands, including
		// this one
		if (command.getReverse() == null) {
			mCommandHistory.clear();
			mCommandHistoryCursor = 0;
		}

		if (mCommandHistoryCursor > MAX_COMMAND_HISTORY_SIZE) {
			int del = mCommandHistoryCursor - MAX_COMMAND_HISTORY_SIZE;
			mCommandHistoryCursor -= del;
			mCommandHistory.subList(0, del).clear();
		}
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

	private void setClipboard(EdObjectArray clipboard) {
		mClipboard = clipboard.freeze();
	}

	private void setObjects(EdObjectArray objects) {
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
					polygon.add(new Point(polyline.getPoint(i)));
				if (polygon.numVertices() < 3)
					continue;
				int orientation = polygon.orientation();
				if (orientation == 0)
					continue;
				if (orientation < 0)
					polygon.reverse();

				polygonList.add(polygon);
			}
		}
		algorithmInput.points = pointList.toArray(new Point[0]);
		algorithmInput.polygons = polygonList.toArray(new Polygon[0]);
		return algorithmInput;
	}

	void setState(EditorState state) {
		setObjects(state.getObjects().getMutableCopy());
		setClipboard(state.getClipboard());
		objects().setSelected(state.getSelectedSlots());
		mDupAccumulator = new DupAccumulator(state.getDupAccumulator());
	}

	// ----------- event listener - related

	private void setOperation(EditorEventListener operation) {
		if (operation == null) {
			mDefaultOperation = new DefaultEventListener(this);
			operation = mDefaultOperation;
		}

		// Clear any pending 'add' operation (which was waiting for a DOWN event
		// to activate it)
		mPendingAddObjectOperation = null;

		// If an operation was active, stop it
		if (mCurrentOperation != null) {
			mCurrentOperation.processEvent(EditorEvent.STOP);
		}
		mCurrentOperation = operation;
	}

	/**
	 * Determine if there's an editable object which can construct an edit
	 * operation for a particular location. If so, start the operation and
	 * return true
	 * 
	 * @param event
	 *            EditorEvent; if not (single) DOWN event, always returns false
	 */
	private boolean startEditableObjectOperation(EditorEvent event) {
		if (!(event.isDownVariant() && !event.isMultipleTouch()))
			return false;
		int editableSlot = getEditableSlot();
		if (editableSlot >= 0) {
			EdObject obj = objects().get(editableSlot);
			EditorEventListener operation = obj.buildEditOperation(
					editableSlot, event.getLocation());

			if (operation != null) {
				setOperation(operation);
				return true;
			}
		}
		return false;
	}

	private Map<String, EdObjectFactory> mObjectTypes;
	private EditorEventListener mCurrentOperation;
	private EditorEventListener mDefaultOperation;
	private EdObjectFactory mLastAddObjectOperation;
	// If not null, on next DOWN event, we create a new object and start
	// operation to edit it
	private EdObjectFactory mPendingAddObjectOperation;
	private View mEditorView;
	private ConcreteStepper mStepper;
	private EdObjectArray mObjects = new EdObjectArray();
	private QuiescentDelayOperation mPendingFlushOperation;
	private String mLastSavedState;
	private List<Command> mCommandHistory = new ArrayList();
	private int mCommandHistoryCursor;
	private float mPickRadius;
	private Point mPlotTouchLocation;
	private AlgorithmOptions mOptions;
	private LinearLayout mAuxView;
	private EdObjectArray mClipboard = new EdObjectArray();
	private QuiescentDelayOperation mPendingEnableOperation;
	private CheckBoxWidget mRenderAlways;
	private DupAccumulator mDupAccumulator;

}
