package com.js.samplealgorithm;

import java.util.ArrayList;
import java.util.Random;

import android.graphics.Color;

import com.js.geometry.*;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmDisplayElement;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.widget.ComboBoxWidget;

import static com.js.basic.Tools.*;

public class DelaunayDriver implements Algorithm {

	private static final String BGND_ELEMENT_MESH = "00";
	private static final String BGND_ELEMENT_VORONOI_CELLS = "01";

	private static final int COLOR_LIGHTBLUE = Color.argb(80, 100, 100, 255);

	@Override
	public String getAlgorithmName() {
		return "Delaunay Triangulation";
	}

	@Override
	public void prepareOptions(AlgorithmOptions options) {
		mOptions = options;
		mOptions.addSlider("Seed", "min", 1, "max", 300);
		mOptions.addCheckBox("Deletions", "value", true);
		mOptions.addCheckBox("Delete all");
		mOptions.addCheckBox("Voronoi cells", "value", true);
		ComboBoxWidget w = mOptions.addComboBox("Pattern");
		w.addItem("Random");
		w.addItem("Circle");
		w.prepare();
		mOptions.addSlider("Points", "min", 1, "max", 250, "value", 25);

		mOptions.addCheckBox(Delaunay.DETAIL_SWAPS, "value", true);
		mOptions.addCheckBox(Delaunay.DETAIL_FIND_TRIANGLE, "value", true);
	}

	@Override
	public void run(AlgorithmStepper stepper) {

		Rect pointBounds = new Rect(50, 50, 900, 900);
		s = stepper;
		mMesh = new Mesh();
		mRandom = new Random(mOptions.getIntValue("Seed"));
		boolean deleteAll = mOptions.getBooleanValue("Delete all");
		boolean withDeletions = deleteAll
				|| mOptions.getBooleanValue("Deletions");

		if (s.isActive()) {
			s.openLayer(BGND_ELEMENT_MESH);
			s.setLineWidth(1);
			s.setColor(COLOR_LIGHTBLUE);
			s.plotMesh(mMesh);
			s.closeLayer();
		}

		Rect delaunayBounds = new Rect(pointBounds);
		delaunayBounds.inset(-10, -10);
		mDelaunay = new Delaunay(mMesh, delaunayBounds, stepper);
		if (s.bigStep())
			s.show("Initial triangulation");

		int numPoints = mOptions.getIntValue("Points");
		mVertices = new ArrayList();

		ComboBoxWidget w = mOptions.getWidget("Pattern");
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

		if (deleteAll) {
			while (!mVertices.isEmpty())
				removeArbitraryVertex();
		}

		if (mOptions.getBooleanValue("Voronoi cells")) {
			s.openLayer(BGND_ELEMENT_VORONOI_CELLS);
			s.plot(new AlgorithmDisplayElement() {
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
			});
			s.closeLayer();
			if (s.step())
				s.show("Voronoi cells");
		}
	}

	private void removeArbitraryVertex() {
		Vertex v = removeAndFill(mVertices, mRandom.nextInt(mVertices.size()));
		mDelaunay.remove(v);
	}

	private AlgorithmOptions mOptions;
	private AlgorithmStepper s;
	private Mesh mMesh;
	private Delaunay mDelaunay;
	private Random mRandom;
	private ArrayList<Vertex> mVertices;
}
