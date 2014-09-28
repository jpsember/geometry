package com.js.geometrytest;

import java.util.ArrayList;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.geometry.Vertex;
import com.js.testUtils.IOSnapshot;
import com.js.testUtils.MyTestCase;
import com.js.testUtils.MyTestUtils;
import static com.js.basic.Tools.*;

public class GeometryContextTest extends MyTestCase {

	private GeometryContext mContext;
	private Polygon mPolygon;

	private GeometryContext context() {
		if (mContext == null)
			mContext = new GeometryContext();
		return mContext;
	}

	private Polygon square() {
		if (mPolygon == null) {
			mPolygon = Polygon.polygonWithScript("20 20 200 20 200 200 20 200");
		}
		return mPolygon;
	}

	private Polygon polygon() {
		return polygon(Polygon.TESTPOLY_DRAGON_X + 5);
	}

	private Polygon polygon(int defaultName) {
		if (mPolygon == null) {
			mPolygon = Polygon.testPolygon(defaultName);
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

	private String dump(Edge edge) {
		return dump(edge.sourceVertex()) + " --> " + dump(edge.destVertex());
	}

	private String dump(Vertex v) {
		return nameOf(v, false);
	}

	public void testDeleteEdges() {
		IOSnapshot.open();
		polygon(Polygon.TESTPOLY_DRAGON_X + 1);
		triangulatePolygon();
		ArrayList<Edge> edges = context().constructListOfEdges(true);
		MyTestUtils.permute(random(), edges);
		for (Edge edge : edges) {
			pr(context().dumpMesh(false, true));
			if (random().nextBoolean())
				edge = edge.dual();
			pr("Deleting: " + dump(edge));
			context().deleteEdge(edge);
			pr("");
		}
		pr("after deleting all edges:");
		pr(context().dumpMesh(false, true));
		IOSnapshot.close();
	}

	public void testDeleteVertices() {
		IOSnapshot.open();
		polygon(Polygon.TESTPOLY_DRAGON_X + 1);
		triangulatePolygon();
		int nVertices = mContext.vertexBuffer().size();
		while (nVertices != 0) {
			Vertex v = mContext.vertex(random().nextInt(nVertices));
			nVertices--;

			pr(context().dumpMesh(false, true));
			pr("Deleting: " + dump(v));
			mContext.deleteVertex(v);
			pr("");
		}
		pr("after deleting all vertices:");
		pr(context().dumpMesh(false, true));
		IOSnapshot.close();
	}

}
