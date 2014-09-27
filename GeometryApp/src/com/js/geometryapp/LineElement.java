package com.js.geometryapp;

import com.js.geometry.Point;

class LineElement extends AlgorithmDisplayElement {

	public LineElement(Point p1, Point p2) {
		mPoint1 = p1;
		mPoint2 = p2;
	}

	@Override
	public void render() {
		setColorState(color());
		setLineWidthState(lineWidth());
		renderLine(mPoint1, mPoint2);
	}

	private Point mPoint1, mPoint2;
}
