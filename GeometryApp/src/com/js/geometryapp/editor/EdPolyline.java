package com.js.geometryapp.editor;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmStepper;

import static com.js.basic.Tools.*;

public class EdPolyline extends EdObject {

	public EdPolyline() {
	}

	@Override
	public boolean valid() {
		return nPoints() >= 2;
	}

	@Override
	public void render(AlgorithmStepper s) {
		Point prev = null;
		for (int i = 0; i < nPoints(); i++) {
			Point pt = getPoint(i);
			if (prev != null)
				s.plotLine(prev, pt);
			prev = pt;
		}
		super.render(s);
	}

	@Override
	public EditorEventListener buildEditOperation(Editor editor, int slot,
			Point location) {
		unimp("implement polyline-specific features");
		int vertexIndex = closestPoint(location, editor.pickRadius());
		if (vertexIndex >= 0)
			return new EditorOperation(editor, slot, vertexIndex);
		return null;
	}

	public float distFrom(Point targetPoint) {
		Point prev = null;
		float minDistance = -1;
		ASSERT(nPoints() >= 2);
		for (int i = 0; i < nPoints(); i++) {
			Point pt = getPoint(i);
			if (prev != null) {
				float distance = MyMath.ptDistanceToSegment(targetPoint, prev,
						pt, null);
				if (minDistance < 0)
					minDistance = distance;
				minDistance = Math.min(minDistance, distance);
			}
			prev = pt;
		}
		return minDistance;
	}

	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory("pl") {
		public EdObject construct() {
			return new EdPolyline();
		}

		@Override
		public EditorEventListener buildNewObjectEditorOperation(Editor editor,
				int slot) {
			return new EditorOperation(editor, slot, -1);
		}
	};

	private static class EditorOperation implements EditorEventListener {
		public EditorOperation(Editor editor, int slot, int vertexNumber) {
			mEditor = editor;
			mEditSlot = slot;
			mCursor = vertexNumber;
		}

		/**
		 * Initialize the edit operation, if it hasn't already been
		 * 
		 * This is necessary because we may start the operation without an
		 * EVENT_DOWN_x
		 */
		private void initializeOperation(Point location) {
			if (mOriginal != null)
				return;

			EdPolyline polyline = mEditor.objects().get(mEditSlot);
			mOriginal = mEditor.objects().getSubset(mEditSlot);

			while (polyline.nPoints() < 2) {
				polyline.addPoint(location);
			}

			if (mCursor < 0) {
				mCursor = polyline.nPoints() - 1;
			}
		}

		@Override
		public int processEvent(int eventCode, Point location) {

			final boolean db = true && DEBUG_ONLY_FEATURES;
			if (db)
				pr("EdPolyline processEvent "
						+ Editor.editorEventName(eventCode));

			if (location != null)
				initializeOperation(location);

			// By default, we'll be handling the event
			int returnCode = EVENT_NONE;

			switch (eventCode) {
			default:
				// we don't know how to handle this event, so pass it
				// through
				returnCode = eventCode;
				break;

			case EVENT_DOWN:
				break;

			case EVENT_DRAG: {
				EdPolyline polyline = mEditor.objects().get(mEditSlot);
				// Create a new copy of the segment, with modified endpoint
				EdPolyline polyline2 = (EdPolyline) polyline.clone();
				polyline2.setPoint(mCursor, location);
				if (db)
					pr(" changed endpoint; " + polyline2);
				mEditor.objects().set(mEditSlot, polyline2);
				mModified = true;
			}
				break;

			case EVENT_UP:
				if (db)
					pr(" modified " + mModified);
				if (mModified) {
					mEditor.pushCommand(Command.constructForEditedObjects(
							mEditor.objects(), mOriginal, FACTORY.getTag()));
				}
				// stop the operation on UP events
				returnCode = EVENT_STOP;
				break;

			case EVENT_UP_MULTIPLE:
				// stop the operation on UP events
				returnCode = EVENT_STOP;
				break;
			}
			return returnCode;
		}

		@Override
		public void render(AlgorithmStepper s) {
		}

		private Editor mEditor;
		// Index of object being edited
		private int mEditSlot;
		private int mCursor;
		private boolean mModified;
		private EdObjectArray mOriginal;
	}
}
