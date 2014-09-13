package com.js.geometryapp;

import com.js.geometry.Point;

public class AlgDisplayPoint extends AlgDisplayElement {

	public AlgDisplayPoint(Point point) {
		mPoint = new Point(point);
	}

	@Override
	public void render() {
		renderPoint(mPoint);
	}

	private Point mPoint;

}
