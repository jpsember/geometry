package com.js.opengl;

import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.js.geometry.Point;

import static com.js.basic.Tools.*;

import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;

public class OurGLRenderer implements GLSurfaceView.Renderer {

	public static final String TRANSFORM_NAME_DEVICE_TO_NDC = "device->ndc";

	public OurGLRenderer(Context context) {
		mContext = context;
	}

	public Context context() {
		return mContext;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLTools.defineOpenGLThread();
		mSurfaceId += 1;
		SpriteContext.prepare(this);
	}

	/**
	 * Construct matrix to transform from device coordinates to OpenGL's
	 * normalized device coordinates (-1,-1 ... 1,1)
	 */
	private Matrix buildDeviceToNDCProjectionMatrix() {
		float w = mDeviceSize.x;
		float h = mDeviceSize.y;
		float sx = 2.0f / w;
		float sy = 2.0f / h;
		Matrix mScreenToNDCTransform = new Matrix();
		mScreenToNDCTransform.setScale(sx, sy);
		mScreenToNDCTransform.preTranslate(-w / 2.0f, -h / 2.0f);
		return mScreenToNDCTransform;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int w, int h) {
		gl.glViewport(0, 0, w, h);
		mDeviceSize.setTo(w, h);
		mMatrixId += 1;
		constructTransforms();
	}

	/**
	 * Add a transformation matrix
	 * 
	 * @param name
	 *            unique name for the matrix
	 * @param transform
	 *            the matrix
	 */
	protected void addTransform(String name, Matrix transform) {
		mTransformMap.put(name, transform);
	}

	/**
	 * Construct transformation matrices for the current surface. Default
	 * implementation throws out any old transforms, and generates
	 * TRANSFORM_NAME_DEVICE_TO_NDC which converts from device space to
	 * normalized device coordinates (see
	 * https://github.com/jpsember/geometry/issues/22)
	 * 
	 * Here's a summary of the coordinate spaces:
	 * 
	 * <pre>
	 * 
	 *  algorithm space : this maps a fixed virtual coordinate system 
	 *      (e.g. 1000 x 1200) to the device, no matter its size or 
	 *      orientation (origin at bottom left)
	 * 
	 *  device space : origin at bottom left, this corresponds to pixels
	 * 
	 *  normalized device coordinates : the coordinate system that OpenGL 
	 *     uses
	 * 
	 *  view space : the coordinate system of an Android View; like device
	 *     space, but has its origin in the top left
	 * 
	 * </pre>
	 */
	protected void constructTransforms() {
		mTransformMap.clear();
		addTransform(TRANSFORM_NAME_DEVICE_TO_NDC,
				buildDeviceToNDCProjectionMatrix());
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(0, 0, 0, 1);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
	}

	public Matrix getTransform(String key) {
		Matrix m = mTransformMap.get(key);
		if (m == null)
			die("transform not found for key: " + key);
		return m;
	}

	/**
	 * Get the projection matrix identifier. This changes each time the
	 * projection matrix changes, and can be used to determine if a previously
	 * cached matrix is valid
	 * 
	 * @return id, a positive integer (if surface has been created)
	 */
	public int projectionMatrixId() {
		// TODO: consider renaming this to 'Surface Id' or something, since in
		// addition to projection matrices,
		// OpenGL programs and shaders are also no longer valid.. or are they
		// only no longer valid when a surface is created as opposed to changed?
		GLTools.ensureRenderThread();
		return mMatrixId;
	}

	public int surfaceId() {
		GLTools.ensureRenderThread();
		return mSurfaceId;
	}

	public Point deviceSize() {
		return mDeviceSize;
	}

	// TODO: what do we think of these 'final' attributes?
	private final Context mContext;
	private final Map<String, Matrix> mTransformMap = new HashMap();
	private final Point mDeviceSize = new Point();
	private int mMatrixId;
	private int mSurfaceId;
}
