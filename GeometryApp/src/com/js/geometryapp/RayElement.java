package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;

class RayElement extends AlgorithmDisplayElement {

	public RayElement(Point p1, Point p2) {
		mPoint1 = p1;
		mPoint2 = p2;
	}

	@Override
	public void render(AlgorithmStepper s) {
		renderRay(mPoint1, mPoint2);
	}

	private Point mPoint1, mPoint2;
}
