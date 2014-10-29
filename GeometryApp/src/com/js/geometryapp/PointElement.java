package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;
import com.js.geometry.Renderable;

public class PointElement implements Renderable {

	public PointElement(Point point, float radius) {
		mPoint = new Point(point);
		mRadius = radius;
	}

	@Override
	public void render(AlgorithmStepper s) {
		AlgorithmDisplayElement.renderPoint(mPoint, mRadius);
	}

	private Point mPoint;
	private float mRadius;

}
