package com.js.geometrytest;

import java.util.ArrayList;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.testUtils.MyTestCase;

public class GeometryContextTest extends MyTestCase {

	private GeometryContext mContext;
	private Polygon mPolygon;

	private GeometryContext context() {
		if (mContext == null)
			mContext = new GeometryContext(42);
		return mContext;
	}

	private Polygon square() {
		if (mPolygon == null) {
			mPolygon = Polygon.polygonWithScript("20 20 200 20 200 200 20 200");
		}
		return mPolygon;
	}

	private Polygon polygon() {
		if (mPolygon == null) {
			mPolygon = Polygon.testPolygon(new GeometryContext(1),
					Polygon.TESTPOLY_DRAGON_X + 5);
		}
		return mPolygon;
	}

	private void embedPolygon() {
		polygon().embed(context());
	}

	private void triangulatePolygon() {
		PolygonTriangulator tri = PolygonTriangulator.triangulator(context(),
				polygon());
		tri.triangulate();
	}

	private void verifyEdgesFound(boolean omitDuals) {
		ArrayList<Edge> edges = context().constructListOfEdges(omitDuals);
		int nVert = polygon().numVertices();
		int nEdges = nVert + (nVert - 3);
		if (!omitDuals)
			nEdges *= 2;
		assertEquals(nEdges, edges.size());
	}

	private void verifySquareEdgesFound(boolean omitDuals) {
		ArrayList<Edge> edges = context().constructListOfEdges(omitDuals);
		int nVert = polygon().numVertices();
		int nEdges = nVert;
		if (!omitDuals)
			nEdges *= 2;
		assertEquals(nEdges, edges.size());
	}

	public void testConstructEdgeListOmitDuals() {
		triangulatePolygon();
		verifyEdgesFound(true);
	}

	public void testConstructEdgeListincludingDuals() {
		triangulatePolygon();
		verifyEdgesFound(false);
	}

	public void testConstructEdgeListOmitDualsSquare() {
		// Build square mesh first, so dragon is not built
		square();
		embedPolygon();
		verifySquareEdgesFound(true);
	}

	public void testConstructEdgeListIncludingDualsSquare() {
		// Build square mesh first, so dragon is not built
		square();
		embedPolygon();
		verifySquareEdgesFound(false);
	}

}
