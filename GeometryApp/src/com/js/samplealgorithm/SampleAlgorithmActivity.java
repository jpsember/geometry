package com.js.samplealgorithm;

import static com.js.basic.Tools.*;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.OurGLSurfaceView;

public class SampleAlgorithmActivity extends GeometryStepperActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		doNothing();
		mAlgorithm = new SampleAlgorithm();
		super.onCreate(savedInstanceState);

		// SampleAlgorithm a = new SampleAlgorithm(getGLSurfaceView());
		mAlgorithm.setView(getGLSurfaceView());

		setAlgorithmDelegate(mAlgorithm);
		// new AlgorithmStepper.Delegate() {
		//
		// @Override
		// public void runAlgorithm() {
		// AlgorithmStepper s = AlgorithmStepper.sharedInstance();
		//
		// // To synchronize between the UI (algorithm) and OpenGL thread,
		// // we'll just lock on this activity class. This is good enough
		// // for algorithm test purposes.
		//
		// synchronized (SampleAlgorithmActivity.class) {
		// int lastFrame = 40;
		//
		// Point origin = new Point(180, 220);
		// Point prevPoint = origin;
		//
		// for (int i = 0; i <= lastFrame; i++) {
		// sAnimFrame = i;
		//
		// Point currPoint = MyMath.pointOnCircle(origin, i * 13
		// * MyMath.M_DEG, 150);
		// if (i % 3 == 2) {
		// if (s.update())
		// s.show("*Milestone at " + i);
		// }
		//
		// if (s.update()) {
		// s.show("Algorithm step #" + i
		// + s.plotRay(prevPoint, currPoint));
		// }
		// prevPoint = currPoint;
		// }
		// }
		// }
		//
		// @Override
		// public void displayResults() {
		// getGLSurfaceView().requestRender();
		// }
		// });
	}

	@Override
	protected GLSurfaceView buildOpenGLView() {
		GLSurfaceView v = new OurGLSurfaceView(this, new SampleRenderer(this,
				mAlgorithm));
		v.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		return v;
	}

	private SampleAlgorithm mAlgorithm;
}
