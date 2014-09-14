package com.js.geometryapp;

import static com.js.basic.Tools.*;

//import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Rect;

import android.content.Context;
import android.graphics.Matrix;

public class AlgorithmRenderer extends OurGLRenderer {

	public static final int ALGORITHM_SPACE_WIDTH = 1000;
	public static final int ALGORITHM_SPACE_HEIGHT = 1200;

	public AlgorithmRenderer(Context context) {
		super(context);
		mAlgorithmRect = new Rect(0, 0, ALGORITHM_SPACE_WIDTH,
				ALGORITHM_SPACE_HEIGHT);

		doNothing();
	}

	public Rect algorithmRect() {
		return mAlgorithmRect;
	}

	public Rect deviceRect() {
		return mDeviceRect;
	}

	/**
	 * Construct matrix to transform from screen coordinates to OpenGL's
	 * normalized device coordinates (-1,-1 ... 1,1)
	 * 
	 * @param w
	 *            width of view, pixels
	 * @param h
	 *            height of view, pixels
	 */
	private void buildProjectionMatrix() {
		mDeviceRect = new Rect(0, 0, mDeviceSize.x, mDeviceSize.y);

		// Add a bit of padding to the device rectangle
		// Rect a = new Rect(algorithmRect());
		float paddingInset = Math.max(10, mDeviceSize.x / 40);
		float titleInset = Math.max(60, mDeviceSize.y / 10);

		Rect paddedDeviceRect = new Rect(mDeviceRect);
		paddedDeviceRect.x += paddingInset;
		paddedDeviceRect.y += paddingInset + titleInset;
		paddedDeviceRect.width -= paddingInset * 2;
		paddedDeviceRect.height -= paddingInset * 2 + titleInset;

		mAlgorithmToDeviceTransform = MyMath.calcRectFitRectTransform(
				algorithmRect(), paddedDeviceRect);

		Matrix m = new Matrix();

		float sx = 2.0f / mDeviceSize.x;
		float sy = 2.0f / mDeviceSize.y;

		m.setScale(sx, sy);
		m.preTranslate(-mDeviceSize.x / 2.0f, -mDeviceSize.y / 2.0f);

		mDeviceToNDCTransform = m;

		mAlgorithmToNDCTransform = new Matrix(mAlgorithmToDeviceTransform);
		mAlgorithmToNDCTransform.postConcat(mDeviceToNDCTransform);
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		mDeviceSize = new Point(w, h);
		gl.glViewport(0, 0, w, h);
		buildProjectionMatrix();
		GLSpriteProgram.setProjection(mDeviceToNDCTransform);
	}

	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(1f, 1f, 1f, 1f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	public Matrix projectionMatrix() {
		return mAlgorithmToNDCTransform;
	}

	private Rect mAlgorithmRect;
	private Rect mDeviceRect;
	private Point mDeviceSize;
	private Matrix mDeviceToNDCTransform;
	private Matrix mAlgorithmToDeviceTransform;
	private Matrix mAlgorithmToNDCTransform;
}
