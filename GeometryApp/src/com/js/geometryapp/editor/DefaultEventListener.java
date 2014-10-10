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
	 * Unselect any currently selected objects within editor, optionally
	 * omitting a subset
	 * 
	 * @param omit
	 *            if not null, subset of objects whose selected state should not
	 *            change; must be in back to front order
	 */
	private void unselectObjects(List<Integer> omit) {
		int omitCursor = 0;
		for (int i = 0; i < mEditor.objects().size(); i++) {
			if (omit != null && omitCursor < omit.size()
					&& i == omit.get(omitCursor)) {
				omitCursor++;
				continue;
			}
			editorObject(i).setSelected(false);
		}
	}

	private void selectObjects(List<Integer> slots) {
		EdObjectArray list = mEditor.objects();
		for (int slot : slots) {
			EdObject obj = list.get(slot);
			obj.setSelected(true);
		}
	}

	private int mSelectedVertex;

	private int findSelectedObjectVertex(EdObject object, Point location) {
		int objVertex = -1;
		if (object.isSelected())
			objVertex = object.closestPoint(location, mEditor.pickRadius());
		mSelectedVertex = objVertex;
		return objVertex;
	}

	private void doClick(Point location) {
		// Construct pick set of selected objects. If empty, unselect
		// all objects; else cycle to next object and make it editable
		List<Integer> pickSet = getPickSet(location);
		if (pickSet.isEmpty()) {
			unselectObjects(null);
			return;
		}
		// Find selected item with highest index
		int highestIndex = pickSet.size();
		for (int i = 0; i < pickSet.size(); i++) {
			if (mEditor.objects().get(pickSet.get(i)).isSelected())
				highestIndex = i;
		}
		int nextSelectedIndex = MyMath.myMod(highestIndex - 1, pickSet.size());
		unselectObjects(null);
		mEditor.objects().get(pickSet.get(nextSelectedIndex)).setEditable(true);
	}

	private void doStartDrag(Point location) {
		/**
		 * <pre>
		 * 
		 * If there is an editable object, and location is at one of its vertices,
		 * edit that vertex;
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
		 * Drag a selection rectangle and select the items contained within it
		 * 
		 * </pre>
		 */
		List<Integer> pickSet = getPickSet(location);
		List<Integer> hlPickSet = getSelectedObjects(pickSet);

		if (hlPickSet.size() == 1) {
			int slot = hlPickSet.get(0);
			EdObject obj = editorObject(slot);
			if (obj.isEditable()) {
				int selVert = findSelectedObjectVertex(obj, location);
				if (selVert >= 0) {
					mEditor.startEditVertexOperation(slot, mSelectedVertex);
					return;
				}
			}
		}

		if (hlPickSet.isEmpty() && !pickSet.isEmpty()) {
			hlPickSet = SlotList.build(last(pickSet));
			unselectObjects(null);
			selectObjects(hlPickSet);
			// fall through to next...
		}
		if (!hlPickSet.isEmpty()) {
			// Get all selected objects, and store in a list since we
			// will want access to their original positions; then replace the
			// objects with copies that will be moved
			List<Integer> selSlots = mEditor.objects().getSelectedSlots();
			mMoveObjectsOriginals = mEditor.objects().getSubset(selSlots);
			mEditor.objects().replaceWithCopies(selSlots);
			mPreviousMoveLocation = mInitialDownLocation;
		} else {
			mDraggingRect = true;
			unselectObjects(null);
		}
	}

	private void doContinueDrag(Point location) {
		if (mDraggingRect) {
			mDragCorner = location;
		} else {
			Point delta = MyMath.subtract(location, mPreviousMoveLocation);
			if (delta.magnitude() == 0)
				return;

			for (int i = 0; i < mMoveObjectsOriginals.size(); i++) {
				int slot = mMoveObjectsOriginals.getSlot(i);
				EdObject obj = mEditor.objects().get(slot);
				EdObject orig = mMoveObjectsOriginals.get(i);
				obj.moveBy(orig, delta);
			}
		}
	}

	private void doFinishDrag() {
		if (mDraggingRect) {
			Rect dragRect = getDragRect();
			if (dragRect == null)
				return;
			for (EdObject edObject : mEditor.objects()) {
				edObject.setSelected(dragRect.contains(edObject
						.getBounds(mEditor)));
			}
		} else if (mMoveObjectsOriginals != null) {
			// Create command and undo
			Command cmd = Command.constructForEditedObjects(mEditor.objects(),
					mMoveObjectsOriginals, "move");
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
	private EdObjectArray mMoveObjectsOriginals;
	private Point mPreviousMoveLocation;
}
