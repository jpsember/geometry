package com.js.gest;

import static com.js.basic.Tools.*;

/**
 * For recording and displaying statistics about the matching algorithm
 */
public class AlgorithmStats {

  public float cellsExaminedRatio() {
    if (mTotalCellCount == 0)
      return 0;
    return ((float) mActualCellsExamined) / mTotalCellCount;
  }

  public void incrementCellsExamined() {
    mActualCellsExamined++;
  }

  public void adjustTotalCellCount(int mMaxCellsExamined) {
    mTotalCellCount += mMaxCellsExamined;
  }

  public void incrementExecutionCount() {
    mExecutions++;
  }

  public void clear() {
    mActualCellsExamined = 0;
    mTotalCellCount = 0;
    mExecutions = 0;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("------------------------------------------------\n");
    sb.append("      Run count: " + d(mExecutions) + "\n");
    sb.append("    Total cells: " + d(mTotalCellCount) + "\n");
    sb.append(" Cells examined: " + d(mActualCellsExamined) + " ("
        + d((int) (100 * cellsExaminedRatio())) + " %)\n");
    sb.append("------------------------------------------------\n");
    return sb.toString();
  }

  private int mActualCellsExamined;
  private int mTotalCellCount;
  private int mExecutions;
}
