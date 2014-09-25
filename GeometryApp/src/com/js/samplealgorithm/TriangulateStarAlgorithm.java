package com.js.samplealgorithm;

import java.util.Random;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
import com.js.geometry.GeometryException;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;
import com.js.geometryapp.AbstractWidget;
import com.js.geometryapp.AlgorithmDisplayElement;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;

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
					edge);
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
	}

	private int buildStarshapedPolygon() {
		int nPoints = sOptions.getIntValue("numpoints");
		ASSERT(nPoints >= 3);
		Rect r = mRenderer.algorithmRect();
		Polygon p = new Polygon();
		for (int i = 0; i < nPoints; i++) {
			float angle = i * (360.0f / nPoints) * MyMath.M_DEG;
			float t = mRandom.nextFloat();
			t = 1 - t * t;
			float radius = (t * .8f + .2f) * r.width * .5f;
			Point pt = MyMath.pointOnCircle(r.midPoint(), angle, radius);
			p.add(pt);
		}
		int baseVertex = p.embed(mContext);
		return baseVertex;
	}

	private static AlgorithmOptions sOptions;
	private static AlgorithmStepper mStepper;

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
			mStepper.setLineWidth(2);
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

		private static final int COLOR_LIGHTBLUE = Color
				.argb(80, 100, 100, 255);
		private static final int COLOR_DARKGREEN = Color.argb(255, 30, 128, 30);

		public void run() {
			if (mStepper.isActive()) {
				mStepper.plotToBackground(BGND_ELEMENT_HOLE_POLYGON);
				mStepper.plotElement(new AlgDisplayHoleBoundary());

				mStepper.plotToBackground(BGND_ELEMENT_MESH);
				mStepper.setLineWidth(1);
				mStepper.setColor(COLOR_LIGHTBLUE);
				mStepper.plot(mContext);
			}
			if (update())
				show("*Initial hole");
		}

		public static StarTriangulator buildTriangulator(GeometryContext mesh,
				Edge edgeOnHole) {
			return new StarTriangulator(mesh, edgeOnHole);
		}

		private StarTriangulator(GeometryContext mesh, Edge edgeOnHole) {
			mContext = mesh;
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
				setLineWidthState(2);
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
		private Edge mStartEdge;
	}
}
