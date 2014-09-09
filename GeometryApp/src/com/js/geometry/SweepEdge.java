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

	public Point positionOnSweepLine(float sweepLinePosition,
			GeometryContext context) {
		Edge edge = mPolygonEdge;

		Point ipt = context.segHorzLineIntersection(edge.destVertex().point(),
				edge.sourceVertex().point(), sweepLinePosition);

		if (context.checkError(ipt == null)) {
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
