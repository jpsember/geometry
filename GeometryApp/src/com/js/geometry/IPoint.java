package com.js.geometry;

import static com.js.basic.Tools.*;

public final class IPoint {

	public IPoint() {
	}

	public IPoint(float x, float y) {
		this.x = (int) x;
		this.y = (int) y;
	}

	public IPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(f(x));
		sb.append(' ');
		sb.append(f(y));
		return sb.toString();
	}

	public String dumpUnlabelled() {
		StringBuilder sb = new StringBuilder();
		sb.append(d(x));
		sb.append(' ');
		sb.append(d(y));
		sb.append(' ');
		return sb.toString();
	}

	public int x;
	public int y;
}
