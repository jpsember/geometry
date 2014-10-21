package com.js.geometrytest;

import com.js.geometry.Mesh;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.testUtils.IOSnapshot;
import com.js.testUtils.MyTestCase;
import static com.js.basic.Tools.*;

public class PolygonTriangulatorTest extends MyTestCase {

	private Mesh mMesh;
	private Polygon mPolygon;
	private PolygonTriangulator mTriangulator;

	private PolygonTriangulator triangulate() {
		mTriangulator = PolygonTriangulator.triangulator(null, mesh(),
				mPolygon);
		mTriangulator.triangulate();
		return mTriangulator;
	}

	private Mesh mesh() {
		if (mMesh == null)
			mMesh = new Mesh();
		return mMesh;
	}

	private void buildTestPolygon(int variety) {
		mPolygon = Polygon.testPolygon(variety);
	}

	public void testDragonPolygonConsistent() {
		IOSnapshot.open();
		buildTestPolygon(Polygon.TESTPOLY_DRAGON_X + 5);
		pr(mPolygon);
		IOSnapshot.close();
	}

	public void testTriangulateSmallPolygon() {
		IOSnapshot.open();
		buildTestPolygon(Polygon.TESTPOLY_DRAGON_X + 1);

		pr(mPolygon);
		pr("\nTriangulated:\n");

		triangulate();

		pr(mesh());
		IOSnapshot.close();
	}

	public void testTriangulateLargePolygon() {
		IOSnapshot.open();
		buildTestPolygon(Polygon.TESTPOLY_DRAGON_X + 6);

		pr(mPolygon);
		pr("\nTriangulated:\n");

		triangulate();

		pr(mesh());
		IOSnapshot.close();
	}

}
