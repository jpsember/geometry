package com.js.samplealgorithm;

import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GeometryStepperActivity;

public class SampleGeometryActivity extends GeometryStepperActivity {

	@Override
	public void addAlgorithms(AlgorithmStepper s) {
		s.addAlgorithm(new DelaunayDriver());
		s.addAlgorithm(new StarshapedDriver());
		s.addAlgorithm(new TriangulatePolygonDriver());
	}
}
