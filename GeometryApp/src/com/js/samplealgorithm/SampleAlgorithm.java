package com.js.samplealgorithm;

import android.opengl.GLSurfaceView;

import com.js.geometry.GeometryContext;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgorithmStepper;

public class SampleAlgorithm implements AlgorithmStepper.Delegate {


	public void setView(GLSurfaceView view) {
		mView = view;
	}

	@Override
	public void runAlgorithm() {

		// To synchronize between the UI (algorithm) and OpenGL thread,
		// we'll just lock on the algorithm object. This is good enough
		// for test purposes.

		synchronized (this) {
			// AlgorithmStepper s = AlgorithmStepper.sharedInstance();

			prepareInput();

			// if (s.update()) {
			// s.show("*Triangulating polygon"+s.plot(mPolygon));
			// }
			PolygonTriangulator t = PolygonTriangulator.triangulator(mContext,
					mPolygon);
			
			t.triangulate();
			
//			int lastFrame = 40;
//
//			Point origin = new Point(180, 220);
//			Point prevPoint = origin;
//
//			for (int i = 0; i <= lastFrame; i++) {
//				mAnimFrame = i;
//
//				Point currPoint = MyMath.pointOnCircle(origin, i * 13
//						* MyMath.M_DEG, 150);
//				if (i % 3 == 2) {
//					if (s.update())
//						s.show("*Milestone at " + i);
//				}
//
//				if (s.update()) {
//					s.show("Algorithm step #" + i
//							+ s.plotRay(prevPoint, currPoint));
//				}
//				prevPoint = currPoint;
		}
	}

	@Override
	public void displayResults() {
		mView.requestRender();
	}

	public int getFrameNumber() {
		return mAnimFrame;
	}

	private void prepareInput() {
		mContext = GeometryContext.contextWithRandomSeed(1965);
		mPolygon = Polygon.testPolygon(mContext, Polygon.TESTPOLY_DRAGON_X + 3);
		mPolygon.transformToFitRect(new Rect(20, 20, 400, 600));
	}

	private GeometryContext mContext;
	private Polygon mPolygon;
	private int mAnimFrame;
	private GLSurfaceView mView;
}
