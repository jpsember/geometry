package com.js.geometryapp;

import com.js.geometry.Point;

public class AlgDisplayPoint extends AlgDisplayElement {

	public AlgDisplayPoint(Point point) {
		mPoint = new Point(point);
	}

	@Override
	public void render() {
		int r = 5;
		extendPolyline(mPoint.x - r, mPoint.y - r);
		extendPolyline(mPoint.x + r, mPoint.y - r);
		extendPolyline(mPoint.x + r, mPoint.y + r);
		extendPolyline(mPoint.x - r, mPoint.y + r);
		closePolyline();
		renderPolyline();
	}

	private Point mPoint;

}
