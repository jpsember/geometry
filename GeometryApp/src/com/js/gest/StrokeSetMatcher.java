package com.js.gest;

import static com.js.basic.Tools.*;

import java.util.ArrayList;

import com.js.basic.MyMath;
import com.js.basic.Point;

class StrokeSetMatcher {

  public StrokeSetMatcher(StrokeSet a, StrokeSet b, MatcherParameters param) {
    mStrokeA = frozen(a);
    mStrokeB = frozen(b);
    if (param == null)
      param = MatcherParameters.DEFAULT;
    mParam = param;
    if (mStrokeA.size() != mStrokeB.size())
      throw new IllegalArgumentException(
          "Different number of strokes in each set");
  }

  public float similarity() {
    if (mSimilarity == null) {
      int[] bOrder = calcBestOrderForB();
      float totalCost = 0;
      int numberOfStrokes = mStrokeA.size();
      for (int i = 0; i < numberOfStrokes; i++) {
        Stroke sa = mStrokeA.get(i);
        Stroke sb = mStrokeB.get(bOrder[i]);
        StrokeMatcher m = new StrokeMatcher(sa, sb, mParam);
        float cost = m.similarity();
        totalCost += cost;
      }
      mSimilarity = totalCost / numberOfStrokes;
    }
    return mSimilarity;
  }

  private int[] calcBestOrderForB() {
    // Pick snapshots that are in the middle of the stroke,
    // since we'll probably have good separation at that time
    int cursor = mStrokeA.length() / 2;
    Point[] aPts = buildSnapshot(mStrokeA, cursor);
    Point[] bPts = buildSnapshot(mStrokeB, cursor);

    // Determine the best permutation of B to match points of A within these two
    // snapshots
    ArrayList<int[]> perms = buildPermutations(mStrokeA.size());
    int[] bestOrder = null;
    float minCost = 0;
    for (int[] order : perms) {
      float cost = orderCost(order, aPts, bPts);
      if (bestOrder == null || minCost > cost) {
        minCost = cost;
        bestOrder = order;
      }
    }
    return bestOrder;
  }

  private float orderCost(int[] ordering, Point[] aPts, Point[] bPts) {
    float totalCost = 0;
    for (int i = 0; i < ordering.length; i++) {
      Point pa = aPts[i];
      Point pb = bPts[ordering[i]];
      totalCost += MyMath.squaredDistanceBetween(pa, pb);
    }
    return totalCost;
  }

  /**
   * Construct a snapshot of all stroke points in a set at a particular point in
   * time
   * 
   * @param set
   * @param cursor
   *          the common index into the strokes
   * @return array of Points extracted
   */
  private Point[] buildSnapshot(StrokeSet set, int cursor) {
    Point[] points = new Point[set.length()];
    for (int i = 0; i < set.size(); i++) {
      points[i] = set.get(i).get(cursor).getPoint();
    }
    return points;
  }

  /**
   * Generate all permutations of the first n integers; uses Heap's algorithm
   * (http://en.wikipedia.org/wiki/Heap%27s_algorithm)
   */
  private ArrayList<int[]> buildPermutations(int n) {
    mPermutations = new ArrayList();
    mPermuteArray = new int[n];
    for (int i = 0; i < n; i++)
      mPermuteArray[i] = i;
    generate(n);
    return mPermutations;
  }

  private void generate(int n) {
    n--;
    if (n == 0) {
      mPermutations.add(mPermuteArray.clone());
    } else {
      for (int i = 0; i <= n; i++) {
        generate(n);
        int j = (n % 2 == 0) ? 0 : i;
        int tmp = mPermuteArray[j];
        mPermuteArray[j] = mPermuteArray[n];
        mPermuteArray[n] = tmp;
      }
    }
  }

  private StrokeSet mStrokeA;
  private StrokeSet mStrokeB;
  private MatcherParameters mParam;
  private Float mSimilarity;
  private int[] mPermuteArray;
  private ArrayList<int[]> mPermutations;
}
