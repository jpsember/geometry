package com.js.gest;

import static com.js.basic.Tools.*;

/**
 * Used by matching algorithm; represents an entry in the dynamic programming
 * table
 */
class Cell {

  public Cell(int aIndex, int bIndex) {
    mIndexA = aIndex;
    mIndexB = bIndex;
    mCost = 0;
  }

  public void setCost(float cost) {
    mCost = cost;
  }

  public float cost() {
    return mCost;
  }

  public void setPrevCell(Cell cell) {
    mPrevCell = cell;
  }

  public Cell getPrevCell() {
    return mPrevCell;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(d(mIndexA, 2));
    sb.append(d(mIndexB, 2));
    sb.append(d(cost()));
    return sb.toString();
  }

  // Indices and previous cell are for display / debug purposes only; all we
  // really need is the cost
  private int mIndexA;
  private int mIndexB;
  private float mCost;
  private Cell mPrevCell;
}
