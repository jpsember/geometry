package com.js.geometry;

import com.js.geometryapp.RenderTools;

public class Segment implements Renderable {

	/**
	 * Construct a directed segment
	 */
	public static Segment directed(Point p1, Point p2) {
		return new Segment(p1, p2, true);
	}

	/**
	 * Construct an undirected segment
	 */
	public Segment(Point p1, Point p2) {
		this(p1, p2, false);
	}

	private Segment(Point p1, Point p2, boolean directed) {
		mPoint1 = p1;
		mPoint2 = p2;
		mDirected = directed;
	}

	public void render(AlgorithmStepper s) {
		if (!mDirected)
			RenderTools.renderLine(mPoint1, mPoint2);
		else
			RenderTools.renderRay(mPoint1, mPoint2);
	}

	private Point mPoint1, mPoint2;
	private boolean mDirected;
}
