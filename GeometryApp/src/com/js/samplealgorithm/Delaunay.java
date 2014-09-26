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
	private static final String BGND_ELEMENT_QUERY_POINT = "20";
	private static final String BGND_ELEMENT_SEARCH_HISTORY = "12";
	private static final String BGND_ELEMENT_BEARING_LINE = "10";
	private static final int COLOR_DARKGREEN = Color.argb(255, 30, 128, 30);

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
			show("*Add point " + point);

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
		throw new UnsupportedOperationException();
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
			show("*Partitioned triangle" + plot(ad) + plot(bd) + plot(cd));

		mActiveDetailName = DETAIL_SWAPS;
		swapTest(abEdge, v);
		swapTest(bcEdge, v);
		swapTest(caEdge, v);
		mActiveDetailName = null;

		if (update())
			show("done insertion");

		return v;
	}

	private void swapTest(Edge abEdge, Vertex p) {
		Edge baEdge = abEdge.dual();
		Edge awEdge = baEdge.nextFaceEdge();
		Vertex farVertex = awEdge.destVertex();
		if (update())
			show("SwapTest" + plot(abEdge) + plot(p) + plot(farVertex));
		if (!pointLeftOfEdge(farVertex, baEdge)) {
			if (update())
				show("boundary edge detected" + plot(baEdge) + plot(farVertex));
			return;
		}

		Point a = abEdge.sourceVertex();
		Point b = farVertex;
		Point c = abEdge.destVertex();
		Point d = p;

		double determinant;
		{

			double c11 = a.x - d.x;
			double c12 = a.y - d.y;
			double c13 = c11 * c11 + c12 * c12;
			double c21 = b.x - d.x;
			double c22 = b.y - d.y;
			double c23 = c21 * c21 + c22 * c22;
			double c31 = c.x - d.x;
			double c32 = c.y - d.y;
			double c33 = c31 * c31 + c32 * c32;

			determinant = c11 * (c22 * c33 - c32 * c23) - c12
					* (c21 * c33 - c31 * c23) + c13 * (c21 * c32 - c31 * c22);

			// Choose an epsilon value that is related to the bounding box of
			// the points
			float sx = Math.abs(a.x - b.x);
			float sx2 = Math.abs(a.x - c.x);
			if (sx2 > sx)
				sx = sx2;
			sx2 = Math.abs(a.y - b.y);
			if (sx2 > sx)
				sx = sx2;
			sx2 = Math.abs(a.y - c.y);
			if (sx2 > sx)
				sx = sx2;

			float epsilon = (sx * sx) * 1e-3f;
			if (update())
				show("determinant " + determinant);
			mContext.testForZero((float) determinant, epsilon);
		}
		if (determinant > 0) {
			if (update())
				show("*flipping edge" + plot(abEdge) + plot(p, farVertex));

			mContext.deleteEdge(abEdge);
			Edge pw = mContext.addEdge(p, farVertex);
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
			show("*Finding triangle containing point");

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
			show("*Found triangle containing query point"
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
}
