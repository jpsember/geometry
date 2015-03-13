package com.js.basic;

import static com.js.basic.Tools.*;

import android.graphics.Matrix;

public class Point {

	public static final Point ZERO = new Point();

	public Point() {
	}

	public final void apply(Matrix m) {
		float[] f = new float[9];
		m.getValues(f);
		float newX = f[0] * x + f[1] * y + f[2];
		float newY = f[3] * x + f[4] * y + f[5];
		this.x = newX;
		this.y = newY;
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

	public final void clear() {
		setTo(0, 0);
	}

	public final void setTo(final Point source) {
		setTo(source.x, source.y);
	}

	public final float magnitude() {
		return (x * x) + (y * y);
	}

	public final void add(Point point) {
		x += point.x;
		y += point.y;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(d(x));
		sb.append(' ');
		sb.append(d(y));
		return sb.toString();
	}

	public String toStringAsInts() {
		StringBuilder sb = new StringBuilder();
		sb.append(d((int) x, 4));
		sb.append(' ');
		sb.append(d((int) y, 4));
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

	public float x;
	public float y;

}
