package com.js.geometryapp;

import static com.js.basic.Tools.*;

import javax.microedition.khronos.opengles.GL10;

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

	@Override
	protected void constructTransforms() {
		super.constructTransforms();
		float deviceWidth = deviceSize().x;
		float deviceHeight = deviceSize().y;

		// Add a bit of padding to the device rectangle
		float paddingInset = Math.max(10, deviceWidth / 40);
		float titleInset = Math.max(60, deviceHeight / 10);

		Rect paddedDeviceRect = new Rect(Point.ZERO, deviceSize());
		paddedDeviceRect.x += paddingInset;
		paddedDeviceRect.y += paddingInset + titleInset;
		paddedDeviceRect.width -= paddingInset * 2;
		paddedDeviceRect.height -= paddingInset * 2 + titleInset;

		Matrix mAlgorithmToDeviceTransform = MyMath.calcRectFitRectTransform(
				algorithmRect(), paddedDeviceRect);

		Matrix mAlgorithmToNDCTransform = new Matrix(
				mAlgorithmToDeviceTransform);
		mAlgorithmToNDCTransform
				.postConcat(getTransform(TRANSFORM_NAME_DEVICE_TO_NDC));
		addTransform(TRANSFORM_NAME_ALGORITHM_TO_NDC, mAlgorithmToNDCTransform);
	}

	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(1f, 1f, 1f, 1f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
	}

	private Rect mAlgorithmRect;
}
