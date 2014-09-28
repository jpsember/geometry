package com.js.samplealgorithm;

import java.util.ArrayList;
import java.util.Random;

import android.graphics.Color;

import com.js.geometry.Edge;
import com.js.geometry.Mesh;
import com.js.geometry.GeometryException;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
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
	private static final int EDGEFLAG_HORIZON = (1 << 1);

	private static final int INITIAL_MESH_VERTICES = 4;

	/**
	 * Constructor
	 * 
	 * @param context
	 *            context to use; its mesh will be cleared
	 * @param boundingRect
	 *            bounding rect, or null to use (large) default
	 */
	public Delaunay(Mesh context, Rect boundingRect) {
		doNothing();
		s = AlgorithmStepper.sharedInstance();
		constructMesh(context, boundingRect);
		mRandom = new Random(1);
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
		if (s.isActive()) {
			s.openLayer(BGND_ELEMENT_QUERY_POINT);
			plot(point);
			s.closeLayer();
		}

		if (s.bigStep())
			s.show("Add point");

		Edge edge = findTriangleContainingPoint(point);

		Vertex newVertex = insertPointIntoTriangle(point, edge);

		if (s.isActive()) {
			s.removeLayer(BGND_ELEMENT_QUERY_POINT);
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
		if (s.isActive()) {
			s.openLayer(BGND_ELEMENT_QUERY_POINT);
			plot(vertex);
			s.closeLayer();
		}

		if (s.bigStep())
			s.show("Remove vertex");

		// Find arbitrary edge leaving vertex, and use it to find an arbitrary
		// edge bounding the polygonal hole that will result from deleting this
		// vertex
		Edge edge = vertex.edges();
		Edge holeEdge = edge.nextFaceEdge();
		if (s.step())
			s.show("Edge of resulting hole" + plot(holeEdge));

		mContext.deleteVertex(vertex);

		markHoleBoundary(holeEdge);

		triangulateHole(vertex);

		if (s.isActive()) {
			s.removeLayer(BGND_ELEMENT_HOLE_BOUNDARY);
			s.removeLayer(BGND_ELEMENT_QUERY_POINT);
		}

		removeHoleBoundary();
	}

	public int nSites() {
		return mContext.numVertices() - INITIAL_MESH_VERTICES;
	}

	public Vertex site(int index) {
		if (index < 0)
			throw new IllegalArgumentException();
		int i = index + INITIAL_MESH_VERTICES;
		return mContext.vertex(i);
	}

	/**
	 * Construct the Voronoi polygon for one of the sites
	 */
	public Polygon constructVoronoiPolygon(int siteIndex) {
		Polygon p = new Polygon();
		Vertex site = site(siteIndex);
		Edge startEdge = site.edges();

		Point prevSeg1 = new Point();
		Point prevSeg2 = new Point();

		// Calculate last bisector
		{
			Edge lastEdge = startEdge.prevEdge();
			calculateBisector(site, lastEdge.destVertex(), prevSeg1, prevSeg2);
		}
		Point currSeg1 = new Point();
		Point currSeg2 = new Point();

		Edge edge = startEdge;
		while (true) {
			calculateBisector(site, edge.destVertex(), currSeg1, currSeg2);
			Point polyVertex = lineLineIntersection(prevSeg1, prevSeg2,
					currSeg1, currSeg2, null);
			p.add(polyVertex);

			edge = edge.nextEdge();
			if (edge == startEdge)
				break;

			prevSeg1.setTo(currSeg1);
			prevSeg2.setTo(currSeg2);
		}
		return p;
	}

	/**
	 * Calculate the bisector between two Voronoi sites
	 * 
	 * @param aSite
	 *            first site
	 * @param bSite
	 *            second site
	 * @param pt1
	 *            where to store one point on bisector
	 * @param pt2
	 *            where to store second point on bisector
	 */
	private void calculateBisector(Point aSite, Point bSite, Point pt1,
			Point pt2) {
		pt1.setTo((aSite.x + bSite.x) / 2, (aSite.y + bSite.y) / 2);
		pt2.setTo(pt1.x - (bSite.y - aSite.y), pt1.y + (bSite.x - aSite.x));
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
		if (s.isActive()) {
			s.openLayer(BGND_ELEMENT_HOLE_BOUNDARY);
			s.plot(new AlgorithmDisplayElement() {
				@Override
				public void render() {
					s.setLineWidth(1);
					s.setColor(COLOR_DARKGREEN);
					for (Edge edge : mHoleEdges) {
						s.plotLine(edge.sourceVertex(), edge.destVertex());
					}
				}
			});
			s.closeLayer();
		}
	}

	private void triangulateHole(Point kernelPoint) {
		if (s.step())
			s.show("Triangulating hole");
		StarshapedHoleTriangulator triangulator = StarshapedHoleTriangulator
				.buildTriangulator(mContext, kernelPoint, mHoleEdges.get(0));
		s.pushActive(false);
		triangulator.run();
		s.popActive();

		for (Edge abEdge : triangulator.getNewEdges()) {
			if (s.step())
				s.show("Process next hole edge" + plot(abEdge));
			swapTestQuad(abEdge);
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

		mContext.addEdge(va, v);
		mContext.addEdge(vb, v);
		mContext.addEdge(vc, v);
		if (s.step())
			s.show("Partitioned triangle" + plot(va, vb, vc)
					+ s.plotLine(va, v) + s.plotLine(vb, v) + s.plotLine(vc, v));

		// Note (see issue #53): the sequence of edges examined in each of the
		// three swapTest calls are disjoint, so no special bookkeeping is
		// required.

		s.pushActive(DETAIL_SWAPS);
		swapTest(abEdge, v);
		swapTest(bcEdge, v);
		swapTest(caEdge, v);
		s.popActive();

		if (s.step())
			s.show("Done insertion");

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
		ASSERT(!abEdge.deleted());

		// This is perhaps not necessary, since if no opposite face exists,
		// it would choose the next horizon vertex which should always
		// fail the incircle test;
		// but that is a subtle point so we'll be explicit
		if (abEdge.hasFlags(EDGEFLAG_HORIZON))
			return;

		Edge baEdge = abEdge.dual();
		Edge awEdge = baEdge.nextFaceEdge();
		Vertex w = awEdge.destVertex();
		if (s.step())
			s.show("SwapTest" + plot(abEdge) + plot(p) + plot(w));

		Point a = abEdge.sourceVertex();
		Point b = abEdge.destVertex();

		double determinant = doInCircleTest(a, b, w, p);
		if (s.step())
			s.show("Sign of determinant: " + Math.signum(determinant));
		if (determinant > 0) {

			if (s.step())
				s.show("Flipping edge" + plot(abEdge) + plot(p, w));

			mContext.deleteEdge(abEdge);
			Edge pw = mContext.addEdge(p, w);
			Edge wbEdge = pw.nextFaceEdge();
			swapTest(awEdge, p);
			swapTest(wbEdge, p);
		}
	}

	/**
	 * Given edge ab, of face abc, determines if a face baw to the right of ab
	 * exists, and if so, whether its third vertex w intersects the circumcircle
	 * of abc. If so, flips ab with wc, and recursively tests the four edges aw,
	 * wb, bc, and ca.
	 * 
	 * @param abEdge
	 */
	private void swapTestQuad(Edge abEdge) {

		// If this edge has been deleted, do nothing
		if (abEdge.deleted()) {
			if (s.step())
				s.show("SwapTestQuad, edge has been deleted"
						+ plot(abEdge.sourceVertex(), abEdge.destVertex()));
			return;
		}
		if (abEdge.hasFlags(EDGEFLAG_HOLEBOUNDARY)) {
			if (s.step())
				s.show("SwapTestQuad, hole boundary" + plot(abEdge));
			return;
		}

		Edge baEdge = abEdge.dual();
		Edge awEdge = baEdge.nextFaceEdge();

		Vertex w = awEdge.destVertex();
		if (s.step())
			s.show("SwapTestQuad" + plot(abEdge) + plot(baEdge.nextFaceEdge())
					+ plot(baEdge.prevFaceEdge()) + plot(abEdge.nextFaceEdge())
					+ plot(abEdge.prevFaceEdge()) + plot(w));

		Point a = abEdge.sourceVertex();
		Point b = abEdge.destVertex();
		Vertex c = abEdge.nextFaceEdge().destVertex();

		double determinant = doInCircleTest(a, b, w, c);
		if (s.step())
			s.show("Sign of determinant: " + Math.signum(determinant));
		if (determinant > 0) {
			if (s.step())
				s.show("Flipping edge" + plot(abEdge) + plot(c, w));

			mContext.deleteEdge(abEdge);

			Edge cwEdge = mContext.addEdge(c, w);
			Edge wbEdge = cwEdge.nextFaceEdge();
			Edge bcEdge = cwEdge.prevFaceEdge();
			Edge caEdge = cwEdge.prevEdge();

			swapTestQuad(awEdge);
			swapTestQuad(wbEdge);
			swapTestQuad(bcEdge);
			swapTestQuad(caEdge);
		}
	}

	private double doInCircleTest(Point a, Point b, Point w, Point p) {
		double c11 = a.x - p.x;
		double c12 = a.y - p.y;
		double c13 = c11 * c11 + c12 * c12;
		double c21 = w.x - p.x;
		double c22 = w.y - p.y;
		double c23 = c21 * c21 + c22 * c22;
		double c31 = b.x - p.x;
		double c32 = b.y - p.y;
		double c33 = c31 * c31 + c32 * c32;

		double determinant = c11 * (c22 * c33 - c32 * c23) - c12
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
		testForZero((float) determinant, epsilon);
		return determinant;
	}

	private void chooseSampleVertices() {
		int nSamples = determineOptimalSampleSize();
		int numVertices = mContext.numVertices();
		mSamples.clear();
		for (int i = 0; i < nSamples; i++) {
			int k = mRandom.nextInt(numVertices);
			Vertex sample = mContext.vertex(k);
			mSamples.add(sample);
		}
	}

	private Vertex findClosestSampleVertex(Point point) {
		Vertex closestSample = null;
		float closestSquaredDistance = 0;
		for (Vertex sample : mSamples) {
			float dist = MyMath.squaredDistanceBetween(point, sample);
			if (closestSample == null || dist < closestSquaredDistance) {
				closestSample = sample;
				closestSquaredDistance = dist;
			}
		}
		return closestSample;
	}

	private Edge findInitialSearchEdgeForPoint(Point point) {
		s.pushActive(DETAIL_FIND_TRIANGLE);

		chooseSampleVertices();
		Vertex closestSample = findClosestSampleVertex(point);

		// Choose arbitrary edge from this vertex
		Edge edge = closestSample.edges();
		if (MyMath.sideOfLine(edge.sourceVertex(), edge.destVertex(), point) < 0)
			edge = edge.dual();
		Edge initialEdge = edge;
		if (s.step())
			s.show("Closest sample and initial edge" + plot(initialEdge)
					+ plot(closestSample)
					+ s.plot(new AlgorithmDisplayElement() {
						@Override
						public void render() {
							s.setColor(COLOR_DARKGREEN);
							for (Vertex v : mSamples) {
								s.plot(v);
							}
						}
					}));
		s.popActive();
		return initialEdge;
	}

	private Vertex oppositeVertex(Edge triangleEdge) {
		return triangleEdge.nextFaceEdge().destVertex();
	}

	private Edge findTriangleContainingPoint(Point queryPoint) {
		s.pushActive(DETAIL_FIND_TRIANGLE);

		if (s.isActive()) {
			mSearchHistory = new ArrayList();
			s.openLayer(BGND_ELEMENT_SEARCH_HISTORY);
			s.plot(new AlgorithmDisplayElement() {

				@Override
				public void render() {
					if (mSearchHistory == null)
						return;
					s.setColor(COLOR_DARKGREEN);
					s.setLineWidth(2);
					Edge prevEdge = null;
					Point prevCentroid = null;
					for (Edge edge : mSearchHistory) {
						s.setLineWidth(1);
						s.setColor(COLOR_DARKGREEN);
						Point p1 = edge.sourceVertex();
						Point p2 = edge.destVertex();
						Point centroid = faceCentroid(edge);

						if (prevEdge != null) {
							// If segment connecting centroids intersects edge,
							// just draw straight line
							if (segSegIntersection(prevCentroid, centroid, p1,
									p2, null) != null) {
								s.plotLine(prevCentroid, centroid);
							} else {
								Point midPoint = MyMath.interpolateBetween(p1,
										p2, .5f);
								s.plotLine(midPoint, centroid);
								s.plotLine(prevCentroid, midPoint);
							}
						}
						s.plot(centroid, 1.5f);
						prevEdge = edge;
						prevCentroid = centroid;
					}
				}
			});
			s.closeLayer();
		}

		if (s.step())
			s.show("Finding triangle containing point");

		Edge aEdge = findInitialSearchEdgeForPoint(queryPoint);
		Edge bEdge = null;
		Edge cEdge = null;

		if (!pointLeftOfEdge(queryPoint, aEdge)) {
			GeometryException.raise("query point " + queryPoint
					+ " not to left of search edge " + aEdge);
		}

		// Construct segment from midpoint of the initial edge and the query
		// point; we'll try to follow this line
		Point bearingStartPoint = MyMath.interpolateBetween(
				aEdge.sourceVertex(), aEdge.destVertex(), .5f);
		if (s.isActive()) {
			s.openLayer(BGND_ELEMENT_BEARING_LINE);
			s.setLineWidth(2);
			s.setColor(Color.LTGRAY);
			s.plotRay(bearingStartPoint, queryPoint);
			s.closeLayer();
		}

		int maxIterations = mContext.numVertices();
		while (true) {
			if (maxIterations-- == 0)
				GeometryException.raise("too many iterations");

			if (s.isActive()) {
				mSearchHistory.add(aEdge);
			}

			bEdge = aEdge.nextFaceEdge();
			cEdge = bEdge.nextFaceEdge();
			if (oppositeVertex(bEdge) != aEdge.sourceVertex())
				GeometryException.raise("search edge not adjacent to triangle "
						+ aEdge);
			if (s.step())
				s.show("Current search triangle" + plot(aEdge)
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
				if (MyMath.sideOfLine(bearingStartPoint, queryPoint, farPoint) > 0) {
					aEdge = bEdge.dual();
				} else {
					aEdge = cEdge.dual();
				}
			}
		}
		if (s.bigStep())
			s.show("Triangle containing query point"
					+ plot(aEdge.sourceVertex(), bEdge.sourceVertex(),
							cEdge.sourceVertex()));
		if (s.isActive()) {
			s.removeLayer(BGND_ELEMENT_SEARCH_HISTORY);
			s.removeLayer(BGND_ELEMENT_BEARING_LINE);
		}
		s.popActive();

		return aEdge;
	}

	private int determineOptimalSampleSize() {
		int populationSize = mContext.numVertices();
		// Calculate log_2 (population)
		int optimalSize = (int) (Math.log(populationSize) / .693f);
		if (s.step())
			s.show("Population:" + populationSize + " Optimal:" + optimalSize);
		return optimalSize;
	}

	private boolean pointLeftOfEdge(Point query, Edge edge) {
		Point p1 = edge.sourceVertex();
		Point p2 = edge.destVertex();
		return sideOfLine(p1, p2, query) > 0;
	}

	private void constructMesh(Mesh c, Rect boundingRect) {
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

		// Mark these four edges (not their duals) as horizon edges
		c.addEdge(v0, v1).addFlags(EDGEFLAG_HORIZON);
		c.addEdge(v1, v2).addFlags(EDGEFLAG_HORIZON);
		c.addEdge(v2, v3).addFlags(EDGEFLAG_HORIZON);
		c.addEdge(v3, v0).addFlags(EDGEFLAG_HORIZON);

		c.addEdge(v0, v2);

		mContext = c;
	}

	// Convenience methods for using stepper

	private String plot(Point a, Point b, Point c) {
		s.setLineWidth(2);
		s.setColor(Color.RED);
		return s.plotLine(a, b) + s.plotLine(b, c) + s.plotLine(c, a);
	}

	private String plot(Edge edge) {
		return plot(edge.sourceVertex(), edge.destVertex());
	}

	private String plot(Point p1, Point p2) {
		s.setLineWidth(2);
		s.setColor(Color.RED);
		return s.plotRay(p1, p2);
	}

	private String plot(Point v) {
		s.setColor(Color.RED);
		return s.plot(v);
	}

	private Point faceCentroid(Edge faceEdge) {
		Point p = new Point(faceEdge.sourceVertex());
		p.add(faceEdge.destVertex());
		p.add(oppositeVertex(faceEdge));
		p.setTo(p.x / 3, p.y / 3);
		return p;
	}

	private static AlgorithmStepper s;

	private Random mRandom;
	private Mesh mContext;
	private ArrayList<Edge> mSearchHistory;
	private ArrayList<Edge> mHoleEdges = new ArrayList();
	private ArrayList<Vertex> mSamples = new ArrayList();
}
