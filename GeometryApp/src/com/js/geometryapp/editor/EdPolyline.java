package com.js.geometryapp.editor;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmStepper;

import static com.js.basic.Tools.*;

public class EdPolyline extends EdObject {

	private static final Point DEFAULT_OFFSET = new Point(20, 10);

	private EdPolyline(Point initialLocation) {
		addPoint(initialLocation);
		addPoint(MyMath.add(initialLocation, DEFAULT_OFFSET));
	}

	@Override
	public boolean valid() {
		return nPoints() >= 2 && mCursor >= 0 && mCursor < nPoints();
	}

	@Override
	public void render(Editor editor, AlgorithmStepper s) {
		prepareTabs(editor);
		Point prev = null;
		for (int i = 0; i < nPoints(); i++) {
			Point pt = getPoint(i);
			if (prev != null)
				s.plotLine(prev, pt);
			prev = pt;
		}
		super.render(editor, s);

		if (!isEditable())
			return;
		if (mTabsHidden)
			return;
		if (mInsertForwardTab != null)
			s.highlight(mInsertForwardTab, 1.5f);
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

	@Override
	public void setPoint(int ptIndex, Point point) {
		super.setPoint(ptIndex, point);
		mTabsValid = false;
	}

	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory("pl") {

		private static final String JSON_KEY_CURSOR = "c";

		public EdObject construct(Point initialLocation) {
			return new EdPolyline(initialLocation);
		}

		@Override
		public Map write(EdObject obj) {
			EdPolyline p = (EdPolyline) obj;
			Map map = super.write(obj);
			map.put(JSON_KEY_CURSOR, p.cursor());
			return map;
		}

		@Override
		public EdPolyline parse(JSONObject map) throws JSONException {
			EdPolyline p = super.parse(map);
			// TODO: is it necessary to persist the cursor?
			p.setCursor(map.optInt(JSON_KEY_CURSOR));
			return p;
		};
	};

	private void setCursor(int c) {
		mCursor = c;
	}

	private int cursor() {
		return mCursor;
	}

	private void prepareTabs(Editor editor) {
		if (!isEditable())
			return;
		if (mTabsValid)
			return;
		mInsertForwardTab = MyMath.pointOnCircle(getPoint(cursor()),
				MyMath.PI * .3f, editor.pickRadius() * 2);

		mTabsValid = true;
	}

	private void setTabsHidden(boolean f) {
		mTabsHidden = f;
	}

	void invalidateTabs() {
		mTabsValid = false;
	}

	// information concerning editable object
	private int mCursor;
	private boolean mTabsValid;
	private boolean mTabsHidden;
	private Point mInsertForwardTab;

	private static class EditorOperation implements EditorEventListener {

		public EditorOperation(Editor editor, int slot, int vertexNumber) {
			mEditor = editor;
			mEditSlot = slot;
			mOriginalObjectSet = mEditor.objects().getSubset(mEditSlot);
		}

		/**
		 * Initialize the edit operation, if it hasn't already been
		 * 
		 * This is necessary because we may start the operation without an
		 * EVENT_DOWN_x
		 */
		private void initializeOperation(Point location) {
			if (mInitialized)
				return;
			mInitialized = true;
		}

		@Override
		public int processEvent(int eventCode, Point location) {

			final boolean db = false && DEBUG_ONLY_FEATURES;
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
				EdPolyline pOrig = mEditor.objects().get(mEditSlot);
				// Create a new copy of the segment, with modified endpoint
				mNewPolyline = (EdPolyline) pOrig.clone();
				mNewPolyline.setPoint(mNewPolyline.cursor(), location);
				mEditor.objects().set(mEditSlot, mNewPolyline);
				mModified = true;
				mNewPolyline.setTabsHidden(true);
			}
				break;

			case EVENT_UP:
				if (db)
					pr(" modified " + mModified);
				if (mModified) {
					mEditor.pushCommand(Command.constructForEditedObjects(
							mEditor.objects(), mOriginalObjectSet,
							FACTORY.getTag()));
				}
				if (mNewPolyline != null)
					mNewPolyline.setTabsHidden(false);

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
		private boolean mModified;
		private EdObjectArray mOriginalObjectSet;
		private EdPolyline mNewPolyline;
		private boolean mInitialized;
	}

}
