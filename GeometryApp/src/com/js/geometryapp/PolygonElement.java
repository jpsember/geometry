package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonMesh;

import static com.js.basic.Tools.*;

class PolygonElement extends AlgorithmDisplayElement {

	public static enum Style {
		FILLED, BOUNDARY, POLYLINE,
	};

	public PolygonElement(Polygon polygon, Style style) {
		mPolygon = new Polygon(polygon);
		// Always perturb polygons that are rendered, since we want to
		// avoid geometry exceptions when displaying an algorithm (as
		// opposed to those that occur during an algorithm execution)
		mPolygon.perturb(random());
		mStyle = style;
	}

	@Override
	public void render(AlgorithmStepper s) {

		if (mStyle == Style.FILLED) {
			int orientation = mPolygon.orientation();
			if (orientation != 1) {
				pr("polygon isn't ccw orientation=" + orientation);
				mStyle = Style.BOUNDARY;
			}
		}

		if (mStyle == Style.FILLED) {
			boolean useStrips = false;
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
