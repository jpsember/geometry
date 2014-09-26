package com.js.samplealgorithm;

import java.util.ArrayList;

import android.graphics.Color;

import com.js.android.MyActivity;
import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
import com.js.geometry.GeometryException;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.geometry.Vertex;
import com.js.geometryapp.AlgorithmDisplayElement;
import com.js.geometryapp.AlgorithmStepper;

import static com.js.geometry.MyMath.*;
import static com.js.basic.Tools.*;

public class Delaunay {

	public static final String DETAIL_SWAPS = "Swaps";
	public static final String DETAIL_FIND_TRIANGLE = "Find Triangle";

	private static final float HORIZON = 1e7f;
	// Add prefix to distinguish this algorithm's elements from others
	private static final String BGND_ELEMENT_QUERY_POINT = "d20";
	private static final String BGND_ELEMENT_SEARCH_HISTORY = "d12";
	private static final String BGND_ELEMENT_BEARING_LINE = "d10";
	private static final String BGND_ELEMENT_HOLE_BOUNDARY = "d15";
	private static final int COLOR_DARKGREEN = Color.argb(255, 30, 128, 30);

	private static final int EDGEFLAG_HOLEBOUNDARY = (1 << 0);

	/**
	 * Constructor
	 * 
	 * @param context
	 *            context to use; its mesh will be cleared
	 * @param boundingRect
	 *            bounding rect, or null to use (large) default
	 */
	public Delaunay(GeometryContext context, Rect boundingRect) {
		doNothing();
		mStepper = AlgorithmStepper.sharedInstance();
		constructMesh(context, boundingRect);
	}

	/**
	 * Add a site, triangulating as required. Adds a new vertex to the mesh
	 * 
	 * @param point
	 *            location of site
	 * 
	 * @return the new Vertex
	 */
	public Vertex add(Point point) {
		if (mStepper.isActive()) {
			mStepper.plotToBackground(BGND_ELEMENT_QUERY_POINT);
			plot(point);
		}

		if (update())
			show("*Add point");

		Edge edge = findTriangleContainingPoint(point);

		Vertex newVertex = insertPointIntoTriangle(point, edge);

		if (mStepper.isActive()) {
			mStepper.removeBackgroundElement(BGND_ELEMENT_QUERY_POINT);
		}

		return newVertex;
	}

	/**
	 * Remove a vertex
	 * 
	 * @param vertex
	 *            vertex previously returned by add()
	 */
	public void remove(Vertex vertex) {
		if (mStepper.isActive()) {
			mStepper.plotToBackground(BGND_ELEMENT_QUERY_POINT);
			plot(vertex);
		}

		if (update())
			show("*Remove vertex");

		// Find arbitrary edge leaving vertex, and use it to find an arbitrary
		// edge bounding the polygonal hole that will result from deleting this
		// vertex
		Edge edge = vertex.edges();
		Edge holeEdge = edge.nextFaceEdge();
		if (update())
			show("Edge of resulting hole" + plot(holeEdge));

		mContext.deleteVertex(vertex);

		markHoleBoundary(holeEdge);

		triangulateHole(vertex);

		if (mStepper.isActive()) {
			mStepper.removeBackgroundElement(BGND_ELEMENT_HOLE_BOUNDARY);
			mStepper.removeBackgroundElement(BGND_ELEMENT_QUERY_POINT);
		}

		removeHoleBoundary();
	}

	/**
	 * Gather edges of hole into array, and flag them as such
	 * 
	 * @param holeEdge
	 *            arbitrary edge on hole boundary
	 */
	private void markHoleBoundary(Edge holeEdge) {
		// Throw out any previous hole, in case there was some sort of error
		// previously
		removeHoleBoundary();
		Edge edge = holeEdge;
		while (true) {
			edge.addFlags(EDGEFLAG_HOLEBOUNDARY);
			mHoleEdges.add(edge);
			edge = edge.nextFaceEdge();
			if (edge == holeEdge)
				break;
		}
		if (mStepper.isActive()) {
			mStepper.plotToBackground(BGND_ELEMENT_QUERY_POINT);
			mStepper.plotElement(new AlgorithmDisplayElement() {
				@Override
				public void render() {
					mStepper.setLineWidth(1);
					mStepper.setColor(COLOR_DARKGREEN);
					for (Edge edge : mHoleEdges) {
						mStepper.plotLine(edge.sourceVertex(),
								edge.destVertex());
					}
				}
			});
		}
	}


	private void triangulateHole(Point kernelPoint) {
		StarshapedHoleTriangulator triangulator = StarshapedHoleTriangulator
				.buildTriangulator(mContext, kernelPoint, mHoleEdges.get(0));
		triangulator.run();

		for (Edge abEdge : triangulator.getNewEdges()) {
			if (update())
				show("Process next hole edge" + plot(abEdge));
			if (abEdge.deleted()) {
				if (update())
					show("Deleted, skipping");
				continue;
			}

			// Determine the parameters for the two calls to swapTest(), one for
			// each side of the edge. The call may end up deleting some edges,
			// so figure this out before making these calls
			Edge baEdge = abEdge.dual();

			Edge bcEdge = abEdge.nextFaceEdge();
			Vertex c = bcEdge.destVertex();

			Edge adEdge = baEdge.nextFaceEdge();
			Vertex d = adEdge.destVertex();

			swapTest(abEdge, c);
			swapTest(baEdge, d);
		}
	}

	/**
	 * Clear the 'hole' flags from the hole boundary edges, and throw away the
	 * edges
	 */
	private void removeHoleBoundary() {
		for (Edge e : mHoleEdges) {
			e.clearFlags(EDGEFLAG_HOLEBOUNDARY);
		}
		mHoleEdges.clear();
	}

	private Vertex insertPointIntoTriangle(Point point, Edge abEdge) {
		Vertex v = mContext.addVertex(point);
		Vertex va = abEdge.sourceVertex();
		Vertex vb = abEdge.destVertex();
		Edge bcEdge = abEdge.nextFaceEdge();
		Edge caEdge = bcEdge.nextFaceEdge();

		Vertex vc = bcEdge.destVertex();

		Edge ad = mContext.addEdge(va, v);
		Edge bd = mContext.addEdge(vb, v);
		Edge cd = mContext.addEdge(vc, v);
		if (update())
			show("Partitioned triangle" + plot(ad) + plot(bd) + plot(cd));

		mActiveDetailName = DETAIL_SWAPS;
		swapTest(abEdge, v);
		swapTest(bcEdge, v);
		swapTest(caEdge, v);
		mActiveDetailName = null;

		if (update())
			show("done insertion");

		return v;
	}

	/**
	 * Given edge ab and a vertex p, determines if a face baw to the right of ab
	 * exists, and if so, whether its third vertex w intersects the circumcircle
	 * of abp. If so, flips ab with wp, and recursively tests aw and wb against
	 * vertex p.
	 * 
	 * @param abEdge
	 * @param p
	 */
	private void swapTest(Edge abEdge, Vertex p) {

		// If this edge has been deleted, do nothing
		if (abEdge.deleted()) {
			if (update())
				show("SwapTest, edge has been deleted"
						+ plot(abEdge.sourceVertex(), abEdge.destVertex()));
			return;
		}
		if (abEdge.hasFlags(EDGEFLAG_HOLEBOUNDARY)) {
			if (update())
				show("SwapTest, hole boundary" + plot(abEdge));
			return;
		}

		Edge baEdge = abEdge.dual();
		Edge awEdge = baEdge.nextFaceEdge();
		Vertex w = awEdge.destVertex();
		if (update())
			show("SwapTest" + plot(abEdge) + plot(p) + plot(w));
		if (!pointLeftOfEdge(w, baEdge)) {
			if (update())
				show("boundary edge detected" + plot(baEdge) + plot(w));
			return;
		}

		Point a = abEdge.sourceVertex();
		Point b = abEdge.destVertex();

		double determinant;
		{

			double c11 = a.x - p.x;
			double c12 = a.y - p.y;
			double c13 = c11 * c11 + c12 * c12;
			double c21 = w.x - p.x;
			double c22 = w.y - p.y;
			double c23 = c21 * c21 + c22 * c22;
			double c31 = b.x - p.x;
			double c32 = b.y - p.y;
			double c33 = c31 * c31 + c32 * c32;

			determinant = c11 * (c22 * c33 - c32 * c23) - c12
					* (c21 * c33 - c31 * c23) + c13 * (c21 * c32 - c31 * c22);

			// Choose an epsilon value that is related to the bounding box of
			// the points
			float sx = Math.abs(a.x - w.x);
			float sx2 = Math.abs(a.x - b.x);
			if (sx2 > sx)
				sx = sx2;
			sx2 = Math.abs(a.y - w.y);
			if (sx2 > sx)
				sx = sx2;
			sx2 = Math.abs(a.y - b.y);
			if (sx2 > sx)
				sx = sx2;

			float epsilon = (sx * sx) * 1e-3f;
			if (update())
				show("Sign of determinant: " + Math.signum(determinant));
			mContext.testForZero((float) determinant, epsilon);
		}
		if (determinant > 0) {
			if (update())
				show("*flipping edge" + plot(abEdge) + plot(p, w));

			mContext.deleteEdge(abEdge);
			Edge pw = mContext.addEdge(p, w);
			Edge wbEdge = pw.nextFaceEdge();
			if (update())
				show("recursive swap test" + plot(awEdge));
			swapTest(awEdge, p);
			if (update())
				show("recursive swap test" + plot(wbEdge));
			swapTest(wbEdge, p);
		}
	}

	private Edge findInitialSearchEdgeForPoint(Point point) {
		Vertex v = mContext.vertex(0);
		return v.edges();
	}

	private Vertex oppositeVertex(Edge triangleEdge) {
		return triangleEdge.nextFaceEdge().destVertex();
	}

	private Edge findTriangleContainingPoint(Point queryPoint) {
		mActiveDetailName = DETAIL_FIND_TRIANGLE;

		if (mStepper.isActive()) {
			mSearchHistory = new ArrayList();
			mStepper.plotToBackground(BGND_ELEMENT_SEARCH_HISTORY);
			mStepper.plotElement(new AlgDisplaySearchHistory());
		}

		if (update())
			show("Finding triangle containing point");

		Edge aEdge = findInitialSearchEdgeForPoint(queryPoint);
		Edge bEdge = null;
		Edge cEdge = null;

		if (update())
			show("Initial edge" + plot(aEdge));

		if (!pointLeftOfEdge(queryPoint, aEdge)) {
			GeometryException.raise("query point " + queryPoint
					+ " not to left of search edge " + aEdge);
		}

		// Construct segment from midpoint of the initial edge and the query
		// point; we'll try to follow this line
		Point bearingStartPoint = MyMath.interpolateBetween(
				aEdge.sourceVertex(), aEdge.destVertex(), .5f);
		if (mStepper.isActive()) {
			mStepper.plotToBackground(BGND_ELEMENT_BEARING_LINE);
			mStepper.setLineWidth(2);
			mStepper.setColor(Color.LTGRAY);
			mStepper.plotRay(bearingStartPoint, queryPoint);
		}
		if (update())
			show("Bearing line");

		int maxIterations = mContext.vertexBuffer().size();
		while (true) {
			if (maxIterations-- == 0)
				GeometryException.raise("too many iterations");

			if (mStepper.isActive()) {
				mSearchHistory.add(aEdge);
			}

			bEdge = aEdge.nextFaceEdge();
			cEdge = bEdge.nextFaceEdge();
			if (oppositeVertex(bEdge) != aEdge.sourceVertex())
				GeometryException.raise("search edge not adjacent to triangle "
						+ aEdge);
			if (update())
				show("Current search triangle" + plot(aEdge)
						+ plot(oppositeVertex(aEdge)));

			boolean bLeftFlag = pointLeftOfEdge(queryPoint, bEdge);
			boolean cLeftFlag = pointLeftOfEdge(queryPoint, cEdge);
			if (bLeftFlag && cLeftFlag) {
				break;
			}

			if (bLeftFlag) {
				aEdge = cEdge.dual();
			} else if (cLeftFlag) {
				aEdge = bEdge.dual();
			} else {
				Vertex farPoint = bEdge.destVertex();
				if (false && update())
					show("Testing side of bearing line"
							+ plot(bearingStartPoint, queryPoint)
							+ plot(farPoint));
				if (MyMath.sideOfLine(bearingStartPoint, queryPoint, farPoint) > 0) {
					aEdge = bEdge.dual();
				} else {
					aEdge = cEdge.dual();
				}
			}
		}
		if (update())
			show("*Triangle containing query point"
					+ plot(aEdge.sourceVertex(), bEdge.sourceVertex(),
							cEdge.sourceVertex()));
		mActiveDetailName = null;
		if (mStepper.isActive()) {
			mStepper.removeBackgroundElement(BGND_ELEMENT_SEARCH_HISTORY);
			mStepper.removeBackgroundElement(BGND_ELEMENT_BEARING_LINE);
		}

		return aEdge;
	}

	private boolean pointLeftOfEdge(Point query, Edge edge) {
		Point p1 = edge.sourceVertex();
		Point p2 = edge.destVertex();
		return sideOfLine(p1, p2, query) > 0;
	}

	private void constructMesh(GeometryContext c, Rect boundingRect) {
		c.clearMesh();

		if (boundingRect == null)
			boundingRect = new Rect(-HORIZON, -HORIZON, HORIZON * 2,
					HORIZON * 2);

		Vertex v0 = c.addVertex(boundingRect.bottomLeft());
		Vertex v1 = c.addVertex(boundingRect.bottomRight());
		Vertex v2 = c.addVertex(boundingRect.topRight());
		Point p3 = boundingRect.topLeft();
		// Perturb one point so the four points aren't collinear
		p3.x -= 1e-3f;
		p3.y += 1e-3f;
		Vertex v3 = c.addVertex(p3);

		c.addEdge(v0, v1);
		c.addEdge(v1, v2);
		c.addEdge(v2, v3);
		c.addEdge(v3, v0);
		c.addEdge(v0, v2);

		mContext = c;
	}

	// Convenience methods for using stepper

	private boolean update() {
		return mStepper.update(mActiveDetailName);
	}

	private void show(Object message) {
		mStepper.show(message);
	}

	private String plot(Point a, Point b, Point c) {
		mStepper.setLineWidth(2);
		mStepper.setColor(Color.RED);
		return mStepper.plotLine(a, b) + mStepper.plotLine(b, c)
				+ mStepper.plotLine(c, a);

	}

	private String plot(Edge edge) {
		return plot(edge.sourceVertex(), edge.destVertex());
	}

	private String plot(Point p1, Point p2) {
		mStepper.setLineWidth(2);
		mStepper.setColor(Color.RED);
		return mStepper.plotRay(p1, p2);
	}

	private String plot(Point v) {
		mStepper.setColor(Color.RED);
		return mStepper.plot(v);
	}

	private Point faceCentroid(Edge faceEdge) {
		Point p = new Point(faceEdge.sourceVertex());
		p.add(faceEdge.destVertex());
		p.add(oppositeVertex(faceEdge));
		p.setTo(p.x / 3, p.y / 3);
		return p;
	}

	/**
	 * Custom display element for triangle search history
	 */
	private class AlgDisplaySearchHistory extends AlgorithmDisplayElement {

		@Override
		public void render() {
			if (mSearchHistory == null)
				return;
			setColorState(COLOR_DARKGREEN);
			setLineWidthState(2);
			Edge prevEdge = null;
			Point prevCentroid = null;
			for (Edge edge : mSearchHistory) {
				mStepper.setLineWidth(1);
				mStepper.setColor(COLOR_DARKGREEN);
				Point p1 = edge.sourceVertex();
				Point p2 = edge.destVertex();
				Point centroid = faceCentroid(edge);

				if (prevEdge != null) {
					// If segment connecting centroids intersects edge, just
					// draw straight line
					if (mContext.segSegIntersection(prevCentroid, centroid, p1,
							p2) != null) {
						mStepper.plotLine(prevCentroid, centroid);
					} else {
						Point midPoint = MyMath.interpolateBetween(p1, p2, .5f);
						mStepper.plotLine(midPoint, centroid);
						mStepper.plotLine(prevCentroid, midPoint);
					}
					mStepper.plot(centroid, MyActivity.inchesToPixels(.01f));
				}
				prevEdge = edge;
				prevCentroid = centroid;
			}
		}
	}

	private String mActiveDetailName;
	private GeometryContext mContext;
	private AlgorithmStepper mStepper;
	private ArrayList<Edge> mSearchHistory;
	private ArrayList<Edge> mHoleEdges = new ArrayList();
}
