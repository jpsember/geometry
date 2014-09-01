package com.js.geometry;

import static com.js.basic.Tools.*;
import android.graphics.Matrix;

public class MyMath {

	/**
	 * Multiplicative factor that converts angles expressed in degrees to
	 * radians
	 */
	public static final double M_DEG = Math.PI / 180.0;

	public static final double PSEUDO_ANGLE_RANGE = 8;
	public static final double PSEUDO_ANGLE_RANGE_12 = (PSEUDO_ANGLE_RANGE * .5);
	public static final double PSEUDO_ANGLE_RANGE_14 = (PSEUDO_ANGLE_RANGE * .25);
	public static final double PSEUDO_ANGLE_RANGE_34 = (PSEUDO_ANGLE_RANGE * .75);
	public static final double PERTURB_AMOUNT_DEFAULT = .5;

	public static int myMod(int value, int divisor) {
		ASSERT(divisor > 0);
		int k = value % divisor;
		if (value < 0) {
			if (k != 0)
				k = divisor + k;
		}
		return k;
	}

	public static double myMod(double value, double divisor) {
		ASSERT(divisor > 0);
		double scaledValue = value / divisor;
		scaledValue -= Math.floor(scaledValue);
		return scaledValue * divisor;
	}

	public static double squaredMagnitudeOfRay(double x, double y) {
		return (x * x) + (y * y);
	}

	public static double magnitudeOfRay(double x, double y) {
		return Math.sqrt(squaredMagnitudeOfRay(x, y));
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
	public static double snapToGrid(double n, double size) {
		return size * Math.round(n / size);
	}

	public static double interpolateBetweenScalars(double v1, double v2,
			double parameter) {
		return (v1 * (1 - parameter)) + v2 * parameter;
	}

	public static double normalizeAngle(double a) {
		double kRange = Math.PI * 2;
		double an = myMod(a, kRange);
		if (an >= kRange / 2)
			an -= kRange;
		return an;
	}

	public static double interpolateBetweenAngles(double a1, double a2,
			double parameter) {
		double aDiff = normalizeAngle(a2 - a1) * parameter;
		return normalizeAngle(a1 + aDiff);
	}

	public static int clamp(int value, int min, int max) {
		if (value < min)
			value = min;
		else if (value > max)
			value = max;
		return value;
	}

	public static double clamp(double value, double min, double max) {
		if (value < min)
			value = min;
		else if (value > max)
			value = max;
		return value;
	}

	public static Point clampPointToRect(Point pt, Rect r) {
		double x = clamp(pt.x, r.x, r.x + r.width);
		double y = clamp(pt.y, r.y, r.y + r.height);
		return new Point(x, y);
	}

	public static double pseudoAngle(double x, double y) {
		// For consistency, always insist that y is nonnegative
		boolean negateFlag = (y <= 0);
		if (negateFlag)
			y = -y;

		double ret;
		if (y > Math.abs(x)) {
			double rat = x / y;
			ret = PSEUDO_ANGLE_RANGE_14 - rat;
		} else {
			if (x == 0)
				throw new GeometryException("degenerate ray");
			double rat = y / x;
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
			// TODO: instead of returning NaN, throw exceptions
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

	public static double polarAngleOfSegment(Point s1, Point s2) {
		return Math.atan2(s2.y - s1.y, s2.x - s1.x);
	}

	// TODO: should these be MyMath functions, and not Point functions?
	public static Point add(Point a, Point b) {
		return new Point(a.x + b.x, a.y + b.y);
	}

	public static Point subtract(Point a, Point b) {
		return new Point(a.x - b.x, a.y - b.y);
	}

	//
	public static Point interpolateBetween(Point s1, Point s2, double parameter) {
		return new Point(
				MyMath.interpolateBetweenScalars(s1.x, s2.x, parameter),
				MyMath.interpolateBetweenScalars(s1.y, s2.y, parameter));
	}

	public double polarAngle(Point point) {
		return Math.atan2(point.y, point.x);
	}

	public static Point pointOnCircle(Point origin, double angle, double radius) {
		return new Point(origin.x + radius * Math.cos(angle), origin.y + radius
				* Math.sin(angle));
	}

	public static double dotProduct(Point s1, Point s2) {
		return s1.x() * s2.x() + s1.y() * s2.y();
	}

	public static double squaredDistanceBetween(Point s1, Point s2) {
		return squaredMagnitudeOfRay(s2.x - s1.x, s2.y - s2.y);
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
		float v[] = new float[9];
		m.getValues(v);
		return dumpMatrix(v, 3, 3, true);
	}

}
