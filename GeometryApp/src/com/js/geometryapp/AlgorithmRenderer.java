package com.js.geometryapp;

import static com.js.basic.Tools.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.js.android.MyActivity;
import com.js.android.ResolutionInfo;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;
import com.js.geometryapp.editor.Editor;
import com.js.opengl.OurGLRenderer;

import android.content.Context;
import android.graphics.Matrix;
import android.view.View;

public class AlgorithmRenderer extends OurGLRenderer {

	public static final String TRANSFORM_NAME_ALGORITHM_TO_NDC = "algorithm->ndc";
	public static final String TRANSFORM_NAME_ALGORITHM_TO_DEVICE = "algorithm->device";
	public static final String TRANSFORM_NAME_DEVICE_TO_ALGORITHM = "device->algorithm";

	public AlgorithmRenderer(Context context) {
		super(context);
	}

	void setDependencies(Editor editor, ConcreteStepper stepper) {
		mEditor = editor;
		mStepper = stepper;
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
			mStepper.setSurfacePrepared();
			// Let the algorithm stepper elements prepare using this renderer
			RenderTools.setRenderer(mStepper.algorithmRect(), this);
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
			mStepper.setRendering(true);
			// Clear the entire OpenGL view to a gray
			final float GRAY = .8f;
			gl.glClearColor(GRAY, GRAY, GRAY, 1f);
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

			mEditor.render();
			if (!mEditor.isActive())
				mStepper.render();

			// Call user method, now that synchronized
			onDrawFrame();
			mStepper.setRendering(false);
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

		ResolutionInfo resolutionInfo = MyActivity.getResolutionInfo();

		// Add a bit of padding to the device rectangle
		float paddingInset = resolutionInfo.density() * 10;
		float titleInset = resolutionInfo.density() * 30;

		Rect paddedDeviceRect = new Rect(Point.ZERO, deviceSize());
		paddedDeviceRect.x += paddingInset;
		paddedDeviceRect.y += paddingInset + titleInset;
		paddedDeviceRect.width -= paddingInset * 2;
		paddedDeviceRect.height -= paddingInset * 2 + titleInset;

		mStepper.prepareAlgorithmRect(paddedDeviceRect);

		Matrix algorithmToDeviceTransform = MyMath.calcRectFitRectTransform(
				mStepper.algorithmRect(), paddedDeviceRect);
		float[] v = new float[9];
		algorithmToDeviceTransform.getValues(v);
		float scaleDeviceToAlgorithm = (1.0f / v[0]);

		resolutionInfo.setInchesToPixelsAlgorithm(scaleDeviceToAlgorithm
				* resolutionInfo.inchesToPixelsUI(1.0f));

		Matrix algorithmToNDCTransform = new Matrix(
				algorithmToDeviceTransform);
		algorithmToNDCTransform
				.postConcat(getTransform(TRANSFORM_NAME_DEVICE_TO_NDC));
		addTransform(TRANSFORM_NAME_ALGORITHM_TO_NDC, algorithmToNDCTransform);

		// Add a transform to convert algorithm -> device, for rendering text
		addTransform(TRANSFORM_NAME_ALGORITHM_TO_DEVICE,
				algorithmToDeviceTransform);

		// Construct inverse of the previous transform, for editor operations
		Matrix deviceToAlgorithmTransform = new Matrix();
		boolean inverted = algorithmToDeviceTransform
				.invert(deviceToAlgorithmTransform);
		if (!inverted)
			die("failed to invert matrix");

		// See OurGLRenderer.constructTransforms() for a discussion of the
		// coordinate spaces.

		// I can't find a 'transformation' field in the View class, so let's
		// assume the views have unit scale and origin in top left.
		// Construct a matrix that converts this space to bottom left, and
		// concatenate to get the device->algorithm transform

		View editorView = mEditor.getView();
		float height = editorView.getHeight();
		v[0] = 1;
		v[1] = 0;
		v[2] = 0;
		v[3] = 0;
		v[4] = -1;
		v[5] = height;
		v[6] = 0;
		v[7] = 0;
		v[8] = 1;

		Matrix viewToDeviceMatrix = new Matrix();
		viewToDeviceMatrix.setValues(v);
		deviceToAlgorithmTransform.preConcat(viewToDeviceMatrix);

		addTransform(TRANSFORM_NAME_DEVICE_TO_ALGORITHM,
				deviceToAlgorithmTransform);

		// Now that device->algorithm transform known, apply it to the
		// device bounds to get the visible rect (in alg space)
		{
			Point p0 = paddedDeviceRect.bottomLeft();
			Point p1 = paddedDeviceRect.topRight();
			p0.apply(deviceToAlgorithmTransform);
			p1.apply(deviceToAlgorithmTransform);
			mStepper.setVisibleRect(new Rect(p0, p1));
		}
	}

	private ConcreteStepper mStepper;
	private Editor mEditor;
}
