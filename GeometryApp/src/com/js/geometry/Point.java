package com.js.geometry;

import static com.js.basic.Tools.*;
import android.graphics.Matrix;

public class Point {

	public static final Point ZERO = new Point();
	
	public Point() {
	}

	public final void apply(Matrix m) {
		float[] f = new float[9];
		m.getValues(f);
		x = f[0] * x + f[1] * y + f[2];
		y = f[3] * x + f[4] * y + f[5];
	}

	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Point(Point point) {
		this(point.x, point.y);
	}

	public final float x() {
		return x;
	}

	public final float y() {
		return y;
	}

	public final void setTo(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public final float magnitude() {
		return MyMath.magnitudeOfRay(x, y);
	}

	public final float pseudoAngle() {
		return MyMath.pseudoAngle(x, y);
	}

	public final void add(Point point) {
		x += point.x;
		y += point.y;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(f(x));
		sb.append(' ');
		sb.append(f(y));
		return sb.toString();
	}

	public String toStringAsInts() {
		StringBuilder sb = new StringBuilder();
		sb.append(f((int) x, 4));
		sb.append(' ');
		sb.append(f((int) y, 4));
		return sb.toString();
	}

	public String dumpUnlabelled() {
		StringBuilder sb = new StringBuilder();
		sb.append(f(x));
		sb.append(' ');
		sb.append(f(y));
		sb.append(' ');
		return sb.toString();
	}

	public float x;
	public float y;
}
