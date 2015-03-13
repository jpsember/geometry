package com.js.geometry;

import static com.js.basic.Tools.*;
import static com.js.basic.MyMath.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.TreeSet;

import com.js.basic.GeometryException;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.basic.Rect;
import com.js.geometryapp.RenderTools;

import android.graphics.Color;

/**
 * Triangulates a polygon.
 * 
 * Polygon must be CCW oriented.
 * 
 */
public class PolygonTriangulator {

	public static final String DETAIL_TRIANGULATE_MONOTONE_FACE = "Triangulate monotone face";

	public static PolygonTriangulator triangulator(AlgorithmStepper stepper,
			Mesh context, Polygon polygon) {
		return new PolygonTriangulator(stepper, context, polygon);
	}

	private PolygonTriangulator(AlgorithmStepper stepper, Mesh mesh,
			Polygon polygon) {
		if (stepper == null)
			stepper = AlgorithmStepper.INACTIVE_STEPPER;
		s = stepper;
		mMesh = mesh;
		mPolygon = polygon;
		ASSERT(polygon.isCCW(mesh));
		mMonotoneQueue = new ArrayDeque();
		mVertexList = new ArrayList();
	}

	// Vertex flags internal to this algorithm
	private static final int VERTEXFLAG_MERGE = 1 << 0;
	private static final int VERTEXFLAG_LEFTSIDE = 1 << 1;

	// Characterizations of each polygon vertex.
	// Derived from http://www.cs.uu.nl/docs/vakken/ga/slides3.pdf, but
	// with two versions of REGULAR depending upon orientation of the boundary
	private static final int VTYPE_START = 0;
	private static final int VTYPE_REGULAR_UP = 1;
	private static final int VTYPE_REGULAR_DOWN = 2;
	private static final int VTYPE_END = 3;
	private static final int VTYPE_SPLIT = 4;
	private static final int VTYPE_MERGE = 5;

	private static final String BGND_ELEMENT_POLYGON_FILLED = "10";
	private static final String BGND_ELEMENT_POLYGON_OUTLINE = "11";
	private static final String BGND_ELEMENT_MONOTONE_FACE = "15";
	private static final String BGND_ELEMENT_SWEEPSTATUS = "20";
	private static final String BGND_ELEMENT_MESH = "00:mesh";

	public void triangulate() {
		s.addLayer(
				BGND_ELEMENT_POLYGON_FILLED,
				s.colored(Color.argb(0x40, 0x80, 0x80, 0x80),
						mPolygon.renderable(true)));
		s.addLayer(BGND_ELEMENT_POLYGON_OUTLINE, mPolygon);
		s.addLayer(BGND_ELEMENT_MESH, mMesh);

		if (s.bigStep())
			s.show("Triangulating polygon");

		mPolygonMeshBase = mPolygon.embed(mMesh);
		createEventList();
		createSweepStatus();

		for (Vertex v : mVertexEvents) {
			processVertexEvent(v);
		}

		if (s.isActive()) {
			s.removeLayer(BGND_ELEMENT_SWEEPSTATUS);
		}

	}

	private void createSweepStatus() {
		mSweepStatus = new TreeSet<SweepEdge>(new Comparator<SweepEdge>() {
			@Override
			public int compare(SweepEdge a, SweepEdge b) {
				Point sa = a.positionOnSweepLine(mSweepLinePosition, mMesh,
						false);
				Point sb = b.positionOnSweepLine(mSweepLinePosition, mMesh,
						false);
				return (int) Math.signum(sa.x - sb.x);
			}
		});
		mSweepLineVisible = false;

		s.addLayer(BGND_ELEMENT_SWEEPSTATUS, new Renderable() {
			@Override
			public void render(AlgorithmStepper s) {
				if (!mSweepLineVisible)
					return;
				s.setColor(RenderTools.COLOR_DARKGREEN);
				Rect r = s.algorithmRect();
				float horizExtent = r.width * .25f;
				s.renderLine(new Point(-horizExtent, mSweepLinePosition),
						new Point(r.width + horizExtent, mSweepLinePosition));
				s.setLineWidth(2);
				for (SweepEdge e : mSweepStatus) {

					// Extrapolate a little above and below the
					// sweep line
					float vertExtent = 22;
					Point p1 = e.positionOnSweepLine(mSweepLinePosition
							- vertExtent * .8f, mMesh, true);
					Point p2 = e.positionOnSweepLine(mSweepLinePosition
							+ vertExtent * 1.2f, mMesh, true);
					s.render(Segment.directed(p1, p2));

					Point pt = e.positionOnSweepLine(mSweepLinePosition, mMesh,
							false);
					s.render(pt);
				}
			}
		});
	}

	private void createEventList() {
		ArrayList<Vertex> array = new ArrayList();
		for (int i = 0; i < mPolygon.numVertices(); i++) {
			Vertex vertex = mMesh.vertex(mPolygonMeshBase + i);
			vertex.clearFlags();
			array.add(vertex);
		}

		Collections.sort(array, new Comparator<Vertex>() {
			@Override
			public int compare(Vertex va, Vertex vb) {
				float diff = va.y - vb.y;
				testForZero(diff);
				return (int) Math.signum(diff);
			}
		});
		mVertexEvents = array;
	}

	private void moveSweepLineTo(float y) {
		mSweepLinePosition = y;
		mSweepLineVisible = true;
	}

	private Edge polygonEdgeLeavingVertex(Vertex vertex) {
		Edge edge = vertex.edges();
		while (true) {
			if (edge.isPolygon())
				return edge;
			edge = edge.nextEdge();
			if (edge == vertex.edges())
				throw new IllegalStateException();
		}
	}

	private void polygonEdgesThroughVertex(Vertex vertex, Edge output[]) {
		Edge edge = polygonEdgeLeavingVertex(vertex);
		output[1] = edge;
		Edge edgeB = edge.nextEdge().dual();
		if (edgeB.destVertex() != vertex)
			throw new IllegalStateException();
		output[0] = edgeB;
	}

	private int vertexType(Vertex v, Edge incoming, Edge outgoing) {
		Point ipt = incoming.dual().destVertex();
		Point opt = outgoing.destVertex();

		int type;
		if (opt.y > v.y) {
			if (ipt.y > v.y) {
				if (pseudoAngleIsConvex(outgoing.angle(), incoming.dual()
						.angle())) {
					type = VTYPE_START;
				} else {
					type = VTYPE_SPLIT;
				}
			} else {
				type = VTYPE_REGULAR_UP;
			}
		} else {
			if (ipt.y < v.y) {
				if (pseudoAngleIsConvex(outgoing.angle(), incoming.dual()
						.angle())) {
					type = VTYPE_END;
				} else {
					type = VTYPE_MERGE;
				}
			} else {
				type = VTYPE_REGULAR_DOWN;
			}
		}
		return type;
	}

	/**
	 * Replace helper, and possibly add edge if old helper was a MERGE
	 */
	private Edge replaceHelperForEdge(SweepEdge sweepEdge, Vertex newHelper) {
		Vertex prevHelper = sweepEdge.helper();
		if (s.step())
			s.show("Replace edge helper", s.highlighted(prevHelper),
					s.highlighted(newHelper), highlighted(sweepEdge));

		Edge newEdge = null;
		if ((prevHelper.flags() & VERTEXFLAG_MERGE) != 0) {
			newEdge = addEdge(newHelper, prevHelper);
			ASSERT(newEdge.angle() < 0);

			// Test if we've formed a new 'end' vertex to either side of the new
			// (downward-facing) edge;
			// if so, triangulate the monotone face that ends at that vertex.

			Edge auxEdge = newEdge.nextEdge();
			if (auxEdge.angle() > newEdge.angle() && auxEdge.angle() < 0) {
				triangulateMonotoneFace(auxEdge.dual());
			}

			auxEdge = newEdge.prevEdge();
			if (auxEdge.angle() < newEdge.angle()) {
				triangulateMonotoneFace(newEdge.dual());
			}
		}
		sweepEdge.setHelper(newHelper);
		return newEdge;
	}

	private SweepEdge findExistingEdge(Edge polygonEdge) {
		SweepEdge sentinel = SweepEdge.edge(polygonEdge, null);
		TreeSet<SweepEdge> tail = (TreeSet<SweepEdge>) mSweepStatus.tailSet(
				sentinel, true);
		SweepEdge found = tail.first();
		ASSERT(found.polygonEdge() == polygonEdge);
		ASSERT(found.helper() != null);
		return found;
	}

	private SweepEdge findEdgeFollowing(Edge polygonEdge) {
		SweepEdge sentinel = SweepEdge.edge(polygonEdge, null);
		TreeSet<SweepEdge> tail = (TreeSet<SweepEdge>) mSweepStatus.tailSet(
				sentinel, false);

		SweepEdge found = tail.first();

		ASSERT(found.helper() != null);
		return found;
	}

	private void processVertexEvent(Vertex vertex) {
		if (s.step())
			s.show("Process vertex event", s.highlighted(vertex));

		moveSweepLineTo(vertex.y);
		Edge edges[] = new Edge[2];
		polygonEdgesThroughVertex(vertex, edges);
		Edge incoming = edges[0];
		Edge outgoing = edges[1];
		int vType = vertexType(vertex, incoming, outgoing);
		Edge newEdge = null;
		Edge delEdge = null;

		switch (vType) {
		default:
			ASSERT(vType == VTYPE_START);
			newEdge = outgoing;
			break;

		case VTYPE_REGULAR_UP:
			newEdge = outgoing;
			delEdge = incoming;
			break;

		case VTYPE_END:
			delEdge = incoming;
			triangulateMonotoneFace(delEdge);
			break;

		case VTYPE_REGULAR_DOWN: {
			SweepEdge se = findEdgeFollowing(outgoing);
			replaceHelperForEdge(se, vertex);
		}
			break;

		case VTYPE_MERGE: {
			vertex.addFlags(VERTEXFLAG_MERGE);
			SweepEdge se = findEdgeFollowing(incoming);
			replaceHelperForEdge(se, vertex);
			delEdge = incoming;
		}
			break;

		case VTYPE_SPLIT: {
			SweepEdge se = findEdgeFollowing(outgoing);
			Vertex oldHelper = se.helper();
			Edge mergeEdge = replaceHelperForEdge(se, vertex);
			if (mergeEdge == null) {
				addEdge(vertex, oldHelper);
			}
			newEdge = outgoing;
		}
			break;
		}

		if (delEdge != null) {
			SweepEdge se = findExistingEdge(delEdge);
			replaceHelperForEdge(se, vertex);
			if (s.step())
				s.show("Removing status edge", highlighted(se));
			boolean existed = mSweepStatus.remove(se);
			if (!existed) {
				GeometryException.raise("could not find item in sweep status");
			}
		}

		if (newEdge != null) {
			SweepEdge se = SweepEdge.edge(newEdge, vertex);
			if (s.step())
				s.show("Adding status edge", highlighted(se));
			mSweepStatus.add(se);
		}
	}

	private void buildVertexList(Edge edgePointingToHighestVertex) {
		mVertexList.clear();
		Vertex startVertex = edgePointingToHighestVertex.destVertex();

		Edge eRight = edgePointingToHighestVertex.dual();
		Edge eLeft = eRight.prevEdge();

		startVertex.clearFlags(VERTEXFLAG_LEFTSIDE);
		if (eLeft.destVertex().y > eRight.destVertex().y) {
			startVertex.addFlags(VERTEXFLAG_LEFTSIDE);
		}
		mVertexList.add(startVertex);

		while (true) {
			Vertex nextLeft = eLeft.destVertex();
			Vertex nextRight = eRight.destVertex();
			if (nextLeft.y > nextRight.y) {
				nextLeft.addFlags(VERTEXFLAG_LEFTSIDE);
				mVertexList.add(nextLeft);
				eLeft = eLeft.dual().prevEdge();
			} else {
				nextRight.clearFlags(VERTEXFLAG_LEFTSIDE);
				mVertexList.add(nextRight);
				eRight = eRight.dual().nextEdge();
			}
			if (nextLeft == nextRight) {
				break;
			}
		}
	}

	private void triangulateMonotoneFace(final Edge edgePointingToHighestVertex) {
		// have stepper display the face while triangulating it
		s.addLayer(BGND_ELEMENT_MONOTONE_FACE, new Renderable() {
			public void render(AlgorithmStepper s) {
				// Construct CCW-ordered polygon from monotone face's vertices
				buildVertexList(edgePointingToHighestVertex);
				Polygon facePolygon = new Polygon();
				Deque<Point> points = new ArrayDeque();
				for (Vertex vertex : mVertexList) {
					if (vertex.hasFlags(VERTEXFLAG_LEFTSIDE))
						points.addLast(vertex);
					else
						points.addFirst(vertex);
				}
				while (!points.isEmpty())
					facePolygon.add(points.removeFirst());
				s.setColor(Color.argb(0x60, 0x80, 0xff, 0x80));
				s.render(facePolygon.renderable(true));
			}
		});

		// call an auxilliary function to do the actual triangulation
		s.pushActive(DETAIL_TRIANGULATE_MONOTONE_FACE);
		triangulateMonotoneFaceAux(edgePointingToHighestVertex);
		s.popActive();

		if (s.isActive())
			s.removeLayer(BGND_ELEMENT_MONOTONE_FACE);
	}

	// See:
	// http://www.personal.kent.edu/~rmuhamma/Compgeometry/MyCG/PolyPart/polyPartition.htm
	//
	private void triangulateMonotoneFaceAux(Edge edgePointingToHighestVertex) {
		if (s.bigStep())
			s.show("Triangulate monotone face",
					s.highlighted(edgePointingToHighestVertex));
		if (edgePointingToHighestVertex.visited()) {
			if (s.step())
				s.show("Edge already visited");
			return;
		}
		edgePointingToHighestVertex.setVisited(true);

		buildVertexList(edgePointingToHighestVertex);

		boolean queueIsLeft;
		int vIndex = 0;
		{
			mMonotoneQueue.clear();
			Vertex v = mVertexList.get(vIndex++);
			queueIsLeft = (v.flags() & VERTEXFLAG_LEFTSIDE) != 0;

			mMonotoneQueue.addLast(v);
			Vertex v2 = mVertexList.get(vIndex++);
			if (s.step())
				s.show("Queuing vertex", s.highlighted(v2));
			mMonotoneQueue.addLast(v2);
		}

		// We don't want to add edges that already exist. The last edge
		// added (if we follow the pseudocode) actually already exists in
		// the mesh; so use a counter to determine when we're done.

		int edgesRemaining = mVertexList.size() - 3;

		while (edgesRemaining != 0) {
			ASSERT(vIndex < mVertexList.size());
			Vertex vertex = mVertexList.get(vIndex++);

			if (queueIsLeft != ((vertex.flags() & VERTEXFLAG_LEFTSIDE) != 0)) {
				while (edgesRemaining != 0 && mMonotoneQueue.size() > 1) {
					// Skip the first queued vertex
					Vertex v1 = mMonotoneQueue.removeFirst();
					if (s.step())
						s.show("Skipping first queued vertex",
								s.highlighted(v1));
					Vertex v2 = mMonotoneQueue.getFirst();
					addEdge(v2, vertex);
					edgesRemaining--;
				}
				if (s.step())
					s.show("Queuing vertex", s.highlighted(vertex));
				mMonotoneQueue.addLast(vertex);
				queueIsLeft ^= true;
			} else {
				while (edgesRemaining != 0 && mMonotoneQueue.size() > 1) {
					// Peek at the last two vertices in the queue (..v2,v1]) by
					// temporarily removing v1
					Vertex v1 = mMonotoneQueue.removeLast();
					Vertex v2 = mMonotoneQueue.getLast();
					mMonotoneQueue.addLast(v1);
					float distance = pointUnitLineSignedDistance(vertex, v1, v2);
					boolean isConvex = ((distance > 0) ^ queueIsLeft);
					if (s.step())
						s.show("Test for convex angle", s.highlighted(v1),
								s.highlighted(v2));

					if (!isConvex)
						break;
					addEdge(v2, vertex);
					edgesRemaining--;
					mMonotoneQueue.removeLast();
				}
				if (s.step())
					s.show("Queuing vertex", s.highlighted(vertex));
				mMonotoneQueue.addLast(vertex);
			}
		}
	}

	private Edge addEdge(Vertex v1, Vertex v2) {
		if (s.step())
			s.show("Adding mesh edge", s.highlightedLine(v1, v2));
		return mMesh.addEdge(v1, v2);
	}

	// Convenience methods for displaying algorithm objects

	private Renderable highlighted(SweepEdge edge) {
		return s.highlighted(edge.polygonEdge());
	}

	private static class SweepEdge {

		public Edge mPolygonEdge;
		public Vertex mHelper;

		public Edge polygonEdge() {
			return mPolygonEdge;
		}

		public static SweepEdge edge(Edge polygonEdge, Vertex helper) {
			SweepEdge s = new SweepEdge();
			s.mPolygonEdge = polygonEdge;
			s.mHelper = helper;
			return s;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("SweepEdge(polygonEdge ");
			sb.append(mPolygonEdge);
			sb.append(" helper ");
			sb.append(nameOf(mHelper));
			return sb.toString();
		}

		/**
		 * Calculate intersection point of edge with horizontal sweep line
		 * 
		 * @param sweepLinePosition
		 * @param context
		 * @param clampWithinRange
		 *            if true, clamps sweep line to lie within edge's vertical
		 *            range
		 * @return intersection point
		 */
		public Point positionOnSweepLine(float sweepLinePosition, Mesh context,
				boolean clampWithinRange) {
			Edge edge = mPolygonEdge;

			Point v1 = edge.sourceVertex();
			Point v2 = edge.destVertex();

			if (clampWithinRange) {
				float y0 = Math.min(v1.y, v2.y);
				float y1 = Math.max(v1.y, v2.y);
				sweepLinePosition = MyMath.clamp(sweepLinePosition, y0, y1);
			}

			Point ipt = MyMath.segHorzLineIntersection(v1, v2,
					sweepLinePosition, null);

			if (ipt == null) {
				GeometryException.raise("Sweep " + d(sweepLinePosition)
						+ " doesn't intersect edge " + edge);
			}
			return ipt;
		}

		public Vertex helper() {
			return mHelper;
		}

		public void setHelper(Vertex h) {
			mHelper = h;
		}
	}

	private AlgorithmStepper s;
	private Mesh mMesh;
	private Polygon mPolygon;
	private ArrayList<Vertex> mVertexEvents;
	private TreeSet<SweepEdge> mSweepStatus;
	private boolean mSweepLineVisible;
	private float mSweepLinePosition;
	private Deque<Vertex> mMonotoneQueue;
	private ArrayList<Vertex> mVertexList;
	private int mPolygonMeshBase;
}
