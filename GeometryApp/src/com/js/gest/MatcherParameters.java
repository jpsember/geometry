package com.js.gest;

import static com.js.basic.Tools.*;

import com.js.basic.Freezable;

public class MatcherParameters extends Freezable.Mutable {

  public static final MatcherParameters DEFAULT = frozen(new MatcherParameters());

  public MatcherParameters() {
    setZeroDistanceThreshold(StrokeSet.STANDARD_WIDTH * .01f);
    setMaximumCostRatio(1.6f);
    setWindowSize(Math
        .round(StrokeNormalizer.DEFAULT_DESIRED_STROKE_LENGTH * .20f));
  }

  public void setZeroDistanceThreshold(float threshold) {
    mutate();
    mZeroDistanceThreshold = threshold;
  }

  public float zeroDistanceThreshold() {
    return mZeroDistanceThreshold;
  }

  /**
   * Get the maximum cost ratio. The matcher multiples this value by the
   * previous lowest cost gesture recognized, to use as an upper bound on
   * subsequent matching attempts
   */
  public float maximumCostRatio() {
    return mMaximumCostRatio;
  }

  public void setMaximumCostRatio(float ratio) {
    mutate();
    mMaximumCostRatio = ratio;
  }

  public void setWindowSize(int windowSize) {
    mutate();
    mWindowSize = windowSize;
  }

  public int windowSize() {
    return mWindowSize;
  }

  @Override
  public Freezable getMutableCopy() {
    MatcherParameters m = new MatcherParameters();
    m.setZeroDistanceThreshold(zeroDistanceThreshold());
    m.setMaximumCostRatio(maximumCostRatio());
    m.setWindowSize(windowSize());
    return m;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MatcherParameters");
    sb.append("\n zero threshold: " + d(zeroDistanceThreshold()));
    sb.append("\n max cost ratio: " + d(maximumCostRatio()));
    sb.append("\n    window size: " + d(windowSize()));
    return sb.toString();
  }

  private int mWindowSize;
  private float mZeroDistanceThreshold;
  private float mMaximumCostRatio;
}
