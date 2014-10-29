package com.js.geometry;

import com.js.geometryapp.LineElement;

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
		Renderable r = new LineElement(mPoint1, mPoint2, mDirected);
		r.render(s);
	}

	private Point mPoint1, mPoint2;
	private boolean mDirected;
}
