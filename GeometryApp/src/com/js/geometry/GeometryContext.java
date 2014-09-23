package com.js.geometry;

import java.util.ArrayList;
import java.util.Random;
import static com.js.basic.Tools.*;

public class GeometryContext {

	public static final float PSEUDO_ANGLE_RANGE = 8;
	public static final float PSEUDO_ANGLE_RANGE_12 = (PSEUDO_ANGLE_RANGE * .5f);
	public static final float PSEUDO_ANGLE_RANGE_14 = (PSEUDO_ANGLE_RANGE * .25f);
	public static final float PSEUDO_ANGLE_RANGE_34 = (PSEUDO_ANGLE_RANGE * .75f);
	public static final float PERTURB_AMOUNT_DEFAULT = .5f;

	public Random random() {
		return mRandom;
	}

	public ArrayList<Vertex> vertexBuffer() {
		return mVertexBuffer;
	}

	/**
	 * Construct a list of all the edges
	 * 
	 * @return array of edges
	 */
	public ArrayList<Edge> constructListOfEdges() {
		return constructListOfEdges(false);
	}

	/**
	 * Construct a list of all the edges
	 * 
	 * @param omitDuals
	 *            if true, exactly one of an edge or its dual will appear in the
	 *            array
	 * @return array of edges
	 */
	public ArrayList<Edge> constructListOfEdges(boolean omitDuals) {
		ArrayList<Edge> edges = new ArrayList();
		for (Vertex vertex : mVertexBuffer) {
			Edge edge = vertex.edges();
			if (edge == null)
				continue;
			while (true) {
				if (!omitDuals || edge.angle() >= 0)
					edges.add(edge);
				edge = edge.nextEdge();
				if (edge == vertex.edges())
					break;
			}
		}
		return edges;
	}

	public int seed() {
		return mSeed;
	}

	public float perturbAmount() {
		return mPerturbAmount;
	}

	public static GeometryContext contextWithRandomSeed(int seed) {
		return new GeometryContext(seed);
	}

	public GeometryContext(int seed) {
		resetWithSeed(seed);
		mPerturbAmount = PERTURB_AMOUNT_DEFAULT;
	}

	public void resetWithSeed(int seed) {
		mSeed = seed;
		resetRandom(mSeed);
		clearMesh();
	}

	public void resetRandom(int seed) {
		if (seed == 0)
			mRandom = new Random();
		else
			mRandom = new Random(seed);
	}

	public boolean checkError(boolean errorFlag) {
		return errorFlag;
	}

	/**
	 * Test if a value is essentially zero, and raise exception if so
	 * 
	 * @param value
	 */
	public void testForZero(float value) {
		testForZero(value, 1e-8f);
	}

	/**
	 * Test if a value is essentially zero, and raise exception if so
	 * 
	 * @param value
	 * @param epsilon
	 */
	public void testForZero(float value, float epsilon) {
		if (checkError(Math.abs(value) <= epsilon)) {
			GeometryException.raise("Value is very near zero: " + value
					+ " (epsilon " + epsilon + ")");
		}
	}

	/**
	 * Raise exception if reference is null
	 * 
	 * @param ptr
	 */
	public void testForNil(Object ptr) {
		if (ptr == null)
			GeometryException.raise("Object is null");
	}

	/**
	 * Raise exception if value's magnitude exceeds MAXVALUE
	 * 
	 * @param value
	 */
	public void testForOverflow(float value) {
		if (checkError(value > MyMath.MAXVALUE || value < -MyMath.MAXVALUE
				|| Float.isNaN(value))) {
			GeometryException.raise("Value has overflowed: " + value);
		}
	}

	public float polarAngleOfSegment(Point s1, Point s2) {
		return polarAngle(s2.x - s1.x, s2.y - s1.y);
	}

	public float polarAngle(Point ray) {
		return polarAngle(ray.x, ray.y);
	}

	public float polarAngle(float x, float y) {
		float max = Math.max(Math.abs(x), Math.abs(y));
		if (checkError(max <= 1e-8f)) {
			GeometryException.raise("Point is too close to origin: " + x + ","
					+ y);
		}
		return (float) Math.atan2(y, x);
	}

	public float pseudoPolarAngle(Point point) {
		return pseudoPolarAngle(point.x, point.y);
	}

	public float pseudoPolarAngle(float x, float y) {
		// For consistency, always insist that y is nonnegative
		boolean negateFlag = (y <= 0);
		if (negateFlag)
			y = -y;

		float ret;
		if (y > Math.abs(x)) {
			float rat = x / y;
			ret = PSEUDO_ANGLE_RANGE_14 - rat;
		} else {
			testForZero(x);
			float rat = y / x;
			if (x < 0) {
				ret = PSEUDO_ANGLE_RANGE_12 + rat;
			} else {
				ret = rat;
			}
		}
		if (negateFlag)
			ret = -ret;

		return ret;
	}

	public float pseudoPolarAngleOfSegment(Point s1, Point s2) {
		return pseudoPolarAngle(s2.x - s1.x, s2.y - s1.y);
	}

	public float normalizePseudoAngle(float a) {
		float b = a;
		if (b < -PSEUDO_ANGLE_RANGE_12) {
			b += PSEUDO_ANGLE_RANGE;

			if (checkError((b < -PSEUDO_ANGLE_RANGE_12))) {
				GeometryException.raise("Cannot normalize " + a);
			}
		} else if (b >= PSEUDO_ANGLE_RANGE_12) {
			b -= PSEUDO_ANGLE_RANGE;
			if (checkError((b >= PSEUDO_ANGLE_RANGE_12))) {
				GeometryException.raise("Cannot normalize " + a);
			}
		}
		return b;
	}

	public boolean pseudoAngleIsConvex(float angle) {
		return angle > 0;
	}

	public boolean pseudoAngleIsConvex(float startAngle, float endAngle) {
		return pseudoAngleIsConvex(normalizePseudoAngle(endAngle - startAngle));
	}

	public float pointUnitLineSignedDistance(Point pt, Point s1, Point s2) {
		// Translate so s1 is at origin
		float sx = s2.x - s1.x;
		float sy = s2.y - s1.y;
		float pt_x = pt.x - s1.x;
		float pt_y = pt.y - s1.y;
		return -sy * pt_x + sx * pt_y;
	}

	public float perturb(float val) {
		float GRID = mPerturbAmount;
		float NOISE = GRID * .8f;

		float aligned = (float) Math.floor((val / GRID) + .5f);
		float frac = mRandom.nextFloat() * (2 * NOISE) - NOISE;
		float ret = (aligned + frac) * GRID;

		return ret;
	}

	public void perturb(Point pt) {
		pt.x = perturb(pt.x);
		pt.y = perturb(pt.y);
	}

	public void clearMesh() {
		mVertexBuffer.clear();
	}

	public void clearMeshFlags(int vertexFlags, int edgeFlags) {
		if (vertexFlags != 0) {
			for (Vertex v : mVertexBuffer) {
				v.clearFlags(vertexFlags);
			}
		}
		if (edgeFlags != 0) {
			for (Edge edge : constructListOfEdges(true)) {
				edge.clearFlags(edgeFlags);
				edge.dual().clearFlags(edgeFlags);
			}
		}
	}

	public Edge addEdge(Vertex v0, Vertex v1) {
		return addEdge(v0, v1, false);
	}

	public Edge addEdge(Vertex v0, Vertex v1, boolean ifNoEdgeExists) {
		if (ifNoEdgeExists) {
			Edge existing = edgeExistsBetween(v0, v1);
			if (existing != null)
				return existing;
		}
		Edge edge = new Edge();
		Edge dual = new Edge();
		edge.setDual(dual);
		dual.setDual(edge);

		Point delta = new Point(v1.point().x - v0.point().x, v1.point().y
				- v0.point().y);
		float angle = pseudoPolarAngle(delta);

		edge.setAngle(angle);
		edge.setDestVertex(v1);

		dual.setAngle(normalizePseudoAngle(angle + PSEUDO_ANGLE_RANGE_12));
		dual.setDestVertex(v0);

		addEdgeToVertex(edge, v0);
		addEdgeToVertex(dual, v1);

		return edge;
	}

	public Edge edgeExistsBetween(Vertex sourceVert, Vertex destVert) {
		Edge foundEdge = null;
		Edge edge = sourceVert.edges();
		if (edge != null) {

			while (true) {
				if (edge.destVertex() == destVert) {
					foundEdge = edge;
					break;
				}
				edge = edge.nextEdge();
				if (edge == sourceVert.edges())
					break;
			}
		}
		return foundEdge;
	}

	public Edge polygonEdgeFromVertex(Vertex v) {
		Edge output = null;
		do {
			Edge edge = v.edges();
			if (edge == null)
				break;
			if (edge.isPolygon()) {
				output = edge;
				break;
			}
			edge = edge.nextEdge();
			if (edge.isPolygon()) {
				output = edge;
			}
		} while (false);
		if (checkError(output == null)) {
			GeometryException
					.raise("cannot find polygonal edge leaving vertex " + v);
		}
		return output;
	}

	public Edge polygonEdgeToVertex(Vertex v) {
		Edge output = null;
		do {
			Edge edge = v.edges();
			if (edge == null)
				break;
			if (edge.isPolygon()) {
				edge = edge.nextEdge();
			}
			edge = edge.dual();
			if (edge.isPolygon()) {
				output = edge;
			}
		} while (false);
		if (checkError(output == null)) {
			GeometryException
					.raise("cannot find polygonal edge entering vertex " + v);
		}
		return output;
	}

	public Vertex vertex(int index) {
		return mVertexBuffer.get(index);
	}

	public Vertex addVertex(Point location) {
		testForOverflow(location.x);
		testForOverflow(location.y);
		Vertex v = new Vertex(location);
		mVertexBuffer.add(v);
		return v;
	}

	public Vertex addVertex(Vertex vertex, Point location) {
		if (vertex == null) {
			vertex = addVertex(location);
		} else {
			ASSERT(vertex.deleted());
			vertex.clearFlags();
			vertex.setLocation(location);
		}
		return vertex;
	}

	public void addEdgeToVertex(Edge edge, Vertex vertex) {
		if (vertex.edges() == null) {
			vertex.setEdges(edge);
			edge.setNextEdge(edge);
			edge.setPrevEdge(edge);
		} else {
			// Find insertion point for edge
			Edge existingEdge = vertex.edges();
			// Look for the existing edge that will immediately preceded this
			// one
			while (true) {
				float diff = edge.angle() - existingEdge.angle();

				if (diff < 0) {
					existingEdge = existingEdge.prevEdge();
					break;
				} else {
					Edge nextEdge = existingEdge.nextEdge();
					if (nextEdge == vertex.edges()) {
						break;
					}
					existingEdge = nextEdge;
				}
			}

			// It's a degeneracy if the angle between the new edge and its
			// neighbors is too close to zero or PI/2 ...
			Edge degenerateEdge = null;
			if (checkError(angleDifferenceIsDegenerate(edge.angle()
					- existingEdge.angle()))) {
				degenerateEdge = existingEdge;
			}
			Edge followingEdge = existingEdge.nextEdge();
			if (followingEdge != existingEdge) {
				if (checkError(angleDifferenceIsDegenerate(followingEdge
						.angle() - edge.angle()))) {
					degenerateEdge = followingEdge;
				}
			}

			if (degenerateEdge != null) {
				if (degenerateEdge.destVertex() == vertex) {
					GeometryException.raise("edge already exists: "
							+ degenerateEdge);
				} else {
					GeometryException.raise("edges collinear: " + edge + " "
							+ degenerateEdge + " " + edge.angle() + " "
							+ degenerateEdge.angle());
				}
			}

			insertEdgeAfter(edge, existingEdge);
			if (edge.angle() < existingEdge.angle()) {
				vertex.setEdges(edge);
			}
		}
	}

	private float mParameter;

	public float getParameter() {
		return mParameter;
	}

	public Point segHorzLineIntersection(Point pt1, Point pt2, float yLine) {
		Point ipt = null;

		float denom = pt2.y - pt1.y;
		testForZero(denom);

		float numer = yLine - pt1.y;
		float t = numer / denom;

		if (!(t < 0 || t > 1)) {
			mParameter = t;

			ipt = new Point(pt1.x + (pt2.x - pt1.x) * t, pt1.y + denom * t);
		}
		return ipt;
	}

	private void insertEdgeAfter(Edge newEdge, Edge previousEdge) {
		Edge follower = previousEdge.nextEdge();
		previousEdge.setNextEdge(newEdge);
		newEdge.setPrevEdge(previousEdge);
		newEdge.setNextEdge(follower);
		follower.setPrevEdge(newEdge);
	}

	private boolean angleDifferenceIsDegenerate(float angleDiff) {
		final float kMinSeparation = 1e-7f;
		angleDiff = Math.abs(normalizePseudoAngle(angleDiff));
		return (angleDiff <= kMinSeparation || angleDiff >= PSEUDO_ANGLE_RANGE_12
				- kMinSeparation);
	}

	public String dumpMesh() {
		return dumpMesh(true, false);
	}

	public String dumpMesh(boolean withVertexLocations, boolean withVertexNames) {
		StringBuilder sb = new StringBuilder("GeometryContext vertices:\n");
		for (Vertex v : mVertexBuffer) {
			sb.append(" ");
			Point source = v.point();
			if (withVertexNames) {
				sb.append(nameOf(source, false));
				sb.append(' ');
			}
			if (withVertexLocations) {
				sb.append(source.toStringAsInts());
			}

			Edge e = v.edges();
			if (e != null) {
				sb.append(" -->");
				while (true) {
					Point dest = e.destVertex().point();
					if (withVertexNames) {
						sb.append(nameOf(dest, false));
						sb.append(' ');
					}
					if (withVertexLocations) {
						sb.append(dest.toStringAsInts());
					}
					e = e.nextEdge();
					if (e == v.edges())
						break;
					sb.append("   ");
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	private Random mRandom;
	private int mSeed;
	private float mPerturbAmount;
	private ArrayList<Vertex> mVertexBuffer = new ArrayList();
}
