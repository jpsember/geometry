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
import static com.js.basic.Tools.*;

/**
 * Algorithm that triangulates a star-shaped hole within a mesh
 */
public class StarshapedHoleTriangulator {

	/**
	 * Constructor
	 * 
	 * @param stepper
	 *            AlgorithmStepper
	 * @param mesh
	 *            mesh containing hole to be triangulated
	 * @param kernelPoint
	 *            point known to lie in kernel of hole
	 * @param edgeOnHole
	 *            edge lying on CCW boundary of hole
	 */
	public StarshapedHoleTriangulator(AlgorithmStepper stepper, Mesh mesh,
			Point kernelPoint, Edge edgeOnHole) {
		s = stepper;
		mMesh = mesh;
		mKernelPoint = kernelPoint;
		mStartEdge = edgeOnHole;
	}

	private static final String BGND_ELEMENT_HOLE_POLYGON = "10";
	private static final String BGND_ELEMENT_MESH = "00:mesh";
	private static final String BGND_ELEMENT_KERNEL = "05";

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
		if (s.openLayer(BGND_ELEMENT_HOLE_POLYGON)) {
			s.plot(new AlgorithmDisplayElement() {
				@Override
				public void render() {
					if (mStartEdge == null)
						return;
					s.setColor(COLOR_DARKGREEN);
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
		}

		if (s.openLayer(BGND_ELEMENT_MESH)) {
			s.setColor(COLOR_LIGHTBLUE);
			s.plotMesh(mMesh);
			s.closeLayer();
		}

		if (s.openLayer(BGND_ELEMENT_KERNEL)) {
			s.setColor(COLOR_DARKGREEN);
			s.plot(mKernelPoint);
			s.closeLayer();
		}
		calcHoleSize();
		mInitialHoleSize = mHoleSize;
		mTotalSteps = 0;
		if (s.bigStep())
			s.show("Initial hole (" + mInitialHoleSize + " vertices)");

		mNewEdges = new ArrayList();

		while (mHoleSize > 3) {

			mTotalSteps++;

			Edge nextEdge = mStartEdge.nextFaceEdge();
			Vertex v0 = mStartEdge.sourceVertex();
			Vertex v1 = mStartEdge.destVertex();
			Vertex v2 = nextEdge.destVertex();

			if (s.step())
				s.show("Current edge" + s.highlight(mStartEdge));

			if (MyMath.sideOfLine(v0, v1, v2) < 0) {
				if (s.step())
					s.show("Vertex is reflex" + s.highlight(mStartEdge)
							+ s.highlight(nextEdge) + s.highlight(v1));
				mStartEdge = nextEdge;
				continue;
			}

			if (MyMath.sideOfLine(v0, v2, mKernelPoint) < 0) {
				if (s.step())
					s.show("Kernel to right of candidate"
							+ s.highlightLine(v0, v2));
				mStartEdge = nextEdge;
				continue;
			}

			mStartEdge = mMesh.addEdge(v0, v2);
			mNewEdges.add(mStartEdge);

			if (s.step())
				s.show("Adding edge" + s.highlight(mStartEdge));

			mHoleSize--;
		}

		if (s.bigStep())
			s.show("Hole now three edges, done; "
					+ f(mTotalSteps / (float) mInitialHoleSize)
					+ " steps/vertex");
	}

	/**
	 * Get the list of edges added by the algorithm
	 */
	public ArrayList<Edge> getNewEdges() {
		return mNewEdges;
	}

	private int mInitialHoleSize;
	private int mTotalSteps;
	private AlgorithmStepper s;
	private Mesh mMesh;
	private Point mKernelPoint;
	private Edge mStartEdge;
	private int mHoleSize;
	private ArrayList<Edge> mNewEdges;
}
