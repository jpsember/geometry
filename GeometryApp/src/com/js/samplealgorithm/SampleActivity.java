package com.js.samplealgorithm;

import android.os.Bundle;

import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.OurGLSurfaceView;

public class SampleActivity extends GeometryStepperActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		if (true) {
			TriangulateStarAlgorithm algorithm = new TriangulateStarAlgorithm(
					this);
			super.onCreate(savedInstanceState);

			OurGLSurfaceView view = (OurGLSurfaceView) getGLSurfaceView();
			algorithm.setView(view, (AlgorithmRenderer) view.renderer());

			setAlgorithmDelegate(algorithm);
		} else {
			Algorithm algorithm = new Algorithm(this);
			super.onCreate(savedInstanceState);

			OurGLSurfaceView view = (OurGLSurfaceView) getGLSurfaceView();
			algorithm.setView(view, (AlgorithmRenderer) view.renderer());

			setAlgorithmDelegate(algorithm);
		}
	}
}
