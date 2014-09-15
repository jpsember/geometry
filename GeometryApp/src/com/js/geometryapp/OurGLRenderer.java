package com.js.geometryapp;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.js.geometry.Point;

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
		GLSpriteProgram.prepare(this);
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
	protected void buildProjectionMatrix() {
		float w = mDeviceSize.x;
		float h = mDeviceSize.y;
		float sx = 2.0f / w;
		float sy = 2.0f / h;

		mScreenToNDCTransform.setScale(sx, sy);
		mScreenToNDCTransform.preTranslate(-w / 2.0f, -h / 2.0f);
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		mDeviceSize.setTo(w, h);
		mMatrixId += 1;
		gl.glViewport(0, 0, w, h);
		buildProjectionMatrix();
	}

	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(1f, 1f, 1f, 1f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	public Matrix projectionMatrix() {
		return mScreenToNDCTransform;
	}

	/**
	 * Get the projection matrix identifier. This changes each time the
	 * projection matrix changes, and can be used to determine if a previously
	 * cached matrix is valid
	 * 
	 * @return id, a positive integer (if surface has been created)
	 */
	public int projectionMatrixId() {
		return mMatrixId;
	}

	public Point deviceSize() {
		return mDeviceSize;
	}

	private final Context mContext;

	private Point mDeviceSize = new Point();
	private Matrix mScreenToNDCTransform = new Matrix();
	private int mMatrixId;
}
