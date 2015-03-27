package com.js.gest;

import static com.js.basic.Tools.*;

import java.util.BitSet;

import com.js.basic.MyMath;
import com.js.basic.Point;

class FeaturePointList {

  private static final float FEATURE_LENGTH_MIN = StrokeSet.STANDARD_WIDTH * .05f;
  private static final float FEATURE_ANGLE_MIN = MyMath.M_DEG * 70.0f;

  /**
   * Construct a FeaturePointList for a stroke
   */
  public static FeaturePointList constructFor(Stroke s) {
    return new FeaturePointList(s);
  }

  private FeaturePointList(Stroke stroke) {
    stroke.assertFrozen();
    doNothing();
    determineFeaturePoints(stroke);
  }

  /**
   * Determine if a point is a feature point
   * 
   * @param pointIndex
   *          index of point
   */
  public boolean contains(int pointIndex) {
    return mBitmap.get(pointIndex);
  }

  private void determineFeaturePoints(Stroke s) {

    mBitmap = new BitSet(s.size());

    float previousAngle = 0;
    boolean previousAngleDefined = false;

    int clumpedPointsCount = 0;
    for (int i = 1; i < s.size(); i++) {
      Point aPt = s.getPoint(i - 1);
      Point bPt = s.getPoint(i);

      float distance = MyMath.distanceBetween(aPt, bPt);
      if (distance < FEATURE_LENGTH_MIN) {
        clumpedPointsCount++;
        continue;
      }
      float angle = MyMath.polarAngleOfSegment(aPt, bPt);
      if (previousAngleDefined) {
        float angleDiff = Math
            .abs(MyMath.normalizeAngle(angle - previousAngle));
        if (angleDiff >= FEATURE_ANGLE_MIN)
          mBitmap.set(i - 1 - clumpedPointsCount/2);
      }
      previousAngle = angle;
      previousAngleDefined = true;
      clumpedPointsCount = 0;
    }
  }

  private BitSet mBitmap;

}
