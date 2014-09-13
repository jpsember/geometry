package com.js.samplealgorithm;

import android.opengl.GLSurfaceView;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.AlgorithmStepper;

public class SampleAlgorithm implements AlgorithmStepper.Delegate {

	public void setView(GLSurfaceView view) {
		mView = view;
	}

	@Override
	public void runAlgorithm() {
		AlgorithmStepper s = AlgorithmStepper.sharedInstance();

		// To synchronize between the UI (algorithm) and OpenGL thread,
		// we'll just lock on the algorithm object. This is good enough
		// for test purposes.

		synchronized (this) {
			int lastFrame = 40;

			Point origin = new Point(180, 220);
			Point prevPoint = origin;

			for (int i = 0; i <= lastFrame; i++) {
				sAnimFrame = i;

				Point currPoint = MyMath.pointOnCircle(origin, i * 13
						* MyMath.M_DEG, 150);
				if (i % 3 == 2) {
					if (s.update())
						s.show("*Milestone at " + i);
				}

				if (s.update()) {
					s.show("Algorithm step #" + i
							+ s.plotRay(prevPoint, currPoint));
				}
				prevPoint = currPoint;
			}
		}
	}

	@Override
	public void displayResults() {
		mView.requestRender();
	}

	public int getFrameNumber() {
		return sAnimFrame;
	}

	private int sAnimFrame;
	private GLSurfaceView mView;
}
