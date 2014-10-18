package com.js.geometryapp.editor;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import static com.js.basic.Tools.*;

/**
 * Logic for offsetting duplicated or pasted objects based on user's adjustments
 * of the objects' positions.
 * 
 * This maintains two translation vectors: a 'duplication' translation vector D,
 * and a 'paste' translation vector P. Initially, D and P are both set to some
 * small value, e.g., D0 = (+x, 0).
 * 
 * A duplication operation involves taking object(s) at X, duplicating them, and
 * placing them at location Y = X + D0. If user subsequently moves these objects
 * to location Y' = Y + D1, and duplicates them again, they will be placed at
 * location Y'' = Y' + (Y' - X) = Y' + (D1 + D0). This class maintains the
 * 'primary' translation vector D0 + D1 + ... + Dn, which is a sum of
 * (relatively small) adjustments whose total represents the desired offset of
 * one version of X from its immediate predecessor.
 * 
 * A copy operation places copies of objects at X into the clipboard. Each
 * subsequent paste operation duplicates these objects and places them at
 * location Yn = Y(n-1) + P0 + P1 + ... + Pn, where Y0 = P0, and the sum P0 + P1
 * + ... + Pn represents approximately k multiples of the offset of an X' from
 * its predecessor X.
 * 
 * The simple result of all this is when a move operation occurs, we add the
 * move translation vector to both D and P; and when a paste operation occurs,
 * we paste the clipboard objects after translating them by P, then add D to P.
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
				}
			}
		}
	}

	/**
	 * Determine translation for a paste operation
	 */
	public Point getOffsetForPaste() {
		applyFilterToAccumulator();
		Point pasteOffset = MyMath.add(mAccumulator, mClipboardAdjustment);
		// add the duplication adjustment to the clipboard adjustment, since
		// we're now k generations advanced from the clipboard position
		mClipboardAdjustment.add(mAccumulator);
		return pasteOffset;
	}

	/**
	 * Update accumulator for a move operation
	 * 
	 * @param translation
	 *            amount of move
	 */
	public void processMove(Point translation) {
		mAccumulator.add(translation);
		mClipboardAdjustment.add(translation);
		if (db)
			pr("processMove translation " + translation + ", now\n" + this);
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
