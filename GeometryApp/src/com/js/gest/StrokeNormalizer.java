package com.js.gest;

import java.util.ArrayList;
import java.util.List;

import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.gest.Stroke.DataPoint;

class StrokeNormalizer {

  public static final int DEFAULT_DESIRED_STROKE_LENGTH = 32;

  /**
   * Normalize a stroke set to default length
   */
  public static StrokeSet normalize(StrokeSet originalSet) {
    return normalize(originalSet, DEFAULT_DESIRED_STROKE_LENGTH);
  }

  /**
   * Normalize a stroke set to have arbitrary length; if length is already
   * desired length, returns original set
   */
  public static StrokeSet normalize(StrokeSet originalSet,
      int desiredStrokeLength) {
    if (originalSet.length() == desiredStrokeLength)
      return originalSet;
    StrokeNormalizer n = new StrokeNormalizer(originalSet);
    n.setDesiredStrokeSize(desiredStrokeLength);
    return n.getNormalizedSet();
  }

  /**
   * Construct a normalizer for a particular stroke set
   * 
   * @param strokeSet
   */
  private StrokeNormalizer(StrokeSet strokeSet) {
    mOriginalStrokeSet = strokeSet;
    mDesiredStrokeSize = DEFAULT_DESIRED_STROKE_LENGTH;
  }

  private void setDesiredStrokeSize(int size) {
    if (mNormalizedStrokeSet != null)
      throw new IllegalStateException();
    mDesiredStrokeSize = size;
  }

  /**
   * Perform normalization (if not already done)
   * 
   * @return normalized stroke set
   */
  private StrokeSet getNormalizedSet() {
    if (mNormalizedStrokeSet == null) {
      List<Stroke> normalizedList = new ArrayList();
      for (Stroke s : mOriginalStrokeSet) {
        Stroke normalized = normalizeStroke(s);
        normalizedList.add(normalized);
      }
      mNormalizedStrokeSet = StrokeSet.buildFromStrokes(normalizedList,
          mOriginalStrokeSet);
      mNormalizedStrokeSet.freeze();
    }
    return mNormalizedStrokeSet;
  }

  /**
   * Construct a normalized version of a stroke
   * 
   * @param origStroke
   * @return normalized stroke
   */
  private Stroke normalizeStroke(Stroke originalStroke) {
    // We will be including the original stroke's endpoints
    int interpPointsTotal = mDesiredStrokeSize - 2;
    int interpPointsGenerated = 0;
    float strokeTimeRemaining = originalStroke.totalTime();

    Stroke normalizedStroke = new Stroke();

    int cursor = 0;
    DataPoint strokePoint = originalStroke.get(cursor);

    // Add fragment's start point, if it's the first fragment
    if (normalizedStroke.isEmpty())
      normalizedStroke.addPoint(strokePoint);

    // Determine number of interpolation points to distribute to this fragment
    float fragmentProportionOfTotalTime = originalStroke.totalTime()
        / strokeTimeRemaining;
    int fragInterpPointTotal = Math.round(fragmentProportionOfTotalTime
        * (interpPointsTotal - interpPointsGenerated));
    interpPointsGenerated += fragInterpPointTotal;

    strokeTimeRemaining -= originalStroke.totalTime();

    // Determine time interval between generated points
    // (The fragment endpoints are NOT interpolated)
    float timeStepWithinFragment = originalStroke.totalTime()
        / (1 + fragInterpPointTotal);
    float currentTime = strokePoint.getTime();
    float nextInterpolationTime = currentTime + timeStepWithinFragment;

    int fragInterpPointCount = 0;
    while (fragInterpPointCount < fragInterpPointTotal) {

      // Advance to next interpolation point, or next source element,
      // whichever is first
      DataPoint nextStrokePoint = originalStroke.get(cursor + 1);

      if (nextInterpolationTime < nextStrokePoint.getTime()) {
        // generate a new interpolation point
        currentTime = nextInterpolationTime;
        float timeAlongCurrentEdge = currentTime - strokePoint.getTime();
        float currentEdgeTotalTime = nextStrokePoint.getTime()
            - strokePoint.getTime();
        if (currentEdgeTotalTime <= 0
            || timeAlongCurrentEdge > currentEdgeTotalTime)
          throw new IllegalStateException("illegal values");
        float t = timeAlongCurrentEdge / currentEdgeTotalTime;
        Point position = MyMath.interpolateBetween(strokePoint.getPoint(),
            nextStrokePoint.getPoint(), t);
        normalizedStroke.addPoint(currentTime, position);
        nextInterpolationTime += timeStepWithinFragment;
        fragInterpPointCount++;
      } else {
        // Advance to next original point
        currentTime = nextStrokePoint.getTime();
        strokePoint = nextStrokePoint;
        cursor += 1;
      }
    }
    // Add fragment's end point
    normalizedStroke.addPoint(originalStroke.last());
    return normalizedStroke;
  }

  private StrokeSet mOriginalStrokeSet;
  private StrokeSet mNormalizedStrokeSet;
  private int mDesiredStrokeSize;
}
