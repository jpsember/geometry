package com.js.geometryapp.editor;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmStepper;

import static com.js.basic.Tools.*;

public class EdPolyline extends EdObject {

	private static final boolean DB_PLOT_RAYS = false && DEBUG_ONLY_FEATURES;
	private static final float ABSORBTION_FACTOR_NORMAL = 1.4f;
	private static final float ABSORBTION_FACTOR_WHILE_INSERTING = .3f;

	private EdPolyline() {
	}

	@Override
	public boolean valid() {
		return nPoints() >= 2 && mCursor >= 0 && mCursor < nPoints();
	}

	@Override
	public void render(AlgorithmStepper s) {
		prepareTabs();
		boolean showTabs = isEditable() && !mTabsHidden;
		Point cursor = null;

		if (showTabs) {
			cursor = getPoint(mCursor);
			s.setColor(Color.GRAY);
			s.plotLine(cursor, mInsertForwardTab);
			s.plotLine(cursor, mInsertBackwardTab);
		}

		Point prev = null;
		s.setColor(Color.BLUE);

		for (int i = 0; i < nPoints(); i++) {
			Point pt = getPoint(i);
			if (prev != null) {
				if (DB_PLOT_RAYS)
					s.plotRay(prev, pt);
				else
					s.plotLine(prev, pt);
			}
			prev = pt;
		}
		super.render(s);

		if (showTabs) {
			s.highlight(cursor, 1.5f);
			s.highlight(mInsertForwardTab, 1.5f);
			s.highlight(mInsertBackwardTab, 1.5f);
		}
	}

	private boolean targetWithinTab(Point target, Point tabLocation) {
		return MyMath.distanceBetween(target, tabLocation) <= editor()
				.pickRadius();
	}

	@Override
	public EditorEventListener buildEditOperation(int slot, Point location) {

		prepareTabs();

		// 'absorbing' vertices factor is much smaller with the insert tabs;
		// this allows the user to place vertices very close together if they
		// desire

		if (targetWithinTab(location, mInsertForwardTab)) {
			EdPolyline mod = (EdPolyline) this.clone();
			// Insert a new vertex after the cursor
			mod.mCursor++;
			mod.addPoint(mod.mCursor, location);
			return new EditorOperation(editor(), slot, mod)
					.setAbsorbFactor(ABSORBTION_FACTOR_WHILE_INSERTING);
		}

		if (targetWithinTab(location, mInsertBackwardTab)) {
			EdPolyline mod = (EdPolyline) this.clone();
			// Insert a new vertex before the cursor
			mod.addPoint(mod.mCursor, location);
			return new EditorOperation(editor(), slot, mod)
					.setAbsorbFactor(ABSORBTION_FACTOR_WHILE_INSERTING);
		}

		int vertexIndex = closestPoint(location, editor().pickRadius());
		if (vertexIndex >= 0) {
			mCursor = vertexIndex;
			invalidateTabs();
			EdPolyline mod = (EdPolyline) this.clone();
			mod.mCursor = vertexIndex;
			return new EditorOperation(editor(), slot, mod)
					.setAbsorbFactor(ABSORBTION_FACTOR_NORMAL);
		}
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

	@Override
	public void removePoint(int index) {
		unimp("have a 'points modified' flag we can check & clear in EdObject, so we don't need to override these methods");
		super.removePoint(index);
		mTabsValid = false;
	}

	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory("pl") {

		private static final String JSON_KEY_CURSOR = "c";

		public EdObject construct() {
			return new EdPolyline();
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

		@Override
		public int minimumPoints() {
			return 2;
		}
	};

	private void setCursor(int c) {
		mCursor = c;
	}

	public int cursor() {
		return mCursor;
	}

	private static float calcAngle(Point p1, Point p2) {
		if (MyMath.distanceBetween(p1, p2) < 5) {
			return 0;
		}
		return MyMath.polarAngleOfSegment(p1, p2);
	}

	private void prepareTabs() {
		if (!isEditable())
			return;
		if (mTabsValid)
			return;

		Point[] tabs = calculateInsertTabPositions(this);
		mInsertBackwardTab = tabs[0];
		mInsertForwardTab = tabs[1];

		mTabsValid = true;
	}

	private void setTabsHidden(boolean f) {
		mTabsHidden = f;
	}

	void invalidateTabs() {
		mTabsValid = false;
	}

	/**
	 * Calculate a reasonable place to put the insert vertex tabs for an
	 * editable polyline
	 * 
	 * @param polyline
	 * @return array of [backward, forward] insertion tab locations
	 */
	private static Point[] calculateInsertTabPositions(EdPolyline polyline) {
		Point[] tabs = new Point[2];
		int c = polyline.cursor();

		float defaultDist = polyline.editor().pickRadius() * 1.3f;
		float dist1 = defaultDist;
		float dist2 = defaultDist;

		float a1 = 0, a2 = 0;
		boolean convex = true;
		Point pa = null;
		Point pb = polyline.getPoint(c);
		Point pc = null;
		if (c > 0)
			pa = polyline.getPoint(c - 1);
		if (c + 1 < polyline.nPoints())
			pc = polyline.getPoint(c + 1);

		if (pa != null) {
			dist1 = MyMath.distanceBetween(pa, pb) * .5f;
			a1 = calcAngle(pa, pb);
		}
		if (pc != null) {
			dist2 = MyMath.distanceBetween(pb, pc) * .5f;
			a2 = calcAngle(pb, pc);
		}
		dist1 = MyMath.clamp(dist1, defaultDist * .8f, defaultDist * 1.2f);
		dist2 = MyMath.clamp(dist2, defaultDist * .8f, defaultDist * 1.2f);

		if (pa == null)
			a1 = a2;
		if (pc == null)
			a2 = a1;
		if (pa != null && pc != null) {
			convex = MyMath.sideOfLine(pa, pb, pc) > 0;
		}
		float diff = MyMath.M_DEG * 15;
		if (!convex) {
			diff = -diff;
		}
		a1 += diff;
		a2 -= diff;

		tabs[0] = MyMath.pointOnCircle(pb, a1 + MyMath.PI, dist1);
		tabs[1] = MyMath.pointOnCircle(pb, a2, dist2);

		return tabs;
	}

	// information concerning editable object
	private int mCursor;
	private boolean mTabsValid;
	private boolean mTabsHidden;
	private Point mInsertForwardTab;
	private Point mInsertBackwardTab;

	private static class EditorOperation implements EditorEventListener {

		/**
		 * Constructor
		 * 
		 * @param editor
		 * @param slot
		 *            slot containing object being edited
		 * @param vertexNumber
		 *            vertex number being edited
		 * @param original
		 *            original object (before editing)
		 */
		public EditorOperation(Editor editor, int slot, EdPolyline modified) {
			mEditor = editor;
			mEditSlot = slot;
			mOriginalObjectSet = new EdObjectArray();
			mOriginalObjectSet.add(editor.objects().get(slot));
			mOriginalObjectSet.setSlots(SlotList.build(slot));
			editor.objects().set(slot, modified);
		}

		public EditorOperation setAbsorbFactor(float factor) {
			mAbsorptionFactor = factor;
			return this;
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
				int absorbVertex = findAbsorbingVertex(mNewPolyline,
						mAbsorptionFactor);
				mAbsorbSignal = (absorbVertex >= 0);
				if (mAbsorbSignal) {
					mNewPolyline.setPoint(mNewPolyline.cursor(),
							mNewPolyline.getPoint(absorbVertex));
				}
				mEditor.objects().set(mEditSlot, mNewPolyline);
				mModified = true;
				mNewPolyline.setTabsHidden(true);
			}
				break;

			case EVENT_UP:
				if (db)
					pr(" modified " + mModified);
				if (mModified) {
					// Determine if user just dragged a vertex essentially on
					// top of one of its neighbors. If so, the vertex is
					// 'absorbed': delete that vertex
					int absVert = findAbsorbingVertex(mNewPolyline,
							mAbsorptionFactor);
					if (absVert >= 0) {
						performAbsorption(mNewPolyline, absVert);
					}
					// Don't allow any merging with polygon commands, because
					// the user may end up doing a lot of work on a single
					// polygon and he should be able to undo individual steps
					mEditor.pushCommand(Command.constructForEditedObjects(
							mEditor.objects(), mOriginalObjectSet, null));
				}
				// stop the operation on UP events
				returnCode = EVENT_STOP;
				break;

			case EVENT_UP_MULTIPLE:
				// stop the operation on UP events
				returnCode = EVENT_STOP;
				break;

			case EVENT_STOP:
				if (mNewPolyline != null) {
					mNewPolyline.setTabsHidden(false);
				}
				break;

			}
			return returnCode;
		}

		/**
		 * Determine which vertex, if any, is close enough to absorb the
		 * cursor's
		 * 
		 * @param p
		 *            polyline
		 * @param factor
		 * @return index of absorbing vertex, or -1
		 */
		private int findAbsorbingVertex(EdPolyline p, float factor) {
			if (p.nPoints() == 2)
				return -1;
			Point cp = p.getPoint(p.cursor());
			for (int pass = 0; pass < 2; pass++) {
				int delta = (pass == 0) ? -1 : 1;
				int neighbor = p.cursor() + delta;
				if (neighbor < 0 || neighbor >= p.nPoints())
					continue;
				Point c2 = p.getPoint(neighbor);
				float dist = MyMath.distanceBetween(cp, c2);
				if (dist >= mEditor.pickRadius() * factor)
					continue;
				return neighbor;
			}
			return -1;
		}

		private void performAbsorption(EdPolyline p, int absorberIndex) {
			// Delete the cursor vertex
			p.removePoint(p.cursor());
			p.setCursor(Math.min(p.cursor(), absorberIndex));
		}

		@Override
		public void render(AlgorithmStepper s) {
			if (mAbsorbSignal) {
				Point signalLocation = mNewPolyline.getPoint(mNewPolyline
						.cursor());
				s.setColor(Color.argb(0x40, 0xff, 0x80, 0x80));
				s.plot(signalLocation, 15);
			}
		}

		// Index of object being edited
		private int mEditSlot;
		private boolean mModified;
		private EdObjectArray mOriginalObjectSet;
		private EdPolyline mNewPolyline;
		private boolean mInitialized;
		private Editor mEditor;
		private float mAbsorptionFactor;
		private boolean mAbsorbSignal;
	}

}
