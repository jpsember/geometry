package com.js.gest;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;

import com.js.basic.Point;
import com.js.basic.Freezable;

import static com.js.basic.Tools.*;

/**
 * A Stroke is a sequence of StrokePoints, with strictly increasing times,
 * starting from zero
 * 
 * It represents a user's touch/drag/release action, for a single finger.
 */
public class Stroke extends Freezable.Mutable implements
    Iterable<Stroke.DataPoint> {

  public Stroke() {
    mPoints = new ArrayList();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(nameOf(this));
    sb.append("Stroke\n");
    int index = 0;
    for (DataPoint pt : mPoints) {
      sb.append(d(index));
      sb.append(" ");
      sb.append(pt);
      sb.append('\n');
      index++;
    }

    return sb.toString();
  }

  /**
   * Get number of points in stroke
   */
  public int size() {
    return mPoints.size();
  }

  public DataPoint get(int index) {
    return mPoints.get(index);
  }

  public Point getPoint(int i) {
    return get(i).getPoint();
  }

  public boolean isEmpty() {
    return mPoints.isEmpty();
  }

  void addPoint(DataPoint point) {
    addPoint(point.getTime(), point.getPoint());
  }

  /**
   * Add a new point to this (mutable) stroke
   * 
   * @param time
   *          time, in seconds, of point; the time of the first point will be
   *          subtracted from this one, so that the resulting sequence starts at
   *          time zero
   * @param location
   */
  public void addPoint(float time, Point location) {
    mutate();
    float shiftedTime = 0;
    if (isEmpty()) {
      mStartTime = time;
    } else {
      DataPoint previousDataPoint = last(mPoints);
      shiftedTime = Math.max(previousDataPoint.getTime() + 0.001f, time
          - mStartTime);
    }
    mPoints.add(new DataPoint(shiftedTime, location));
  }

  // Divide stroke point time values by this to get time in seconds
  private static final float FLOAT_TIME_SCALE = 60.0f;

  public JSONArray toJSONArray() throws JSONException {
    assertFrozen();
    JSONArray a = new JSONArray();
    for (DataPoint pt : mPoints) {
      a.put((int) (pt.getTime() * FLOAT_TIME_SCALE));
      a.put((int) pt.getPoint().x);
      a.put((int) pt.getPoint().y);
    }
    return a;
  }

  public static Stroke parseJSONArray(JSONArray array) throws JSONException {
    Stroke s = new Stroke();

    if (array.length() % 3 != 0)
      throw new JSONException("malformed stroke array");
    int nPoints = array.length() / 3;

    for (int i = 0; i < nPoints; i++) {
      int j = i * 3;
      float time = array.getInt(j + 0) / FLOAT_TIME_SCALE;
      float x = array.getInt(j + 1);
      float y = array.getInt(j + 2);
      s.addPoint(time, new Point(x, y));
    }
    s.freeze();
    return s;
  }
 
  public Iterator<DataPoint> iterator() {
    return mPoints.iterator();
  }

  @Override
  public Freezable getMutableCopy() {
    Stroke s = new Stroke();
    s.mStartTime = mStartTime;
    for (DataPoint pt : mPoints) {
      s.mPoints.add(new DataPoint(pt));
    }
    return s;
  }

  /**
   * Get duration of this stroke; the time elapsed from the first to the last
   * point
   */
  public float totalTime() {
    float time = 0;
    if (!mPoints.isEmpty())
      time = last(mPoints).getTime();
    return time;
  }

  public static class DataPoint {

    public DataPoint(float time, Point point) {
      mTime = time;
      mPoint = new Point(point);
    }

    public DataPoint(DataPoint source) {
      this(source.mTime, source.mPoint);
    }

    public Point getPoint() {
      return mPoint;
    }

    public float getTime() {
      return mTime;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(d(getTime()));
      sb.append(getPoint());
      return sb.toString();
    }

    private Point mPoint;
    private float mTime;
  }

  public boolean isFeaturePoint(int pointIndex) {
    assertFrozen();
    // Reading/writing a reference is always an atomic action, hence
    // synchronization can be done after (1); see
    // http://docs.oracle.com/javase/tutorial/essential/concurrency/atomic.html)
    //
    // We DO mark mFeaturePoints as volatile, since it may be changed by another
    // thread between (1) and (2) below
    //
    if (mFeaturePoints == null) { // (1)
      synchronized (this) {
        if (mFeaturePoints == null) // (2)
          mFeaturePoints = FeaturePointList.constructFor(this);
      }
    }
    return mFeaturePoints.contains(pointIndex);
  }

  private ArrayList<DataPoint> mPoints;
  private float mStartTime;
  // Lazy-initialized, thread-safe reference to list of feature points
  private volatile FeaturePointList mFeaturePoints;

}
