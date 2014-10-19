package com.js.geometryapp.editor;

import static com.js.basic.Tools.*;

import java.util.List;

import android.graphics.Color;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgorithmStepper;

public class DefaultEventListener implements EditorEventListener {

	public DefaultEventListener(Editor editor) {
		mEditor = editor;
	}

	/**
	 * Determine which slot, if any, holds the (at most one) editable object
	 * 
	 * @return slot if found, or -1
	 */
	private int getEditableSlot() {
		EdObjectArray srcObjects = mEditor.objects();
		for (int slot = 0; slot < srcObjects.size(); slot++) {
			EdObject src = srcObjects.get(slot);
			if (src.isEditable()) {
				return slot;
			}
		}
		return -1;
	}

	private List<Integer> getPickSet(Point location) {
		List<Integer> slots = SlotList.build();
		EdObjectArray srcObjects = mEditor.objects();
		for (int slot = 0; slot < srcObjects.size(); slot++) {
			EdObject src = srcObjects.get(slot);
			float dist = src.distFrom(location);
			float pickRadius = mEditor.pickRadius();
			if (src.isSelected())
				pickRadius *= 2f;
			if (dist > pickRadius)
				continue;
			slots.add(slot);
		}
		return slots;
	}

	private EdObject editorObject(int slot) {
		return mEditor.objects().get(slot);
	}

	/**
	 * Given an item list, get subsequence of those items that are selected
	 * 
	 * @param slotList
	 *            list of item slots
	 * @return subsequence of slotList that are selected
	 */
	private List<Integer> getSelectedObjects(List<Integer> slotList) {
		List<Integer> selectedSlots = SlotList.build();
		for (int slot : slotList) {
			EdObject obj = editorObject(slot);
			if (obj.isSelected())
				selectedSlots.add(slot);
		}
		return selectedSlots;
	}

	/**
	 * User did a DOWN+UP without any dragging
	 * 
	 * @param location
	 *            location where DOWN occurred
	 */
	private void doClick(Point location) {

		// As we did for the drag logic, first check: if there is an editable
		// object, and object can construct an edit operation for the location,
		// start that operation
		if (startEditableObjectOperation(location))
			return;

		// Construct pick set of selected objects. If empty, unselect
		// all objects; else cycle to next object and make it editable
		List<Integer> pickSet = getPickSet(location);
		if (pickSet.isEmpty()) {
			if (mEditor.addMultiplePossible(location)) {
				return;
			}
			mEditor.objects().unselectAll();
			mEditor.resetDuplicationOffset();
			return;
		}

		// Find selected item with highest index
		int highestIndex = pickSet.size();
		for (int i = 0; i < pickSet.size(); i++) {
			if (mEditor.objects().get(pickSet.get(i)).isSelected())
				highestIndex = i;
		}
		int nextSelectedIndex = MyMath.myMod(highestIndex - 1, pickSet.size());
		mEditor.objects().unselectAll();
		EdObject editObject = mEditor.objects().get(
				pickSet.get(nextSelectedIndex));
		editObject.setEditable(true);
		editObject.selectedForEditing(location);
		mEditor.resetDuplicationOffset();
	}

	/**
	 * Determine if there's an editable object which can construct an edit
	 * operation for a particular location. If so, start the operation and
	 * return true
	 */
	private boolean startEditableObjectOperation(Point location) {
		int editableSlot = getEditableSlot();
		if (editableSlot >= 0) {
			EdObject obj = editorObject(editableSlot);
			EditorEventListener operation = obj.buildEditOperation(
					editableSlot, location);

			if (operation != null) {
				mEditor.setOperation(operation);
				return true;
			}
		}
		return false;
	}

	private void doStartDrag(Point location) {

		/**
		 * <pre>
		 * 
		 * If there is an editable object, and object can construct an edit operation
		 * for the location, start that operation;
		 * 
		 * else
		 * 
		 * If initial press pickset contains any selected objects, move all 
		 * selected objects;
		 * 
		 * else
		 * 
		 * If initial press pickset contains any objects, select and move topmost;
		 * 
		 * else
		 * 
		 * If 'add multiple' is selected, and previous add object type defined, 
		 * start adding another;
		 * 
		 * else
		 * 
		 * Drag a selection rectangle and select the items contained within it
		 * 
		 * </pre>
		 */

		if (startEditableObjectOperation(location))
			return;

		// get 'pick set' for touch location
		List<Integer> pickSet = getPickSet(location);
		// get subset of pick set that are currently selected
		List<Integer> hlPickSet = getSelectedObjects(pickSet);

		if (hlPickSet.isEmpty() && !pickSet.isEmpty()) {
			hlPickSet = SlotList.build(last(pickSet));
			mEditor.objects().selectOnly(hlPickSet);
			mEditor.resetDuplicationOffset();
			// fall through to next...
		}
		if (!hlPickSet.isEmpty()) {
			mOriginalState = new EditorState(mEditor);
			// Replace selected objects with copies in preparation for moving
			mEditor.objects().replaceSelectedObjectsWithCopies();
		} else if (mEditor.addMultiplePossible(location)) {
		} else {
			mDraggingRect = true;
			mEditor.objects().unselectAll();
			mEditor.resetDuplicationOffset();
		}
	}

	private void doContinueDrag(Point location) {
		if (mDraggingRect) {
			mDragCorner = location;
		} else {
			mTranslate = MyMath.subtract(location, mInitialDownLocation);
			if (mTranslate.magnitude() == 0)
				return;

			for (int slot : mOriginalState.getSelectedSlots()) {
				EdObject obj = mEditor.objects().get(slot);
				EdObject orig = mOriginalState.getObjects().get(slot);
				obj.moveBy(orig, mTranslate);
			}
		}
	}

	private void doFinishDrag() {
		if (mDraggingRect) {
			Rect dragRect = getDragRect();
			if (dragRect == null)
				return;
			List<Integer> selectedList = SlotList.build();
			for (int slot = 0; slot < mEditor.objects().size(); slot++) {
				EdObject edObject = mEditor.objects().get(slot);
				if (dragRect.contains(edObject.getBounds(mEditor)))
					selectedList.add(slot);
			}
			mEditor.objects().selectOnly(selectedList);
		} else if (mOriginalState != null) {
			if (mTranslate != null) {
				mEditor.getDupAccumulator().processMove(mTranslate);
			}
			Command cmd = Command.constructForGeneralChanges(mOriginalState,
					new EditorState(mEditor), "move");
			mEditor.pushCommand(cmd);
		}
	}

	/**
	 * <pre>
	 * 
	 * Picking behaviour
	 * 
	 * The 'pick set' under a point p is the ordered sequence of all objects that contain p. These
	 * objects may or may not be currently selected.  Some objects, such as segments, should have
	 * a fuzzy definition of what it means to contain a point.
	 * 
	 * Repeated clicks at a point should cycle between the pick set under that point, by selecting 
	 * each item in sequence.
	 * 
	 * Clicking should unselect all objects that are not in the current point's click set.
	 * 
	 * There should be a distinction between clicking (down+up) and dragging (down+drag+up).
	 * 
	 * A down event on an empty pick set, followed by a drag, should produce a rectangle used to
	 * select contained objects.
	 * 
	 * </pre>
	 */
	@Override
	public int processEvent(int eventCode, Point location) {
		// By default, we'll be handling this event; so clear return code
		int returnCode = EVENT_NONE;

		final boolean db = false && DEBUG_ONLY_FEATURES;
		if (db)
			pr("DefaultEventListener " + Editor.editorEventName(eventCode)
					+ " at " + location);

		switch (eventCode) {
		default:
			// Don't know how to handle this event, so restore return code
			returnCode = eventCode;
			break;

		case EVENT_DOWN:
			mInitialDownLocation = location;
			break;

		case EVENT_DRAG:
			if (!mDragStarted) {
				mDragStarted = true;
				doStartDrag(mInitialDownLocation);
				if (mEditor.currentOperation() != this)
					break;
			}
			doContinueDrag(location);
			break;

		case EVENT_UP:
			if (!mDragStarted) {
				doClick(mInitialDownLocation);
			} else {
				doFinishDrag();
			}
			returnCode = EVENT_STOP;
			break;

		// A double tap will add another object of the last type added
		case EVENT_DOWN_MULTIPLE:
			mEditor.startAddAnotherOperation();
			break;

		case EVENT_UP_MULTIPLE:
			returnCode = EVENT_STOP;
			break;
		}

		return returnCode;
	}

	@Override
	public void render(AlgorithmStepper s) {
		Rect r = getDragRect();
		if (r != null) {
			s.setColor(Color.argb(0x80, 0xff, 0x40, 0x40));
			EditorTools.plotRect(s, r);
		}
	}

	/**
	 * Get the rectangle for the current drag selection operation, or null if
	 * none is active (or rectangle is not available)
	 * 
	 * @return Rect, or null
	 */
	private Rect getDragRect() {
		if (mDraggingRect && mDragCorner != null)
			return new Rect(mInitialDownLocation, mDragCorner);
		return null;
	}

	private Editor mEditor;
	private Point mInitialDownLocation;
	private Point mDragCorner;
	private boolean mDragStarted;
	private boolean mDraggingRect;
	private EditorState mOriginalState;
	private Point mTranslate;
}
