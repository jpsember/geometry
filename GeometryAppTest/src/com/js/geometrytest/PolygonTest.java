package com.js.geometrytest;

import com.js.geometry.Polygon;
import com.js.testUtils.MyTestCase;
import static com.js.basic.Tools.*;

public class PolygonTest extends MyTestCase {

	private Polygon mPolygon;

	public void testPolygonConvexityTest() {
		doNothing();
		int YES = 1;
		int NO = 0;
		final int s[] = { Polygon.TESTPOLY_SQUARE, YES,
				Polygon.TESTPOLY_DIAMOND, YES,
				Polygon.TESTPOLY_LARGE_RECTANGLE, YES,
				Polygon.TESTPOLY_TRIANGLE_HORZ_BASE, YES,
				Polygon.TESTPOLY_TRIANGLE_SLOPED_BASE, YES,
				Polygon.TESTPOLY_CONCAVE_BLOB, NO,
				Polygon.TESTPOLY_Y_EQUALS_X_SQUARED, YES,
				Polygon.TESTPOLY_MERGESPLIT, NO,
				Polygon.TESTPOLY_VULCAN_SYMBOL, NO,
				Polygon.TESTPOLY_LARGE_CONVEX, YES, };
		for (int i = 0; i < s.length; i += 2) {
			buildTestPolygon(s[i]);
			assertEquals(s[i + 1] != 0, mPolygon.isConvex());
		}
	}

	private void buildTestPolygon(int variety) {
		mPolygon = Polygon.testPolygon(variety);
	}

}
