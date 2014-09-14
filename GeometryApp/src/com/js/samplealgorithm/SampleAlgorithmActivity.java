package com.js.samplealgorithm;

import static com.js.basic.Tools.*;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.GeometryStepperActivity;
import com.js.geometryapp.OurGLSurfaceView;

public class SampleAlgorithmActivity extends GeometryStepperActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		doNothing();
		mAlgorithm = new SampleAlgorithm();
		super.onCreate(savedInstanceState);

		OurGLSurfaceView view = (OurGLSurfaceView) getGLSurfaceView();
		mAlgorithm.setView(view, (AlgorithmRenderer) view.renderer());

		setAlgorithmDelegate(mAlgorithm);
	}

	@Override
	protected GLSurfaceView buildOpenGLView() {
		mRenderer = new SampleRenderer(this, mAlgorithm);
		GLSurfaceView v = new OurGLSurfaceView(this, mRenderer);
		v.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		return v;
	}

	private AlgorithmRenderer mRenderer;
	private SampleAlgorithm mAlgorithm;
}
