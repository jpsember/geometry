package com.js.samplealgorithm;

import android.graphics.Color;

import com.js.geometry.Edge;
import com.js.geometry.GeometryContext;
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
			GeometryContext mesh, Point kernelPoint, Edge edgeOnHole) {
		return new StarshapedHoleTriangulator(mesh, kernelPoint, edgeOnHole);
	}

	private StarshapedHoleTriangulator(GeometryContext mesh, Point kernelPoint,
			Edge edgeOnHole) {
		mStepper = AlgorithmStepper.sharedInstance();
		mContext = mesh;
		mKernelPoint = kernelPoint;
		mStartEdge = edgeOnHole;
	}

	// Convenience methods for using stepper

	private boolean update() {
		return mStepper.update();
	}

	private void show(Object message) {
		mStepper.show(message);
	}

	private String plot(Edge edge) {
		return plotEdge(edge.sourceVertex(), edge.destVertex());
	}

	private String plotEdge(Point p1, Point p2) {
		mStepper.setLineWidth(5);
		mStepper.setColor(Color.RED);
		return mStepper.plotLine(p1, p2);
	}

	private String plot(Point v) {
		mStepper.setColor(Color.RED);
		return mStepper.plot(v);
	}

	private static final String BGND_ELEMENT_HOLE_POLYGON = "10";
	private static final String BGND_ELEMENT_MESH = "00";
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
		while (mHoleSize > 3) {
			stepsWithoutProgress++;
			if (stepsWithoutProgress > mHoleSize) {
				if (update())
					show("*No progress made");
				break;
			}

			Edge nextEdge = mStartEdge.nextFaceEdge();
			Vertex v0 = mStartEdge.sourceVertex();
			Vertex v1 = mStartEdge.destVertex();
			Vertex v2 = nextEdge.destVertex();

			if (update())
				show("Current edge" + plot(mStartEdge));

			if (MyMath.sideOfLine(v0, v1, v2) < 0) {
				if (update())
					show("Vertex is reflex" + plot(mStartEdge) + plot(nextEdge)
							+ plot(v1));
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

	private AlgorithmStepper mStepper;
	private GeometryContext mContext;
	private Point mKernelPoint;
	private Edge mStartEdge;
	private int mHoleSize;
}