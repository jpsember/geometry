package com.js.samplealgorithm;

import android.opengl.GLSurfaceView;

import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;
import static com.js.basic.Tools.*;

public class SampleAlgorithm implements AlgorithmStepper.Delegate {

	public SampleAlgorithm() {
		doNothing();
		mStepper = AlgorithmStepper.sharedInstance();
	}

	public void setView(GLSurfaceView view, AlgorithmRenderer renderer) {
		mView = view;
		mRenderer = renderer;
	}

	@Override
	public void runAlgorithm() {
		prepareInput();

		PolygonTriangulator t = PolygonTriangulator.triangulator(mContext,
				mPolygon);

		t.triangulate();
	}

	@Override
	public void displayResults() {
		mAnimFrame++;
		mView.requestRender();
	}

	@Override
	public void optionsChanged() {
		prepareInput();
		mStepper.resetStep();
	}

	public int getFrameNumber() {
		return mAnimFrame;
	}

	private void prepareInput() {
		mContext = GeometryContext.contextWithRandomSeed(1965);
		int testPolys[] = { //
		//
				Polygon.TESTPOLY_DRAGON_X + 6,//
				Polygon.TESTPOLY_CONCAVE_BLOB,//
				Polygon.TESTPOLY_DRAGON_X, //
				Polygon.TESTPOLY_Y_EQUALS_X_SQUARED,//
				Polygon.TESTPOLY_LARGE_RECTANGLE,//
				Polygon.TESTPOLY_DRAGON_X + 8,//
		};
		int choice = mStepper.getTestButtonPressCount() % testPolys.length;

		mPolygon = Polygon.testPolygon(mContext, testPolys[choice]);
		mPolygon.rotateBy(16 * MyMath.M_DEG);
		mPolygon.transformToFitRect(mRenderer.algorithmRect(), false);
	}

	private GeometryContext mContext;
	private Polygon mPolygon;
	private int mAnimFrame;
	private GLSurfaceView mView;
	private AlgorithmRenderer mRenderer;
	private AlgorithmStepper mStepper;
}
