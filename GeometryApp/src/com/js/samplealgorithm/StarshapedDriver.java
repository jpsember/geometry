package com.js.samplealgorithm;

import java.util.Random;

import com.js.geometry.Edge;
import com.js.geometry.Mesh;
import com.js.geometry.GeometryException;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.FloatArray;

import static com.js.basic.Tools.*;

public class StarshapedDriver implements Algorithm {

	public StarshapedDriver() {
		mStepper = AlgorithmStepper.sharedInstance();
	}

	@Override
	public String getAlgorithmName() {
		return "Triangulate Star-shaped Polygon";
	}

	@Override
	public void run() {
		try {
			mContext = new Mesh();
			mRandom = new Random(sOptions.getIntValue("Seed"));

			int baseVertex = buildPolygon();
			Edge edge = mContext.polygonEdgeFromVertex(mContext
					.vertex(baseVertex));
			StarshapedHoleTriangulator t = StarshapedHoleTriangulator
					.buildTriangulator(mContext, mKernelPoint, edge);
			t.run();
		} catch (GeometryException e) {
			mStepper.show("caught exception: " + e);
			pr("\n\ncaught exception:\n" + e);
		}
	}

	@Override
	public void prepareOptions() {
		sOptions = AlgorithmOptions.sharedInstance();

		sOptions.addSlider("Seed", "min", 0, "max", 300);
		sOptions.addSlider("Points", "min", 3, "max", 250, "value", 18);
		sOptions.addCheckBox("experiment");
		sOptions.addSlider("spikes", "min", 2, "max", 50);
		sOptions.addSlider("girth", "min", 3, "max", 80, "value", 50);
	}

	private Polygon buildExperimentalPolygon(int nPoints) {
		int nSpikes = sOptions.getIntValue("spikes");
		float girth = sOptions.getIntValue("girth") * (.5f / 100);

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
		Rect r = mStepper.algorithmRect();
		return Polygon.starshapedPolygon(r, a.array(true));
	}

	private int buildPolygon() {
		int nPoints = sOptions.getIntValue("Points");
		mKernelPoint = mStepper.algorithmRect().midPoint();
		Polygon p;

		if (sOptions.getBooleanValue("experiment")) {
			p = buildExperimentalPolygon(nPoints);
		} else {
			p = Polygon.starshapedPolygon(mStepper.algorithmRect(), nPoints,
					mRandom);
		}
		int baseVertex = p.embed(mContext);
		return baseVertex;
	}

	private static AlgorithmOptions sOptions;
	private static AlgorithmStepper mStepper;

	private Point mKernelPoint;
	private Mesh mContext;
	private Random mRandom;

}
