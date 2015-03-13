package com.js.geometry;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.js.basic.GeometryException;
import com.js.basic.MyMath;
import com.js.basic.Point;

import static com.js.basic.Tools.*;

/**
 * Note: this class can be easily adapted to generate triangle strips for
 * arbitrary (planar) meshes within Meshes, so long as certain edges are flagged
 * as polygon edges.
 */
public class PolygonMesh {

	public static final int TYPE_TRIANGLES = 0;
	public static final int TYPE_FANS = 1;
	public static final int TYPE_STRIPS = 2;

	// For investigating issue #34, strip efficiency
	// Dumps vertices to output as XYZ(GAP)ABC...
	private static final boolean DUMP_STRIP = false;

	// Warps vertices to emphasize triangle boundaries
	private static final boolean EMPHASIZE_INDIVIDUAL_TRIANGLES = false;

	/**
	 * The number of floats per vertex
	 */
	public static final int VERTEX_COMPONENTS = 2; // x y

	private static final int EDGE_FLAG_INTERIOR = 1 << 0;

	/**
	 * Compile a TYPE_FANS mesh from a convex polygon
	 */
	public static PolygonMesh meshForConvexPolygon(Polygon p) {
		PolygonMesh m = new PolygonMesh();
		m.compileConvexPolygonIntoFan(p);
		return m;
	}

	/**
	 * Compile a mesh of TYPE_TRIANGLES from a polygon
	 */
	public static PolygonMesh meshForPolygon(Polygon polygon) {
		return meshForPolygon(polygon, false);
	}

	/**
	 * Compile a mesh from a polygon
	 * 
	 * @param polygon
	 *            polygon
	 * @param useStrips
	 *            if true, generates mesh of TYPE_STRIPS; else, TYPE_TRIANGLES
	 */
	public static PolygonMesh meshForPolygon(Polygon polygon, boolean useStrips) {
		if (EMPHASIZE_INDIVIDUAL_TRIANGLES)
			warning("contracting strip vertices for demonstration purposes");
		PolygonMesh m = new PolygonMesh();
		if (useStrips)
			m.compilePolygonIntoStrips(polygon);
		else
			m.compilePolygonIntoTriangles(polygon);
		return m;
	}

	/**
	 * Determine what type of mesh this is
	 * 
	 * @return TYPE_xxx
	 */
	public int type() {
		return mType;
	}

	/**
	 * Get the compiled vertices
	 */
	public FloatBuffer getVertexBuffer() {
		return mTriangleSet;
	}

	/**
	 * Get the number of compiled vertices
	 */
	public int getVertexCount() {
		return mTriangleSet.capacity() / 2;
	}

	/**
	 * Get any GeometryException that was produced during the triangulation /
	 * strip extraction process
	 */
	public GeometryException getError() {
		return mException;
	}

	/**
	 * Set convex polygon as source
	 */
	private void compileConvexPolygonIntoFan(Polygon p) {
		mType = TYPE_FANS;
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
		mTriangleSet = mFloatArray.asFloatBuffer();
	}

	/**
	 * Set arbitrary (simple, but possibly nonconvex) polygon as source;
	 * triangulate it extract individual polygons
	 */
	private void compilePolygonIntoTriangles(Polygon polygon) {
		mType = TYPE_TRIANGLES;
		try {
			mMesh = new Mesh();
			triangulatePolygon(polygon);
			extractTriangles();
		} catch (GeometryException e) {
			warning("caught: " + e);
			mException = e;
		}
		cleanUpConstructionResources();
	}

	/**
	 * Set arbitrary (simple, but possibly nonconvex) polygon as source;
	 * triangulate it and extract polygon strips
	 */
	private void compilePolygonIntoStrips(Polygon polygon) {
		mType = TYPE_STRIPS;
		try {
			mMesh = new Mesh();
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
		mMeshEdges = null;
		mMesh = null;
		mInteriorEdgeStack = null;
	}

	private void triangulatePolygon(Polygon polygon) {
		PolygonTriangulator t = PolygonTriangulator.triangulator(null, mMesh,
				polygon);
		t.triangulate();
	}

	/**
	 * Move first point slightly towards a second
	 * 
	 * @return new version of first point
	 */
  private static Point inset(Point a, Point b) {
		ASSERT(EMPHASIZE_INDIVIDUAL_TRIANGLES);
		float dist = MyMath.distanceBetween(a, b);
		float factor = .1f;
		int pixels = 25;
		if (dist > pixels) {
			factor = pixels / dist;
		}
		return MyMath.interpolateBetween(a, b, factor);
	}

	private void extractTriangles() {
		mMeshEdges = mMesh.constructListOfEdges();
		findInteriorEdges();

		for (Edge edge : mMeshEdges) {
			// If this edge was included in a previous triangle, skip
			if (edge.visited())
				continue;
			// If the face to its left is not inside the polygon, skip
			if (!markedAsInteriorEdge(edge))
				continue;

			Edge abEdge = edge;
			Edge bcEdge = abEdge.nextFaceEdge();
			Edge caEdge = bcEdge.nextFaceEdge();
			bcEdge.setVisited(true);
			caEdge.setVisited(true);

			Point pa = caEdge.destVertex();
			Point pb = abEdge.destVertex();
			Point pc = bcEdge.destVertex();

			if (EMPHASIZE_INDIVIDUAL_TRIANGLES) {
				Point centroid = new Point(pa);
				centroid.add(pb);
				centroid.add(pc);
				centroid.setTo(centroid.x / 3, centroid.y / 3);
				pa = inset(pa, centroid);
				pb = inset(pb, centroid);
				pc = inset(pc, centroid);
			}

			mFloatArray.add(pa);
			mFloatArray.add(pb);
			mFloatArray.add(pc);
		}
		int trianglesFound = bufferedVertexCount() / 3;
		if (trianglesFound != mTrianglesExpected) {
			warning("expected " + mTrianglesExpected + " but got "
					+ trianglesFound);
		}

		compileTriangleSet();
		cleanUpConstructionResources();
	}

	private void extractStrip() {
		mMeshEdges = mMesh.constructListOfEdges();
		findInteriorEdges();

		if (DUMP_STRIP)
			prr("Strip: ");
		for (Edge edge : mMeshEdges) {
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
			firstPoint = baseEdge.sourceVertex();
			secondPoint = baseEdge.destVertex();

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
				Edge nextBaseEdge = baseEdge.nextFaceEdge();
				nextBaseEdge.setVisited(true);
				Edge edge3 = baseEdge.prevFaceEdge();
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
			Point point = baseEdge.destVertex();
			if (EMPHASIZE_INDIVIDUAL_TRIANGLES) {
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
		float[] array = mFloatArray.array(false);
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
		return pop(mInteriorEdgeStack);
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
		mInteriorEdgeStack = new ArrayList();

		for (Edge edge : mMeshEdges) {
			if (markedAsInteriorEdge(edge))
				continue;

			if (!edge.isPolygon())
				continue;

			stackInteriorEdge(edge);

			while (!mInteriorEdgeStack.isEmpty()) {
				edge = popInteriorEdge();

				for (int pass = 0; pass < 2; pass++) {

					Edge edge2 = (pass == 0) ? edge.nextFaceEdge() : edge
							.prevFaceEdge();
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

		if (mInteriorEdgeCount % 3 != 0)
			GeometryException
					.raise("unexpected number of interior edges found: "
							+ mInteriorEdgeCount);

		mTrianglesExpected = mInteriorEdgeCount / 3;
	}

	private static Edge nextEdgeInCWTriangle(Edge edge) {
		return edge.dual().nextEdge();
	}

	private static Edge prevEdgeInCWTriangle(Edge edge) {
		return edge.prevEdge().dual();
	}

	private int mType;
	private FloatBuffer mTriangleSet;
	private GeometryException mException;

	// These are used only during the mesh construction process:
	private FloatArray mFloatArray = new FloatArray();
	private ArrayList<Edge> mMeshEdges;
	private int mTrianglesExtracted;
	private Mesh mMesh;
	private ArrayList<Edge> mInteriorEdgeStack;
	private int mInteriorEdgeCount;
	private int mTrianglesExpected;
	private Point mLastVertexGenerated;
}
