package com.js.starshaped;

import java.util.ArrayList;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Edge;
import com.js.geometry.GeometryException;
import com.js.geometry.Mesh;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Renderable;
import com.js.geometry.Vertex;
import com.js.geometryapp.RenderTools;

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
		s.addLayer(BGND_ELEMENT_HOLE_POLYGON, new Renderable() {
			public void render(AlgorithmStepper s) {
				s.setColor(RenderTools.COLOR_DARKGREEN);
				Edge edge = mStartEdge;
				while (true) {
					s.plotLine(edge.sourceVertex(), edge.destVertex());
					edge = edge.nextFaceEdge();
					if (edge == mStartEdge)
						break;
				}
			}
		});
		s.addLayer(BGND_ELEMENT_MESH, mMesh);
		// TODO: does this need coloring?
		s.addLayer(BGND_ELEMENT_KERNEL, RenderTools.buildColoredRenderable(
				RenderTools.COLOR_DARKGREEN, mKernelPoint));
		calcHoleSize();
		mInitialHoleSize = mHoleSize;
		mTotalSteps = 0;
		if (s.bigStep())
			s.show("Initial hole (" + mInitialHoleSize + " vertices)");

		mNewEdges = new ArrayList();

		while (mHoleSize > 3) {

			mTotalSteps++;
			if (mTotalSteps > mInitialHoleSize * 50)
				GeometryException.raise("Too many steps!");

			Edge advanceEdge = mStartEdge.nextFaceEdge();
			Vertex v0 = mStartEdge.sourceVertex();
			Vertex v1 = mStartEdge.destVertex();
			Vertex v2 = advanceEdge.destVertex();

			if (s.step())
				s.show("Current edge" + s.highlight(mStartEdge));

			if (MyMath.sideOfLine(v0, v1, v2) < 0) {
				if (s.step())
					s.show("Vertex is reflex" + s.highlight(mStartEdge)
							+ s.highlight(advanceEdge) + s.highlight(v1));
				mStartEdge = advanceEdge;
				continue;
			}

			if (MyMath.sideOfLine(v0, v2, mKernelPoint) < 0) {
				if (s.step())
					s.show("Kernel to right of candidate"
							+ s.highlightLine(v0, v2));
				mStartEdge = advanceEdge;
				continue;
			}

			mStartEdge = mStartEdge.prevFaceEdge();
			Edge newEdge = mMesh.addEdge(v0, v2);
			mNewEdges.add(newEdge);

			if (s.step())
				s.show("Adding edge" + s.highlight(newEdge));
			mHoleSize--;
		}

		s.setDoneMessage("Hole now three edges, done; "
				+ d(mTotalSteps / (float) mInitialHoleSize) + " steps/vertex");
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
