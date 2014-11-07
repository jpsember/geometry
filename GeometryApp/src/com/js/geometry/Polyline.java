package com.js.geometry;

import java.util.ArrayList;
import java.util.List;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;
import com.js.geometry.Renderable;
import com.js.geometryapp.PolylineProgram;
import com.js.geometryapp.RenderTools;

public class Polyline implements Renderable {

	public static Polyline polyline(Point... vertices) {
		Polyline p = new Polyline();
		p.add(vertices);
		return p;
	}

	public static Polyline closedPolyline(Point... vertices) {
		Polyline p = polyline(vertices);
		p.close();
		return p;
	}

	public void add(Point vertexLocation) {
		mVertices.add(vertexLocation);
	}

	public void add(Point... vertices) {
		for (Point v : vertices)
			add(v);
	}

	/**
	 * Make this polyline a closed loop
	 */
	public void close() {
		mClosed = true;
	}

	public boolean isClosed() {
		return mClosed;
	}

	@Override
	public void render(AlgorithmStepper s) {
		PolylineProgram p = RenderTools.polylineProgram();
		p.setColor(RenderTools.getRenderColor());
		p.render(mVertices, null, null, mClosed);
	}

	public List<Point> vertices() {
		return mVertices;
	}

	private List<Point> mVertices = new ArrayList();
	private boolean mClosed;
}
