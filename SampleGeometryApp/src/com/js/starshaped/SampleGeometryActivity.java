package com.js.starshaped;

import java.util.Random;

import com.js.geometry.Edge;
import com.js.geometry.GeometryException;
import com.js.geometry.Mesh;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GeometryStepperActivity;

public class SampleGeometryActivity extends GeometryStepperActivity implements
		Algorithm {

	private static final String WIDGET_ID_USE_EDITOR = "Use editor polygon";

	@Override
	public void addAlgorithms(AlgorithmStepper s) {
		s.addAlgorithm(this);
	}

	@Override
	public String getAlgorithmName() {
		return "Triangulate Star-shaped Polygon";
	}

	@Override
	public void prepareOptions(AlgorithmOptions options) {
		mOptions = options;
		mOptions.addSlider("Seed", "min", 0, "max", 300);
		mOptions.addSlider("Points", "min", 3, "max", 250, "value", 18);
		mOptions.addStaticText("");
		mOptions.addCheckBox(WIDGET_ID_USE_EDITOR);
	}

	@Override
	public void prepareInput(AlgorithmInput input) {
		mEditorPolygon = input.getPolygon(null);
	}

	@Override
	public void run(AlgorithmStepper stepper) {
		mStepper = stepper;
		mMesh = new Mesh();
		mRandom = new Random(mOptions.getIntValue("Seed"));

		int baseVertex = buildPolygon();
		Edge edge = mMesh.polygonEdgeFromVertex(mMesh.vertex(baseVertex));
		StarshapedHoleTriangulator t = new StarshapedHoleTriangulator(mStepper,
				mMesh, mKernelPoint, edge);
		t.run();
	}

	private int buildPolygon() {
		int nPoints = mOptions.getIntValue("Points");
		Rect bounds = mStepper.algorithmRect();
		mKernelPoint = bounds.midPoint();
		Polygon p;

		if (mOptions.getBooleanValue(WIDGET_ID_USE_EDITOR)) {
			p = mEditorPolygon;
			if (p == null)
				GeometryException.raise("No polygon");
		} else {
			p = Polygon.starshapedPolygon(bounds, nPoints, mRandom);
			p.transformToFitRect(mStepper.algorithmRect(), true);
		}
		int baseVertex = p.embed(mMesh);
		return baseVertex;
	}

	private AlgorithmOptions mOptions;
	private AlgorithmStepper mStepper;
	private Point mKernelPoint;
	private Mesh mMesh;
	private Random mRandom;
	private Polygon mEditorPolygon;
}
