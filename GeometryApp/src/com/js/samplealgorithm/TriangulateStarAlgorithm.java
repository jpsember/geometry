package com.js.samplealgorithm;

import java.util.Random;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
import com.js.geometry.GeometryException;
import com.js.geometry.InfiniteLoopDetector;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.geometry.Vertex;
import com.js.geometryapp.AbstractWidget;
import com.js.geometryapp.AlgorithmDisplayElement;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;
import static com.js.geometry.MyMath.*;

import static com.js.basic.Tools.*;

public class TriangulateStarAlgorithm implements AlgorithmStepper.Delegate {

	public TriangulateStarAlgorithm(Context context) {
		doNothing();
		mStepper = AlgorithmStepper.sharedInstance();
	}

	public void setView(GLSurfaceView view, AlgorithmRenderer renderer) {
		mView = view;
		mRenderer = renderer;
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
			StarTriangulator t = StarTriangulator.buildTriangulator(mContext,
					mKernelPoint, edge);
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
		Rect r = mRenderer.algorithmRect();
		nPoints = Math.max(4, nPoints);

		// Calculate circle that's some distance along +x axis
		float radiusFar = r.width * 2.3f;
		float radiusNear = r.width * .5f;

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
		Rect r = mRenderer.algorithmRect();
		for (int i = 0; i < nPoints; i++) {
			float angle = i * (360.0f / nPoints) * MyMath.M_DEG;
			float t = mRandom.nextFloat();
			t = 1 - t * t;
			float radius = (t * .8f + .2f) * r.width * .5f;
			Point pt = MyMath.pointOnCircle(mKernelPoint, angle, radius);
			p.add(pt);
		}
	}

	private int buildStarshapedPolygon() {
		int nPoints = sOptions.getIntValue("numpoints");
		ASSERT(nPoints >= 3);
		mKernelPoint = mRenderer.algorithmRect().midPoint();
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
	private AlgorithmRenderer mRenderer;

	// private
	static class StarTriangulator {

		// Convenience methods for using stepper

		private boolean update() {
			return mStepper.update();
		}

		// private
		boolean update(String detailName) {
			return mStepper.update(detailName);
		}

		private void show(Object message) {
			mStepper.show(message);
		}

		// private
		String plot(Edge edge) {
			return plotEdge(edge.sourceVertex(), edge.destVertex());
		}

		// private
		String plotEdge(Point p1, Point p2) {
			mStepper.setLineWidth(5);
			mStepper.setColor(Color.RED);
			return mStepper.plotLine(p1, p2);
		}

		// private
		String plot(Point v) {
			mStepper.setColor(Color.RED);
			return mStepper.plot(v);
		}

		private static final String BGND_ELEMENT_HOLE_POLYGON = "10";
		private static final String BGND_ELEMENT_MESH = "00";
		private static final String BGND_ELEMENT_KERNEL = "05";

		private static final int COLOR_LIGHTBLUE = Color
				.argb(80, 100, 100, 255);
		private static final int COLOR_DARKGREEN = Color.argb(255, 30, 128, 30);

		private void calcHoleSize() {
			int size = 0;
			Edge edge = mStartEdge;
			while (true) {
				size++;
				edge = edge.nextFaceEdge();
				if (edge == mStartEdge)
					break;
			}
			mHoleSize = size;
		}

		public void run() {
			if (mStepper.isActive()) {
				mStepper.plotToBackground(BGND_ELEMENT_HOLE_POLYGON);
				mStepper.plotElement(new AlgDisplayHoleBoundary());

				mStepper.plotToBackground(BGND_ELEMENT_MESH);
				mStepper.setLineWidth(1);
				mStepper.setColor(COLOR_LIGHTBLUE);
				mStepper.plot(mContext);

				mStepper.plotToBackground(BGND_ELEMENT_KERNEL);
				mStepper.setColor(COLOR_DARKGREEN);
				mStepper.plot(mKernelPoint);
			}
			calcHoleSize();
			if (update())
				show("*Initial hole; size " + mHoleSize);

			int stepsWithoutProgress = 0;
			// InfiniteLoopDetector.reset();
			while (mHoleSize > 3) {
				stepsWithoutProgress++;
				if (stepsWithoutProgress > mHoleSize) {
					if (update())
						show("*No progress made");
					break;
				}
				// InfiniteLoopDetector.update();

				Edge nextEdge = mStartEdge.nextFaceEdge();
				Vertex v0 = mStartEdge.sourceVertex();
				Vertex v1 = mStartEdge.destVertex();
				Vertex v2 = nextEdge.destVertex();

				if (update())
					show("Current edge" + plot(mStartEdge));

				if (MyMath.sideOfLine(v0, v1, v2) < 0) {
					if (update())
						show("Vertex is reflex" + plot(mStartEdge)
								+ plot(nextEdge) + plot(v1));
					mStartEdge = nextEdge;
					continue;
				}

				if (MyMath.sideOfLine(v0, v2, mKernelPoint) < 0) {
					if (update())
						show("Kernel to right of candidate" + plotEdge(v0, v2));
					mStartEdge = nextEdge;
					continue;
				}

				mStartEdge = mContext.addEdge(v0, v2);
				if (update())
					show("Adding edge" + plot(mStartEdge));
				mHoleSize -= 1;
				stepsWithoutProgress = 0;
			}
			if (mStepper.isActive())
				mStepper.removeBackgroundElement(BGND_ELEMENT_HOLE_POLYGON);

			if (update())
				show("*Hole now three edges, done");

		}

		public static StarTriangulator buildTriangulator(GeometryContext mesh,
				Point kernelPoint, Edge edgeOnHole) {
			return new StarTriangulator(mesh, kernelPoint, edgeOnHole);
		}

		private StarTriangulator(GeometryContext mesh, Point kernelPoint,
				Edge edgeOnHole) {
			mContext = mesh;
			mKernelPoint = kernelPoint;
			mStartEdge = edgeOnHole;
		}

		/**
		 * Custom display element for sweep status
		 */
		private class AlgDisplayHoleBoundary extends AlgorithmDisplayElement {

			@Override
			public void render() {
				if (mStartEdge == null)
					return;
				setColorState(COLOR_DARKGREEN);
				setLineWidthState(1);
				Edge edge = mStartEdge;
				while (true) {
					mStepper.plotLine(edge.sourceVertex(), edge.destVertex());
					edge = edge.nextFaceEdge();
					if (edge == mStartEdge)
						break;
				}
			}
		}

		private GeometryContext mContext;
		private Point mKernelPoint;
		private Edge mStartEdge;
		private int mHoleSize;
	}
}
