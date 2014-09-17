package com.js.samplealgorithm;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.js.basic.Tools.*;

import com.js.geometry.MyMath;
import com.js.geometry.Point;
import com.js.geometry.Polygon;
import com.js.geometry.R;
import com.js.geometry.Rect;
import com.js.geometryapp.AlgDisplayElement;
import com.js.geometryapp.AlgorithmRenderer;
import com.js.geometryapp.AlgorithmStepper;
import com.js.geometryapp.GLSpriteProgram;
import com.js.geometryapp.GLTexture;
import com.js.geometryapp.PolygonRenderer;
import com.js.geometryapp.SpriteContext;

import android.content.Context;
import android.graphics.Color;

public class SampleRenderer extends AlgorithmRenderer {

	private static final boolean ADD_TEST_SPRITE = false;
	private static final boolean ADD_TEST_POLYGON = true;

	public SampleRenderer(Context context, SampleAlgorithm algorithm) {
		super(context);
		mStepper = AlgorithmStepper.sharedInstance();
		mAlgorithm = algorithm;
		doNothing();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
		// Let the algorithm stepper elements prepare using this renderer
		AlgDisplayElement.setRenderer(this);

		if (ADD_TEST_POLYGON) {
			warning("adding polygon for test purposes");
			mSamplePolygon = Polygon.discPolygon(Point.ZERO, 100, 13);
			mSamplePolygon.transformToFitRect(new Rect(200, 200, 500, 250),
					false);
		}

		if (ADD_TEST_SPRITE) {
			warning("adding sprite for test purposes");
			GLTexture t = new GLTexture(context(), R.raw.texture);
			Rect window = new Rect(0, 0, t.width(), t.height());

			mSprite = new GLSpriteProgram(SpriteContext.normalContext(), t,
					window);
		}
	}

	public void onDrawFrame(GL10 gl) {
		super.onDrawFrame(gl);

		mStepper.render();

		if (mSprite != null) {
			int frame = mAlgorithm.getFrameNumber();
			Point pt = MyMath.pointOnCircle(new Point(250, 100), frame * 7.0f
					* MyMath.M_DEG, 100);
			mSprite.setPosition(pt.x, pt.y);
			mSprite.render();
		}

		if (mSamplePolygon != null) {
			PolygonRenderer.setColor(Color.DKGRAY);
			PolygonRenderer.renderConvex(mSamplePolygon);
		}
	}

	private GLSpriteProgram mSprite;
	private Polygon mSamplePolygon;
	private SampleAlgorithm mAlgorithm;
	private AlgorithmStepper mStepper;
}
