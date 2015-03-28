package com.js.gest;

import static com.js.basic.Tools.*;

import java.util.Arrays;

import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.gest.Stroke.DataPoint;

/**
 * Determines how closely two strokes match
 * 
 * Public for test purposes
 * 
 * Consider making this package visibility only, and testing at higher level
 */
public class StrokeMatcher {

  public StrokeMatcher(AlgorithmStats stats) {
    mStats = stats;
  }

  /**
   * A value representing 'infinite' cost. It should not be so large that it
   * can't be safely doubled or tripled without overflowing
   */
  public static final float INFINITE_COST = Float.MAX_VALUE / 100;

  /**
   * Prepare matcher for new pair of strokes. Also resets cost cutoff
   * 
   * @param a
   * @param b
   * @param parameters
   */
  public void setArguments(Stroke a, Stroke b, MatcherParameters parameters) {
    mStrokeA = frozen(a);
    mStrokeB = frozen(b);
    if (mStrokeA.size() != mStrokeB.size())
      throw new IllegalArgumentException("stroke lengths mismatch");
    if (parameters == null)
      parameters = MatcherParameters.DEFAULT;
    mParameters = frozen(parameters);
    // We must have a positive window size, otherwise the algorithm will abort
    // since some slices will produce infinite costs
    if (mParameters.windowSize() <= 0)
      throw new IllegalArgumentException("bad window size");
    prepareTable();
    mCostCalculated = false;
  }

  /**
   * Set upper bound on the cost. The algorithm will exit early if it determines
   * the cost will exceed this bound
   */
  public void setMaximumCost(float maximumCost) {
    mMaximumCost = maximumCost;
  }

  public float getMaximumCost() {
    return mMaximumCost;
  }

  /**
   * Determine the cost, or distance, between the two strokes
   */
  public float cost() {
    if (!mCostCalculated) {
      if (mStrokeA == null)
        throw new IllegalStateException();
      performAlgorithm();
    }
    return mCost;
  }

  private void prepareTable() {
    if (mTableSize != mStrokeA.size()) {
      mWindowSize = -1;
      mTableSize = mStrokeA.size();
      int tableCells = mTableSize * mTableSize;
      mTable = new float[tableCells];
      mCostNormalizationFactor = 1.0f / (2 * mTableSize);
    }
    if (mWindowSize != mParameters.windowSize()) {
      mWindowSize = mParameters.windowSize();
      // Fill all cells with infinite cost, since with a window, we may be
      // referencing cells we haven't otherwise visited
      Arrays.fill(mTable, INFINITE_COST);
      int n = mTableSize - (2 * mWindowSize + 1);
      mMaxCellsExamined = (mTableSize * mTableSize) - (n * (n + 1));
    }
  }

  private int cellIndex(int a, int b) {
    return a + b * mTableSize;
  }

  private void storeCost(int a, int b, float cost) {
    mTable[cellIndex(a, b)] = cost;
  }

  /**
   * Perform matching algorithm
   * 
   * We perform dynamic programming. Conceptually we construct a square table of
   * cells, where the x axis represents the cursor within stroke A, and the y
   * axis the cursor within stroke B. Thus x and y range from 0...n-1, where n
   * is the length of the stroke.
   * 
   * The initial state is with x=y=0 (the bottom left corner), and the final
   * state is x=y=n-1 (the top right corner).
   * 
   * Each cell (x,y) in the table stores the lowest cost leading to that cell,
   * where possible moves are from (x-1,y), (x-1,y-1), or (x,y-1).
   */
  private void performAlgorithm() {
    mStats.incrementExecutionCount();
    mStats.adjustTotalCellCount(mMaxCellsExamined);

    // Multiply bottom left cost by 2, for symmetric weighting, since it
    // conceptually represents advancement to the first point in both A and B
    float startCost = comparePoints(0, 0) * 2;
    storeCost(0, 0, startCost);

    // In case we exit early due to maximum cost exceeded,
    // set an infinite cost as the output
    mCostCalculated = true;
    mCost = INFINITE_COST;

    int windowSize = mWindowSize;
    int tableSize = mTableSize;

    // Do the bottom left triangle
    for (int x = 1; x < tableSize; x++) {
      float minCost = INFINITE_COST;
      int jMin, jMax;
      int overflow = 2 * windowSize - x;
      if (overflow >= 0) {
        jMin = 0;
        jMax = x + 1;
      } else {
        jMin = -(overflow - 1) / 2;
        jMax = x + 1 + (overflow - 1) / 2;
      }
      for (int j = jMin; j < jMax; j++) {
        minCost = Math.min(minCost, processCell(x - j, j));
      }
      if (minCost >= mMaximumCost) {
        return;
      }
    }

    // Do the top right triangle. For simplicity, use the same code as the
    // previous example, and do a flip of the cell coordinates only at
    // processCell() time (but reverse the order of the outer loop so we sweep
    // in the correct direction)

    for (int x = tableSize - 2; x >= 0; x--) {
      float minCost = INFINITE_COST;
      int jMin, jMax;
      int overflow = 2 * windowSize - x;
      if (overflow >= 0) {
        jMin = 0;
        jMax = x + 1;
      } else {
        jMin = -(overflow - 1) / 2;
        jMax = x + 1 + (overflow - 1) / 2;
      }
      for (int j = jMin; j < jMax; j++) {
        minCost = Math.min(minCost,
            processCell(tableSize - 1 - (x - j), tableSize - 1 - j));
      }
      if (minCost >= mMaximumCost) {
        return;
      }
    }
    mCost = mTable[mTable.length - 1];
  }

  private float processCell(int a, int b) {
    int abIndex = cellIndex(a, b);
    float bestCost;
    float abCost = comparePoints(a, b);
    if (a > 0) {
      bestCost = mTable[abIndex - 1] + abCost;
      if (b > 0) {
        float prevCost = mTable[abIndex - mTableSize] + abCost;
        if (bestCost > prevCost)
          bestCost = prevCost;
        // Multiply cost by 2, since we're moving diagonally (this is symmetric
        // weighting, as described in the literature)
        prevCost = mTable[abIndex - mTableSize - 1] + abCost * 2;
        if (bestCost > prevCost)
          bestCost = prevCost;
      }
    } else {
      bestCost = mTable[abIndex - mTableSize] + abCost;
    }
    mTable[abIndex] = bestCost;
    return bestCost;
  }

  private float comparePoints(int aIndex, int bIndex) {
    DataPoint elemA = mStrokeA.get(aIndex);
    DataPoint elemB = mStrokeB.get(bIndex);
    Point posA = elemA.getPoint();
    Point posB = elemB.getPoint();
    mStats.incrementCellsExamined();
    float dist = MyMath.squaredDistanceBetween(posA, posB);
    dist *= mCostNormalizationFactor;
    return dist;
  }

  private int mTableSize;
  private float[] mTable;
  private Stroke mStrokeA;
  private Stroke mStrokeB;
  private boolean mCostCalculated;
  private float mCost;
  private MatcherParameters mParameters;
  // Scaling factor to apply to a distance before storing in cell, so that sum
  // of entire path is normalized
  private float mCostNormalizationFactor;
  private float mMaximumCost;
  private int mWindowSize;
  private int mMaxCellsExamined;
  private AlgorithmStats mStats;
}
