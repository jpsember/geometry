package com.js.geometryapp;

import com.js.geometry.Edge;
import com.js.geometry.Mesh;

class MeshElement extends AlgorithmDisplayElement {

	public MeshElement(Mesh mesh) {
		mMesh = mesh;
	}

	@Override
	public void render() {
		setColorState(color());
		setLineWidthState(lineWidth());
		for (Edge e : mMesh.constructListOfEdges(true)) {
			renderLine(e.sourceVertex(), e.destVertex());
		}
	}

	private Mesh mMesh;
}
