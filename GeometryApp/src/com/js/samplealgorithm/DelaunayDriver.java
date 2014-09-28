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
		s = AlgorithmStepper.sharedInstance();
	}

	public void setView(GLSurfaceView view) {
		mView = view;
	}

	@Override
	public void runAlgorithm() {

		Rect pointBounds = new Rect(50, 50, 900, 900);

		mContext = new Mesh();
		mRandom = new Random(sOptions.getIntValue("Seed"));
		boolean withDeletions = sOptions.getBooleanValue("Deletions");
		boolean empty = sOptions.getBooleanValue("Empty");

		if (s.isActive()) {
			s.openLayer(BGND_ELEMENT_MESH);
			s.setLineWidth(1);
			s.setColor(COLOR_LIGHTBLUE);
			s.plotMesh(mContext);
			s.closeLayer();
		}

		Rect delaunayBounds = new Rect(pointBounds);
		delaunayBounds.inset(-10, -10);
		mDelaunay = new Delaunay(mContext, delaunayBounds);
		if (s.bigStep())
			s.show("Initial triangulation");

		int numPoints = sOptions.getIntValue("Points");
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
							/ (numPoints - 1), .49f * pointBounds.minDim());
				MyMath.perturb(mRandom, pt);
			} else {
				pt = new Point(pointBounds.x + mRandom.nextFloat()
						* pointBounds.width, pointBounds.y
						+ mRandom.nextFloat() * pointBounds.height);
			}

			mVertices.add(mDelaunay.add(pt));

			if (withDeletions) {
				// Once in a while, remove a series of points
				if (mRandom.nextInt(3) == 0) {
					int rem = Math.min(mVertices.size(), mRandom.nextInt(5));
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

		if (s.bigStep())
			s.show("Done");

		if (sOptions.getBooleanValue("Voronoi cells")) {
			if (s.step())
				s.show("Voronoi cells" + s.plot(mVoronoiElement));
		}
	}

	private AlgorithmDisplayElement mVoronoiElement = new AlgorithmDisplayElement() {
		@Override
		public void render() {
			s.setLineWidth(2);
			s.setColor(Color.argb(0x80, 0x20, 0x80, 0x20));

			for (int i = 0; i < mDelaunay.nSites(); i++) {
				Vertex v = mDelaunay.site(i);
				s.plot(v);
				Polygon p = mDelaunay.constructVoronoiPolygon(i);
				s.plot(p, false);
			}
		}
	};

	private void removeArbitraryVertex() {
		Vertex v = removeAndFill(mVertices, mRandom.nextInt(mVertices.size()));
		mDelaunay.remove(v);
	}

	@Override
	public void displayResults() {
		mView.requestRender();
	}

	@Override
	public void prepareOptions() {
		sOptions = AlgorithmOptions.sharedInstance();

		sOptions.addSlider("Seed", 1, 300);
		sOptions.addCheckBox("Deletions", true);
		sOptions.addCheckBox("Empty", true);
		sOptions.addCheckBox("Voronoi cells", false);
		ComboBoxWidget w = sOptions.addComboBox("Pattern");
		w.addItem("Random");
		w.addItem("Circle");
		w.prepare();
		sOptions.addSlider("Points", 1, 250);

		sOptions.addCheckBox(Delaunay.DETAIL_SWAPS, true);
		sOptions.addCheckBox(Delaunay.DETAIL_FIND_TRIANGLE, true);
	}

	private static AlgorithmOptions sOptions;
	private static AlgorithmStepper s;

	private Mesh mContext;
	private Delaunay mDelaunay;
	private Random mRandom;
	private GLSurfaceView mView;
	private ArrayList<Vertex> mVertices;
}
