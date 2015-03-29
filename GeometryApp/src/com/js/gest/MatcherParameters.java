package com.js.gest;

import static com.js.basic.Tools.*;

import com.js.basic.Freezable;

public class MatcherParameters extends Freezable.Mutable {

  public static final MatcherParameters DEFAULT = frozen(new MatcherParameters());

  public MatcherParameters() {
    setMaximumCostRatio(1.3f);
    setWindowSize(Math
        .round(StrokeNormalizer.DEFAULT_DESIRED_STROKE_LENGTH * .20f));
    setMaxResults(3);
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

  public void setMaxResults(int maxResults) {
    mutate();
    mMaxResults = maxResults;
  }

  public int maxResults() {
    return mMaxResults;
  }

  @Override
  public Freezable getMutableCopy() {
    MatcherParameters m = new MatcherParameters();
    m.mFlags = mFlags;
    m.setMaximumCostRatio(maximumCostRatio());
    m.setWindowSize(windowSize());
    m.setMaxResults(maxResults());
    return m;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MatcherParameters");
    sb.append("\n max cost ratio: " + d(maximumCostRatio()));
    sb.append("\n    window size: " + d(windowSize()));
    return sb.toString();
  }

  /* private */void setFlag(int flag, boolean state) {
    mutate();
    if (!state)
      mFlags &= ~flag;
    else
      mFlags |= flag;
  }

  /* private */boolean hasFlag(int flag) {
    return 0 != (mFlags & flag);
  }

  private int mWindowSize;
  private float mMaximumCostRatio;
  private int mFlags;
  private int mMaxResults;
}
