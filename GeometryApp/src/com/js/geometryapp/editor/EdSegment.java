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
		if (!complete())
			return;
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
		return nPoints() == 2;
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
		if (!DEBUG_ONLY_FEATURES)
			return null;
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

		@Override
		public EditorEventListener buildEditorOperation(Editor editor, int slot) {
			return new EditorOperation(editor, slot);
		}
	};

	private static class EditorOperation implements EditorEventListener {
		public EditorOperation(Editor editor, int slot) {
			mEditor = editor;
			mEditSlot = slot;
		}

		@Override
		public int processEvent(int eventCode, Point location) {

			// By default, we'll be handling the event
			int returnCode = EVENT_NONE;

			switch (eventCode) {
			default:
				// we don't know how to handle this event, so pass it
				// through
				returnCode = eventCode;
				break;

			case EVENT_DOWN: {
				EdSegment seg = mEditor.objects().get(mEditSlot);
				mOriginal = mEditor.objects().getList(mEditSlot);

				if (seg.nPoints() == 0) {
					seg.addPoint(location);
					seg.addPoint(location);
				}
			}
				break;

			case EVENT_DRAG: {
				EdSegment seg = mEditor.objects().get(mEditSlot);
				// Create a new copy of the segment, with modified endpoint
				EdSegment seg2 = (EdSegment) seg.clone();
				seg2.setPoint(1, location);
				mEditor.objects().set(mEditSlot, seg2);
				mModified = true;
			}
				break;

			case EVENT_UP:
				mEditor.clearOperation();
				if (mModified) {
					mEditor.pushCommand(Command.constructForEditedObjects(
							mEditor.objects(), mOriginal, "segendpoint"));
				}
				break;

			}
			return returnCode;
		}

		private Editor mEditor;
		// Index of object being edited
		private int mEditSlot;
		private boolean mModified;
		private EdObjectArray mOriginal;
	}
}
