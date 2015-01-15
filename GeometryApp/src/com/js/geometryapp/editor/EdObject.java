package com.js.geometryapp.editor;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Matrix;

import com.js.editor.UserEvent;
import com.js.editor.UserOperation;
import com.js.geometry.AlgorithmStepper;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import static com.js.basic.Tools.*;
import com.js.basic.Freezable;

public abstract class EdObject extends Freezable.Mutable {

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
   * @param initialPress
   *          DOWN event; e.g., to see if it is at a draggable vertex
   * @return operation, or null
   */
  public abstract UserOperation buildEditOperation(int slot,
      UserEvent initialPress);

  /**
   * Clone the object
   */
  protected EdObject(EdObject source) {
    if (source == null)
      return;

    for (Point pt : source.mPoints)
      mPoints.add(new Point(pt));
    mEditor = source.mEditor;
  }

  /**
   * Object has just been made editable
   * 
   * @param location
   *          location where user pressed to cause this
   */
  public void selectedForEditing(Point location) {
  }

  /**
   * Get bounding rectangle of object. Default implementation calculates minimum
   * bounding rectangle of the object's points
   */
  public Rect getBounds() {
    if (mBounds == null) {
      mBounds = Rect.rectContainingPoints(mPoints);
    }
    return mBounds;
  }

  @Override
  public void freeze() {
    // Make sure we've performed all possible lazy initialization
    getBounds();
    super.freeze();
  }

  /**
   * Set point, by replacing existing (if index < size()) or by adding new (if
   * index == size())
   */
  public void setPoint(int index, Point point) {
    storePoint(index, new Point(point));
  }

  public void clearPoints() {
    mutate();
    mPoints.clear();
  }

  /**
   * Store a point, without copying it
   * 
   * @param ptIndex
   *          index of point; must be index of an existing point, or the current
   *          number of points
   * @param point
   *          location of point
   */
  private void storePoint(int ptIndex, Point point) {
    mutate();
    if (mPoints.size() == ptIndex)
      mPoints.add(point);
    else
      mPoints.set(ptIndex, point);
  }

  /**
   * Add a point at a particular location, shifting following points to make
   * room
   * 
   * @param ptIndex
   *          location to insert point
   * @param point
   */
  public void addPoint(int ptIndex, Point point) {
    mutate();
    mPoints.add(ptIndex, point);
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
   * Get location of a particular point, where index is taken modulo the number
   * of points (useful for walking around a polygon's vertices, for instance)
   * 
   * @param ptIndex
   *          index of point; it is converted to modulo(nPoints())
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
   *          index of point within this object
   */
  public float distanceFromPoint(Point point, int ptIndex) {
    return MyMath.distanceBetween(point, getPoint(ptIndex));
  }

  /**
   * Determine closest vertex to a point
   * 
   * @param point
   * @param maxDistance
   *          maximum distance to accept (distance must be strictly less than
   *          this)
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
   * @param orig
   *          a copy of the original object, representing its original position
   * @param delta
   *          amount to move by
   */
  public void moveBy(EdObject orig, Point delta) {
    if (orig == null || orig == this)
      throw new IllegalArgumentException();
    mutate();
    for (int i = 0; i < orig.nPoints(); i++) {
      Point pt = orig.getPoint(i);
      setPoint(i, MyMath.add(pt, delta));
    }
  }

  /**
   * Render object within editor. Override this to change highlighting behaviour
   * for points. Default implementation highlights each vertex if the object is
   * selected.
   */
  public void render(AlgorithmStepper s, boolean selected, boolean editable) {
    if (selected) {
      s.setColor(Color.RED);
      for (int i = 0; i < nPoints(); i++) {
        s.render(getPoint(i));
      }
    }
  }

  /**
   * Plot a line segment between two points, and emphasize one of the endpoints
   * if the segment is very short
   * 
   * @param v0
   * @param v1
   */
  protected void renderLine(AlgorithmStepper s, Point v0, Point v1) {
    s.renderLine(v0, v1);
    if (MyMath.distanceBetween(v0, v1) <= editor().pickRadius() * .2f) {
      s.render(v0);
    }
  }

  public void setEditor(Editor editor) {
    mutate();
    mEditor = editor;
  }

  public Editor editor() {
    return mEditor;
  }

  /**
   * Apply a transformation to this object. Default implementation transforms
   * each vertex
   */
  public void applyTransform(Matrix m) {
    for (int i = 0; i < nPoints(); i++) {
      Point v = new Point(getPoint(i));
      v.apply(m);
      setPoint(i, v);
    }
  }

  /**
   * Determine if object intersects a rectangle. Default implementation checks
   * if any of the object's vertices lie within the rectangle
   */
  public boolean intersects(Rect r) {
    for (int j = 0; j < nPoints(); j++) {
      Point v = getPoint(j);
      if (r.contains(v))
        return true;
    }
    return false;
  }

  @Override
  public void mutate() {
    super.mutate();
    mBounds = null;
  }

  private Editor mEditor;
  private List<Point> mPoints = new ArrayList();
  // cached bounds of object, or null
  private Rect mBounds;
}
