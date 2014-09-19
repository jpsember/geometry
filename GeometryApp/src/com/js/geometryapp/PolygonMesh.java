package com.js.geometryapp;

import static com.js.basic.Tools.warning;

import java.util.ArrayList;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
import com.js.geometry.GeometryException;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;

/**
 * Note: this class can be easily adapted to generate triangle strips for
 * arbitrary (planar) meshes within GeometryContexts, so long as certain edges
 * are flagged as polygon edges.
 */
public class PolygonMesh {

	/**
	 * The number of floats per vertex
	 */
	public static final int VERTEX_COMPONENTS = 2; // x y

	private static final int EDGE_FLAG_INTERIOR = 1 << 0;

	public static PolygonMesh meshForConvexPolygon(Polygon p) {
		PolygonMesh m = new PolygonMesh(true);
		m.setConvexPolygon(p);
		return m;
	}

	public static PolygonMesh meshForSimplePolygon(Polygon p) {
		PolygonMesh m = new PolygonMesh(false);
		m.setPolygon(p);
		return m;
	}

	/**
	 * Determine if the compiled triangle sets represent strips or fans
	 */
	public boolean usesStrips() {
		return !mFanFlag;
	}

	/**
	 * Get the list of compiled triangle sets
	 */
	public ArrayList<CompiledTriangleSet> triangleSets() {
		return mTriangleSets;
	}

	/**
	 * Get any GeometryException that was produced during the triangulation /
	 * strip extraction process
	 */
	public GeometryException getError() {
		return mException;
	}

	private PolygonMesh(boolean usesFan) {
		mFanFlag = usesFan;
	}

	/**
	 * Set convex polygon as source
	 */
	private void setConvexPolygon(Polygon p) {
		for (int i = 0; i < p.numVertices(); i++) {
			mFloatArray.add(p.vertex(i));
		}
		compileTriangleSet();
		cleanUpConstructionResources();
	}

	/**
	 * Construct a CompiledTriangleSet. Reads points from the float array, adds
	 * the compiled triangle set to our list, then clears the float array
	 */
	private void compileTriangleSet() {
		mTriangleSets.add(new CompiledTriangleSet(mFloatArray.asFloatBuffer(),
				mFloatArray.size() / VERTEX_COMPONENTS));
		mFloatArray.clear();
	}

	/**
	 * Set arbitrary (simple, but possibly nonconvex) polygon as source;
	 * triangulate it and extract polygon strips
	 */
	private void setPolygon(Polygon polygon) {
		try {
			mContext = new GeometryContext(1965);
			triangulatePolygon(polygon);
			extractStrips();
		} catch (GeometryException e) {
			warning("caught: " + e);
			mException = e;
		}
		cleanUpConstructionResources();
	}

	/**
	 * Throw away any resources that are no longer needed once the mesh has been
	 * constructed
	 */
	private void cleanUpConstructionResources() {
		mFloatArray = null;
		mContext = null;
		mInteriorEdgeStack = null;
	}

	private void triangulatePolygon(Polygon polygon) {
		PolygonTriangulator t = PolygonTriangulator.triangulator(mContext,
				polygon);
		t.triangulate();
	}

	private void extractStrips() {
		mTriangleSets.clear();
		mTrianglesExtracted = 0;

		findInteriorEdges();
		if (mInteriorEdgeCount % 3 != 0)
			GeometryException
					.raise("unexpected number of interior edges found: "
							+ mInteriorEdgeCount);

		mTrianglesExpected = mInteriorEdgeCount / 3;

		// Note: I originally did two passes, the first starting only at polygon
		// edges, to see if this reduced the strip count. It doesn't have much
		// of an effect.

		for (Edge edge : mContext.edgeBuffer()) {
			if (edge.visited())
				continue;
			if (!markedAsInteriorEdge(edge))
				continue;

			// Start a strip with the triangle to the left of this edge
			buildTriangleStrip(edge);
		}

		if (mTrianglesExtracted != mTrianglesExpected)
			GeometryException
					.raise("unexpected number of triangles generated for strips");
	}

	/**
	 * Build a triangle strip. Throws exception if the number of triangles
	 * extracted ever exceeds the expected number
	 * 
	 * @param baseEdge
	 *            edge with first triangle to its left
	 */
	private void buildTriangleStrip(Edge baseEdge) {
		mFloatArray.add(baseEdge.sourceVertex().point());
		mFloatArray.add(baseEdge.destVertex().point());
		boolean parity = false;

		while (true) {
			baseEdge.setVisited(true);
			mTrianglesExtracted++;
			if (mTrianglesExtracted > mTrianglesExpected)
				GeometryException
						.raise("too many triangles generated for strips");

			Edge edge2 = nextEdgeInTriangle(baseEdge);
			Edge edge3 = prevEdgeInTriangle(baseEdge);

			mFloatArray.add(edge2.destVertex().point());
			edge2.setVisited(true);
			edge3.setVisited(true);

			// See if we can extend this strip further
			Edge nextEdge = parity ? edge3 : edge2;
			nextEdge = nextEdge.dual();

			// If edge is not interior, or is already part of an earlier strip,
			// no.
			if (nextEdge.visited() || !nextEdge.hasFlags(EDGE_FLAG_INTERIOR))
				break;

			parity ^= true;
			baseEdge = nextEdge;
		}
		compileTriangleSet();
	}

	/**
	 * Mark an edge as an interior edge, and push it onto the search stack; also
	 * increment the number of interior edges found
	 * 
	 * @param edge
	 */
	private void stackInteriorEdge(Edge edge) {
		mInteriorEdgeStack.add(edge);
		edge.addFlags(EDGE_FLAG_INTERIOR);
		mInteriorEdgeCount++;
	}

	/**
	 * Determine if this edge has been identified as an interior one
	 */
	private static boolean markedAsInteriorEdge(Edge edge) {
		return edge.hasFlags(EDGE_FLAG_INTERIOR);
	}

	private Edge popInteriorEdge() {
		return mInteriorEdgeStack.remove(mInteriorEdgeStack.size() - 1);
	}

	/**
	 * Mark all interior edges within mesh (i.e. edges whose area to their left
	 * lies inside the polygon). Also clears all the edge visited flags.
	 * 
	 * We perform a flood fill from polygon edges. It's slightly complicated by
	 * the fact that we don't have a data structure for faces.
	 * 
	 */
	private void findInteriorEdges() {

		mInteriorEdgeCount = 0;
		mInteriorEdgeStack.clear();

		mContext.clearMeshFlags(0, Edge.FLAG_VISITED | EDGE_FLAG_INTERIOR);

		for (Edge edge : mContext.edgeBuffer()) {
			if (markedAsInteriorEdge(edge))
				continue;

			if (!edge.isPolygon())
				continue;

			stackInteriorEdge(edge);

			while (!mInteriorEdgeStack.isEmpty()) {
				edge = popInteriorEdge();

				for (int pass = 0; pass < 2; pass++) {

					Edge edge2 = (pass == 0) ? nextEdgeInTriangle(edge)
							: prevEdgeInTriangle(edge);
					if (markedAsInteriorEdge(edge2))
						continue;

					stackInteriorEdge(edge2);

					// If this is an interior edge, add dual edge if not already
					// marked as interior edge
					if (edge2.isPolygon())
						continue;
					Edge dual = edge2.dual();
					if (markedAsInteriorEdge(dual))
						continue;
					stackInteriorEdge(dual);
				}
			}
		}
	}

	private static Edge nextEdgeInTriangle(Edge edge) {
		return edge.dual().prevEdge();
	}

	private static Edge prevEdgeInTriangle(Edge edge) {
		return edge.nextEdge().dual();
	}

	private final boolean mFanFlag;
	private final ArrayList<CompiledTriangleSet> mTriangleSets = new ArrayList();
	private GeometryException mException;
	private FloatArray mFloatArray = new FloatArray();

	// These are used only during the triangle strip construction process:
	private int mTrianglesExtracted;
	private GeometryContext mContext;
	private ArrayList<Edge> mInteriorEdgeStack = new ArrayList();
	private int mInteriorEdgeCount;
	private int mTrianglesExpected;
}
