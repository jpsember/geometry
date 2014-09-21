package com.js.geometryapp;

import java.util.ArrayList;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
import com.js.geometry.GeometryException;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;

import static com.js.basic.Tools.*;

/**
 * Note: this class can be easily adapted to generate triangle strips for
 * arbitrary (planar) meshes within GeometryContexts, so long as certain edges
 * are flagged as polygon edges.
 */
public class PolygonMesh {

	// For investigating issue #34, strip efficiency
	// Dumps vertices to output as XYZ(GAP)ABC...
	private static final boolean DUMP_STRIP = true;

	// Warps vertices to emphasize strip boundaries
	private static final boolean CONTRACT_STRIP_VERTICES = true;

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
	 * Determine if the compiled triangle set represents a strip or a fan
	 */
	public boolean usesStrip() {
		return !mFanFlag;
	}

	/**
	 * Get the compiled triangle set
	 */
	public CompiledTriangleSet triangleSet() {
		return mTriangleSet;
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
	 * Get the number of vertices within mFloatArray
	 */
	private int bufferedVertexCount() {
		return mFloatArray.size() / VERTEX_COMPONENTS;
	}

	/**
	 * Construct a CompiledTriangleSet. Reads points from the float array, adds
	 * the compiled triangle set to our list, then clears the float array
	 */
	private void compileTriangleSet() {
		mTriangleSet = new CompiledTriangleSet(mFloatArray.asFloatBuffer(),
				bufferedVertexCount());
	}

	/**
	 * Set arbitrary (simple, but possibly nonconvex) polygon as source;
	 * triangulate it and extract polygon strips
	 */
	private void setPolygon(Polygon polygon) {
		try {
			mContext = new GeometryContext(1965);
			triangulatePolygon(polygon);
			extractStrip();
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

	private void extractStrip() {
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

		if (CONTRACT_STRIP_VERTICES)
			warning("contracting strip vertices for demonstration purposes");

		if (DUMP_STRIP)
			prr("Strip: ");
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
		if (DUMP_STRIP) {
			pr("");
			int vertices = bufferedVertexCount();
			pr("triangles=" + mTrianglesExtracted);
			pr(" vertices=" + vertices);
			int max = mTrianglesExtracted * 3;
			pr("  maximum=" + max);
		}

		compileTriangleSet();
	}

	private boolean stripParity() {
		return bufferedVertexCount() % 1 != 0;
	}

	private void addPointToStrip(Point point) {
		if (DUMP_STRIP)
			prr(nameOf(point, false).substring(0, 1));
		mFloatArray.add(point);
	}

	/**
	 * Build a triangle strip. Throws exception if the number of triangles
	 * extracted ever exceeds the expected number. Extends a previous strip, if
	 * necessary, by adding duplicate vertices to produce degenerate triangles
	 * 
	 * @param ccwBaseEdge
	 *            edge with first triangle to its left
	 */
	private void buildTriangleStrip(Edge ccwBaseEdge) {

		// For simplicity, we maintain both the ccwBaseEdge, and its the
		// corresponding edge that alternates between ccw and cw orientations.

		Edge baseEdge = ccwBaseEdge;
		if (stripParity())
			baseEdge = ccwBaseEdge.dual();

		{
			Point firstPoint, secondPoint;
			firstPoint = baseEdge.sourceVertex().point();
			secondPoint = baseEdge.destVertex().point();

			// If we're continuing a previous strip, add degenerate
			// triangles to bridge the gap
			if (mLastVertexGenerated != null) {
				if (DUMP_STRIP)
					prr("(");
				// Special case if last vertex generated equals new start vertex
				if (mLastVertexGenerated != firstPoint) {
					addPointToStrip(mLastVertexGenerated);
				}
				addPointToStrip(firstPoint);
				if (DUMP_STRIP)
					prr(")");
			}
			addPointToStrip(firstPoint);
			addPointToStrip(secondPoint);
		}

		while (true) {
			// If edge is not interior, or is already part of an earlier strip,
			// stop the strip
			if (ccwBaseEdge.visited()
					|| !ccwBaseEdge.hasFlags(EDGE_FLAG_INTERIOR))
				break;
			ccwBaseEdge.setVisited(true);

			mTrianglesExtracted++;
			if (mTrianglesExtracted > mTrianglesExpected)
				GeometryException
						.raise("too many triangles generated for strips");

			if (!stripParity()) {
				Edge nextBaseEdge = nextEdgeInTriangle(baseEdge);
				nextBaseEdge.setVisited(true);
				Edge edge3 = prevEdgeInTriangle(baseEdge);
				edge3.setVisited(true);
				ccwBaseEdge = nextBaseEdge;
				baseEdge = nextBaseEdge;
			} else {
				Edge nextBaseEdge = nextEdgeInCWTriangle(baseEdge);
				nextBaseEdge.dual().setVisited(true);
				Edge edge3 = prevEdgeInCWTriangle(baseEdge);
				edge3.dual().setVisited(true);
				ccwBaseEdge = nextBaseEdge.dual();
				baseEdge = nextBaseEdge;
			}
			Point point = baseEdge.destVertex().point();
			if (CONTRACT_STRIP_VERTICES) {
				Point p1 = peekLastPoint(2);
				Point p2 = peekLastPoint(1);
				Point mid = MyMath.interpolateBetween(p1, p2, .5f);
				float distance = MyMath.distanceBetween(mid, point);
				float distance2 = Math.max(0, distance - 30);
				point = MyMath.interpolateBetween(mid, point, distance2
						/ distance);
			}
			addPointToStrip(point);
			mLastVertexGenerated = point;
		}
	}

	/**
	 * Get one of the last points added to mFloatArray
	 * 
	 * @param distanceFromEnd
	 *            1:last point; 2:second to last; etc
	 * @return Point
	 */
	private Point peekLastPoint(int distanceFromEnd) {
		int position = mFloatArray.size() - distanceFromEnd * VERTEX_COMPONENTS;
		float[] array = mFloatArray.array();
		return new Point(array[position], array[position + 1]);
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

	private static Edge nextEdgeInCWTriangle(Edge edge) {
		return edge.dual().nextEdge();
	}

	private static Edge prevEdgeInCWTriangle(Edge edge) {
		return edge.prevEdge().dual();
	}

	private final boolean mFanFlag;
	private CompiledTriangleSet mTriangleSet;
	private GeometryException mException;
	private FloatArray mFloatArray = new FloatArray();

	// These are used only during the triangle strip construction process:
	private int mTrianglesExtracted;
	private GeometryContext mContext;
	private ArrayList<Edge> mInteriorEdgeStack = new ArrayList();
	private int mInteriorEdgeCount;
	private int mTrianglesExpected;
	private Point mLastVertexGenerated;
}
