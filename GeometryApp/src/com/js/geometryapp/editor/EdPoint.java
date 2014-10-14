package com.js.geometryapp.editor;

import android.graphics.Color;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmStepper;

import static com.js.basic.Tools.*;

public class EdPoint extends EdObject {

	private EdPoint() {
	}

	public Point location() {
		return getPoint(0);
	}

	@Override
	public boolean valid() {
		return nPoints() == 1;
	}

	@Override
	public float distFrom(Point pt) {
		return MyMath.distanceBetween(pt, location());
	}

	@Override
	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	@Override
	public EditorEventListener buildEditOperation(int slot, Point location) {
		int vertexIndex = closestVertex(location, editor().pickRadius());
		if (vertexIndex >= 0)
			return new EditorOperation(editor(), slot, vertexIndex);
		return null;
	}

	@Override
	public void render(AlgorithmStepper s) {
		if (isSelected()) {
			super.render(s);
		} else {
			s.setColor(Color.BLUE);
			s.plot(getPoint(0));
		}
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory("pt") {
		@Override
		public EdObject construct(Point defaultLocation) {
			EdPoint pt = new EdPoint();
			if (defaultLocation != null)
				pt.addPoint(defaultLocation);
			return pt;
		}

	};

	private static class EditorOperation implements EditorEventListener {
		public EditorOperation(Editor editor, int slot, int vertexNumber) {
			mEditor = editor;
			mEditSlot = slot;
		}

		/**
		 * Initialize the edit operation, if it hasn't already been
		 */
		private void initializeOperation(Point location) {
			if (mOriginal != null)
				return;

			EdPoint pt = mEditor.objects().get(mEditSlot);
			mOriginal = mEditor.objects().getSubset(mEditSlot);

			if (pt.nPoints() == 0) {
				pt.addPoint(location);
			}
		}

		@Override
		public int processEvent(int eventCode, Point location) {

			final boolean db = false && DEBUG_ONLY_FEATURES;
			if (db)
				pr("EdPoint processEvent " + Editor.editorEventName(eventCode));

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
				EdPoint pt = mEditor.objects().get(mEditSlot);
				// Create a new copy of the point, with modified location
				EdPoint pt2 = (EdPoint) pt.clone();
				pt2.setPoint(0, location);
				mEditor.objects().set(mEditSlot, pt2);
				mModified = true;
			}
				break;

			case EVENT_UP:
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

		// Index of object being edited
		private int mEditSlot;
		private boolean mModified;
		private EdObjectArray mOriginal;
		private Editor mEditor;
	}
}
