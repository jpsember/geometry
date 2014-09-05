package com.js.geometry;

import static com.js.basic.Tools.*;

public final class Vertex {
	public static final int FLAG_INVISIBLE = 1 << 28;
	public static final int FLAG_AT_INFINITY = 1 << 29;
	public static final int FLAG_VISITED = 1 << 30;
	public static final int FLAG_DELETED = 1 << 31;

	public Point point() {
		return mPt;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("v ");
		sb.append(mPt.dumpUnlabelled());
		sb.append(nameOf(this));
		return sb.toString();
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

	public Edge edges() {
		return mEdges;
	}

	public void setEdges(Edge edge) {
		mEdges = edge;
	}

	private Point mPt;
	private Edge mEdges;
	private int mFlags;

}
