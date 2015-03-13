package com.js.gest;

import com.js.basic.Point;
import static com.js.basic.Tools.*;

/**
 * A StrokePoint represents a discrete position along a Stroke
 */
class StrokePoint {

	public StrokePoint(float time, Point point) {
		mTime = time;
		mPoint = new Point(point);
	}

	public StrokePoint(StrokePoint source) {
		this(source.mTime, source.mPoint);
	}

	public Point getPoint() {
		return mPoint;
	}

	public float getTime() {
		return mTime;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(d(getTime()));
		sb.append(getPoint());
		return sb.toString();
	}

	private Point mPoint;
	private float mTime;
}
