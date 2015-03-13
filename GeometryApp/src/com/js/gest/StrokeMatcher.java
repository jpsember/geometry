package com.js.gest;

import static com.js.basic.Tools.*;

import java.util.ArrayList;
import java.util.Collections;

import com.js.basic.MyMath;
import com.js.basic.Point;

/**
 * Determines how closely two strokes match
 * 
 * Public for test purposes
 */
public class StrokeMatcher {

	public StrokeMatcher(Stroke a, Stroke b, MatcherParameters parameters) {
		mStrokeA = frozen(a);
		mStrokeB = frozen(b);
		if (parameters == null)
			parameters = MatcherParameters.DEFAULT;
		mParameters = parameters;
	}

	private boolean matched() {
		return mSimilarity != null;
	}

	public float similarity() {
		if (mSimilarity == null)
			calculateSimilarity();
		return mSimilarity;
	}

	/**
	 * Construct the optimal path within the dynamic table
	 * 
	 * @return an array of cells leading from the bottom left to the top right
	 */
	public ArrayList<Cell> optimalPath() {
		if (!matched())
			throw new IllegalStateException();
		ArrayList<Cell> list = new ArrayList();

		Cell cell = mBestCell;
		while (cell != null) {
			list.add(cell);
			cell = cell.getPrevCell();
		}
		Collections.reverse(list);
		return list;
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
	 * 
	 * If we examine diagonal slices of the table, i.e., sets of cells (x,y) where
	 * x+y = some constant, observe that no such slice depends on more than the
	 * preceding two slices. Hence we need not store the entire table, but just
	 * three slices: the two preceding ones, and the one being constructed
	 * (actually, if we get clever, the third one might be able to occupy the same
	 * space as the earlier of the two preceding ones; we'll leave this as a
	 * future optimization).
	 * 
	 * We identify the 'axis' of the table as the diagonal line from
	 * (0,0)...(n-1,n-1)
	 */
	private void calculateSimilarity() {
		if (mStrokeA.size() != mStrokeB.size())
			throw new IllegalArgumentException("stroke lengths mismatch");
		prepare();

		// We've already generated the first column, so start with second
		int aCursor = mWindowSize + 1;
		int bCursor = -mWindowSize;
		for (int column = 1; column < mTotalColumns; column++) {
			boolean parity = (column & 1) != 0;
			generateDynamicTableColumn(aCursor, bCursor);
			if (!parity)
				aCursor++;
			else
				bCursor++;
		}

		mBestCell = mColumn1[mWindowSize];

		mSimilarity = cost();
	}

	/**
	 * Generate another column in the dynamic programming table
	 * 
	 * @param aBottomCursor
	 *          stroke A cursor for bottom row of the new column
	 * @param bBottomCursor
	 *          stroke B cursor for bottom row of the new column
	 */
	private void generateDynamicTableColumn(int aBottomCursor, int bBottomCursor) {

		boolean parity = ((aBottomCursor ^ bBottomCursor) & 1) != 0;
		mColumn2 = buildColumn();

		for (int y = 0; y < mColumnSize; y++) {
			int aIndex = aBottomCursor - y;
			int bIndex = bBottomCursor + y;
			if (!isLegalCell(aIndex, bIndex))
				continue;
			clearOptimalEdge();
			// Multiply by 2 here, since we count each distance twice when advancing
			// along both paths
			examineEdge(mColumn0[y], aIndex, bIndex, 2f);
			examineEdge(mColumn1[y], aIndex, bIndex, 1f);
			if (parity && y > 0)
				examineEdge(mColumn1[y - 1], aIndex, bIndex, 1f);
			else if (!parity && y + 1 < mColumnSize)
				examineEdge(mColumn1[y + 1], aIndex, bIndex, 1f);
			storeBestEdgeIntoCell(y, aIndex, bIndex);
		}

		mColumn0 = mColumn1;
		mColumn1 = mColumn2;
	}

	private boolean isLegalCell(int aIndex, int bIndex) {
		return aIndex >= 0 && aIndex < pathLength() && bIndex >= 0
				&& bIndex < pathLength();
	}

	/**
	 * Initialize 'best edge' to undefined for candidates leading into a cell
	 */
	private void clearOptimalEdge() {
		mMinCost = 0;
		mMinPredecessor = null;
	}

	/**
	 * Examine an edge from a source to a destination cell, and store as the
	 * optimal edge if the resulting total cost at the destination is the minimum
	 * seen yet
	 * 
	 * @param sourceCell
	 *          source cell; if null, does nothing
	 * @param a
	 *          coefficients of destination cell
	 * @param b
	 * @param multiplier
	 *          amount to weight the cost of the edge; normally 1, but can be 2 if
	 *          edge represents advancement along both strokes
	 */
	private void examineEdge(Cell sourceCell, int a, int b, float multiplier) {
		if (sourceCell == null)
			return;
		float cost = comparePoints(a, b);
		cost = cost * multiplier + sourceCell.cost();

		float diff = mMinCost - cost;
		diff = mMinCost - cost;

		if (mMinPredecessor == null || diff > 0) {
			mMinCost = cost;
			mMinPredecessor = sourceCell;
		}
	}

	/**
	 * Store the optimal edge leading to this cell (does nothing if no optimal
	 * edge exists)
	 */
	private void storeBestEdgeIntoCell(int row, int a_index, int b_index) {
		if (mMinPredecessor == null)
			return;
		Cell cell = buildNewCell(a_index, b_index);
		cell.setCost(mMinCost);
		cell.setPrevCell(mMinPredecessor);
		mColumn2[row] = cell;
	}

	private float cost() {
		if (mBestCell == null)
			throw new IllegalStateException();
		float c = mBestCell.cost();
		// Divide by the number steps taken, including one for the initial cost
		c /= mTotalColumns;
		if (mParameters.squaredErrorFlag()) {
			c = (float) Math.sqrt(c);
		}
		// Scale by the width of the standard rectangle
		c /= StrokeSet.STANDARD_WIDTH;
		return c;
	}

	private int pathLength() {
		return mStrokeA.size();
	}

	private void prepare() {
		mTotalColumns = pathLength() * 2 - 1;
		mWindowSize = mTotalColumns / 4;
		mWindowSize = (pathLength()) / 2;

		mColumnSize = 1 + 2 * mWindowSize;
		buildColumns();

		mBestCell = null;
	}

	private void buildColumns() {
		mColumn0 = buildColumn();
		mColumn1 = buildColumn();

		Cell cell00 = buildNewCell(0, 0);

		float cost = comparePoints(0, 0);
		cell00.setCost(cost);
		mColumn1[mWindowSize] = cell00;
	}

	private float comparePoints(int aIndex, int bIndex) {
		StrokePoint elem_a = mStrokeA.get(aIndex);
		StrokePoint elem_b = mStrokeB.get(bIndex);
		Point pos_a = elem_a.getPoint();
		Point pos_b = elem_b.getPoint();

		float dist;
		if (mParameters.squaredErrorFlag()) {
			dist = MyMath.squaredDistanceBetween(pos_a, pos_b);
			if (dist < mParameters.zeroDistanceThreshold()
					* mParameters.zeroDistanceThreshold())
				dist = 0;
		} else {
			dist = MyMath.distanceBetween(pos_a, pos_b);
			if (dist < mParameters.zeroDistanceThreshold())
				dist = 0;
		}
		return dist;
	}

	private Cell buildNewCell(int aIndex, int bIndex) {
		Cell cell = new Cell(aIndex, bIndex);
		return cell;
	}

	private Cell[] buildColumn() {
		return new Cell[mColumnSize];
	}

	private Stroke mStrokeA;
	private Stroke mStrokeB;
	private MatcherParameters mParameters;
	private Float mSimilarity;
	// The maximum distance a path can stray from the central axis
	private int mWindowSize;
	// The maximum number of steps in a path taken to compare the two
	// strokes. Each step advances along at least one of the paths, possibly
	// both
	private int mTotalColumns;
	private int mColumnSize;
	private Cell[] mColumn0;
	private Cell[] mColumn1;
	private Cell[] mColumn2;
	private Cell mBestCell;
	private float mMinCost;
	private Cell mMinPredecessor;
}
