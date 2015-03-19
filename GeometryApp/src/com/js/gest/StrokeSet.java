package com.js.gest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.graphics.Matrix;

import com.js.basic.Freezable;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.basic.Rect;
import com.js.gest.Stroke.DataPoint;

import static com.js.basic.Tools.*;

/**
 * A collection of Strokes, which ultimately will be recognized as a touch
 * gesture
 */
public class StrokeSet extends Freezable.Mutable implements Iterable<Stroke> {

  static final String KEY_NAME = "name";
  static final String KEY_ALIAS = "alias";
  static final String KEY_STROKES = "strokes";

  public StrokeSet() {
  }

  public StrokeSet(String name) {
    setName(name);
  }

  /**
   * Normalize this stroke set to default length
   */
  public StrokeSet normalize() {
    return normalize(0);
  }

  /**
   * Normalize a stroke set to have arbitrary length
   * 
   * @param desiredStrokeLength
   *          desired length; if zero, uses default length
   */
  public StrokeSet normalize(int desiredStrokeLength) {
    if (desiredStrokeLength == 0)
      desiredStrokeLength = StrokeNormalizer.DEFAULT_DESIRED_STROKE_LENGTH;
    return StrokeNormalizer.normalize(this, desiredStrokeLength);
  }

  /**
   * Add a point to a stroke within the set. Construct a stroke for this pointer
   * id, if none currently exists
   * 
   * @param eventTime
   *          time, in seconds, associated with point
   * @param pointerId
   *          id of pointer (i.e. finger) generating point
   * @param pt
   *          location of point
   */
  public void addPoint(float eventTime, int pointerId, Point pt) {
    mutate();
    if (isEmpty())
      mInitialEventTime = eventTime;
    Stroke s = strokeForId(pointerId);
    s.addPoint(eventTime - mInitialEventTime, pt);
  }

  public static StrokeSet buildFromStrokes(List<Stroke> strokes) {
    StrokeSet s = new StrokeSet();
    for (Stroke stroke : strokes) {
      s.mStrokes.add(stroke);
    }
    return s;
  }

  /**
   * Stop adding points to stroke corresponding to a pointer id, so that if a
   * subsequent point is generated for this pointer id, it will be stored within
   * a fresh stroke
   */
  public void stopStroke(int pointerId) {
    mutate();
    mStrokeIdToIndexMap.remove(pointerId);
  }

  /**
   * Determine if any strokes are active (i.e., not yet stopped)
   */
  public boolean areStrokesActive() {
    if (isFrozen())
      return false;
    return !mStrokeIdToIndexMap.isEmpty();
  }

  /**
   * Determine if this set represents a single tap
   */
  public boolean isTap() {
    assertFrozen();
    return size() == 1 && length() <= 2;
  }

  @Override
  public void freeze() {
    if (isFrozen())
      return;
    if (areStrokesActive())
      throw new IllegalStateException();
    // Throw out unnecessary resources
    mStrokeIdToIndexMap = null;
    // Calculate bounds, now that frozen
    mBounds = calculateBounds();
    if (mBounds == null)
      throw new IllegalStateException("set " + name() + " has no points");
    for (Stroke s : mStrokes)
      s.freeze();
    super.freeze();
  }

  private Stroke strokeForId(int pointerId) {
    Integer strokeIndex = mStrokeIdToIndexMap.get(pointerId);
    if (strokeIndex == null) {
      strokeIndex = mStrokes.size();
      mStrokes.add(new Stroke());
      mStrokeIdToIndexMap.put(pointerId, strokeIndex);
    }
    return mStrokes.get(strokeIndex);
  }

  private boolean isEmpty() {
    return mStrokes.isEmpty();
  }

  public String toJSON() throws JSONException {
    assertNamed();
    StringBuilder sb = new StringBuilder(",{");
    quote(sb, KEY_NAME);
    sb.append(':');
    quote(sb, name());
    sb.append(',');
    if (hasAlias()) {
      quote(sb, KEY_ALIAS);
      sb.append(':');
      quote(sb, aliasName());
    }
    quote(sb, KEY_STROKES);
    sb.append(":[");
    for (int i = 0; i < size(); i++) {
      if (i != 0)
        sb.append(',');
      sb.append(get(i).toJSONArray());
    }
    sb.append("]}\n");
    return sb.toString();
  }

  private static void quote(StringBuilder sb, String text) {
    sb.append('"');
    sb.append(text);
    sb.append('"');
  }

  public Iterator<Stroke> iterator() {
    return mStrokes.iterator();
  }

  /**
   * Get the number of strokes in this set
   */
  public int size() {
    return mStrokes.size();
  }

  public int length() {
    if (isEmpty())
      throw new IllegalStateException();
    return mStrokes.get(0).size();
  }

  public Stroke get(int index) {
    return mStrokes.get(index);
  }

  @Override
  public Freezable getMutableCopy() {
    StrokeSet s = new StrokeSet();
    s.mName = mName;
    s.mAliasName = mAliasName;
    for (Stroke st : mStrokes) {
      s.mStrokes.add(mutableCopyOf(st));
    }
    return s;
  }

  public Rect getBounds() {
    assertFrozen();
    return mBounds;
  }

  private Rect calculateBounds() {
    Rect r = null;
    for (Stroke s : mStrokes) {
      for (DataPoint spt : s) {
        Point pt = spt.getPoint();
        if (r == null)
          r = new Rect(pt, pt);
        r.include(pt);
      }
    }
    return r;
  }

  public static final int STANDARD_WIDTH = 256;
  private static final float STANDARD_ASPECT_RATIO = 1.0f;

  public static final Rect sStandardRect = new Rect(0, 0, STANDARD_WIDTH - 1,
      (STANDARD_WIDTH - 1) * STANDARD_ASPECT_RATIO);

  /**
   * Construct version of this StrokeSet that has been fit within a rectangle,
   * preserving the aspect ratio
   * 
   * @param destinationRect
   *          rectangle to fit within, or null to use standard rectangle
   * @return StrokeSet fitted to destinationRect
   */
  public StrokeSet fitToRect(Rect destinationRect) {
    Rect origBounds = getBounds();
    if (destinationRect == null)
      destinationRect = sStandardRect;
    Matrix transform = MyMath.calcRectFitRectTransform(origBounds,
        destinationRect);
    return applyTransform(transform);
  }

  /**
   * Construct a transformed version of this stroke set
   * 
   * @param transform
   *          transformation matrix to apply
   * @return transformed stroke set
   */
  public StrokeSet applyTransform(Matrix transform) {
    StrokeSet transformedSet = mutableCopyOf(this);
    for (Stroke stroke : transformedSet) {
      for (DataPoint strokePoint : stroke) {
        strokePoint.getPoint().apply(transform);
      }
    }
    transformedSet.freeze();
    return transformedSet;
  }

  public String name() {
    return mName;
  }

  public void setName(String name) {
    mutate();
    mName = name;
  }

  public void setAliasName(String aliasName) {
    mutate();
    mAliasName = aliasName;
  }

  /**
   * Get the name of the stroke set this one is an alias of; returns our name if
   * we are not an alias
   */
  String aliasName() {
    if (mAliasName == null)
      return mName;
    return mAliasName;
  }

  boolean hasAlias() {
    return mAliasName != null;
  }

  public void assertNamed() {
    if (mName == null)
      throw new IllegalStateException();
  }

  public void inheritMetaDataFrom(StrokeSet source) {
    mutate();
    mName = source.mName;
    mAliasName = source.mAliasName;
  }

  private String mAliasName;
  private String mName;
  private ArrayList<Stroke> mStrokes = new ArrayList();
  private Rect mBounds;

  // Only used when mutable
  private float mInitialEventTime;
  private Map<Integer, Integer> mStrokeIdToIndexMap = new HashMap();

}
