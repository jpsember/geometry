package com.js.gest;

import static com.js.basic.Tools.*;

import com.js.basic.Freezable;

public class MatcherParameters extends Freezable.Mutable {

  public static final MatcherParameters DEFAULT = frozen(new MatcherParameters());

  public MatcherParameters() {
    setZeroDistanceThreshold(StrokeSet.STANDARD_WIDTH * .01f);
  }

  public void setZeroDistanceThreshold(float threshold) {
    mutate();
    mZeroDistanceThreshold = threshold;
  }

  public float zeroDistanceThreshold() {
    return mZeroDistanceThreshold;
  }

  @Override
  public Freezable getMutableCopy() {
    MatcherParameters m = new MatcherParameters();
    m.setZeroDistanceThreshold(zeroDistanceThreshold());
    return m;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MatcherParameters");
    sb.append("\n zero threshold: " + d(zeroDistanceThreshold()));
    return sb.toString();
  }

  private float mZeroDistanceThreshold;

}
