package com.js.geometryapp.editor;

import android.graphics.Color;
import android.graphics.Matrix;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.R;
import com.js.geometry.Rect;
import com.js.geometry.Sprite;

import static com.js.basic.Tools.*;

public class RotateOperation extends EditorEventListenerAdapter {

	public RotateOperation(Editor editor) {
		mEditor = editor;
		prepareRotateOperation();
		doNothing();
	}

	private boolean touchReasonableDistanceFromOrigin(Point touch) {
		return MyMath.distanceBetween(touch, mRect.midPoint()) > mEditor
				.pickRadius() * .5f;
	}

	@Override
	public EditorEvent processEvent(EditorEvent event) {

		// By default, we'll be handling this event; so clear return code
		EditorEvent outputEvent = EditorEvent.NONE;

		switch (event.getCode()) {
		default:
			// Don't know how to handle this event, so restore return code
			outputEvent = event;
			break;

		case EditorEvent.CODE_DOWN:
			prepareRotateOperation();
			// Get location of press within rotated space
			Point rotLoc = new Point(event.getLocation());
			rotLoc.apply(calcTotalRotateInvTransform());
			if (!mRect.contains(rotLoc)
					|| !touchReasonableDistanceFromOrigin(event.getLocation())) {
				outputEvent = EditorEvent.STOP;
				break;
			}
			mInitialTouchLocation = event.getLocation();
			mDragged = false;
			break;

		case EditorEvent.CODE_DRAG:
			mDragged = true;
			performRotate(event.getLocation());
			break;

		case EditorEvent.CODE_UP: {
			if (event.isMultipleTouch()) {
				outputEvent = EditorEvent.STOP;
				break;
			}
			if (!mDragged) {
				outputEvent = EditorEvent.STOP;
				break;
			}
			if (mOperationPrepared) {
				Command c = Command.constructForGeneralChanges(mOriginalState,
						new EditorState(mEditor), "rotate");
				mEditor.pushCommand(c);
				setUnprepared();
			}
		}
			break;
		}

		return outputEvent;
	}

	@Override
	public void render(AlgorithmStepper s) {
		boolean equalsOriginal = false;
		if (false) {
			// Render with emphasis if rotation angle matches some groove value
			// (multiple of 30 or 45 degrees for example)
			equalsOriginal = (mRotAmount == 0);
		}
		if (equalsOriginal)
			s.setColor(Color.argb(0xff, 0xff, 0x40, 0x40));
		else
			s.setColor(Color.argb(0x80, 0xff, 0x40, 0x40));

		Polygon p = new Polygon();
		for (int i = 0; i < 4; i++) {
			Point corner = mRect.corner(i);
			corner.apply(calcTotalRotateTransform());
			p.add(corner);
		}
		s.render(p);
		s.render(new Sprite(R.raw.crosshairicon, mRect.midPoint()));
	}

	@Override
	public boolean allowEditableObject() {
		return false;
	}

	/**
	 * Calculate the bounding rectangle for a set of objects
	 * 
	 * @param objects
	 * @return bounding rectangle, or null if there are no objects
	 */
	private Rect boundsForObjects(EdObjectArray objects) {
		Rect bounds = null;
		for (EdObject obj : objects) {
			Rect objBounds = obj.getBounds();
			if (bounds == null)
				bounds = objBounds;
			else
				bounds.include(objBounds);
		}
		return bounds;
	}

	private void prepareRotateOperation() {
		if (!mOperationPrepared) {
			mOriginalState = new EditorState(mEditor);
			// Don't replace an existing bounding rectangle, since it may have
			// been derived from a previous rotate procedure involving these
			// objects, and recalculating it may produce a different rectangle
			// which can be disorienting to the user.
			if (mRect == null) {
				mRect = boundsForObjects(mEditor.objects().getSelectedObjects());
				mRotStart = 0;
			}
			mRotAmount = 0;
			mOperationPrepared = true;
		}
	}

	private void setUnprepared() {
		if (mOperationPrepared) {
			// Add any previous rotation amount to the start rotation, so
			// the displayed rectangle starts with the same rotation
			mRotStart += mRotAmount;
			mRotAmount = 0;
			mRotMatrixCurrent = null;
			mRotMatrixTotal = null;
			mRotMatrixInvTotal = null;
			mOperationPrepared = false;
		}
	}

	private void performRotate(Point touchLocation) {
		if (!touchReasonableDistanceFromOrigin(touchLocation))
			return;
		Point origRay = MyMath
				.subtract(mInitialTouchLocation, mRect.midPoint());
		Point newRay = MyMath.subtract(touchLocation, mRect.midPoint());
		float origAngle = MyMath.polarAngle(origRay);
		float newAngle = MyMath.polarAngle(newRay);
		rotateObjects(newAngle - origAngle);
	}

	/**
	 * Calculate transform to rotate objects relative to their bounding rect's
	 * center
	 */
	private Matrix calcRotateTransform(float angle) {
		Matrix matrix = new Matrix();
		Matrix matrix2 = new Matrix();
		Point origin = mRect.midPoint();
		matrix2.setTranslate(-origin.x, -origin.y);
		matrix.postConcat(matrix2);
		matrix2.setRotate(angle / MyMath.M_DEG);
		matrix.postConcat(matrix2);
		matrix2.setTranslate(origin.x, origin.y);
		matrix.postConcat(matrix2);
		return matrix;
	}

	private Matrix calcCurrentRotateTransform() {
		if (mRotMatrixCurrent == null) {
			mRotMatrixCurrent = calcRotateTransform(mRotAmount);
		}
		return mRotMatrixCurrent;
	}

	private Matrix calcTotalRotateTransform() {
		if (mRotMatrixTotal == null) {
			mRotMatrixTotal = calcRotateTransform(mRotAmount + mRotStart);
			mRotMatrixInvTotal = calcRotateTransform(-(mRotAmount + mRotStart));
		}
		return mRotMatrixTotal;
	}

	private Matrix calcTotalRotateInvTransform() {
		calcTotalRotateTransform();
		return mRotMatrixInvTotal;
	}

	/**
	 * Replace selected objects with rotated counterparts
	 */
	private void rotateObjects(float angle) {
		if (mRotAmount != angle) {
			mRotAmount = angle;
			mRotMatrixCurrent = null;
			mRotMatrixTotal = null;
		}
		Matrix matrix = calcCurrentRotateTransform();
		for (int slot : mOriginalState.getSelectedSlots()) {
			EdObject object = mOriginalState.getObjects().get(slot);
			EdObject rotated = object.getCopy();
			rotated.applyTransform(matrix);
			mEditor.objects().set(slot, rotated);
		}
	}

	private Editor mEditor;

	// True if processing a down/drag/up rotation sequence
	private boolean mOperationPrepared;

	private EditorState mOriginalState;
	// Bounding rect of original objects
	private Rect mRect;
	private float mRotStart;
	private float mRotAmount;
	private Matrix mRotMatrixTotal;
	private Matrix mRotMatrixInvTotal;
	private Matrix mRotMatrixCurrent;

	private Point mInitialTouchLocation;
	// if user clicks without dragging, we'll cancel the operation;
	// this may be the only easy way if the selected objects occupy the whole
	// view
	private boolean mDragged;
}
