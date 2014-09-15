package com.js.geometryapp;

import static com.js.basic.Tools.*;

import javax.microedition.khronos.opengles.GL10;

import com.js.geometry.MyMath;
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

	@Override
	/**
	 * Construct matrix to transform from screen coordinates to OpenGL's
	 * normalized device coordinates (-1,-1 ... 1,1)
	 */
	protected void buildProjectionMatrix() {
		float deviceWidth = deviceSize().x;
		float deviceHeight = deviceSize().y;

		mDeviceRect = new Rect(0, 0, deviceWidth, deviceHeight);

		// Add a bit of padding to the device rectangle
		float paddingInset = Math.max(10, deviceWidth / 40);
		float titleInset = Math.max(60, deviceHeight / 10);

		Rect paddedDeviceRect = new Rect(mDeviceRect);
		paddedDeviceRect.x += paddingInset;
		paddedDeviceRect.y += paddingInset + titleInset;
		paddedDeviceRect.width -= paddingInset * 2;
		paddedDeviceRect.height -= paddingInset * 2 + titleInset;

		mAlgorithmToDeviceTransform = MyMath.calcRectFitRectTransform(
				algorithmRect(), paddedDeviceRect);

		Matrix m = new Matrix();

		float sx = 2.0f / deviceWidth;
		float sy = 2.0f / deviceHeight;

		m.setScale(sx, sy);
		m.preTranslate(-deviceWidth / 2.0f, -deviceHeight / 2.0f);

		mDeviceToNDCTransform = m;

		mAlgorithmToNDCTransform = new Matrix(mAlgorithmToDeviceTransform);
		mAlgorithmToNDCTransform.postConcat(mDeviceToNDCTransform);
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
	private Matrix mDeviceToNDCTransform = new Matrix();
	private Matrix mAlgorithmToDeviceTransform;
	private Matrix mAlgorithmToNDCTransform;
}
