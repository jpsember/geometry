package com.js.geometryapp;

import com.js.android.MyActivity;
import com.js.geometry.Disc;
import com.js.geometry.MyMath;
import com.js.geometry.Point;

class DiscElement extends AlgorithmDisplayElement {

	public DiscElement(Disc disc) {
		mDisc = disc;
	}

	@Override
	public void render(AlgorithmStepper s) {
		setColorState(color());
		setLineWidthState(lineWidth());

		Point origin = mDisc.getOrigin();
		float radius = mDisc.getRadius();
		final float SIDES_FACTOR = .06f;
		final int SIDES_MIN = 4;
		final int SIDES_MAX = 50;

		// Construct a polygonal approximation to this disc, one that
		// is smooth yet doesn't have more edges than are necessary

		// Get physical size of disc, and take its cube root (approx)
		float pickRadius = MyActivity.getResolutionInfo()
				.inchesToPixelsAlgorithm(.14f);
		float scaledRadius = (float) Math.pow(radius / pickRadius, .3f);
		// Scale this by a constant to get the number of polygonal edges
		int numberOfSides = (int) (scaledRadius / SIDES_FACTOR);
		int numberOfSidesClamped = MyMath.clamp(numberOfSides, SIDES_MIN,
				SIDES_MAX);

		float angleSep = ((360 * MyMath.M_DEG) / numberOfSidesClamped);
		// List<Point> pts = new ArrayList();
		for (int i = 0; i < numberOfSidesClamped; i++) {
			extendPolyline(MyMath.pointOnCircle(origin, i * angleSep, radius));
		}
		closePolyline();
		renderPolyline();
	}

	private Disc mDisc;
}
