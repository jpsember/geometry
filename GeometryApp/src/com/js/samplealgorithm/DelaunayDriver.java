package com.js.samplealgorithm;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView;

import com.js.geometry.*;
import com.js.geometryapp.AlgorithmDisplayElement;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.ComboBoxWidget;

import static com.js.basic.Tools.*;

public class DelaunayDriver implements AlgorithmStepper.Delegate {

	private static final String BGND_ELEMENT_MESH = "00";

	private static final int COLOR_LIGHTBLUE = Color.argb(80, 100, 100, 255);

	public DelaunayDriver(Context context) {
		doNothing();
		mStepper = AlgorithmStepper.sharedInstance();
	}

	public void setView(GLSurfaceView view) {
		mView = view;
	}

	private boolean update() {
		return mStepper.update();
	}

	private void show(Object message) {
		mStepper.show(message);
	}

	@Override
	public void runAlgorithm() {

		Rect pointBounds = new Rect(50, 50, 900, 900);

		try {
			mContext = GeometryContext.contextWithRandomSeed(sOptions
					.getIntValue("seed"));
			boolean withDeletions = sOptions.getBooleanValue("Deletions");
			boolean empty = sOptions.getBooleanValue("Empty");

			mRandom = mContext.random();
			if (mStepper.isActive()) {
				mStepper.plotToBackground(BGND_ELEMENT_MESH);
				mStepper.setLineWidth(1);
				mStepper.setColor(COLOR_LIGHTBLUE);
				mStepper.plot(mContext);
			}

			Rect delaunayBounds = new Rect(pointBounds);
			delaunayBounds.inset(-10, -10);
			mDelaunay = new Delaunay(mContext, delaunayBounds);
			if (update())
				show("*Initial triangulation");

			int numPoints = sOptions.getIntValue("numpoints");
			mVertices = new ArrayList();

			ComboBoxWidget w = sOptions.getWidget("Pattern");
			String pattern = (String) w.getSelectedKey();

			for (int i = 0; i < numPoints; i++) {
				Point pt;
				if (pattern.equals("Circle")) {
					Point center = pointBounds.midPoint();
					if (i == numPoints - 1)
						pt = center;
					else
						pt = MyMath.pointOnCircle(center, (i * MyMath.PI * 2)
								/ numPoints, .49f * pointBounds.minDim());
					mContext.perturb(pt);
				} else {
					pt = new Point(pointBounds.x + mRandom.nextFloat()
							* pointBounds.width, pointBounds.y
							+ mRandom.nextFloat() * pointBounds.height);
				}

				mVertices.add(mDelaunay.add(pt));

				if (withDeletions) {
					// Once in a while, remove a series of points
					if (mRandom.nextInt(3) == 0) {
						int rem = Math
								.min(mVertices.size(), mRandom.nextInt(5));
						while (rem-- > 0) {
							removeArbitraryVertex();
						}
					}
				}
			}

			if (withDeletions && empty) {
				while (!mVertices.isEmpty())
					removeArbitraryVertex();
			}

			if (update())
				show("*Done");

			if (sOptions.getBooleanValue("Voronoi cells")) {
				if (update())
					show("Voronoi cells"
							+ mStepper.plotElement(mVoronoiElement));
			}

		} catch (GeometryException e) {
			pr("\n\ncaught exception:\n" + e);
			mStepper.show("caught exception: " + e);
		}
	}

	private AlgorithmDisplayElement mVoronoiElement = new AlgorithmDisplayElement() {
		@Override
		public void render() {
			setLineWidthState(2);
			setColorState(Color.argb(0x80, 0x20, 0x80, 0x20));

			for (int i = 0; i < mDelaunay.nSites(); i++) {
				Vertex v = mDelaunay.site(i);
				renderPoint(v);
				Polygon p = mDelaunay.constructVoronoiPolygon(i);
				for (int j = 0; j < p.numVertices(); j++)
					extendPolyline(p.vertex(j));
				closePolyline();
				renderPolyline();
			}
		}
	};

	private void removeArbitraryVertex() {
		Vertex v = mVertices.remove(mRandom.nextInt(mVertices.size()));
		mDelaunay.remove(v);
	}

	@Override
	public void displayResults() {
		mView.requestRender();
	}

	@Override
	public void prepareOptions() {
		sOptions = AlgorithmOptions.sharedInstance();

		sOptions.addSlider("seed", 1, 300);
		sOptions.addSlider("numpoints", 1, 250);
		sOptions.addCheckBox("Deletions").setValue(true);
		sOptions.addCheckBox("Empty").setValue(true);
		sOptions.addCheckBox("Voronoi cells");
		ComboBoxWidget w = sOptions.addComboBox("Pattern");
		w.addItem("Random");
		w.addItem("Circle");
		w.prepare();

		sOptions.addDetailBox(Delaunay.DETAIL_SWAPS).setValue(true);
		sOptions.addDetailBox(Delaunay.DETAIL_FIND_TRIANGLE).setValue(true);
		sOptions.addDetailBox(Delaunay.DETAIL_SAMPLES).setValue(true);
	}

	private static AlgorithmOptions sOptions;
	private static AlgorithmStepper mStepper;

	private GeometryContext mContext;
	private Delaunay mDelaunay;
	private Random mRandom;
	private GLSurfaceView mView;
	private ArrayList<Vertex> mVertices;
}
