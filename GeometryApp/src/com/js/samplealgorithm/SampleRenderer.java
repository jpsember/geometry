package com.js.samplealgorithm;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.js.basic.Tools.*;

import com.js.geometry.GeometryContext;
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
import com.js.geometryapp.PolygonProgram;
import com.js.geometryapp.SpriteContext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;

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

		// Synchronize with stepper, so no race conditions with UI thread.
		synchronized (mStepper) {

			if (ADD_TEST_POLYGON) {
				warning("adding polygon for test purposes");
				mSamplePolygon = Polygon.discPolygon(Point.ZERO, 100, 13);
				mSamplePolygon.transformToFitRect(new Rect(200, 200, 500, 250),
						false);

				mSamplePolygon2 = Polygon.discPolygon(Point.ZERO, 100, 13);
				mSamplePolygon2.transformToFitRect(
						new Rect(300, 200, 400, 350), false);

				GeometryContext c = new GeometryContext(11);
				mSamplePolygon3 = Polygon.testPolygon(c,
						true ? Polygon.TESTPOLY_CONCAVE_BLOB
								: Polygon.TESTPOLY_DRAGON_X + 8);
				if (false) {
					warning("perturbing sample polygon");
					mSamplePolygon3.perturb(c);
				}
				mSamplePolygon3.transformToFitRect(algorithmRect(), false);

				mPolygonRenderer = null;
			}

			if (ADD_TEST_SPRITE) {
				warning("adding sprite for test purposes");
				GLTexture t = new GLTexture(context(), R.raw.texture);
				Rect window = new Rect(0, 0, t.width(), t.height());

				mSprite = new GLSpriteProgram(SpriteContext.normalContext(), t,
						window);
			}
		}
	}

	public void onDrawFrame(GL10 gl) {

		// Synchronize with stepper, so no race conditions with UI thread.
		synchronized (mStepper) {
			super.onDrawFrame(gl);

			mStepper.render();

			if (mSprite != null) {
				int frame = mAlgorithm.getFrameNumber();
				Point pt = MyMath.pointOnCircle(new Point(250, 100), frame
						* 7.0f * MyMath.M_DEG, 100);
				mSprite.setPosition(pt.x, pt.y);
				mSprite.render();
			}

			if (mSamplePolygon != null) {
				if (mPolygonRenderer == null) {
					mPolygonRenderer = new PolygonProgram(this,
							TRANSFORM_NAME_ALGORITHM_TO_NDC);
					mPolygonRenderer.setConvexPolygon(mSamplePolygon);
				}
				{
					int duration = 8;
					int step = mFrame % (duration * 2);
					if (step % duration == 0) {
						mPolygonRenderer.setColor(step > 0 ? Color.DKGRAY
								: Color.argb(120, 200, 200, 200));
					}
				}

				Matrix additionalTransform = null;
				{
					int duration = 13;
					int step = mFrame % (duration * 2);
					if (step >= duration) {
						additionalTransform = new Matrix();
						float s = 1.5f + ((step - duration) / (float) duration);
						additionalTransform.setScale(s, s);
					}
				}

				{
					int duration = 11;
					if (mFrame >= duration) {
						int step = mFrame % (duration * 4);
						if (step % duration == 0) {
							boolean convex = (step / (duration * 2)) == 0;
							convex = false;
							Polygon sourcePolygon = (step % duration) != 0 ? mSamplePolygon2
									: mSamplePolygon;
							if (!convex)
								sourcePolygon = mSamplePolygon3;
							if (convex) {
								mPolygonRenderer
										.setConvexPolygon(sourcePolygon);
							} else {
								mPolygonRenderer.setPolygon(sourcePolygon);
							}
						}
					}
				}

				mPolygonRenderer.render(additionalTransform);
			}

			mFrame++;
		}
	}

	private int mFrame;
	private PolygonProgram mPolygonRenderer;
	private GLSpriteProgram mSprite;
	private Polygon mSamplePolygon;
	private Polygon mSamplePolygon2;
	private Polygon mSamplePolygon3;
	private SampleAlgorithm mAlgorithm;
	private AlgorithmStepper mStepper;
}
