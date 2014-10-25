package com.js.geometryapp;

import com.js.geometry.Disc;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;

public class AlgorithmInput {

	/**
	 * Get (first) polygon from the input
	 * 
	 * @param defaultIfNone
	 *            if array is empty, returns this instead
	 */
	public Polygon getPolygon(Polygon defaultIfNone) {
		return getPolygon(0, defaultIfNone);
	}

	/**
	 * Get a polygon from the input
	 * 
	 * @param index
	 *            index to use; taken modulo the array size
	 * @param defaultIfNone
	 *            if array is empty, returns this instead
	 */
	public Polygon getPolygon(int index, Polygon defaultIfNone) {
		if (polygons.length == 0)
			return defaultIfNone;
		return polygons[MyMath.myMod(index, polygons.length)];
	}

	/**
	 * Get (first) point from the input
	 * 
	 * @param defaultIfNone
	 *            if array is empty, returns this instead
	 */
	public Point getPoint(Point defaultIfNone) {
		return getPoint(0, defaultIfNone);
	}

	/**
	 * Get a point from the input
	 * 
	 * @param index
	 *            index to use; taken modulo the array size
	 * @param defaultIfNone
	 *            if array is empty, returns this instead
	 */
	public Point getPoint(int index, Point defaultIfNone) {
		if (points.length == 0)
			return defaultIfNone;
		return points[MyMath.myMod(index, points.length)];
	}

	/**
	 * Get (first) disc from the input
	 * 
	 * @param defaultIfNone
	 *            if array is empty, returns this instead
	 */
	public Disc getDisc(Disc defaultIfNone) {
		return getDisc(0, defaultIfNone);
	}

	/**
	 * Get a disc from the input
	 * 
	 * @param index
	 *            index to use; taken modulo the array size
	 * @param defaultIfNone
	 *            if array is empty, returns this instead
	 */
	public Disc getDisc(int index, Disc defaultIfNone) {
		if (discs.length == 0)
			return defaultIfNone;
		return discs[MyMath.myMod(index, discs.length)];
	}

	public Point[] points;
	public Polygon[] polygons;
	public Disc[] discs;
}
