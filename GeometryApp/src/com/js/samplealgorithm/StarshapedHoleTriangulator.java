package com.js.samplealgorithm;

import java.util.ArrayList;

import android.graphics.Color;

import com.js.geometry.Edge;
import com.js.geometry.Mesh;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Vertex;
import com.js.geometryapp.AlgorithmDisplayElement;
import com.js.geometryapp.AlgorithmStepper;

/**
 * Algorithm that triangulates a star-shaped hole within a mesh
 */
public class StarshapedHoleTriangulator {

	/**
	 * Factory constructor
	 * 
	 * @param mesh
	 *            mesh containing hole to be triangulated
	 * @param kernelPoint
	 *            point known to lie in kernel of hole
	 * @param edgeOnHole
	 *            edge lying on CCW boundary of hole
	 */
	public static StarshapedHoleTriangulator buildTriangulator(
			Mesh mesh, Point kernelPoint, Edge edgeOnHole) {
		return new StarshapedHoleTriangulator(mesh, kernelPoint, edgeOnHole);
	}

	private StarshapedHoleTriangulator(Mesh mesh, Point kernelPoint,
			Edge edgeOnHole) {
		s = AlgorithmStepper.sharedInstance();
		mContext = mesh;
		mKernelPoint = kernelPoint;
		mStartEdge = edgeOnHole;
	}

	// Convenience methods for using stepper

	private String plot(Edge edge) {
		return plotEdge(edge.sourceVertex(), edge.destVertex());
	}

	private String plotEdge(Point p1, Point p2) {
		s.setLineWidth(5);
		s.setColor(Color.RED);
		return s.plotLine(p1, p2);
	}

	private String plot(Point v) {
		s.setColor(Color.RED);
		return s.plot(v);
	}

	private static final String BGND_ELEMENT_HOLE_POLYGON = "s10";
	private static final String BGND_ELEMENT_MESH = "s00";
	private static final String BGND_ELEMENT_KERNEL = "s05";

	private static final int COLOR_LIGHTBLUE = Color.argb(80, 100, 100, 255);
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
		if (s.isActive()) {
			s.openLayer(BGND_ELEMENT_HOLE_POLYGON);
			s.plot(new AlgorithmDisplayElement() {
				@Override
				public void render() {
					if (mStartEdge == null)
						return;
					s.setColor(COLOR_DARKGREEN);
					s.setLineWidth(1);
					Edge edge = mStartEdge;
					while (true) {
						s.plotLine(edge.sourceVertex(), edge.destVertex());
						edge = edge.nextFaceEdge();
						if (edge == mStartEdge)
							break;
					}
				}
			});
			s.closeLayer();

			s.openLayer(BGND_ELEMENT_MESH);
			s.setLineWidth(1);
			s.setColor(COLOR_LIGHTBLUE);
			s.plotMesh(mContext);
			s.closeLayer();

			s.openLayer(BGND_ELEMENT_KERNEL);
			s.setColor(COLOR_DARKGREEN);
			s.plot(mKernelPoint);
			s.closeLayer();
		}
		calcHoleSize();
		if (s.bigStep())
			s.show("Initial hole");

		mNewEdges = new ArrayList();

		int stepsWithoutProgress = 0;
		while (mHoleSize > 3) {
			stepsWithoutProgress++;
			if (stepsWithoutProgress > mHoleSize) {
				if (s.bigStep())
					s.show("No progress made");
				break;
			}

			Edge nextEdge = mStartEdge.nextFaceEdge();
			Vertex v0 = mStartEdge.sourceVertex();
			Vertex v1 = mStartEdge.destVertex();
			Vertex v2 = nextEdge.destVertex();

			if (s.step())
				s.show("Current edge" + plot(mStartEdge));

			if (MyMath.sideOfLine(v0, v1, v2) < 0) {
				if (s.step())
					s.show("Vertex is reflex" + plot(mStartEdge)
							+ plot(nextEdge) + plot(v1));
				mStartEdge = nextEdge;
				continue;
			}

			if (MyMath.sideOfLine(v0, v2, mKernelPoint) < 0) {
				if (s.step())
					s.show("Kernel to right of candidate" + plotEdge(v0, v2));
				mStartEdge = nextEdge;
				continue;
			}

			mStartEdge = mContext.addEdge(v0, v2);
			mNewEdges.add(mStartEdge);

			if (s.step())
				s.show("Adding edge" + plot(mStartEdge));
			mHoleSize -= 1;
			stepsWithoutProgress = 0;
		}

		if (s.bigStep())
			s.show("Hole now three edges, done");

		if (s.isActive()) {
			s.removeLayer(BGND_ELEMENT_KERNEL);
			s.removeLayer(BGND_ELEMENT_MESH);
			s.removeLayer(BGND_ELEMENT_HOLE_POLYGON);
		}
	}

	/**
	 * Get the list of edges added by the algorithm
	 */
	public ArrayList<Edge> getNewEdges() {
		return mNewEdges;
	}

	private static AlgorithmStepper s;

	private Mesh mContext;
	private Point mKernelPoint;
	private Edge mStartEdge;
	private int mHoleSize;
	private ArrayList<Edge> mNewEdges;
}