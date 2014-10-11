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
		Point[] tabLocations = null;
		if (isEditable() && !mTabsHidden) {
			tabLocations = calculateTabPositions(this);
		}

		Point cursor = null;
		if (tabLocations != null) {
			cursor = getPoint(mCursor);
			s.setColor(Color.GRAY);
			for (Point pt : tabLocations) {
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

		if (tabLocations != null) {
			s.highlight(cursor, 1.5f);
			for (Point pt : tabLocations) {
				if (pt == null)
					continue;
				s.highlight(pt, 1.5f);
			}
		}
	}

	private boolean targetWithinTab(Point target, Point tabLocation) {
		boolean within = false;
		if (tabLocation != null) {
			float dist = MyMath.distanceBetween(target, tabLocation);
			within = (dist <= editor().pickRadius());
		}
		return within;
	}

	@Override
	public EditorEventListener buildEditOperation(int slot, Point location) {

		Point[] tabLocations = calculateTabPositions(this);

		if (targetWithinTab(location, tabLocations[TAB_INSERT_FORWARD])) {
			EdPolyline mod = (EdPolyline) this.clone();
			// Insert a new vertex after the cursor
			mod.mCursor++;
			mod.addPoint(mod.mCursor, location);
			return EditorOperation.buildInsertOperation(editor(), slot, mod);
		}

		if (targetWithinTab(location, tabLocations[TAB_INSERT_BACKWARD])) {
			EdPolyline mod = (EdPolyline) this.clone();
			// Insert a new vertex before the cursor
			mod.addPoint(mod.mCursor, location);
			return EditorOperation.buildInsertOperation(editor(), slot, mod);
		}

		if (targetWithinTab(location, tabLocations[TAB_SPLIT])) {
			EdPolyline mod = (EdPolyline) this.clone();
			return EditorOperation.buildSplitOperation(editor(), slot, mod);
		}

		int vertexIndex = closestPoint(location, editor().pickRadius());
		if (vertexIndex >= 0) {
			mCursor = vertexIndex;
			EdPolyline mod = (EdPolyline) this.clone();
			mod.mCursor = vertexIndex;
			return EditorOperation.buildMoveOperation(editor(), slot, mod);
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

	private void setTabsHidden(boolean f) {
		mTabsHidden = f;
	}

	/**
	 * Calculate a reasonable place to put the insert vertex tabs for an
	 * editable polyline
	 * 
	 * @param polyline
	 * @return array of TAB_TOTAL tab locations
	 */
	private static Point[] calculateTabPositions(EdPolyline polyline) {
		Point[] tabLocations = new Point[TAB_TOTAL];
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
		a1 += diff + MyMath.PI;
		a2 -= diff;

		tabLocations[TAB_INSERT_BACKWARD] = MyMath.pointOnCircle(pb, a1, dist1);
		tabLocations[TAB_INSERT_FORWARD] = MyMath.pointOnCircle(pb, a2, dist2);
		if (polyline.closed()) {
			float a3 = MyMath.interpolateBetweenAngles(a1, a2, .5f);
			tabLocations[TAB_SPLIT] = MyMath
					.pointOnCircle(pb, a3, dist2 * 1.5f);
		}

		return tabLocations;
	}

	private boolean mClosed;

	// information concerning editable object
	private int mCursor;
	private boolean mTabsHidden;

	private static class EditorOperation implements EditorEventListener {

		private final static int OPER_MOVE = 0;
		private final static int OPER_INSERT = 1;
		private final static int OPER_SPLIT = 2;

		/**
		 * Constructor
		 * 
		 * @param editor
		 * @param slot
		 *            slot containing object being edited
		 * @param modified
		 *            new object
		 * @param vertexNumber
		 *            vertex number being edited
		 */
		private EditorOperation(Editor editor, int slot, EdPolyline modified,
				int operType) {
			mOperType = operType;
			mEditor = editor;
			mEditSlot = slot;
			mReference = modified;
			mOriginalObjectSet = new EdObjectArray();
			mOriginalObjectSet.add(editor.objects().get(slot));
			mOriginalObjectSet.setSlots(SlotList.build(slot));
			editor.objects().set(slot, mReference);
		}

		private EdPolyline activePolyline() {
			return mEditor.objects().get(mEditSlot);
		}

		public static EditorEventListener buildMoveOperation(Editor editor,
				int slot, EdPolyline mod) {
			EditorOperation oper = new EditorOperation(editor, slot, mod,
					EditorOperation.OPER_MOVE);
			return oper;
		}

		public static EditorEventListener buildInsertOperation(Editor editor,
				int slot, EdPolyline mod) {
			EditorOperation oper = new EditorOperation(editor, slot, mod,
					EditorOperation.OPER_INSERT);
			return oper;
		}

		public static EditorEventListener buildSplitOperation(Editor editor,
				int slot, EdPolyline mod) {
			EditorOperation oper = new EditorOperation(editor, slot, mod,
					EditorOperation.OPER_SPLIT);
			return oper;
		}

		@Override
		public int processEvent(int eventCode, Point location) {

			final boolean db = false && DEBUG_ONLY_FEATURES;
			if (db && eventCode != mPreviousEventProcessed)
				pr("EdPolyline processEvent "
						+ Editor.editorEventName(eventCode) + " loc "
						+ location);
			mPreviousEventProcessed = eventCode;

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
				// Create a new copy of the polyline, with modified endpoint
				EdPolyline polyline = (EdPolyline) mReference.clone();
				mEditor.objects().set(mEditSlot, polyline);
				polyline.setTabsHidden(true);
				if (mOperType == OPER_SPLIT) {
					// If we're moved sufficiently far from the original point,
					// perform a split
					mSignal = true;
					mSignalAlt = MyMath.distanceBetween(location,
							mReference.getPoint(mReference.cursor())) > mEditor
							.pickRadius() * 3;
					mChangesMade = mSignalAlt;
					if (mChangesMade) {
						polyline = constructSplitPolygon(polyline,
								polyline.cursor());
						mEditor.objects().set(mEditSlot, polyline);
					}
				} else {
					mChangesMade = true;
					polyline.setPoint(polyline.cursor(), location);
					int absorbVertex = findAbsorbingVertex(polyline);
					mSignal = (absorbVertex >= 0);
					if (mSignal) {
						mSignalAlt = true;
						polyline.setPoint(polyline.cursor(),
								polyline.getPoint(absorbVertex));
					}
				}
			}
				break;

			case EVENT_UP:
				if (mChangesMade) {
					switch (mOperType) {
					case OPER_MOVE:
					case OPER_INSERT:
						EdPolyline polyline = activePolyline();
						int absVert = findAbsorbingVertex(polyline);
						if (absVert >= 0) {
							performAbsorption(polyline, absVert);
						}
						break;
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
				activePolyline().setTabsHidden(false);
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
		private int findAbsorbingVertex(EdPolyline p) {
			float factor = absorptionFactor();
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

			Point na = p.getPointMod(cursor - 1);
			Point nb = p.getPointMod(cursor + 1);
			float angle = MyMath.polarAngleOfSegment(na, nb);
			float dist = mEditor.pickRadius() * .5f;

			for (int i = 0; i <= p.nPoints(); i++) {
				Point v = new Point(p.getPointMod(i + cursor));
				if (i == 0 || i == p.nPoints()) {
					v = MyMath.pointOnCircle(v, (i == 0) ? angle : angle
							+ MyMath.PI, dist);
				}
				c.addPoint(v);
			}
			return c;
		}

		private void performAbsorption(EdPolyline p, int absorberIndex) {
			int removedIndex = p.cursor();
			// Delete the cursor vertex
			p.removePoint(removedIndex);
			// Check if we're closing an open polygon by this procedure
			boolean closingFlag = Math.abs(removedIndex - absorberIndex) > 1;
			if (closingFlag)
				p.setClosed(closingFlag);
			p.setCursor(removedIndex < absorberIndex ? absorberIndex - 1
					: absorberIndex);
		}

		@Override
		public void render(AlgorithmStepper s) {
			if (mSignal) {
				EdPolyline polyline = activePolyline();
				Point signalLocation = polyline.getPoint(polyline.cursor());
				s.setColor(mSignalAlt ? Color.argb(0x40, 0x60, 0xff, 0x60)
						: Color.argb(0x40, 0xff, 0x80, 0x80));
				s.plot(signalLocation, 15);
			}
		}

		private float absorptionFactor() {
			// 'absorbing' vertices factor is much smaller when inserting;
			// this allows the user to place vertices very close together if
			// they desire
			switch (mOperType) {
			case OPER_INSERT:
				return ABSORBTION_FACTOR_WHILE_INSERTING;
			default:
				return ABSORBTION_FACTOR_NORMAL;
			}
		}

		// Index of object being edited
		private Editor mEditor;
		private int mEditSlot;
		private EdObjectArray mOriginalObjectSet;
		// polyline when editing operation began
		private EdPolyline mReference;
		private boolean mChangesMade;
		private boolean mSignal;
		private boolean mSignalAlt;
		private int mOperType;
		// To filter some debug printing only
		private int mPreviousEventProcessed;
	}

}
