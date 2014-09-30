package com.js.samplealgorithm;

import android.os.Bundle;

import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GeometryStepperActivity;

public class SampleActivity extends GeometryStepperActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AlgorithmStepper s = AlgorithmStepper.sharedInstance();
		s.addAlgorithm(new DelaunayDriver());
		s.addAlgorithm(new StarshapedDriver());
		s.addAlgorithm(new TriangulatePolygonDriver());
	}
}
