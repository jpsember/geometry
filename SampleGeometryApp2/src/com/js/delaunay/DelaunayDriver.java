package com.js.delaunay;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.Color;

import com.js.geometry.*;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.widget.ComboBoxWidget;

import static com.js.basic.Tools.*;

public class DelaunayDriver implements Algorithm {

	private static final String BGND_ELEMENT_MESH = "50:mesh";
	private static final String BGND_ELEMENT_VORONOI_CELLS = "60";
	private static final String USE_EDITOR_POINTS = "Use editor points";

	private static final int COLOR_LIGHTBLUE = Color.argb(80, 100, 100, 255);

	@Override
	public String getAlgorithmName() {
		return "Delaunay Triangulation";
	}

	@Override
	public void prepareOptions(AlgorithmOptions options) {
		mOptions = options;
		mOptions.addSlider("Seed", "min", 1, "max", 300);
		mOptions.addCheckBox("Small initial mesh", "value", true);
		mOptions.addCheckBox("Random disc", "value", false);
		mOptions.addCheckBox("Deletions", "value", true);
		mOptions.addCheckBox("Delete all");
		mOptions.addCheckBox("Voronoi cells", "value", true);
		mOptions.addSlider("Attempts", "min", 1, "max", 5);
		ComboBoxWidget w = mOptions.addComboBox("Pattern");
		w.addItem("Random");
		w.addItem("Circle");
		w.prepare();
		mOptions.addSlider("Points", "min", 1, "max", 250, "value", 25);

		mOptions.addCheckBox(Delaunay.DETAIL_SWAPS, "value", true);
		mOptions.addCheckBox(Delaunay.DETAIL_FIND_TRIANGLE, "value", true);
		mOptions.addCheckBox(Delaunay.DETAIL_TRIANGULATE_HOLE, "value", false);
		mOptions.addCheckBox(USE_EDITOR_POINTS);
	}

	@Override
	public void run(final AlgorithmStepper s, AlgorithmInput input) {
		mEditorPoints = input.points;
		mPointBounds = new Rect(50, 50, 900, 900);
		mMesh = new Mesh();
		mRandom = new Random(mOptions.getIntValue("Seed"));
		boolean deleteAll = mOptions.getBooleanValue("Delete all");
		boolean withDeletions = deleteAll
				|| mOptions.getBooleanValue("Deletions");

		if (s.openLayer(BGND_ELEMENT_MESH)) {
			s.setLineWidth(1);
			s.setColor(COLOR_LIGHTBLUE);
			s.plot(mMesh);
			s.closeLayer();
		}

		Rect delaunayBounds = null;
		if (mOptions.getBooleanValue("Small initial mesh")) {
			delaunayBounds = new Rect(mPointBounds);
			delaunayBounds.inset(-10, -10);
		}
		mDelaunay = new Delaunay(mMesh, delaunayBounds, s);
		if (s.bigStep())
			s.show("Initial triangulation");

		List<Point> inputPoints = new ArrayList();
		if (mOptions.getBooleanValue(USE_EDITOR_POINTS)) {
			for (Point pt : mEditorPoints) {
				if (mPointBounds.contains(pt))
					inputPoints.add(pt);
			}
		} else {
			constructRandomPoints(inputPoints);
		}
		if (inputPoints.isEmpty())
			GeometryException.raise("no points");

		mVertices = new ArrayList();
		for (Point pt : inputPoints) {
			int attempt = 0;
			while (true) {
				try {
					mVertices.add(mDelaunay.add(pt));
					break;
				} catch (GeometryException e) {
					attempt++;
					pr("Problem adding " + pt + ", attempt #" + attempt);
					if (s.step())
						s.show("Problem adding " + pt + ", attempt #" + attempt);
					if (attempt >= mOptions.getIntValue("Attempts")) {
						pr("Failed several attempts at insertion");
						throw e;
					}
					MyMath.perturb(mRandom, pt);
				}
			}

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
			s.setDoneMessage("Removed all vertices");
		} else if (mOptions.getBooleanValue("Voronoi cells")) {
			if (s.openLayer(BGND_ELEMENT_VORONOI_CELLS)) {
				s.plot(new Renderable() {
					@Override
					public void render(AlgorithmStepper s) {
						s.setLineWidth(2);
						s.setColor(Color.argb(0x80, 0x20, 0x80, 0x20));
						for (int i = 0; i < mDelaunay.nSites(); i++) {
							Vertex v = mDelaunay.site(i);
							s.plot(v);
							Polygon p = mDelaunay.constructVoronoiPolygon(i);
							s.plot(p);
						}
					}
				});
				s.closeLayer();
			}
			s.setDoneMessage("Voronoi cells");
		}
	}

	private void constructRandomPoints(List<Point> points) {
		int numPoints = mOptions.getIntValue("Points");

		ComboBoxWidget w = mOptions.getWidget("Pattern");
		String pattern = (String) w.getSelectedKey();

		for (int i = 0; i < numPoints; i++) {
			Point pt;
			if (pattern.equals("Circle")) {
				Point center = mPointBounds.midPoint();
				if (i == numPoints - 1)
					pt = center;
				else
					pt = MyMath.pointOnCircle(center, (i * MyMath.PI * 2)
							/ (numPoints - 1), .49f * mPointBounds.minDim());
				MyMath.perturb(mRandom, pt);
			} else {
				if (mOptions.getBooleanValue("Random disc")) {
					pt = MyMath.randomPointInDisc(mRandom,
							mPointBounds.midPoint(), mPointBounds.minDim() / 2);
				} else
					pt = new Point(mPointBounds.x + mRandom.nextFloat()
							* mPointBounds.width, mPointBounds.y
							+ mRandom.nextFloat() * mPointBounds.height);
			}

			points.add(pt);
		}
	}

	private void removeArbitraryVertex() {
		Vertex v = removeAndFill(mVertices, mRandom.nextInt(mVertices.size()));
		mDelaunay.remove(v);
	}

	private AlgorithmOptions mOptions;
	private Mesh mMesh;
	private Delaunay mDelaunay;
	private Random mRandom;
	private List<Vertex> mVertices;
	private Rect mPointBounds;
	private Point[] mEditorPoints;
}
