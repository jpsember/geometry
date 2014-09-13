package com.js.geometryapp;

import com.js.geometry.Point;

public class AlgDisplayPoint extends AlgDisplayElement {

	public AlgDisplayPoint(Point point, float radius) {
		mPoint = new Point(point);
		mRadius = radius;
	}

	@Override
	public void render() {
		setColorState(color());
		renderPoint(mPoint, mRadius);
	}

	private Point mPoint;
	private float mRadius;

}
