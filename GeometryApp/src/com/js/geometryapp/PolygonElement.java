package com.js.geometryapp;

import com.js.geometry.GeometryContext;
import com.js.geometry.Polygon;
import static com.js.basic.Tools.*;

public class PolygonElement extends AlgorithmDisplayElement {

	public PolygonElement(Polygon polygon, boolean filled) {
		mPolygon = new Polygon(polygon);
		mFilled = filled;
		doNothing();
	}

	@Override
	public void render() {

		if (mFilled) {
			int orientation = mPolygon.orientation(new GeometryContext(1));
			if (orientation != 1) {
				pr("polygon isn't ccw orientation=" + orientation);
				mFilled = false;
			}
		}

		if (!mFilled) {
			setColorState(color());
			setLineWidthState(lineWidth());
			for (int i = 0; i < mPolygon.numVertices(); i++)
				extendPolyline(mPolygon.vertex(i));
			closePolyline();
			renderPolyline();
		} else {
			PolygonMesh mesh = PolygonMesh.meshForSimplePolygon(mPolygon);
			PolygonProgram p = AlgorithmDisplayElement.polygonProgram();
			p.setColor(color());
			p.render(mesh);
		}
	}

	private Polygon mPolygon;
	private boolean mFilled;
}