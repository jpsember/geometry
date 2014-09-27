package com.js.samplealgorithm;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.ComboBoxWidget;

import static com.js.basic.Tools.*;

public class Algorithm implements AlgorithmStepper.Delegate {

	public Algorithm(Context context) {
		doNothing();
	}

	public void setView(GLSurfaceView view, AlgorithmRenderer renderer) {
		mView = view;
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

	@Override
	public void prepareOptions() {
		sOptions = AlgorithmOptions.sharedInstance();

		ComboBoxWidget w = sOptions.addComboBox("polygon");
		w.setLabel("Polygon:");

		w.addItem("Dragon #6", Polygon.TESTPOLY_DRAGON_X + 6);
		w.addItem("Concave Blob", Polygon.TESTPOLY_CONCAVE_BLOB);
		w.addItem("Dragon #0", Polygon.TESTPOLY_DRAGON_X);
		w.addItem("Quadratic function", Polygon.TESTPOLY_Y_EQUALS_X_SQUARED);
		w.addItem("Large rectangle", Polygon.TESTPOLY_LARGE_RECTANGLE);
		w.addItem("Dragon #8", Polygon.TESTPOLY_DRAGON_X + 8);
		w.prepare();

		sOptions.addCheckBox("Triangulate monotone face", false);
	}

	private void prepareInput() {
		ComboBoxWidget w = sOptions.getWidget("polygon");
		int polygonName = (Integer) w.getSelectedValue();

		mContext = GeometryContext.contextWithRandomSeed(1965);
		mPolygon = Polygon.testPolygon(mContext, polygonName);
		mPolygon.rotateBy(16 * MyMath.M_DEG);
		mPolygon.transformToFitRect(AlgorithmStepper.sharedInstance()
				.algorithmRect(), false);
	}

	private static AlgorithmOptions sOptions;

	private GeometryContext mContext;
	private Polygon mPolygon;
	private GLSurfaceView mView;
}
