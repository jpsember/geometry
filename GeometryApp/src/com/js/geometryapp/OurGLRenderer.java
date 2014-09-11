package com.js.geometryapp;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.js.basic.Tools.*;

import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;
import com.js.geometry.Rect;

import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;

public class OurGLRenderer implements GLSurfaceView.Renderer {

	private static Thread sOpenGLThread;

	public OurGLRenderer(Context context) {
		mContext = context;
		mRotatingMesh = createMesh(0, 0);
		mStaticMesh = createMesh(150, 300);
		mPolyline = createPolyline();

		doNothing();
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

		mVertexShader = GLShader.readVertexShader(mContext,
				R.raw.simple_vertex_shader);
		mFragmentShader = GLShader.readFragmentShader(mContext,
				R.raw.simple_fragment_shader);
		mProgram = GLProgram.build(mVertexShader, mFragmentShader);

		if (mSprite == null) {
			GLTexture t = new GLTexture(mContext, R.raw.texture);
			mSprite = new GLSpriteProgram(t, new Rect(0, 0, 64, 64));
		}
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

		Matrix objectMatrix = new Matrix();

		mRotation += 1.0f;
		if (false) {
			objectMatrix.setRotate(mRotation);
			objectMatrix.postScale(mScale, mScale);
			objectMatrix.postTranslate(300, 200);

			mProgram.render(mRotatingMesh, this, objectMatrix);
		}

		mProgram.render(mStaticMesh, this, null);

		if (false) {
			{
				objectMatrix = new Matrix();
				objectMatrix.setRotate(-mRotation * 0.2f);
				objectMatrix.postScale(mScale * 6.0f, mScale * 6.0f);
				objectMatrix.postTranslate(420, 300);
			}
			mProgram.render(mPolyline, this, objectMatrix);
		}

		if (mSampleContext != null) {
			{
				objectMatrix = new Matrix();
				objectMatrix.postScale(.34f, .34f);
				objectMatrix.postTranslate(10, 10);
			}
			mProgram.render(mSampleContext, this, objectMatrix);
		}

		if (true) {
			if (mGLText == null)
				mGLText = new OurGLText("Howdy");
			mGLText.render(this);
		}

		if (mSprite != null) {
			Point pt = MyMath.pointOnCircle(new Point(250, 250), mRotation
					* MyMath.M_DEG * 1.2f, 100);
			mSprite.setPosition(pt.x, pt.y);
			mSprite.render();
		}
	}

	private OurGLText mGLText;

	public void bumpScale() {
		mScale *= 1.2f;
	}

	// These vertices and triangles define a stylized 'J' polygon centered at
	// the origin with linear size of about 50 pixels

	private static final int[] testVertices = { 1, 3, 1, 2, 2, 1, 4, 1, 5, 2,
			5, 5, 6, 5, 6, 6, 3, 6, 3, 5, 4, 5, 4, 2, 2, 2, 2, 3 };

	private static final int[] testTriangles = { 0, 1, 13, 1, 12, 13, 1, 2, 12,
			2, 3, 12, 3, 11, 12, 3, 4, 11, 11, 4, 5, 11, 5, 10, 9, 10, 8, 8,
			10, 7, 7, 10, 5, 7, 5, 6, };

	private Mesh createMesh(float originX, float originY) {

		Mesh mMesh = new Mesh();

		for (int i = 0; i < testTriangles.length; i += 3) {
			for (int s = 0; s < 3; s++) { // 3 vertices per triangle
				int vi = testTriangles[i + s] * 2; // 2 coordinates per test
													// vertex
				float x = (testVertices[vi + 0] - 3) * 10 + originX;
				float y = (testVertices[vi + 1] - 3) * 10 + originY;
				mMesh.add(new Point(x, y));
			}
		}
		return mMesh;
	}

	public Mesh mesh() {
		return mRotatingMesh;
	}

	public Matrix projectionMatrix() {
		return mScreenToNDCTransform;
	}

	private Polyline createPolyline() {
		Polyline p = new Polyline();
		p.setColor(.6f, .2f, .2f);
		float originX = 0;
		float originY = 0;

		for (int i = 0; i < testVertices.length / 2; i++) {
			if ((i % 2) == 0) {
				p.setColor(.6f, 0, 0);
			} else {
				p.setColor(0, 0, 1.0f);
			}
			float x = (testVertices[i * 2 + 0] - 3) * 10 + originX;
			float y = (testVertices[i * 2 + 1] - 3) * 10 + originY;
			p.add(new Point(x, y));
		}
		p.close();
		return p;
	}

	private float mRotation;
	private float mScale = 1.0f;
	private Mesh mRotatingMesh;
	private Mesh mStaticMesh;
	private Polyline mPolyline;

	private GLShader mVertexShader;
	private GLShader mFragmentShader;
	private GLProgram mProgram;

	private final Context mContext;

	private float mRed;
	private float mGreen;
	private float mBlue;
	private int mCounter;
	private Matrix mScreenToNDCTransform;
	private GeometryContext mSampleContext;

	public void setSampleContext(GeometryContext c) {
		mSampleContext = c;
	}

	private GLSpriteProgram mSprite;
}
