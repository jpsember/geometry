package com.js.geometry;

import java.util.ArrayList;
import java.util.List;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Point;
import com.js.geometry.Renderable;
import com.js.geometryapp.RenderTools;

public class Polyline implements Renderable {

	public void add(Point vertexLocation) {
		mVertices.add(vertexLocation);
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
		RenderTools.startPolyline();
		for (Point v : mVertices) {
			RenderTools.extendPolyline(v);
		}
		if (mClosed)
			RenderTools.closePolyline();
		RenderTools.renderPolyline();
	}

	public List<Point> vertices() {
		return mVertices;
	}

	private List<Point> mVertices = new ArrayList();
	private boolean mClosed;
}
