package com.js.geometryapp.editor;

import com.js.geometry.MyMath;
import com.js.geometry.Point;

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

	/**
	 * Copy constructor
	 */
	public DupAccumulator(DupAccumulator source) {
		mPickRadius = source.mPickRadius;
		mPreviousUserDirection = source.mPreviousUserDirection;
		mAccumulator = new Point(source.mAccumulator);
		mClipboardAdjustment = new Point(source.mClipboardAdjustment);
	}

	public DupAccumulator(float pickRadius) {
		mPickRadius = pickRadius;
		mAccumulator = constructDefaultTranslation();
	}

	private Point constructDefaultTranslation() {
		return new Point(mPickRadius, 0);
	}

	/**
	 * Apply filter to accumulator. If user has changed direction abruptly, and
	 * distance is large, resets it so things don't get too wild.
	 */
	private void applyFilterToAccumulator() {
		float largeDistance = mPickRadius * 5;

		if (mAccumulator.magnitude() > largeDistance) {
			float dir = MyMath.polarAngle(mAccumulator);

			if (mPreviousUserDirection == null) {
				mPreviousUserDirection = dir;
			} else {
				float angDiff = MyMath.normalizeAngle(mPreviousUserDirection
						- dir);
				if (Math.abs(angDiff) > MyMath.M_DEG * 30) {
					mPreviousUserDirection = null;
					mAccumulator = constructDefaultTranslation();
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
		return new Point(mClipboardAdjustment);
	}

	/**
	 * Determine translation for a duplication operation
	 */
	public Point getOffsetForDup() {
		applyFilterToAccumulator();
		return new Point(mAccumulator);
	}

	/**
	 * Update accumulator for a move operation
	 */
	public void processMove(Point translation) {
		mAccumulator.add(translation);
		// add to clipboard offset as well
		mClipboardAdjustment.add(translation);
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
