package com.js.geometrytest;

import android.graphics.Matrix;

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

}
