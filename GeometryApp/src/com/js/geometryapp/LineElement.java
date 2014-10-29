package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;
import com.js.geometry.Renderable;

public class LineElement implements Renderable {

	public LineElement(Point p1, Point p2, boolean directed) {
		mPoint1 = p1;
		mPoint2 = p2;
		mDirected = directed;
	}

	@Override
	public void render(AlgorithmStepper s) {
		if (!mDirected)
			RenderTools.renderLine(mPoint1, mPoint2);
		else
			RenderTools.renderRay(mPoint1, mPoint2);
	}

	private Point mPoint1, mPoint2;
	private boolean mDirected;
}
