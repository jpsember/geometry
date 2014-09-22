package com.js.samplealgorithm;

import android.opengl.GLSurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;
import static com.js.basic.Tools.*;

public class Algorithm implements AlgorithmStepper.Delegate {

	private static int testPolys[] = { //
	//
			Polygon.TESTPOLY_DRAGON_X + 6,//
			Polygon.TESTPOLY_CONCAVE_BLOB,//
			Polygon.TESTPOLY_DRAGON_X, //
			Polygon.TESTPOLY_Y_EQUALS_X_SQUARED,//
			Polygon.TESTPOLY_LARGE_RECTANGLE,//
			Polygon.TESTPOLY_DRAGON_X + 8,//
	};
	private static String testPolyNames[] = { "Dragon #6", "Concave Blob",
			"Dragon #0", "Quadratic function", "Large rectangle", "Dragon #8", };

	public Algorithm() {
		doNothing();
		mDesiredPolygonName = testPolys[0];
	}

	public void setView(GLSurfaceView view, AlgorithmRenderer renderer) {
		mView = view;
		mRenderer = renderer;
	}

	@Override
	public void runAlgorithm() {
		prepareInput(mDesiredPolygonName);

		PolygonTriangulator t = PolygonTriangulator.triangulator(mContext,
				mPolygon);

		t.triangulate();
	}

	@Override
	public void displayResults() {
		mAnimFrame++;
		mView.requestRender();
	}

	public int getFrameNumber() {
		return mAnimFrame;
	}

	public void prepareOptions() {
		AlgorithmOptions options = AlgorithmOptions.sharedInstance();
		ASSERT(options != null);

		options.addDropdown(testPolyNames, new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {

				mDesiredPolygonName = testPolys[position];
				unimp("we may need synchronization here (or actually elsewhere)");
				mPolygon = null;
				unimp("trigger rerun of algorithm somehow, also redetermination of # steps");
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	private void prepareInput(int polygonName) {
		if (mPolygon != null)
			return;

		mContext = GeometryContext.contextWithRandomSeed(1965);
		mPolygon = Polygon.testPolygon(mContext, polygonName);
		mPolygon.rotateBy(16 * MyMath.M_DEG);
		mPolygon.transformToFitRect(mRenderer.algorithmRect(), false);
	}

	private GeometryContext mContext;
	private Polygon mPolygon;
	private int mDesiredPolygonName;
	private int mAnimFrame;
	private GLSurfaceView mView;
	private AlgorithmRenderer mRenderer;
}
