package com.js.geometrytest;

import java.util.ArrayList;

import com.js.geometry.Edge;
import com.js.geometry.Mesh;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.geometry.Vertex;
import com.js.testUtils.IOSnapshot;
import com.js.testUtils.MyTestCase;
import com.js.testUtils.MyTestUtils;
import static com.js.basic.Tools.*;

public class MeshTest extends MyTestCase {

	private Mesh mMesh;
	private Polygon mPolygon;

	private Mesh mesh() {
		if (mMesh == null)
			mMesh = new Mesh();
		return mMesh;
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
		polygon().embed(mesh());
	}

	private void triangulatePolygon() {
		PolygonTriangulator tri = PolygonTriangulator.triangulator(null,
				mesh(), polygon());
		tri.triangulate();
	}

	private void verifyEdgesFound(boolean omitDuals) {
		ArrayList<Edge> edges = mesh().constructListOfEdges(omitDuals);
		int nVert = polygon().numVertices();
		int nEdges = nVert + (nVert - 3);
		if (!omitDuals)
			nEdges *= 2;
		assertEquals(nEdges, edges.size());
	}

	private void verifySquareEdgesFound(boolean omitDuals) {
		ArrayList<Edge> edges = mesh().constructListOfEdges(omitDuals);
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
		// Dumping a mesh is not available except in debug
		if (!DEBUG_ONLY_FEATURES)
			return;

		IOSnapshot.open();
		polygon(Polygon.TESTPOLY_DRAGON_X + 1);
		triangulatePolygon();
		ArrayList<Edge> edges = mesh().constructListOfEdges(true);
		MyTestUtils.permute(random(), edges);
		for (Edge edge : edges) {
			pr(mesh().dump(false, true));
			if (random().nextBoolean())
				edge = edge.dual();
			pr("Deleting: " + dump(edge));
			mesh().deleteEdge(edge);
			pr("");
		}
		pr("after deleting all edges:");
		pr(mesh().dump(false, true));
		IOSnapshot.close();
	}

	public void testDeleteVertices() {
		// Dumping a mesh is not available except in debug
		if (!DEBUG_ONLY_FEATURES)
			return;

		IOSnapshot.open();
		polygon(Polygon.TESTPOLY_DRAGON_X + 1);
		triangulatePolygon();
		int nVertices = mMesh.numVertices();
		while (nVertices != 0) {
			Vertex v = mMesh.vertex(random().nextInt(nVertices));
			nVertices--;

			pr(mesh().dump(false, true));
			pr("Deleting: " + dump(v));
			mMesh.deleteVertex(v);
			pr("");
		}
		pr("after deleting all vertices:");
		pr(mesh().dump(false, true));
		IOSnapshot.close();
	}

}
