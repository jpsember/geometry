package com.js.geometryapp;

import com.js.geometry.Polygon;

public class AlgDisplayPolygon extends AlgDisplayElement {
	public AlgDisplayPolygon(Polygon polygon) {
		mPolygon = new Polygon(polygon);
	}

	@Override
	public void render() {
		setColorState(color());
		setLineWidthState(lineWidth());
		for (int i = 0; i < mPolygon.numVertices(); i++)
			extendPolyline(mPolygon.vertex(i));
		closePolyline();
		renderPolyline();
	}

	private Polygon mPolygon;
}
