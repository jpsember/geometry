package com.js.samplealgorithm;

import java.util.Random;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
import com.js.geometry.GeometryException;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.geometryapp.AbstractWidget;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import static com.js.geometry.MyMath.*;

import static com.js.basic.Tools.*;

public class TriangulateStarAlgorithm implements AlgorithmStepper.Delegate {

	public TriangulateStarAlgorithm(Context context) {
		doNothing();
		mStepper = AlgorithmStepper.sharedInstance();
	}

	public void setView(GLSurfaceView view) {
		mView = view;
	}

	@Override
	public void runAlgorithm() {
		try {
			mContext = GeometryContext.contextWithRandomSeed(sOptions
					.getIntValue("seed"));
			mRandom = mContext.random();

			int baseVertex = buildStarshapedPolygon();
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
	public void displayResults() {
		mView.requestRender();
	}

	@Override
	public void prepareOptions() {
		sOptions = AlgorithmOptions.sharedInstance();

		sOptions.addSlider("numpoints", 3, 100).addListener(
				AbstractWidget.LISTENER_UPDATE);
		sOptions.addSlider("seed", 0, 300).addListener(
				AbstractWidget.LISTENER_UPDATE);
		sOptions.addCheckBox("experiment").addListener(
				AbstractWidget.LISTENER_UPDATE);
		sOptions.addCheckBox("inset").addListener(
				AbstractWidget.LISTENER_UPDATE);
		sOptions.addSlider("spikes", 2, 8).addListener(
				AbstractWidget.LISTENER_UPDATE);
	}

	private void buildExperimentalPolygon(Polygon p, int nPoints) {
		Rect r = mStepper.algorithmRect();
		nPoints = Math.max(4, nPoints);

		// Calculate circle that's some distance along +x axis
		float radiusFar = r.minDim() * 2.3f;
		float radiusNear = r.minDim() * .5f;

		int nSpikes = sOptions.getIntValue("spikes");

		for (int pass = 0; pass < nSpikes; pass++) {
			float spikeAngle = (pass * 2 * PI) / nSpikes;

			Point bevelOrigin = pointOnCircle(mKernelPoint, spikeAngle,
					radiusFar);

			Point edgeLoc = pointOnCircle(mKernelPoint, spikeAngle + PI * .05f,
					radiusNear);
			float angleRange = 2 * normalizeAngle(spikeAngle
					- polarAngleOfSegment(edgeLoc, bevelOrigin));
			float bevelRadius = distanceBetween(bevelOrigin, edgeLoc);

			int spikePoints = Math.max(2, nPoints / nSpikes);
			Point pt = null;
			for (int i = 0; i <= spikePoints; i++) {
				float angle2 = ((i / (float) spikePoints) * 2 - 1) * angleRange;
				pt = MyMath.pointOnCircle(bevelOrigin,
						spikeAngle - angle2 + PI, bevelRadius);
				p.add(pt);
			}
			if (sOptions.getBooleanValue("inset")) {
				float angle = polarAngleOfSegment(mKernelPoint, pt);
				float radius = distanceBetween(mKernelPoint, pt);
				Point p2 = pointOnCircle(mKernelPoint, angle + PI * .02f,
						radius * .3f);
				p.add(p2);
			}
		}
	}

	private void buildRandomPolygon(Polygon p, int nPoints) {
		Rect r = mStepper.algorithmRect();
		for (int i = 0; i < nPoints; i++) {
			float angle = i * (360.0f / nPoints) * MyMath.M_DEG;
			float t = mRandom.nextFloat();
			t = 1 - t * t;
			float radius = (t * .8f + .2f) * r.minDim() * .5f;
			Point pt = MyMath.pointOnCircle(mKernelPoint, angle, radius);
			p.add(pt);
		}
	}

	private int buildStarshapedPolygon() {
		int nPoints = sOptions.getIntValue("numpoints");
		ASSERT(nPoints >= 3);
		mKernelPoint = mStepper.algorithmRect().midPoint();
		Polygon p = new Polygon();

		if (sOptions.getBooleanValue("experiment")) {
			buildExperimentalPolygon(p, nPoints);
		} else {
			buildRandomPolygon(p, nPoints);
		}
		int baseVertex = p.embed(mContext);
		return baseVertex;
	}

	private static AlgorithmOptions sOptions;
	private static AlgorithmStepper mStepper;

	private Point mKernelPoint;
	private GeometryContext mContext;
	private Random mRandom;
	private GLSurfaceView mView;

}
