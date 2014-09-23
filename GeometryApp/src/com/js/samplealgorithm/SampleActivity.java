package com.js.samplealgorithm;

import static com.js.basic.Tools.*;

import android.content.Context;
import android.os.Bundle;

import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.OurGLSurfaceView;

public class SampleActivity extends GeometryStepperActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		doNothing();
		mAlgorithm = new Algorithm(this);
		super.onCreate(savedInstanceState);

		OurGLSurfaceView view = (OurGLSurfaceView) getGLSurfaceView();
		mAlgorithm.setView(view, (AlgorithmRenderer) view.renderer());

		setAlgorithmDelegate(mAlgorithm);
	}

	@Override
	protected AlgorithmRenderer buildRenderer(Context context) {
		return new SampleRenderer(this, mAlgorithm);
	}

	@Override
	protected void prepareOptions() {
		mAlgorithm.prepareOptions();
	}

	private Algorithm mAlgorithm;
}
