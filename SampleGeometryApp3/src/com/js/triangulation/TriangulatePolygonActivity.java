package com.js.triangulation;

import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GeometryStepperActivity;

public class TriangulatePolygonActivity extends GeometryStepperActivity {

	@Override
	public void addAlgorithms(AlgorithmStepper s) {
		s.addAlgorithm(new TriangulatePolygonDriver());
	}
}
