package com.js.geometryapp;

import static com.js.basic.Tools.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.js.android.MyActivity;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;

import android.content.Context;
import android.graphics.Matrix;

class AlgorithmRenderer extends OurGLRenderer {

	public static final String TRANSFORM_NAME_ALGORITHM_TO_NDC = "algorithm->ndc";
	public static final String TRANSFORM_NAME_ALGORITHM_TO_DEVICE = "algorithm->device";

	public AlgorithmRenderer(Context context, ConcreteStepper stepper) {
		super(context);
		mStepper = stepper;
		doNothing();
	}

	/**
	 * Marked final to prevent user from overriding. Any user initialization
	 * should be done within onSurfaceChanged() instead
	 */
	@Override
	public final void onSurfaceCreated(GL10 gl, EGLConfig config) {
		synchronized (mStepper.getLock()) {
			mStepper.acquireLock();
			super.onSurfaceCreated(gl, config);
			mStepper.releaseLock();
		}
	}

	/**
	 * Marked final to prevent user from overriding. User should perform
	 * initialization within onSurfaceChanged()
	 */
	@Override
	public final void onSurfaceChanged(GL10 gl, int w, int h) {
		synchronized (mStepper.getLock()) {
			mStepper.acquireLock();
			super.onSurfaceChanged(gl, w, h);
			// Let the algorithm stepper elements prepare using this renderer
			AlgorithmDisplayElement.setRenderer(this);
			// Call user method, now that synchronized
			onSurfaceChanged();
			mStepper.releaseLock();
		}
	}

	/**
	 * Marked final to prevent user from overriding. User should do rendering
	 * within onDrawFrame()
	 */
	@Override
	public final void onDrawFrame(GL10 gl) {
		synchronized (mStepper.getLock()) {
			mStepper.acquireLock();
			AlgorithmDisplayElement.setRendering(true);
			gl.glClearColor(1f, 1f, 1f, 1f);
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			mStepper.render();
			// Call user method, now that synchronized
			onDrawFrame();
			AlgorithmDisplayElement.setRendering(false);
			mStepper.releaseLock();
		}
	}

	/**
	 * Subclass can implement this method to initialize render resources
	 */
	public void onSurfaceChanged() {
	}

	/**
	 * Subclass can implement this method to perform rendering
	 */
	public void onDrawFrame() {
	}

	@Override
	protected void constructTransforms() {
		super.constructTransforms();

		// Add a bit of padding to the device rectangle
		float paddingInset = MyActivity.density() * 10;
		float titleInset = MyActivity.density() * 30;

		Rect paddedDeviceRect = new Rect(Point.ZERO, deviceSize());
		paddedDeviceRect.x += paddingInset;
		paddedDeviceRect.y += paddingInset + titleInset;
		paddedDeviceRect.width -= paddingInset * 2;
		paddedDeviceRect.height -= paddingInset * 2 + titleInset;

		Matrix mAlgorithmToDeviceTransform = MyMath.calcRectFitRectTransform(
				mStepper.algorithmRect(), paddedDeviceRect);
		float[] v = new float[9];
		mAlgorithmToDeviceTransform.getValues(v);
		sAlgorithmToDensityPixels = (1.0f / v[0]) * MyActivity.density();

		Matrix mAlgorithmToNDCTransform = new Matrix(
				mAlgorithmToDeviceTransform);
		mAlgorithmToNDCTransform
				.postConcat(getTransform(TRANSFORM_NAME_DEVICE_TO_NDC));
		addTransform(TRANSFORM_NAME_ALGORITHM_TO_NDC, mAlgorithmToNDCTransform);

		// Add a transform to convert algorithm -> device, for rendering text
		addTransform(TRANSFORM_NAME_ALGORITHM_TO_DEVICE,
				mAlgorithmToDeviceTransform);
	}

	/**
	 * Determine multiplicative factor for converting from algorithm pixels to
	 * density pixels
	 */
	public static float algorithmToDensityPixels() {
		return sAlgorithmToDensityPixels;
	}

	private static float sAlgorithmToDensityPixels;

	private ConcreteStepper mStepper;

}
