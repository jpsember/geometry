package com.js.geometrytest;

import android.graphics.Matrix;

import com.js.geometry.GeometryException;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.testUtils.IOSnapshot;
import com.js.testUtils.MyTestCase;
import static com.js.basic.Tools.*;

public class MyMathTest extends MyTestCase {

	public void testCalcFitRectToRectTransform() {
		IOSnapshot.open();

		Rect r1 = new Rect(13, 77, 200, 80);
		Rect r2 = new Rect(53, 92, 50 + 211, 20);
		Matrix m = MyMath.calcRectFitRectTransform(r1, r2);

		Point p1 = r1.bottomLeft();
		Point p2 = r1.topRight();
		p1.apply(m);
		p2.apply(m);
		pr(" r1: " + r1);
		pr(" r2: " + r2);
		Rect r3 = new Rect(p1, p2);
		pr(" centered within new rect: " + r3);

		IOSnapshot.close();
	}

	public void testCalcFitRectToRectTransformWithoutPreservingAspectRatio() {
		IOSnapshot.open();

		Rect r1 = new Rect(13, 77, 200, 80);
		Rect r2 = new Rect(53, 92, 50 + 211, 20);
		Matrix m = MyMath.calcRectFitRectTransform(r1, r2, false);

		Point p1 = r1.bottomLeft();
		Point p2 = r1.topRight();
		p1.apply(m);
		p2.apply(m);
		pr(" r1: " + r1);
		pr(" r2: " + r2);
		Rect r3 = new Rect(p1, p2);
		pr(" centered within new rect: " + r3);

		IOSnapshot.close();
	}

	public void testRectIntersectRect() {
		Rect r[] = { new Rect(0, 0, 100, 200),//
				new Rect(50, 50, 10, 10),//
				new Rect(100, 20, 300, 50),//
				new Rect(-20, -20, 5, 5),//
				new Rect(20, 300, 50, 50),//
		};

		Object s[] = { 0, 1, true,//
				0, 2, false,//
				0, 3, false,//
				0, 4, false,//
		};
		for (int i = 0; i < s.length; i += 3) {
			int i1 = ((Integer) s[i]);
			int i2 = ((Integer) s[i + 1]);
			boolean f = (Boolean) s[i + 2];
			assertEquals(f, r[i1].intersects(r[i2]));
			assertEquals(f, r[i2].intersects(r[i1]));
			assertEquals(true, r[i2].intersects(r[i2]));
		}
	}

	public void testInterpolateBetweenAngles() {
		float angles[] = { //
		//
				0, 160, .5f, 80, //
				0, -160, .5f, -80,//
				170, -160, .5f, -175, //
				-170, -10, .5f, -90,//
				-170, -10, .25f, -130,//
				170, 10, .5f, 90,//
				170, 10, .25f, 130,//
		};
		for (int i = 0; i < angles.length; i += 4) {
			float a1 = angles[i + 0] * MyMath.M_DEG;
			float a2 = angles[i + 1] * MyMath.M_DEG;
			float t = angles[i + 2];
			float exp = angles[i + 3] * MyMath.M_DEG;

			float got = MyMath.interpolateBetweenAngles(a1, a2, t);
			assertEqualsFloat(exp, got, 1e-6);

			a1 -= MyMath.PI * 4;
			a2 += MyMath.PI * 6;
			got = MyMath.interpolateBetweenAngles(a1, a2, t);
			assertEqualsFloat(exp, got, 1e-5);
		}
	}

	public void testQuadraticSolver() {
		float r[] = new float[2];
		float d[] = { 5, -15, -140, -4, 7,//
				0, 3, 21, -7, -7,//
		};
		for (int i = 0; i < d.length; i += 5) {
			MyMath.solveQuadratic(d[i], d[i + 1], d[i + 2], r);
			assertEqualsFloatWithRelativePrecision(d[i + 3], r[0]);
			assertEqualsFloatWithRelativePrecision(d[i + 4], r[1]);
		}
	}

	public void testQuadraticSolverEdgeCases() {
		float r[] = new float[2];
		float x[] = { 1e-4f, 1e2f,//
				1e-4f, 1e3f,//
				1e-4f, 1e4f,//
				1e-4f, 1e5f,//
		};
		for (int i = 0; i < x.length; i += 2) {
			float x1 = Math.min(x[i], x[i + 1]);
			float x2 = Math.max(x[i], x[i + 1]);
			float a = 7;
			float b = (-x1 - x2) * a;
			float c = x1 * x2 * a;
			try {
				MyMath.solveQuadratic(a, b, c, r);
				assertEqualsFloatWithRelativePrecision(x1, r[0]);
				assertEqualsFloatWithRelativePrecision(x2, r[1]);
			} catch (GeometryException e) {
			}
		}
	}

	public void testQuadraticSolverFails() {
		float d[] = { 3, 0, 2, //
		};
		for (int i = 0; i < d.length; i += 3) {
			try {
				MyMath.solveQuadratic(d[i], d[i + 1], d[i + 2], null);
				fail();
			} catch (GeometryException e) {
			}
		}
	}
}
