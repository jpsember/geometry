package com.js.samplealgorithm;

import static com.js.basic.Tools.*;

import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;

import android.content.Context;

public class SampleRenderer extends AlgorithmRenderer {

	public SampleRenderer(Context context, SampleAlgorithm algorithm) {
		super(context);
		mStepper = AlgorithmStepper.sharedInstance();
		mAlgorithm = algorithm;
		doNothing();
	}

	@Override
	public void onSurfaceChanged() {
	}

	@Override
	public void onDrawFrame() {
	}

	/* private */SampleAlgorithm mAlgorithm;
	/* private */AlgorithmStepper mStepper;
}
