package com.js.geometryapp;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;

public class AlgDisplayMesh extends AlgDisplayElement {

	public AlgDisplayMesh(GeometryContext context) {
		mContext = context;
	}

	@Override
	public void render() {
		setColorState(color());
		setLineWidthState(lineWidth());
		for (Edge e : mContext.edgeBuffer()) {
			Edge dual = e.dual();
			if (e.angle() < dual.angle())
				continue;
			renderLine(e.sourceVertex().point(), e.destVertex().point());
		}
	}

	private GeometryContext mContext;
}
