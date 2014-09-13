package com.js.geometryapp;

import com.js.geometry.MyMath;
import com.js.geometry.Point;

public class AlgDisplayRay extends AlgDisplayElement {
	public AlgDisplayRay(Point p1, Point p2) {
		mPoint1 = p1;
		mPoint2 = p2;
	}

	@Override
	public void render() {

		extendPolyline(mPoint1);
		extendPolyline(mPoint2);
		renderPolyline();
		float angleOfRay = MyMath.polarAngleOfSegment(mPoint1, mPoint2);

		float arrowHeadAngle = MyMath.PI * .8f;
		float arrowHeadLength = 15.0f;
		Point p3 = MyMath.pointOnCircle(mPoint2, angleOfRay - arrowHeadAngle, arrowHeadLength);
		Point p4 = MyMath.pointOnCircle(mPoint2, angleOfRay + arrowHeadAngle, arrowHeadLength);
		extendPolyline(p3);
		extendPolyline(mPoint2);
		extendPolyline(p4);
		renderPolyline();
	}

	private Point mPoint1, mPoint2;
}
