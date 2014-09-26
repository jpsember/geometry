package com.js.samplealgorithm;

import android.os.Bundle;

import com.js.android.AppPreferences;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.OurGLSurfaceView;

public class SampleActivity extends GeometryStepperActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		int appNum = 0;

		if (false) {
			AppPreferences.prepare(this);
			int option = AppPreferences.getInt("z", 0);
			AppPreferences.putInt("z", option + 1);
			appNum = (option / 3) % 3;
		}

		if (appNum == 0) {
			super.onCreate(savedInstanceState);
			DelaunayDriver algorithm = new DelaunayDriver(this);

			OurGLSurfaceView view = (OurGLSurfaceView) getGLSurfaceView();
			algorithm.setView(view);

			setAlgorithmDelegate(algorithm);
		} else if (appNum == 1) {
			super.onCreate(savedInstanceState);
			TriangulateStarAlgorithm algorithm = new TriangulateStarAlgorithm(
					this);

			OurGLSurfaceView view = (OurGLSurfaceView) getGLSurfaceView();
			algorithm.setView(view);

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
