package com.js.geometry;

import static com.js.basic.Tools.*;
import android.graphics.Matrix;

public final class MyMath {

	public static final float MAXVALUE = 1e12f;

	public static float PI = (float) Math.PI;
	/**
	 * Multiplicative factor that converts angles expressed in degrees to
	 * radians
	 */
	public static final float M_DEG = PI / 180.0f;

	public static final float PSEUDO_ANGLE_RANGE = 8;
	public static final float PSEUDO_ANGLE_RANGE_12 = (PSEUDO_ANGLE_RANGE * .5f);
	public static final float PSEUDO_ANGLE_RANGE_14 = (PSEUDO_ANGLE_RANGE * .25f);
	public static final float PSEUDO_ANGLE_RANGE_34 = (PSEUDO_ANGLE_RANGE * .75f);
	public static final float PERTURB_AMOUNT_DEFAULT = .5f;

	public static int myMod(int value, int divisor) {
		ASSERT(divisor > 0);
		int k = value % divisor;
		if (value < 0) {
			if (k != 0)
				k = divisor + k;
		}
		return k;
	}

	public static float myMod(float value, float divisor) {
		ASSERT(divisor > 0);
		float scaledValue = value / divisor;
		scaledValue -= Math.floor(scaledValue);
		return scaledValue * divisor;
	}

	public static float squaredMagnitudeOfRay(float x, float y) {
		return (x * x) + (y * y);
	}

	public static float magnitudeOfRay(float x, float y) {
		return (float) Math.sqrt(squaredMagnitudeOfRay(x, y));
	}

	public static float sin(float angle) {
		return (float) Math.sin(angle);
	}

	public static float cos(float angle) {
		return (float) Math.cos(angle);
	}

	/**
	 * Snap a scalar to a grid
	 * 
	 * @param n
	 *            scalaar
	 * @param size
	 *            size of grid cells (assumed to be square)
	 * @return point snapped to nearest cell corner
	 */
	public static float snapToGrid(float n, float size) {
		return size * Math.round(n / size);
	}

	public static float interpolateBetweenScalars(float v1, float v2,
			float parameter) {
		return (v1 * (1 - parameter)) + v2 * parameter;
	}

	public static float normalizeAngle(float a) {
		float kRange = (float) Math.PI * 2;
		float an = myMod(a, kRange);
		if (an >= kRange / 2)
			an -= kRange;
		return an;
	}

	public static float interpolateBetweenAngles(float a1, float a2,
			float parameter) {
		float aDiff = normalizeAngle(a2 - a1) * parameter;
		return normalizeAngle(a1 + aDiff);
	}

	public static int clamp(int value, int min, int max) {
		if (value < min)
			value = min;
		else if (value > max)
			value = max;
		return value;
	}

	public static float clamp(float value, float min, float max) {
		if (value < min)
			value = min;
		else if (value > max)
			value = max;
		return value;
	}

	public static Point clampPointToRect(Point pt, Rect r) {
		float x = clamp(pt.x, r.x, r.x + r.width);
		float y = clamp(pt.y, r.y, r.y + r.height);
		return new Point(x, y);
	}

	public static float pseudoAngle(float x, float y) {
		// For consistency, always insist that y is nonnegative
		boolean negateFlag = (y <= 0);
		if (negateFlag)
			y = -y;

		float ret;
		if (y > Math.abs(x)) {
			float rat = x / y;
			ret = PSEUDO_ANGLE_RANGE_14 - rat;
		} else {
			if (x == 0)
				throw new GeometryException("degenerate ray");
			float rat = y / x;
			if (x < 0) {
				ret = PSEUDO_ANGLE_RANGE_12 + rat;
			} else {
				ret = rat;
			}
		}
		if (negateFlag)
			ret = -ret;

		return ret;
	}

	public static double pseudoAngleOfSegment(Point s1, Point s2) {
		return pseudoAngle(s2.x - s1.x, s2.y - s1.y);
	}

	public static double normalizePseudoAngle(double a) {
		double b = a;
		if (b < -PSEUDO_ANGLE_RANGE_12) {
			b += PSEUDO_ANGLE_RANGE;
			if (b < -PSEUDO_ANGLE_RANGE_12) {
				throw new GeometryException("range error:" + b);
			}
		} else if (b >= PSEUDO_ANGLE_RANGE_12) {
			b -= PSEUDO_ANGLE_RANGE;
			if (b >= PSEUDO_ANGLE_RANGE_12) {
				throw new GeometryException("range error:" + b);
			}
		}
		return b;
	}

	public static float polarAngleOfSegment(Point s1, Point s2) {
		return (float) Math.atan2(s2.y - s1.y, s2.x - s1.x);
	}

	// TODO: should these be MyMath functions, and not Point functions?
	public static Point add(Point a, Point b) {
		return new Point(a.x + b.x, a.y + b.y);
	}

	public static Point subtract(Point a, Point b) {
		return new Point(a.x - b.x, a.y - b.y);
	}

	//
	public static Point interpolateBetween(Point s1, Point s2, float parameter) {
		return new Point(
				MyMath.interpolateBetweenScalars(s1.x, s2.x, parameter),
				MyMath.interpolateBetweenScalars(s1.y, s2.y, parameter));
	}

	public double polarAngle(Point point) {
		return Math.atan2(point.y, point.x);
	}

	public static Point pointOnCircle(Point origin, float angle, float radius) {
		return new Point(origin.x + radius * (float) Math.cos(angle), origin.y
				+ radius * (float) Math.sin(angle));
	}

	public static double dotProduct(Point s1, Point s2) {
		return s1.x() * s2.x() + s1.y() * s2.y();
	}

	public static float squaredDistanceBetween(Point s1, Point s2) {
		return squaredMagnitudeOfRay(s2.x - s1.x, s2.y - s1.y);
	}

	public static float distanceBetween(Point s1, Point s2) {
		return (float) Math
				.sqrt(squaredMagnitudeOfRay(s2.x - s1.x, s2.y - s1.y));
	}

	public static String dumpMatrix(float[] values, int rows, int columns,
			boolean rowMajorOrder) {
		StringBuilder sb = new StringBuilder();
		for (int row = 0; row < rows; row++) {
			sb.append("[ ");
			for (int col = 0; col < columns; col++) {
				int index = rowMajorOrder ? row * columns + col : col * rows
						+ row;
				sb.append(f(values[index], 3, 5));
				sb.append(' ');
			}
			sb.append("]\n");
		}
		return sb.toString();
	}

	public static String dumpMatrix(Matrix m) {
		if (m == null)
			return "<null>";
		float v[] = new float[9];
		m.getValues(v);
		return dumpMatrix(v, 3, 3, true);
	}

	public static Point pointBesideSegment(Point s1, Point s2, float distance) {
		return pointBesideSegment(s1, s2, distance, .5f);
	}

	public static Point pointBesideSegment(Point s1, Point s2, float distance,
			float t) {
		Point m = interpolateBetween(s1, s2, t);
		float a = polarAngleOfSegment(s1, s2);
		return pointOnCircle(m, a - 90 * M_DEG, distance);
	}

	public static Matrix calcRectFitRectTransform(Rect originalRect,
			Rect fitRect) {
		return calcRectFitRectTransform(originalRect, fitRect, true);
	}

	public static Matrix calcRectFitRectTransform(Rect originalRect,
			Rect fitRect, boolean preserveAspectRatio) {
		float scaleX = fitRect.width / originalRect.width;
		float scaleY = fitRect.height / originalRect.height;
		if (preserveAspectRatio) {
			scaleX = Math.min(scaleX, scaleY);
			scaleY = scaleX;
		}
		float unusedX = fitRect.width - scaleX * originalRect.width;
		float unusedY = fitRect.height - scaleY * originalRect.height;

		Matrix tTranslate1 = new Matrix();
		tTranslate1.setTranslate(-originalRect.x, -originalRect.y);
		Matrix tScale = new Matrix();
		tScale.setScale(scaleX, scaleY);
		Matrix tTranslate2 = new Matrix();
		tTranslate2.setTranslate(fitRect.x + unusedX / 2, fitRect.y + unusedY
				/ 2);

		Matrix work = new Matrix(tTranslate1);
		work.postConcat(tScale);
		work.postConcat(tTranslate2);
		return work;
	}

	public static float sideOfLine(Point ln0, Point ln1, Point pt) {
		float area = (ln1.x - ln0.x) * (pt.y - ln0.y) - (pt.x - ln0.x)
				* (ln1.y - ln0.y);
		return area;
	}
}
