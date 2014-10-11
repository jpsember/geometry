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

	private static final int TAB_INSERT_BACKWARD = 0;
	private static final int TAB_INSERT_FORWARD = 1;
	private static final int TAB_SPLIT = 2;
	private static final int TAB_TOTAL = 3;

	private EdPolyline() {
	}

	@Override
	public boolean valid() {
		return nPoints() >= 2 && mCursor >= 0 && mCursor < nPoints();
	}

	@Override
	public void render(AlgorithmStepper s) {
		if (mAlternate != null) {
			mAlternate.render(s);
			return;
		}
		// Do we need to prepare tabs here? Keep in mind we're in the OpenGL
		// thread
		prepareTabs();

		boolean showTabs = isEditable() && !mTabsHidden;
		Point cursor = null;

		if (showTabs) {
			cursor = getPoint(mCursor);
			s.setColor(Color.GRAY);
			for (Point pt : mTabs) {
				if (pt == null)
					continue;
				s.plotLine(cursor, pt);
			}
		}

		Point prev = null;
		if (closed() && nPoints() > 2)
			prev = getPointMod(-1);

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

		if (showTabs && mTabs != null) {
			s.highlight(cursor, 1.5f);
			for (Point pt : mTabs) {
				if (pt == null)
					continue;
				s.highlight(pt, 1.5f);
			}
		}
	}

	private boolean targetWithinTab(Point target, int tabIndex) {
		boolean within = false;
		Point tabLocation = mTabs[tabIndex];
		if (tabLocation != null) {
			float dist = MyMath.distanceBetween(target, tabLocation);
			within = (dist <= editor().pickRadius());
		}
		return within;
	}

	@Override
	public EditorEventListener buildEditOperation(int slot, Point location) {

		prepareTabs();

		// 'absorbing' vertices factor is much smaller with the insert tabs;
		// this allows the user to place vertices very close together if they
		// desire

		if (targetWithinTab(location, TAB_INSERT_FORWARD)) {
			EdPolyline mod = (EdPolyline) this.clone();
			// Insert a new vertex after the cursor
			mod.mCursor++;
			mod.addPoint(mod.mCursor, location);
			return new EditorOperation(editor(), slot, mod)
					.setAbsorbFactor(ABSORBTION_FACTOR_WHILE_INSERTING);
		}

		if (targetWithinTab(location, TAB_INSERT_BACKWARD)) {
			EdPolyline mod = (EdPolyline) this.clone();
			// Insert a new vertex before the cursor
			mod.addPoint(mod.mCursor, location);
			return new EditorOperation(editor(), slot, mod)
					.setAbsorbFactor(ABSORBTION_FACTOR_WHILE_INSERTING);
		}

		if (targetWithinTab(location, TAB_SPLIT)) {
			EdPolyline mod = (EdPolyline) this.clone();
			return new EditorOperation(editor(), slot, mod).setSplitting();
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
		int max = closed() ? nPoints() : nPoints() - 1;
		for (int i = 0; i <= max; i++) {
			Point pt = getPointMod(i);
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
		invalidateTabs();
	}

	@Override
	public void removePoint(int index) {
		unimp("have a 'points modified' flag we can check & clear in EdObject, so we don't need to override these methods");
		super.removePoint(index);
		invalidateTabs();
	}

	public EdObjectFactory getFactory() {
		return FACTORY;
	}

	public static EdObjectFactory FACTORY = new EdObjectFactory("pl") {

		private static final String JSON_KEY_CURSOR = "c";
		private static final String JSON_KEY_CLOSED = "cl";

		public EdObject construct() {
			return new EdPolyline();
		}

		@Override
		public Map write(EdObject obj) {
			EdPolyline p = (EdPolyline) obj;
			Map map = super.write(obj);
			map.put(JSON_KEY_CURSOR, p.cursor());
			map.put(JSON_KEY_CLOSED, p.closed());
			return map;
		}

		@Override
		public EdPolyline parse(JSONObject map) throws JSONException {
			EdPolyline p = super.parse(map);
			// TODO: is it necessary to persist the cursor?
			p.setCursor(map.optInt(JSON_KEY_CURSOR));
			p.setClosed(map.optBoolean(JSON_KEY_CLOSED));
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

	public boolean closed() {
		return mClosed;
	}

	public void setClosed(boolean f) {
		mClosed = f;
	}

	private static float calcAngle(Point p1, Point p2) {
		if (MyMath.distanceBetween(p1, p2) < 5) {
			return 0;
		}
		return MyMath.polarAngleOfSegment(p1, p2);
	}

	private void prepareTabs() {
		if (!isEditable() || mTabsHidden)
			return;
		if (mTabs != null)
			return;
		mTabs = calculateTabPositions(this);
	}

	private void setTabsHidden(boolean f) {
		mTabsHidden = f;
	}

	void invalidateTabs() {
		mTabs = null;
	}

	/**
	 * Calculate a reasonable place to put the insert vertex tabs for an
	 * editable polyline
	 * 
	 * @param polyline
	 * @return array of [backward, forward] insertion tab locations
	 */
	private static Point[] calculateTabPositions(EdPolyline polyline) {
		Point[] tabs = new Point[TAB_TOTAL];
		int c = polyline.cursor();

		float defaultDist = polyline.editor().pickRadius() * 1.3f;
		float dist1 = defaultDist;
		float dist2 = defaultDist;

		float a1 = 0, a2 = 0;
		boolean convex = true;
		Point pa = null;
		Point pb = polyline.getPoint(c);
		Point pc = null;
		if (c > 0 || polyline.closed())
			pa = polyline.getPointMod(c - 1);
		if (c + 1 < polyline.nPoints() || polyline.closed())
			pc = polyline.getPointMod(c + 1);

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

		tabs[TAB_INSERT_BACKWARD] = MyMath.pointOnCircle(pb, a1 + diff
				+ MyMath.PI, dist1);
		tabs[TAB_INSERT_FORWARD] = MyMath.pointOnCircle(pb, a2 - diff, dist2);
		if (polyline.closed()) {
			// TODO: figure out how to put a3 between a1 and a2 (radial math
			// confusion)
			float a3 = -MyMath.PI / 2; // /MATH_PI/2; //a2 + (a1 - a2) / 2;
			tabs[TAB_SPLIT] = MyMath.pointOnCircle(pb, a3, dist2 * 1.5f);
		}

		return tabs;
	}

	private boolean mClosed;

	// information concerning editable object
	private int mCursor;
	private boolean mTabsHidden;
	private Point[] mTabs;
	private EdPolyline mAlternate;

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

		public EditorEventListener setSplitting() {
			mSplitting = true;
			return this;
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
			if (db && eventCode != mPreviousEventProcessed)
				pr("EdPolyline processEvent "
						+ Editor.editorEventName(eventCode) + " loc "
						+ location);
			mPreviousEventProcessed = eventCode;

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
				if (mSplitting) {
					EdPolyline origPolyline = (EdPolyline) mOriginalObjectSet
							.get(0);
					// If we're moved sufficiently far from the original point,
					// set the split flag
					boolean mSplitSignal = MyMath.distanceBetween(location,
							origPolyline.getPoint(origPolyline.cursor())) > mEditor
							.pickRadius() * 3;
					mSplitVersion = null;
					if (mSplitSignal) {
						mSplitVersion = constructSplitPolygon(origPolyline,
								origPolyline.cursor());
						mSplitVersion.setTabsHidden(true);
					}
					mNewPolyline.mAlternate = mSplitVersion;
				} else {
					int absorbVertex = findAbsorbingVertex(mNewPolyline,
							mAbsorptionFactor);
					mAbsorbSignal = (absorbVertex >= 0);
					if (mAbsorbSignal) {
						mNewPolyline.setPoint(mNewPolyline.cursor(),
								mNewPolyline.getPoint(absorbVertex));
					}
					mModified = true;
				}
				mEditor.objects().set(mEditSlot, mNewPolyline);
				mNewPolyline.setTabsHidden(true);
			}
				break;

			case EVENT_UP:
				if (db)
					pr(" modified " + mModified);
				if (mSplitting) {
					if (mSplitVersion != null) {
						mSplitVersion.setTabsHidden(false);
						mSplitVersion.invalidateTabs();
						mEditor.objects().set(mEditSlot, mSplitVersion);
					}
				} else if (mModified) {
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
					mNewPolyline.mAlternate = null;
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
				int neighbor = MyMath.myMod(p.cursor() + delta, p.nPoints());
				Point c2 = p.getPoint(neighbor);
				float dist = MyMath.distanceBetween(cp, c2);
				if (dist >= mEditor.pickRadius() * factor)
					continue;
				return neighbor;
			}
			return -1;
		}

		/**
		 * Construct an open polyline by splitting a closed one at a particular
		 * vertex
		 */
		private EdPolyline constructSplitPolygon(EdPolyline p, int cursor) {
			ASSERT(p.closed());
			EdPolyline c = (EdPolyline) p.clone();
			c.setClosed(false);
			c.setCursor(0);
			c.clearPoints();

			for (int i = 0; i <= p.nPoints(); i++) {
				Point v = new Point(p.getPointMod(i + cursor));
				if (i == 0 || i == p.nPoints()) {
					// translate point a bit
					v.x += (i == 0) ? 20 : -20;
				}
				c.addPoint(v);
			}
			return c;
		}

		private void performAbsorption(EdPolyline p, int absorberIndex) {
			// Delete the cursor vertex
			p.removePoint(p.cursor());
			// Check if we're closing an open polygon by this procedure
			boolean closingFlag = Math.abs(p.cursor() - absorberIndex) > 1;
			if (closingFlag)
				p.setClosed(closingFlag);
			p.setCursor(Math.min(p.cursor(), absorberIndex));
		}

		@Override
		public void render(AlgorithmStepper s) {
			if (mAbsorbSignal || (mSplitVersion != null)) {
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
		private boolean mSplitting;
		private EdPolyline mSplitVersion;
		// To filter some debug printing only
		private int mPreviousEventProcessed;
	}

}
