package com.js.geometryapp.editor;

import java.util.ArrayList;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgorithmStepper;

public abstract class EdObject implements Cloneable {

	private static final int FLAG_SELECTED = (1 << 31);

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
	 * Determine if object is selected
	 * 
	 * @return true if so
	 */
	public boolean isSelected() {
		return hasFlags(FLAG_SELECTED);
	}

	/**
	 * Set object's selected state
	 * 
	 * @param f
	 *            new state
	 */
	public void setSelected(boolean f) {
		setFlags(FLAG_SELECTED, f);
	}

	/**
	 * Get bounding rectangle of object. Default implementation calculates
	 * minimum bounding rectangle of the object's points
	 * 
	 * @return FRect
	 */
	public Rect getBounds() {
		if (mBounds == null) {
			mBounds = Rect.rectContainingPoints(mPoints);
		}
		return mBounds;
	}

	/**
	 * Determine if object is in a complete state; i.e. if a polygon has at
	 * least three vertices
	 * 
	 * @return true if so
	 */
	public abstract boolean complete();

	/**
	 * Set point
	 * 
	 * @param ptIndex
	 *            index of point
	 * @param point
	 *            new location of point
	 */
	public void setPoint(int ptIndex, Point point) {
		storePoint(ptIndex, new Point(point));
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
		if (ptIndex > mPoints.size())
			throw new IllegalArgumentException();
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
		storePoint(mPoints.size(), point);
	}

	/**
	 * Add a point to the object; adds to end of current points
	 */
	public void addPoint(Point pt) {
		if (pt == null)
			throw new IllegalArgumentException();
		setPoint(nPoints(), pt);
	}

	// /**
	// * Return points of object as an array
	// *
	// * @return Point[] array
	// */
	// public Point[] getPoints() {
	// return (Point[]) mVertices.toArray();
	// }

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
	 * 
	 * @param ptIndex
	 *            index of point
	 * @return location, or null if that point doesn't exist
	 */
	public Point getPoint(int ptIndex) {
		Point ret = null;
		if (ptIndex < mPoints.size())
			ret = mPoints.get(ptIndex);
		return ret;
	}

	/**
	 * Get location of a particular point, where index is taken modulo the
	 * number of points (useful for walking around a polygon's vertices, for
	 * instance)
	 * 
	 * @param ptIndex
	 *            index of point; it is converted to modulo(nPoints())
	 * @return location, or null if that point doesn't exist
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
	 * Determine distance of an object's point from a point
	 * 
	 * @param ptIndex
	 *            index of object's point
	 * @param pt
	 *            point to compare that point to
	 * @return distance, or < 0 if no point ptIndex exists
	 */
	public double distFrom(int ptIndex, Point pt) {
		float ret = -1;
		Point pt2 = getPoint(ptIndex);
		if (pt2 != null)
			ret = MyMath.distanceBetween(pt, pt2);
		return ret;
	}

	/**
	 * Move entire object by a displacement Default implementation just adjusts
	 * each point.
	 * 
	 * Caution: objects should usually be considered immutable for editing
	 * purposes (except for some flags, such as 'selected'). When an editing
	 * operation is to be performed on an object, it should first be replaced by
	 * a clone, so that any references to the original (i.e. within an undo
	 * list) are valid.
	 * 
	 * @param orig
	 *            a copy of the original object
	 * @param delta
	 *            amount to move by
	 */
	public void moveBy(EdObject orig, Point delta) {
		for (int i = 0;; i++) {
			Point pt = orig.getPoint(i);
			if (pt == null)
				break;
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
	 * behaviour for points.
	 */
	public void render(AlgorithmStepper s) {
		if (isSelected()) {
			for (int i = 0; i < nPoints(); i++)
				s.highlight(getPoint(i));
		}
	}

	private int mFlags;
	private ArrayList<Point> mPoints = new ArrayList();
	// cached bounds of object, or null
	private Rect mBounds;
}
