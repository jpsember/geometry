package com.js.triangulation;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.GeometryException;
import com.js.geometry.Mesh;
import com.js.geometry.MyMath;
import com.js.geometry.Polygon;
import com.js.geometry.PolygonTriangulator;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.widget.ComboBoxWidget;

public class TriangulatePolygonDriver implements Algorithm {

	private static final String USE_EDITOR_POLYGON = "Use editor polygon";

	@Override
	public String getAlgorithmName() {
		return "Triangulate Polygon";
	}

	@Override
	public void prepareOptions(AlgorithmOptions options) {
		mOptions = options;

		ComboBoxWidget w = mOptions.addComboBox("Polygon");
		w.setLabel("Polygon:");

		w.addItem("Dragon #6", Polygon.TESTPOLY_DRAGON_X + 6);
		w.addItem("Concave Blob", Polygon.TESTPOLY_CONCAVE_BLOB);
		w.addItem("Dragon #0", Polygon.TESTPOLY_DRAGON_X);
		w.addItem("Quadratic function", Polygon.TESTPOLY_Y_EQUALS_X_SQUARED);
		w.addItem("Large rectangle", Polygon.TESTPOLY_LARGE_RECTANGLE);
		w.addItem("Dragon #8", Polygon.TESTPOLY_DRAGON_X + 8);
		w.addItem("Star-shaped (small)", Polygon.TESTPOLY_STARSHAPED_X + 15);
		w.addItem("Star-shaped (large)", Polygon.TESTPOLY_STARSHAPED_X + 100);
		mOptions.addCheckBox(USE_EDITOR_POLYGON);

		w.prepare();

		mOptions.addCheckBox(
				PolygonTriangulator.DETAIL_TRIANGULATE_MONOTONE_FACE, "value",
				true);
	}

	@Override
	public void prepareInput(AlgorithmInput input) {
		mEditorPolygon = input.getPolygon(null);
	}

	@Override
	public void run(AlgorithmStepper stepper) {
		mStepper = stepper;
		prepareInput();

		PolygonTriangulator t = PolygonTriangulator.triangulator(mStepper,
				mMesh, mPolygon);

		t.triangulate();
	}

	private void prepareInput() {
		mMesh = new Mesh();
		if (mOptions.getBooleanValue(USE_EDITOR_POLYGON)) {
			mPolygon = mEditorPolygon;
		} else {
			ComboBoxWidget w = mOptions.getWidget("Polygon");
			int polygonName = (Integer) w.getSelectedValue();
			mPolygon = Polygon.testPolygon(polygonName);
			mPolygon.rotateBy(16 * MyMath.M_DEG);
			mPolygon.transformToFitRect(mStepper.algorithmRect(), false);
		}
		if (mPolygon == null)
			GeometryException.raise("no polygon");
	}

	private AlgorithmOptions mOptions;
	private AlgorithmStepper mStepper;
	private Mesh mMesh;
	private Polygon mPolygon;
	private Polygon mEditorPolygon;
}
