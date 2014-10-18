package com.js.geometryapp.editor;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import static com.js.basic.Tools.*;

/**
 * Logic for offsetting multiple duplicated, pasted objects based on user's
 * adjustments.
 * 
 * Let A be the position of the new instance of an item, the result of a paste
 * or duplicate operation. Let B be the position of the last instance of this
 * item. Let C be the position of the second-to-last instance of this item.
 * 
 * Let P be the position of the clipboard's instance of the item.
 * 
 * We wish to determine the location of A given those of B and C.
 * 
 * There are two values manipulated:
 * 
 * 1) The accumulator represents the distance from C to B.
 * 
 * 2) The clipboard adjustment represents the distance from P to B.
 * 
 */
class DupAccumulator {

	private static final boolean db = false && DEBUG_ONLY_FEATURES;

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
		Point offset = MyMath.add(mAccumulator, mClipboardAdjustment);
		// add the dup accumulator to the clip adjust, to make clipboard
		// represent newest instance.
		mClipboardAdjustment.add(mAccumulator);
		return offset;
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

	// dupAccum is the sum of the user's little adjustments
	private Point mAccumulator;

	// dupClipAdjust is the translation that must be applied to the clipboard;
	// it reflects amounts added to dupAccum after clipboard last modified
	private Point mClipboardAdjustment = new Point();

	private float mPickRadius;
}
