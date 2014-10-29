package com.js.geometry;

import static com.js.basic.Tools.*;

public final class Edge implements Renderable {

	public static final int FLAG_VISITED = 1 << 31;
	public static final int FLAG_DELETED = 1 << 30;

	/**
	 * The dual to a polygon edge won't have this flag set
	 */
	public static final int FLAG_POLYGON = 1 << 29;

	@Override
	public String toString() {
		if (mDual == null)
			return "(no dual)";

		StringBuilder sb = new StringBuilder();

		Vertex sourceVert = mDual.mDestVertex;
		Vertex destVert = this.mDestVertex;
		sb.append(sourceVert.dumpUnlabelled());
		sb.append(" ");
		sb.append(destVert.dumpUnlabelled());
		sb.append(" ");
		sb.append(nameOf(sourceVert, false));
		sb.append(" --> ");
		sb.append(nameOf(destVert, false));
		sb.append(" ");
		// sb.append(fh(mFlags) + " ");

		return sb.toString();
	}

	public Vertex sourceVertex() {
		return this.mDual.mDestVertex;
	}

	public boolean visited() {
		return hasFlags(FLAG_VISITED);
	}

	public boolean deleted() {
		return hasFlags(FLAG_DELETED);
	}

	/**
	 * Determine if flags contains all of a particular subset of flags
	 * 
	 * @param flags
	 *            subset of flags to test
	 * @return true if every bit in flags is set in this edge
	 */
	public boolean hasFlags(int flags) {
		return (mFlags & flags) == flags;
	}

	public int flags() {
		return mFlags;
	}

	public void setVisited(boolean v) {
		if (v)
			mFlags |= FLAG_VISITED;
		else
			mFlags &= ~FLAG_VISITED;
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
		return hasFlags(FLAG_POLYGON);
	}

	/**
	 * Find the next edge in the CCW face this edge bounds
	 */
	public Edge nextFaceEdge() {
		return dual().prevEdge();
	}

	/**
	 * Find the previous edge in the CCW face this edge bounds
	 */
	public Edge prevFaceEdge() {
		return nextEdge().dual();
	}

	void setAngle(float angle) {
		mPseudoAngle = angle;
	}

	void setNextEdge(Edge edge) {
		mNextEdge = edge;
	}

	void setPrevEdge(Edge edge) {
		mPrevEdge = edge;
	}

	public float angle() {
		return mPseudoAngle;
	}

	public void clearFlags() {
		mFlags = 0;
	}

	void setDestVertex(Vertex v) {
		mDestVertex = v;
	}

	void setDual(Edge d) {
		mDual = d;
	}

	public void addFlags(int f) {
		mFlags |= f;
	}

	@Override
	public void render(AlgorithmStepper stepper) {
		stepper.plot(Segment.directed(sourceVertex(), destVertex()));
	}

	private Vertex mDestVertex;
	private Edge mDual;
	private Edge mNextEdge;
	private Edge mPrevEdge;
	private float mPseudoAngle;
	private int mFlags;

}
