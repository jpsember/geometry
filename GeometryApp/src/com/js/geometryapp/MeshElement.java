package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Edge;
import com.js.geometry.Mesh;
import com.js.geometry.Renderable;

class MeshElement implements Renderable {

	public MeshElement(Mesh mesh) {
		mMesh = mesh;
	}

	@Override
	public void render(AlgorithmStepper s) {
		for (Edge e : mMesh.constructListOfEdges(true)) {
			RenderTools.renderLine(e.sourceVertex(), e.destVertex());
		}
	}

	private Mesh mMesh;
}
