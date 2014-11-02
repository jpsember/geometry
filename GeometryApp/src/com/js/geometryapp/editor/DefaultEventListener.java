package com.js.geometryapp.editor;

import static com.js.basic.Tools.*;

import java.util.List;

import android.graphics.Color;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;

public class DefaultEventListener extends EditorEventListenerAdapter {

	public DefaultEventListener(Editor editor) {
		mEditor = editor;
	}

	private List<Integer> getPickSet(Point location) {
		List<Integer> slots = SlotList.build();
		EdObjectArray srcObjects = mEditor.objects();
		float pickRadius = mEditor.pickRadius();
		for (int slot = 0; slot < srcObjects.size(); slot++) {
			EdObject src = srcObjects.get(slot);
			float dist = src.distFrom(location);
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

		// Construct pick set of selected objects. If empty, unselect
		// all objects; else cycle to next object and make it editable
		List<Integer> pickSet = getPickSet(location);
		if (pickSet.isEmpty()) {
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
		int slot = pickSet.get(nextSelectedIndex);
		mEditor.objects().setEditableSlot(slot);
		mEditor.objects().get(slot).selectedForEditing(location);
		mEditor.resetDuplicationOffset();
	}

	private void doStartDrag(Point location) {

		/**
		 * <pre>
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

		// get 'pick set' for touch location
		List<Integer> pickSet = getPickSet(location);
		// get subset of pick set that are currently selected
		List<Integer> hlPickSet = getSelectedObjects(pickSet);

		if (hlPickSet.isEmpty() && !pickSet.isEmpty()) {
			hlPickSet = SlotList.build(last(pickSet));
			mEditor.objects().setSelected(hlPickSet);
			mEditor.resetDuplicationOffset();
			// fall through to next...
		}
		if (!hlPickSet.isEmpty()) {
			mOriginalState = new EditorState(mEditor);
			// Replace selected objects with copies in preparation for moving
			mEditor.objects().replaceSelectedObjectsWithCopies();
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
				if (dragRect.contains(edObject.getBounds()))
					selectedList.add(slot);
			}
			mEditor.objects().setSelected(selectedList);
		} else if (mOriginalState != null) {
			if (mTranslate != null) {
				mEditor.updateDupAccumulatorForTranslation(mTranslate);
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
	public EditorEvent processEvent(EditorEvent event) {
		final boolean db = false && DEBUG_ONLY_FEATURES;
		if (db)
			event.printProcessingMessage("DefaultEventListener");

		// By default, we'll be handling this event; so clear return code
		EditorEvent outputEvent = EditorEvent.NONE;

		switch (event.getCode()) {
		default:
			// Don't know how to handle this event, so restore return code
			outputEvent = event;
			break;

		case EditorEvent.CODE_DOWN:
			if (event.isMultipleTouch())
				break;
			mInitialDownLocation = event.getLocation();
			break;

		case EditorEvent.CODE_DRAG:
			if (!processingSingleTouch())
				break;
			if (!mDragStarted) {
				mDragStarted = true;
				doStartDrag(mInitialDownLocation);
			}
			doContinueDrag(event.getLocation());
			break;

		case EditorEvent.CODE_UP:
			if (!processingSingleTouch())
				break;
			outputEvent = EditorEvent.STOP;
			if (!mDragStarted) {
				doClick(mInitialDownLocation);
			} else {
				doFinishDrag();
			}
			break;
		}

		return outputEvent;
	}

	/**
	 * The DefaultEventListener, being a special type, may receive a DRAG or UP
	 * event before it has received any DOWN events. Use this method as a guard
	 * to ensure that a (single) DOWN event was received
	 */
	private boolean processingSingleTouch() {
		return mInitialDownLocation != null;
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
