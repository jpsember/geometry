package com.js.samplealgorithm;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.js.basic.Tools.*;

import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometryapp.AlgDisplayElement;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GLProgram;
import com.js.geometryapp.GLSpriteProgram;

import android.content.Context;
import android.graphics.Matrix;

public class SampleRenderer extends AlgorithmRenderer {

	public SampleRenderer(Context context, SampleAlgorithm algorithm) {
		super(context);
		mStepper = AlgorithmStepper.sharedInstance();
		mAlgorithm = algorithm;
		doNothing();
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);

		disposeResources();

		// Let the algorithm stepper elements prepare using this renderer
		AlgDisplayElement.setRenderer(this);

	}

	public void onDrawFrame(GL10 gl) {
		super.onDrawFrame(gl);

		if (mSampleContext != null) {
			Matrix objectMatrix = new Matrix();
			{
				objectMatrix = new Matrix();
				objectMatrix.postScale(.34f, .34f);
				objectMatrix.postTranslate(10, 10);
			}
			mProgram.render(mSampleContext, this, objectMatrix);
		}

		mStepper.render();

		int frame = mAlgorithm.getFrameNumber();
		if (mSprite != null) {
			Point pt = MyMath.pointOnCircle(new Point(250, 100), frame * 7.0f
					* MyMath.M_DEG, 100);
			mSprite.setPosition(pt.x, pt.y);
			mSprite.render();
		}
	}

	public void setSampleContext(GeometryContext c) {
		mSampleContext = c;
	}

	private void disposeResources() {
		mSprite = null;
	}

	private GLProgram mProgram;
	private GeometryContext mSampleContext;
	private GLSpriteProgram mSprite;
	private SampleAlgorithm mAlgorithm;
	private AlgorithmStepper mStepper;
}
