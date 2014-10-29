package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;

public class LineElement extends AlgorithmDisplayElement {

	public LineElement(Point p1, Point p2, boolean directed) {
		mPoint1 = p1;
		mPoint2 = p2;
		mDirected = directed;
	}

	@Override
	public void render(AlgorithmStepper s) {
		if (!mDirected)
		renderLine(mPoint1, mPoint2);
		else
			renderRay(mPoint1, mPoint2);
	}

	private Point mPoint1, mPoint2;
	private boolean mDirected;
}
