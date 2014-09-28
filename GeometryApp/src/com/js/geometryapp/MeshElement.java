package com.js.geometryapp;

import com.js.geometry.Edge;
import com.js.geometry.Mesh;

class MeshElement extends AlgorithmDisplayElement {

	public MeshElement(Mesh context) {
		mContext = context;
	}

	@Override
	public void render() {
		setColorState(color());
		setLineWidthState(lineWidth());
		for (Edge e : mContext.constructListOfEdges(true)) {
			renderLine(e.sourceVertex(), e.destVertex());
		}
	}

	private Mesh mContext;
}
