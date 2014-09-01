package com.js.geometryapp;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.js.basic.Tools.*;

import com.js.geometry.Point;
import com.js.geometry.R;

import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;

public class OurGLRenderer implements GLSurfaceView.Renderer {

	public OurGLRenderer(Context context) {
		mContext = context;
		createMesh();
		doNothing();
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		mRed = .2f;
		mGreen = .6f;
		mCounter += 1;
		mBlue = .2f + (.1f * (mCounter % 5));

		mVertexShader = GLShader.readVertexShader(mContext,
				R.raw.simple_vertex_shader);
		mFragmentShader = GLShader.readFragmentShader(mContext,
				R.raw.simple_fragment_shader);
		mProgram = GLProgram.build(mVertexShader, mFragmentShader);

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
	}

	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		mMesh.setRotation(mMesh.rotation() + 1.0f);

		mProgram.render(mMesh, mScreenToNDCTransform);
	}

	// These vertices and triangles define a stylized 'J' polygon centered at
	// the origin with linear size of about 50 pixels

	private static final int[] testVertices = { 1, 3, 1, 2, 2, 1, 4, 1, 5, 2,
			5, 5, 6, 5, 6, 6, 3, 6, 3, 5, 4, 5, 4, 2, 2, 2, 2, 3 };

	private static final int[] testTriangles = { 0, 1, 13, 1, 12, 13, 1, 2, 12,
			2, 3, 12, 3, 11, 12, 3, 4, 11, 11, 4, 5, 11, 5, 10, 9, 10, 8, 8,
			10, 7, 7, 10, 5, 7, 5, 6, };

	private void createMesh() {

		mMesh = new Mesh();
		mMesh.setLocation(300, 200);

		for (int i = 0; i < testTriangles.length; i += 3) {
			for (int s = 0; s < 3; s++) { // 3 vertices per triangle
				int vi = testTriangles[i + s] * 2; // 2 coordinates per test
													// vertex
				float x = (testVertices[vi + 0] - 3) * 10;
				float y = (testVertices[vi + 1] - 3) * 10;
				mMesh.add(new Point(x, y));
			}
		}
	}

	public Mesh mesh() {
		return mMesh;
	}

	private Mesh mMesh;

	private GLShader mVertexShader;
	private GLShader mFragmentShader;
	private GLProgram mProgram;

	private final Context mContext;

	private float mRed;
	private float mGreen;
	private float mBlue;
	private int mCounter;
	private Matrix mScreenToNDCTransform;
}
