package com.js.samplealgorithm;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.js.basic.Tools.*;

import com.js.geometry.GeometryContext;
import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.R;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgDisplayElement;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GLProgram;
import com.js.geometryapp.GLShader;
import com.js.geometryapp.GLSpriteProgram;
import com.js.geometryapp.GLTexture;
import com.js.geometryapp.Mesh;
import com.js.geometryapp.OurGLRenderer;
import com.js.geometryapp.Polyline;

import android.content.Context;
import android.graphics.Matrix;

public class SampleRenderer extends OurGLRenderer {

	public SampleRenderer(Context context, SampleAlgorithm algorithm) {
		super(context);
		mStepper = AlgorithmStepper.sharedInstance();
		mRotatingMesh = createMesh(0, 0);
		mStaticMesh = createMesh(150, 300);
		mPolyline = createPolyline();
		mAlgorithm = algorithm;
		doNothing();
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);

		disposeResources();

		// Let the algorithm stepper elements prepare using this renderer
		AlgDisplayElement.setRenderer(this);

		mVertexShader = GLShader.readVertexShader(context(),
				R.raw.simple_vertex_shader);
		mFragmentShader = GLShader.readFragmentShader(context(),
				R.raw.simple_fragment_shader);
		mProgram = GLProgram.build(mVertexShader, mFragmentShader);

		GLTexture t = new GLTexture(context(), R.raw.texture);
		mSprite = new GLSpriteProgram(t, new Rect(0, 0, 64, 64));
	}

	public void onDrawFrame(GL10 gl) {
		super.onDrawFrame(gl);

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

		synchronized (mAlgorithm) {
			mStepper.render();

			int frame = mAlgorithm.getFrameNumber();
			if (mSprite != null) {
				Point pt = MyMath.pointOnCircle(new Point(250, 100), frame
						* 7.0f * MyMath.M_DEG, 100);
				mSprite.setPosition(pt.x, pt.y);
				mSprite.render();
			}
		}
	}

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

	public void setSampleContext(GeometryContext c) {
		mSampleContext = c;
	}

	private void disposeResources() {
		mSprite = null;
	}

	private float mRotation;
	private float mScale = 1.0f;
	private Mesh mRotatingMesh;
	private Mesh mStaticMesh;
	private Polyline mPolyline;
	private GLShader mVertexShader;
	private GLShader mFragmentShader;
	private GLProgram mProgram;
	private GeometryContext mSampleContext;
	private GLSpriteProgram mSprite;
	private SampleAlgorithm mAlgorithm;
	private AlgorithmStepper mStepper;
}
