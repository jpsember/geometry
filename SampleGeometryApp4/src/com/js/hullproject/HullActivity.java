package com.js.hullproject;

import com.js.geometry.Disc;
import com.js.geometryapp.Algorithm;
import com.js.geometryapp.AlgorithmInput;
import com.js.geometryapp.AlgorithmOptions;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GeometryStepperActivity;


public class HullActivity extends GeometryStepperActivity implements Algorithm {

	@Override
	public void addAlgorithms(AlgorithmStepper s) {
		s.addAlgorithm(this);
	}

	@Override
	public String getAlgorithmName() {
		return "Convex Hull of Discs";
	}

	@Override
	public void prepareOptions(AlgorithmOptions options) {
		mOptions = options;
	}

	@Override
	public void prepareInput(AlgorithmInput input) {
		mDiscs = input.discs;
	}

	@Override
	public void run(AlgorithmStepper stepper) {
		mStepper = stepper;

		if (mDiscs.length != 2)
			stepper.show("Not exactly two discs");
	}

	private Disc[] mDiscs;
	/* private */AlgorithmOptions mOptions;
	/* private */AlgorithmStepper mStepper;
}
