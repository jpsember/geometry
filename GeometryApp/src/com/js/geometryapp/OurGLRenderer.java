package com.js.geometryapp;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.js.basic.Tools.*;

import com.js.geometry.Point;
import com.js.geometry.R;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class OurGLRenderer implements GLSurfaceView.Renderer {

	public OurGLRenderer(Context context) {
		mContext = context;
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

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		gl.glViewport(0, 0, w, h);
	}

	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		if (mMesh == null)
			createMesh();

		mProgram.render(mMesh);
	}

	private static final int[] testVertices = { 20, 25, 40, 10, 60, 25, 60, 60,
			40, 60, 40, 25, };
	private static final int[] testTriangles = { 0, 1, 5, 1, 2, 5, 5, 2, 3, 5,
			3, 4, };

	private void createMesh() {
		float sc = mScale * (2 / 200f);
		mMesh = new Mesh();
		pr("creating mesh with scale " + sc);
		for (int i = 0; i < testTriangles.length; i += 3) {
			for (int s = 0; s < 3; s++) { // 3 vertices per triangle
				int vi = testTriangles[i + s] * 2; // 2 coordinates per test
													// vertex
				float x = testVertices[vi + 0];
				float y = testVertices[vi + 1];
				if (true) {// scale things up
					x = ((x - 45) * sc) + 0;
					y = ((y - 40) * sc) + 0;
				}
				mMesh.add(new Point(x, y));
			}
		}
	}

	public Mesh mMesh;

	private GLShader mVertexShader;
	private GLShader mFragmentShader;
	private GLProgram mProgram;

	private final Context mContext;

	private float mRed;
	private float mGreen;
	private float mBlue;
	private int mCounter;
	public float mScale = 1.0f;
}
