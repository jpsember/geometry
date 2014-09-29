package com.js.samplealgorithm;

import android.os.Bundle;

import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.OurGLSurfaceView;

public class SampleActivity extends GeometryStepperActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		int appNum = 0;

		if (appNum == 0) {
			super.onCreate(savedInstanceState);
			DelaunayDriver algorithm = new DelaunayDriver(this);

			OurGLSurfaceView view = (OurGLSurfaceView) getGLSurfaceView();
			algorithm.setView(view);

			setAlgorithmDelegate(algorithm);
		} else if (appNum == 1) {
			super.onCreate(savedInstanceState);
			StarshapedDriver algorithm = new StarshapedDriver(
					this);

			OurGLSurfaceView view = (OurGLSurfaceView) getGLSurfaceView();
			algorithm.setView(view);

			setAlgorithmDelegate(algorithm);
		} else {
			TriangulatePolygonDriver algorithm = new TriangulatePolygonDriver(this);
			super.onCreate(savedInstanceState);

			OurGLSurfaceView view = (OurGLSurfaceView) getGLSurfaceView();
			algorithm.setView(view, (AlgorithmRenderer) view.renderer());

			setAlgorithmDelegate(algorithm);
		}
	}
}
