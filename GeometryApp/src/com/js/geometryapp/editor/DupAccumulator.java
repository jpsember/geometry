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

	public DupAccumulator(Editor editor) {
		if (db)
			pr("\n\nConstructed " + this);
		mEditor = editor;
		mAccumulator = new Point(mEditor.pickRadius(), 0);
	}

	/**
	 * Get the accumulator.
	 * 
	 * @param filter
	 *            if true, and accumulator has changed direction radically,
	 *            reset the accumulator value so user has to re-specify this
	 *            offset
	 * @return accumulator
	 */
	public Point getAccum(boolean filter) {
		if (filter)
			getFilteredAccum();
		if (db)
			pr("getAccum (filter " + d(filter) + "): " + mAccumulator);
		return new Point(mAccumulator);
	}

	/**
	 * Get the clipboard adjust value
	 */
	public Point getClipboardAdjust() {
		if (db)
			pr("getClipboardAdjust: " + mClipboardAdjustment);
		return new Point(mClipboardAdjustment);
	}

	/**
	 * Set accumulator
	 */
	public void setAccum(Point a) {
		if (db)
			pr("setAccum from " + mAccumulator + " to " + a);
		mAccumulator.setTo(a);
	}

	/**
	 * Set the clipboard adjust value
	 */
	public void setClipboardAdjust(Point b) {
		if (db)
			pr("setClipboardAdjust from " + mClipboardAdjustment + " to " + b);
		mClipboardAdjustment.setTo(b);
	}

	/**
	 * Update the clipboard adjust value by adding the accumulator to it
	 */
	public void updateClipboardAdjust() {
		mClipboardAdjustment.add(mAccumulator);
		if (db)
			pr("updateClipboardAdjust to " + mClipboardAdjustment);
	}

	/**
	 * Get dup accumulator amount. If user has changed direction abruptly,
	 * resets it so things don't get too wild.
	 * 
	 * @return filtered dup accumulator
	 * 
	 */
	private Point getFilteredAccum() {

		float daLen = mAccumulator.magnitude();
		float daMin = 20;

		if (db)
			pr("getDupAmount; dupAccum=" + mAccumulator + " len=" + d(daLen)
					+ " min=" + d(daMin));

		if (daLen > daMin) {

			float dir = MyMath.polarAngle(mAccumulator);
			if (db)
				pr(" dir=" + da(dir) + " previousUserDirection="
						+ mPreviousUserDirection);

			if (mPreviousUserDirection == null) {
				mPreviousUserDirection = dir;
			} else {
				float angDiff = MyMath.normalizeAngle(mPreviousUserDirection
						- dir);
				if (db)
					pr(" prevDir=" + da(mPreviousUserDirection) + " angDiff="
							+ da(angDiff));

				if (Math.abs(angDiff) > MyMath.M_DEG * 30) {
					if (db)
						pr("  resetting dup offset");

					mPreviousUserDirection = null;
					Point newAccum = new Point(1 - mAccumulator.x,
							1 - mAccumulator.y);

					mAccumulator.add(newAccum);
					if (db)
						pr(" added " + newAccum + " to dupAccum, now "
								+ mAccumulator + " and dupClipAdjust, now "
								+ mClipboardAdjustment);

				}
			}
		}

		return mAccumulator;
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

	private Editor mEditor;
}