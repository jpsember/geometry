package com.js.geometryapp.editor;

import java.util.ArrayList;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgorithmStepper;

import static com.js.basic.Tools.*;

public abstract class EdObject implements Cloneable {

	/**
	 * Replace object's points with those of another object
	 * 
	 * @param src
	 *            : source object
	 */
	public void copyPointsFrom(EdObject src) {
		ASSERT(src != this);
		// Both source and destination objects may have the same point array, so
		// construct a new one
		ArrayList<Point> newPts = new ArrayList();
		for (int i = 0; i < src.nPoints(); i++)
			newPts.add(new Point(src.getPoint(i)));
		pts = newPts;
	}

	// /**
	// * Construct a string that uniquely describes this object
	// *
	// * @return
	// */
	// public String getHash() {
	// StringBuilder sb = new StringBuilder();
	// sb.append(getFactory().getTag());
	// sb.append(' ');
	// for (int j = 0; j < nPoints(); j++)
	// sb.append(getPoint(j));
	// sb.append('\n');
	// return sb.toString();
	// }

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
	 * Toggle state of certain flags
	 * 
	 * @param flg
	 *            flags to toggle
	 */
	public void toggleFlags(int flg) {
		this.flags ^= flg;
	}

	// /**
	// * Plot label for object, if one is defined
	// *
	// * @param loc
	// * location of label
	// */
	// public void plotLabel(Point loc) {
	// plotLabel(loc.x, loc.y);
	// }
	//
	// /**
	// * Plot label for object, if one is defined
	// *
	// * @param x
	// * @param y
	// * location of label
	// */
	// public void plotLabel(double x, double y) {
	// String s = getLabel();
	// if (s != null) {
	// Editor.plotLabel(s, x, y, false);
	// }
	// }
	//
	// /**
	// * Plot label for object, if one is defined and the appropriate labels
	// * option is selected
	// *
	// * @param vert
	// * true if vertex label vs object label
	// * @param loc
	// * location of label
	// */
	// public void plotLabel(boolean vert, Point loc) {
	// plotLabel(vert, loc.x, loc.y);
	// }
	//
	// /**
	// * Plot label for object, if one is defined and the 'show labels' option
	// is
	// * selected
	// *
	// * @param vert
	// * true if vertex label vs object label
	// * @param x
	// * @param y
	// * location of label
	// */
	// public void plotLabel(boolean vert, double x, double y) {
	// if (Editor.withLabels(vert))
	// plotLabel(x, y);
	// }

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
		return Rect.rectContainingPoints(pts);
	}

	/**
	 * Determine if object is in a complete state; i.e. if a polygon has at
	 * least three vertices
	 * 
	 * @return true if so
	 */
	public abstract boolean complete();

	/**
	 * Delete a point, if it exists
	 * 
	 * @param ptIndex
	 *            index of point to delete
	 */
	public void deletePoint(int ptIndex) {
		if (ptIndex >= 0 && ptIndex < pts.size())
			pts.remove(ptIndex);
	}

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

	// /**
	// * Set transformed location of point. Default method calls setPoint(). For
	// * discs, radius point should be calculated from others.
	// *
	// * @param ptIndex
	// * @param point
	// */
	// public void setTransformedPoint(int ptIndex, Point point) {
	// setPoint(ptIndex, point);
	// }

	// /**
	// * Set point, with optional snapping to grid
	// *
	// * @param ptIndex
	// * index of point
	// * @param point
	// * new location of point
	// * @param useGrid
	// * if true, snaps to grid (if one is active)
	// * @param action
	// * if not null, action that caused this edit
	// */
	// public void setPoint(int ptIndex, Point point, boolean useGrid,
	// TBAction action) {
	// if (!useGrid)
	// point = new Point(point);
	// else
	// point = V.snapToGrid(point);
	// storePoint(ptIndex, point);
	// }

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
		ASSERT(ptIndex <= pts.size());
		if (pts.size() == ptIndex)
			pts.add(point);
		else
			pts.set(ptIndex, point);
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
		pts.add(ptIndex, point);
	}

	/**
	 * Add a point to the object; adds to end of current points
	 */
	public void addPoint(Point pt) {
		if (pt == null)
			throw new IllegalArgumentException();
		setPoint(nPoints(), pt);
	}

	/**
	 * Return points of object as an array
	 * 
	 * @return Point[] array
	 */
	public Point[] getPoints() {
		return (Point[]) pts.toArray();
	}

	/**
	 * Get number of points of object
	 * 
	 * @return # points in object
	 */
	public int nPoints() {
		return pts.size();
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
		if (ptIndex < pts.size())
			ret = pts.get(ptIndex);
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

	// /**
	// * Add a large highlight to a point, if it exists
	// *
	// * @param ptIndex
	// * point index
	// */
	// public void hlLarge(int ptIndex) {
	// Point pt = getPoint(ptIndex);
	// if (pt != null) {
	// V.pushColor(Color.RED);
	// V.drawRect(getDisplayBoundingRect(pt));
	// V.popColor();
	// }
	// }

	// /**
	// * Add a small highlight to a point, if it exists
	// *
	// * @param ptIndex
	// * point index
	// */
	// public void hlSmall(int ptIndex) {
	// Point pt = getPoint(ptIndex);
	// if (pt != null) {
	// // vp vp = TestBed.view();
	// V.pushColor(Color.RED);
	// V.drawRect(getDisplayBoundingRect(pt, V.getScale() * .4));
	// V.popColor();
	// }
	// }

	// /**
	// * Construct a rectangle to display around a point, using current view
	// scale
	// *
	// * @param pt
	// * @return
	// */
	// private static Rect getDisplayBoundingRect(Point pt) {
	// double size = V.getScale();
	// return getDisplayBoundingRect(pt, size);
	// }

	/**
	 * Construct a rectangle to display around a point, using arbitrary padding
	 * size
	 * 
	 * @param pt
	 * @param padding
	 *            : amount of padding to each side
	 * @return
	 * @deprecated not sure if this is used
	 */
	public static Rect getDisplayBoundingRect(Point pt, float padding) {
		return new Rect(pt.x - padding, pt.y - padding, padding * 2,
				padding * 2);
	}

	/**
	 * Move entire object by a displacement Default implementation just adjusts
	 * each point.
	 * 
	 * @param orig
	 *            : a copy of the original object
	 * @param delta
	 *            : amount to move by
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
	 * Get next point to insert. Either create a new point and return its index,
	 * or return -1 to indicate this object is complete
	 * 
	 * @param a
	 *            current TBAction (e.g., to examine modifier keys)
	 * @param ptIndex
	 *            : index of point being inserted
	 * @param drift
	 *            if not null, offset of current mouse loc from last event loc
	 * @return index of point to continue editing with, -1 if done, -2 to
	 *         continue waiting
	 */
	public int getNextPointToInsert(/* TBAction a, */int ptIndex, Point drift) {
		int ret = -1;
		if (!complete())
			ret = nPoints();
		return ret;
	}

	/**
	 * Clean up an object after editing is complete. If it is damaged, leave in
	 * an incomplete state. This is used to filter out duplicate vertices in
	 * polygons, for instance.
	 */
	public void cleanUp() {
	}

	/**
	 * Determine if object is active. By default, objects are active. User can
	 * flag objects as inactive, so they are excluded from some operations,
	 * and/or appear different.
	 * 
	 * @return true if it's active
	 */
	public boolean isActive() {
		return !hasFlags(FLAG_INACTIVE);
	}

	/**
	 * Set object's active flag
	 * 
	 * @param f
	 *            true for active, false for inactive
	 */
	public void setActive(boolean f) {
		setFlags(FLAG_INACTIVE, !f);
	}

	/**
	 * Replace existing flags with new ones
	 * 
	 * @param f
	 *            new flags
	 */
	public void setFlags(int f) {
		this.flags = f;
	}

	/**
	 * Add or clear flags
	 * 
	 * @param flags
	 *            flags to modify
	 * @param value
	 *            true to set, false to clear
	 */
	public void setFlags(int flags, boolean value) {
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
		setFlags(flags | f);
	}

	/**
	 * Determine if a set of flags are set
	 * 
	 * @param f
	 *            flags to test
	 * @return true if every one of these flags is set
	 */
	public boolean hasFlags(int f) {
		return (flags & f) == f;
	}

	/**
	 * Turn specific flags off
	 * 
	 * @param f
	 *            flags to turn off
	 */
	public void clearFlags(int f) {
		setFlags(flags & ~f);
	}

	/**
	 * Get current flags
	 * 
	 * @return flags
	 */
	public int flags() {
		return flags;
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

	private static final int FLAG_SELECTED = (1 << 31);
	private static final int FLAG_INACTIVE = (1 << 30);
	public static final int FLAG_PLOTDASHED = (1 << 29);
	/**
	 * Number of bits available for user flags. For instance, any flag from 2^0
	 * to 2^(USER_FLAG_BITS-1) are available for user use. The others are used
	 * for the object's selected and active states. Some of these may be used by
	 * other objects; for instance, the EdPolygon uses one of these already.
	 */
	public static final int USER_FLAG_BITS = 24;

	// /**
	// * Scale a point relative to the center of the view
	// *
	// * @param pt
	// * point to scale
	// * @param factor
	// * scaling factor
	// */
	// public static void scalePoint(Point pt, double factor) {
	// Point ls = V.logicalSize();
	// double mx = ls.x / 2, my = ls.y / 2;
	//
	// pt.setLocation((pt.x - mx) * factor + mx, (pt.y - my) * factor + my);
	// }

	// /**
	// * Scale object. Default implementation just scales all the object's
	// points.
	// *
	// * @param factor
	// * scaling factor
	// */
	// public void scale(double factor) {
	// for (int i = 0;; i++) {
	// Point pt = getPoint(i);
	// if (pt == null)
	// break;
	// scalePoint(pt, factor);
	// setPoint(i, pt);
	// }
	// }

	// /**
	// * Translate object.
	// * @param x
	// * @param y translation amount
	// */
	// public void translate(double x, double y) {
	// for (int i = 0;; i++) {
	// Point pt = getPoint(i);
	// if (pt == null)
	// break;
	// setPoint(i, new Point(pt.x + x, pt.y + y));
	// }
	// }

	/**
	 * Return the DArray used to store the points
	 * 
	 * @return DArray containing Point's
	 */
	public ArrayList<Point> getPts() {
		return pts;
	}

	private int flags;
	private ArrayList<Point> pts = new ArrayList();
}
