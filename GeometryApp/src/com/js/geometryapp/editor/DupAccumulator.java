package com.js.geometryapp.editor;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import static com.js.basic.Tools.*;

/**
 * Logic for offsetting duplicated or pasted objects based on user's adjustments
 * of the objects' positions.
 * 
 * This maintains two translation vectors: a 'duplication' translation vector D,
 * and a 'paste' translation vector P.
 * 
 * D is the sum of a small default translation, plus whatever little adjustments
 * the user has made.
 * 
 * P is equal to D, plus a correcting factor to move the clipboard contents
 * (whose position does not change) to the most recently pasted location.
 * 
 */
class DupAccumulator {

	private static final boolean db = false && DEBUG_ONLY_FEATURES;

	public DupAccumulator(DupAccumulator source) {
		mPickRadius = source.mPickRadius;
		mPreviousUserDirection = source.mPreviousUserDirection;
		mAccumulator = new Point(source.mAccumulator);
		mClipboardAdjustment = new Point(source.mClipboardAdjustment);
	}

	public DupAccumulator(float pickRadius) {
		if (db)
			pr("\n\nConstructed " + this);
		mPickRadius = pickRadius;
		mAccumulator = constructDefaultTranslation();
	}

	private Point constructDefaultTranslation() {
		return new Point(mPickRadius, 0);
	}

	/**
	 * Apply filter to accumulator. If user has changed direction abruptly,
	 * resets it so things don't get too wild.
	 */
	private void applyFilterToAccumulator() {
		float daLen = mAccumulator.magnitude();
		float daMin = mPickRadius;

		if (daLen > daMin) {
			float dir = MyMath.polarAngle(mAccumulator);

			if (mPreviousUserDirection == null) {
				mPreviousUserDirection = dir;
			} else {
				float angDiff = MyMath.normalizeAngle(mPreviousUserDirection
						- dir);
				if (Math.abs(angDiff) > MyMath.M_DEG * 30) {
					mPreviousUserDirection = null;
					mAccumulator = constructDefaultTranslation();
					if (db)
						pr("applyFilter, direction is too different; resetting accum");
				}
			}
		}
	}

	/**
	 * Determine translation for a paste operation
	 */
	public Point getOffsetForPaste() {
		// apply filter to dup offset amount
		applyFilterToAccumulator();

		// pasted objects will be offset by this (filtered) duplication offset,
		// plus the existing clipboard offset
		mClipboardAdjustment.add(mAccumulator);
		return mClipboardAdjustment;
	}

	/**
	 * Determine translation for a duplication operation
	 */
	public Point getOffsetForDup() {
		applyFilterToAccumulator();
		return mAccumulator;
	}

	/**
	 * Update accumulator for a move operation
	 */
	public void processMove(Point translation) {
		mAccumulator.add(translation);
		// add to clipboard offset as well
		mClipboardAdjustment.add(translation);
	}

	@Override
	public String toString() {
		if (!DEBUG_ONLY_FEATURES)
			return super.toString();

		StringBuilder sb = new StringBuilder("DupAccumulator " + nameOf(this));
		if (mPreviousUserDirection != null)
			sb.append(" prevUserDir:" + da(mPreviousUserDirection));
		sb.append(" accum:" + mAccumulator);
		sb.append(" cb adj:" + mClipboardAdjustment);

		return sb.toString();
	}

	// If not null, direction previous duplications were occurring within; used
	// to determine if subsequent adjustments are still generally in that
	// direction
	private Float mPreviousUserDirection;

	// The sum of the user's little adjustments, D0 + D1 + ... +Dn
	private Point mAccumulator;

	// This is the translation to move the clipboard objects to the last
	// user-adjusted paste location
	private Point mClipboardAdjustment = new Point();

	private float mPickRadius;
}
