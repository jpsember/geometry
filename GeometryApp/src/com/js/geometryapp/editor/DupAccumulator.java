package com.js.geometryapp.editor;

import com.js.geometry.MyMath;
import com.js.geometry.Point;

/**
 * Logic for offsetting duplicated or pasted objects based on user's adjustments
 * of the objects' positions.
 * 
 * This maintains a translation vector that is the sum of a small default
 * translation, plus whatever little adjustments the user has made.
 * 
 */
class DupAccumulator {

	/**
	 * Construct a copy of an accumulator, if it exists
	 * 
	 * @param source
	 * @return copy of source, or null if source is null
	 */
	public static DupAccumulator copyOf(DupAccumulator source) {
		if (source == null)
			return null;
		return new DupAccumulator(source);
	}

	/**
	 * Constructor
	 * 
	 * @param pickRadius
	 *            from editor, for choosing reasonable default accumulator
	 * @param polarAngle
	 *            direction of initial translation
	 */
	public DupAccumulator(float pickRadius, float polarAngle) {
		mAccumulator = MyMath.pointOnCircle(Point.ZERO, polarAngle, pickRadius);
	}

	/**
	 * Copy constructor
	 */
	private DupAccumulator(DupAccumulator source) {
		mAccumulator = new Point(source.mAccumulator);
	}

	/**
	 * Determine translation to apply to new objects
	 */
	public Point getAccumulator() {
		return new Point(mAccumulator);
	}

	/**
	 * Add translation to accumulator
	 */
	public void add(Point translation) {
		mAccumulator.add(translation);
	}

	private Point mAccumulator;
}
