package com.js.geometryapp.editor;

import static com.js.basic.Tools.*;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;

import com.js.android.MyActivity;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgorithmStepper;

public class DefaultEventListener implements EditorEventListener {

	public DefaultEventListener(Editor editor) {
		mEditor = editor;
		reset();
	}

	private List<Integer> getPickSet(Point location, boolean selectedObjectsOnly) {
		float pickDistance = MyActivity.inchesToPixels(.22f);
		List<Integer> slots = new ArrayList();
		EdObjectArray srcObjects = mEditor.objects();
		for (int slot = 0; slot < srcObjects.size(); slot++) {
			EdObject src = srcObjects.get(slot);
			if (selectedObjectsOnly && !src.isSelected())
				continue;

			float dist = src.distFrom(location);
			if (dist > pickDistance)
				continue;
			slots.add(slot);
		}
		return slots;
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
		EdObjectArray list = mEditor.objects();
		for (int i = 0; i < list.size(); i++) {
			if (omit != null && omitCursor < omit.size()
					&& i == omit.get(omitCursor)) {
				omitCursor++;
				continue;
			}
			list.get(i).setSelected(false);
		}
	}

	private void doClick(Point location) {
		List<Integer> pickSet = getPickSet(location, false);
		unselectObjects(pickSet);
		if (pickSet.isEmpty())
			return;
		// Find selected item with highest index
		int highestIndex = pickSet.size();
		for (int i = 0; i < pickSet.size(); i++) {
			if (mEditor.objects().get(pickSet.get(i)).isSelected())
				highestIndex = i;
		}
		int nextSelectedIndex = MyMath.myMod(highestIndex - 1, pickSet.size());
		unselectObjects(null);
		mEditor.objects().get(pickSet.get(nextSelectedIndex)).setSelected(true);
	}

	private void doStartDrag(Point location) {
		// If initial press contains selected object, move all selected objects;
		// else,
		// do selection rectangle
		List<Integer> pickSet = getPickSet(location, true);
		if (pickSet.isEmpty()) {
			mDraggingRect = true;
		} else {
			// Get all selected objects
			mMoveObjectsSlots = mEditor.objects().getSelectedSlots();
			unselectObjects(mMoveObjectsSlots);

			// Make a copy of the objects to be moved, to remember their
			// original positions
			mMoveObjectsOriginals = mEditor.objects().deepCopy(
					mMoveObjectsSlots);
			mPreviousMoveLocation = mInitialDownLocation;
		}
	}

	private void doContinueDrag(Point location) {
		if (mDraggingRect) {
			mDragCorner = location;
		} else {
			Point delta = MyMath.subtract(location, mPreviousMoveLocation);
			if (delta.magnitude() == 0)
				return;
			for (int i = 0; i < mMoveObjectsSlots.size(); i++) {
				int slot = mMoveObjectsSlots.get(i);
				EdObject obj = mEditor.objects().get(slot);
				EdObject orig = mMoveObjectsOriginals.get(i);
				obj.moveBy(orig, delta);
				warning("keep objects immutable; slots is more important");
			}
		}
	}

	private void doFinishDrag() {
		if (mDraggingRect) {
			Rect dragRect = getDragRect();
			if (dragRect == null)
				return;
			for (EdObject edObject : mEditor.objects()) {
				edObject.setSelected(dragRect.contains(edObject.getBounds()));
			}
		} else {
		}
	}

	/**
	 * Clear any stateful variables to values they had before any event sequence
	 * was initiated
	 */
	private void reset() {
		mDragging = false;
		mDraggingRect = false;
		mDragCorner = null;
		mInitialDownLocation = null;
	}

	static {
		doNothing();
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
			reset();
			mInitialDownLocation = location;
			break;

		case EVENT_DRAG:
			if (!mDragging) {
				mDragging = true;
				doStartDrag(mInitialDownLocation);
			}
			doContinueDrag(location);
			break;

		case EVENT_UP:
			if (!mDragging) {
				doClick(mInitialDownLocation);
			} else {
				doFinishDrag();
			}
			reset();
			break;

		// A double tap will add another object of the last type added
		case EVENT_DOWN_MULTIPLE:
			reset();
			mInitialDownLocation = location;
			mEditor.startAddAnotherOperation();
			break;

		case EVENT_UP_MULTIPLE:
			reset();
			break;
		}

		return returnCode;
	}

	/**
	 * Perform any auxilliary rendering; specifically, the selection rectangle,
	 * if it's active
	 */
	public void render(AlgorithmStepper s) {
		Rect r = getDragRect();
		if (r != null) {
			Polygon p = new Polygon();
			for (int i = 0; i < 4; i++)
				p.add(r.corner(i));
			s.setColor(Color.argb(0x80, 0xff, 0x40, 0x40));
			s.plot(p, false);
		}
	}

	/**
	 * Get the rectangle for the current drag selection operation, or null if
	 * none is active
	 * 
	 * @return Rect, or null
	 */
	private Rect getDragRect() {
		if (mDragCorner != null)
			return new Rect(mInitialDownLocation, mDragCorner);
		return null;
	}

	private Editor mEditor;
	private Point mInitialDownLocation;
	private Point mDragCorner;
	private boolean mDragging;
	private boolean mDraggingRect;
	private List<Integer> mMoveObjectsSlots;
	private EdObjectArray mMoveObjectsOriginals;
	private Point mPreviousMoveLocation;
}
