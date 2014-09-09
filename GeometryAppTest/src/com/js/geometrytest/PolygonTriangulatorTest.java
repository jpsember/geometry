package com.js.geometrytest;

import com.js.geometry.GeometryContext;
import com.js.geometry.Polygon;
import com.js.testUtils.IOSnapshot;
import com.js.testUtils.MyTestCase;
import static com.js.basic.Tools.*;

public class PolygonTriangulatorTest extends MyTestCase {

	private GeometryContext mContext;
	private Polygon mPolygon;

	private GeometryContext context() {
		if (mContext == null)
			mContext = new GeometryContext(42);
		return mContext;
	}

	private void buildTestPolygon(int variety) {
		mPolygon = Polygon.testPolygon(context(), variety);
	}

	public void testDragonPolygonConsistent() {
		IOSnapshot.open();
		buildTestPolygon(Polygon.TESTPOLY_DRAGON_X + 5);
		pr(mPolygon);
		IOSnapshot.close();
	}
}
