package com.js.delaunay;

import com.js.geometry.AlgorithmStepper;
import com.js.geometryapp.GeometryStepperActivity;

public class DelaunayActivity extends GeometryStepperActivity {
	@Override
	public void addAlgorithms(AlgorithmStepper s) {
		s.addAlgorithm(new DelaunayDriver());
	}

}
