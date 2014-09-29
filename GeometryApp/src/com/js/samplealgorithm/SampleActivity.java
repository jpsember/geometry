package com.js.samplealgorithm;

import android.os.Bundle;

import com.js.geometryapp.GeometryStepperActivity;

public class SampleActivity extends GeometryStepperActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int appNum = 0;

		if (appNum == 0) {
			setAlgorithmDelegate(new DelaunayDriver(this));
		} else if (appNum == 1) {
			setAlgorithmDelegate(new StarshapedDriver(this));
		} else {
			setAlgorithmDelegate(new TriangulatePolygonDriver(this));
		}
	}
}
