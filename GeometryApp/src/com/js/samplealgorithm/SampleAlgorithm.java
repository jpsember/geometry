package com.js.samplealgorithm;

import android.opengl.GLSurfaceView;

import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
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
			prepareInput();

			PolygonTriangulator t = PolygonTriangulator.triangulator(mContext,
					mPolygon);
			
			t.triangulate();
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
		mPolygon = Polygon.testPolygon(mContext, Polygon.TESTPOLY_DRAGON_X + 5);
		mPolygon.rotateBy(16 * MyMath.M_DEG);
		mPolygon.transformToFitRect(new Rect(20, 50, 480 - 2 * 20, 540 - 70),
				false);
	}

	private GeometryContext mContext;
	private Polygon mPolygon;
	private int mAnimFrame;
	private GLSurfaceView mView;
}
