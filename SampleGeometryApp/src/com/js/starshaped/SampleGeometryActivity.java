package com.js.starshaped;

import java.util.Random;

import com.js.geometry.Edge;
import com.js.geometry.FloatArray;
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
		mOptions.addCheckBox("Experiment");
		mOptions.addStaticText("Generate a polygon that causes a poor running time");
		mOptions.addSlider("Spikes", "min", 2, "max", 20, "value", 9);
		mOptions.addSlider("Length", "min", 2, "max", 150, "value", 95);
		mOptions.addSlider("Cutoff", "min", 10, "max", 100, "value", 69);
		mOptions.addCheckBox("Reversed");
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

	private Polygon buildExperimentalPolygon(int nPoints) {
		FloatArray a = new FloatArray();

		int steps = mOptions.getIntValue("Length");
		int steps2 = (steps * mOptions.getIntValue("Cutoff")) / 100;
		float r1 = .3f;
		float r2 = 1;
		for (int i = 0; i <= steps2; i++) {
			float q = i / (float) steps;
			a.add(r1 + (q * q * q * (r2 - r1)));
		}

		int nSpikes = mOptions.getIntValue("Spikes");

		// Scale so max radius is 1.0
		{
			float max = 0;
			for (int i = 0; i < a.size(); i++)
				max = Math.max(max, a.get(i));
			for (int i = 0; i < a.size(); i++)
				a.set(i, a.get(i) / max);
		}

		if (mOptions.getBooleanValue("Reversed"))
			a.reverse();

		// Duplicate this sequence once per spike
		{
			FloatArray b = new FloatArray();
			for (int i = 0; i < nSpikes; i++)
				b.add(a.array(false), 0, a.size());
			a = b;
		}

		return Polygon.starshapedPolygon(mStepper.algorithmRect(),
				a.array(true));
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
			if (mOptions.getBooleanValue("Experiment")) {
				p = buildExperimentalPolygon(nPoints);
			} else {
				p = Polygon.starshapedPolygon(bounds, nPoints, mRandom);
			}
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
