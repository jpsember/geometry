package com.js.geometryapp.editor;

import android.graphics.Color;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmStepper;

import static com.js.basic.Tools.*;

public class EdSegment extends EdObject {

	private EdSegment() {
	}

	@Override
	public boolean valid() {
		return nPoints() == 2;
	}

	@Override
	public void render(AlgorithmStepper s) {
		s.setColor(Color.BLUE);
		renderLine(s, getPoint(0), getPoint(1));
		super.render(s);
	}

	@Override
	public EditorEventListener buildEditOperation(int slot, Point location) {
		int vertexIndex = closestVertex(location, editor().pickRadius());
		if (vertexIndex >= 0)
			return new EditorOperation(editor(), slot, vertexIndex);
		return null;
	}

	public float distFrom(Point pt) {
		Point p1 = getPoint(0);
		Point p2 = getPoint(1);
		return MyMath.ptDistanceToSegment(pt, p1, p2, null);
	}

	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory("seg") {
		public EdObject construct(Point defaultLocation) {
			EdSegment s = new EdSegment();
			if (defaultLocation != null) {
				s.addPoint(defaultLocation);
				s.addPoint(defaultLocation);
			}
			return s;
		}
	};

	private static class EditorOperation implements EditorEventListener {
		public EditorOperation(Editor editor, int slot, int vertexNumber) {
			mEditor = editor;
			mEditSlot = slot;
			mEditPointIndex = vertexNumber;
		}

		/**
		 * Initialize the edit operation, if it hasn't already been
		 * 
		 * This is necessary because we may start the operation without an
		 * EVENT_DOWN_x
		 */
		private void initializeOperation(Point location) {
			if (mOriginalState != null)
				return;

			mOriginalState = new EditorState(mEditor);
			EdSegment seg = mEditor.objects().get(mEditSlot);

			if (mEditPointIndex < 0) {
				mEditPointIndex = seg.nPoints() - 1;
			}
		}

		@Override
		public int processEvent(int eventCode, Point location) {

			final boolean db = false && DEBUG_ONLY_FEATURES;
			if (db)
				pr("EdSegment processEvent "
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
				EdSegment seg = mEditor.objects().get(mEditSlot);
				// Create a new copy of the segment, with modified endpoint
				EdSegment seg2 = (EdSegment) seg.clone();
				seg2.setPoint(mEditPointIndex, location);
				if (db)
					pr(" changed endpoint; " + seg2);
				mEditor.objects().set(mEditSlot, seg2);
				mModified = true;
			}
				break;

			case EVENT_UP:
				if (db)
					pr(" modified " + mModified);
				if (mModified) {
					mEditor.pushCommand(Command.constructForGeneralChanges(
							mOriginalState, new EditorState(mEditor),
							FACTORY.getTag()));
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
		// Index of point being edited
		private int mEditPointIndex;
		private boolean mModified;
		private EditorState mOriginalState;
		private Editor mEditor;
	}
}
