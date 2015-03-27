package com.js.geometryapp;

import com.js.geometry.Disc;
import com.js.basic.Point;
import com.js.geometry.Polygon;
import com.js.basic.Rect;
import static com.js.basic.Tools.*;

/**
 * Instances of this class should be considered immutable (once constructed by
 * the stepper framework)
 */
public class AlgorithmInput {

	public AlgorithmInput(Rect algorithmRect) {
		this.algorithmRect = algorithmRect;
	}

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
    return polygons[myMod(index, polygons.length)];
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
    return points[myMod(index, points.length)];
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
    return discs[myMod(index, discs.length)];
	}

	public Rect algorithmRect;
	public Point[] points;
	public Polygon[] polygons;
	public Disc[] discs;
}
