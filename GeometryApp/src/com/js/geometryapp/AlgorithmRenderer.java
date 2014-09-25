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

public class AlgorithmRenderer extends OurGLRenderer {

	public static final int ALGORITHM_SPACE_WIDTH = 1000;
	public static final int ALGORITHM_SPACE_HEIGHT = 1200;
	public static final String TRANSFORM_NAME_ALGORITHM_TO_NDC = "algorithm->ndc";

	public AlgorithmRenderer(Context context) {
		super(context);
		mAlgorithmRect = new Rect(0, 0, ALGORITHM_SPACE_WIDTH,
				ALGORITHM_SPACE_HEIGHT);

		doNothing();
	}

	public Rect algorithmRect() {
		return mAlgorithmRect;
	}

	/**
	 * Marked final to prevent user from overriding. Any user initialization
	 * should be done within onSurfaceChanged() instead
	 */
	@Override
	public final void onSurfaceCreated(GL10 gl, EGLConfig config) {
		synchronized (AlgorithmStepper.getLock()) {
			super.onSurfaceCreated(gl, config);
		}
	}

	/**
	 * Marked final to prevent user from overriding. User should perform
	 * initialization within onSurfaceChanged()
	 */
	@Override
	public final void onSurfaceChanged(GL10 gl, int w, int h) {
		synchronized (AlgorithmStepper.getLock()) {
			super.onSurfaceChanged(gl, w, h);
			// Let the algorithm stepper elements prepare using this renderer
			AlgorithmDisplayElement.setRenderer(this);
			// Call user method, now that synchronized
			onSurfaceChanged();
		}
	}

	/**
	 * Marked final to prevent user from overriding. User should do rendering
	 * within onDrawFrame()
	 */
	@Override
	public final void onDrawFrame(GL10 gl) {
		synchronized (AlgorithmStepper.getLock()) {
			gl.glClearColor(1f, 1f, 1f, 1f);
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			AlgorithmStepper.sharedInstance().render();
			// Call user method, now that synchronized
			onDrawFrame();
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
				algorithmRect(), paddedDeviceRect);
		float[] v = new float[9];
		mAlgorithmToDeviceTransform.getValues(v);
		sAlgorithmToDensityPixels = (1.0f / v[0]) * MyActivity.density();

		Matrix mAlgorithmToNDCTransform = new Matrix(
				mAlgorithmToDeviceTransform);
		mAlgorithmToNDCTransform
				.postConcat(getTransform(TRANSFORM_NAME_DEVICE_TO_NDC));
		addTransform(TRANSFORM_NAME_ALGORITHM_TO_NDC, mAlgorithmToNDCTransform);
	}

	/**
	 * Determine multiplicative factor for converting from algorithm pixels to
	 * density pixels
	 */
	public static float algorithmToDensityPixels() {
		return sAlgorithmToDensityPixels;
	}

	private static float sAlgorithmToDensityPixels;

	private Rect mAlgorithmRect;
}
