package com.js.samplealgorithm;

import java.util.Random;

import com.js.geometry.Edge;
import com.js.geometry.Mesh;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.FloatArray;

import static com.js.basic.Tools.*;

public class StarshapedDriver implements Algorithm {

	@Override
	public String getAlgorithmName() {
		return "Triangulate Star-shaped Polygon";
	}

	@Override
	public void prepareOptions(AlgorithmOptions options) {
		mOptions = options;
		mOptions.addSlider("Seed", "min", 0, "max", 300);
		mOptions.addSlider("Points", "min", 3, "max", 250, "value", 18);
		mOptions.addCheckBox("experiment");
		mOptions.addSlider("spikes", "min", 2, "max", 50);
		mOptions.addSlider("girth", "min", 3, "max", 80, "value", 50);
	}

	@Override
	public void run(AlgorithmStepper stepper) {
		mStepper = stepper;
		mMesh = new Mesh();
		mRandom = new Random(mOptions.getIntValue("Seed"));

		int baseVertex = buildPolygon();
		Edge edge = mMesh.polygonEdgeFromVertex(mMesh.vertex(baseVertex));
		StarshapedHoleTriangulator t = StarshapedHoleTriangulator
				.buildTriangulator(mStepper, mMesh, mKernelPoint, edge);
		t.run();
	}

	private Polygon buildExperimentalPolygon(int nPoints) {
		int nSpikes = mOptions.getIntValue("spikes");
		float girth = mOptions.getIntValue("girth") * (.5f / 100);

		float spikeWidthRatio = .5f;
		nPoints = Math.max(4, nPoints);
		int pointsPerSpike = (int) ((nPoints * spikeWidthRatio) / nSpikes);

		// Build a radial map of a spike
		FloatArray a = new FloatArray();

		int arcPoints = Math.max(pointsPerSpike - 2, 2);
		for (int j = 0; j < arcPoints; j++) {
			float x = (j / (float) (arcPoints - 1));
			float x0 = 2 * (.5f - x);
			float y = (float) Math.sqrt(1 - x0 * x0);
			float height = 1 - .5f * (y * (1 - 2 * girth));
			a.add(height);
		}
		// Add base segments
		for (int i = 0; i < Math.max(2,
				(int) (pointsPerSpike / spikeWidthRatio)); i++)
			a.add(girth);

		// Duplicate this sequence once per spike
		{
			FloatArray b = new FloatArray();
			for (int i = 0; i < nSpikes; i++)
				b.add(a.array(false), 0, a.size());
			a = b;
		}

		// Shift floats forward
		{
			FloatArray b = new FloatArray();
			for (int i = 0; i < a.size(); i++)
				b.add(a.get((i + (arcPoints / 2)) % a.size()));
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

		if (mOptions.getBooleanValue("experiment")) {
			p = buildExperimentalPolygon(nPoints);
		} else {
			p = Polygon.starshapedPolygon(bounds, nPoints, mRandom);
		}
		int baseVertex = p.embed(mMesh);
		return baseVertex;
	}

	static {
		doNothing();
	}

	private AlgorithmOptions mOptions;
	private AlgorithmStepper mStepper;
	private Point mKernelPoint;
	private Mesh mMesh;
	private Random mRandom;

}
