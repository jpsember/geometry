package com.js.geometry;

import static com.js.basic.Tools.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

import android.graphics.Color;

import com.js.basic.Queue;
import com.js.geometryapp.AlgDisplayElement;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;

/**
 * Triangulates a polygon.
 * 
 * Polygon must be CCW oriented.
 * 
 */
public class PolygonTriangulator {

	public static PolygonTriangulator triangulator(GeometryContext context,
			Polygon polygon) {
		return new PolygonTriangulator(context, polygon);
	}

	private PolygonTriangulator(GeometryContext context, Polygon polygon) {
		mStepper = AlgorithmStepper.sharedInstance();
		mContext = context;
		mPolygon = polygon;
		ASSERT(polygon.isCCW(context));
		mMonotoneQueue = new Queue();
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

	private boolean update() {
		return mStepper.update();
	}

	private void show(Object message) {
		mStepper.show(message);
	}

	private static final String BGND_ELEMENT_POLYGON = "1_Polygon";
	private static final String BGND_ELEMENT_SWEEPSTATUS = "2_Status";
	private static final String BGND_ELEMENT_MESH = "0_Mesh";
	private static final int COLOR_LIGHTBLUE = Color.argb(80, 100, 100, 255);
	private static final int COLOR_DARKGREEN = Color.argb(255, 30, 128, 30);

	public void triangulate() {
		if (mStepper.isActive()) {
			mStepper.plotToBackground(BGND_ELEMENT_POLYGON);
			mStepper.setLineWidth(1);
			mStepper.setColor(Color.BLUE);
			mStepper.plot(mPolygon);

			mStepper.plotToBackground(BGND_ELEMENT_MESH);
			mStepper.setLineWidth(1);
			mStepper.setColor(COLOR_LIGHTBLUE);
			mStepper.plot(mContext);
		}

		if (update())
			show("*Triangulating polygon");

		mPolygonMeshBase = mPolygon.embed(mContext);
		createEventList();
		createSweepStatus();

		for (Vertex v : mVertexEvents) {
			processVertexEvent(v);
		}

		if (mStepper.isActive()) {
			mStepper.removeBackgroundElement(BGND_ELEMENT_SWEEPSTATUS);
		}

		if (update())
			show("*Done triagulating polygon");

	}

	private void createSweepStatus() {
		mSweepStatus = new TreeSet<SweepEdge>(new Comparator<SweepEdge>() {
			@Override
			public int compare(SweepEdge a, SweepEdge b) {
				Point sa = a.positionOnSweepLine(mSweepLinePosition, mContext,
						false);
				Point sb = b.positionOnSweepLine(mSweepLinePosition, mContext,
						false);
				return (int) Math.signum(sa.x - sb.x);
			}
		});
		mSweepLineVisible = false;

		if (mStepper.isActive()) {
			mStepper.plotToBackground(BGND_ELEMENT_SWEEPSTATUS);
			mStepper.plotElement(new AlgDisplaySweepStatus());
		}
	}

	private void createEventList() {
		ArrayList<Vertex> array = new ArrayList();
		for (int i = 0; i < mPolygon.numVertices(); i++) {
			Vertex vertex = mContext.vertex(mPolygonMeshBase + i);
			vertex.clearFlags();
			array.add(vertex);
		}

		Collections.sort(array, new Comparator<Vertex>() {
			@Override
			public int compare(Vertex va, Vertex vb) {
				float diff = va.point().y - vb.point().y;
				mContext.testForZero(diff);
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
			ASSERT(edge != vertex.edges());
		}
	}

	private void polygonEdgesThroughVertex(Vertex vertex, Edge output[]) {
		Edge edge = polygonEdgeLeavingVertex(vertex);
		output[1] = edge;
		Edge edgeB = edge.nextEdge().dual();
		ASSERT(edgeB.destVertex() == vertex);
		output[0] = edgeB;
	}

	private int vertexType(Vertex v, Edge incoming, Edge outgoing) {
		Point ipt = incoming.dual().destVertex().point();
		Point opt = outgoing.destVertex().point();
		Point vpt = v.point();

		int type;
		if (opt.y > vpt.y) {
			if (ipt.y > vpt.y) {
				if (mContext.pseudoAngleIsConvex(outgoing.angle(), incoming
						.dual().angle())) {
					type = VTYPE_START;
				} else {
					type = VTYPE_SPLIT;
				}
			} else {
				type = VTYPE_REGULAR_UP;
			}
		} else {
			if (ipt.y < vpt.y) {
				if (mContext.pseudoAngleIsConvex(outgoing.angle(), incoming
						.dual().angle())) {
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
	private Edge replaceHelperForEdge(SweepEdge s, Vertex newHelper) {
		Vertex prevHelper = s.helper();
		if (update())
			show("replace edge helper" + plot(prevHelper) + plot(newHelper)
					+ plot(s));

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
		s.setHelper(newHelper);
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
		if (update())
			show("process vertex event" + plot(vertex));

		moveSweepLineTo(vertex.point().y);
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
			if (update())
				show("removing status edge" + plot(se));
			boolean existed = mSweepStatus.remove(se);
			if (mContext.checkError(!existed)) {
				GeometryException.raise("could not find item in sweep status");
			}
		}

		if (newEdge != null) {
			SweepEdge se = SweepEdge.edge(newEdge, vertex);
			if (update())
				show("adding status edge" + plot(se));
			mSweepStatus.add(se);
		}
	}

	private void buildVertexList(Edge edgePointingToHighestVertex) {
		mVertexList.clear();
		Vertex startVertex = edgePointingToHighestVertex.destVertex();

		Edge eRight = edgePointingToHighestVertex.dual();
		Edge eLeft = eRight.prevEdge();

		startVertex.clearFlags(VERTEXFLAG_LEFTSIDE);
		if (eLeft.destVertex().point().y > eRight.destVertex().point().y) {
			startVertex.addFlags(VERTEXFLAG_LEFTSIDE);
		}
		mVertexList.add(startVertex);

		while (true) {
			Vertex nextLeft = eLeft.destVertex();
			Vertex nextRight = eRight.destVertex();
			if (nextLeft.point().y > nextRight.point().y) {
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

	// See:
	// http://www.personal.kent.edu/~rmuhamma/Compgeometry/MyCG/PolyPart/polyPartition.htm
	//
	private void triangulateMonotoneFace(Edge edgePointingToHighestVertex) {
		if (update())
			show("*triangulating monotone face"
					+ plot(edgePointingToHighestVertex));
		if (edgePointingToHighestVertex.visited()) {
			if (update())
				show("edge already visited");
			return;
		}
		edgePointingToHighestVertex.setVisited(true);

		buildVertexList(edgePointingToHighestVertex);
		if (mVertexList.size() == 3) {
			if (update())
				show("already a triangle");
			return;
		}

		boolean queueIsLeft;
		int vIndex = 0;
		{
			mMonotoneQueue.clear();
			Vertex v = mVertexList.get(vIndex++);
			queueIsLeft = (v.flags() & VERTEXFLAG_LEFTSIDE) != 0;

			mMonotoneQueue.push(v);
			Vertex v2 = mVertexList.get(vIndex++);
			if (update())
				show("queuing vertex" + plot(v2));
			mMonotoneQueue.push(v2);
		}

		// We don't want to add edges that already exist. The last edge added
		// (if we follow the
		// pseudocode) actually already exists in the mesh; so use a counter to
		// determine when we're done.

		ASSERT(mVertexList.size() >= 3);
		int edgesRemaining = mVertexList.size() - 3;

		while (edgesRemaining != 0) {
			ASSERT(vIndex < mVertexList.size());
			Vertex vertex = mVertexList.get(vIndex++);

			if (queueIsLeft != ((vertex.flags() & VERTEXFLAG_LEFTSIDE) != 0)) {

				while (edgesRemaining != 0 && mMonotoneQueue.size() > 1) {
					// Skip the first queued vertex
					Vertex v1 = mMonotoneQueue.pop();
					if (update())
						show("skipping first queued vertex" + plot(v1));
					Vertex v2 = mMonotoneQueue.peek();
					addEdge(v2, vertex);
					edgesRemaining--;
				}
				if (update())
					show("queuing vertex" + plot(vertex));
				mMonotoneQueue.push(vertex);
				queueIsLeft ^= true;
			} else {
				while (edgesRemaining != 0 && mMonotoneQueue.size() > 1) {
					Vertex v1 = mMonotoneQueue.peek(false, 0);
					Vertex v2 = mMonotoneQueue.peek(false, 1);
					float distance = mContext.pointUnitLineSignedDistance(
							vertex.point(), v1.point(), v2.point());
					boolean isConvex = ((distance > 0) ^ queueIsLeft);
					if (update())
						show("test for convex angle" + plot(v1) + plot(v2));

					if (!isConvex)
						break;
					addEdge(v2, vertex);
					edgesRemaining--;
					mMonotoneQueue.pop(false);
				}
				if (update())
					show("queuing vertex" + plot(vertex));
				mMonotoneQueue.push(vertex);
			}
		}
	}

	private Edge addEdge(Vertex v1, Vertex v2) {
		if (update())
			show("adding mesh edge" + plotEdge(v1, v2));
		return mContext.addEdge(null, v1, v2);
	}

	/**
	 * Custom display element for sweep status
	 */
	private class AlgDisplaySweepStatus extends AlgDisplayElement {

		@Override
		public void render() {
			if (!mSweepLineVisible)
				return;
			setColorState(COLOR_DARKGREEN);
			setLineWidthState(1);
			renderLine(0, mSweepLinePosition,
					AlgorithmRenderer.ALGORITHM_SPACE_WIDTH, mSweepLinePosition);
			setColorState(COLOR_DARKGREEN);
			setLineWidthState(2);
			for (SweepEdge e : mSweepStatus) {

				// Extrapolate a little above and below the sweep line
				float EXTENT = 22 * AlgorithmRenderer
						.algorithmToDensityPixels();
				Point p1 = e.positionOnSweepLine(mSweepLinePosition - EXTENT
						* .8f, mContext, true);
				Point p2 = e.positionOnSweepLine(mSweepLinePosition + EXTENT
						* 1.2f, mContext, true);
				renderRay(p1, p2);

				Point pt = e.positionOnSweepLine(mSweepLinePosition, mContext,
						false);
				renderPoint(pt);
			}
		}
	}

	// Convenience methods for displaying algorithm objects

	private String plot(SweepEdge edge) {
		return plot(edge.polygonEdge());
	}

	private String plot(Edge edge) {
		return plotEdge(edge.sourceVertex(), edge.destVertex());
	}

	private String plotEdge(Point p1, Point p2) {
		mStepper.setLineWidth(2);
		mStepper.setColor(Color.RED);
		return mStepper.plotLine(p1, p2);
	}

	private String plotEdge(Vertex v1, Vertex v2) {
		return plotEdge(v1.point(), v2.point());
	}

	private String plot(Vertex v) {
		mStepper.setColor(Color.RED);
		return mStepper.plot(v.point());
	}

	private AlgorithmStepper mStepper;
	private GeometryContext mContext;
	private Polygon mPolygon;
	private ArrayList<Vertex> mVertexEvents;
	private TreeSet<SweepEdge> mSweepStatus;
	private boolean mSweepLineVisible;
	private float mSweepLinePosition;
	private Queue<Vertex> mMonotoneQueue;
	private ArrayList<Vertex> mVertexList;
	private int mPolygonMeshBase;
}
