package com.js.geometryapp;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.js.basic.Tools.*;

import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;

public class OurGLRenderer implements GLSurfaceView.Renderer {

	private static Thread sOpenGLThread;

	public OurGLRenderer(Context context) {
		mContext = context;
	}

	protected Context context() {
		return mContext;
	}

	public static void ensureOpenGLThread() {
		if (Thread.currentThread() != sOpenGLThread)
			die("not in OpenGL thread");
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		sOpenGLThread = Thread.currentThread();

		GLSpriteProgram.prepare(mContext);

		mRed = .2f;
		mGreen = .6f;

		mRed = 0;
		mGreen = .2f;

		mCounter += 1;
		mBlue = .2f + (.1f * (mCounter % 5));
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
	private void buildProjectionMatrix(int w, int h) {
		Matrix m = new Matrix();

		float sx = 2.0f / w;
		float sy = 2.0f / h;

		m.setScale(sx, sy);
		m.preTranslate(-w / 2.0f, -h / 2.0f);

		mScreenToNDCTransform = m;
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		gl.glViewport(0, 0, w, h);
		buildProjectionMatrix(w, h);
		GLSpriteProgram.setProjection(mScreenToNDCTransform);
	}

	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	public Matrix projectionMatrix() {
		return mScreenToNDCTransform;
	}

	private final Context mContext;

	private float mRed;
	private float mGreen;
	private float mBlue;
	private int mCounter;
	private Matrix mScreenToNDCTransform;
}
