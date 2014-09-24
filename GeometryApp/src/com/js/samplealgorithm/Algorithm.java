package com.js.samplealgorithm;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.js.android.Tools;
import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.geometry.R;
import com.js.geometryapp.AbstractWidget;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;

import static com.js.basic.Tools.*;

public class Algorithm implements AlgorithmStepper.Delegate {

	private static int testPolygonNames[] = { //
	//
			Polygon.TESTPOLY_DRAGON_X + 6,//
			Polygon.TESTPOLY_CONCAVE_BLOB,//
			Polygon.TESTPOLY_DRAGON_X, //
			Polygon.TESTPOLY_Y_EQUALS_X_SQUARED,//
			Polygon.TESTPOLY_LARGE_RECTANGLE,//
			Polygon.TESTPOLY_DRAGON_X + 8,//
	};
	private static String testPolygonLabels[] = { "Dragon #6", "Concave Blob",
			"Dragon #0", "Quadratic function", "Large rectangle", "Dragon #8" };

	public Algorithm(Context context) {
		mAppContext = context;
		doNothing();
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
		mView.requestRender();
	}

	public void prepareOptions() {
		sOptions = AlgorithmOptions.sharedInstance();

		if (true) { // Programmatic construction
			sOptions.addComboBox("polygon", testPolygonLabels)
					.setLabel("Polygon:")
					.addListener(AbstractWidget.LISTENER_HIDE_AND_UPDATE);
			sOptions.addDetailBox("Triangulate monotone face");
		} else { // Construction via script
			sOptions.addWidgets(Tools.readTextFileResource(mAppContext,
					R.raw.algorithm_options));
			sOptions.getWidget("polygon").addListener(
					AbstractWidget.LISTENER_HIDE_AND_UPDATE);
		}
	}

	private void prepareInput() {
		int polygonName = testPolygonNames[sOptions.getIntValue("polygon")];
		mContext = GeometryContext.contextWithRandomSeed(1965);
		mPolygon = Polygon.testPolygon(mContext, polygonName);
		mPolygon.rotateBy(16 * MyMath.M_DEG);
		mPolygon.transformToFitRect(mRenderer.algorithmRect(), false);
	}

	private static AlgorithmOptions sOptions;

	private Context mAppContext;
	private GeometryContext mContext;
	private Polygon mPolygon;
	private GLSurfaceView mView;
	private AlgorithmRenderer mRenderer;
}
