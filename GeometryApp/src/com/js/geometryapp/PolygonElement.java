package com.js.geometryapp;

import com.js.geometry.Polygon;
import com.js.geometry.PolygonMesh;

import static com.js.basic.Tools.*;

class PolygonElement extends AlgorithmDisplayElement {

	public static enum Style {
		FILLED, BOUNDARY, POLYLINE,
	};

	public PolygonElement(Polygon polygon, Style style) {
		mPolygon = new Polygon(polygon);
		mStyle = style;
		doNothing();
	}

	@Override
	public void render() {

		if (mStyle == Style.FILLED) {
			int orientation = mPolygon.orientation();
			if (orientation != 1) {
				pr("polygon isn't ccw orientation=" + orientation);
				mStyle = Style.BOUNDARY;
			}
		}

		if (mStyle == Style.FILLED) {
			boolean useStrips = false;
			if (false) {
				warning("verifying strips still work");
				useStrips = true;
			}
			PolygonMesh mesh = PolygonMesh.meshForPolygon(mPolygon, useStrips);
			PolygonProgram p = AlgorithmDisplayElement.polygonProgram();
			p.setColor(color());
			p.render(mesh);
		} else {
			setColorState(color());
			setLineWidthState(lineWidth());
			for (int i = 0; i < mPolygon.numVertices(); i++)
				extendPolyline(mPolygon.vertex(i));
			if (mStyle == Style.BOUNDARY)
				closePolyline();
			renderPolyline();
		}
	}

	private Polygon mPolygon;
	private Style mStyle;
}
