package com.js.geometry;

import static com.js.basic.Tools.*;

public final class Edge {

	@Override
	public String toString() {
		if (mDual == null)
			return "(no dual)";

		StringBuilder sb = new StringBuilder();

		Vertex sourceVert = mDual.mDestVertex;
		Vertex destVert = this.mDestVertex;
		sb.append("e ");
		sb.append(sourceVert.point().dumpUnlabelled());
		sb.append(" ");
		sb.append(destVert.point().dumpUnlabelled());
		sb.append(" ");
		sb.append(nameOf(sourceVert));
		sb.append(" --> ");
		sb.append(nameOf(destVert));
		sb.append(" ");
		return sb.toString();
	}

	public Vertex sourceVertex() {
		ASSERT(this.mDual != null);
		return this.mDual.mDestVertex;
	}

	public boolean visited() {
		return 0 != (mFlags & FLAG_VISITED);
	}

	public void setVisited(boolean v) {
		if (v)
			mFlags |= FLAG_VISITED;
		else
			mFlags &= ~FLAG_VISITED;
	}

	public boolean deleted() {
		return 0 != (mFlags & FLAG_DELETED);
	}

	public void setDeleted(boolean d) {
		if (d)
			mFlags |= FLAG_DELETED;
		else
			mFlags &= ~FLAG_DELETED;
	}

	public void clearFlags(int f) {
		mFlags &= ~f;
	}

	public Edge dual() {
		return mDual;
	}

	public Edge nextEdge() {
		return mNextEdge;
	}

	public Edge prevEdge() {
		return mPrevEdge;
	}

	public Vertex destVertex() {
		return mDestVertex;
	}

	public boolean isPolygon() {
		return 0 != (mFlags & FLAG_POLYGON);
	}

	public void setAngle(float angle) {
		mPseudoAngle = angle;
	}

	public void setNextEdge(Edge edge) {
		mNextEdge = edge;
	}

	public void setPrevEdge(Edge edge) {
		mPrevEdge = edge;
	}

	public float angle() {
		return mPseudoAngle;
	}

	public void clearFlags() {
		mFlags = 0;
	}

	public void setDestVertex(Vertex v) {
		mDestVertex = v;
	}

	public void setDual(Edge d) {
		mDual = d;
	}

	public static final int FLAG_INVISIBLE = 1 << 28;

	/**
	 * The dual to a polygon edge won't have this flag set
	 */
	public static final int FLAG_POLYGON = 1 << 29;
	public static final int FLAG_VISITED = 1 << 30;
	public static final int FLAG_DELETED = 1 << 31;

	private Vertex mDestVertex;
	private Edge mDual;
	private Edge mNextEdge;
	private Edge mPrevEdge;
	private float mPseudoAngle;
	private int mFlags;

}
