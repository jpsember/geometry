package com.js.geometryapp;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;

public class MeshElement extends AlgorithmDisplayElement {

	public MeshElement(GeometryContext context) {
		mContext = context;
	}

	@Override
	public void render() {
		setColorState(color());
		setLineWidthState(lineWidth());
		for (Edge e : mContext.constructListOfEdges(true)) {
			renderLine(e.sourceVertex().point(), e.destVertex().point());
		}
	}

	private GeometryContext mContext;
}
