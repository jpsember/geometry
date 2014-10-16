package com.js.geometryapp.editor;

import java.util.ArrayList;

import android.graphics.Color;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgorithmStepper;
import static com.js.basic.Tools.*;

public abstract class EdObject implements Cloneable {

	private static final int FLAG_SELECTED = (1 << 31);
	private static final int FLAG_EDITABLE = (1 << 30);

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

	/**
	 * Determine if this object is well-formed. Called after reading object from
	 * JSON, for example
	 */
	public boolean valid() {
		return true;
	}

	/**
	 * If possible, construct an operation to edit this editable object
	 * 
	 * @param slot
	 * @param location
	 *            location of user press; e.g., to see if it is at a draggable
	 *            vertex
	 * @return operation, or null
	 */
	public abstract EditorEventListener buildEditOperation(int slot,
			Point location);

	/**
	 * Replace object's points with those of another object
	 */
	private void copyPointsFrom(EdObject src) {
		// Both source and destination objects may have the same point array, so
		// construct a new one
		ArrayList<Point> newPts = new ArrayList();
		for (Point pt : src.mPoints)
			newPts.add(new Point(pt));
		mPoints = newPts;
	}

	/**
	 * Clone the object
	 */
	public Object clone() {
		try {
			EdObject e = (EdObject) super.clone();
			e.copyPointsFrom(this);
			return e;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Object has just been made editable
	 * 
	 * @param location
	 *            location where user pressed to cause this
	 */
	public void selectedForEditing(Point location) {
	}

	/**
	 * Determine if object is selected
	 */
	public boolean isSelected() {
		return hasFlags(FLAG_SELECTED);
	}

	/**
	 * Set object's selected state
	 */
	public void setSelected(boolean f) {
		int flags = FLAG_SELECTED;
		if (!f)
			flags |= FLAG_EDITABLE;
		setFlags(flags, f);
	}

	public boolean isEditable() {
		return hasFlags(FLAG_EDITABLE);
	}

	public void setEditable(boolean f) {
		int flags = FLAG_EDITABLE;
		if (f)
			flags |= FLAG_SELECTED;
		setFlags(flags, f);
	}

	/**
	 * Get bounding rectangle of object. Default implementation calculates
	 * minimum bounding rectangle of the object's points
	 * 
	 * @return FRect
	 */
	public Rect getBounds(Editor editor) {
		if (mBounds == null) {
			mBounds = Rect.rectContainingPoints(mPoints);
			if (isSelected()) {
				float r = editor.pickRadius();
				mBounds.inset(-r, -r);
			}
		}
		return mBounds;
	}

	/**
	 * Set point, by replacing existing (if index < size()) or by adding new (if
	 * index == size())
	 */
	public void setPoint(int index, Point point) {
		storePoint(index, new Point(point));
	}

	public void clearPoints() {
		mPoints.clear();
		mBounds = null;
	}

	/**
	 * Store a point, without copying it
	 * 
	 * @param ptIndex
	 *            index of point; must be index of an existing point, or the
	 *            current number of points
	 * @param point
	 *            location of point
	 */
	private void storePoint(int ptIndex, Point point) {
		if (mPoints.size() == ptIndex)
			mPoints.add(point);
		else
			mPoints.set(ptIndex, point);
		mBounds = null;
	}

	/**
	 * Add a point at a particular location, shifting following points to make
	 * room
	 * 
	 * @param ptIndex
	 *            location to insert point
	 * @param point
	 */
	public void addPoint(int ptIndex, Point point) {
		mPoints.add(ptIndex, point);
		mBounds = null;
	}

	/**
	 * Add a point to the object; adds to end of current points
	 */
	public void addPoint(Point pt) {
		if (pt == null)
			throw new IllegalArgumentException();
		addPoint(nPoints(), pt);
	}

	public void removePoint(int index) {
		mPoints.remove(index);
	}

	/**
	 * Get number of points of object
	 * 
	 * @return # points in object
	 */
	public int nPoints() {
		return mPoints.size();
	}

	/**
	 * Get location of a particular point
	 */
	public Point getPoint(int ptIndex) {
		return mPoints.get(ptIndex);
	}

	/**
	 * Get location of a particular point, where index is taken modulo the
	 * number of points (useful for walking around a polygon's vertices, for
	 * instance)
	 * 
	 * @param ptIndex
	 *            index of point; it is converted to modulo(nPoints())
	 * @return location
	 */
	public Point getPointMod(int ptIndex) {
		return getPoint(MyMath.myMod(ptIndex, nPoints()));
	}

	/**
	 * Determine Hausdorff distance of object from a point
	 * 
	 * @param pt
	 * @return distance from point, or -1 if no points exist
	 */
	public abstract float distFrom(Point pt);

	/**
	 * Get factory responsible for making these objects
	 * 
	 * @return factory
	 */
	public abstract EdObjectFactory getFactory();

	/**
	 * Determine distance between a point and one of this object's points
	 * 
	 * @param point
	 * @param ptIndex
	 *            index of point within this object
	 */
	public float distanceFromPoint(Point point, int ptIndex) {
		return MyMath.distanceBetween(point, getPoint(ptIndex));
	}

	/**
	 * Determine closest vertex to a point
	 * 
	 * @param point
	 * @param maxDistance
	 *            maximum distance to accept (distance must be strictly less
	 *            than this)
	 * @return index of closest point, or -1
	 */
	public int closestVertex(Point point, float maxDistance) {
		float bestDistance = maxDistance;
		int closestIndex = -1;
		for (int i = 0; i < nPoints(); i++) {
			float dist = distanceFromPoint(point, i);
			if (dist < bestDistance) {
				bestDistance = dist;
				closestIndex = i;
			}
		}
		return closestIndex;
	}

	/**
	 * Move entire object by a displacement. Default implementation just adjusts
	 * each point.
	 * 
	 * Caution: objects should usually be considered immutable for editing
	 * purposes (except for some flags, such as 'selected'). When an editing
	 * operation is to be performed on an object, it should first be replaced by
	 * a clone, so that any references to the original (i.e. within an undo
	 * list) are valid.
	 * 
	 * @param orig
	 *            a copy of the original object; if null, constructs one
	 * @param delta
	 *            amount to move by
	 */
	public void moveBy(EdObject orig, Point delta) {
		// Some objects may enforce constraints that affect other vertices when
		// one is moved; e.g., a square. Thus for safety we should use a
		// distinct object as the 'starting' point
		if (this == orig)
			throw new IllegalArgumentException();
		if (orig == null)
			orig = (EdObject) this.clone();
		for (int i = 0; i < orig.nPoints(); i++) {
			Point pt = orig.getPoint(i);
			setPoint(i, MyMath.add(pt, delta));
		}
	}

	/**
	 * Replace existing flags with new ones
	 * 
	 * @param f
	 *            new flags
	 */
	public void setFlags(int f) {
		if (((f ^ mFlags) & (FLAG_EDITABLE | FLAG_SELECTED)) != 0)
			mBounds = null;
		this.mFlags = f;
	}

	/**
	 * Add or clear flags
	 * 
	 * @param flags
	 *            flags to modify
	 * @param value
	 *            true to set, false to clear
	 */
	private void setFlags(int flags, boolean value) {
		if (!value)
			clearFlags(flags);
		else
			addFlags(flags);
	}

	/**
	 * Turn specific flags on
	 * 
	 * @param f
	 *            flags to turn on
	 */
	public void addFlags(int f) {
		setFlags(mFlags | f);
	}

	/**
	 * Determine if a set of flags are set
	 * 
	 * @param f
	 *            flags to test
	 * @return true if every one of these flags is set
	 */
	public boolean hasFlags(int f) {
		return (mFlags & f) == f;
	}

	/**
	 * Turn specific flags off
	 * 
	 * @param f
	 *            flags to turn off
	 */
	public void clearFlags(int f) {
		setFlags(mFlags & ~f);
	}

	/**
	 * Get current flags
	 * 
	 * @return flags
	 */
	public int flags() {
		return mFlags;
	}

	/**
	 * Render object within editor. Override this to change highlighting
	 * behaviour for points. Default implementation highlights each vertex if
	 * the object is selected.
	 */
	public void render(AlgorithmStepper s) {
		if (isSelected()) {
			s.setColor(Color.RED);
			for (int i = 0; i < nPoints(); i++) {
				s.plot(getPoint(i));
			}
		}
	}

	/**
	 * Plot a line segment between two points, and emphasize one of the
	 * endpoints if the segment is very short
	 * 
	 * @param v0
	 * @param v1
	 */
	protected void renderLine(AlgorithmStepper s, Point v0, Point v1) {
		s.plotLine(v0, v1);
		if (MyMath.distanceBetween(v0, v1) <= editor().pickRadius() * .2f) {
			s.plot(v0);
		}
	}

	static {
		doNothing();
	}

	public void setEditor(Editor editor) {
		mEditor = editor;
	}

	public Editor editor() {
		return mEditor;
	}

	private Editor mEditor;
	private int mFlags;
	private ArrayList<Point> mPoints = new ArrayList();
	// cached bounds of object, or null
	private Rect mBounds;
}
