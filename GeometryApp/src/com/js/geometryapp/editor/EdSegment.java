package com.js.geometryapp.editor;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmStepper;

import static com.js.basic.Tools.*;

public class EdSegment extends EdObject {

	public EdSegment() {
	}

	public EdSegment(float[] p1) {
		for (int i = 0; i < p1.length; i += 2)
			setPoint(i / 2, new Point(p1[i + 0], p1[i + 1]));
	}

	@Override
	public void render(AlgorithmStepper s) {
		s.plotLine(getPoint(0), getPoint(1));
		super.render(s);
	}

	public EdSegment(Point p1, Point p2) {
		setPoint(0, p1);
		setPoint(1, p2);
	}

	public EdSegment(float x0, float y0, float x1, float y1) {
		this(new Point(x0, y0), new Point(x1, y1));
	}

	public boolean complete() {
		return nPoints() >= 2;
	}

	public float distFrom(Point pt) {
		Point p1 = getPoint(0);
		Point p2 = getPoint(1);
		return MyMath.ptDistanceToSegment(pt, p1, p2, null);
	}

	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(nameOf(this));
		sb.append(" [");
		for (int i = 0; i < nPoints(); i++)
			sb.append(getPoint(i));
		sb.append("]");
		return sb.toString();
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory("seg") {
		public EdObject construct() {
			return new EdSegment();
		}
	};

	/**
	 * Construct an event handler for editor operations with these objects
	 */
	public static EditorEventListener buildEditorOperation(Editor editor) {
		return new EditorOperation(editor);
	}

	private static class EditorOperation implements EditorEventListener {
		public EditorOperation(Editor editor) {
			mEditor = editor;
		}

		@Override
		public int processEvent(int eventCode, Point location) {
			if (db)
				pr("EdSegment addNewOperation, event " + eventCode + " loc:"
						+ location);

			// By default, we'll be handling the event
			int returnCode = EVENT_NONE;

			switch (eventCode) {
			default:
				// we don't know how to handle this event, so pass it
				// through
				returnCode = eventCode;
				break;

			case EVENT_DOWN:
				if (db)
					pr("EVENT_DOWN, addNewPending " + mAddNewPending);
				if (mAddNewPending) {
					mAddingNew = true;
					mAddNewPending = false;
					EdSegment seg = new EdSegment(location, location);
					seg.setSelected(true);
					mEditIndex = mEditor.objects().add(seg);
					if (db)
						pr(" just added " + seg);
				}
				break;

			case EVENT_DRAG: {
				ASSERT(mEditIndex >= 0);
				EdSegment seg = (EdSegment) mEditor.objects().get(mEditIndex);
				// Create a new copy of the segment, with modified endpoint
				EdSegment seg2 = (EdSegment) seg.clone();
				seg2.setPoint(1, location);
				mEditor.objects().set(mEditIndex, seg2);
			}
				break;

			case EVENT_UP:
				if (mAddingNew) {
					mAddingNew = false;
					mEditor.clearOperation(this);
				}
				break;

			case EVENT_STOP:
				mAddNewPending = false;
				mAddingNew = false;
				mEditIndex = -1;
				break;

			case EVENT_ADD_NEW:
				mAddNewPending = true;
				break;
			}
			return returnCode;
		}

		private boolean mAddNewPending;
		private Editor mEditor;
		// Index of object being edited
		private int mEditIndex = -1;
		private boolean mAddingNew;
	}
}
