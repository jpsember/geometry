package com.js.geometry;

import static com.js.basic.Tools.*;

public class Disc {

	public Disc(Point origin, float radius) {
		mOrigin = new Point(origin);
		mRadius = radius;
	}

	public Point getOrigin() {
		return mOrigin;
	}

	public float getRadius() {
		return mRadius;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(d(mOrigin.x));
		sb.append(' ');
		sb.append(d(mOrigin.y));
		sb.append(' ');
		sb.append(d(getRadius()));
		return sb.toString();
	}

	private Point mOrigin;
	private float mRadius;
}
