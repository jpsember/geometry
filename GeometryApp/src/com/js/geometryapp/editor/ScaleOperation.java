package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Matrix;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgorithmStepper;
import static com.js.basic.Tools.*;

public class ScaleOperation implements EditorEventListener {

	private static final int NUM_HANDLES = 8;

	public ScaleOperation(Editor editor) {
		mEditor = editor;
		prepareOperation();
	}

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
			prepareOperation();
			// mInitialDownLocation = location;
			// Find handle
			{
				float minDist = 0;
				int minHandle = -1;
				for (int i = 0; i < NUM_HANDLES; i++) {
					float dist = MyMath.distanceBetween(location,
							handleBaseLocation(i));
					if (minHandle < 0 || dist < minDist) {
						minDist = dist;
						minHandle = i;
					}
				}
				if (minDist > mEditor.pickRadius()) {
					returnCode = EVENT_STOP;
					break;
				}
				mActiveHandle = minHandle;
				mInitialHandleOffset = MyMath.subtract(
						handleBaseLocation(mActiveHandle), location);
			}
			break;

		case EVENT_DRAG:
			performScale(mActiveHandle, location);
			break;

		case EVENT_UP: {
			if (mRect != null) {
				Command c = Command.constructForGeneralChanges(mOriginalState,
						new EditorState(mEditor), "scale");
				mEditor.pushCommand(c);
			}
			setUnprepared();
		}
			break;

		case EVENT_UP_MULTIPLE:
			returnCode = EVENT_STOP;
			break;
		}

		return returnCode;
	}

	@Override
	public void render(AlgorithmStepper s) {

		doNothing();
		s.setColor(Color.argb(0x80, 0xff, 0x40, 0x40));

		// Calculate handles corresponding to scaled rect
		List<Point> handles = new ArrayList();
		calculateHandleBaseLocations(mScaledRect, handles);
		for (int i = 0; i < sLinesBetweenHandles.length; i += 2)
			s.plotLine(handles.get(sLinesBetweenHandles[i]),
					handles.get(sLinesBetweenHandles[i + 1]));
		for (int i = 0; i < NUM_HANDLES; i++)
			s.plotSprite(R.raw.squareicon, handles.get(i));
	}

	private static final int[] sLinesBetweenHandles = { 0, 2, 2, 4, 4, 6, 6, 0,
			1, 5, 3, 7, };

	/**
	 * Calculate the bounding rectangle for a set of objects
	 * 
	 * @param objects
	 * @return bounding rectangle, or null if there are no objects
	 */
	private Rect boundsForObjects(EdObjectArray objects) {
		Rect bounds = null;
		for (EdObject obj : objects) {
			Rect objBounds = obj.getBounds(mEditor);
			if (bounds == null)
				bounds = objBounds;
			else
				bounds.include(objBounds);
		}
		return bounds;
	}

	private void prepareOperation() {
		if (mRect == null) {
			mOriginalState = new EditorState(mEditor);
			mHandles = new ArrayList();
			mRect = boundsForObjects(mEditor.objects().getSelectedObjects());
			mScaledRect = mRect;
			calculateHandleBaseLocations(mRect, mHandles);
		}
	}

	private void setUnprepared() {
		mRect = null;
	}

	/**
	 * Calculate handle base locations for a particular bounding rectangle.
	 * 
	 * A 'handle' is represented by an icon that the user can grab to perform a
	 * scale operation. Each handle has a 'base location' which corresponds to
	 * its location before scaling begins.
	 */
	private void calculateHandleBaseLocations(Rect boundingRect,
			List<Point> handleLocations) {
		Point p0 = new Point(boundingRect.x, boundingRect.y);
		Point p1 = new Point(boundingRect.midX(), p0.y);
		Point p2 = new Point(boundingRect.endX(), p0.y);
		Point p3 = new Point(p2.x, boundingRect.midY());
		Point p4 = new Point(p2.x, boundingRect.endY());
		Point p5 = new Point(p1.x, p4.y);
		Point p6 = new Point(p0.x, p4.y);
		Point p7 = new Point(p0.x, p3.y);
		handleLocations.clear();
		handleLocations.add(p0);
		handleLocations.add(p1);
		handleLocations.add(p2);
		handleLocations.add(p3);
		handleLocations.add(p4);
		handleLocations.add(p5);
		handleLocations.add(p6);
		handleLocations.add(p7);
	}

	/**
	 * Filter user's touch location to be a valid new location for a particular
	 * handle
	 * 
	 * @param handle
	 *            index of handle being adjusted
	 * @param touchLocation
	 *            user's touch location
	 */
	private Point filteredHandle(int handle, Point touchLocation) {
		touchLocation = MyMath.add(touchLocation, mInitialHandleOffset);
		float x0 = touchLocation.x;
		float x1 = touchLocation.x;
		float y0 = touchLocation.y;
		float y1 = touchLocation.y;

		// Add some padding to stop the scaled rectangle from becoming
		// degenerate
		final float PADDING = 10;
		if (handle == 0 || handle == 1 || handle == 2)
			y1 = mRect.midY() - PADDING;
		if (handle == 2 || handle == 3 || handle == 4)
			x0 = mRect.midX() + PADDING;
		if (handle == 4 || handle == 5 || handle == 6)
			y0 = mRect.midY() + PADDING;
		if (handle == 6 || handle == 7 || handle == 0)
			x1 = mRect.midX() - PADDING;

		touchLocation.setTo(MyMath.clamp(touchLocation.x, x0, x1),
				MyMath.clamp(touchLocation.y, y0, y1));

		// Replace new handle location with its projection to the line between
		// the handle and the midpoint
		Point origin = mRect.midPoint();
		MyMath.ptDistanceToLine(touchLocation, handleBaseLocation(handle),
				origin, touchLocation);
		return touchLocation;
	}

	private Point handleBaseLocation(int handleIndex) {
		return mHandles.get(handleIndex);
	}

	/**
	 * Calculate the scaled rectangle corresponding to a particular handle
	 * location
	 */
	private Rect calculateScaledRect(int handle, Point handleLocation) {
		Point origin = mRect.midPoint();
		float w = mRect.width / 2;
		float h = mRect.height / 2;
		if (handle != 1 && handle != 5)
			w = Math.abs(origin.x - handleLocation.x);
		if (handle != 3 && handle != 7)
			h = Math.abs(origin.y - handleLocation.y);
		return new Rect(origin.x - w, origin.y - h, w * 2, h * 2);
	}

	private void performScale(int handle, Point dl) {
		dl = filteredHandle(handle, dl);
		mScaledRect = calculateScaledRect(handle, dl);
		scaleObjects();
	}

	/**
	 * Calculate transform to scale objects relative to their bounding rect's
	 * center
	 */
	private Matrix calcScaleTransform() {
		Matrix matrix = new Matrix();
		Matrix matrix2 = new Matrix();
		Point origin = mRect.midPoint();

		matrix2.setTranslate(-origin.x, -origin.y);
		matrix.postConcat(matrix2);
		matrix2.setScale(mScaledRect.width / mRect.width, mScaledRect.height
				/ mRect.height);
		matrix.postConcat(matrix2);
		matrix2.setTranslate(origin.x, origin.y);
		matrix.postConcat(matrix2);
		return matrix;
	}

	/**
	 * Replace selected objects with scaled counterparts
	 */
	private void scaleObjects() {
		Matrix matrix = calcScaleTransform();
		for (int slot : mOriginalState.getSelectedSlots()) {
			EdObject object = mOriginalState.getObjects().get(slot);
			EdObject scaled = object.getCopy();
			scaled.applyTransform(matrix);
			mEditor.objects().set(slot, scaled);
		}
	}

	private Editor mEditor;
	private EditorState mOriginalState;
	// Bounding rect of unscaled objects
	private Rect mRect;
	// Handle base locations for unscaled objects
	private List<Point> mHandles;
	// Scaled bounding rect
	private Rect mScaledRect;
	// Which handle the user is adjusting
	private int mActiveHandle;
	// amount to add to user touch location to place exactly at handle
	private Point mInitialHandleOffset;
}
