package com.js.gest;

import static com.js.basic.Tools.*;

import com.js.basic.Freezable;

public class MatcherParameters extends Freezable.Mutable {

	public static final MatcherParameters DEFAULT = frozen(new MatcherParameters());

	public MatcherParameters() {
		setZeroDistanceThreshold(StrokeSet.STANDARD_WIDTH * .01f);
		mSquaredErrorFlag = true;
	}

	public void setZeroDistanceThreshold(float threshold) {
		mutate();
		mZeroDistanceThreshold = threshold;
	}

	public float zeroDistanceThreshold() {
		return mZeroDistanceThreshold;
	}

	public void setSquaredErrorFlag(boolean f) {
		mutate();
		mSquaredErrorFlag = f;
	}

	public boolean squaredErrorFlag() {
		return mSquaredErrorFlag;
	}

	@Override
	public Freezable getMutableCopy() {
		MatcherParameters m = new MatcherParameters();
		m.setZeroDistanceThreshold(zeroDistanceThreshold());
		m.setSquaredErrorFlag(squaredErrorFlag());
		return m;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("MatcherParameters");
		sb.append("\n zero threshold: " + d(zeroDistanceThreshold()));
		sb.append("\n squared errors: " + d(squaredErrorFlag()));
		return sb.toString();
	}

	private float mZeroDistanceThreshold;
	private boolean mSquaredErrorFlag;

}
