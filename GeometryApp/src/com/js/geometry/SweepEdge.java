package com.js.geometry;

import static com.js.basic.Tools.*;

public class SweepEdge {

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
	 *            if true, clamps sweep line to lie within edge's vertical range
	 * @return intersection point
	 */
	public Point positionOnSweepLine(float sweepLinePosition,
			Mesh context, boolean clampWithinRange) {
		Edge edge = mPolygonEdge;

		Point v1 = edge.sourceVertex();
		Point v2 = edge.destVertex();

		if (clampWithinRange) {
			float y0 = Math.min(v1.y, v2.y);
			float y1 = Math.max(v1.y, v2.y);
			sweepLinePosition = MyMath.clamp(sweepLinePosition, y0, y1);
		}

		Point ipt = MyMath.segHorzLineIntersection(v1, v2, sweepLinePosition,
				null);

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
