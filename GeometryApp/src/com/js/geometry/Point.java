package com.js.geometry;

import static com.js.geometry.MyMath.*;
import static com.js.basic.Tools.*;

public final class Point {

	public Point() {
	}

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double magnitude() {
		return magnitudeOfRay(x, y);
	}

	public double pseudoAngle() {
		return MyMath.pseudoAngle(x, y);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(f(x));
		sb.append(' ');
		sb.append(f(y));
		return sb.toString();
	}

	public double x;
	public double y;
}
