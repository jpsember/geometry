package com.js.geometry;

import static com.js.basic.Tools.*;

import java.util.Random;

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

	/**
	 * Test if a value is essentially zero, and raise exception if so
	 * 
	 * @param value
	 */
	public static void testForZero(float value) {
		testForZero(value, 1e-8f);
	}

	/**
	 * Test if a value is essentially zero, and raise exception if so
	 * 
	 * @param value
	 * @param epsilon
	 */
	public static void testForZero(float value, float epsilon) {
		if (Math.abs(value) <= epsilon) {
			GeometryException.raise("Value is very near zero: " + value
					+ " (epsilon " + epsilon + ")");
		}
	}

	/**
	 * Raise exception if value's magnitude exceeds MAXVALUE
	 * 
	 * @param value
	 */
	public static void testForOverflow(float value) {
		if (value > MyMath.MAXVALUE || value < -MyMath.MAXVALUE
				|| Float.isNaN(value)) {
			GeometryException.raise("Value has overflowed: " + value);
		}
	}

	public static int myMod(int value, int divisor) {
		if (divisor <= 0)
			throw new IllegalArgumentException();
		int k = value % divisor;
		if (value < 0) {
			if (k != 0)
				k = divisor + k;
		}
		return k;
	}

	public static float myMod(float value, float divisor) {
		if (divisor <= 0)
			throw new IllegalArgumentException();
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

	public static double pseudoAngleOfSegment(Point s1, Point s2) {
		return pseudoPolarAngle(s2.x - s1.x, s2.y - s1.y);
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

	public static Point add(Point a, Point b) {
		return new Point(a.x + b.x, a.y + b.y);
	}

	public static Point subtract(Point a, Point b) {
		return new Point(a.x - b.x, a.y - b.y);
	}

	public static Point interpolateBetween(Point s1, Point s2, float parameter) {
		return new Point(
				MyMath.interpolateBetweenScalars(s1.x, s2.x, parameter),
				MyMath.interpolateBetweenScalars(s1.y, s2.y, parameter));
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

	public static float pointUnitLineSignedDistance(Point pt, Point s1, Point s2) {
		// Translate so s1 is at origin
		float sx = s2.x - s1.x;
		float sy = s2.y - s1.y;
		float pt_x = pt.x - s1.x;
		float pt_y = pt.y - s1.y;
		return -sy * pt_x + sx * pt_y;
	}

	/**
	 * Calculate point of intersection of line segment with horizontal line
	 * 
	 * @param pt1
	 * @param pt2
	 * @param yLine
	 * @param parameter
	 *            if not null, and intersection point found, parameter of
	 *            intersection returned here
	 * @return point of intersection, or null
	 */
	public static Point segHorzLineIntersection(Point pt1, Point pt2,
			float yLine, float[] parameter) {
		Point ipt = null;

		float denom = pt2.y - pt1.y;
		testForZero(denom);

		float numer = yLine - pt1.y;
		float t = numer / denom;

		if (!(t < 0 || t > 1)) {
			if (parameter != null)
				parameter[0] = t;

			ipt = new Point(pt1.x + (pt2.x - pt1.x) * t, pt1.y + denom * t);
		}
		return ipt;
	}

	public static Point segSegIntersection(Point s1, Point s2, Point t1,
			Point t2, float[] parameters) {
		Point ipt = null;
		do {
			// First see if segment's bounding boxes intersect; if not, no
			// potentially troubling
			// calculations need be performed
			{
				Rect sBounds = Rect.rectContainingPoints(s1, s2);
				Rect tBounds = Rect.rectContainingPoints(t1, t2);
				// Add a bit of overlap to one rect to ensure a clear separation
				float eps = 1e-8f;
				sBounds.inset(-eps, -eps);
				if (!sBounds.intersects(tBounds))
					break;
			}

			float ty = (t2.y - t1.y);
			float sx = (s2.x - s1.x);
			float tx = (t2.x - t1.x);
			float sy = (s2.y - s1.y);

			float denom = ty * sx - tx * sy;

			testForZero(denom);

			float numer1 = tx * (s1.y - t1.y) - ty * (s1.x - t1.x);
			float numer2 = sx * (s1.y - t1.y) - sy * (s1.x - t1.x);

			float ua = numer1 / denom;
			if (ua < 0 || ua > 1)
				break;
			float ub = numer2 / denom;
			if (ub < 0 || ub > 1)
				break;

			if (parameters != null) {
				parameters[0] = ua;
				parameters[1] = ub;
			}
			ipt = new Point(s1.x + ua * sx, s1.y + ua * sy);
		} while (false);
		return ipt;
	}

	public static Point lineLineIntersection(Point s1, Point s2, Point t1,
			Point t2, float[] parameter) {
		Point ipt = null;
		do {
			float ty = (t2.y - t1.y);
			float sx = (s2.x - s1.x);
			float tx = (t2.x - t1.x);
			float sy = (s2.y - s1.y);

			float denom = ty * sx - tx * sy;

			testForZero(denom);

			float numer1 = tx * (s1.y - t1.y) - ty * (s1.x - t1.x);
			float ua = numer1 / denom;
			if (parameter != null) {
				parameter[0] = ua;
			}

			ipt = new Point(s1.x + ua * sx, s1.y + ua * sy);
		} while (false);
		return ipt;
	}

	public static String dumpMatrix(float[] values, int rows, int columns,
			boolean rowMajorOrder) {
		StringBuilder sb = new StringBuilder();
		for (int row = 0; row < rows; row++) {
			sb.append("[ ");
			for (int col = 0; col < columns; col++) {
				int index = rowMajorOrder ? row * columns + col : col * rows
						+ row;
				sb.append(d(values[index], 3, 5));
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

	public static float polarAngleOfSegment(Point s1, Point s2) {
		return polarAngle(s2.x - s1.x, s2.y - s1.y);
	}

	public static float polarAngle(Point ray) {
		return polarAngle(ray.x, ray.y);
	}

	public static float polarAngle(float x, float y) {
		float max = Math.max(Math.abs(x), Math.abs(y));
		if (max <= 1e-8f) {
			GeometryException.raise("Point is too close to origin: " + x + ","
					+ y);
		}
		return (float) Math.atan2(y, x);
	}

	public static float pseudoPolarAngle(Point point) {
		return pseudoPolarAngle(point.x, point.y);
	}

	public static float pseudoPolarAngle(float x, float y) {
		// For consistency, always insist that y is nonnegative
		boolean negateFlag = (y <= 0);
		if (negateFlag)
			y = -y;

		float ret;
		if (y > Math.abs(x)) {
			float rat = x / y;
			ret = PSEUDO_ANGLE_RANGE_14 - rat;
		} else {
			testForZero(x);
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

	public static float pseudoPolarAngleOfSegment(Point s1, Point s2) {
		return pseudoPolarAngle(s2.x - s1.x, s2.y - s1.y);
	}

	public static float normalizePseudoAngle(float a) {
		float b = a;
		if (b < -PSEUDO_ANGLE_RANGE_12) {
			b += PSEUDO_ANGLE_RANGE;

			if ((b < -PSEUDO_ANGLE_RANGE_12)) {
				GeometryException.raise("Cannot normalize " + a);
			}
		} else if (b >= PSEUDO_ANGLE_RANGE_12) {
			b -= PSEUDO_ANGLE_RANGE;
			if ((b >= PSEUDO_ANGLE_RANGE_12)) {
				GeometryException.raise("Cannot normalize " + a);
			}
		}
		return b;
	}

	public static boolean pseudoAngleIsConvex(float angle) {
		return angle > 0;
	}

	public static boolean pseudoAngleIsConvex(float startAngle, float endAngle) {
		return pseudoAngleIsConvex(normalizePseudoAngle(endAngle - startAngle));
	}

	public static float perturb(Random random, float val) {
		final float GRID = PERTURB_AMOUNT_DEFAULT;
		final float NOISE = GRID * .8f;

		float aligned = (float) Math.floor((val / GRID) + .5f);
		float frac = random.nextFloat() * (2 * NOISE) - NOISE;
		float ret = (aligned + frac) * GRID;

		return ret;
	}

	public static void perturb(Random random, Point pt) {
		pt.x = perturb(random, pt.x);
		pt.y = perturb(random, pt.y);
	}

	public static Point randomPointInDisc(Random random, Point origin,
			float radius) {
		float val = radius * (float) Math.sqrt(random.nextFloat());
		return pointOnCircle(origin, random.nextFloat() * PI * 2, val);
	}

}
