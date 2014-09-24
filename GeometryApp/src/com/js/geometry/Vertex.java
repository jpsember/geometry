package com.js.geometry;

import static com.js.basic.Tools.*;

public final class Vertex extends Point {
	public static final int FLAG_VISITED = 1 << 31;

	public Vertex(int index, Point location) {
		super(location);
		mIndex = index;
	}

	void setIndex(int index) {
		mIndex = index;
	}

	int index() {
		return mIndex;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("v ");
		sb.append(dumpUnlabelled());
		sb.append(nameOf(this));
		return sb.toString();
	}

	public boolean visited() {
		return hasFlags(FLAG_VISITED);
	}

	public void setVisited(boolean v) {
		if (v)
			mFlags |= FLAG_VISITED;
		else
			mFlags &= ~FLAG_VISITED;
	}

	public void addFlags(int f) {
		mFlags |= f;
	}

	public void clearFlags() {
		mFlags = 0;
	}

	public void clearFlags(int f) {
		mFlags &= ~f;
	}

	public Edge edges() {
		return mEdges;
	}

	void setEdges(Edge edge) {
		mEdges = edge;
	}

	public int flags() {
		return mFlags;
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

	/**
	 * Remove a half edge leaving this source vertex; ignores its dual
	 */
	void removeEdge(Edge edge) {
		Edge nextEdge = edge.nextEdge();
		Edge prevEdge = edge.prevEdge();
		if (edge == mEdges) {
			if (nextEdge == edge) {
				// This is the only edge leaving the vertex
				mEdges = null;
				return;
			} else {
				mEdges = nextEdge;
			}
		}
		nextEdge.setPrevEdge(prevEdge);
		prevEdge.setNextEdge(nextEdge);
	}

	private Edge mEdges;
	private int mFlags;

	// index of vertex within vertex array
	private int mIndex;
}
