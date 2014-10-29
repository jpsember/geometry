package com.js.geometryapp;

import java.util.Random;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonMesh;
import com.js.geometry.Renderable;

import static com.js.basic.Tools.*;

// TODO: should this be made a package-vis class of js.geometry?
public class PolygonElement implements Renderable {

	public static enum Style {
		FILLED, BOUNDARY, POLYLINE,
	};

	public PolygonElement(Polygon polygon, Style style) {
		mPolygon = new Polygon(polygon);
		// Always perturb polygons that are rendered, since we want to
		// avoid geometry exceptions when displaying an algorithm (as
		// opposed to those that occur during an algorithm execution)
		mPolygon.perturb(new Random(1));
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
			PolygonProgram p = RenderTools.polygonProgram();
			p.setColor(RenderTools.getRenderColor()); // color());
			p.render(mesh);
		} else {
			if (mPolygon.numVertices() == 0) {
				warning("rendering PolygonElement with no vertices");
				return;
			}
			for (int i = 0; i < mPolygon.numVertices(); i++)
				RenderTools.extendPolyline(mPolygon.vertex(i));
			if (mStyle == Style.BOUNDARY)
				RenderTools.closePolyline();
			RenderTools.renderPolyline();
		}
	}

	private Polygon mPolygon;
	private Style mStyle;
}
